/*
 * File: TSNode.java 
 * Created on: 22/09/2004
 */

package tvla.analysis.interproc.transitionsystem.program;

import tvla.analysis.interproc.semantics.ActionInstance;
import tvla.analysis.interproc.transitionsystem.method.CFGNode;
import tvla.analysis.interproc.transitionsystem.method.MethodTS;
import tvla.analysis.interproc.transitionsystem.method.TSNode;


/** A MethodTS Node in a specific MethodTS
 *  
 * @author maon
 */
public abstract class CallSite{
	public static final int STATIC = 0; 
	public static final int VIRTUAL = 1; 
	public static final int CONSTRUCTOR = 2; 

	protected MethodTS caller;
	protected TSNode callSite;
	protected MethodTS callee;
	protected ActionInstance call;
	protected ActionInstance ret;


	/**
	 * 
	 */
	protected CallSite(
			MethodTS caller, 
			TSNode callSite, 
			MethodTS callee,
			ActionInstance call,
			ActionInstance ret) {
		super();
		
		this.caller = caller;
		this.callSite = callSite;
		this.callee = callee;
		this.call = call;
		this.ret = ret;
	}
	
	public static CallSite newInterProcTSNode(
			int nodeType, 
			MethodTS caller, 
			TSNode callSite,  
			MethodTS callee,
			ActionInstance call,
			ActionInstance ret,
			ActionInstance guard) {
		switch(nodeType) {
			case STATIC:
				assert(guard == null);
				return new CallSiteStatic(
						caller, 
						callSite, 
						callee,
						call,
						ret);
			case VIRTUAL:
				return new CallSiteVirtual(
						caller, 
						callSite, 
						callee,
						call,
						ret,
						guard);
			case CONSTRUCTOR:
				return new CallSiteConstructor(
						caller, 
						callSite, 
						callee,
						call,
						ret);
			default: 
				throw new InternalError("newMInterProcTSNode: Unknown node type " + nodeType);
		}

	}

	public abstract int getType();
	
	public MethodTS getCallee() {
		return callee;
	}
	
	public MethodTS getCaller() {
		return caller;
	}

	public CFGNode getCallSite() {
		return callSite;
	}
	
	public ActionInstance getCallAction() {
		return call;
	}
	
	public ActionInstance getRetAction() {
		return ret;
	}
}
