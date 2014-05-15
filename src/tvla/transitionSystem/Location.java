package tvla.transitionSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.analysis.AnalysisStatus;
import tvla.core.HighLevelTVS;
import tvla.core.StoresCanonicMaps;
import tvla.core.TVS;
import tvla.core.TVSFactory;
import tvla.core.TVSSet;
import tvla.core.TVSSetToCollectionAdapter;
import tvla.io.IOFacade;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Pair;
/** This class represents a node in the CFG.
 * @author Tal Lev-Ami.
 */
public class Location implements Comparable<Location>, PrintableProgramLocation {
		
	/** The information evaluated at this location.
	 */
	public TVSSet structures = TVSFactory.getInstance().makeEmptySet(false);
	
	/** Maps structures to string buffers containing their associated messages.
	 */
	public Map<TVS, StringBuffer> messages = HashMapFactory.make(0);
	
	/** Signals that the information in this location will be printed out.
	 */
	public boolean shouldPrint;

	/** The position of this location in a post-order traversal of the CFG.
	 */
	public int postOrder = -1;
	
	/** The position of this location in a pre-order traversal of the CFG.
	 */
	public int preOrder = -1;
	
	public boolean hasBackEdge = false;
	
	/** Inconing edges counter.
	 */
	public int incoming = 0;

	/** Should this structure store joined structures.
	 */
	public boolean doJoin;

	/** The location's label, as specified by the TVP.
	 */
	protected String label;

	/** A collection of structures that need to evaluated.
	 */
	protected Collection<HighLevelTVS> unprocessed = createUnprocessed();
	
	/** A list of actions from this location to outgoing locations.
	 */
	protected List<Action> actions = new ArrayList<Action>();
	
	/** A list of strings of the target locations.
	 */
	protected List<String> targets = new ArrayList<String>();

    private long startTime;

    public long totalTime;
	
	/** Constructs and initializes a CFG location with the specified label.
	 * @param the label associated with the location.
	 * @author Tal Lev-Ami.
	 * @since 16.12.2000
	 */
	public Location(String label) {
		this(label, true);
	}
	
	/** Constructs and initializes a CFG location with the specified label
	 * and signals whether its information will need to be printed.
	 * @param the label associated with the location.
	 * @param shouldPrint Signals whether the information in this location
	 * will be printed.
	 * @author Tal Lev-Ami.
	 * @since 16.12.2000
	 */
	public Location(String label, boolean shouldPrint) {
		this.label = label;
		this.shouldPrint = shouldPrint;
	}

	/** Compares the position of this location to another location according
	 * to a pre-order/post-order traversal of the CFG.
	 */
	public int compareTo(Location other) {	
		if (AnalysisGraph.activeGraph.postOrder)
			return this.postOrder - other.postOrder;
		else 
			return other.postOrder - this.postOrder; // Notice this is reversed!
	}

	/** Removes the unprocessed set of structures and returns it.
	 */
	public Collection<HighLevelTVS> removeUnprocessed() {
		Collection<HighLevelTVS> result = null;
		if (doJoin) {
			result = unprocessed;
			unprocessed = createUnprocessed();
		}
		else {
			result = TVSSetToCollectionAdapter.staticInstance(structures);
			structures = TVSFactory.getInstance().makeEmptySet(doJoin);
		}
		//return new TreeSet(result);
		return result;
	}
	
	/** Returns the label of this location, as specified in the TVP.
	 */
	public String label() {
		return label;
	}
	
	public Action getAction(int i) {
		return actions.get(i);
	}

	public String getTarget(int i) {
		return targets.get(i);
	}

	public List<String> getTargets() {
		return this.targets;
	}
	
	public List<Action> getActions() {
		return this.actions;
	}
	
	public 	boolean setShouldPrint(boolean val) {
		boolean old = this.shouldPrint;
		this.shouldPrint = val;
		return old;
	}
	
	public boolean getShouldPrint() {
		return this.shouldPrint;
	}
	
	public Iterator<HighLevelTVS> getStructuresIterator() {
		return structures.iterator();
	}

	public Map<TVS, StringBuffer> getMessages() {
		return messages;
	}
	
	public void clearMessages() {
		messages = HashMapFactory.make(0);
	}

	public int size() {
	    return structures.size();
	}
	
	/** Adds an action from this location to another location.
	 * @param action The action to apply.
	 * @param target The label of the location at the destination end of
	 * the action.
	 */
	public void addAction(Action action, String target) {
		actions.add(action);
		targets.add(target);
		action.setLocation(this);
	}

	/** Adds messages to the specified structures.
	 * @param messages a map from structures to collections of messages.
	 * @return the total number of added messages.
	 * @author Tal Lev-Ami
	 */
	public int addMessages(Map<HighLevelTVS, Set<String>> addedMessages) {
		int count = 0;
		for (Map.Entry<HighLevelTVS, Set<String>> entry : addedMessages.entrySet()) {
			TVS structure = entry.getKey();
			Collection<String> newMessages = entry.getValue();
			count += addMessages(structure, newMessages);
		}
		return count;
	}

	/** Adds a collection of messages to the specified structure.
	 * @param structure the structure to which the messages are associated.
	 * @param newMessages a collection of (message) strings.
	 * @return the total number of added messages.
	 * @author Tal Lev-Ami
	 */
	public int addMessages(TVS structure, Collection<String> newMessages) {
		StringBuffer messagesString = new StringBuffer();
		StringBuffer oldMessages = messages.get(structure);
		final String oldMessagesString = oldMessages != null ? oldMessages.toString() : null;
		int result = 0;
		for (String message : newMessages) {
			if (oldMessagesString != null && oldMessagesString.indexOf(message) >=0)
				continue;
			++result;
			messagesString.append(message);
		}
		if (AnalysisStatus.debug) {
			IOFacade.instance().printStructure(structure, messagesString.toString());
		}
		if (oldMessages == null)
			messages.put(structure, messagesString);
		else
			oldMessages.append(messagesString.toString());
		return result;
	}

	/** Returns the messages associated with a structure, and removes the association.
	 * @param structure the structure associated with the messages.
	 * @return the (message) string associated with the specified structure.
	 */
	public String getMessages(TVS structure) {
		StringBuffer structureMessages = messages.remove(structure);
		if (structureMessages == null)
			return "";
		else
			return structureMessages.toString();	
	}

	/** Returns a string representing statistics 
	 * about internal information.
	 */
	public String status() {
		String result;
		if (doJoin) {
			result = "unprocessed=" + unprocessed.size() + "\tsaved=" + structures.size();
		}
		else {
			result = "unprocessed=" + structures.size() + "\tsaved=" + 0;
		}
		result = label + " : \t" + result + "\t" + " #messages=" + messages.size();
		return result;
	}

	/** Adds a structure to the set stored in this location.
	 * If the applying join causes a change, a structure will be 
	 * scheduled for further processing by storing it in the
	 * unprocessed collection.
	 * @return The delta structure (if it caused a change to the set) 
	 * or null (if it was already embedded there).
	 */
	public HighLevelTVS join(HighLevelTVS structure) {
		HighLevelTVS delta = structures.mergeWith(structure);
		if (doJoin && delta != null)
			unprocessed.add(delta);
		return delta;
	}
	
	/** Adds a structure to the set stored in this location.
	 * If the applying join causes a change (several strucutres in the set
	 * or the new one are blured togehter),  the mergureMap is set to the 
	 * mapping of strucutres in the set before the join, to strucutres
	 * after the join. See TVSSet metgeWith(TVS,Collection) form more information.
	 * @return if it caused a change to the set 
	 */
	public boolean join(HighLevelTVS structure, Collection<Pair<HighLevelTVS, HighLevelTVS>> mergedWith) {
		boolean changed = structures.mergeWith(structure, mergedWith);
		if (doJoin && changed) {
			Iterator<Pair<HighLevelTVS, HighLevelTVS>> pairItr = mergedWith.iterator();
			boolean strucutreInserted = true;
			while (pairItr.hasNext()) {
				Pair<HighLevelTVS, HighLevelTVS> pair = pairItr.next();
				unprocessed.add(pair.second);
				if (structure == pair.first)
					strucutreInserted = false;
			}
			if (strucutreInserted)
				unprocessed.add(structure);
		}
			
		return changed;	
	}

	
	/** Clears the canonic maps of the structures stored 
	 * in this location.
	 * @author Roman Manevich
	 * @since tvla-0.91 May 28 2001 Initial creation.
	 */
	public void compress() {
		for (TVS structure : structures) {
			if (structure instanceof StoresCanonicMaps)
				((StoresCanonicMaps) structure).clearCanonic();
		}
	}
	
	/** Clears the structures stored in this location.
	 * @author Alexey Loginov
	 * @since  June 19 2003 Initial creation.
	 */
	public void clearLocation() {
		structures = TVSFactory.getInstance().makeEmptySet(false);
		messages = HashMapFactory.make(0);
		unprocessed = createUnprocessed();
		compress();
	}
	
	private static final Collection<HighLevelTVS> createUnprocessed() {
		// ROMAN Join optimizations
		//return new ArrayList<HighLevelTVS>(5);
		return HashSetFactory.make(10);
		//return new TreeSet();
	}

	/**
	 * initialize location ordering, to allow for a reconstruction
	 * of the analysis graph (when changed)
	 */	
	public void initLocationOrder() {
		postOrder = -1;
		preOrder = -1;
		incoming = 0;
	}

	/** returns true if structure has messages */
	public boolean hasMessages() {
		return !messages.isEmpty();
	}
	
	/** Returns the answer to whether there are outgoing actions that
	 * may change a structure.
	 * @author Roman Manevich
	 * @since August 27 2001 Initial creation.
	 */
	public boolean isSkipLocation() {
		if (actions.isEmpty())
			return false;
		return !hasBackEdge && !doJoin;
	}

	/** Returns an iterator that goes over the frozen structures in this location.
	 * @author Roman Manevich.
	 * @since 2.1.2001 Initial creation.
	 */
	public Iterator<HighLevelTVS> frozenStructures() {
		return structures.iterator();
	}

	/** Returns an iterator that goes over all the structures in this location -
	 * both frozen and active.
	 * @author Roman Manevich.
	 * @since 2.1.2001 Initial creation.
	 */
	public Iterator<HighLevelTVS> allStructures() {
		return structures.iterator();
	}
	
	/**
	 * Returns the location label
	 * @author Noam Rinetzky
	 * @since 3.3.05
	 */
	public String toString() {
		return label;
	}
	
	public void startTimer() {
        this.startTime = System.currentTimeMillis();
    }

    public void stopTimer() {
        this.totalTime += System.currentTimeMillis() - startTime;        
    }

    /** An iterator that goes over all the structures in this location -
	 * both frozen and active.
	 * @author Roman Manevich.
	 * @since 2.1.2001 Initial creation.
	 */
	protected class AllStructuresIterator implements Iterator<HighLevelTVS> {
		private Iterator<HighLevelTVS> iter;
		private HighLevelTVS nextStructure;
		private boolean switchedIterators;
		
		public AllStructuresIterator() {
			iter = structures.iterator();
			findNextStructure();
		}
		
		public boolean hasNext() {
			return nextStructure != null;
		}
		
		public HighLevelTVS next() {
            HighLevelTVS result = nextStructure;
			findNextStructure();
			return result;
		}
		
		public void remove() {
			throw new UnsupportedOperationException();
		}

		private void findNextStructure() {
			if (iter.hasNext()) {
				nextStructure = iter.next();
			}
			else {
				if (switchedIterators)
					nextStructure = null;
				else {
					switchedIterators = true;
					iter = unprocessed.iterator();
					if (iter.hasNext())
						nextStructure = iter.next();
					else
						nextStructure = null;
				}
			}
		}
	}
}
