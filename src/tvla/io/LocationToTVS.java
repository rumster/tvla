package tvla.io;

import java.util.Iterator;
import java.util.Map;

import tvla.core.TVS;
import tvla.transitionSystem.PrintableProgramLocation;
import tvla.util.StringUtils;

/** Converts a location with its structures and messages to TVS format.
 * @author Roman manevich.
 */
public class LocationToTVS extends LocationConverter {
	public static LocationToTVS defaultInstance = new LocationToTVS();

	public LocationToTVS() {
	}
	
	public String convert(Object o) {
		PrintableProgramLocation location = (PrintableProgramLocation) o;
		
		StringBuffer result = new StringBuffer();

		result.append(locationHeader(location.label()));
		result.append(convertMessages(location));
		result.append(convertStructures(location));
		result.append(locationFooter(location.label()));
		
		return result.toString();
	}
	
	String locationHeader(String locationLabel) {
		return "%location " + labelToString(locationLabel) + " = {\n";
	}
	
	String locationFooter(String locationLabel) {
		return "}";
	}

	
	
	/** Converts the structures in the specified location excluding messages.
	 */
	String convertStructures(PrintableProgramLocation location) {
		StringBuffer result = new StringBuffer();
		Iterator res = location.getStructuresIterator();
		while (res.hasNext()) {
			TVS structure = (TVS) res.next();
			String structureInTVS = StructureToTVS.defaultInstance.convert(structure);
			result.append(structureInTVS + "\n");
			if (res.hasNext())
				result.append("\n");				
		}			
		return result.toString();
	}
	
	/** Converts only the structures with associated messages for the 
	 * specified location.
	 */
	String convertMessages(PrintableProgramLocation location) {
		StringBuffer result = new StringBuffer();
		for (Iterator it = location.getMessages().entrySet().iterator(); it.hasNext(); ) {
			Map.Entry report = (Map.Entry) it.next();
			TVS structure = (TVS) report.getKey();
//			String message = ((StringBuffer) report.getValue()).toString();
			String message = report.getValue().toString();
			message = StringUtils.replace(message, "\\n", " ");
			result.append("  %message \"" + message + "\"\n");
			result.append(StructureToTVS.defaultInstance.convert(structure) + "\n");
		}
		return result.toString();
	}
	
}