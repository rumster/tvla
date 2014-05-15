package tvla.io;

import java.util.Iterator;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.common.NodeTupleIterator;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.ProgramProperties;

/** Converts a structure to TVS format.
 * @author Roman Manevich
 * @since tvla-2-alpha added support for arbitrary arity predicates (May 16 2002 Roman).
 */
public class StructureToTVS extends StringConverter {
	/** A convenience object.
	 */
	public static StructureToTVS defaultInstance = new StructureToTVS();

	private static boolean tvla091BackwardCompatibility;
	
	/** Needed for backward compatibility.
	 * In TVLA 0.91 nodes used to be prefixed by an underscore.
	 */
	private static String nodePrefix;

	// Initializing static attributes from properties.
	static {
		tvla091BackwardCompatibility = ProgramProperties.getBooleanProperty("tvla.tvs.tvla091BackwardCompatibility", true);
		if (tvla091BackwardCompatibility)
			nodePrefix = "_";
	}

	public StructureToTVS() {
	}
	
	/** Converts the specified structure to TVS string format.
	 */
	public String convert(Object o) {
		return convert(o, "");
	}

	/** Converts the specified structure to TVS 
	 * string format and adds the specified header 
	 * as a comment.
	 */
	public String convert(Object o, String header) {
		TVS structure = (TVS) o;
		StringBuffer result = new StringBuffer();

		if (!header.equals(""))
			result.append("\n" + CommentToTVS.defaultInstance.convert(header));
		
		result.append("  %n = {");
		String sep = "";
		for (Node node : structure.nodes()) {
			if (node == null)
				throw new RuntimeException("" + structure.getClass());
			result.append(sep + nodePrefix + node.name());
			sep = ", ";
		}
		result.append("}\n  %p = {\n");

		for (Predicate predicate : structure.getVocabulary().nullary()) {
			Kleene value = structure.eval(predicate);
			if (value != Kleene.falseKleene)
				result.append("    " + predicate + " = " + value + "\n");
		}

        for (Predicate predicate : structure.getVocabulary().positiveArity()) {
			String predicateName = predicate.name();
			
			// convert to the old name of the active predicate - inac
			if (tvla091BackwardCompatibility && predicate == Vocabulary.active)
				predicateName = "inac";
			
			if (structure.numberSatisfy(predicate) == 0)
				continue;
			result.append("    " + predicateName + " = {");

			Iterator<? extends NodeTuple> tupleIter = NodeTupleIterator.createIterator(structure.nodes(), predicate.arity());
			while (tupleIter.hasNext()) {
				NodeTuple tuple = tupleIter.next();
				Kleene value = structure.eval(predicate, tuple);
				if (value != Kleene.falseKleene) {
					if (predicate.arity() == 1)
						result.append(nodePrefix + tuple.get(0) + ":" + value + ", ");
					else if (predicate.arity() == 2)
						result.append(nodePrefix + tuple.get(0) + "->" + 
									  nodePrefix + tuple.get(1) + ":" + value + ", ");
					else
						result.append(tuple.toString() + ":" + value + ", ");
				}
			}
			result.delete(result.length()-2, result.length());
			result.append("}\n");
		}
		
		result.append("  }");
		return result.toString();
	}
}