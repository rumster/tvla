package tvla.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import tvla.exceptions.TVLAException;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.ProgramProperties;
import tvla.util.StringUtils;

/** A helper class, used to retrieve predicate display properties
 * and update the corresponding predicates.
 * @author Roman Manevich.
 * @since tvla-2-alpha, 13.4.2002, Initial creation.
 */
public class DOTDisplayProperties {
	/** A collection of DisplayProperty objects specified by the user.
	 */
	protected Collection properties = new ArrayList();

	/** The name of the property used to pass the display properties.
	 */
	protected static final String propertyName = "tvla.predicateDisplay";

	/** The one and only instance of this class.
	 */
	private static DOTDisplayProperties theInstance;
	
	/** Use this operation to retrieve the single instance
	 * of this class.
	 */
	public static DOTDisplayProperties instance() {
		if (theInstance == null)
			theInstance = new DOTDisplayProperties();
		return theInstance;
	}
	
	/** Reads the display properties and assigns them to each predicate.
	 */
	public void initDisplayProperties() {
		String displayProperties = ProgramProperties.getProperty(propertyName, "");
		properties = new DisplayPropertiesParser(displayProperties).parse();

		// Updating the display properties for each entry specified.
		for (Iterator propIter = properties.iterator(); propIter.hasNext(); ) {
			DisplayProperty prop = (DisplayProperty) propIter.next();

			// Check whether the identifiers refers to a predicate.
			Predicate predicate = Vocabulary.getPredicateByName(prop.id);
			if (predicate != null) {
				assignPropertiesToPredicate(predicate, prop);
				continue;
			}
			
			// Check whether the identifier refers to a set of predicates.
			List predicates = (List) tvla.language.TVP.SetDefAST.allSets.get(prop.id);
			if (predicates != null) {
				// Update the display properties for each predicate in the set.
				for (Iterator predIter = predicates.iterator(); predIter.hasNext(); ) {
					String predicateName = (String) predIter.next();
					predicate = Vocabulary.getPredicateByName(predicateName);
					if (predicate != null)
						assignPropertiesToPredicate(predicate, prop);
					else {
						System.err.println("Unable to find a predicate by the name " + 
										   predicateName + " in the set " + prop.id + 
										   " specified for the property " + 
										   propertyName);
					}
				}
			}
			else {
				System.err.println("Unable to find a set or a predicate by the name " + 
								   prop.id + " specified in property " + 
								   propertyName);
			}
		}
	}

	/** Assigns display properties to the Predicate object.
	 */
	protected void assignPropertiesToPredicate(Predicate p, DisplayProperty prop) {
		p.setShowAttr(prop.pointer);
		p.setShowAttr(prop.displayTrue, 
					  prop.displayUnknown,
					  prop.displayFalse);
	}
	
	/** Singleton pattern.
	 */
	protected DOTDisplayProperties() {
	}
	
	/** A class storing the display properties of a predicate
	 * or a set of predicates.
	 */
	public static class DisplayProperty {
		/** The identifying string of the predicate or set.
		 */
		public String id;
		
		/** Specifies whether false values of a predicate should be displayed.
		 */
		public boolean displayFalse;
		
		/** Specifies whether true values of a predicate should be displayed.
		 */
		public boolean displayTrue;
		
		/** Specifies whether unknown values of a predicate should be displayed.
		 */
		public boolean displayUnknown;

		/** Specifies whether a predicate should be displayed as a pointer.
		 */
		public boolean pointer;
	}
	
	/** A customized parser for parsing the value of the display property.
	 */
	public static class DisplayPropertiesParser {
		/** The working input.
		 */
		protected String str;
		
		/** A collection of DisplayProperty objects.
		 */
		protected Collection answer = new ArrayList();
		
		/** Constructs and initializes the parser with
		 * an input string.
		 */
		public DisplayPropertiesParser(String str) {
			this.str = str;
		}
		
		/** Starts the parser, which returns a collection of properties.
		 * @return A collection of DisplayProperty objects found in the
		 * input string.
		 */
		public Collection parse() {
			str = normalizeString(str);
			while (str.length() > 0) {
				int oldLength = str.length();
				
				DisplayProperty prop = parseNextPair();
				answer.add(prop);
				
				if (str.length() == oldLength) // prevent infinite loops
					break;
			}
			return answer;
		}
		
		/** Removes commas and extra spaces from a string.
		 */
		protected String normalizeString(String in) {
			boolean change = false;
			do {
				change = false;
				
				String oldString = in;

				// remove commas
				in = StringUtils.replace(in, ",", " ");

				// remove redundant spaces
				in = StringUtils.replace(in, "  ", " ");
				
				change |= !oldString.equals(in);
			}
				while (change);
			return in;
		}
		
		/** Finds the next pair of id and its display properties,
		 * parses it into a DisplayProperty removes the part of the
		 * string that was parsed from the working string.
		 */
		protected DisplayProperty parseNextPair() {
			DisplayProperty property = new DisplayProperty();
			
			int indexOfColon = str.indexOf(":");
			if (indexOfColon < 0)
				throw new SyntaxError("Missing :");
			
			int indexOfLeftBrace = str.indexOf("{");
			if (indexOfLeftBrace < 0)
				throw new SyntaxError("Missing {");
			
			int indexOfRightBrace = str.indexOf("}");
			if (indexOfRightBrace < 0)
				throw new SyntaxError("Missing }");
			
			if (!(indexOfRightBrace > indexOfLeftBrace && indexOfLeftBrace > indexOfColon))
				throw new SyntaxError("");
			
			property.id = str.substring(0, indexOfColon);
			String propertyList = str.substring(indexOfLeftBrace+1, indexOfRightBrace);
			parsePropertyList(propertyList, property);
			
			if (str.length() - indexOfRightBrace > 4)
				str = str.substring(indexOfRightBrace+2, str.length());
			else
				str = "";
			
			return property;
		}
		
		/** Fills the property object with the values found in the
		 * string storing the properties list.
		 */
		protected void parsePropertyList(String list, DisplayProperty property) {
			if (list.equals(""))
				return;
			
			if (list.indexOf("pointer") >=0)
				property.pointer = true;
			
			if (list.indexOf("0") >=0)
				property.displayFalse = true;
			
			if (list.indexOf("1/2") >=0)
				property.displayUnknown = true;

			// find whether the list includes 1, without confusing it with 1/2
			int indexOf1 = list.indexOf("1");
			if (list.charAt(list.length()-1) == '1') // last character - definitely 1
				property.displayTrue = true;
			if (list.indexOf("1 ") >= 0)
				property.displayTrue = true;
		}
	}

	/** A class represeting a syntax error in the value of the display properties.
	 */
	protected static class SyntaxError extends TVLAException {
		public SyntaxError(String message) {
			super("Syntax error in value of property " + propertyName + "!" +
				  message);
		}
	}
}