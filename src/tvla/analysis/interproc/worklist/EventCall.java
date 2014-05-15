/*
 * Created on 22/09/2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package tvla.analysis.interproc.worklist;

import tvla.analysis.interproc.transitionsystem.AbstractState.Fact;
import tvla.analysis.interproc.transitionsystem.method.CFGNode;
import tvla.analysis.interproc.transitionsystem.method.MethodTS;
import tvla.analysis.interproc.transitionsystem.method.TSNode;


/**
 * An event that corresponds to a propogation of a structure over an
 * interprocedural *call* edge.
 * 
 * @author maon
 */
public abstract class EventCall extends Event {
	private Fact ctx;
	private MethodTS callee;

	protected EventCall(
			MethodTS mtd,
			TSNode site, 
			Fact ctx, 
			MethodTS callee) {
		super(mtd,site);
		this.ctx = ctx;
		this.callee = callee;
	}

	public CFGNode getCallSite() {
		return site;
	}
	
	public Fact getCTX() {
		return ctx;
	}
	
	public MethodTS getCallee() {
		return callee;	
	}
	
	public String toString() {
		return (super.toString() +
				" at " + site.getLabel());
	}
}
