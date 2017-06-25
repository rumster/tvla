package tvla.termination;

import tvla.core.Node;
import tvla.core.TVS;
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
  private static int s_ID;

  public Location Location;
  public TVS      Structure;

  public String Label = "";
  public Object Tag; // for generic use

  public int LoopIndex = 0;
  public RTNodeType Type = RTNodeType.Default;

  private Map<Integer, RTSubNode> m_SubNodes = null; 

  public RTNode(Pair<TVS, Location> pair, Integer loopIndex) {
    this(pair.second, pair.first, loopIndex);
  }

  public RTNode(Location loc, TVS struct, int loopIndex) {
    ID = s_ID++;
    Location = loc;
    Structure = struct;
    LoopIndex = loopIndex;

    if (loc == null || struct == null || LoopIndex < 0) {
      System.out.println("TraceNode internal error");
    }
  }

  @Override
  public String toString() {
    String result = Integer.toString(ID);

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

  public Map<Integer, RTSubNode> GetSubNodes() {

    if (m_SubNodes == null) {
      m_SubNodes = new HashMap<>();

      for (Node node : Structure.nodes()) {
        m_SubNodes.put(node.id(), new RTSubNode(this, node.id()));
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

  public static RTNode Create(Location loc, TVS struct, Boolean parseLoopIndex, List<String> messages) {
      
      int loopIndex = 1;
      
      if (parseLoopIndex) {
          try {
              loopIndex = Integer.parseInt(loc.label().trim().substring(1, 2));
          }
          catch (Exception e) {
              loopIndex = 1;
              messages.add("Termination analysis failed to parse location " + loc.label());
          }
      }
      
      RTNode result = new RTNode(loc, struct, loopIndex);
      return result;
  }

  /*
  @Override public boolean equals(Object aThat) {

    return Structure == ((RTNode)aThat).Structure && Location == ((RTNode)aThat).Location;
  }
  */
}
