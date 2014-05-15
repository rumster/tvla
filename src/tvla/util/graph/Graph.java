/*
 * Created on Mar 21, 2004
 *
 */
package tvla.util.graph;

import java.util.Collection;

/**
 * An interface for a mutable directed graph ADT.  The graph nodes are
 * arbitrary objects supplied by the graph clients.
 * 
 * Any restrictions and possibilities on the form of the graph, e.g., whether
 * it is directed or undirected, can have self-loops or not, can have multiple
 * edges between two nodes (i.e., be a hyper-graph) is left to the specific
 * implementation.
 * 
 * @author Roman Manevich
 */
public interface Graph {
    /**
     * Returns the set of nodes in the graph in a collection.
     * 
     * Postcondition: ret_val != null &&
     * tvla.util.CollectionUtils.assertNodeDuplicates(ret_val)
     * 
     * TODO Implement tvla.util.CollectionUtils.assertNoDuplicates
     */
    public Collection getNodes();

    /**
     * Returns the total number of edges in the graph. <br>
     * Postcondition: ret_val > 0 || (ret_val == 0 && isEmpty)
     * 
     * @return The number of edges in the graph.
     */
    public int getNumberOfEdges();

    /**
     * Returns a collection of <code>Edge</code>s that have <code>node</code>
     * as their source node. <br>
     * Precondition: node != null && containsNode(node) <br>
     * Postcondition: ret_val != null
     * 
     * @return A collection of <code>Edge</code>s.
     */
    public Collection getOutgoingEdges(Object node);

    /**
     * Returns a collection of nodes that have <code>node</code> as their
     * source node.<br>
     * Precondition: node != null && containsNode(node) <br>
     * Postcondition: ret_val != null
     * 
     * @return A collection of Objects.
     */
    public Collection getOutgoingNodes(Object node);

    /**
     * Returns a collection of <code>Edge</code>s that have <code>node</code> as
     * their destination node. <br>
     * Precondition: node != null && containsNode(node) <br>
     * Postcondition: ret_val != null
     * 
     * @return A collection of <code>Edge</code>s.
     */
    public Collection getIncomingEdges(Object node);

    /**
     * Returns a collection of nodes that have <code>node</code> as their
     * destination node. <br>
     * Precondition: node != null && containsNode(node) <br>
     * Postcondition: ret_val != null
     * 
     * @return A collection of Objects.
     */
    public Collection getIncomingNodes(Object node);

    /**
     * Returns the number of incoming edges of <code>node</code>.<br>
     * Precondition: node != null && containsNode(node)
     * 
     * @param node
     *            An object.
     * @return The number incoming edges of <code>node</code>.
     *  
     */
    public int getInDegree(Object node);

    /**
     * Returns the number of outgoing edges of <code>node</code>.<br>
     * Precondition: node != null && containsNode(node)
     * 
     * @param node
     *            An object.
     * @return The number of outgoing edges of <code>node</code>.
     */
    public int getOutDegree(Object node);

    /**
     * Returns the total number edges incident on <code>node</code>.<br>
     * Precondition: node != null && containsNode(node)
     * 
     * @param node
     *            An object.
     * @return The total number edges incident on <code>node</code>.
     */
    public int getDegree(Object node);

    /**
     * Returns true if this graph contains no nodes.
     * 
     * @return true if this graph contains no nodes.
     */
    public boolean isEmpty();

    /**
     * Returns true if this graph contains <code>node</code>.<br>
     * Precondition: node != null
     * 
     * @param node
     *            An object.
     * @return true if this graph contains <code>node</code>.
     */
    public boolean containsNode(Object node);

    /**
     * Returns true if the graph contains an edge between <code>from</code>
     * and <code>to</code> labelled by <code>edgeLabel</code>.<br>
     * This operation is optional.<br>
     * Precondition: from != null && to != null && containsNode(from) &&
     * containsNode(to)
     * 
     * @param from
     *            An object
     * @param to
     *            An object
     * @param edgeLabel
     *            An object
     * @return true if the graph contains an edge between <code>from</code>
     *         and <code>to</code> with <code>edgeLabel</code>.
     */
    public boolean containsEdge(Object from, Object to, Object edgeLabel);

    /**
     * Returns true if the graph contains an edge between <code>from</code>
     * and <code>to</code>.<br>
     * Precondition: from != null && to != null && containsNode(from) &&
     * containsNode(to)
     * 
     * @param from
     *            An object
     * @param to
     *            An object
     * @return true if the graph contains any edge between <code>from</code>
     *         and <code>to</code>.
     */
    public boolean containsEdge(Object from, Object to);

    /**
     * Makes the given object a node in the graph. <br>
     * Precondition: node != null<br>
     * Postcondition: containsNode(node) && ret_val == pre(containsNode(node))
     * 
     * @param node
     *            A node graph.
     * @return true is <code>node</code> already existed in the graph.
     */
    public boolean addNode(Object node);

    /**
     * Adds an edge between <code>from</code> and <code>to</code> labelled by
     * <code>edgeLabel</code>.<br>
     * This operation is optional.<br>
     * Precondition: from != null && to != null && containsNode(from) &&
     * containsNode(to) <br>
     * Postcondition: containsEdge(from, to, edgeLabel)
     * 
     * @return <code>true</code> if this graph changed as a result of the call. 
     */
    public boolean addEdge(Object from, Object to, Object edgeLabel);

    /**
     * Adds an edge between <code>from</code> and <code>to</code>.<br>
     * Precondition: from != null && to != null && contains(from) &&
     * contains(to) <br>
     * Postcondition: containsEdge(from, to)
     * 
     * @return <code>true</code> if this graph changed as a result of the call.
     */
    public boolean addEdge(Object from, Object to);

    /**
     * Removes the specified node from the graph. <br>
     * Precondition: node != null<br>
     * Postcondition: !containsNode(node)
     * 
     * @param node
     *            A node in the graph.
     * @return true if the graph contained the specified element
     */
    public boolean removeNode(Object node);

    /** Removes the specified edge from the graph.
     * 
     * @param edge An edge. 
     * @return <code>true</code> if the graph changed as a result of the call.
     */
    public boolean removeEdge(Edge edge);

    /**
     * Removes all edges between <code>from</code> and <code>to</code> that
     * are labelled with <code>edgeLabel</code>.<br>
     * This operation is optional.<br>
     * Precondition: from != null && to != null && containsNode(from) &&
     * containsNode(to) && containsEdge(from, to)
     * 
     * @param from
     *            A graph node.
     * @param to
     *            A graph node.
     * @param edgeLabel
     *            An object.
     * @return <code>true</code> if the graph changed as a result of the call.
     */
    public boolean removeEdge(Object from, Object to, Object edgeLabel);

    /**
     * Removes all edges between <code>from</code> and <code>to</code>.<br>
     * This operation is optional.<br>
     * Precondition: from != null && to != null && containsNode(from) &&
     * containsNode(to) && containsEdge(from, to)
     * 
     * @param from
     *            A graph node.
     * @param to
     *            A graph node.
     * @return <code>true</code> if the graph changed as a result of the call.
     */
    public boolean removeEdge(Object from, Object to);

    /**
     * Removes the nodes in <code>c</code> from the graph.<br>
     * This operation is optional.<br>
     * Precondition: c != null && forall node in c { containsNode(node) }
     * 
     * @param nodes
     *            A collection.
     * @return <code>true</code> if the graph changed as a result of the call.
     */
    public boolean removeAllNodes(Collection c);

    /**
     * Removes the edges in <code>c</code> from the graph.<br>
     * This operation is optional.<br>
     * Precondition: c != null && forall edge in c { containsEdge(edge) }
     * 
     * @param edges
     *            A collection.
     * @return <code>true</code> if the graph changed as a result of the call.
     */
    public boolean removeAllEdges(Collection c);
    
    /**
     * Removes the edges not in <code>c</code> from the graph.<br>
     * This operation is optional.<br>
     * Precondition: c != null && forall edge in c { containsEdge(edge) }
     * 
     * @param edges
     *            A collection.
     * @return <code>true</code> if the graph changed as a result of the call.
     */
    public boolean retainAllEdges(Collection c);
    
    /**
     * Removes the nodes that are not in <code>c</code> from the graph.<br>
     * This operation is optional.<br>
     * Precondition: c != null && forall node in c { containsNode(node) }
     * 
     * @param nodes
     *            A collection.
     * @return <code>true</code> if the graph changed as a result of the call.
     */
    public boolean retainAllNodes(Collection c);
    
    /**
     * Merges the node from into to by redirecting all edges incoming/leaving fromNode
     * to enter / leave toNode. After the merge, fromNode is removed from the graph.
     * 
     * @param from a graph node
     * @param to a graph node
     * 
     * pre fromNode and toNode are distinct (i.e., fromNode != toNode) and in the graph.  
     */
    public void mergeInto(Object fromNode, Object toNode); 

    /////////////////////////////////////////////////////////////
    ///                   Additional  Methods                 ///
    /////////////////////////////////////////////////////////////
    

	/** Returns the number of nodes in the graph.
	 * 
	 * @return The number of nodes in the graph.
	 */
	public int getNumberOfNodes();
	
	/** Returns the set of edges in the graph.
	 */
	public Collection getEdges();

	
	/** Returns a copy of the graph.
	 * This is a shallow clone: the same objects are used for nodes, 
	 * and edge info. 
	 * @author maon
	 */
	public Graph shallowCopy();

	/**
	 * Returns the graph edge between from and to.
	 * Optional method.
	 * @param from a node in the graph
	 * @param to a node in the graph
	 * @return a pointer to the edge, or a null if 
	 * an edge from->to does not exist.
	 */
	public Edge getEdge(Object from, Object to);


	/**
	 * Removes all nodes and all edges from the graph.
	 * Optional method.
	 */
	public void clear();

    /**
     * An interface for a labeled graph edge.
     */
    public interface Edge {
        /**
         * Returns the node at the source of the edge.
         * 
         * Postcondition: ret_val != null
         */
        public Object getSource();

        /**
         * Returns the node at the destination of the edge.
         * 
         * Postcondition: ret_val != null
         */
        public Object getDestination();

        /**
         * Returns the label associated with the edge (possibly null).<br>
         * This operation is optional.
         */
        public Object getLabel();
    }
}