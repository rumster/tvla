package tvla.termination.carteq;

import java.util.*;

/**
 * Created by BorisD on 09/14/2015.
 */
public final class SNode {

  public final int    ID;
  public final String Name;

  public final Map<String, SSubNode> SubNodes;
  public final Map<SNode, Map<SSubNode, List<SSubNode>>> RegionTransition;
  public final Map<SNode, Map<SSubNode, List<SSubNode>>> EqualityTransition;

  private static int s_ID = 0;

  public SNode(String name) {
    ID = s_ID++;

    Name = name;
    SubNodes = new HashMap<>();
    RegionTransition = new HashMap<>();
    EqualityTransition = new HashMap<>();
  }

  public void ToDot(StringBuffer stringBuffer, String prefix) {
    prefix = prefix != null ? prefix : "";

    stringBuffer.append("\"" + prefix + ID + "\" ");
    stringBuffer.append("[label=<<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"5\"><TR><TD>");
    stringBuffer.append(prefix + Name + "</TD>");

    for (SSubNode subNode : SubNodes.values()) {

      stringBuffer.append("<TD PORT=\"");
      stringBuffer.append(subNode.Name);
      stringBuffer.append("\"");

      stringBuffer.append(">");
      stringBuffer.append(subNode.Name);
      stringBuffer.append("</TD>");
    }

    stringBuffer.append("</TR></TABLE>>];");
  }

  @Override
  public String toString() {
    return Name;
  }
}
