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
public class CallSiteStatic extends CallSite {
	public CallSiteStatic(
			MethodTS caller, 
			TSNode callSite, 
			MethodTS callee,
			ActionInstance call,
			ActionInstance ret){
		
		super(caller, callSite, callee, call, ret);
	}
	
	public int getType() {
		return STATIC;
	}
}
