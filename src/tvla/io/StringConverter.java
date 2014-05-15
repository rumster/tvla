package tvla.io;

import java.io.PrintStream;

/** A base class for converters of objects to strings in a specific format.
 * @author Roman manevich.
 */
public abstract class StringConverter {
	public abstract String convert(Object o);
	
	public String convert(Object o, String header) {
		return convert(o);
	}
	
	/**
	 * Allows the converter to print directly to the output stream.
	 * This is useful in case the esulting string is very big.
	 */
	public void print(PrintStream outStream, Object o, String header) {
		outStream.print(convert(o,header));
	}

	
	/**
	 * Puts quotation around a string
	 * @param str
	 * @return
	 */
	protected String quote(String str) {
		return  new String("\"" + str + "\"");
	}
}