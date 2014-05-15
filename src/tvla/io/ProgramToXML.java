package tvla.io;

import java.util.Collection;
import java.util.Iterator;

import tvla.transitionSystem.Action;
import tvla.transitionSystem.AnalysisGraph;
import tvla.transitionSystem.Location;
import tvla.util.ProgramProperties;


/** A class that converts a CFG to DOT format.
 * @author Roman manevich.
 */
public class ProgramToXML extends StringConverter {
	/** A convenience instance.
	 */
	public static ProgramToXML defaultInstance = new ProgramToXML();
	
	
	/** Converts a set of transitions to a TVS representation of the CFG.
	 */
	public String convert(Object o) {
		if (o instanceof AnalysisGraph) {
			AnalysisGraph cfg = (AnalysisGraph) o;
			return convertAnalysisGraph(cfg);
		}
				
		throw new InternalError("ProgramToXML.convert: Unknown ptogram type");
	}
	
	public String convertAnalysisGraph(AnalysisGraph cfg) 
	{	
		Collection locations = cfg.getLocations();
		String programName = ProgramProperties.getProperty("tvla.programName", "program");

		StringBuffer result = new StringBuffer(CommentToTVS.defaultInstance.convert(programName));

		result.append("<CFG>\n");
		
		
		// Print a graph node for every CFG node.
		for (Iterator locationIt = locations.iterator(); locationIt.hasNext(); ) {
			Location location = (Location)locationIt.next();
			result.append("<node id=\"" + location.label() + "\" />\n" );
		}
		
		// Print a graph edge for every CFG edge.
		for (Iterator locationIt = locations.iterator(); locationIt.hasNext(); ) {
			Location location = (Location)locationIt.next();
			for (int i = 0; i < location.getActions().size(); i++) {
				Action action = (Action)location.getActions().get(i);
				String target = (String)location.getTargets().get(i);
				result.append("<edge source=\"" + location.label() + "\"" + 
						      " target=\"" + target + "\" " + 
							  " label=\"" + action + "\"/>\n"); 
			}
		}
		result.append("</CFG>\n");
		return result.toString();
	}
}


