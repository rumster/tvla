/*
 * File: MethodTSNodeIntra.java 
 * Created on: 14/10/2004
 */

package tvla.analysis.interproc.transitionsystem.program;

import tvla.analysis.interproc.semantics.ActionInstance;
import tvla.analysis.interproc.transitionsystem.method.MethodTS;
import tvla.analysis.interproc.transitionsystem.method.TSNode;

/** 
 * @author maon
 */
public class CallSiteVirtual extends CallSite {
	private ActionInstance guard;
	public CallSiteVirtual(
			MethodTS caller, 
			TSNode callSite, 
			MethodTS callee,
			ActionInstance call,
			ActionInstance ret,
			ActionInstance guard){
		super(caller, callSite, callee, call, ret);
		this.guard = guard;
	}
	
	public int getType() {
		return VIRTUAL;
	}
	
	public ActionInstance getGuardAction() {
		return guard;
	}
	
}
