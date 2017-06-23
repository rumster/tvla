/*
 * File: IntraProcTransitionRelation.java 
 * Created on: 02/03/2005
 * 
 */

package tvla.analysis;

import java.util.*;

import tvla.core.Node;
import tvla.core.TVS;
import tvla.io.IOFacade;
import tvla.util.HashSetFactory;
import tvla.util.Logger;
import tvla.util.ProgramProperties;
import tvla.util.graph.Graph;
import tvla.util.graph.GraphUtils;
import tvla.util.graph.HashGraph;

/** Implements a transition relation using a graph 
 * @author maon
 */
public class IntraProcTransitionRelation implements TransitionRelation {
	Set<Object> locations = null; 
	Graph transitions = null;
	Map<TranisitionRelationNode, Long> ids = null;
	Map<Object, Collection<AbstractState>> loc2abstractStates = null;
	Map<AbstractState, Collection<String>> messages = null;

	public IntraProcTransitionRelation(int numOfLocations) {
		super();

		locations = HashSetFactory.make(numOfLocations);
		transitions = new HashGraph();
		messages = new LinkedHashMap<AbstractState, Collection<String>>();
	}

	@Override
	public Collection getLocations() {
		return Collections.unmodifiableCollection(locations);
	}
    
    /** A graph (V,E) where V is a set of abstract states (structure/location)
     * pairs and E is a set of labeled edges describing how one state
     * evolved into another state.
     * 
     * @return The graph representing the transition relation.
     */
	@Override
    public Graph getConfigurationGraph() {
    	return transitions;
    }
    
    /**
     * Adds a new location to the transition relation
     * @param loc a location (a cfg node).
     * @pre: loc was not inserted to the transition relation
     */
    public void addLocation(Object loc) {
    	assert(!locations.contains(loc));
    	loc2abstractStates = null;
    	locations.add(loc);
     }
    
    /**
     * Adds a new abstract state <loc, tvs> to the transition relation
     * @param loc the location (program counter) that the TVS is at
     * @param tvs a pointer ot the TVS
     * @pre: loc (the actual object!) was inserted to the transition relation.
     * The abstract state was not inserted. 
     */
    public void addAbstractState(Object loc, TVS tvs) {
    	assert(locations.contains(loc));
    	assert(loc != null);
    	assert(tvs != null);
    	
    	TranisitionRelationNode newAbstractState = new TranisitionRelationNode(tvs,loc);
    	assert(!transitions.containsNode(newAbstractState));
    	
    	transitions.addNode(newAbstractState);
    	
    	locations.add(loc);
    	ids = null;
    	loc2abstractStates = null;
    }
    
    
  
    /**
     * Adds a new abstract transition to the transition relation
     * @param fromLoc the source location of the transition 
     * @param fromTVS the source TVS of the transition
     * @param toLoc the destination location of the transition
     * @param toTVS the destination TVS of the transition
     * @param transitionLabel
     * @pre: the abstract states <fromLoc, fromTVS> and <toLoc, TVS toTVS>
     * were inserted to the transition system. The transition (with the given label) was not.
     */
    public void addAbstractTransition(Object fromLoc, TVS fromTVS, Object toLoc, TVS toTVS, Object transitionLabel) {
    	assert(fromLoc != null && locations.contains(fromLoc));
    	assert(toLoc != null && locations.contains(toLoc));
    	
    	TranisitionRelationNode fromAbstractState = new TranisitionRelationNode(fromTVS,fromLoc);
    	TranisitionRelationNode toAbstractState = new TranisitionRelationNode(toTVS,toLoc);
    	assert(transitions.containsNode(fromAbstractState));
    	assert(transitions.containsNode(toAbstractState));
    	
    	transitions.addEdge(fromAbstractState, toAbstractState, transitionLabel);
    	ids = null;
    }
 
    /**
     * Adds all messages in mc to AbstractState <loc,tvs>
     */
    public void addMessage(Object loc, TVS tvs, Object label, Map<? extends TVS, Set<String>> mc) {
    	AbstractState as = new TranisitionRelationNode(tvs,loc);
    	Collection<String> msgs = messages.get(as);
    	if (msgs == null) {
    		msgs = HashSetFactory.make();
    	}
    	for (Collection<String> messagesOfFocusedStrucutre : mc.values()) {
    		msgs.addAll(messagesOfFocusedStrucutre);
    	}
    	messages.put(as,msgs);
    }
    
    /**
     * Merges all the abstract state <fromLoc, fromTVS> and <toLoc, TVS toTVS> by directing all the edges
     * pointing to <fromLoc, fromTVS> into <toLoc, TVS toTVS> and all the edges leaving <fromLoc, fromTVS>  
     * to ...
     * @param fromLoc
     * @param fromTVS
     * @param toLoc
     * @param toTVS
     */
    public void mergeAbstractStates(Object fromLoc, TVS fromTVS, Object toLoc, TVS toTVS) {
    	assert(fromLoc != null && locations.contains(fromLoc));
    	assert(toLoc != null && locations.contains(toLoc));
    	assert(fromLoc == toLoc);
    	assert(fromTVS != toTVS);
    	TranisitionRelationNode fromAbstractState = new TranisitionRelationNode(fromTVS,fromLoc);
    	TranisitionRelationNode toAbstractState = new TranisitionRelationNode(toTVS,toLoc);
    	
    	transitions.mergeInto(fromAbstractState,toAbstractState);
    	ids = null;
    	loc2abstractStates = null;

    	
    	Collection<String> fromMsgs = messages.get(fromAbstractState);
    	if (fromMsgs != null) {
    		messages.remove(fromAbstractState);
    		Collection<String> toMsgs = messages.get(toAbstractState);
    		if (toMsgs == null)
    			messages.put(toAbstractState, fromMsgs);
    		else
    			toMsgs.addAll(fromMsgs);
    	}   
    }
 
    public Map<Object, Collection<AbstractState>> getAbstractStatesAtLocations() {
    	if (loc2abstractStates != null)
    		return Collections.unmodifiableMap(loc2abstractStates); 
    	
    	Map<Object, Collection<AbstractState>> map = new LinkedHashMap<Object, Collection<AbstractState>>();
    	for (AbstractState as : (Collection<TranisitionRelationNode>) transitions.getNodes()) {
    		assert(as != null);
    		Collection<AbstractState> tvsAtlocations =  map.get(as.getLocation());
    		if (tvsAtlocations == null) {
    			tvsAtlocations = new ArrayList<AbstractState>(2);
    			map.put(as.getLocation(),tvsAtlocations);
    		}
    		tvsAtlocations.add(as);
    	}
    	
    	loc2abstractStates = map;
    	return map;
    }
 
    public void assignIds() {
    	ids = new LinkedHashMap<TranisitionRelationNode, Long>();
    	long id = 1;
        for (TranisitionRelationNode as : (Collection<TranisitionRelationNode>) transitions.getNodes()) {
    		if (id == Long.MAX_VALUE)
    			throw new InternalError("Abstract State Id overflow");
    		if (!ids.containsKey(as)) 
    			ids.put(as, new Long(id++));
    	}
    }

    public long getId(AbstractState as) {
    	assert(ids != null);

    	Long id = ids.get(as);
    	assert(id != null);
    	
    	return id.longValue();
    }
    
    public Map<AbstractState, Collection<String>> getMessages() {
    	return Collections.unmodifiableMap(messages);
    }    
    
    public void dump() {
    	assignIds();
    	
    	Logger.println("Configuration graph #nodes="
				+ transitions.getNumberOfNodes() + " #edges="
				+ transitions.getNumberOfEdges());

		String project = ProgramProperties.getProperty("tvla.tr.project",
				"full");
		// ROMAN: for some reason this value is set to 'trace' somewhere.
		project = "full";
		
		if (project.equals("reachMessages")) {
			Logger.println("pruning for messages...");
			pruneFromMessages();
			Logger.println("Configuration graph #nodes="
					+ transitions.getNumberOfNodes() + " #edges="
					+ transitions.getNumberOfEdges());
		} else if (project.equals("trace")) {
			// We perform an initial pruning to get some
			// statistics.
			Logger.println("pruning for messages...");
			pruneFromMessages();
			Logger.println("Configuration graph #nodes="
					+ transitions.getNumberOfNodes() + " #edges="
					+ transitions.getNumberOfEdges());

			Logger.println("pruning for shortest path...");
			pruneForAbstractErrorTrace();
			Logger.println("Error trace #nodes="
					+ transitions.getNumberOfNodes() + " #edges="
					+ transitions.getNumberOfEdges());
		} else if (project.equals("full")) {
			// nothing to do
		} else {
			throw new RuntimeException(
					"Illegal value for property tvla.tr.project: " + project);
		}
    	
    	IOFacade.instance().printTransitionRelation(this);
    }
    
    /** Returns the subgraph containing the shortest
     * path to a state with an associated message. 
     */
    public void pruneForAbstractErrorTrace() {
    	// Check for degenerate case where there are no structures with messages.
    	if (messages.keySet().isEmpty()) {
    		transitions.clear();
    		return;
    	}
    	
    	List path = getErrorTrace();
    	
    	// ROMAN: TODO: check why sometimes path.size > #graph nodes
    	//Set<TranisitionRelationNode> discardNodes = new HashSet<TranisitionRelationNode>(transitions.getNumberOfNodes() - keepNodes.size());
    	assert transitions.getNumberOfNodes() >= path.size();
    	Logger.println(path.size());
    	Logger.println(transitions.getNumberOfNodes());
    	
    	transitions.retainAllNodes(path);
    }
    
    public List getErrorTrace() {
    	// Check for degenerate case where there are no structures with messages.
    	if (messages.keySet().isEmpty()) {
    		transitions.clear();
    		return Collections.EMPTY_LIST;
    	}
    	
    	TranisitionRelationNode initialNode = (TranisitionRelationNode) transitions.getNodes().iterator().next();
    	TranisitionRelationNode messageNode = (TranisitionRelationNode) messages.keySet().iterator().next();
    	List path = GraphUtils.shortestPath(transitions, initialNode, messageNode);
    	return path;
    }
    
    /** Returns the subgraph containing only states that
     * reach other states with messages. 
     */
    public void pruneFromMessages() {
    	// Check for degenerate case where there are no structures with messages.
    	if (messages.keySet().isEmpty()) {
    		transitions.clear();
    		return;
    	}
    	
    	Set<TranisitionRelationNode> reachingNodes = (Set<TranisitionRelationNode>) GraphUtils.getReachableNodes(transitions, messages.keySet(), false, true);
    	transitions.retainAllNodes(reachingNodes);
    }
    
    public static class TranisitionRelationNode implements AbstractState {
    	  protected TVS tvs;
        protected Object loc;
        protected long id;

        TranisitionRelationNode(TVS tvs, Object loc) {
        	this.tvs = tvs;
        	this.loc = loc;
        }
        
        public TVS getStructure() {
        	return tvs;
        }
        
        public Object getLocation() {
        	return loc;
        }
                
        public boolean equals(Object obj) {
        	if (obj == null || !(obj instanceof TranisitionRelationNode))
        		return false;
        	
        	TranisitionRelationNode other = (TranisitionRelationNode) obj;
        	
        	return other.tvs == this.tvs && other.loc == this.loc;
        }

        public int hashCode() {
        	return System.identityHashCode(tvs) + 31 * System.identityHashCode(loc);
        }
    
        public String toString() {
            return id + "=" + loc + ":" + tvs; 
        }
    }

}
