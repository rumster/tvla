package tvla.analysis.decompose;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.Engine;
import tvla.core.TVSFactory;
import tvla.exceptions.ExceptionHandler;
import tvla.io.IOFacade;
import tvla.language.TVP.TVPParser;
import tvla.language.TVS.TVSParser;
import tvla.transitionSystem.AnalysisGraph;
import tvla.transitionSystem.Location;
import tvla.util.Logger;
import tvla.util.ProgramProperties;
import tvla.util.PropertiesEx;
import tvla.util.StringUtils;

/** This class represents a command-line interface for TVLA users.
 * @author Tal Lev-Ami
 * @author Roman Manevich
 */
public class DecomposerRunner {
	protected static String programName;
	protected static String inputFile;
	protected static String propertyName;
	protected static Collection<String> specificPropertiesFiles = new ArrayList<String>();
	protected static String engineType;
	
	/** Search path for the pre-processor.
	 */
	protected static String searchPath;
	
	/** Used to supply information about the software version.
	 */
	private static final String versionInfo;
	static {
		PropertiesEx props = new PropertiesEx("/tvla/version.properties");
		versionInfo = props.getProperty("version", "Unknown TVLA version");
	}

	/** Main entry point of TVLA.
	 * @author Tal Lev-Ami 
	 */
	public static void main(String args[]) {
		try {
			loadProgramProperties(args);
			parseArgs(args);
			initProgramProperties(args);

			if (!inputFile.endsWith(".out.tvs"))
			    inputFile = inputFile + ".out.tvs";
			ProgramProperties.setProperty("tvla.dot.outputFile", programName + "_dc.dt");
            ProgramProperties.setProperty("tvla.tvs.outputFile", programName + "_dc.tvs");
			
            Engine.activeEngine = new DecompositionIntraProcEngine();
			
			// Sets the search path fo the pre-processor
			String pathString = programName;
			int lastSep = pathString.lastIndexOf(File.separator);
			if (lastSep >=0)
				searchPath += ";" + args[0].substring(0, lastSep+1);
			ProgramProperties.setProperty("tvla.searchPath", searchPath);

			printLogHeader(args);
			if (ProgramProperties.getBooleanProperty("tvla.log.addPropertiesToLog", false)) {
				String header = StringUtils.addUnderline("TVLA Properties");
				ProgramProperties.list(Logger.getUnderlyingStream(), header);
			}
			if (!AnalysisStatus.terse)
				Logger.print(StringUtils.newLine + "Loading specification ... ");
			AnalysisStatus.loadTimer.start();

			AnalysisGraph.activeGraph = new DecomposeAnalysisGraph();
            TVPParser.configure(programName, searchPath);
            AnalysisGraph.activeGraph.init();
            IOFacade.instance().printProgram(AnalysisGraph.activeGraph);

			if (!AnalysisStatus.terse)
				Logger.println("done"); // done loading the specification
						
			TVSFactory.getInstance().init();

			if (!AnalysisStatus.terse)
				Logger.print("Reading TVS files ... ");
			Collection<Location> locations = TVSParser.readLocations(inputFile);
			for (Location location : locations) {
			    Location glocation = AnalysisGraph.activeGraph.getLocationByLabel(location.label());
			    AnalysisGraph.activeGraph.storeStructures(glocation, location.structures);
			}
			AnalysisStatus.loadTimer.stop();

            if (AnalysisGraph.activeGraph != null)
                AnalysisGraph.activeGraph.dump(); // this has no effect for multithreaded engines

			if (!AnalysisStatus.terse)
				System.out.println(StringUtils.newLine + "All tasks completed");
		}
		catch (Throwable t) {
			ExceptionHandler.instance().handleException(t);
		}
	}

	/** Informs the user how to use TVLA and what are the available options.
	 */
	protected static void usage() {
		usage(null);
	}
	
	protected static void usage(String errorMsg) {
		if (errorMsg != null) {
			System.err.println("Error: " + errorMsg);
		}
		
		System.err.println("Usage: tvla <program name> [input file] [options]");
		System.err.println("Options:");

		System.err.println(" -join [algorithm]       Determines the type of join method to apply.");
		System.err.println("                         rel  - Relational join.");
		System.err.println("                         part - Partial join.");
		System.err.println("                         ind  - Independent attributes (single structure).");
		System.err.println("                         conc - Concrete join (no abstraction).");
		System.err.println(" -props <file name>      Can be used to specify a properties file.");
		System.err.println(" -log <file name>        Creates a log file of the execution.");
		System.err.println(" -tvs <file name>        Creates a TVS formatted output.");
		System.err.println(" -xml <file name>        Creates a XML formatted output of the program CFG.");
		System.err.println(" -dot <file name>        Creates a DOT formatted output.");
		System.err.println(" -D<macro name>[(value)] Defines a C preprocessor macro.");
		System.err.println(" -terse                  Turns off on-line information printouts.");
		System.err.println(" -path <directory path>  Can be used to specify a search path.");
		System.exit(0);
	}

	/** Parses the arguments passed from the command-line.
	 */
	static protected void parseArgs(String args[]) throws Exception {
		int i = 0;
		
		// help options
		if (args.length == 0 || 
			( // args.length >= 1
			 args[i].equals("-h") || args[i].equals("-help") ||
			 args[i].equals("?") || args[i].equals("/?"))
			)
			usage();
		
		if (args[0].charAt(0) == '-') {
			System.err.println("Error: first argument should be the program name" +
							   "(saw " + args[0] + ")!");
			System.err.println();
			usage();
		}
		
		programName = args[0];
		propertyName = args[0];
		ProgramProperties.setProperty("tvla.programName", programName);
		ProgramProperties.setProperty("tvla.propertyName", propertyName);
		++i;

		inputFile = args[0];
		propertyName = args[0];
		if (args.length > 1 && args[1].charAt(0) != '-') {
			inputFile = args[1];
			++i;
		}

		for (; i  < args.length; i++) {
		    if (args[i].equals("-join")) {
				i++;
				if (i >= args.length) {
					System.err.println("Missing argument after -join!");
					usage();
				}
				String joinType = args[i];
				if (!joinType.equals("rel") && 
					!joinType.equals("part") &&
					!joinType.equals("part_embedding") &&
					!joinType.equals("j3") && 
					!joinType.equals("ind") &&
					!joinType.equals("conc")) {
					System.err.println("Invalid join option specified: " + joinType + "!");
					usage();
				}
				ProgramProperties.setProperty("tvla.joinType", joinType);
			} else if (args[i].startsWith("-D")) {
				if (args[i].length() > 2) {
					String macro = args[i].substring(2, args[i].length());
					ProgramProperties.appendToStringListProperty("tvla.parser.externalMacros", macro);
				}
			} else if (args[i].equals("-log")) {
				i++;
				if (i >= args.length) {
					System.err.println("Missing argument after -log!");
					usage();
				}
				ProgramProperties.setProperty("tvla.log.logFileName", args[i]);
				ProgramProperties.setBooleanProperty("tvla.output.redirectToDirectory", false);
			} else if (args[i].equals("-xml")) {
				i++;
				if (i >= args.length) {
					System.err.println("Missing argument after -xml!");
					usage();
				}
				ProgramProperties.setProperty("tvla.xml.outputFile", args[i]);
				ProgramProperties.setBooleanProperty("tvla.output.redirectToDirectory", false);
				ProgramProperties.setBooleanProperty("tvla.xml.enabled", true);
			} else if (args[i].equals("-tvs")) {
				i++;
				if (i >= args.length) {
					System.err.println("Missing argument after -tvs!");
					usage();
				}
				ProgramProperties.setProperty("tvla.tvs.outputFile", args[i]);
				ProgramProperties.setBooleanProperty("tvla.output.redirectToDirectory", false);
				ProgramProperties.setBooleanProperty("tvla.tvs.enabled", true);
			} else if (args[i].equals("-dot")) {
				i++;
				if (i >= args.length) {
					System.err.println("Missing argument after -dot!");
					usage();
				}
				ProgramProperties.setProperty("tvla.dot.outputFile", args[i]);
				ProgramProperties.setBooleanProperty("tvla.output.redirectToDirectory", false);
				ProgramProperties.setBooleanProperty("tvla.dot.enabled", true);
			} else if (args[i].equals("-terse")) {
				ProgramProperties.setProperty("tvla.terse", "true");
			} else if (args[i].equals("-props")) {
				++i;
			} else if (args[i].equals("-path")) {
				i++;
				if (i >= args.length) {
					System.err.println("Missing argument after -path!");
					usage();
				}
				ProgramProperties.setProperty("tvla.parser.searchPath", args[i]);
			} else {
				System.err.println("Unknown option " + args[i] + "!");
				System.err.println();
				usage();
			}
		}
	}
	
	/** Prints information regarding to the analysis mode.
	 * @author Roman Manevich.
	 * @since 30.7.2001 Initial creation.
	 */
	protected static void printLogHeader(String [] args) {
		if (!ProgramProperties.getProperty("tvla.log.logFileName", "null").equals("null"))
			Logger.print(versionInfo + " ");
		
        Logger.println("Running Decomposing TVP mode");

        if (!ProgramProperties.getProperty("tvla.log.logFileName", "null").equals("null")) {
			Logger.print("Arguments: ");
			for (int index=0; index<args.length; ++index)
				Logger.print(args[index] + " ");
			Logger.println();
		}
	}
	
	/** Initializes and validates program properties that are not handled by any other class
	 * from command-line options.
	 * @author Roman Manevich.
	 * @since 22.11.2001 Initial creation.
	 */
	protected static void initProgramProperties(String [] args) {
		String tvpName = null;
		if (args.length == 0 || args[0].charAt(0) == '-')
			usage();
		else
			tvpName = args[0];

        ProgramProperties.setProperty("tvla.engine.type", "dtvla");  
		
		searchPath = "";
		String propPath = ProgramProperties.getProperty("tvla.parser.searchPath", "");
		if (propPath.length() > 0)
			searchPath += ";" + propPath;
		searchPath += ";" + computeTvlaHome();
		
		// finds the name of the analyzed program and its directory
		String progDir = ".";
		String progName = tvpName;
		
		int last = tvpName.lastIndexOf(File.separator);
		if ( 0 < last)  {
			assert (last < tvpName.length());
			progDir = tvpName.substring(0,last+1);			
			progName = tvpName.substring(last+1,tvpName.length());
			int index = progName.indexOf(".tvp");
			if (index >=0)
				progName = progName.substring(0, index);
			index = progName.indexOf(".pts");
			if (index >=0)
				progName = progName.substring(0, index);
		}		
		assert(progName != null);
		ProgramProperties.setProperty("tvla.output.progName", progName);
		
		setupOutputDirectories(progDir);
		
		// determine the name of the dot output file
		String outputFile;
		if (!optionSpecified(args, "-dot") && 
			ProgramProperties.getBooleanProperty("tvla.dot.enabled", true) &&
			tvpName != null) {
			outputFile = ProgramProperties.getProperty("tvla.dot.outputFile", "null");
			if (outputFile.equals("null"))
				outputFile = progName + ".dt";
			ProgramProperties.setProperty("tvla.dot.outputFile", outputFile);
		}
		
		// determines the name of the tvs output file 
		if (!optionSpecified(args, "-tvs") && 
			ProgramProperties.getBooleanProperty("tvla.tvs.enabled", true) &&
			tvpName != null) {
				outputFile = ProgramProperties.getProperty("tvla.tvs.outputFile", "null");
				if (outputFile.equals("null"))
					outputFile = progName + ".out.tvs";
				ProgramProperties.setProperty("tvla.tvs.outputFile", outputFile);
		}
	}
	
	/**
	 * Setup the directories in which the output will be generated 
	 * @param progName
	 */
	private static void setupOutputDirectories(String progDir) {				
		String absoluteDirectory = ProgramProperties.getProperty("tvla.output.absoluteDirectory", "null");
		if (absoluteDirectory == null || absoluteDirectory.equals("null"))
			absoluteDirectory = progDir;
		
		String outputDirectory = absoluteDirectory;
		
		String outputSubDirectory = ProgramProperties.getProperty("tvla.output.subDirectory", "null");
		if (outputSubDirectory != null && !outputSubDirectory.equals("null")) 
			outputDirectory += File.separator + outputSubDirectory;
		
		ProgramProperties.setProperty("tvla.output.outputDirectory", outputDirectory);
	}

	
	/** Loads the property files.
	 * The directory path for the default properties file (tvla.properties)
	 * is extracted from the java.class.path property.
	 * @param The program arguments are needed in order to extract information needed
	 * to access the desired property files.
	 * @author Roman Manevich.
	 * @since 14.10.2001 Initial creation.
	 */
	protected static void loadProgramProperties(String [] args) throws Exception {
		for (int i = 0; i < args.length; ++i) {
			if (i == 0)
				programName = args[i];
			if (args[i].equals("-props")) {
				++i;
				if (i > args.length) {
					System.err.println("-props was specified without a file name!");
					usage();
				}
				File file = new File(args[i]);
				if (!file.exists()) {
					System.err.println("-props specified the file " + args[i] + ", which does not exist!");
					usage();
				}
				specificPropertiesFiles.add(args[i]);
			}
		}

		String tvlaHome = computeTvlaHome();
		String fileSeparator = System.getProperty("file.separator");

		// add the default properties file tvla.properties from the application's home directory.
		String defaultPropertiesFile = tvlaHome;
		if (!defaultPropertiesFile.endsWith(fileSeparator))
			defaultPropertiesFile = defaultPropertiesFile + fileSeparator;
		defaultPropertiesFile = defaultPropertiesFile + "tvla.properties";
		ProgramProperties.addPropertyFile(defaultPropertiesFile);

		// add the user properties file user.properties from the application's home directory,
		String userPropertiesFile = tvlaHome;
		if (!userPropertiesFile.endsWith(fileSeparator))
			userPropertiesFile = userPropertiesFile + fileSeparator;
		userPropertiesFile = userPropertiesFile + "user.properties";
		ProgramProperties.addPropertyFile(userPropertiesFile);
		
		// If the directory includes a properties file with the same name as the
		// TVP file, load it.
		File programSpecificProperties = new File(programName + ".properties");
		if (programSpecificProperties.exists()) {
		    ProgramProperties.addPropertyFile(programSpecificProperties.toString());
		}
		
		// Add a run-specific property file if one was specified in the command-line.
		if (!specificPropertiesFiles.isEmpty()) {
			for (Iterator<String> propIter = specificPropertiesFiles.iterator(); propIter.hasNext(); ) {
				String propertyFile = propIter.next();
				File f = new File(propertyFile);
				if (f.exists())
					ProgramProperties.addPropertyFile(propertyFile);
			}
		}
		ProgramProperties.load();
	}
	
	/** Computes the path to the TVLA home directory.
	 * @author Roman Manevich.
	 * @since November 17 2001 Initial creation.
	 */
	protected static String computeTvlaHome() {
		// Check the program properties
		String tvlaHome = System.getProperty("tvla.home", null);
		
		// If failed try to deduce it from the class path
		if (tvlaHome == null) {
			String classPath = System.getProperty("java.class.path");
			File tvlaHomeFile = new File(classPath);
			if (tvlaHomeFile.isFile()) { // a .jar file
				String fileName = tvlaHomeFile.getName();
				int i = classPath.indexOf(fileName);
				tvlaHome = classPath.substring(0, i);
			}
			else {
				tvlaHome = classPath;
			}
		}
		
		return tvlaHome;
	}
	
	/** Searchs the command-line options for a specific argument.
	 * @author Roman Manevich.
	 * @since 24.11.2001 Initial creation.
	 */
	protected static boolean optionSpecified(String [] args, String option) {
		for (int index = 0; index < args.length; ++index) {
			if (args[index].equals(option))
				return true;
		}
		return false;
	}
}