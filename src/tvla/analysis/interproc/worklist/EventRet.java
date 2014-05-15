/*
 * Created on 22/09/2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package tvla.analysis.interproc.worklist;

import tvla.analysis.interproc.transitionsystem.AbstractState.Fact;
import tvla.analysis.interproc.transitionsystem.method.MethodTS;



/**
 * An event that corresponds to a propogation of a structure over an
 * interprocedural *return* edge.
 * 
 * @author maon
 */
public final class EventRet extends Event {
	private Fact entryS;
	private Fact exitS;
	/**
	 * 
	 */
	EventRet(
			MethodTS mtd, 
			Fact  entryS,
			Fact  exitS) {
		super(mtd, mtd.getExitSite());
		this.entryS = entryS; 
		this.exitS = exitS;
}

	public int getType() {
		return RET;
	}
	
	public Fact  getEntryS() {
		return entryS;
	}
	
	public Fact  getExitS() {
		return exitS;
	}

}
