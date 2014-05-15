package tvla.iawp.tp.spass;

import tvla.iawp.tp.TheoremProverResult;

/**
 * @author user
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SpassCNFClause implements TheoremProverResult {

	private String clause;
	
	public SpassCNFClause(String s)
	{
		clause = s;
	}
	
	public String toString()
	{
		return clause;
	}
}
