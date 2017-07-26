package tvla.analysis;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import tvla.core.Canonic;
import tvla.core.Constraints;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.TVS;
import tvla.core.TVSFactory;
import tvla.core.generic.BoundedStructEmbeddingTest;
import tvla.core.generic.GenericHashPartialJoinTVSSet;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.termination.RTNode;
import tvla.termination.TerminationAnalysisInput;
import tvla.termination.TerminationVerifier;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.AnalysisGraph;
import tvla.transitionSystem.Location;
import tvla.util.HashMapFactory;
import tvla.util.Logger;
import tvla.util.Pair;
import tvla.util.ProgramProperties;
import tvla.util.StringUtils;
import tvla.util.graph.Graph;
import tvla.util.graph.GraphFactory;

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
	protected boolean checkTermination = false;
	protected boolean summarizeLoops = true;

	/**
	 * Constructs and initializes an intra-procedural engine.
	 */
	public IntraProcEngine() {
		super();
	}

	public void evaluate(Collection<HighLevelTVS> initial) {
		this.cfg = AnalysisGraph.activeGraph;
		init();
		status.numberOfStructures = initial.size();

		Graph transitionGraph = GraphFactory.newGraph();
		List<RTNode> entryNodes = new ArrayList<>();
		Map<Pair<Location, Set<Canonic>>, RTNode> nodes = new HashMap<>();
		Map<Node, List<Node>> embeddingFunction = HashMapFactory.make();
		Map<HighLevelTVS, Map<Node, Node>> nodesTransition = new HashMap<>();
		List<String> nodeMessages = new ArrayList<>();
		int nestingDepth = 1;

		// Joining the input structures and putting them in the entry location.
		Location entryLocation = cfg.getEntryLocation();
		cfg.storeStructures(entryLocation, initial);

		SortedSet<Location> workSet = new TreeSet<Location>();
		workSet.add(cfg.getEntryLocation());
		OUTER: while (!workSet.isEmpty()) {
			++numberOfIterations;
			maxWorkSetSize = maxWorkSetSize < workSet.size() ? workSet.size() : maxWorkSetSize;
			averageWorkSetSize += workSet.size();

			Iterator<Location> first = workSet.iterator();
			currentLocation = first.next();
			Location currentLocationLoc = (Location) currentLocation;

			first.remove();
			if (!AnalysisStatus.terse)
				System.err.print("\r" + currentLocation.label() + "    ");

			Collection<HighLevelTVS> unprocessed = currentLocationLoc.removeUnprocessed();

			for (int actionIt = 0; actionIt < currentLocationLoc.getActions().size(); actionIt++) {

				currentAction = currentLocationLoc.getAction(actionIt);
				String target = currentLocationLoc.getTarget(actionIt);

				Location nextLocation = cfg.getLocationByLabel(target);

				for (Iterator<HighLevelTVS> structureIt = unprocessed.iterator(); structureIt.hasNext();) {

					HighLevelTVS structure = structureIt.next();

					RTNode curr = null;
					List<Node> filter = null;

					if (checkTermination) {
						Set<Canonic> canonic = GenericHashPartialJoinTVSSet.getCanonicSetForBlurred(structure);
						Pair<Location, Set<Canonic>> currKey = new Pair<Location, Set<Canonic>>(currentLocationLoc,
								canonic);

						curr = nodes.getOrDefault(currKey, null);
						if (curr == null) {
							curr = RTNode.Create(currentLocationLoc, structure, summarizeLoops, nodeMessages);
							filter = FilterNodes(structure); // interprocedure fix, skip 'List' nodes

							nodes.putIfAbsent(currKey, curr);
							transitionGraph.addNode(curr);
							if (currentLocationLoc == cfg.getEntryLocation())
								entryNodes.add(curr);

							nestingDepth = Math.max(curr.LoopIndex, nestingDepth);
						}
					}

					if (actionIt == ((Location) currentLocation).getActions().size() - 1) {
						structureIt.remove();
					}

					Map<HighLevelTVS, Set<String>> messages = HashMapFactory.make(0);
					nodesTransition.clear();
					Collection<HighLevelTVS> results = apply(currentAction, structure, currentLocation.label(),
							messages, nodesTransition);

					// Replay the last action to show the user details of the
					// failure.
					if (Engine.coerceAfterUpdateFailed || (!messages.isEmpty() && hasPostMessages(messages))) {
						boolean debug = AnalysisStatus.debug;
						AnalysisStatus.debug = true;
						apply(currentAction, structure, currentLocation.label(), messages, null);
						AnalysisStatus.debug = debug;
					}

					status.numberOfMessages += ((Location) currentLocation).addMessages(messages);

					for (HighLevelTVS result : results) {
						HighLevelTVS resultOrig = null, resultCopy = null;

						if (checkTermination) {
							resultOrig = result;
							resultCopy = result.copy();
						}

						status.startTimer(AnalysisStatus.JOIN_TIME);
						HighLevelTVS structureInTarget = nextLocation.join(result);
						boolean needJoin = structureInTarget != null;
						status.stopTimer(AnalysisStatus.JOIN_TIME);

						if (needJoin) {
							workSet.add(nextLocation);
							++status.numberOfStructures;
							updateStatus();
							if (status.shouldFinishAnalysis())
								break OUTER;
						}

						if (checkTermination) {
							status.startTimer(AnalysisStatus.TD_TIME);

							embeddingFunction.clear();
							result = nextLocation.structures.LastStructureMerge;
							boolean b = BoundedStructEmbeddingTest.isEmbedded(resultCopy, result, embeddingFunction);
							assert b;

							/*
							 * if (structureInTarget != null) { result =
							 * structureInTarget; boolean b =
							 * BoundedStructEmbeddingTest.isEmbedded(resultCopy,
							 * structureInTarget, embeddingFunction); assert b;
							 * } else { for (HighLevelTVS s :
							 * nextLocation.structures) { if
							 * (BoundedStructEmbeddingTest.isEmbedded(
							 * resultCopy, s, embeddingFunction)) { result = s;
							 * break; } } }
							 */
							Set<Canonic> canonic = GenericHashPartialJoinTVSSet.getCanonicSetForBlurred(result);
							Pair<Location, Set<Canonic>> nextKey = new Pair<Location, Set<Canonic>>(nextLocation,
									canonic);
							RTNode next = nodes.getOrDefault(nextKey, null);
							if (next == null) {
								next = RTNode.Create(nextLocation, result, summarizeLoops, nodeMessages);
								nodes.putIfAbsent(nextKey, next);
								transitionGraph.addNode(next);
								if (nextLocation == cfg.getEntryLocation())
									entryNodes.add(next);

								nestingDepth = Math.max(next.LoopIndex, nestingDepth);
							}

							Map<Node, Node> nodesMap = nodesTransition.getOrDefault(resultOrig, null);
							Map<Node, List<Node>> regionTransition = new HashMap<>();

							if (nodesMap != null) {
								for (Map.Entry<Node, Node> entry : nodesMap.entrySet()) {
									regionTransition.putIfAbsent(entry.getValue(), new ArrayList<Node>());
									regionTransition.get(entry.getValue())
											.addAll(embeddingFunction.get(entry.getKey()));
								}
							} else {
								regionTransition = new HashMap<>(embeddingFunction);
							}

							// skip allocations
							regionTransition.keySet().retainAll(curr.Structure.nodes());

							// interprocedure fix, skip 'List' nodes
							if (filter != null)
								regionTransition.keySet().retainAll(filter);

							/*
							 * if (transitionGraph.containsEdge(curr, next)) {
							 * Map<Node, List<Node>> regionTransition2 =
							 * (Map<Node,
							 * List<Node>>)transitionGraph.getEdge(curr,
							 * next).getLabel();
							 * 
							 * // Consistency assertion assert
							 * regionTransition.equals(regionTransition2); }
							 * else {
							 */
							if (!transitionGraph.containsEdge(curr, next)) { // Don't
																				// add
																				// twice
																				// the
																				// same
																				// edge
								transitionGraph.addEdge(curr, next, regionTransition);
								next.UpdateSubData(regionTransition);
							}
						}

						status.stopTimer(AnalysisStatus.TD_TIME);
					}

					if (status.shouldFinishAnalysis())
						break OUTER;
				}
				// nextLocation.compress(); // clear the canonic maps in the
				// location's structures
			}
		}

		if (ProgramProperties.getBooleanProperty("tvla.engine.checkMessagesAtFixpoint", false))
			evaluateMessagesAtFixpoint();

		status.stopTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);

		if (maintainTransitionRelation) {
			postHocTransitionRelation();
		}

		if (checkTermination) {
			// Old version
			// TerminationAnalysisInput terminationAnalysisInput =
			// ProduceTerminationAnalysisData();
			// TerminationVerifier.defaultInstance.Analyze(terminationAnalysisInput);

			// TODO Delete
			// System.out.println("The Graphs are " +
			// areLike(terminationAnalysisInput.RegionTransitionGraph,
			// transitionGraph));

			if (nodeMessages.size() > 0 && ProgramProperties.getBooleanProperty("tvla.td.verbose", false)) {
				Logger.println();
				Logger.println("Parsing warnings:");
				for (String msg : nodeMessages) {
					Logger.println(msg);
				}
			}

			String dotOutDir = ProgramProperties.getProperty("tvla.td.dot", null);
			TerminationAnalysisInput terminationAnalysisInput = new TerminationAnalysisInput(transitionGraph,entryNodes, nestingDepth, dotOutDir, summarizeLoops);
			TerminationVerifier.defaultInstance.Analyze(terminationAnalysisInput);
		}

		printAnalysisInfo();

		String stablePredStr = ProgramProperties.getProperty("tvla.stableSuffixAnalysis", "");
		if (!stablePredStr.equals("")) {
			Predicate marker = Vocabulary.getPredicateByName(stablePredStr);
			if (marker == null)
				throw new Error("Predicate " + stablePredStr
						+ " specified in property tvla.stableSuffixAnalysis was not specified!");
			cfg.constructIncoming();
			Collection<Location> markedLocations = cfg.findLatestLocations(marker, Kleene.trueKleene);
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
				((IntraProcTransitionRelation) transitionRelation).addAbstractState(loc, tvs);
			}
		}

		// Second, create all transition relation edges.
		for (Location currentLocation : cfg.getLocations()) {
			for (HighLevelTVS tvs : currentLocation.structures) {
				for (int actionIt = 0; actionIt < ((Location) currentLocation).getActions().size(); actionIt++) {
					currentAction = ((Location) currentLocation).getAction(actionIt);
					String target = ((Location) currentLocation).getTarget(actionIt);
					Location nextLocation = cfg.getLocationByLabel(target);

					Map<HighLevelTVS, Set<String>> messages = HashMapFactory.make(0);
					Collection<HighLevelTVS> results = apply(currentAction, tvs, currentLocation.label(), messages,
							null);

					if (!messages.isEmpty())
						((IntraProcTransitionRelation) transitionRelation).addMessage(currentLocation, tvs,
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
							BoundedStructEmbeddingTest.isEmbedded(resultTVS, tvsInTarget, null)) {
								++numEmbeddings;
								((IntraProcTransitionRelation) transitionRelation).addAbstractTransition(
										currentLocation, tvs, nextLocation, tvsInTarget, currentAction);
							}
						}
						assert numEmbeddings > 0;
					}
				}
			}
		}
	}

	protected TerminationAnalysisInput ProduceTerminationAnalysisData() {

		Graph transitionGraph = GraphFactory.newGraph();
		List<RTNode> entryNodes = new ArrayList<>();
		Map<Pair<TVS, Location>, RTNode> nodes = new HashMap<>();
		Map<Node, List<Node>> embeddingFunction = HashMapFactory.make();
		Map<HighLevelTVS, Map<Node, Node>> nodesTransition = new HashMap<>();
		Map<HighLevelTVS, Set<String>> messages = HashMapFactory.make(0);

		int nestingDepth = 1;

		// First, create all transition relation nodes.
		for (Location loc : cfg.getLocations()) {

			// TODO more generic
			String label = loc.label().trim();
			int loopIndex = label.length() > 1 && Character.isDigit(loc.label().charAt(1)) ? Integer.parseInt(loc.label().substring(1, 2)) : 1;

			for (TVS tvs : loc.structures) {

				Pair<TVS, Location> pair = new Pair<TVS, Location>(tvs, loc);
				RTNode node = new RTNode(pair, loopIndex);
				nodes.put(pair, node);

				if (loc == cfg.getEntryLocation())
					entryNodes.add(node);

				transitionGraph.addNode(node);

				nestingDepth = Math.max(nestingDepth, loopIndex);
			}
		}

		// Second, create all transition relation edges.
		for (Location currentLocation : cfg.getLocations()) {
			for (HighLevelTVS tvs : currentLocation.structures) {
				RTNode curr = nodes.get(new Pair<TVS, Location>(tvs, currentLocation));

				// interprocedural
				List<Node> filter = null;// FilterNodes(tvs);

				for (int actionIt = 0; actionIt < currentLocation.getActions().size(); actionIt++) {

					currentAction = currentLocation.getAction(actionIt);
					String target = currentLocation.getTarget(actionIt);
					Location nextLocation = cfg.getLocationByLabel(target);

					nodesTransition.clear();
					messages.clear();

					Collection<HighLevelTVS> results = apply(currentAction, tvs, currentLocation.label(), messages,
							nodesTransition);

					for (HighLevelTVS resultTVS : results) {
						int numEmbeddings = 0;

						for (HighLevelTVS tvsInTarget : nextLocation.structures) {
							if (BoundedStructEmbeddingTest.isEmbedded(resultTVS, tvsInTarget, embeddingFunction)) {

								RTNode next = nodes.get(new Pair<TVS, Location>(tvsInTarget, nextLocation));

								Map<Node, Node> nodesMap = nodesTransition.getOrDefault(resultTVS, null);
								Map<Node, List<Node>> regionTransition = new HashMap<>();

								if (nodesMap != null) {
									for (Map.Entry<Node, Node> entry : nodesMap.entrySet()) {
										regionTransition.putIfAbsent(entry.getValue(), new ArrayList<Node>());

										// HashSet<Node> hashSet = new
										// HashSet<>(embeddingFunction.get(entry.getKey()));
										// hashSet.addAll(regionTransition.get(entry.getValue()));

										// regionTransition.get(entry.getValue()).clear();
										// regionTransition.get(entry.getValue()).addAll(hashSet);
										regionTransition.get(entry.getValue())
												.addAll(embeddingFunction.get(entry.getKey()));
									}
								} else {
									assert embeddingFunction.size() > 0;
									regionTransition = new HashMap<>(embeddingFunction);
								}

								// skip allocations
								regionTransition.keySet().retainAll(tvs.nodes());

								if (filter != null)
									regionTransition.keySet().retainAll(filter);

								if (transitionGraph.containsEdge(curr, next)) {
									// Consistency assertion
									@SuppressWarnings("unchecked")
									Map<Node, List<Node>> regionTransition2 = (Map<Node, List<Node>>) transitionGraph
											.getEdge(curr, next).getLabel();
									assert regionTransition.equals(regionTransition2);
								} else {
									transitionGraph.addEdge(curr, next, regionTransition);
									next.UpdateSubData(regionTransition);
								}
								++numEmbeddings;
								break;
							}

							assert numEmbeddings > 0;
						}
					}
				}
			}
		}

		TerminationAnalysisInput result = new TerminationAnalysisInput(transitionGraph,
		                                                               entryNodes,
		                                                               nestingDepth,
		                                                               ProgramProperties.getProperty("tvla.td.dot", null),
		                                                               summarizeLoops);

		return result;
	}

	private List<Node> FilterNodes(HighLevelTVS tvs) {

		for (Predicate predicate : tvs.getVocabulary().unary()) {

			if (predicate.name().equals("List")) {
				List<Node> result = new ArrayList<>();

				for (Node node : tvs.nodes()) {

					if (tvs.eval(predicate, node) != Kleene.falseKleene) {
						result.add(node);
					}
				}

				return result;
			}
		}

		return null;
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
					System.out.println("Found " + numMessages + " at " + loc.label());
				}
				for (HighLevelTVS structure : loc.structures) {
					Map<HighLevelTVS, Set<String>> messages = HashMapFactory.make(0);
					apply(currentAction, structure, loc.label(), messages, null);
					// Replay the last action to show the user details of the
					// failure.
					status.numberOfMessages += loc.addMessages(messages);
				}
			}
		}
	}

	protected static boolean hasPostMessages(Map<HighLevelTVS, Set<String>> messages) {
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

	public boolean updateTransitionRelation(Location currentLocation, Action currentAction, Location nextLocation,
			TVS structure, TVS result, Collection<Pair<HighLevelTVS, HighLevelTVS>> mergeMap, boolean stateChanged) {
		if (!stateChanged) {
			// A TVS embedding result is already in the abstract state.
			// We only need to add a transition to the transition relation.
			assert mergeMap.size() == 1 : "mergeMap.size()==" + mergeMap.size() + " expected to be 1!";
			Pair<HighLevelTVS, HighLevelTVS> pair = mergeMap.iterator().next();
			assert (pair.first == result);
			assert (pair.second != result);
			TVS embeddingTVS = pair.second;
			((IntraProcTransitionRelation) transitionRelation).addAbstractTransition(currentLocation, structure,
					nextLocation, embeddingTVS, currentAction);

			return false;
		}

		Iterator<Pair<HighLevelTVS, HighLevelTVS>> pairItr = mergeMap.iterator();
		boolean resultInserted = true;
		while (pairItr.hasNext()) {
			Pair<HighLevelTVS, HighLevelTVS> pair = pairItr.next();
			TVS embeddedTVS = pair.first;
			TVS embeddingTVS = pair.second;

			assert (result != embeddingTVS);

			// ROMAN: trying to fix a bug
			((IntraProcTransitionRelation) transitionRelation).addAbstractState(nextLocation, embeddingTVS);

			if (result == embeddedTVS) {
				resultInserted = false;
				((IntraProcTransitionRelation) transitionRelation).addAbstractTransition(currentLocation, structure,
						nextLocation, embeddingTVS, currentAction);
			} else {
				// Never happens in relational + partial join
				assert (false);
				((IntraProcTransitionRelation) transitionRelation).mergeAbstractStates(currentLocation, embeddedTVS,
						nextLocation, embeddingTVS);
			}

		}

		if (resultInserted) {
			((IntraProcTransitionRelation) transitionRelation).addAbstractState(nextLocation, result);
			((IntraProcTransitionRelation) transitionRelation).addAbstractTransition(currentLocation, structure,
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

		maintainTransitionRelation = ProgramProperties.getBooleanProperty("tvla.tr.enabled", false);

		checkTermination = ProgramProperties.getBooleanProperty("tvla.td.enabled", false);
		summarizeLoops = !ProgramProperties.getBooleanProperty("tvla.td.summarizationOff", false);

		if (maintainTransitionRelation) {
			postHocTransitionRelation = ProgramProperties.getBooleanProperty("tvla.tr.posthoc", false);
			dynamicTransitionRelation = ProgramProperties.getBooleanProperty("tvla.tr.dynamic", false);

			Collection<Location> locations = cfg.getLocations();
			transitionRelation = new IntraProcTransitionRelation(locations.size());
			for (Location loc : locations) {
				((IntraProcTransitionRelation) transitionRelation).addLocation(loc);
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

		Logger.println("#locations                 : " + cfg.getLocations().size());
		Logger.println("#actions                   : " + cfg.getNumberOfActions());
		Logger.println("#predicates                : " + Vocabulary.size());
		Logger.println("#nullary predicates        : " + Vocabulary.allNullaryPredicates().size());
		Logger.println("#nullary abs predicates    : " + numNullaryAbsPredicates);
		Logger.println("#unary predicates          : " + Vocabulary.allUnaryPredicates().size());
		Logger.println("#unary abs predicates      : " + numUnaryAbsPredicates);
		Logger.println("#binary predicates         : " + Vocabulary.allBinaryPredicates().size());
		Logger.println("#constraints               : " + Constraints.getInstance().constraints().size());
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
				.getBooleanProperty("tvla.log.detailedPredicateStatistics", true);

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
			maxStructuresInLocation = maxStructuresInLocation < locationSize ? locationSize : maxStructuresInLocation;
			averageStructuresInLocation += locationSize;
			if (locationSize != 0 || location.messages.size() > 0) {
				Logger.print(location.label() + ":\t" + locationSize + "\tstructures");
				int maxSatisfy = 0;
				int locMaxNodes = 0;
				for (Iterator<HighLevelTVS> res = location.allStructures(); res.hasNext();) {
					HighLevelTVS structure = res.next();
					++numberOfStructures;
					averageNumberOfNodes += structure.nodes().size();

					locMaxNodes = locMaxNodes < structure.nodes().size() ? structure.nodes().size() : locMaxNodes;
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
				largestMaxSatisfy = largestMaxSatisfy < maxSatisfy ? maxSatisfy : largestMaxSatisfy;
				boolean propertyFailed = Action.locationsWherePropertyFails.contains(location);
				String propertyFailedStr = propertyFailed ? " PROPERTY FAILED" : "";
				Logger.println("\tmax graph=" + (detailedPredicateStatistics ? maxSatisfy : locMaxNodes) + "\t"
						+ location.messages.size() + " messages" + propertyFailedStr + ", time: "
						+ (location.totalTime / 1000.0));
			}
		}
		averageStructuresInLocation = averageStructuresInLocation / cfg.getInOrder().size();
		if (numberOfStructures != 0)
			averageMaxSatisfy = averageMaxSatisfy / numberOfStructures;
		if (numberOfStructures != 0)
			averageNumberOfNodes = averageNumberOfNodes / numberOfStructures;
		if (numberOfUnaryPredicates != 0)
			averageUnaryPredicateSize = averageUnaryPredicateSize / numberOfUnaryPredicates;
		if (numberOfBinaryPredicates != 0)
			averageBinaryPredicateSize = averageBinaryPredicateSize / numberOfBinaryPredicates;
		if (numberOfNonZeroUnaryPredicates != 0)
			averageNonZeroUnaryPredicateSize = averageNonZeroUnaryPredicateSize / numberOfNonZeroUnaryPredicates;
		if (numberOfNonZeroBinaryPredicates != 0)
			averageNonZeroBinaryPredicateSize = averageNonZeroBinaryPredicateSize / numberOfNonZeroBinaryPredicates;

		Logger.println();
		Logger.println("number of structures in graph          : " + numberOfStructures);
		Logger.println("maximum #structures in any location    : " + maxStructuresInLocation);
		Logger.println("average #structures in locations       : " + averageStructuresInLocation);

		Logger.println("maximal node set                       : " + maxNodes);
		Logger.println("average node set                       : " + averageNumberOfNodes);

		if (detailedPredicateStatistics) {
			Logger.println("sum structure bindings                 : " + sumSatisfy);
			Logger.println("maximal structure max graph            : " + largestMaxSatisfy);
			Logger.println("average structure max graph            : " + averageMaxSatisfy);
			Logger.println("maximal unary predicate size           : " + maxUnaryPredicateSize);
			Logger.println("average unary predicate size           : " + averageUnaryPredicateSize);
			Logger.println("average non-zero unary predicate size  : " + averageNonZeroUnaryPredicateSize);
			Logger.println("maximal binary predicate size          : " + maxBinaryPredicateSize);
			Logger.println("average binary predicate size          : " + averageBinaryPredicateSize);
			Logger.println("average non-zero binary predicate size : " + averageNonZeroBinaryPredicateSize);
		}
	}

	protected boolean shouldUpdate(int every) {
		return every > 0 && (status.numberOfStructures / every > prevUpdate / every);
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
				Logger.println("Statistics at " + status.numberOfStructures + " structures");
				Logger.println("***************************************************");
				TVSFactory.printStatistics();
				printStatistics();
			}
			status.startTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);
		}

		// if (status.numberOfStructures % status.gcEvery == 0)
		// System.gc();

		if (!AnalysisStatus.terse && shouldUpdate(100)) {
			long currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.err.print("\r" + currentLocation.label() + "\t\t\t\t" + status.numberOfStructures + " structures "
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
