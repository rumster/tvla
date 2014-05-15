package tvla.iawp.tp;

import tvla.core.HighLevelTVS;

/**
 * @author user
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class TheoremProverModelResult implements TheoremProverResult {
	
	protected HighLevelTVS model;
	protected boolean isError;
	protected String errMessage;
	
	
	public HighLevelTVS getModel()
	{
		return model;
	}
	
	public String getMessage()
	{
		return errMessage;
	}
	
	public TheoremProverModelResult()
	{
		this.model = null;
		this.isError = true;
		this.errMessage = null;
	}
	
	public TheoremProverModelResult(HighLevelTVS model)
	{
		this.model = model;
		this.isError = false;
		this.errMessage = null;	
	}
	
	public TheoremProverModelResult(String errMessage)
	{
		this.model = null;
		this.errMessage = errMessage;
		this.isError = true;
	}
}
