/*
 * Created on 22/09/2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package tvla.analysis.interproc.worklist;

import java.util.Collection;

import tvla.analysis.interproc.transitionsystem.AbstractState.Fact;
import tvla.analysis.interproc.transitionsystem.method.MethodTS;
import tvla.analysis.interproc.transitionsystem.method.TSNode;


/**
 * An event that corresponds to a propogation of a structure over an
 * interprocedural *call* edge.
 * 
 * @author maon
 */
public final class EventVirtualCall extends EventCall {
	private final Collection refinedCTXs;
	/**
	 * 
	 */
	EventVirtualCall(
			MethodTS mtd,
			TSNode site, 
			Fact  ctx,
			Collection refinedCTXs,
			MethodTS callee) {
		super(mtd,site,ctx,callee);
		
		assert(refinedCTXs != null);
		assert(!refinedCTXs.isEmpty());
		
		this.refinedCTXs=refinedCTXs;
	}

	public int getType() {
		return VIRTUAL_CALL;
	}

	public Collection getRefinedCTXs() {
		return refinedCTXs;
	}

}
