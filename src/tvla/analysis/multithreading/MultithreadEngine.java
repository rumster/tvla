package tvla.analysis.multithreading;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.Engine;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.TVS;
import tvla.exceptions.AnalysisHaltException;
import tvla.io.IOFacade;
import tvla.language.TVM.ActionAST;
import tvla.logic.Kleene;
import tvla.predicates.LocationPredicate;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.Location;
import tvla.util.HashMapFactory;
import tvla.util.Logger;

/** An engine used for analyzing multi-threaded programs.
 * @author Eran Yahav
 */
public class MultithreadEngine extends Engine {
  
  /** status is to be updated every how many structures */
	private static final int STATUS_EVERY = 100;

	/** extreme detailed debug mode 	 */
	private static final boolean XDEBUG = false;
  
	/** @since 28.2.2000 moved out from the evaluate loop.
	 */
	protected StateSpace stateSpace = null;

	/** records maximal search stack depth for statistics*/
	protected int maxStackDepth = 0;
	/** records current search stack depth */
	protected int stackDepth = 0;

	/** the program is a HashMap of (label,Location) tuples
	 * each location has a set of corresponding actions
	 */
	private Map<String, Location> program = HashMapFactory.make();

	/** all threads in the program.
	 * Tuples of (threadName,ProgramThread)
	 */
	private Map<String, ProgramThread> programThreads = HashMapFactory.make();
	/**
	 * all methods in the program
	 * Tuples of (methodName -- fully qualified,ProgramMethodBody)
	 */
	private Map<String, ProgramMethodBody> programMethods = HashMapFactory.make();
	/**
	 * A list of global actions that are evaluated on every step of exploration
	 */
	private List<Action> globalActions = new ArrayList<Action>();
	/**
	 * the single global location
	 */
	private Location globalLocation = new Location("global", true);

	/**
	 * get name of thread entry label
	 * @param threadName - name of thread
	 * @return name of entry label for the thread
	 */
	public String getEntryLabel(String threadName) {
		ProgramThread progThread =
			programThreads.get(threadName);
		if (progThread != null) {
			return progThread.getEntryLabel();
		} else {
			throw new RuntimeException(
				"Thread type " + threadName + " does not exist");
		}
	}

	/**
	 * adds a global action to the program.
	 * @param action - action to be added
	 **/
	public void addAction(Action action) {
		globalActions.add(action);
		globalLocation.addAction(action, "global");
	}

	/**
	 * adds a thread definition to the program
	 * @param name - thread name
	 * @param actions - list of thread actions
	 * @return a new program thread for the name and actions
	 * */
	public ProgramThread addThreadDefinition(String name, List<ActionAST> actions) {
		ProgramThread progThread = programThreads.get(name);
		if (progThread == null) {
			// create a new program thread
			progThread = new ProgramThread(name, actions, program);
			// add thread actions to a "central" mapping from a label to a location.
			// this is done mainly to improve performance
			programThreads.put(name, progThread);
			program.putAll(progThread.getThreadActions());
			//addActions(progThread.getThreadActions());
		} else {
			throw new RuntimeException("Multiple definition of thread " + name);
		}
		return progThread;
	}

	/**
	 * add a new method definition to the program
	 * @param name - method name
	 * @param actions - method actions
	 * @return a new ProgramMethodBody for the method
	 */
	public ProgramMethodBody addMethodDefinition(String name, List<ActionAST> actions) {
		ProgramMethodBody progMethod =
			programMethods.get(name);
		if (progMethod == null) {
			// create a new program thread
			progMethod = new ProgramMethodBody(name, actions, program);
			// add method actions to a "central" mapping from a label to a location.
			// this is done mainly to improve performance
			programMethods.put(name, progMethod);

			program.putAll(progMethod.getActions());
			//addActions(progMethod.getActions());
		} else {
			throw new RuntimeException("Multiple definition of method " + name);
		}
		return progMethod;
	}

	/**
	 * compile the definition of a single thread
	 * @param name - thread name to be compiled
	 */
	public void compileThreadDefinition(String name) {
		ProgramThread progThread = programThreads.get(name);
		if (progThread == null) {
			throw new RuntimeException("Thread " + name + " does not exist");
		}
		progThread.compile();
		///addActions(progThread.getThreadActions());
	}

	/**
	 * compile a single method body definition
	 * @param name - name of method to be compiled
	 */
	public void compileBodyDefinition(String name) {
		ProgramMethodBody progMethod =
			programMethods.get(name);
		if (progMethod == null) {
			throw new RuntimeException("Method " + name + " does not exist");
		}

		progMethod.compile();
		///addActions(progMethod.getActions());
	}

	/**
	 * evaluate
	 * State space exploration
	 * @param initial - set of initial structures for the analysis
	 */
	public void evaluate(Collection<HighLevelTVS> initial) {
		/** the search stack is a stack of configurations (structures) */
		Stack<TVS> searchStack = new Stack<TVS>();
		stateSpace = new StateSpace("State_Space", true);
		init();

		///entryLocation = prepareProgram(entry);

		/**
		* initialize
		* for each s in Initial: push(s)
		*/
		for (TVS s : initial) {
			searchStack.push(s);
		}

		maxStackDepth = searchStack.size();

		/**
		* explore
		*/
		try {
			OUTER : while (!searchStack.isEmpty()) {
				// structure = pop()
				HighLevelTVS structure = (HighLevelTVS) searchStack.pop();
				if (XDEBUG) {
					Logger.println("Structure popped" + searchStack.size());
				}
				// if structure is not a member of the statespace
				status.startTimer(AnalysisStatus.JOIN_TIME);
				structure = stateSpace.join(structure);
				boolean delta = (structure != null);
				status.stopTimer(AnalysisStatus.JOIN_TIME);
				if (XDEBUG) {
					Logger.println("Delta exists =" + delta);
				}

				if (delta) {
					// add structure to the statespace
					// if delta != null structure was already added
					//stateSpace.join(delta);
					++status.numberOfStructures;			
					updateStatus();
					if (status.shouldFinishAnalysis()) {
						break OUTER;
					}

					// find set of enabled actions
					// global actions are always enabled, so they are initially
					// insereted into "enabledActions"
					List<Action> enabledActions = new ArrayList<Action>(globalActions);

					for (Iterator<Node> i = TVMCMacros.allThreadNodes(structure); i.hasNext();) {

						Node n = i.next();
						if (structure.eval(Vocabulary.ready, n) == Kleene.trueKleene) {
							// n is a ready thread, add its actions
							// iterate over location predicates	   
							for (LocationPredicate lp :	Vocabulary.locationPredicates) {
								if (structure.eval(lp, n) == Kleene.trueKleene) {
									enabledActions.addAll(lp.getLocation().getActions());
								}
							}
						}
					}

					// for each enabled action
					for (int actionIt = 0;
						actionIt < enabledActions.size();
						actionIt++) {
						Action action = enabledActions.get(actionIt);
						String label = action.location().label();

						// update Rumster's fields
						currentLocation = action.location();
						currentAction = action;

						Map<HighLevelTVS, Set<String>> messages = HashMapFactory.make(0);

						if (XDEBUG) {
							Logger.println(
								"Action precondition "
									+ action.getPrecondition());
						}

						Collection<HighLevelTVS> results =
							apply(action, structure, label, messages, null);
						// Replay the last action to show the user details of the failure.
						if (Engine.coerceAfterUpdateFailed) {
							boolean debug = AnalysisStatus.debug;
							AnalysisStatus.debug = true;
							apply(action, structure, label, messages, null);
							AnalysisStatus.debug = debug;
						}
						//System.out.println("<" + currentLocation + "," + currentAction + ">: " + results.size());
						if (XDEBUG) {
							Logger.println("Action Applied");
						}
						status.numberOfMessages
							+= stateSpace.addMessages(messages);
						if (status.shouldFinishAnalysis()) {
							break OUTER;
						}

						for (TVS result : results) {
							searchStack.push(result);
							if (XDEBUG) {
								Logger.println(
									"Structure pushed" + searchStack.size());
							}

							stackDepth = searchStack.size();
							if (stackDepth > maxStackDepth) {
								maxStackDepth = stackDepth;
							}
						} // end for results

					} // end for actions
				}
			}
		} catch (AnalysisHaltException ahe) {
			System.err.println(ahe.getMessage());
		}

		status.stopTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);

		printAnalysisInfo();
	}

	/** Prints all statistics and info for the current analysis run
	 *  (and current iteration of abstraction refinement).
	 * @author Alexey Loginov.
	 * @since 4.1.2004 Initial creation.
	 */
	public void printAnalysisInfo() {
		if (!AnalysisStatus.terse) {
			printStateSpaceStatistics();
		}

		statistics.doStatistics();
		printStatistics();

		// Printing the structures in the state space.
		IOFacade.instance().printAnalysisState(
			Collections.singleton(stateSpace));
	}

	/**
	 * updates analysis status
	 */
	protected void updateStatus() {
		if (status.numberOfStructures % statistics.statisticsEvery == 0) {
			statistics.doStatistics();
		}

		//if (status.numberOfStructures % status.gcEvery == 0) {
		//	System.gc();
		//}
		
		if (!AnalysisStatus.terse
			&& status.numberOfStructures % STATUS_EVERY == 0) {
			status.updateStatus();
			long currentMemory =
				Runtime.getRuntime().totalMemory()
					- Runtime.getRuntime().freeMemory();
			long deltaMemory = currentMemory - status.initialMemory;
			System.err.print(
				"\r"
					+ stateSpace.label()
					+ "		  "
					+ status.numberOfStructures
					+ "	"
					+ deltaMemory
					+ " "
					+ "		  ");
		}
	}

	/** Prints statistical information about the state space structures.
	 * @author Eran Yahav.
	 * @since 21.7.2001 Moved here from outside the evaluate method.
	 */
	protected void printStateSpaceStatistics() {
		Logger.println();
		if (!stateSpace.structures.isEmpty()) {
			Logger.print(
				stateSpace.label()
					+ ": "
					+ stateSpace.structures.size()
					+ " structures");
			int maxSatisfy = 0;
			for (TVS structure : stateSpace.structures) {
				int satisfy = 0;
				for (Predicate pred : Vocabulary.allUnaryPredicates()) {
					satisfy += structure.numberSatisfy(pred);
				}
				for (Predicate pred : Vocabulary.allBinaryPredicates()) {
					satisfy += structure.numberSatisfy(pred);
				}

				if (satisfy > maxSatisfy) {
					maxSatisfy = satisfy;
				}

			}
			Logger.println(" max graph=" + maxSatisfy + "");
		}
		Logger.println();
	}

	/** Gives the engine a chance to do some initializations.
	 * @author Roman Manevich
	 * @since 13.10.2001 Initial creation.
	 */
	public void init() {
		super.init();
		statistics = new SpaceStatistics(Collections.singleton(stateSpace));
		maxStackDepth = 0;
		stackDepth = 0;
		status.startTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);
	}

	/** Prints statistics gathered during the analysis to the log stream.
	 * @author Roman Manevich.
	 * @since 21.7.2001
	 */
	protected void printStatistics() {
		status.printStatistics();
		Logger.println("Maximal stack depth " + maxStackDepth);
	}
}