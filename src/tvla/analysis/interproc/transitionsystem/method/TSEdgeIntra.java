/*
 * File: TSEdgeIntra.java 
 * Created on: 22/09/2004
 */

package tvla.analysis.interproc.transitionsystem.method;

import tvla.analysis.interproc.semantics.ActionInstance;

/** An edge corresponding to an intraprocedural *atomic* statement 
 * (e.g., assignments, evaluation of a conditional) in a specific MethodTS.
 * 
 * @author maon
 */
public class TSEdgeIntra extends TSEdge implements CFGEdgeIntra {
	private final ActionInstance action;
	/**
	 * 
	 */
	public TSEdgeIntra(
			TSNode src, TSNode dst,
			long edgeId,
			ActionInstance action){
		super(src, dst, edgeId); //, transition);
		this.action = action; 
	}
	
	public ActionInstance getActionInstance() {
		return action;
	}
	
	public String getLabel() {
		return  action.getTitle();
	}
	
	public String getActionId() {
		return action.getMacroName(true);
	}
	
	public String title() {
		return printTitle ? getLabel() : getActionId();
	}
}
