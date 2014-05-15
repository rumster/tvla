package tvla.analysis.relevance;

/**
 * Models a user query
 * 
 * @author Ran Shaham, Eran Yahav
 * @since Feb 5, 2003
 */
public class RelevantLocationQuery {

  /**
   * identifies an irrelevant location kind
   */
  public static final int NONE = 0;
  /**
   * identifies a single-object relevant location kind
   */
  public static final int SINGLE_OBJECT = 1;
  /**
   * identifies a multi-object relevant location kind
   */
  public static final int MULTI_OBJECT = 2;

  /**
   * debug mode?
   */
  private static final boolean DEBUG = false;

  /**
   * location string
   */
  private String locationStr;
  /**
   * location value
   */
  private String locationVal;
  /**
   * kind of location
   */
  private int kind;

  /**
   * is the given location a relevant location
   * @param label - label of location to be checked for releavnce
   * @return the kind of location - irrelevant, single-object relevance, or multi-object relevance
   */
  public int inRelevantLocation(String label) {
    int result = RelevantLocationQuery.NONE;

    if (DEBUG)
      System.out.println("InRelevantLocation: " + label);

    if (label.indexOf(locationStr) >= 0)
      result = kind;
    return result;
  }

  /**
   * creates a new relevant location from a query string and a value string.
   * The query strings identifies the location (label), and the value string
   * identifies the kind of location.
   */
  public RelevantLocationQuery(String q, String val) {
    locationStr = q;
    locationVal = val;
    if (val.equals("single"))
      kind = RelevantLocationQuery.SINGLE_OBJECT;
    else
      kind = RelevantLocationQuery.MULTI_OBJECT;
  }
}