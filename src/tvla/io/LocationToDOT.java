package tvla.io;

import java.util.Iterator;
import java.util.Map;

import tvla.core.TVS;
import tvla.transitionSystem.PrintableProgramLocation;
import tvla.util.StringUtils;

/** Converts a location with its structures and messages to DOT format.
 * @author Roman manevich.
 */
public class LocationToDOT extends LocationConverter {
	/** A convenience instance.
	 */
	public static final LocationToDOT defaultInstance = new LocationToDOT();
	
	public LocationToDOT() {
	}
	
	public String convert(Object o) {
		PrintableProgramLocation location = (PrintableProgramLocation) o;
		StringBuffer result = new StringBuffer();

		result.append(locationHeader(location.label()));		
		result.append(convertMessages(location));
		result.append(convertStructures(location));
		
		return result.toString();
	}
	
	String locationHeader(String locationLabel) {
		StringBuffer result = new StringBuffer();

		result.append(CommentToDOT.getPageComment());
		String locationProlog = "Program Location\n" + locationLabel;
		result.append(DOTMessage.defaultInstance.convert(locationProlog) + StringUtils.newLine);
		return result.toString();
	}
	
	String locationFooter(String locationLabel) {
		return "";
	}
	
	
	/** Converts the structures in the specified location excluding messages.
	 */
	String convertStructures(PrintableProgramLocation location) {
		StringBuffer result = new StringBuffer();

		Iterator res = location.getStructuresIterator();
		while (res.hasNext()) {
			TVS structure = (TVS) res.next();
			String structureInDOT = StructureToDOT.defaultInstance.convert(structure);
			result.append(structureInDOT + StringUtils.newLine);
		}
		
		return result.toString();
	}

	/** Converts only the structures with associated messages for the 
	 * specified location.
	 */
	String convertMessages(PrintableProgramLocation location) {
		StringBuffer result = new StringBuffer();

		if (!location.getMessages().isEmpty()) {
			String messagesProlog = "Messages for\n" + location.label();
			result.append(CommentToDOT.getPageComment());
			result.append(DOTMessage.defaultInstance.convert(messagesProlog));
			for (Iterator it = location.getMessages().entrySet().iterator(); it.hasNext(); ) {
				Map.Entry report = (Map.Entry)it.next();
				TVS structure = (TVS) report.getKey();
//					StringBuffer message = (StringBuffer)report.getValue();
//					String structureWithMessage = StructureToDOT.defaultInstance.convert(structure, message.toString());
					String messageStr  = report.getValue().toString();
					String structureWithMessage = StructureToDOT.defaultInstance.convert(structure, messageStr);
					result.append(structureWithMessage + "\n");
			}
		}	    
		
		return result.toString();
	}
}