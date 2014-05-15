package tvla.iawp.tp.spass;

import tvla.iawp.tp.NativeProcess;
import tvla.iawp.tp.TheoremProverOutputNoPushback;
import tvla.iawp.tp.TheoremProverResult;
import tvla.iawp.tp.TheoremProverValueResult;

/**
 * @author Eran Yahav (yahave)
 */
public class SpassOutput extends TheoremProverOutputNoPushback {

	/**
	 * @param np
	 */
	public SpassOutput(NativeProcess np) {
		super(np);
		// TODO Auto-generated constructor stub
	}


	private final static int iValid = 80;   // P
	private final static int iInvalid = 67; // C
	protected final static int iError = 'L';   // L
	private final static char cValid = 'P';
	private final static char cInvalid = 'C';
	private final static String spassVersionLine = "SPASS V 2.0g";
	private final static String spassResultLine = "SPASS beiseite: ";
	private final static String validString = "Proof found";
	private final static String invalidString = "Completion found.";

	public SpassOutput(NativeProcess np, String query) {
		super(np);
		np.send(query);
	}


	protected void skipOutputHeader() {
		from.skipWhitespaces();
		from.skipString(spassVersionLine);
		from.skipWhitespaces();
		from.skipString(spassResultLine);
	}	

	protected boolean isResultPrefix(char c) {
		return (c == cValid) || (c == cInvalid);
	}

	/**
	 * parse the next input as a result, having previously
	 * peeked to see that it should be a result description.
	 */
	protected TheoremProverResult parseResult() {
		TheoremProverResult result;
		switch (from.peek()) {
			case iValid : // 'Proof found'
				from.skipString(validString);
				result = TheoremProverValueResult.VALID;
				break;

			case iInvalid : // 'Completion found'
				from.skipString(invalidString);
				result = TheoremProverValueResult.INVALID;
				break;

			default :
				return TheoremProverValueResult.UNKNOWN;

		}
		from.skipWhitespaces();
		return result;
	}

}
