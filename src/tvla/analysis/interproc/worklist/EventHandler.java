/*
 * File: EventHandler.java 
 * Created on: 15/10/2004
 */

package tvla.analysis.interproc.worklist;

/** An interface that should be implemented by the event listeners. 
 * @author maon
 */
public interface EventHandler {
	public void handleEvent(Object event);
}
