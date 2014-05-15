package tvla.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;


/** Outputs information to the log stream.
 * @author Tal Lev-Ami
 */
public class Logger {
	private static String fileSeperator = java.io.File.separator; //ProgramProperties.getProperty("tvla.output.file.seperator", "\\");
	private static PrintStream logStream = initLogStream();
	//private static PrintStream logStream = System.err;
	
	/** Outputs the specified object to the log stream.
	 * @author Tal Lev-Ami.
	 */
	public static void print(Object o) {
		logStream.print(o);
		logStream.flush();
	}

	/** Outputs the specified object to the log stream and appends
	 * a newline character.
	 * @author Tal Lev-Ami.
	 */
	public static void println(Object o) {
		logStream.println(o);
		logStream.flush();
	}

	/** Prints a newline character.
	 * @author Roman Manevich.
	 * @since 14.10.2001 Initial creation.
	 */
	public static void println() {
		logStream.println();
        logStream.flush();
	}
	
	public static void printf(String format, Object ... args) {
	    logStream.printf(format, args);
	    logStream.flush();
	}

	/** Returns the actual output stream used by the logger.
	 * @author Roman Manevich.
	 * @since 14.10.2001 Initial creation.
	 */
	public static PrintStream getUnderlyingStream() {
		return logStream;
	}

	/** Returns the PrintStream used to output log information to.
	 * noam: Added support for specifying the directory for the log files 
	 * @author Roman Manevich.
	 * @aince 2.2.2002 Initial creation.
	 */
	public static PrintStream initLogStream() {
		String logFileName = ProgramProperties.getProperty("tvla.log.logFileName", "null");
		if (logFileName == null || logFileName.equals("null"))
			return setLogStream(null,null);
		
		
		String logSubDirName = null;
		
		boolean	redirectOutputToDir = ProgramProperties.getBooleanProperty("tvla.output.redirectToDirectory",false);
		if (redirectOutputToDir) {
		  logSubDirName = ProgramProperties.getProperty("tvla.log.logSubDirectory", "null");
		  if (logSubDirName != null && logSubDirName.equals("null"))
		    logSubDirName = null;
		  
		  String outputDirectory =ProgramProperties.getProperty("tvla.output.outputDirectory", "null");
		  if (outputDirectory != null && outputDirectory.equals("null")) 
		    outputDirectory = null;
		  
		  
		  if (outputDirectory != null) 
		    if (logSubDirName != null)
		      logSubDirName = outputDirectory + Logger.fileSeperator + logSubDirName;
		    else
		      logSubDirName = outputDirectory;
		  
		}
		
        return setLogStream(logSubDirName, logFileName);
	}

	public static void fatalError(Object o) {
		logStream.print(o);
		logStream.flush();
		throw new RuntimeException("Fatal Eror " + o.toString());
	}

	
	private static PrintStream setLogStream(String logDir, String logFileName) {
		PrintStream result = System.err; // the default stream
				
		if (logDir != null) {
			assert(!logDir.equals("null"));
			assert(logFileName != null);
			assert(!logFileName.equals("null"));
				
			logFileName = logDir +  Logger.fileSeperator + logFileName;
			File dir = new File(logDir);
			
			boolean dirExists = dir.exists();
			if (!dirExists) {
				System.err.println("Log directory " + logDir + " created");					 	
			
				// Create output directory  if it does not exist
				dir.mkdirs();
			}
			else {
				// directory exits. If the file exists - erase it	
				File file = new File(logFileName);
				boolean exists = file.exists();
				// erase existing file
				file.delete();
			}
		}
		
		try{
			if (logFileName != null) {
				assert(!logFileName.equals("null")); 
				result = new PrintStream(new FileOutputStream(logFileName), true);
			}
		} 
		catch (IOException e) {
				System.err.println("Unable to create log: " + e.getMessage());
		}

		return result;
	}

}