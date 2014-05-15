package tvla.analysis;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import tvla.core.Constraints;
import tvla.core.HighLevelTVS;
import tvla.core.TVS;
import tvla.core.TVSFactory;
import tvla.core.generic.BoundedStructEmbeddingTest;
import tvla.core.meet.Meet;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.AnalysisGraph;
import tvla.transitionSystem.Location;
import tvla.util.HashMapFactory;
import tvla.util.Logger;
import tvla.util.Pair;
import tvla.util.ProgramProperties;
import tvla.util.StringUtils;

/**
 * This class represents a engine that applies a fixed-point iterative algorithm
 * to a program specified by the TVP file and program inputs specified by the
 * TVS file.
 * 
 * @author Tal Lev-Ami
 */
public class IntraProcEngine extends Engine {
	protected AnalysisGraph cfg;
	protected int numberOfIterations;
	protected int prevUpdate = Integer.MIN_VALUE;
	protected int maxWorkSetSize;
	protected int averageWorkSetSize;
	protected boolean maintainTransitionRelation = false;
	protected boolean postHocTransitionRelation = false;
	protected boolean dynamicTransitionRelation = false;

	/**
	 * Constructs and initializes an intra-procedural engine.
	 */
	public IntraProcEngine() {
		super();
	}

	class StructureInGraph extends Pair<Location, HighLevelTVS> implements
			Comparable<Pair<Location, HighLevelTVS>> {
		public StructureInGraph(Location l, HighLevelTVS s) {
			super(l, s);
		}

		public int compareTo(Pair<Location, HighLevelTVS> o) {
			StructureInGraph other = (StructureInGraph) o;
			int i = second.compareTo(other.second);
			if (i != 0)
				return i;
			i = first.compareTo(other.first);
			if (i != 0)
				return i;
			return second.hashCode() - other.second.hashCode();
		}
	};

	public void evaluate(Collection<HighLevelTVS> initial) {
		// List<Pair<HighLevelTVS, HighLevelTVS>> mergeMap = new
		// ArrayList<Pair<HighLevelTVS, HighLevelTVS>>(2); // used to construct
		// the transition relation

		this.cfg = AnalysisGraph.activeGraph;
		init();
		status.numberOfStructures = initial.size();

		// Joining the input structures and putting them in the entry location.
		Location entryLocation = cfg.getEntryLocation();
		cfg.storeStructures(entryLocation, initial);

		// if (maintainTransitionRelation) {
		// Iterator<HighLevelTVS> structItr = entryLocation.allStructures();
		// while (structItr.hasNext()) {
		// TVS tvs = structItr.next();
		// ((IntraProcTransitionRelation)
		// transitionRelation).addAbstractState(entryLocation,tvs);
		// }
		// }

		SortedSet<Location> workSet = new TreeSet<Location>();
		workSet.add(cfg.getEntryLocation());
		OUTER: while (!workSet.isEmpty()) {
			++numberOfIterations;
			maxWorkSetSize = maxWorkSetSize < workSet.size() ? workSet.size()
					: maxWorkSetSize;
			averageWorkSetSize += workSet.size();

			Iterator<Location> first = workSet.iterator();
			currentLocation = first.next();
			first.remove();
			if (!AnalysisStatus.terse)
				System.err.print("\r" + currentLocation.label() + "    ");

			Collection<HighLevelTVS> unprocessed = ((Location) currentLocation)
					.removeUnprocessed();
			for (int actionIt = 0; actionIt < ((Location) currentLocation)
					.getActions().size(); actionIt++) {
				currentAction = ((Location) currentLocation)
						.getAction(actionIt);
				String target = ((Location) currentLocation)
						.getTarget(actionIt);
				Location nextLocation = cfg.getLocationByLabel(target);

				for (Iterator<HighLevelTVS> structureIt = unprocessed
						.iterator(); structureIt.hasNext();) {
					HighLevelTVS structure = structureIt.next();

					if (actionIt == ((Location) currentLocation).getActions()
							.size() - 1)
						structureIt.remove();
					Map<HighLevelTVS, Set<String>> messages = HashMapFactory
							.make(0);
					Collection<HighLevelTVS> results = apply(currentAction,
							structure, currentLocation.label(), messages);
					// Replay the last action to show the user details of the
					// failure.
					if (Engine.coerceAfterUpdateFailed
							|| (!messages.isEmpty() && hasPostMessages(messages))) {
						boolean debug = AnalysisStatus.debug;
						AnalysisStatus.debug = true;
						apply(currentAction, structure,
								currentLocation.label(), messages);
						AnalysisStatus.debug = debug;
					}
					status.numberOfMessages += ((Location) currentLocation)
							.addMessages(messages);

					// if (!maintainTransitionRelation) {
					for (HighLevelTVS result : results) {
						status.startTimer(AnalysisStatus.JOIN_TIME);
						boolean needJoin = (nextLocation.join(result) != null);
						status.stopTimer(AnalysisStatus.JOIN_TIME);

						if (needJoin) {
							workSet.add(nextLocation);
							++status.numberOfStructures;
							updateStatus();
							if (status.shouldFinishAnalysis())
								break OUTER;
						}
					}
					// }
					// else { // transition relation stuff
					// if (!messages.isEmpty())
					// ((IntraProcTransitionRelation) transitionRelation).
					// addMessage(currentLocation, structure, currentAction,
					// messages);
					//
					// for (HighLevelTVS result : results) {
					// mergeMap.clear();
					// status.startTimer(AnalysisStatus.JOIN_TIME);
					// boolean changedState =
					// nextLocation.join(result,mergeMap);
					// status.stopTimer(AnalysisStatus.JOIN_TIME);
					//
					// updateTransitionRelation(
					// (Location) currentLocation,
					// currentAction,
					// nextLocation,
					// structure,
					// result,
					// mergeMap,
					// changedState);
					//
					// if (changedState) {
					// // Noam: The if is commented out in order to be
					// compatible with the version
					// // that does not create a transition relation
					// // if (resultInserted)
					// ++status.numberOfStructures;
					//
					// workSet.add(nextLocation);
					//
					// updateStatus();
					// if (status.shouldFinishAnalysis())
					// break OUTER;
					// }
					// }
					// }

					if (status.shouldFinishAnalysis())
						break OUTER;
				}
				// nextLocation.compress(); // clear the canonic maps in the
				// location's structures
			}
		}

		if (ProgramProperties.getBooleanProperty(
				"tvla.engine.checkMessagesAtFixpoint", false))
			evaluateMessagesAtFixpoint();
		status.stopTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);

		if (maintainTransitionRelation) {
			postHocTransitionRelation();
		}

		printAnalysisInfo();
		String stablePredStr = ProgramProperties.getProperty(
				"tvla.stableSuffixAnalysis", "");
		if (!stablePredStr.equals("")) {
			Predicate marker = Vocabulary.getPredicateByName(stablePredStr);
			if (marker == null)
				throw new Error(
						"Predicate "
								+ stablePredStr
								+ " specified in property tvla.stableSuffixAnalysis was not specified!");
			cfg.constructIncoming();
			Collection<Location> markedLocations = cfg.findLatestLocations(
					marker, Kleene.trueKleene);
			try {
				FileWriter fw = new FileWriter("stable_suffix.txt");
				for (Location loc : markedLocations)
					fw.write(loc.label() + StringUtils.newLine);
				fw.close();
			} catch (IOException e) {
				throw new Error(e.getMessage());
			}
		}
	}

	protected void postHocTransitionRelation() {
		// First, create all transition relation nodes.
		for (Location loc : cfg.getLocations()) {
			for (TVS tvs : loc.structures) {
				((IntraProcTransitionRelation) transitionRelation)
						.addAbstractState(loc, tvs);
			}
		}

		// Second, create all transition relation edges.
		for (Location currentLocation : cfg.getLocations()) {
			for (HighLevelTVS tvs : currentLocation.structures) {
				for (int actionIt = 0; actionIt < ((Location) currentLocation)
						.getActions().size(); actionIt++) {
					currentAction = ((Location) currentLocation)
							.getAction(actionIt);
					String target = ((Location) currentLocation)
							.getTarget(actionIt);
					Location nextLocation = cfg.getLocationByLabel(target);

					Map<HighLevelTVS, Set<String>> messages = HashMapFactory
							.make(0);
					Collection<HighLevelTVS> results = apply(currentAction,
							tvs, currentLocation.label(), messages);

					if (!messages.isEmpty())
						((IntraProcTransitionRelation) transitionRelation)
								.addMessage(currentLocation, tvs,
										currentAction, messages);

					for (HighLevelTVS resultTVS : results) {
						// Now, find all structures stored in 'nextLocation'
						// that embed 'resultTVS'. There must be at least
						// one.
						int numEmbeddings = 0;
						for (HighLevelTVS tvsInTarget : nextLocation.structures) {
							// The following test is rather expensive as it
							// does not take advantage of the fact that both
							// structures are bounded.
							if (// Meet.isEmbedded(resultTVS, tvsInTarget)
							BoundedStructEmbeddingTest.isEmbedded(resultTVS,
									tvsInTarget)) {
								++numEmbeddings;
								((IntraProcTransitionRelation) transitionRelation)
										.addAbstractTransition(currentLocation,
												tvs, nextLocation, tvsInTarget,
												currentAction);
							}
						}
						assert numEmbeddings > 0;
					}
				}
			}
		}
	}

	public void evaluateMessagesAtFixpoint() {
		status.numberOfMessages = 0;
		for (Location loc : cfg.getLocations()) {
			loc.clearMessages();
			for (int actionIt = 0; actionIt < loc.getActions().size(); actionIt++) {
				Action currentAction = loc.getAction(actionIt);
				if (currentAction.getMessages().isEmpty())
					continue;
				int numMessages = currentAction.getMessages().size();
				if (numMessages > 1) {
					System.out.println("Found " + numMessages + " at "
							+ loc.label());
				}
				for (HighLevelTVS structure : loc.structures) {
					Map<HighLevelTVS, Set<String>> messages = HashMapFactory
							.make(0);
					apply(currentAction, structure, loc.label(), messages);
					// Replay the last action to show the user details of the
					// failure.
					status.numberOfMessages += loc.addMessages(messages);
				}
			}
		}
	}

	protected static boolean hasPostMessages(
			Map<HighLevelTVS, Set<String>> messages) {
		boolean result = false;
		for (Collection<String> msgs : messages.values()) {
			for (String message : msgs) {
				if (message.contains("post"))
					return true;
			}
		}
		return result;
	}

	/**
	 * Updates the transition relation according to a specific structure,
	 * action, and result structure. returns true if the new structure was
	 * inserted to the set at the nextLocation
	 */

	public boolean updateTransitionRelation(Location currentLocation,
			Action currentAction, Location nextLocation, TVS structure,
			TVS result, Collection<Pair<HighLevelTVS, HighLevelTVS>> mergeMap,
			boolean stateChanged) {
		if (!stateChanged) {
			// A TVS embedding result is already in the abstract state.
			// We only need to add a transition to the transition relation.
			assert mergeMap.size() == 1 : "mergeMap.size()==" + mergeMap.size()
					+ " expected to be 1!";
			Pair<HighLevelTVS, HighLevelTVS> pair = mergeMap.iterator().next();
			assert (pair.first == result);
			assert (pair.second != result);
			TVS embeddingTVS = pair.second;
			((IntraProcTransitionRelation) transitionRelation)
					.addAbstractTransition(currentLocation, structure,
							nextLocation, embeddingTVS, currentAction);

			return false;
		}

		Iterator<Pair<HighLevelTVS, HighLevelTVS>> pairItr = mergeMap
				.iterator();
		boolean resultInserted = true;
		while (pairItr.hasNext()) {
			Pair<HighLevelTVS, HighLevelTVS> pair = pairItr.next();
			TVS embeddedTVS = pair.first;
			TVS embeddingTVS = pair.second;

			assert (result != embeddingTVS);

			// ROMAN: trying to fix a bug
			((IntraProcTransitionRelation) transitionRelation)
					.addAbstractState(nextLocation, embeddingTVS);

			if (result == embeddedTVS) {
				resultInserted = false;
				((IntraProcTransitionRelation) transitionRelation)
						.addAbstractTransition(currentLocation, structure,
								nextLocation, embeddingTVS, currentAction);
			} else {
				// Never happens in relational + partial join
				assert (false);
				((IntraProcTransitionRelation) transitionRelation)
						.mergeAbstractStates(currentLocation, embeddedTVS,
								nextLocation, embeddingTVS);
			}

		}

		if (resultInserted) {
			((IntraProcTransitionRelation) transitionRelation)
					.addAbstractState(nextLocation, result);
			((IntraProcTransitionRelation) transitionRelation)
					.addAbstractTransition(currentLocation, structure,
							nextLocation, result, currentAction);
		}

		return resultInserted;
	}

	/**
	 * Prints all statistics and info for the current analysis run (and current
	 * iteration of abstraction refinement).
	 * 
	 * @author Alexey Loginov.
	 * @since 4.1.2004 Initial creation.
	 */
	public void printAnalysisInfo() {
		Logger.println("\nAnalysis finished.");
		Logger.println("Preparing statistics ...");
		statistics.doStatistics();
		printLocationsInfo();
		printStatistics();
	}

	/**
	 * Gives the engine a chance to do some initializations.
	 * 
	 * @author Roman Manevich
	 * @since 13.10.2001 Initial creation.
	 */
	public void init() {
		super.init();

		if (cfg == null) {
			cfg = AnalysisGraph.activeGraph;
		}

		maintainTransitionRelation = ProgramProperties.getBooleanProperty(
				"tvla.tr.enabled", false);

		if (maintainTransitionRelation) {
			postHocTransitionRelation = ProgramProperties.getBooleanProperty(
					"tvla.tr.posthoc", false);
			dynamicTransitionRelation = ProgramProperties.getBooleanProperty(
					"tvla.tr.dynamic", false);

			Collection<Location> locations = cfg.getLocations();
			transitionRelation = new IntraProcTransitionRelation(
					locations.size());
			for (Location loc : locations) {
				((IntraProcTransitionRelation) transitionRelation)
						.addLocation(loc);
			}
		}

		// Initialize
		for (Location location : cfg.getLocations()) {
			for (Action action : location.getActions()) {
				action.init();
			}
		}

		statistics = new SpaceStatistics(cfg.getLocations());
		status.startTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);
	}

	/**
	 * Prints statistics gathered during the analysis to the log stream.
	 * 
	 * @author Roman Manevich.
	 * @since 21.7.2001
	 */
	protected void printStatistics() {
		if (numberOfIterations != 0)
			averageWorkSetSize = averageWorkSetSize / numberOfIterations;
		int numNullaryAbsPredicates = 0;
		int numUnaryAbsPredicates = 0;
		for (Predicate predicate : Vocabulary.allNullaryPredicates()) {
			if (predicate.abstraction())
				++numNullaryAbsPredicates;
		}
		for (Predicate predicate : Vocabulary.allNullaryPredicates()) {
			if (predicate.abstraction())
				++numUnaryAbsPredicates;
		}

		Logger.println();
		Logger.println("max work set               : " + maxWorkSetSize);
		Logger.println("average work set           : " + averageWorkSetSize);
		Logger.println("#iterations                : " + numberOfIterations);

		Logger.println("#locations                 : "
				+ cfg.getLocations().size());
		Logger.println("#actions                   : "
				+ cfg.getNumberOfActions());
		Logger.println("#predicates                : " + Vocabulary.size());
		Logger.println("#nullary predicates        : "
				+ Vocabulary.allNullaryPredicates().size());
		Logger.println("#nullary abs predicates    : "
				+ numNullaryAbsPredicates);
		Logger.println("#unary predicates          : "
				+ Vocabulary.allUnaryPredicates().size());
		Logger.println("#unary abs predicates      : " + numUnaryAbsPredicates);
		Logger.println("#binary predicates         : "
				+ Vocabulary.allBinaryPredicates().size());
		Logger.println("#constraints               : "
				+ Constraints.getInstance().constraints().size());
		Logger.println();

		AnalysisStatus.exhaustiveGC();
		status.updateStatus();
		status.printStatistics();
	}

	/**
	 * Prints information resulting from the algorithm's evaluation. The output
	 * is directed to the log file.
	 * 
	 * @since 8.12.2000
	 */
	protected void printLocationsInfo() {
		Logger.println();
		boolean detailedPredicateStatistics = ProgramProperties
				.getBooleanProperty("tvla.log.detailedPredicateStatistics",
						true);

		int largestMaxSatisfy = 0;
		int averageMaxSatisfy = 0;
		int sumSatisfy = 0;
		int averageNumberOfNodes = 0;
		int maxNodes = 0;
		int numberOfStructures = 0;
		int maxUnaryPredicateSize = 0;
		int averageUnaryPredicateSize = 0;
		int numberOfUnaryPredicates = 0;
		int numberOfNonZeroUnaryPredicates = 0;
		int averageNonZeroUnaryPredicateSize = 0;
		int maxBinaryPredicateSize = 0;
		int numberOfBinaryPredicates = 0;
		int numberOfNonZeroBinaryPredicates = 0;
		int averageBinaryPredicateSize = 0;
		int averageNonZeroBinaryPredicateSize = 0;
		int maxStructuresInLocation = 0;
		int averageStructuresInLocation = 0;

		for (String locName : cfg.getInOrder()) {
			Location location = cfg.getLocationByLabel(locName);
			int locationSize = location.size();
			maxStructuresInLocation = maxStructuresInLocation < locationSize ? locationSize
					: maxStructuresInLocation;
			averageStructuresInLocation += locationSize;
			if (locationSize != 0 || location.messages.size() > 0) {
				Logger.print(location.label() + ":\t" + locationSize
						+ "\tstructures");
				int maxSatisfy = 0;
				int locMaxNodes = 0;
				for (Iterator<HighLevelTVS> res = location.allStructures(); res
						.hasNext();) {
					HighLevelTVS structure = res.next();
					++numberOfStructures;
					averageNumberOfNodes += structure.nodes().size();

					locMaxNodes = locMaxNodes < structure.nodes().size() ? structure
							.nodes().size() : locMaxNodes;
					if (!detailedPredicateStatistics)
						continue;
					int satisfy = 0;
					for (Predicate pred : structure.getVocabulary().unary()) {
						int unarySatisfy = structure.numberSatisfy(pred);
						if (unarySatisfy != 0) {
							++numberOfNonZeroUnaryPredicates;
							averageNonZeroUnaryPredicateSize += unarySatisfy;
						}
						++numberOfUnaryPredicates;
						maxUnaryPredicateSize = maxUnaryPredicateSize < unarySatisfy ? unarySatisfy
								: maxUnaryPredicateSize;
						averageUnaryPredicateSize += unarySatisfy;
						satisfy += unarySatisfy;
					}
					for (Predicate pred : structure.getVocabulary().binary()) {
						int binarySatisfy = structure.numberSatisfy(pred);
						if (binarySatisfy != 0) {
							++numberOfNonZeroBinaryPredicates;
							averageNonZeroBinaryPredicateSize += binarySatisfy;
						}
						++numberOfBinaryPredicates;
						maxBinaryPredicateSize = maxBinaryPredicateSize < binarySatisfy ? binarySatisfy
								: maxBinaryPredicateSize;
						averageBinaryPredicateSize += binarySatisfy;
						satisfy += binarySatisfy;
					}
					if (satisfy > maxSatisfy)
						maxSatisfy = satisfy;
					averageMaxSatisfy += maxSatisfy;
					sumSatisfy += satisfy;
				}
				maxNodes = maxNodes < locMaxNodes ? locMaxNodes : maxNodes;
				largestMaxSatisfy = largestMaxSatisfy < maxSatisfy ? maxSatisfy
						: largestMaxSatisfy;
				boolean propertyFailed = Action.locationsWherePropertyFails
						.contains(location);
				String propertyFailedStr = propertyFailed ? " PROPERTY FAILED"
						: "";
				Logger.println("\tmax graph="
						+ (detailedPredicateStatistics ? maxSatisfy
								: locMaxNodes) + "\t"
						+ location.messages.size() + " messages"
						+ propertyFailedStr + ", time: "
						+ (location.totalTime / 1000.0));
			}
		}
		averageStructuresInLocation = averageStructuresInLocation
				/ cfg.getInOrder().size();
		if (numberOfStructures != 0)
			averageMaxSatisfy = averageMaxSatisfy / numberOfStructures;
		if (numberOfStructures != 0)
			averageNumberOfNodes = averageNumberOfNodes / numberOfStructures;
		if (numberOfUnaryPredicates != 0)
			averageUnaryPredicateSize = averageUnaryPredicateSize
					/ numberOfUnaryPredicates;
		if (numberOfBinaryPredicates != 0)
			averageBinaryPredicateSize = averageBinaryPredicateSize
					/ numberOfBinaryPredicates;
		if (numberOfNonZeroUnaryPredicates != 0)
			averageNonZeroUnaryPredicateSize = averageNonZeroUnaryPredicateSize
					/ numberOfNonZeroUnaryPredicates;
		if (numberOfNonZeroBinaryPredicates != 0)
			averageNonZeroBinaryPredicateSize = averageNonZeroBinaryPredicateSize
					/ numberOfNonZeroBinaryPredicates;

		Logger.println();
		Logger.println("number of structures in graph          : "
				+ numberOfStructures);
		Logger.println("maximum #structures in any location    : "
				+ maxStructuresInLocation);
		Logger.println("average #structures in locations       : "
				+ averageStructuresInLocation);

		Logger.println("maximal node set                       : " + maxNodes);
		Logger.println("average node set                       : "
				+ averageNumberOfNodes);

		if (detailedPredicateStatistics) {
			Logger.println("sum structure bindings                 : "
					+ sumSatisfy);
			Logger.println("maximal structure max graph            : "
					+ largestMaxSatisfy);
			Logger.println("average structure max graph            : "
					+ averageMaxSatisfy);
			Logger.println("maximal unary predicate size           : "
					+ maxUnaryPredicateSize);
			Logger.println("average unary predicate size           : "
					+ averageUnaryPredicateSize);
			Logger.println("average non-zero unary predicate size  : "
					+ averageNonZeroUnaryPredicateSize);
			Logger.println("maximal binary predicate size          : "
					+ maxBinaryPredicateSize);
			Logger.println("average binary predicate size          : "
					+ averageBinaryPredicateSize);
			Logger.println("average non-zero binary predicate size : "
					+ averageNonZeroBinaryPredicateSize);
		}
	}

	protected boolean shouldUpdate(int every) {
		return every > 0
				&& (status.numberOfStructures / every > prevUpdate / every);
	}

	protected void updateStatus() {
		if (shouldUpdate(status.dumpEvery)) {
			// IOFacade.instance().printAnalysisState(new
			// TreeSet(cfg.getLocations()));
			System.out.println("Structures: " + status.numberOfStructures);
		}
		if (shouldUpdate(statistics.statisticsEvery)) {
			status.stopTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);
			statistics.doStatistics();
			if (status.continuousStatisticsReports) {
				Logger.println();
				Logger.println("Statistics at " + status.numberOfStructures
						+ " structures");
				Logger.println("***************************************************");
				TVSFactory.printStatistics();
				printStatistics();
			}
			status.startTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);
		}

		// if (status.numberOfStructures % status.gcEvery == 0)
		// System.gc();

		if (!AnalysisStatus.terse && shouldUpdate(100)) {
			long currentMemory = Runtime.getRuntime().totalMemory()
					- Runtime.getRuntime().freeMemory();
			System.err.print("\r" + currentLocation.label() + "\t\t\t\t"
					+ status.numberOfStructures + " structures "
					+ (currentMemory / (float) 1000000.0) + " Mb ");
			status.updateStatus();
			System.err.print("      ");
		}
		if (!AnalysisStatus.terse && shouldUpdate(statistics.statisticsEvery)) {
			System.err.println("\t");
			for (String locName : cfg.getInOrder()) {
				Location loc = cfg.getLocationByLabel(locName);
				if (loc.size() > 0)
					System.err.println(loc.status());
			}
			System.err.println(currentLocation.label());
		}
		prevUpdate = status.numberOfStructures;
	}
}
