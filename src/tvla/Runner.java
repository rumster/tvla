package tvla;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import tvla.absRef.AbstractionRefinement;
import tvla.analysis.AnalysisStatus;
import tvla.analysis.Engine;
import tvla.analysis.IntraProcEngine;
import tvla.analysis.decompose.DecomposeAnalysisGraph;
import tvla.analysis.decompose.DecompositionIntraProcEngine;
import tvla.analysis.interproc.InterProcEngine;
import tvla.analysis.interproc.PastaRunner;
import tvla.analysis.multithreading.MultithreadEngine;
import tvla.analysis.multithreading.buchi.BuchiAutomaton;
import tvla.analysis.multithreading.buchi.MultithreadEngineBuchi;
import tvla.core.Constraints;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.TVSFactory;
import tvla.core.base.BaseBlur;
import tvla.core.base.BaseTVSCache;
import tvla.core.base.PredicateUpdater;
import tvla.core.base.SparseHashTVSSet;
import tvla.core.common.ModifiedPredicates;
import tvla.core.functional.FnUniverse;
import tvla.core.generic.AdvancedCoerceOld;
import tvla.core.generic.CoerceTVLA2;
import tvla.core.generic.ConcreteTVSSet;
import tvla.core.generic.EmbeddingBlur;
import tvla.core.generic.EvalFormula;
import tvla.core.generic.FilterNodes;
import tvla.core.generic.GenericBlur;
import tvla.core.generic.GenericCoerce;
import tvla.core.generic.GenericFocus;
import tvla.core.generic.GenericTVSSet;
import tvla.core.generic.GenericUpdate;
import tvla.core.generic.MultiConstraint;
import tvla.core.generic.TVSHashFunc;
import tvla.differencing.Differencing;
import tvla.differencing.FormulaDifferencing;
import tvla.exceptions.AbstractionRefinementException;
import tvla.exceptions.ExceptionHandler;
import tvla.exceptions.UserErrorException;
import tvla.io.IOFacade;
import tvla.language.BUC.BUCParser;
import tvla.language.BUC.BuchiAutomatonAST;
import tvla.language.TVM.TVMAST;
import tvla.language.TVM.TVMParser;
import tvla.language.TVP.ActionMacroAST;
import tvla.language.TVP.SetDefAST;
import tvla.language.TVP.TVPParser;
import tvla.language.TVS.TVSParser;
import tvla.predicates.DynamicVocabulary;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.AnalysisGraph;
import tvla.util.Logger;
import tvla.util.ProgramProperties;
import tvla.util.PropertiesEx;
import tvla.util.StringUtils;

/** This class represents a command-line interface for TVLA users.
 * @author Tal Lev-Ami
 * @author Roman Manevich
 */
public class Runner {
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
	private static String versionInfo;
	
	static {
		PropertiesEx props = new PropertiesEx("/tvla/version.properties");
		versionInfo = props.getProperty("version", "Unknown TVLA version");
	}

	/** Main entry point of TVLA.
	 * @author Tal Lev-Ami 
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {

		try {
			loadProgramProperties(args);
			parseArgs(args);
			initProgramProperties(args);
			
		    //if (!AnalysisStatus.terse)
		      //  System.out.println( StringUtils.addUnderline(versionInfo) );
			
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
			
			// Sets the search path fo the pre-processor
			String pathString = engineType.equals("ddfs") ? propertyName : programName;
			int lastSep = pathString.lastIndexOf(File.separator);
			if (lastSep >=0)
				searchPath += ";" + args[0].substring(0, lastSep+1);
			ProgramProperties.setProperty("tvla.searchPath", searchPath);

			printLogHeader(args);
			if (ProgramProperties.getBooleanProperty("tvla.log.addPropertiesToLog", false)) {
				String header = StringUtils.addUnderline("TVLA Properties");
				ProgramProperties.list(Logger.getUnderlyingStream(), header);
			}
			// Pasta Dispatch point
			if (engineType.equals("pasta")){
				PastaRunner.go(programName,searchPath,inputFile);
				return;
			}
			if (!AnalysisStatus.terse)
				Logger.print(StringUtils.newLine + "Loading specification ... ");
			AnalysisStatus.loadTimer.start();

			if (engineType.equals("tvla")) {
				AnalysisGraph.activeGraph = new AnalysisGraph();
				TVPParser.configure(programName, searchPath);
				AnalysisGraph.activeGraph.init();
				IOFacade.instance().printProgram(AnalysisGraph.activeGraph);
				//System.out.println("Allocated objects: " + tvla.language.TVP.AST.allocated());
				//System.out.println("Allocated ActionMacro objects: " + tvla.language.TVP.ActionMacroAST.allocated());
				//System.out.println("Allocated ActionDef objects: " + tvla.language.TVP.ActionDefAST.allocated());
			} 
			else if (engineType.equals("tvmc")) {
				TVMAST tvmFile = TVMParser.configure(programName, searchPath);
				tvmFile.compileAll();
			}
			else if (engineType.equals("ddfs")) {
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
            else if (engineType.equals("dtvla")) {
                AnalysisGraph.activeGraph = new DecomposeAnalysisGraph();
                TVPParser.configure(programName, searchPath);
                AnalysisGraph.activeGraph.init();
                IOFacade.instance().printProgram(AnalysisGraph.activeGraph);
                //System.out.println("Allocated objects: " + tvla.language.TVP.AST.allocated());
                //System.out.println("Allocated ActionMacro objects: " + tvla.language.TVP.ActionMacroAST.allocated());
                //System.out.println("Allocated ActionDef objects: " + tvla.language.TVP.ActionDefAST.allocated());
            } 
			else {
				throw new UserErrorException("An invalid engine was specified: " + engineType);
			}			

			if (!AnalysisStatus.terse)
				Logger.println("done"); // done loading the specification
						
			TVSFactory.getInstance().init();
			if (!AnalysisStatus.terse)
				Logger.print("Reading TVS files ... ");
			Collection<HighLevelTVS> initial = TVSParser.readStructures(inputFile);
			AnalysisStatus.loadTimer.stop();
			if (!AnalysisStatus.terse)
				Logger.println("done");
			
			// Perform finite differencing.
			tvla.differencing.Differencing.differencing();

			// Create a new abstraction refinement object. 
			AbstractionRefinement absRef = new AbstractionRefinement(engineType, inputFile, searchPath);
			AbstractionRefinementException arException = null;
			
			// Coerce initial set
			Engine.activeEngine.prepare(initial);
			
			// Construct abstract input from scratch, if requested.
			absRef.constructAbstractInput(initial);
			do {
                try {
                    // Perform the fixpoint finding analysis.
                    if (!AnalysisStatus.terse)
                        System.out.println(StringUtils.newLine + "Starting analysis ...");
                    Engine.activeEngine.evaluate(initial);                    

                    arException = null;
                } 
                catch (AbstractionRefinementException e) {
                    Engine.activeEngine.stopTimers();
                    // Dump statistics after every refinement iteration.
                    Engine.activeEngine.printAnalysisInfo();
                    arException = e;
                } 
                finally {
					/** Note that you may get an exception in CanonicNodeSet having to do with
					 *  an attempt to normalize the structure in the dump() call.  If so, either
					 *  set tvla.dot.normalizeStructures and tvla.tvsOutput.normalizeStructures
					 *  to false or move the two graph dump lines into the try block above. */
					// Dump graph and statistics after every refinement iteration.
				    if (ProgramProperties.getBooleanProperty("tvla.log.implementationSpecificStatistics", false))
					    TVSFactory.printStatistics();

  					if (AnalysisGraph.activeGraph != null)
							AnalysisGraph.activeGraph.dump(); // this has no effect for multithreaded engines

						// Clear the static load timer after load time gets reported once.
						//if (Engine.activeEngine.getTransitionRelation() != null)
							//Engine.activeEngine.getTransitionRelation().dump();

						AnalysisStatus.loadTimer = new tvla.util.Timer();
                    String shell = ProgramProperties.getProperty("tvla.beanshell", null);
                    if (shell != null) {
                        Class.forName(shell).getMethod("main", String[].class).invoke(null, new Object[] {new String[0]});
                        return;
                    }
				}
			} while (absRef.refineIfImprecise(initial, arException));

//			int totalMeets = tvla.core.meet.Meet.totalNumberOfTvsMeets;
//			int successfullMeets = tvla.core.meet.Meet.successfullTvsMeets;
//			successfullMeets = successfullMeets == 0 ? 1 : successfullMeets; // avoid division by zero
//			float ratio = (float)totalMeets / (float)successfullMeets;
//			System.out.println("#meet applications: " + totalMeets);
//			System.out.println("#successful meet applications: " + successfullMeets);
//			System.out.println("ratio: " + ratio);

			if (!AnalysisStatus.terse)
				System.out.println(StringUtils.newLine + "All tasks completed");
		}
		catch (Throwable t) {
			ExceptionHandler.instance().handleException(t);
			System.exit(-1);
		}
	}
	
	/**
	 * Resets global variables to allow TVLA to be run again.
	 */
	public static void reset() {
		ProgramProperties.reset();
		programName = null;
		inputFile = null;
		propertyName = null;
		specificPropertiesFiles = new ArrayList<String>();
		engineType = null;
		searchPath = null;
		PropertiesEx props = new PropertiesEx("/tvla/version.properties");
				versionInfo = props.getProperty("version", "Unknown TVLA version");
				
		Node.reset();
		FnUniverse.reset();
		MultiConstraint.reset();
		HighLevelTVS.reset();
		Constraints.reset();
		CoerceTVLA2.reset();
		AdvancedCoerceOld.reset();
		Vocabulary.reset();
		TVSFactory.reset();
		BaseTVSCache.reset();
		AnalysisStatus.reset();
		BaseBlur.reset();
		PredicateUpdater.reset();
		SparseHashTVSSet.reset();
		ModifiedPredicates.reset();
		ConcreteTVSSet.reset();
		EmbeddingBlur.reset();
		EvalFormula.reset();
		FilterNodes.reset();
		GenericBlur.reset();
		GenericCoerce.reset();
		GenericFocus.reset();
		GenericTVSSet.reset();
		GenericUpdate.reset();
		TVSHashFunc.reset();
		Differencing.reset();
		FormulaDifferencing.reset();
		ActionMacroAST.reset();
		SetDefAST.reset();
		DynamicVocabulary.reset();
		Predicate.reset();
		Action.reset();
		AnalysisGraph.reset();
		IOFacade.reset();
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

		System.err.println(" -d                      Turns on debug mode.");
		System.err.println(" -action [f][c]pu[c]b    Determines the order of operations computed");
		System.err.println("                         by an action. The default is fpucb.");
		System.err.println("                         f - Focus,  c - Coerce, p - Precondition.");
		System.err.println("                         u - Update, b - Blur.");
		System.err.println(" -join [algorithm]       Determines the type of join method to apply.");
		System.err.println("                         rel  - Relational join.");
		System.err.println("                         part - Partial join.");
		System.err.println("                         ind  - Independent attributes (single structure).");
		System.err.println("                         conc - Concrete join (no abstraction).");
		System.err.println(" -ms <number>            Limits the number of structures.");
		System.err.println(" -mm <number>            Limits the number of messages.");
		System.err.println(" -save {back|ext|all}    Determines which locations store structures.");
		System.err.println("                         back - at every back edge (the default).");
		System.err.println("                         ext  - at every beginning of an extended block.");
		System.err.println("                         all  - at every program location.");		
		System.err.println(" -noautomatic            Supresses generation of automatic constraints.");
		System.err.println(" -props <file name>      Can be used to specify a properties file.");
		System.err.println(" -log <file name>        Creates a log file of the execution.");
		System.err.println(" -tvs <file name>        Creates a TVS formatted output.");
		System.err.println(" -xml <file name>        Creates a XML formatted output of the program CFG.");
		System.err.println(" -tr:tvs <file name>     Creates a transition relation output in tvs-like format.");
		System.err.println(" -dot <file name>        Creates a DOT formatted output.");
		System.err.println(" -tr:dot <file name>     Creates a transition relation output in dot format.");
		System.err.println(" -td                     Enables termination analysis.");
		System.err.println(" -td.dot  <dir name>     Enables termination analysis, and saves the debug info to <dir name>.");
		System.err.println(" -td.summarizationOff    Disables loops summarization during termination analysis.");
		System.err.println(" -td.verbose             Enables termination analysis verbose mode.");
		System.err.println(" -D<macro name>[(value)] Defines a C preprocessor macro.");
		System.err.println(" -terse                  Turns off on-line information printouts.");
		System.err.println(" -nowarnings             Causes all warnings to be ignored.");
		System.err.println(" -path <directory path>  Can be used to specify a search path.");
		System.err.println(" -post                   Post order evaluation of actions.");
		System.exit(0);
	}

	/** Parses the arguments passed from the command-line.
	 */
	static protected void parseArgs(String args[]) throws Exception {
		int i = 0;
		
		// Detect calls to the diff utility
		if (args.length >=1 && args[0].equals("-diff")) {
			String newArgs [] = new String[args.length-1];
			System.arraycopy(args, 1, newArgs, 0, args.length-1);
			tvla.diffUtility.Runner.main(newArgs);
			System.exit(0);
		}

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
			if (args[i].equals("-mode")) {
				i++;
				if (i<args.length)
					ProgramProperties.setProperty("tvla.engine.type", args[i]);
				continue;
			}
			if (args[i].equals("-d"))
				ProgramProperties.setProperty("tvla.debug", "true");
			else if (args[i].equals("-noautomatic"))
				ProgramProperties.setProperty("tvla.generateAutomaticConstraints", "false");
			else if (args[i].equals("-post"))
				ProgramProperties.setProperty("tvla.cfg.postOrder", "true");
			else if (args[i].equals("-join")) {
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
			} else if (args[i].equals("-backward")) {
				ProgramProperties.setProperty("tvla.cfg.backwardAnalysis", "true");
			} else if (args[i].startsWith("-D")) {
				if (args[i].length() > 2) {
					String macro = args[i].substring(2, args[i].length());
					ProgramProperties.appendToStringListProperty("tvla.parser.externalMacros", macro);
				}
			} else if (args[i].equals("-save")) {
				i++;
				ProgramProperties.setProperty("tvla.cfg.saveLocations", args[i]);
				if (args[i] == null || 
					(!args[i].equals("back") && !args[i].equals("ext") && !args[i].equals("all"))) {
					System.err.println("Invalid save option specified: " + args[i] + "!");
					usage();
				}					
			} else if (args[i].equals("-ms")) {
				i++;
				if (i >= args.length) {
					System.err.println("Missing argument after -ms!");
					usage();
				}
				ProgramProperties.setProperty("tvla.engine.maxStructures", args[i]);
			} else if (args[i].equals("-mm")) {
				i++;
				if (i >= args.length) {
					System.err.println("Missing argument after -mm!");
					usage();
				}
				ProgramProperties.setProperty("tvla.engine.maxMessages", args[i]);
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
			} else if (args[i].equals("-tr:tvs")) {
				i++;
				if (i >= args.length) {
					System.err.println("Missing argument after -tr:tvs!");
					usage();
				}
				ProgramProperties.setProperty("tvla.tr.tvs.outputFile", args[i] +".tr");
				ProgramProperties.setBooleanProperty("tvla.output.redirectToDirectory", false);
				ProgramProperties.setBooleanProperty("tvla.tr.enabled", true);
				ProgramProperties.setBooleanProperty("tvla.tr.tvs.enabled", true);
				ProgramProperties.setBooleanProperty("tvla.tvs.enabled", true);
			}
			else if (args[i].equals("-dot")) {
				i++;
				if (i >= args.length) {
					System.err.println("Missing argument after -dot!");
					usage();
				}
				ProgramProperties.setProperty("tvla.dot.outputFile", args[i]);
				ProgramProperties.setBooleanProperty("tvla.output.redirectToDirectory", false);
				ProgramProperties.setBooleanProperty("tvla.dot.enabled", true);
			} 
			else if (args[i].equals("-tr:dot")) {
				i++;
				if (i >= args.length) {
					System.err.println("Missing argument after -tr:dot!");
						usage();
				}
				ProgramProperties.setProperty("tvla.tr.dot.outputFile", args[i] +".tr.dt");
				ProgramProperties.setBooleanProperty("tvla.output.redirectToDirectory", false);
				ProgramProperties.setBooleanProperty("tvla.tr.enabled", true);
				ProgramProperties.setBooleanProperty("tvla.tr.dot.enabled", true);
				ProgramProperties.setBooleanProperty("tvla.dot.enabled", true);
			}
			else if (args[i].equals("-td")) {
				ProgramProperties.setBooleanProperty("tvla.td.enabled", true);
			}
			else if (args[i].equals("-td.dot")) {
				i++;
				if (i >= args.length) {
					System.err.println("Missing argument after -td:dot!");
					usage();
				}

				ProgramProperties.setBooleanProperty("tvla.td.enabled", true);
				ProgramProperties.setProperty("tvla.td.dot", args[i]);
			}
			else if (args[i].equals("-td.summarizationOff")) {
				ProgramProperties.setBooleanProperty("tvla.td.summarizationOff", true);
			}
			else if (args[i].equals("-td.verbose")) {
				ProgramProperties.setBooleanProperty("tvla.td.verbose", true);
			}
			else if (args[i].equals("-terse")) {
				ProgramProperties.setProperty("tvla.terse", "true");
			} else if (args[i].equals("-nowarnings")) {
				ProgramProperties.setProperty("tvla.emitWarnings", "false");
			} else if (args[i].equals("-props")) {
				++i;
			} else if (args[i].equals("-path")) {
				i++;
				if (i >= args.length) {
					System.err.println("Missing argument after -path!");
					usage();
				}
				ProgramProperties.setProperty("tvla.parser.searchPath", args[i]);
			} else if (args[i].equals("-action")) {
				i++;
				if (i >= args.length) {
					System.err.println("Missing argument after -action!");
					usage();
				}
				ProgramProperties.setProperty("tvla.engine.actionOrder", args[i]);
			}
			else {
				System.err.println("Unknown option " + args[i] + "!");
				System.err.println();
				usage();
			}
		}
	}
	
	/** Resolves the mode/engine from the file extensions.
	 */
	protected static void resolveMode() {
		boolean autoResolve = ProgramProperties.getBooleanProperty("tvla.engine.autoResolveType", true);
		engineType = ProgramProperties.getProperty("tvla.engine.type", "tvla");		
		if (autoResolve) {		
			File tvp = new File(programName + ".tvp");
			File tvm = new File(programName + ".tvm");
			File buc = new File(propertyName + ".buc");
			File pts = new File(programName + ".pts");
			if (tvp.exists())
				engineType = "tvla";
			if (tvm.exists())
				engineType = "tvmc";
			if (buc.exists())
				engineType = "ddfs";
			if (pts.exists())
				engineType = "pasta";
            
            ProgramProperties.setProperty("tvla.engine.type", engineType);  
		}
	}

	/** Prints information regarding to the analysis mode.
	 * @author Roman Manevich.
	 * @since 30.7.2001 Initial creation.
	 */
	protected static void printLogHeader(String [] args) {
		if (!ProgramProperties.getProperty("tvla.log.logFileName", "null").equals("null"))
			Logger.print(versionInfo + " ");
		
		if (engineType.equals("tvla")) {
			Logger.println("Running TVP mode");
		}
        else if (engineType.equals("dtvla")) {
            Logger.println("Running Decomposing TVP mode");
        }
		else if (engineType.equals("tvmc")) {
			Logger.println("Running TVM mode");
		}
		else if (engineType.equals("ddfs")) {
			Logger.println("Running TVM mode with double-DFS");
		}
		else if (engineType.equals("pasta")) {
			Logger.println("Running PASTA mode");
		}
		else {
			throw new UserErrorException("An invalid engine was specified: " + engineType);
		}
		
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

		resolveMode();
		
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
		
		if (ProgramProperties.getBooleanProperty("tvla.tr.enabled", true)) {
			// determines the name of the tr output file in tvs format
			if (!optionSpecified(args, "-tr:tvs") && 				
				ProgramProperties.getBooleanProperty("tvla.tr.tvs.enabled", true) &&
				tvpName != null) {
					// FIXME int index = tvpName.indexOf(".tvp");
					outputFile = ProgramProperties.getProperty("tvla.tr.tvs.outputFile", "null");
					if (outputFile.equals("null"))
						outputFile = progName + ".tvs.tr";
					ProgramProperties.setProperty("tvla.tr.tvs.outputFile", outputFile);				 
			}

			// determines the name of the tr output file in dot format
			if (!optionSpecified(args, "-tr:dot") && 
				ProgramProperties.getBooleanProperty("tvla.tr.dot.enabled", true) &&
				tvpName != null) {
				outputFile = ProgramProperties.getProperty("tvla.tr.dot.outputFile", "null");
				if (outputFile.equals("null"))
					outputFile = progName + ".tr.dt";
				ProgramProperties.setProperty("tvla.tr.dot.outputFile", outputFile);				 
			}

			// determines the name of the tr output file in xml format
			if (!optionSpecified(args, "-tr:xml") && 
				ProgramProperties.getBooleanProperty("tvla.tr.xml.enabled", true) &&
				tvpName != null) {
				outputFile = ProgramProperties.getProperty("tvla.tr.xml.outputFile", "null");
				if (outputFile.equals("null"))
					outputFile = progName + ".xml.tr";
				ProgramProperties.setProperty("tvla.tr.xml.outputFile", outputFile);				 
			}
		
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
		String TVLA_HOME = System.getenv("TVLA_HOME");

		String homePath = tvlaHome != null ? tvlaHome : TVLA_HOME;

		// If failed try to deduce it from the class path
		if (homePath == null) {
			String classPath = System.getProperty("java.class.path");
			File tvlaHomeFile = new File(classPath);
			if (tvlaHomeFile.isFile()) { // a .jar file
				String fileName = tvlaHomeFile.getName();
				int i = classPath.indexOf(fileName);
				homePath = classPath.substring(0, i);
			} else {
				homePath = classPath;
			}
		}

		return homePath;
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