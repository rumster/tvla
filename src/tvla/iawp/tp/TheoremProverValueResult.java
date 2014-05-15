package tvla.iawp.tp;

/**
 * A result code from the theorem prover. Following the typesafe enum pattern.
 * (see <a
 * href="http://developer.java.sun.com/developer/Books/effectivejava/Chapter5.pdf">
 * Effective Java - Chapter 5</a> )
 * 
 * @author Eran Yahav (eyahav)
 */
public class TheoremProverValueResult implements TheoremProverResult {

  private final String name;

  private TheoremProverValueResult(String name) {
    this.name = name;
  }

  public String toString() {
    return name;
  }

  public static final TheoremProverValueResult VALID = new TheoremProverValueResult("Valid");

  public static final TheoremProverValueResult INVALID = new TheoremProverValueResult("Invalid");

  public static final TheoremProverValueResult UNKNOWN = new TheoremProverValueResult("Unknown");

  public static final TheoremProverValueResult INCONSISTENT = new TheoremProverValueResult("Inconsistent");

  public static final TheoremProverValueResult ERROR = new TheoremProverValueResult("Error");

}
