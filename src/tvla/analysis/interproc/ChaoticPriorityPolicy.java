/*
 * File: ChaoticPriorityPolicy.java 
 * Created on: 24/10/2004
 */

package tvla.analysis.interproc;

import tvla.analysis.interproc.transitionsystem.ProgramTS;
import tvla.analysis.interproc.worklist.Event;
import tvla.analysis.interproc.worklist.EventTransition;
import tvla.analysis.interproc.worklist.Worklist;

/** Allocates priorities to various events
 * @author maon
 */
public class ChaoticPriorityPolicy {
//	private final ProgramTS progTS;
	private static final Worklist.Priority theInstance = new FixedPriority();
	/**
	 * 
	 */
	public ChaoticPriorityPolicy(ProgramTS progTS) {
//		this.progTS = progTS;
	}
	
	public Worklist.Priority calcPriority(Event e) {
		if (e instanceof EventTransition)
			return null;
		else
			return theInstance;
	}

	
	private static class FixedPriority implements Worklist.Priority {
		public int hashCode() {
			return 0;
		}
		
		public boolean equals(Object other) {
			if (other == null)
				return false;
			
			if (! (other instanceof FixedPriority))
				return false;
			
			return true;
		}
		
		public int compareTo(Object to) {
			if (to == null)
				throw new NullPointerException();
			
			if (! (to instanceof FixedPriority))
				throw new InternalError("FixedPRiority is cmpared to a non FixedPriority object");
				
			return 0;
		}
	}
}
