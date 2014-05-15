/*
 * File: Worklist.java 
 * Created on: 15/10/2004
 */

package tvla.analysis.interproc.worklist;

/** The interface exposed by Worklist.
 * Provides the facility to add events with an associated 
 * priority.
 * The class implmentation may ignore the priorities.
 * In this case, the invoking ignoresPriorities() returns true.
 * Otherwise, i.e., priorities are not ignored, ignoresPriorities() 
 * returns false. In this case, Higher priority events are handled 
 * before lower priority events.
 * Priority order is defined according to the Compareable interface.
 * In case several events have the same priority, the order in which
 * they are porcessed is not defined.
 * 
 * @author maon
 */
public interface Worklist {
	/**
	 * Adds an event with default priority (highest).
	 * @param listener handler of the event 
	 * @param event
	 */
	public void addEvent(Event  event);

	/**
	 * Adds an event with a given priority.
	 * Note that if a priority is specified it is always
	 * lower than the default priority.
	 * Null is thge lowest priority of all.
	 * 
	 * @param listener handler of the event 
	 * @param event
	 * @param priority the priority of the event
	 */
	public void addEvent(Event  event,  Priority priority);
	
	/** Is the worklist empty? 
	 * @return whether there are events in the queue.
	 */
	public boolean hasEvent();
	
	/** Does the worklist ignores priorities?
	 * @return
	 */
	public boolean ignoresPriorites(); 

	/** Does the worklist respect lowest (null) /mid / highest priorities ?
	 * @return
	 */
	public boolean repsectLowAndHighPriorites(); 

	/** 
	 * Extracts the highest priority event from the queue.
	 * If there are several "highest priority" objects, one is 
	 * arbitrarily chosen. 
	 * @return the extracted evetnt, or null if there is no such event.
	 */
	public Event  extractEvent() ;
	
	public interface Priority extends Comparable {
	}
}
