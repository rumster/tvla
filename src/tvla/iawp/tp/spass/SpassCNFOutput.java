package tvla.iawp.tp.spass;

import tvla.iawp.tp.NativeProcess;
import tvla.iawp.tp.TheoremProverResult;


/**
 * @author user
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SpassCNFOutput extends SpassOutput {

	private final static String spassClauseLine = "list_of_clauses(axioms, cnf).";
	private final static String spassEndOfList = "end_of_list.";

	public SpassCNFOutput(NativeProcess np, String query) {
		super(np);
		np.send(query);
	}


	protected void skipOutputHeader() {
		
		while (true) {
			from.skipWhitespaces();
			if (from.peek() == iError) {
				return;
			}
			String s = from.readLine();
			if (s == null)
				return;
			if (s.startsWith(spassClauseLine)) {
				break;
			}
		}
		from.skipWhitespaces();
	}	

	protected boolean isResultPrefix(char c) {
		return (c == 'c');
	}

	/**
	 * parse the next input as a result, having previously
	 * peeked to see that it should be a result description.
	 */
	protected TheoremProverResult parseResult() {
		
		
		StringBuffer result = new StringBuffer();
		String s = from.readLine();
		
		while (s != null)
		{
			if (s.startsWith(spassEndOfList)) 
			{
				s = null;
			} else {
				result.append(s);
				s = from.readLine();
			}
		}
		return new SpassCNFClause(result.toString());
	}

}
