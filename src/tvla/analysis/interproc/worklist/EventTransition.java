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
 * interprocedural *return* edge.
 * 
 * @author maon
 */
public final class EventTransition extends Event {
	private final Fact  fromFact;
	private final TSNode toNode;
	private final Fact toFact;
	/**
	 * 
	 */
	EventTransition(
			MethodTS mtd, 
			TSNode fromNode,
			Fact  fromFact,
			TSNode toNode,
			Fact  toFact) {
		super(mtd, fromNode);
		this.fromFact = fromFact; 
		this.toNode = toNode;
		this.toFact = toFact;
}

	public int getType() {
		return TRANSITION;
	}
	
	public Fact  getFromFact() {
		return fromFact;
	}
	
	public TSNode  getToNode() {
		return toNode;
	}

	public Fact  getToFact() {
		return toFact;
	}
	

}
