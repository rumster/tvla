package tvla.transitionSystem;

import java.util.ArrayList;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import tvla.core.HighLevelTVS;
import tvla.core.TVS;
import tvla.core.TVSFactory;
import tvla.exceptions.UserErrorException;
import tvla.io.IOFacade;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Logger;
import tvla.util.ProgramProperties;

/** A control flow graph storing analysis information.
 * @author Roman Manevich.
 * @since 29.3.2002 Initial creation.
 */
public class AnalysisGraph {
	public static AnalysisGraph activeGraph;
	public boolean postOrder =
		ProgramProperties.getBooleanProperty("tvla.cfg.postOrder", false);

	private static final int SAVE_BACK = 0;
	private static final int SAVE_EXT = 1;
	private static final int SAVE_ALL = 2;

	private int saveLocations = SAVE_BACK;

	/** Maps string labels to their corresponding CFG locations.
	 */
	protected Map<String, Location> program = HashMapFactory.make();
	
	protected Map<Location, Set<Location>> incoming;

	/** A list of strings specifying the CFG location names.
	 * The order is the order in which the location were added by calling
	 * addAction() (and completeInOrderList()).
	 */
	protected List<String> inOrder = new ArrayList<String>();
    protected Location entryLocation;
    protected int currentPostOrderID;
    protected int currentPreOrderID;

	protected int numberOfActions;
    protected boolean backwardAnalysis =
		ProgramProperties.getBooleanProperty(
			"tvla.cfg.backwardAnalysis",
			false);
    protected static Collection<Location> reachableLocations;

    public static void reset() {
    	activeGraph = null;
    	reachableLocations = null;
    }
    
    public Collection<Location> findLatestLocations(Predicate marker,
    		Kleene val) {
    	assert marker.arity() == 0;
    	Logger.println("Stable suffix analysis started with " + marker + "=" + val);
    	//Location latestLocation = findLatestChosenLocation(chooser);
    	Location latestLocation = program.get("tvla_exit");
    	Logger.println("Searching backward from " + latestLocation.label);
    	Collection<Location> visited = new HashSet<Location>();
    	Collection<Location> result = new HashSet<Location>();
    	Queue<Location> pending = new LinkedList<Location>();
    	if (isSatLocation(latestLocation, marker, val))
    		pending.add(latestLocation);
    	while (!pending.isEmpty()) {
    		Location loc = pending.remove();
    		if (visited.contains(loc))
    			continue;
    		visited.add(loc);
    		Collection<Location> pred = incoming.get(loc);
    		if (pred == null) { // this is a source location
    			Logger.println("Location " + loc.label + " is a CFG source.");
    			if (isSatLocation(loc, marker, val))
    				result.add(loc);
    			continue;
    		}
    		if (pred.size() == 1) {
    			Location predLoc = pred.iterator().next();
    			if (isSatLocation(predLoc, marker, val)) {
    				pending.add(predLoc);
    				// This is an earlier location than 'loc'
    				// so don't add 'loc' to the result.
    			}
    			else {
    				// This is the earliest location so add 'loc' to the result
    				// and stop searching.
    				Logger.println("Straight-line code, lasst flip from location " + predLoc.label + " to " + loc.label);
    				result.add(loc);
    			}
    		}
    		else {
    			boolean allPredMarked = true;
    			Collection<Location> badPred = new HashSet<Location>(); 
    			for (Location predLoc : pred) {
        			if (isSatLocation(predLoc, marker, val)) {
        				pending.add(predLoc);
        			}
        			else {
        				allPredMarked = false;
        				badPred.add(predLoc);
        			}
        		}
    			if (!allPredMarked) {
    				StringBuffer predsStr = new StringBuffer();
    				for (Location predTmp : badPred) {
    					predsStr.append(predTmp.label + ", ");
    				}
    				Logger.println("Conditional code flip at location " + loc.label + " with \"bad\" predecessors " + badPred);
    				result.add(loc);
    			}
    		}
    	}
    	Logger.println("Stable suffix analysis done.  Found " + result.size() + " locations");
    	return result;
    }
    
    public boolean isSatLocation(Location loc, Predicate marker, Kleene val) {
    	if (loc.structures.isEmpty())
    		return true;
    	
    	boolean answer = true;
    	for (TVS structure : loc.structures) { 
    		answer = answer & structure.eval(marker) == val; 
    		if (!answer)
    			break;
    	}
    	return answer;
    }
    
    public Location findLatestChosenLocation(Predicate chooser) {
    	Location result = null;
    	for (String locStr : inOrder) {
    		boolean chosenLoc = false;
    		Location loc = program.get(locStr);
    		for (TVS structure : loc.structures) {
    			if (structure.eval(chooser) != Kleene.falseKleene) {
    				chosenLoc = true;
    				break;
    			}
    		}
    		if (chosenLoc && (result == null || result.compareTo(loc) < 0))
    			result = loc;
    	}
    	return result;
    }
    
    public void constructIncoming() {
    	incoming = HashMapFactory.make();
    	for (Location loc : reachableLocations) {
    		for (String succStr : loc.targets) {
    			Location succ = program.get(succStr);
    			Set<Location> in = null;
    			if (incoming.containsKey(succ)) {
    				in = incoming.get(succ);
    			}
    			else {
    				in = new HashSet<Location>(2);
    				incoming.put(succ, in);
    			}
    			in.add(loc);
    		}
    	}
    }
    
	/** Prepares the graph to be used by an analysis engine.
	 */
	public void init() {
		completeInOrderList();
		if (ProgramProperties
			.getBooleanProperty("tvla.cfg.removeSkipChains", false))
			removeSkipChains();

		// re-initialize order for case of reconstruction
		initLocationsOrder();

		if (inOrder.size() > 0) {
			entryLocation = program.get(inOrder.get(0));
			currentPostOrderID = 0;
			currentPreOrderID = 0;
			reachableLocations = new ArrayList<Location>(inOrder.size());
			DFS(entryLocation); // sort the CFG locations.

			// Do a sanity check on the graph
			if (inOrder.size() != reachableLocations.size()) {
				String message =
					"Warning: Not all CFG locations are reahcable from the entry location:";
				Logger.println(message);
				for (Iterator locationIter = program.entrySet().iterator();
					locationIter.hasNext();
					) {
					Map.Entry entry = (Map.Entry) locationIter.next();
					Location location = (Location) entry.getValue();
					if (reachableLocations.contains(location))
						continue;
					String locationName = (String) entry.getKey();
					Logger.print(locationName);
					if (locationIter.hasNext())
					    Logger.print(", ");
				}
				Logger.println();
			}

			//reachableLocations = null; // Reclaim memory resources.
			                           // Opportunity for compile-time GC.
			initLocations();
		}
	}

	/** Adds an action to a CFG edge, and to the specified source location.
	 * The locations specified by the strings are created if they don't already
	 * exist.
	 * @param source a string specifying the source location.
	 * @param action an action associated with a CFG edge.
	 * @param target a string specifying the target location.
	 * @author Tal Lev-Ami
	 * @since 21.7.2001 Added support for reversing edges (Roman).
	 */
	public void addAction(String source, Action action, String target) {
		if (backwardAnalysis) { // swap source and target
			String tmpLabel = source;
			source = target;
			target = tmpLabel;
		}

		if (!program.containsKey(source))
			program.put(source, new Location(source, true));
		if (!program.containsKey(target))
			program.put(target, new Location(target, true));

		if (!inOrder.contains(source))
			inOrder.add(source);
		if (!inOrder.contains(target))
			inOrder.add(target);

		Location sourceLocation = program.get(source);
		sourceLocation.addAction(action, target);

		++numberOfActions;
	}

	public Collection<Location> getLocations() {
		return program.values();
	}

	public int getNumberOfActions() {
		return numberOfActions;
	}

	public Collection<String> getInOrder() {
		return inOrder;
	}

	public Location getEntryLocation() {
		return entryLocation;
	}

	public Location getLocationByLabel(String label) {
		return program.get(label);
	}

	public void storeStructures(Location location, Iterable<HighLevelTVS> structures) {
		for (HighLevelTVS structure : structures) {
			location.join(structure);
		}
	}

	/** Turns on the printing flag for the specified locations.
	 * @param toPrint a collection of locations.
	 * @author Tal Lev-Ami	 
	 */
	public void setPrintableLocations(Collection toPrint) {
		// Turn printing off for all locations.
		for (Iterator<Location> i = program.values().iterator(); i.hasNext();) {
			Location location = i.next();
			location.shouldPrint = false;
		}

		// Turn it back on where requested.
		for (Iterator i = toPrint.iterator(); i.hasNext();) {
			String label = (String) i.next();
			Location location = getLocation(label);
			if (location == null)
				throw new UserErrorException(
					"Unknown program location " + label);
			location.shouldPrint = true;
		}
	}

	/** Prints the current state of the CFG and the structures in it.
	 */
	public void dump() {
		Collection<Location> inOrderLocations = new ArrayList<Location>(inOrder.size());
		for (String locStr : inOrder) {
			Location loc = program.get(locStr);
			inOrderLocations.add(loc);
		}
		//IOFacade.instance().printAnalysisState(new TreeSet<Location>(program.values()));
		IOFacade.instance().printAnalysisState(inOrderLocations);
	}

	protected Location getLocation(String label) {
		return program.get(label);
	}

	/** Does a topological sort algorithm on the graph.
	 * The information is stored in each location.
	 * @author Tal Lev-Ami
	 */
	protected void DFS(Location current) {
		reachableLocations.add(current);
		current.postOrder = 0;
		current.preOrder = ++currentPreOrderID;
		for (int i = current.getTargets().size() - 1; i >= 0; i--) {
			Location nextLocation =
				program.get(current.getTargets().get(i));
			if (nextLocation != null) {
				nextLocation.incoming++;
				if (nextLocation.postOrder == -1) {
					DFS(nextLocation);
					// This is a tree edge.
				} else if (current.preOrder < nextLocation.preOrder) {
					// This is a forward edge.
				} else if (nextLocation.postOrder == 0) {
					// This is a back edge
					nextLocation.hasBackEdge = true;
				} else {
					/// This is a cross edge.
				}
			}
		}
		current.postOrder = ++currentPostOrderID;
	}

	/** Removes chains of "skip" actions (do-nothing actions) by modifying the edges to
	 * bypass locations with exactly one skip action.
	 * @author Roman Manevich.
	 * @since 27.8.2001 Initial creation.
	 */
	protected void removeSkipChains() {
		Set<Location> locations = HashSetFactory.make(program.values());
		Set<Location> skipLocations = HashSetFactory.make();
		Set<String> skipLabels = HashSetFactory.make();
		int skips = 0;
		int oldSkips = 0;
		do {
			oldSkips = skips;
			Iterator<Location> iter = locations.iterator();
			while (iter.hasNext()) {
				Location location = iter.next();
				if (location.getActions().size() == 0) {
					iter.remove();
					continue;
				}

				{ // Handle locations with incoming skips edges
					Iterator actionIter = location.getActions().iterator();
					Iterator targetIter = location.getTargets().iterator();
					Set<String> newTargets = HashSetFactory.make();
					while (actionIter.hasNext()) {
						Action action = (Action) actionIter.next();
						Location target =
							program.get(targetIter.next());
						if (target.getActions().size() == 1
							&& ((Action) target.getActions().iterator().next())
								.isSkipAction()
							&& target.isSkipLocation()) {
							Location newTarget =
								program.get(
                            	target.getTargets().iterator().next());
							action.setLocation(newTarget);
							targetIter.remove();
							newTargets.add(newTarget.label());
							skipLocations.add(target);
							skipLabels.add(target.label());
						}
					}
					location.getTargets().addAll(newTargets);
				}

				{ // Handle locations with outgoing skips edges
					Set<Location> skipOver = HashSetFactory.make();
					Iterator actionIter = location.getActions().iterator();
					Iterator targetIter = location.getTargets().iterator();
					while (actionIter.hasNext()) {
						Action action = (Action) actionIter.next();
						Location target =
							program.get(targetIter.next());
						if (action.isSkipAction()
							&& target.isSkipLocation()
							&& target.getActions().size() == 1) {
							++skips;
							actionIter.remove();
							targetIter.remove();
							skipOver.add(target);
							skipLocations.add(target);
							skipLabels.add(target.label());
						}
					}
					for (Iterator<Location> skipOverIter = skipOver.iterator();
						skipOverIter.hasNext();
						) {
						Location target = skipOverIter.next();
						location.getActions().addAll(target.getActions());
						location.getTargets().addAll(target.getTargets());
					}
				}
			}
		}
		while (skips != oldSkips);
		Logger.println("skipped over " + skipLocations.size() + " locations.");
		//program.keySet().removeAll(skipLabels);
		inOrder.removeAll(skipLabels);
		locations.removeAll(skipLocations);
	}

	/** Initializes the inOrder list by adding any location that was not 
	 * specified with addAction().
	 * @since 8.12.2000 
	 */
	protected void completeInOrderList() {
		for (Iterator<String> i = program.keySet().iterator(); i.hasNext();) {
			String label = i.next();
			if (!inOrder.contains(label))
				inOrder.add(label);
		}
	}

	/**
	 * initialize location order to allow reocnstruction of a new 
	 * graph when some locations in the graph were already 
	 * ordered in the past
	 */
	protected void initLocationsOrder() {
		for (Iterator<Location> it = program.values().iterator(); it.hasNext();) {
			Location curr = it.next();
			curr.initLocationOrder();
		}
	}

	/** Initializes the do-join decision for each location.
	 */
	protected void initLocations() {
		String saveProperty = "tvla.cfg.saveLocations";
		String savingScheme =
			ProgramProperties.getProperty(saveProperty, "back");
		if (savingScheme.equals("back"))
			saveLocations = SAVE_BACK;
		else if (savingScheme.equals("ext"))
			saveLocations = SAVE_EXT;
		else if (savingScheme.equals("all"))
			saveLocations = SAVE_ALL;
		else {
			String message =
				"Invalid value specified for property "
					+ saveProperty
					+ " : "
					+ savingScheme;
			throw new UserErrorException(message);
		}

		String joinType =
			ProgramProperties.getProperty("tvla.joinType", "relational");
		boolean singleGraph =
			(joinType.equals("single") || joinType.equals("partial"));
		for (Iterator<Location> i = program.values().iterator(); i.hasNext();) {
			Location location = i.next();
			if (singleGraph) {
				location.doJoin = true;
			} else {
				switch (saveLocations) {
					case SAVE_BACK :
						location.doJoin =
							location.hasBackEdge || location.shouldPrint;
						;
						break;
					case SAVE_EXT :
						location.doJoin =
							(location.incoming > 1) || location.shouldPrint;
						break;
					case SAVE_ALL :
						location.doJoin = true;
						break;
					default :
						throw new RuntimeException(
							"Invalid save locations scheme: " + saveLocations);
				}
			}
			location.structures =
				TVSFactory.getInstance().makeEmptySet(location.doJoin);
		}
	}
}
