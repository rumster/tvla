/*
 * Created on Jan 19, 2004
 * 
 */
package tvla.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import tvla.formulae.Var;

/** A class implementing different string related utilities.
 * @author Roman Manevich
 */
public class StringUtils {
	/** The character used to create a line (for #addUnderline).
	 */
	public static final char underlineCharacter = '-';
	
	/** Line separator ("\n" on UNIX).
	 */
	public static final String newLine = System.getProperty("line.separator");

	/** Splits a string by considering " " as a separator and storing
	 * the resulting sub-string in a list.
	 * @param s A string.
	 * @return A list of strings.
	 */
	public static List breakString(String s) {
		return breakString(s, " ");
	}
	
	/** Splits a string according to the specified separator string and stores
	 * the result in a list.
	 * @param s A string.
	 * @param sep A separator string.
	 * @return A list of strings.
	 */
	public static List breakString(String s, String sep) {
		List result = new ArrayList();
		StringTokenizer tokenizer = new StringTokenizer(s, sep);

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			result.add(token);
		}

		return result;
	}	

	/** Can be used to add an underline to another string.
	 * 
	 * @param source The original string.
	 * @return The source string with an underline added to it.
	 */
	public static String addUnderline(String source) {
		StringBuffer result = new StringBuffer();
		result.append(source + newLine);

		for (int index = 0; index < source.length(); ++index) {
			result.append(underlineCharacter);
		}

		return result.toString();
	}

	/** Replaces all instances of oldSubString with newSubString in source and
	 * returns the resulting string.
	 * 
	 * @param source The source in which the replacement is done.
	 * @param oldSubString The string to be replaced.
	 * @param newSubString The string to put instead of oldSubString.
	 * @return The string with all instance replaced.
	 */
	public static String replace(
		String source,
		String oldSubString,
		String newSubString) {
		StringBuffer result = new StringBuffer();

		int lastpos = 0;
		while (lastpos < source.length()) {
			int pos = source.indexOf(oldSubString, lastpos);
			if (pos >= 0) {
				result.append(source.substring(lastpos, pos) + newSubString);
				lastpos = pos + oldSubString.length();
			} else { // last occurance of the old substring
				pos = source.length();
				result.append(source.substring(lastpos, pos));
				break;
			}
		}

		return result.toString();
	}

	/** Concatenates the string in the array to create a single string.
	 * @param arr An array of strings.
	 */
	public static String concatenate(String[] arr) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < arr.length; ++i)
			buffer.append(arr[i]);
		return buffer.toString();
	}
	
    public static String collectionToList(Iterable<? extends Object> c) {
        StringBuffer ret = new StringBuffer();
        for(Iterator<? extends Object> itr = c.iterator(); itr.hasNext(); ) {
          ret.append(itr.next().toString());
          if (itr.hasNext())
            ret.append(",");
        }
        
        return ret.toString();
      }

	/** Concatenates the string in the array to create a single string
	 * by inserting a specified separator sub-string between the strings
	 * in the array.
	 * @param arr An array of strings.
	 * @param separator A string to be inserted between the strings in the array.
	 */
	public static String concatenate(String[] arr, String separator) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < arr.length; ++i)
			buffer.append(arr[i] + separator);
		return buffer.toString();
	}
}