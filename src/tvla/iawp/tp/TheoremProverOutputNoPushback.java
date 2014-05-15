package tvla.iawp.tp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tvla.util.Logger;

/**
 * An adapter over the output from the TheoremProver + additional functionality.
 * This wraps the raw output produced by the theorem prover as a result from the
 * issued query. Usually has to be parsed to obtain the TheoremProverResult.
 * 
 * @author Eran Yahav (eyahav)
 */
public abstract class TheoremProverOutputNoPushback extends TheoremProverOutputBase {

  /**
   * debugging flag
   */
  private static final boolean debug = false;

  /**
   * parsed results obtained from output
   */
  protected List parsedResults = new ArrayList();

  /**
   * is this output complete?
   */
  protected boolean done = false;

  /**
   * creates an output parser for the theorem prover native process
   */
  public TheoremProverOutputNoPushback(NativeProcess np) {
    super(np);
  }

  /**
   * make sure that all results available have been parsed to allow another use
   * of the theorem-prover stream.
   */
  public void complete() {
    while (!done)
      parseNextOutput();
  }

  /**
   * is there a next theorem prover output?
   */
  public boolean hasNext() {

    if (parsedResults.isEmpty() && !done)
      parseNextOutput();
    return !parsedResults.isEmpty();

  }

  /**
   * get the next theorem prover output
   */
  public Object next() {
    Object result = parsedResults.remove(0);
    if (debug)
      Logger.println("TPOutput:" + result.toString());
    return result;
  }

  /**
   * this is unsupported for theorem prover output
   */
  public void remove() {
    throw new UnsupportedOperationException();
  }

  protected abstract void skipOutputHeader();

  protected abstract boolean isResultPrefix(char c);

  protected abstract TheoremProverResult parseResult();

  /**
   * parse next output and set done=true if result is depleated. parsed output
   * should be placed in the parsedResults collection.
   */
  protected void parseNextOutput() {
    TheoremProverResult tpr;
    skipOutputHeader();
    char nextChar = (char) from.peek();
    if (isResultPrefix(nextChar)) {
      tpr = parseResult();
      done = true;
    } else {
      StringBuffer fromContent = new StringBuffer();
      try {
        for (int i = 0, n = from.available(); i < n; i++)
          fromContent.append(from.readChar());
      } catch (IOException e) {
      }
      throw new RuntimeException("parseNextOutput(), parse error \n theorem prover returned: \n" + fromContent.toString()
          + "\n end of theorem prover result");
    }
    if (tpr != null) {
      parsedResults.add(tpr);
    }

  }

}
