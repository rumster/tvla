package tvla.diffUtility;

import java.io.File;
import java.util.List;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.Engine;
import tvla.analysis.IntraProcEngine;
import tvla.analysis.decompose.DecompositionIntraProcEngine;
import tvla.analysis.interproc.InterProcEngine;
import tvla.analysis.interproc.PastaRunner;
import tvla.analysis.interproc.semantics.AuxiliaryPredicates;
import tvla.analysis.multithreading.MultithreadEngine;
import tvla.analysis.multithreading.buchi.BuchiAutomaton;
import tvla.analysis.multithreading.buchi.MultithreadEngineBuchi;
import tvla.core.TVSFactory;
import tvla.exceptions.ExceptionHandler;
import tvla.exceptions.UserErrorException;
import tvla.language.BUC.BUCParser;
import tvla.language.BUC.BuchiAutomatonAST;
import tvla.language.TVM.TVMAST;
import tvla.language.TVM.TVMParser;
import tvla.language.TVP.TVPParser;
import tvla.language.TVS.TVSParser;
import tvla.transitionSystem.AnalysisGraph;
import tvla.util.Logger;
import tvla.util.ProgramProperties;

/** A command-line interface for differentiating TVLA outputs.
 * Used to test TVLA.
 * @author Roman Manevich.
 * @since 19.12.2000 Initial creation.
 */
public class Runner extends tvla.Runner {
	protected static String programName;
	protected static String inputFile1;
	protected static String inputFile2;
	protected static String diffFile;
	protected static String dotFile;
	protected static String path = ".";	

	/** Main entry point of the diff utility.
	 */
	public static void main(String [] args) {
		try {
			loadProgramProperties(args);
			parseArgs(args);
			initProgramProperties(args);

			if (engineType.equals("tvla"))
				Engine.activeEngine = new IntraProcEngine();
			else if (engineType.equals("tvmc"))
				Engine.activeEngine = new MultithreadEngine();
			else if (engineType.equals("ddfs"))
				Engine.activeEngine = new MultithreadEngineBuchi();
			else if (engineType.equals("pasta"))
				Engine.activeEngine = new InterProcEngine();
            else if (engineType.equals("dtvla")) 
                Engine.activeEngine = new DecompositionIntraProcEngine();
			else {
				throw new UserErrorException("An invalid engine was specified: " + engineType);
			}
			
			Logger.print("Loading ... ");
			AnalysisStatus.loadTimer.start();
			
			AnalysisGraph.activeGraph = new AnalysisGraph();
			if (engineType.equals("tvla") || engineType.equals("dtvla")) {
				int lastSep = programName.lastIndexOf(File.separator);
				if (lastSep >=0) {
					searchPath += ";" + args[0].substring(0, lastSep+1);
					programName = programName.substring(lastSep+1,programName.length());
				}
				TVPParser.configure(programName, searchPath);
			} 
			else if (engineType.equals("tvmc")) {
				int lastSep = programName.lastIndexOf(File.separator);
				if (lastSep >=0)
					searchPath += ";" + args[0].substring(0, lastSep+1);
				TVMAST tvmFile = TVMParser.configure(programName, searchPath);
				tvmFile.compileAll();
			}
			else if (engineType.equals("ddfs")) {
				int lastSep = propertyName.lastIndexOf(File.separator);
				if (lastSep >=0)
					searchPath += ";" + args[0].substring(0, lastSep+1);
				BuchiAutomatonAST propertyAST = BUCParser.configure(propertyName, searchPath);
				TVMAST tvmFile = TVMParser.configure(programName, searchPath);
				tvmFile.generateProgram();
				propertyAST.generate();
				tvmFile.generateDeclarations();
				tvmFile.compileProgram();
				propertyAST.compile();
				BuchiAutomaton property = propertyAST.getAutomaton();
				TVSParser.setProperty(property);
				((MultithreadEngineBuchi) Engine.activeEngine).setProperty(property);
			}  
			else if (engineType.equals("pasta")) {
				AuxiliaryPredicates.init();
				int lastSep = programName.lastIndexOf(File.separator);
				if (lastSep >=0)
					searchPath += ";" + args[0].substring(0, lastSep+1);
				PastaRunner.readProgram(programName, searchPath);
			}
			else {
				throw new UserErrorException("An invalid engine was specified: " + engineType);
			}

			TVSFactory.getInstance().init(); // Necessary, if using the functional implementation.
			List refLocations = TVSParser.readLocations(inputFile1);
			List outputLocations = TVSParser.readLocations(inputFile2);
			Logger.print("done"); // done loading everthing
			
			String outputFile = inputFile1;
			int pathPosition = inputFile1.lastIndexOf(File.separator);
			if (pathPosition > -1)
				outputFile = inputFile1.substring(pathPosition+1, inputFile1.length());


			OutputComparator comparator = new OutputComparator();
			comparator.setDotOutputFile(dotFile);
			comparator.setTVSOutputFile(diffFile);

			boolean foundDifferences = comparator.compareLocationSets(refLocations, outputLocations,
				inputFile1, inputFile2);
			if (foundDifferences)
				System.out.println("Found some differences.");
			else
				System.out.println("No differences.");
		}
		catch (Throwable t) {
			ExceptionHandler.instance().handleException(t);
		}
	}

	/** Parses the arguments passed from the command-line.
	 */
	protected static void parseArgs(String args[]) {
		int i = 0;
		if ((args.length < 3) || (args[i].charAt(0) == '-')) {
			System.err.println("Illegal number of arguments : " + args.length);
			usage();
		}
		programName = args[i];
		
		++i;
		if (args[i].charAt(0) == '-') {
			System.err.println("Illegal argument : " + args[i] + "!");
			usage();
		} 
		inputFile1 = args[i];
		++i;
		
		if (args[i].charAt(0) == '-') {
			System.err.println("Illegal argument : " + args[i] + "!");
			usage();
		} 
		inputFile2 = args[i];
		++i;

		for (; i  < args.length; i++) {
			if (args[i].equals("-path")) {
				i++;
				if (i >= args.length) {
					System.err.println("Missing argument after -path!");
					usage();
				}
				path = args[i];
            } else if (args[i].startsWith("-D")) {
                if (args[i].length() > 2) {
                    String macro = args[i].substring(2, args[i].length());
                    ProgramProperties.appendToStringListProperty("tvla.parser.externalMacros", macro);
                }
			}
			else if (args[i].equals("-dot")) {
				i++;
				if (i >= args.length) {
					System.err.println("-dot specified without a file name!");
					usage();
				}
				dotFile = args[i];
			}
			else if (args[i].equals("-tvs")) {
				i++;
				if (i >= args.length) {
					System.err.println("-tvs specified without a file name!");
					usage();
				}
				diffFile = args[i];
			}
			else if (args[i].equals("-props")) {
				i++;
			}
			else 
				System.err.println("Unknown option " + args[i]);
		}

		// Add program's directory to path.
		int lastSep = programName.lastIndexOf(File.separator);
		if (lastSep >=0) path += ";" + programName.substring(0, lastSep+1);

		// Add contents of searchPath property to path.
		String propPath = ProgramProperties.getProperty("tvla.parser.searchPath", "");
		if (propPath.length() > 0) path += ";" + propPath;

		// Add tvla.home property to path (set to $TVLA_HOME in tvla script).
		String tvlaHome = System.getProperty("tvla.home", null);
		if (tvlaHome != null) path += ";" + tvlaHome;
	}
	
	/** Informs the user how to use the diff utiltiy and what its available options are.
	 */
	protected static void usage() {
		System.err.println("TVLA-2-alpha diff utility");
		System.err.println("Usage: tvla <program name> <reference tvs> <new tvs> [options]");
		System.err.println("options:");
		System.err.println("  -tvs <tvs diff file>      outputs differences in TVS format");
		System.err.println("  -dot <dot diff file>      outputs differences in DOT format");
		System.err.println("  -path <search path>       search path");
		
		System.exit(1);
	}
}
