package tvla.termination;

/**
 * Created by BorisD on 09/14/2015.
 */
public final class SSubNode {

  public final int    ID;
  public final String Name;
  public final SNode  Parent;

  private static int s_ID = 0;

  public SSubNode(String name, SNode parent) {
    ID = s_ID++;

    Name = name;
    Parent = parent;
  }

  public boolean IsSummary() {
    return Name.startsWith("s");
  }
}