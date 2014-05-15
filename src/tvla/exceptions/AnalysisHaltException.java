package tvla.exceptions;

import tvla.transitionSystem.Action;

/** An exception used to terminate the execution of the analysis when
 * a user-defined condition is met.
 * @author Eran Yahav
 * @since tvla-0.91
 */
public class AnalysisHaltException extends TVLAException {
	protected Action theAction;
	protected String theLabel;
	
	public AnalysisHaltException(String label, Action act) {
		super("");
		theAction = act;
		theLabel = label;
	}
	
	public Action getAction() {
		return theAction;
	}

	public String getLabel() {
		return theLabel;
	}
	
	public String getMessage() {
		if (theAction.isHalting())
			return "Analysis stopped at " + theLabel 
				   + " " + theAction 				
				   + " when evaluating " + theAction.haltCondition();
		else
			return "Analysis stopped at " 
				   + theLabel + " " + theAction + " at user request";
	}
}