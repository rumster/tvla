package tvla.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** This class represents a persistent database of properties
 * that belong to the application.
 * The properties database is built of layers:
 * 1. Java system properties.
 * 2. Layers made of files that were added using the addPropertyFile method,
 *    in the order they were added.
 * The layers override each other, such that if a value is specified
 * for a property in two layers, then the value in the higher layer
 * overrides the one in the lower layer.
 * 
 * @author Roman Manevich.
 * @since 13.10.2001 Initial creation.
 */
public final class ProgramProperties {
	public static boolean debug = false;
	
	/** A collection of file names, corresponding to property files.
	 */
	private static List propertyFiles = new ArrayList();
	
	/** The properties database.
	 */
	private static PropertiesEx properties = new PropertiesEx();
	
	public static void reset() {
		debug = false;
		propertyFiles = new ArrayList();
		properties = new PropertiesEx();
	}
	
	/** Returns a map containing all properties.
	 */
	public static Map getAllProperties() {
		return Collections.unmodifiableMap(properties);
	}
	
	/** Updates the properties database with the specified associative pair.
	 */
	public static void setProperty(String key, String value) {
		if (properties == null)
			properties = new PropertiesEx(System.getProperties());
		properties.setProperty(key, value);
	}
	
	/** Returns the value of a property or the default
	 * value in case there's no associated value.
	 */
	public static String getProperty(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	/** Returns the boolean value of a property or the default
	 * value in case there's no association or the specified
	 * value could not be parsed as a boolean.
	 */
	public static boolean getBooleanProperty(String key, boolean defaultValue) {
		return properties.getBooleanProperty(key, defaultValue);
	}

	/** Sets the property to the given boolean value
	 */
	public static void setBooleanProperty(String key, boolean defaultValue) {
		setProperty(key, defaultValue ? "true" : "false");
	}
	
	/** Returns the integer value of a property or the default
	 * value in case there's no association or the specified
	 * value could not be parsed as an integer.
	 */
	public static int getIntProperty(String key, int defaultValue) {
		return properties.getIntProperty(key, defaultValue);
	}
	
	/** Returns a list of strings for the specified key, by breaking the value 
	 * on every white space character.
	 * Note that changing the list does not affect the stored value
	 * (use the append method to change it).
	 */
	public static List<String> getStringListProperty(String key, List<String> defaultValue) {
		return properties.getStringListProperty(key, defaultValue);
	}
	
	/** Appends the specified value to the string list stored with
	 * the property.
	 */
	public static void appendToStringListProperty(String key, String value) {
		properties.appendToStringListProperty(key, value);
	}

	/** Adds a property file as the top layer, overriding any properties specified
	 * until now.
	 */
	public static void addPropertyFile(String path) {
		propertyFiles.add(path);
	}
	
	/** Writes the properties database to the specified stream.
	 */
	public static void list(PrintStream out, String header) {
		if (properties != null) {
			if (header != null)
				out.println(header);
			
			if (!properties.isEmpty()) {
				Map props = new TreeMap(); // sorts entries lexicographicaly
				props.putAll(properties);
				for (Iterator i = props.entrySet().iterator(); i.hasNext(); ) {
					Map.Entry entry = (Map.Entry) i.next();
					String key = (String) entry.getKey();
					if (!key.startsWith("tvla."))
						continue;
					out.println(key + "=" + entry.getValue().toString());
				}
			}
		}
	}
	
	/** Loads the properties from the default property files.
	 */
	public static void load() {
		//properties.putAll(System.getProperties());
		for (Iterator iter = propertyFiles.iterator(); iter.hasNext(); ) {
			String path = (String) iter.next();
			load(path);
		}
	}
	
	/** Loads the specified file.
	 */
	public static void load(String path) {
		if (path != null) {
			try {
				FileInputStream in = new FileInputStream(path);
				PropertiesEx tmpProps = new PropertiesEx();
				tmpProps.load(in);
				in.close();
				properties.putAll(tmpProps);
			}
			catch (FileNotFoundException e) {
				System.err.println("Properties file " + path + " does not exist!");
			}
			catch (IOException e) {
				System.err.println("Failed to open properties file " + path + "!");
			}			
		}
	}
}