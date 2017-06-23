/*
 * Created on Mar 21, 2004
 *
 */
package tvla.util.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import tvla.util.HashSetFactory;

// import tvla.util.graph.Graph.Edge;

/**
 * Implementation of a mutable directed graph based on hash tables.
 * The implementation allows associating objects (called labels) with edges.
 * Thus, multiple edges with different labels can exist between two nodes.

 * NOTE: the hash code values of nodes should remain fixed throughout their
 * existence in the graph in order to avoid breaking the underlying hash
 * tables.  Changing the hash code value of edge labels is okay.
 * 
 * @author Roman Manevich
 */
public class HashGraph extends AbstractGraph {
    // The set of nodes is not stored explicitly.  Instead we use the set of
    // keys in the nodeToOutgoing map.
    
    /**
     * Maps nodes to sets of (outgoing) edges.
     */
    private Map nodeToOutgoing = new LinkedHashMap();

    /**
     * Maps nodes to sets of (incoming) edges.
     */
    private Map nodeToIncoming = new LinkedHashMap();

	
	///////////////////////////////////////////////
	//////            CONSTRUCTORS          ///////
	///////////////////////////////////////////////
	
	/** Constructs an empty graph.
	 */
	public HashGraph() {
		nodeToOutgoing = new LinkedHashMap();
		nodeToIncoming = new LinkedHashMap();
	}

	
	/** Constructs an empty graph with an hint for the number of nodes.
	 */
	public 	HashGraph(int numOfNodes) {
		float loadCapacity = 0.75f;
		nodeToOutgoing = new LinkedHashMap((4 * numOfNodes) / 3 + 1, loadCapacity);
		nodeToIncoming = new LinkedHashMap((4 * numOfNodes) / 3 + 1, loadCapacity);
	}
	
	/** Constructs an empty graph with an hint for the number of nodes.
	 */
	public 	HashGraph(int numOfNodes, float loadCapacity) {
		nodeToOutgoing = new LinkedHashMap(numOfNodes, loadCapacity);
		nodeToIncoming = new LinkedHashMap(numOfNodes, loadCapacity);
	}

	/** Constructs an empty graph with an hints for the number of nodes
	 * and expected in-degree and out-degree.
	 */
	public 	HashGraph(int numOfNodes, float loadCapacity, int inDegree, int outDegree) {
		nodeToOutgoing = new LinkedHashMap(numOfNodes, loadCapacity);
		nodeToIncoming = new LinkedHashMap(numOfNodes, loadCapacity);
	}
	
	
	protected HashGraph(HashGraph graph) {
		super();
		nodeToOutgoing = new LinkedHashMap(graph.nodeToOutgoing);
		nodeToIncoming = new LinkedHashMap(graph.nodeToIncoming);
	}	

    
	
	///////////////////////////////////////////////
	//////             ACCESSORS            ///////
	///////////////////////////////////////////////

    /**
     * @see tvla.util.graph.Graph#getNodes()
     */
	@Override
    public Collection getNodes() {
        Collection answer = nodeToOutgoing.keySet();
        // We return an unmodifiable collection to avoid inconsistencies
        // between nodeToIncoming and nodeToOutgoing as a result of changes
        // to just one of the maps.
        // In the future, if removing edges during iteration is desired,
        // we can return a special collection that allows mutations and
        // ensures consistency.
        return Collections.unmodifiableCollection(answer);
    }

    /**
     * @see tvla.util.graph.Graph#getNumberOfNodes()
     */
	@Override
	public int getNumberOfNodes() {
		return nodeToIncoming.size();
	}    
  
	
	
    
	/** @see tvla.util.graph.Graph#getEdges()
	 */
	@Override
	public Collection getEdges() {
		Collection allEdges = HashSetFactory.make();		
		Iterator nodeItr = nodeToOutgoing.keySet().iterator();
		while (nodeItr.hasNext()) {
			Object node = nodeItr.next();
			Set outEdges = (Set) nodeToOutgoing.get(node);
			allEdges.addAll(outEdges);
		}
		return allEdges;
	}   
    
    /**
     * @see tvla.util.graph.Graph#getOutgoingEdges(java.lang.Object)
     */
	@Override
    public Collection getOutgoingEdges(Object node) {
        assert node != null && containsNode(node);
        Collection answer = (Collection) nodeToOutgoing.get(node);
        // We return an unmodifiable collection to avoid inconsistencies
        // between nodeToIncoming and nodeToOutgoing as a result of changes
        // to just one of the maps.
        // In the future, if removing edges during iteration is desired,
        // we can return a special collection that allows mutations and
        // ensures consistency.
        return Collections.unmodifiableCollection(answer);
    }

    /**  
     * @see tvla.util.graph.Graph#getIncomingEdges(java.lang.Object)
     */
	@Override
    public Collection getIncomingEdges(Object node) {
        assert node != null && containsNode(node);
        Collection answer = (Collection) nodeToIncoming.get(node);
        // We return an unmodifiable collection to avoid inconsistencies
        // between nodeToIncoming and nodeToOutgoing as a result of changes
        // to just one of the maps.
        // In the future, if removing edges during iteration is desired,
        // we can return a special collection that allows mutations and
        // ensures consistency.
        return Collections.unmodifiableCollection(answer);
    }
    
	@Override
	public Edge getEdge(Object from, Object to) {
		assert(from != null && to != null);
		assert(containsNode(from));
		assert(containsNode(to));

		//Edge seekedEdge = new HashEdge(from,to,null);
		Iterator edgeItr = getOutgoingEdges(from).iterator();
		while (edgeItr.hasNext()) {
			Edge edge = (Edge) edgeItr.next();
			if (edge.getSource().equals(from) &&
          edge.getDestination().equals(to)) {
				assert(getIncomingEdges(to).contains(edge));
				assert(getIncomingNodes(to).contains(from));
				assert(getOutgoingNodes(from).contains(to));
				return edge;
			}
		}
			
		assert(!getIncomingNodes(to).contains(from));
		assert(!getOutgoingNodes(from).contains(to));
		return null;
		
	}   

	///////////////////////////////////////////////
	//////             MUTATORS             ///////
	///////////////////////////////////////////////

    /**
     * @see tvla.util.graph.Graph#addNode(java.lang.Object)
     */
	@Override
    public boolean addNode(Object node) {
        assert node != null;
        boolean contained = containsNode(node);
        if (!contained) {
            nodeToOutgoing.put(node, new LinkedHashSet());
        	nodeToIncoming.put(node, new LinkedHashSet());
        }
        ++version;
        return contained;
    }

    /**
     * @see tvla.util.graph.Graph#addEdge(java.lang.Object, java.lang.Object,
     *      java.lang.Object)
     */
	@Override
    public boolean addEdge(Object from, Object to, Object edgeLabel) {
        assert from != null && to != null && containsNode(from)
                && containsNode(to);
        HashEdge newEdge = new HashEdge(from, to, edgeLabel);
        ++version;
        return addEdge(newEdge);
    }

    /**
     * @see tvla.util.graph.Graph#removeNode(java.lang.Object)
     */
	@Override
    public boolean removeNode(Object node) {
        assert node != null;
        boolean contained = containsNode(node);
        if (contained) {
        	// First, disconnect all edges to remove 'node'
        	// from the neighbour sets of other nodes.
        	Collection outEdges = (Collection) getOutgoingEdges(node);
        	Collection inEdges = (Collection) getIncomingEdges(node);
        	Collection edges = new HashSet(inEdges.size() + outEdges.size());
        	edges.addAll(inEdges);
        	edges.addAll(outEdges);
        	for (Object edge : edges) {
        		removeEdge((Edge) edge);
        	}
        	
        	// Now, remove node from the incoming/outgoing maps.
            nodeToOutgoing.remove(node);
            nodeToIncoming.remove(node);
        }
        ++version;
        return contained;
    }

    /**
     * @see tvla.util.graph.Graph#removeEdge(tvla.util.graph.Graph.Edge)
     */
	@Override
    public boolean removeEdge(Edge edge) {
        assert edge != null;
        Object sourceNode = edge.getSource();
        Collection outgoing = (Collection) nodeToOutgoing.get(sourceNode);
        boolean change = outgoing.remove(edge);

        Object destNode = edge.getDestination();
        Collection incoming = (Collection) nodeToIncoming.get(destNode);
        incoming.remove(edge);

        ++version;        
        return change;
    }
    
    /**
     * @see tvla.util.graph.Graph#removeEdge(java.lang.Object, java.lang.Object, java.lang.Object)
     */
	@Override
    public boolean removeEdge(Object from, Object to, Object edgeLabel) {
        assert containsNode(from) && containsNode(to);
        ++version;
        return removeEdge(new HashEdge(from, to, edgeLabel));
    }

    /**
     * @see tvla.util.graph.Graph#removeEdge(java.lang.Object, java.lang.Object)
     */
	@Override
    public boolean removeEdge(Object from, Object to) {
        assert containsNode(from) && containsNode(to);
        
        boolean change = false;
        
        Collection outgoing = (Collection) nodeToOutgoing.get(from);
        for (Iterator edgeIter = outgoing.iterator(); edgeIter.hasNext(); ) {
            Edge edge = (Edge) edgeIter.next();
            if (edge.getDestination().equals(to)) {
                change = true;
                edgeIter.remove();
            }
        }
        
        // The following condition is a small optimization.  Since every edge
        // exists in both incoming and outgoing maps, the meaning of
        // change==false means that there is no edge between from and to,
        // and so there is no need to continue working on the incoming
        // collection.
        if (change) {            
            Collection incoming = (Collection) nodeToIncoming.get(to);
            for (Iterator edgeIter = incoming.iterator(); edgeIter.hasNext();) {
                Edge edge = (Edge) edgeIter.next();
                if (edge.getSource().equals(from)) {
                    edgeIter.remove();
                }
            }
        }
        
        ++version;        
        return change;
    }
    
    /**
     * @see tvla.util.graph.Graph#removeAllEdges(java.util.Collection)
     */
//    public boolean removeAllEdges(Collection edges) {
//        assert edges != null;
//        boolean change = false;
//        for (Iterator outgoingIter = nodeToOutgoing.values().iterator(); outgoingIter.hasNext(); ) {
//            Collection outgoingEdges = (Collection) outgoingIter.next();
//            change |= outgoingEdges.removeAll(edges);
//        }
//        for (Iterator incomingIter = nodeToIncoming.values().iterator(); incomingIter.hasNext(); ) {
//            Collection dataArray = (Collection) incomingIter.next();
//            dataArray.removeAll(edges);
//        }
//        ++version;
//    }

    protected boolean addEdge(Edge edge) {
        Object from = edge.getSource();
        Object to = edge.getDestination();

        Collection outgoing = (Collection) nodeToOutgoing.get(from);
        boolean result = outgoing.add(edge);

        Collection incoming = (Collection) nodeToIncoming.get(to);
        incoming.add(edge);

        ++version;
        return result;
    }

    @Override    
    public void mergeInto(Object fromNode, Object toNode) {
    	assert(fromNode != null && toNode != null && fromNode != toNode);
 
    	Collection outgoingFrom = (Collection) nodeToOutgoing.get(fromNode); 
    	Collection incomingFrom = (Collection) nodeToIncoming.get(fromNode);
    	assert(outgoingFrom != null && incomingFrom != null);

    	Collection outgoingTo = (Collection) nodeToOutgoing.get(toNode); 
    	assert(outgoingTo != null);
    	
    	if (! outgoingFrom.isEmpty()) {
    		Iterator inItr = outgoingFrom.iterator();
    		while (inItr.hasNext()) {
    			Edge oldInEdge = (Edge) inItr.next();
    			Edge newInEdge = new HashEdge(oldInEdge.getDestination(), toNode, oldInEdge.getLabel());
    			addEdge(newInEdge);
    		}
    	
    		Iterator outItr = outgoingFrom.iterator();
    		while (outItr.hasNext()) {
    			Edge oldOutEdge = (Edge) outItr.next();
    			Edge newOutEdge = new HashEdge(toNode, oldOutEdge.getDestination(), oldOutEdge.getLabel());
    			addEdge(newOutEdge);
    		}
    	}
    	
    	removeNode(fromNode);
    }

    // See tvla.util.graph.Graph#shallowCopy()
    @Override
	public Graph shallowCopy() {
		Graph cp = new HashGraph(nodeToOutgoing.size());
		
		if (nodeToOutgoing.isEmpty())
			return cp;
		
		// populating the copied graph nodes
		Iterator nodeItr = nodeToOutgoing.keySet().iterator();
		while (nodeItr.hasNext()) {
			Object node = nodeItr.next();
			cp.addNode(node);
		}

		// Initializing the copied graph edges accroding to the graph's edges
		Iterator outEdgesItr = getEdges().iterator();
		while (outEdgesItr.hasNext()) {
			Object edgeObj = outEdgesItr.next();
			HashEdge edge = (HashEdge) edgeObj;
			cp.addEdge(edge.source, edge.destination, edge.getLabel());
		}

		return cp;
	}	

    // See tvla.util.graph.Graph#clear()
	public void clear() {
		nodeToIncoming.clear();	
		nodeToOutgoing.clear();
	}
    
    
    /**
     * An implementation of Graph.Edge suitable for storing in a hash table.
     */
    protected class HashEdge implements Graph.Edge {
        /**
         * The source node.
         */
        protected final Object source;

        /**
         * The destination node.
         */
        protected final Object destination;

        /**
         * The edge label.
         */
        protected final Object label;

        /**
         * The initial hash code is stored and later verified to make sure that
         * it doesn't change; changing it would break the underlying
         * representation based on hash tables.
         */
        private final int cachedHashCode;

        /**
         * Returns the node at the source of the edge.
         */
        @Override
        public Object getSource() {
            return source;
        }

        /**
         * Returns the node at the destination of the edge.
         */
        @Override
        public Object getDestination() {
            return destination;
        }

        /**
         * Returns the label on this edge.
         */
        @Override
        public Object getLabel() {
            return label;
        }

        /**
         * @see java.lang.Object#equals(Object)
         */
        @Override
        public boolean equals(Object other) {
            if (other instanceof Edge) {
                Edge otherEdge = (Edge) other;
                Object otherEdgeInfo = otherEdge.getLabel();
                return this.source.equals(otherEdge.getSource())
                        && this.destination.equals(otherEdge.getDestination())
                        && (   (this.label == null && otherEdgeInfo == null)
                            || (this.label != null && this.label.equals(otherEdgeInfo)));
            }
            else {
                return false;
            }
        }

        /**
         * Returns the hash code of the edge.
         */
        @Override
        public int hashCode() {
            int hashResult = ((source.hashCode() * 31) + destination.hashCode()) * 31;

            // The check could be made an assertion if performance
            // becomes a problem.
            if (hashResult != cachedHashCode)
                throw new RuntimeException("Hascode has changed for the edge :"
                        + toString()
                        + " breaking the underlying graph representation!");

            return hashResult;
        }

        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            if (label == null)
                return source + "->" + destination;
            else
                return source + "->" + destination + ":" + label;
        }

        /**
         * Constructs an edge from the specified nodes and info object.
         */
        protected HashEdge(Object source, Object destination, Object info) {
            assert source != null && destination != null;
            this.source = source;
            this.destination = destination;
            this.label = info;

            // Compute the hash code and store the result.
            int hashResult = ((source.hashCode() * 31) + destination.hashCode()) * 31;
            cachedHashCode = hashResult;
        }
    }
}