package tvla.language;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import tvla.util.ProgramProperties;

import com.ibm.dk.dps.io.CPreProcessorStream;

/** A helper class that passes user-defined macros to the CPreProcessor stream.
 * @author Roman Manevich.
 * @since 6.4.2002 Initial creation.
 */
public class AddMacrosToPreProcessor {
	/** Adds the macros stored in the program properties to the stream.
	 * There are two variations to the macro syntax.
	 * The first is symbol, which is the same as #define symbol,
	 * and the second is symbol(value), which is the same as #define symbol value.
	 */
	public static void add(CPreProcessorStream stream) {
		List macroList = ProgramProperties.getStringListProperty("tvla.parser.externalMacros", Collections.EMPTY_LIST);
		for (Iterator i = macroList.iterator(); i.hasNext(); ) {
		    String macro = (String) i.next();
			
			int sepIndex = macro.indexOf('(');
			if (sepIndex < 0) {// a macro without a value
				stream.addMacro(macro, null, null);
			}
			else { // a macro with a value in the syntax - symbol(value)
				try {
					String symbol = macro.substring(0, sepIndex);
					String value = macro.substring(sepIndex+1, macro.length()-1);
					stream.addMacro(symbol, null, value);
				}
				catch (ArrayIndexOutOfBoundsException e) {
					String message = "Illegal macro syntax : " + macro + "!\n" +
									 "Use the syntax symbol(value).";
					throw new RuntimeException(message);
				}
			}
		}
	}
}