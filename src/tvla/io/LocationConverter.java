package tvla.io;

import tvla.transitionSystem.PrintableProgramLocation;


/** Converts a location with its structures and messages to TVS format.
 * @author Roman manevich.
 */
public abstract class LocationConverter extends StringConverter {
	/**
	 * Converts a location (strucutres and messages) 
	 * Adds location header and footer.
	 */
	public abstract String convert(Object o);

	
	/** Converts the structures in the specified location excluding messages.
	 * Does *not* add location header and footer
	 */
	abstract String convertStructures(PrintableProgramLocation location);
	
	/** Converts only the structures with associated messages for the 
	 * specified location.
	 * Does *not* add location header and footer
	 */
	abstract String convertMessages(PrintableProgramLocation location);
	
	
	/** Returns a header for the given location
	 * @param label the location's label
	 */
	abstract String locationHeader(String locationLabel);
	
	/** Returns a footer for the given label
	 * @param label the location's label
	 */
	abstract String locationFooter(String locationLabel);

	/** Puts the label in quotes if needed (if it does not make 
	 * a legal TVLA identifier).
	 */
	String labelToString(String label) {
		if (label.indexOf(' ') >= 0
			|| label.indexOf("(") >= 0
			|| label.indexOf(")") >= 0) {
			return "\"" + label + "\"";
		}
		else {
			return label;
		}
	}
}