package tvla.io;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;

import tvla.transitionSystem.Location;

/** Converts a CFG with its structures and messages to DOT format.
 * @author Roman manevich.
 */
public class AnalysisStateToTVS extends AnalysisStateConverter {
	/** A convenience instance.
	 */
	public static final AnalysisStateToTVS defaultInstance = new AnalysisStateToTVS();

	/** Converts a collection of locations that contain structures to a TVS
	 * string representation.
	 * @param o A collection of locations.
	 */
	public String convert(Object o) {
		Collection locations = (Collection) o;
		StringBuffer result = new StringBuffer();
		
		for (Iterator i = locations.iterator(); i.hasNext(); ) {
			Location location = (Location) i.next();
			if (location.shouldPrint) {
				result.append( LocationToTVS.defaultInstance.convert(location) );
				if (i.hasNext())
					result.append("\n\n");
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
				result.append( LocationToTVS.defaultInstance.convert(location) );
				if (i.hasNext())
					result.append("\n\n");
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
				result.append( LocationToTVS.defaultInstance.convertMessages(location) );
			}
		}
		
		return result.toString();
	}
	
	/** This conversion filters the structures and leaves
	 * only the one without messages.
	 */
	public String convertWithoutMessages(Object o) {
		CommentToDOT.pageCounter = 1;
		Collection locations = (Collection) o;
		StringBuffer result = new StringBuffer();
		
		for (Iterator i = locations.iterator(); i.hasNext(); ) {
			Location location = (Location) i.next();
			if (location.shouldPrint) {
				result.append( LocationToTVS.defaultInstance.convertStructures(location) );
			}
		}
		
		return result.toString();
	}	
}