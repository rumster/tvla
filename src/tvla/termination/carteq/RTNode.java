package tvla.termination.carteq;

import tvla.core.Node;
import tvla.core.TVS;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.transitionSystem.Location;
import tvla.util.Pair;

import java.util.*;

/**
 * Created by BorisD on 09/14/2015.
 */
public final class RTNode {

  public enum RTNodeType {
    NestedLoopEnter,
    NestedLoopExit,
    ParentLoopEnter,
    ParentLoopExit,
    Default
  }


  public final int ID;
  public static int s_ID;

  public Location Location;
  public TVS      Structure;

  public String Label = "";
  public Object Tag; // for generic use

  public int LoopIndex = 0;
  public RTNodeType Type = RTNodeType.Default;

  private Map<Integer, RTSubNode> m_SubNodes = null;

  public RTNode(Pair<TVS, Location> pair) {
    this(pair.second, pair.first);
  }

  public RTNode(Location loc, TVS struct) {
    ID = s_ID++;
    Location = loc;
    Structure = struct;

    if (loc == null || struct == null) {
      System.out.println("TraceNode internal error");
    }

    LoopIndex = Character.isDigit(loc.label().charAt(1)) ? Integer.parseInt(loc.label().substring(1, 2)) : 1;
  }

  @Override
  public String toString() {
    String result = Integer.toString(ID);

    return result;
  }

  public String toStringFull() {
    String result = Location.label() + "_N" + Integer.toString(ID);

    return result;
  }

  public void ToDot(StringBuffer stringBuffer) {

    stringBuffer.append("\"" + Integer.toString(ID) + "\" ");
    stringBuffer.append("[label=<<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"5\"><TR><TD>");
    stringBuffer.append(Location.label() + "_N" + Integer.toString(ID) + " " + Label + "</TD>");

    Map<Integer, RTSubNode> subNodes = GetSubNodes();

    for (Node node : Structure.nodes()) {

      RTSubNode subNode = subNodes.get(node.id());

      stringBuffer.append("<TD PORT=\"");
      stringBuffer.append(subNode.NamePrefixed);
      stringBuffer.append("\"");

      if (subNode.Type == RTSubNode.RTSubNodeType.Frame) {
        stringBuffer.append(" BGCOLOR=\"#C8C8C8\"");
      } else if (subNode.Type == RTSubNode.RTSubNodeType.CutPoint) {
        stringBuffer.append(" BGCOLOR=\"#C9FF00\"");
      } else if (subNode.Type == RTSubNode.RTSubNodeType.NewNode) {
        stringBuffer.append(" BGCOLOR=\"#87CEFA\"");
      }

      stringBuffer.append(">");
      stringBuffer.append(subNode.NamePrefixed);
      stringBuffer.append("</TD>");
    }

    stringBuffer.append("</TR></TABLE>>];");
  }

  public void ToSimpleFormat(StringBuffer stringBuffer) {
    stringBuffer.append(toStringFull());

    Map<Integer, RTSubNode> subNodes = GetSubNodes();

    for (Node node : Structure.nodes()) {
      RTSubNode subNode = subNodes.get(node.id());

      stringBuffer.append(", ");
      stringBuffer.append(subNode.NamePrefixed);
    }
  }

  public Map<Integer, RTSubNode> GetSubNodes() {

    if (m_SubNodes == null) {
      m_SubNodes = new HashMap<>();

      Predicate sm = null;
      for (Predicate p : Structure.getVocabulary().unary()) {
        if (p.name() == "sm") {
          sm = p;
          break;
        }
      }

      for (Node node : Structure.nodes()) {
        Kleene k = Structure.eval(sm, node);
        m_SubNodes.put(node.id(), new RTSubNode(this, node.id(), k != Kleene.falseKleene));
      }
    }

    return m_SubNodes;
  }

  public void UpdateSubData(Map<Node, List<Node>> transitionSource) {

    Map<Integer, RTSubNode> subNodes = GetSubNodes();

    for (Map.Entry<Node, List<Node>> entry : transitionSource.entrySet()) {

      for (Node node : entry.getValue()) {
        subNodes.get(node.id()).DegreeIn++;
      }
    }
  }

  public Map<Node, List<Node>> GetIdentityRT() {

    Map<Node, List<Node>> result = new HashMap<>();

    for (Node node : Structure.nodes()) {
      result.put(node, Arrays.asList(node));
    }

    return result;
  }

  @Override public boolean equals(Object aThat) {

    return Structure.equals(((RTNode)aThat).Structure) && Location.equals(((RTNode)aThat).Location);
  }
}
