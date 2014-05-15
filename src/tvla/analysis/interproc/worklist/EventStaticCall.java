/*
 * Created on 22/09/2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package tvla.analysis.interproc.worklist;

import tvla.analysis.interproc.transitionsystem.AbstractState.Fact;
import tvla.analysis.interproc.transitionsystem.method.MethodTS;
import tvla.analysis.interproc.transitionsystem.method.TSNode;


/**
 * An event that corresponds to a propogation of a structure over an
 * interprocedural *call* edge.
 * 
 * @author maon
 */
public final class EventStaticCall extends EventCall {
	/**
	 * 
	 */
	EventStaticCall(
			MethodTS mtd,
			TSNode site, 
			Fact ctx, 
			MethodTS callee) {
		super(mtd,site,ctx,callee);
	}

	public int getType() {
		return STATIC_CALL;
	}
}
