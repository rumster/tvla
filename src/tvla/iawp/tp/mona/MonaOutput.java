package tvla.iawp.tp.mona;

import java.io.IOException;

import tvla.iawp.tp.NativeProcess;
import tvla.iawp.tp.TheoremProverOutputNoPushback;
import tvla.iawp.tp.TheoremProverResult;
import tvla.iawp.tp.TheoremProverValueResult;

/**
 * @author Eran Yahav (yahave)
 */
public class MonaOutput extends TheoremProverOutputNoPushback {

  private final static String validString = "Formula is valid";

  private final static String invalidString = "Formula is unsatisfiable";

  private final static String satString = "A satisfying example";

  private final static String counterString = "A counter-example";

  private final static String parseString = "PARSING";

  private final static String freevarString = "Free variables are:";

  public MonaOutput(NativeProcess np, String query) {
    super(np);
  }

  /**
   * parse next output and set done=true if result is depleated. parsed output
   * should be placed in the parsedResults collection.
   */
  protected void parseNextOutput() {
    TheoremProverResult result;
    from.skipWhitespaces();
    // check first line
    String line = from.readLine();
    if (line.startsWith(freevarString)) {
      from.skipWhitespaces();
      line = from.readLine();
    }
    if (line.startsWith(validString)) {
      result = TheoremProverValueResult.VALID;
    } else if (line.startsWith(invalidString)) {
      // result = TheoremProverValueResult.INCONSISTENT;
      result = TheoremProverValueResult.INVALID;
    } else if (line.startsWith(parseString)) {
      result = TheoremProverValueResult.UNKNOWN;
    } else if (line.startsWith(counterString)) {
      result = TheoremProverValueResult.INVALID;
    } else {
      result = TheoremProverValueResult.UNKNOWN;
    }

    // read the rest of the buffer
    StringBuffer fromContent = new StringBuffer();
    fromContent.append(line);
    try {
      for (int i = 0, n = from.available(); i < n; i++)
        fromContent.append(from.readChar());
    } catch (IOException e) {
    }
    if (result.equals(TheoremProverValueResult.UNKNOWN)) {
      throw new RuntimeException("parseNextOutput(), parse error \n theorem prover returned: \n" + fromContent.toString()
          + "\n end of theorem prover result");
    }
    done = true;
    parsedResults.add(result);
  }

  /*
   * (non-Javadoc)
   * 
   * @see tvla.tp.TheoremProverOutput#skipOutputHeader()
   */
  protected void skipOutputHeader() {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see tvla.tp.TheoremProverOutput#isResultPrefix(char)
   */
  protected boolean isResultPrefix(char c) {
    // TODO Auto-generated method stub
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see tvla.tp.TheoremProverOutput#parseResult()
   */
  protected TheoremProverResult parseResult() {
    // TODO Auto-generated method stub
    return null;
  }
}
