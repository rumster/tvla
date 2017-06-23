package tvla.termination;

/**
 * Created by BorisD on 09/14/2015.
 */
public final class RTSubNode {

  public enum RTSubNodeType {
    CutPoint,
    NewNode,
    Frame,
    Default
  }

  private static int s_ID;

  public final RTNode Parent;
  public final int ID;

  public final int SubNodeID;
  public final String NamePrefixed;

  public RTSubNodeType Type;

  public int RefCount = 0; // For optimization of internal alg.
  public int DegreeIn = 0; // For optimization of internal alg.

  public int MarkVersion = -1;
  public static int s_MarkVersion = 0;

  public RTSubNode(RTNode parent, int subNodeID) {
    ID = s_ID++;
    Parent = parent;
    SubNodeID = subNodeID;
    Type = RTSubNodeType.Default;
    NamePrefixed = "n" + SubNodeID;
  }

  public String toString() {
    String result = Parent.toString() + "_" + Integer.toString(SubNodeID);

    return result;
  }
}
