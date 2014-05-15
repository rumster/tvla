package tvla.io;

//import tvla.analysis.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.common.NodeTupleIterator;
import tvla.exceptions.UserErrorException;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Logger;
import tvla.util.ProgramProperties;
import tvla.util.StringUtils;

/**
 * Converts a structure to DOT format.
 * 
 * @author Tal Lev-Ami.
 * @author Roman manevich.
 */
public class StructureToDOT extends StringConverter {
    /**
     * A convenience object.
     */
    public static final StructureToDOT defaultInstance = new StructureToDOT();

    /** A string for writing new line characters in DOT labels.
     */
    private static final String DOT_NEWLINE = "\\n";

    /** A string opening a table containing values of nullary predicates.
     */
    private static final String NULLARY_TABLE_BEGIN = "<TABLE BORDER=\"1\" CELLBORDER=\"0\" CELLSPACING=\"0\">";

    /** A string closing a table containing values of nullary predicates.
     */
    private static final String NULLARY_TABLE_END = "</TABLE>";

    /** Indicates that values of nullary predicates should be drawn inside a table.
     */
    private static final int NULLARIES_AS_HTML_TABLE = 0;

    /** Indicates that values of nullary predicates should appear in diamond shaped nodes.
     */
    private static final int NULLARIES_AS_DIAMONDS = 1;

    /** Indicates that values of nullary predicates should be drawn inside text shaped
     * nodes arranged in a matrix formation.
     */
    private static final int NULLARIES_AS_MATRIX = 2;

    
    /** Should the node name be printed
     */
    private static final boolean printNodeName = true;

    private static final int MAX_DOT_HEADER_LINE = 50;
    
    /** Indicates the style that is used to draw values of nullary predicates.
     */
    private int nullaryStyle;

    /** A true value Indicates that landscape layout should be used (rankdir=LR).
     */
    private boolean rotate;

    /** Global DOT attributes that should be added to every drawn structure.
     */
    private String structureAttributes;

    /** Global DOT attributes for every page.
     */
    private String pageAttributes;

    /** A map from Predicate objects to their respective DOT attributes, as specified
     * in ProgramProperties.
     */
    private Map<Predicate, String> predicateToAttributes = HashMapFactory.make();

    /** DOT attributes for the active predicate.
     */
    private String acAttributes;

    /** DOT attributes for pointer (box) unary predicates.
     */
    private String pointerAttributes;

    /** Constructs a structure to DOT converter object.
     */
    public StructureToDOT() {
        init();
    }

    /**
     * Converts the specified structure to DOT format.
     */
    public String convert(Object o) {
        return convert(o, "");
    }

    /**
     * Converts the specified structure to DOT format and adds the specified
     * header message.
     * 
     * @since 18/12/2000 changed representation of nullary predicates - instead
     *        of diamonds, all nullary predicates are listed in a box.
     */
    public String convert(Object o, String heading) {
        if (heading == null)
            heading = "";
        TVS structure = (TVS) o;
        StringBuffer result = new StringBuffer(CommentToDOT.getPageComment());

        boolean constraint = heading.indexOf("Constraint Breached:") >= 0;
        heading = StringUtils
                .replace(heading, StringUtils.newLine, DOT_NEWLINE);
        if (constraint) { // break the message into 3 lines.
            heading = StringUtils.replace(heading, "Constraint Breached:",
                    "Constraint Breached:" + DOT_NEWLINE);
            heading = StringUtils.replace(heading, " on assignment",
                    DOT_NEWLINE + "on assignment");
        }
        // break in at most 50 chars per line
        String[] lines = heading.split(DOT_NEWLINE);
        StringBuilder headingBuilder = new StringBuilder();
        String sep = "";
        for (String line : lines) {
            for (int i = 0; i < line.length(); i += MAX_DOT_HEADER_LINE) {
                headingBuilder.append(sep);
                sep = DOT_NEWLINE;
                int end = Math.min(i + MAX_DOT_HEADER_LINE, line.length());
                headingBuilder.append(line.substring(i, end));
            }
        }
        heading = headingBuilder.toString();
        
        result.append("digraph structure {" + StringUtils.newLine
                + pageAttributes);
        if (rotate)
            result.append("rankdir=LR;" + StringUtils.newLine);
        if (!heading.equals("")) {
            result.append("subgraph cluster_lab { label = \"" + heading + "\";"
                    + StringUtils.newLine);
        }

        result.append(convertNullaryPredicates(structure));

        // detect structures with an empty universe
        if (structure.nodes().isEmpty()) {
            result.append("\"Structure with empty universe\" [fontsize = 20, shape=plaintext]; "
                            + StringUtils.newLine + "}" + StringUtils.newLine);
            if (!heading.equals(""))
                result.append(StringUtils.newLine + "}" + StringUtils.newLine);
            return result.toString();
        }

        // set global drawing attributes
        result.append(structureAttributes);

   // Noam: Prevents a constranit from appearing inside the graph, beside the heading
   //     if (constraint)
   //         result.append("\"" + heading + "\" [style=invis];"
   //                 + StringUtils.newLine);

        result.append(createNodesForUnaryPointerPredicates(structure));
        result.append(convertUnaryPredicates(structure));
        result.append(convertBinaryPredicates(structure));
        if (!structure.getVocabulary().kary().isEmpty())
            result.append(convertHighArityPredicates(structure));

        if (!heading.equals(""))
            result.append("}" + StringUtils.newLine);
        result.append("}" + StringUtils.newLine);

        return result.toString();
    }

    /** Converts a structure into a DOT representation of text nodes 
     * for unary predicates with the 'pointer' property.
     * 
     * @param structure A 3-valued structure.
     * @return DOT representation of text nodes for unary predicates with the
     * 'pointer' property.
     */
    protected String createNodesForUnaryPointerPredicates(TVS structure) {
        StringBuffer result = new StringBuffer("");
        for (Predicate predicate : structure.getVocabulary().unary()) {
            int numberSatisfy = structure.numberSatisfy(predicate);
            if (numberSatisfy == 0 || !predicate.pointer())
                continue;
            boolean draw = false;
            if (predicate.showFalse() && numberSatisfy == 0)
                draw = true;
            else if (predicate.showUnknown() && numberSatisfy != 0)
                draw = true;
            else if (!predicate.showFalse() && !predicate.showUnknown()
                    && predicate.showTrue() && numberSatisfy != 0) {
                for (Node node : structure.nodes()) {
                    Kleene predicateValue = structure.eval(predicate, node);
                    if (predicateValue == Kleene.trueKleene) {
                        draw = true;
                        break;
                    }
                }
            }
            else
                draw = false;
            if (draw)
                result.append("\"" + predicate.name() + "\" ["
                        + pointerAttributes + "];" + StringUtils.newLine);
        }
        return result.toString();
    }

    protected String convertUnaryPredicates(TVS structure) {
        StringBuffer result = new StringBuffer("");

        for (Node node : structure.nodes()) {
            StringBuffer instrumsTrue = null;
            StringBuffer instrumsUnknown = null;
            StringBuffer instrumsFalse = null;

            for (Predicate predicate : structure.getVocabulary().unary()) {
                if (predicate == Vocabulary.sm
                        || predicate == Vocabulary.active)
                    continue;

                Kleene value = structure.eval(predicate, node);
                boolean showAsTrue = value == Kleene.trueKleene
                        && predicate.showTrue();
                boolean showAsFalse = value == Kleene.falseKleene
                        && predicate.showFalse();
                boolean showAsUnknown = value == Kleene.unknownKleene
                        && predicate.showUnknown();
                boolean show = showAsTrue || showAsFalse || showAsUnknown;
                if (!show)
                    continue;

                if (predicate.pointer()) {
                    result.append("\"" + predicate.name() + "\"");
                    result.append("->\"");
                    result.append(node.name() + "\"");
                    if (showAsUnknown)
                        result.append(" [style=dotted]");
                    else if (showAsFalse)
                        result.append(" [color=red]");
                    result.append(";" + StringUtils.newLine);
                }
                else if (showAsTrue) {
                    if (instrumsTrue == null) {
                        instrumsTrue = new StringBuffer();
                    }
                    else {
                        instrumsTrue.append(DOT_NEWLINE);
                    }
                    instrumsTrue.append(predicate.name());
                }
                else if (showAsUnknown) {
                    if (instrumsUnknown == null) {
                        instrumsUnknown = new StringBuffer();
                    }
                    else {
                        instrumsUnknown.append(DOT_NEWLINE);
                    }
                    instrumsUnknown.append(predicate.name() + "=1/2");
                }
                else if (showAsFalse) {
                    if (instrumsFalse == null) {
                        instrumsFalse = new StringBuffer();
                    }
                    else {
                        instrumsFalse.append(DOT_NEWLINE);
                    }
                    instrumsFalse.append(predicate.name() + "=0");
                }
            }
            result.append("\"" + node.name() + "\" [");

            String sep = "";
            result.append("label=\"");
            if (printNodeName)
                result.append(node.name() + DOT_NEWLINE);
            if (instrumsTrue != null) {
                result.append(sep);
                sep = DOT_NEWLINE;
                result.append(instrumsTrue);
            }
            if (instrumsUnknown != null) {
                result.append(sep);
                sep = DOT_NEWLINE;
                result.append(instrumsUnknown);
            }
            if (instrumsFalse != null) {
                result.append(sep);
                sep = DOT_NEWLINE;
                result.append(instrumsFalse);
            }
            result.append("\"");

            sep = ", ";
            if (structure.eval(Vocabulary.active, node) == Kleene.unknownKleene)
                result.append(acAttributes);
            for (Map.Entry<Predicate, String> entry : predicateToAttributes.entrySet()) {
                Predicate predicate = entry.getKey();
                if (predicate.arity() != 1 || predicate == Vocabulary.active)
                    continue;
                if (structure.eval(predicate, node) != Kleene.falseKleene)
                    result.append(sep + entry.getValue());
            }

            result.append("];" + StringUtils.newLine);
        }
        return result.toString();
    }

    protected String convertBinaryPredicates(TVS structure) {
        StringBuffer result = new StringBuffer("");

        for (Predicate pred : structure.getVocabulary().binary()) {
            if (structure.numberSatisfy(pred) == 0)
                continue;
            Set<NodeTuple> printed = HashSetFactory.make();
            for (Node left : structure.nodes()) {
                for (Node right : structure.nodes()) {
                    NodeTuple tmpPair = NodeTuple.createPair(left, right);
                    Kleene value = structure.eval(pred, left, right);
                    if (value == Kleene.falseKleene && pred.showFalse()) {
                        result.append("\"" + left.name());
                        result.append("\"->\"");
                        result.append(right.name());
                        result.append("\" [label=\"" + pred.name() + "\"");
                        result.append(", color=red");
                        result.append("];" + StringUtils.newLine);
                    }
                    else if ((value == Kleene.trueKleene && pred.showTrue())
                            || (value == Kleene.unknownKleene && pred
                                    .showUnknown())) {
                        if (printed.contains(tmpPair))
                            continue;
                        else {
                            printed.add(NodeTuple.createPair(left, right));
                        }

                        Kleene invValue = structure.eval(pred, right, left);
                        boolean bidir = (invValue == value)
                                && !(left.equals(right));
                        result.append("\"" + left.name());
                        result.append("\"->\"");
                        result.append(right.name());
                        result.append("\" [label=\"" + pred.name() + "\"");
                        if (value == Kleene.unknownKleene)
                            result.append(", style=dotted");
                        if (bidir) {
                            printed.add(NodeTuple.createPair(right, left));
                            result.append(", dir=both");
                        }
                        result.append("];" + StringUtils.newLine);
                    }
                }
            }
        }

        return result.toString();
    }

    protected String convertHighArityPredicates(TVS structure) {
        StringBuffer result = new StringBuffer("");

        int bindingId = 0;
        for (Predicate predicate : structure.getVocabulary().kary()) {
            if (structure.numberSatisfy(predicate) == 0)
                continue;
            Iterator<? extends NodeTuple> tupleIter = NodeTupleIterator.createIterator(structure
                    .nodes(), predicate.arity());
            while (tupleIter.hasNext()) {
                NodeTuple tuple = tupleIter.next();
                Kleene value = structure.eval(predicate, tuple);
                boolean showAsTrue = value == Kleene.trueKleene
                        && predicate.showTrue();
                boolean showAsFalse = value == Kleene.falseKleene
                        && predicate.showFalse();
                boolean showAsUnknown = value == Kleene.unknownKleene
                        && predicate.showUnknown();
                boolean show = showAsTrue || showAsFalse || showAsUnknown;
                if (!show)
                    continue;

                ++bindingId;
                String bindingName = predicate.name() + "_binding" + bindingId;
                result.append(bindingName + "[label=\"" + predicate.name()
                        + ":" + tuple.size() + "\""
                        + ", color=grey, style=filled];" + StringUtils.newLine);
                for (int nodeIndex = 0; nodeIndex < tuple.size(); ++nodeIndex) {
                    String edgeStyle = "";
                    if (value == Kleene.unknownKleene)
                        edgeStyle = "style=dotted, ";
                    if (value == Kleene.falseKleene && showAsFalse)
                        edgeStyle = "color=red, ";
                    String edgeAttributes = " [" + edgeStyle + "label=\""
                            + (nodeIndex + 1) + "\"" + ", dir=none];";
                    result.append(bindingName + "->"
                            + tuple.get(nodeIndex).name() + edgeAttributes
                            + StringUtils.newLine);
                }
                result.append(StringUtils.newLine);
            }
        }

        return result.toString();
    }

    protected String convertNullaryPredicates(TVS structure) {
        if (structure.getVocabulary().nullary().isEmpty())
            return "";
        String result = null;
        if (nullaryStyle == NULLARIES_AS_HTML_TABLE) {
            result = convertNullaryPredicatesToHTMLTable(structure);
        }
        else if (nullaryStyle == NULLARIES_AS_DIAMONDS) {
            result = convertNullaryPredicatesToNodes(structure);
        }
        else if (nullaryStyle == NULLARIES_AS_MATRIX) {
            result = convertNullaryPredicatesToNodeTable(structure);
        }
        else {
            throw new UserErrorException(
                    "Internal Error! Invalid value for drawing of nullary predicates.");
        }
        return result;
    }

    protected String convertNullaryPredicatesToHTMLTable(TVS structure) {
        StringBuffer result = new StringBuffer("");

        Set<Predicate> shownPredicates = HashSetFactory.make();
        for (Predicate predicate : structure.getVocabulary().nullary()) {
            Kleene value = structure.eval(predicate);
            boolean showAsTrue = (value == Kleene.trueKleene)
                    && predicate.showTrue();
            boolean showAsFalse = (value == Kleene.falseKleene)
                    && predicate.showFalse();
            boolean showAsUnknown = (value == Kleene.unknownKleene)
                    && predicate.showUnknown();
            boolean show = showAsTrue || showAsFalse || showAsUnknown;
            if (show) {
                shownPredicates.add(predicate);
            }
        }
        final int ROW_LENGTH = (int) Math.sqrt((double) shownPredicates.size());
        if (ROW_LENGTH == 0)
            return "";

        result.append("nullary [shape=plaintext, label=<" + StringUtils.newLine
                + NULLARY_TABLE_BEGIN + StringUtils.newLine);
        result.append("<TR><TD COLSPAN=\"" + ROW_LENGTH
                + "\">nullary</TD></TR>" + StringUtils.newLine);

        int counter = 0;
        for (Iterator<Predicate> i = shownPredicates.iterator(); i.hasNext(); ++counter) {
            result.append("<TR>");
            for (int rowCounetr = 0; rowCounetr < ROW_LENGTH && i.hasNext(); ++rowCounetr) {
                result.append("<TD>");
                Predicate predicate = i.next();
                Kleene value = structure.eval(predicate);
                if (value == Kleene.trueKleene) {
                    result.append(predicate);
                }
                else {
                    result.append(predicate + "=" + value);
                }
                result.append("</TD>");
            }
            result.append("</TR>" + StringUtils.newLine);
        }
        result.append(NULLARY_TABLE_END + ">];");
        return result.toString();
    }

    protected String convertNullaryPredicatesToNodes(TVS structure) {
        StringBuffer result = new StringBuffer("");
        for (Predicate predicate : structure.getVocabulary().nullary()) {
            Kleene value = structure.eval(predicate);
            boolean showAsTrue = (value == Kleene.trueKleene)
                    && predicate.showTrue();
            boolean showAsFalse = (value == Kleene.falseKleene)
                    && predicate.showFalse();
            boolean showAsUnknown = (value == Kleene.unknownKleene)
                    && predicate.showUnknown();
            boolean show = showAsTrue || showAsFalse || showAsUnknown;

            if (show) {
                result.append("\"" + predicate + "\" [shape=diamond");
                if (showAsFalse) {
                    result.append(",color=red");
                }
                else if (showAsUnknown) {
                    result.append(",style=dotted");
                }
                result.append("];" + StringUtils.newLine);
            }
        }

        return result.toString();
    }

    protected String convertNullaryPredicatesToNodeTable(TVS structure) {
        StringBuffer result = new StringBuffer("");

        final int ROW_LENGTH = (int) Math.sqrt((double) structure.getVocabulary().nullary().size());
        Map<String, String> shownPredicates = HashMapFactory.make();
        result.append("subgraph cluster_nullaries { label = \"nullary\";"
                + StringUtils.newLine); // open subgraph box
        result.append("ranksep= 0.00;\nnodesep = 0.00;" + StringUtils.newLine
                + "node [shape=plaintext];\nedge [style=\"invis\", dir=none];"
                + StringUtils.newLine);
        int nularyPredicateCounter = 0;
        for (Predicate predicate : structure.getVocabulary().nullary()) {
            Kleene value = structure.eval(predicate);
            boolean showAsTrue = (value == Kleene.trueKleene)
                    && predicate.showTrue();
            boolean showAsFalse = (value == Kleene.falseKleene)
                    && predicate.showFalse();
            boolean showAsUnknown = (value == Kleene.unknownKleene)
                    && predicate.showUnknown();
            boolean show = showAsTrue || showAsFalse || showAsUnknown;
            if (show) {
                String predString = "\"" + predicate.toString() + "\" ";
                result.append(predString + "->");
                String color = null;
                if (value == Kleene.falseKleene)
                    color = "red";
                else if (value == Kleene.unknownKleene)
                    color = "green";
                else
                    color = "black";
                shownPredicates.put(predicate.toString(), color);
            }
            else
                continue;

            ++nularyPredicateCounter;
            if (nularyPredicateCounter % ROW_LENGTH == 0) {
                result.delete(result.length() - 2, result.length());
                result.append(";" + StringUtils.newLine);
            }
        }

        if (result.charAt(result.length() - 1) == '>')
            result.delete(result.length() - 2, result.length());
        result.append(StringUtils.newLine);
        for (Map.Entry<String, String> entry : shownPredicates.entrySet()) {
            result.append("\"" + entry.getKey() + "\" [fontcolor="
                    + entry.getValue() + "]");
            result.append(";" + StringUtils.newLine);
        }
        result.append("}" + StringUtils.newLine); // close the subgraph box
        return result.toString();
    }

    /**
     * Initializes private data.
     */
    protected void init() {
        rotate = ProgramProperties.getBooleanProperty("tvla.dot.rotate", false);
        String style = ProgramProperties.getProperty("tvla.dot.nullaryStyle",
                "table");
        if (style.equals("table"))
            nullaryStyle = NULLARIES_AS_HTML_TABLE;
        else if (style.equals("matrix"))
            nullaryStyle = NULLARIES_AS_MATRIX;
        else if (style.equals("diamonds"))
            nullaryStyle = NULLARIES_AS_DIAMONDS;
        else
            throw new UserErrorException(
                    "Invalid property value! tvla.dot.rotate = " + style);

        structureAttributes = "ranksep= 0.2;nodesep=0.2;edge [fontsize=10];node [fontsize=10]";
        structureAttributes = ProgramProperties.getProperty(
                "tvla.dot.structureAttributes", structureAttributes);
        structureAttributes = structureAttributes + StringUtils.newLine;

        pageAttributes = "size = \"7.5,10\";center=true;fonstsize=6;";
        pageAttributes = ProgramProperties.getProperty(
                "tvla.dot.pageAttributes", pageAttributes);
        pageAttributes = pageAttributes + StringUtils.newLine;

        acAttributes = ProgramProperties.getProperty(
                "tvla.dot.predicateAttributes.active", "color=green");
        pointerAttributes = ProgramProperties
                .getProperty("tvla.dot.pointerAttributes",
                        "tvla.dot.pointerAttributes = shape=plaintext, style=bold, fontsize=18");
        DOTDisplayProperties.instance().initDisplayProperties();

        Map<String,String> allProperties = ProgramProperties.getAllProperties();
        for (Map.Entry<String, String> entry : allProperties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!key.startsWith("tvla.dot.predicateAttributes"))
                continue;
            int dotposition = key.lastIndexOf('.');
            if (dotposition < 0)
                continue;
            String predicateName = key.substring(dotposition + 1, key.length());
            Predicate predicate = Vocabulary.getPredicateByName(predicateName);
            if (predicate == null) {
                Logger
                        .println("Unable to find predicate specified by the property "
                                + key);
                continue;
            }
            predicateToAttributes.put(predicate, value);
        }
    }
}
