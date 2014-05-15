package tvla.exceptions;

import java.io.FileNotFoundException;

import tvla.util.Logger;
import tvla.util.ProgramProperties;

/** A class for processing exceptions on the application level.
 * @author Roman Manevich.
 * @since tvla-2-alpha (April 6 2002) Initial creation.
 */
public class ExceptionHandler {
	/** The one and only instance of this class.
	 */
	private static final ExceptionHandler theHandler = new ExceptionHandler();

	/** Retrieves the single instance of this class.
	 */
	public static ExceptionHandler instance() {
		return theHandler;
	}
	
	/** Processes the exception.
	 */
	public void handleException(Throwable t) {
		if (t instanceof ExceptionInInitializerError) {
			t = ((ExceptionInInitializerError)t).getException();
		}
		
		String message = t.getMessage();
		if (message == null || message.equals(""))
			message = "Exception " + t.getClass();
		
		if (t instanceof FileNotFoundException) {
			message = "File not found : " + message;
		}
		else if (t instanceof TVLAException) {
			// Do nothing. Leave the message as is.
		}
		else if (t instanceof Error && t.getMessage() != null && t.getMessage().startsWith("Symbol recycling detected")) {
			// This is really a hack to deal with this strange problem.
			message = "";
		}
		else {
			message = "Internal Error :" + message;
		}
		
		message = "\n" + message;
		
		String logFileName = ProgramProperties.getProperty("tvla.log.logFileName", "null");
		if (logFileName != null && !logFileName.equals("null"))
			System.err.println(message);
		
		Logger.println(message);
		
		if ((t instanceof AssertionError)
			|| ProgramProperties.getBooleanProperty(
				"tvla.printExceptionStackTrace",
				false))
			t.printStackTrace();
	}
	
	/** Singleton pattern
	 */
	protected ExceptionHandler() {
	}
}