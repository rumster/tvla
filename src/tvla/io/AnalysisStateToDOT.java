package tvla.io;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;

import tvla.transitionSystem.Location;

/** Converts a CFG with its structures and messages to DOT format.
 * @author Roman manevich.
 */
public class AnalysisStateToDOT extends AnalysisStateConverter {
	/** A convenience instance.
	 */
	public static final AnalysisStateToDOT defaultInstance = new AnalysisStateToDOT();

	/** Converts a collection of locations that contain structures to a DOT
	 * string representation.
	 * @param o A collection of locations.
	 */
	public String convert(Object o) {
		Collection locations = (Collection) o;
		StringBuffer result = new StringBuffer();
		
		for (Iterator i = locations.iterator(); i.hasNext(); ) {
			Location location = (Location) i.next();
			if (location.shouldPrint) {
				result.append( LocationToDOT.defaultInstance.convert(location) );
			} else if (location.messages.size() > 0) {
				result.append( LocationToDOT.defaultInstance.convertMessages(location) );
			}
		}
		
		return result.toString();
	}
	
	public void print(PrintStream result, Object o, String header) {
		Collection locations = (Collection) o;
		
		result.append(header);
		for (Iterator i = locations.iterator(); i.hasNext(); ) {
			Location location = (Location) i.next();
			if (location.shouldPrint) {
				result.append( LocationToDOT.defaultInstance.convert(location) );
			} else if (location.messages.size() > 0) {
				result.append( LocationToDOT.defaultInstance.convertMessages(location) );
			}
		}
	}
	
	/** This conversion filters the structures and leaves
	 * only the one with messages.
	 */
	public String convertMessagesOnly(Object o) {
		Collection locations = (Collection) o;
		StringBuffer result = new StringBuffer();
		
		for (Iterator i = locations.iterator(); i.hasNext(); ) {
			Location location = (Location) i.next();
			if (location.messages.size() > 0) {
				result.append( LocationToDOT.defaultInstance.convertMessages(location) );
			}
		}
		
		return result.toString();
	}
	
	/** This conversion filters the structures and leaves
	 * only the one without messages.
	 */
	public String convertWithoutMessages(Object o) {
		Collection locations = (Collection) o;
		StringBuffer result = new StringBuffer();
		
		for (Iterator i = locations.iterator(); i.hasNext(); ) {
			Location location = (Location) i.next();
			if (location.shouldPrint) {
				result.append( LocationToDOT.defaultInstance.convertStructures(location) );
			}
		}
		
		return result.toString();
	}	
}
