/*
 * Created on Oct 1, 2003
 */
package tvla.analysis.relevance;

import java.util.StringTokenizer;

/**
 * Represnts a refinement message containing information 
 * to be used to refine the abstraction. 
 * @author Eran Yahav eyahav
 */
public class RefinementMessage {

  	/** debug flag **/
	private static final boolean DEBUG = true;

	/** assume a fixed structure of the refinement message reported by analysis.
	 * We currently assume that the message begins with a fixed prefix.
	 */ 
	private static final String REFINEMENT_MESSAGE_PREFIX = "Refinement:";
	/**
	 * refinement message - variable tag
	 */
	private static final String VAR_TAG = "var";
	/**
	 * refinement message - type tag
	 */
	private static final String TYPE_TAG = "type";
	/**
	 * refinment message - label tag
	 */
	private static final String LABEL_TAG = "label";

	/**
	 * variable to be refined
	 */
	private String var;
	/**
	 * type of object to be refined
	 */
	private String type;
	/**
	 * label in which precision violation occurred
	 */
	private String label;

	/**
	 * returns a new RefinmentMessage if the provided tvlaMessage
	 * is an actual refinement message. Otherwise returns null.
	 * @param tvlaMessage
	 * @return a RefinementMessage constructed from a TVLA text message
	 */
	public static RefinementMessage getRefinementMessage(String tvlaMessage) {
		if (isRefinementMessage(tvlaMessage))
			return new RefinementMessage(tvlaMessage);
		else
			return null;
	}

	/**
	 * checks whether the provided tvla message is a refinement message.
	 * @param tvlaMessage
	 * @return true if the message is a refinement message, false otherwise
	 */
	public static boolean isRefinementMessage(String tvlaMessage) {
		return tvlaMessage.startsWith(REFINEMENT_MESSAGE_PREFIX);
	}

	/**
	 * parses a TVLA message and constructs a new 
	 * refinement message.
	 * @param tvlaMessage
	 */
	private RefinementMessage(String tvlaMessage) {
		if (DEBUG)
			System.out.println("RefinementMessage " + tvlaMessage);

		StringTokenizer st = new StringTokenizer(tvlaMessage);
		while (st.hasMoreTokens()) {
			String currToken = st.nextToken();
			if (currToken.equals(VAR_TAG)) {
				String dummy = st.nextToken();
				this.var = st.nextToken();
			} else if (currToken.equals(TYPE_TAG)) {
				String dummy = st.nextToken();
				this.type = st.nextToken();
			} else if (currToken.equals(LABEL_TAG)) {
				String dummy = st.nextToken();
				this.label = st.nextToken();
			}
		}

		if (DEBUG)
			System.out.println(
				"Got var=" + var + " type=" + type + " label=" + label);

	}

	/**
	 * @return the message's variable
	 */
	public String var() {
	  return var;
	}
	
}
