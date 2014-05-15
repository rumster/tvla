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
 * intraprocedural edge in the MethodTS (i.e., an intraprocedural statement).
 * 
 * @author maon
 */
public final class EventIntra extends Event {
	private Fact fact;
	
	/**
	 * 
	 */
	EventIntra(MethodTS mtd, TSNode site, Fact fact) {
		super(mtd, site);
		this.fact = fact;
	}

	public int getType() {
		return INTRA;
	}
	
	public TSNode getSite() {
		return site;
	}
	
	public Fact getUnprocessedFact() {
		return fact;
	}
	
	public String toString() {
		return (super.toString() +
				" at " + site.getLabel());
	}
}
