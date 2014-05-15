package tvla.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import tvla.exceptions.UserErrorException;

/** This class extends the standard Properties class by supplying facilities
 * for getting and setting properties with different types of values.
 * @author Roman Manevich
 * @since tvla-2-alpha Initial creation.
 */
public class PropertiesEx extends Properties {
	/** Creates an empty property list.
	 */
	public PropertiesEx() {
		super();
	}

	/** Constructs and initializes a PropertiesEx from a specified
	 * resource (such as a file). This is useful for retrieving
	 * properties from files inside an archived application.
	 */
	public PropertiesEx(String resourceFileName) {
		super();
		InputStream in = PropertiesEx.class.getResourceAsStream(resourceFileName);
		if (in == null)
			System.err.println("Resource " + resourceFileName + " is missing!");
		else {

			try {
				super.load(in);
			}
			catch (IOException e) {
				throw new RuntimeException("Resource " + resourceFileName + " is missing!");
			}
		}
	}
	
	/** Creates an empty property list with the specified defaults.	
	 */
	public PropertiesEx(Properties defaults) {
		super(defaults);
	}
	
	/** Returns the boolean value of a property or the default
	 * value in case there's no association or the specified
	 * value could not be parsed as a boolean.
	 */
	public boolean getBooleanProperty(String key, boolean defaultValue) {
		boolean result = defaultValue;
		
		String value = getProperty(key);
		if (value != null) {
			if (value.equals("true"))
				result = true;
			else if (value.equals("false"))
				result = false;
			else {
				String message = "Unable to parse property " + key + " - invalid format!";
				throw new RuntimeException(message);
			}
		}
		
		return result;
	}

	/** Returns the integer value of a property or the default
	 * value in case there's no association or the specified
	 * value could not be parsed as an integer.
	 */
	public int getIntProperty(String key, int defaultValue) {
		int result = defaultValue;
		
		String value = getProperty(key);
		if (value != null) {
			try {
				result = Integer.parseInt(value);
			}
			catch (NumberFormatException e) {
				String message = "Unable to parse property " + key + " - invalid format!";
				throw new RuntimeException(message);
			}
		}
		
		return result;
	}
	
	/** Returns a list of strings for the specified key, by breaking the value 
	 * on every white space character.
	 * Note that changing the list does not affect the stored value
	 * (use the append method to change it).
	 */
	public List<String> getStringListProperty(String key, List<String> defaultValue) {
		String value = getProperty(key, null);
		if (value == null)
			return Collections.EMPTY_LIST;
		else
			return StringUtils.breakString(value);
	}
	
	/** Appends the specified value to the string list stored with
	 * the property.
	 */
	public void appendToStringListProperty(String key, String value) {
		String oldValue = getProperty(key, "");
		String separator = (oldValue.equals("")) ? "" : " ";
		setProperty(key, oldValue + separator + value);
	}
	
	/** Returns the class specified by the key.
	 * @exception ClassNotFoundException is thrown if the class was not found.
	 */
	public Class getClassProperty(String key, Class defaultValue) throws ClassNotFoundException {
		String value = getProperty(key, null);
		if (value == null)
			return defaultValue;
		else {
			Class answer = Class.forName(value);
			return answer;
		}
	}
	
	/** Prints this property list out to the specified output stream.
	 * THe properties are first sorted lexicographicaly.
	 */
	public void list(PrintStream out) {
		if (!isEmpty()) {
			Map props = new TreeMap(); // sorts entries lexicographicaly
			props.putAll(this);
			for (Iterator i = props.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry entry = (Map.Entry) i.next();
				String key = (String) entry.getKey();
				out.println(key + "=" + entry.getValue().toString());
			}
		}
	}
	
	/** Reads a property list (key and element pairs) from the specified file.
	 */
	public void load(String filename) {
		if (filename != null) {
			try {
				FileInputStream in = new FileInputStream(filename);
				super.load(in);
				in.close();
			}
			catch (FileNotFoundException e) {
				throw new UserErrorException("Properties file " + filename + " does not exist!");
			}
			catch (IOException e) {
				throw new UserErrorException("Failed to open properties file " + filename + "!");
			}			
		}
	}
}