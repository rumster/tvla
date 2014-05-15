/*
 * File: PrintableProgramLocation.java 
 * Created on: 18/10/2004
 */

package tvla.transitionSystem;

import java.util.Iterator;
import java.util.Map;

/** 
 * @author maon
 */
public interface PrintableProgramLocation {
	/** Returns the label of this location, as specified in the TVP.
	 */
	public abstract String label();
	
	public boolean setShouldPrint(boolean val);
	
	public boolean getShouldPrint();
	
	public Iterator getStructuresIterator();
	
	public Map getMessages();
}