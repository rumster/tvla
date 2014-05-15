/*
 * Created on 25/11/2004
 *
 */
package tvla.util.graph;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This class provides a skeletal implementation of the Graph interface, to
 * minimize the effort required to implement this interface.
 * 
 * @author Roman Manevich
 */
public abstract class AbstractGraph implements Graph {
    /** Used internally to keep track of mutations in order to avoid concurrent
     * modifications.
     */
    protected int version;

    /** Returns a hash code for the graph.
     */
    @Override
    public int hashCode() {
        int result = 0;
        for (Iterator nodeIter = getNodes().iterator(); nodeIter.hasNext();) {
            Object node = nodeIter.next();
            result += node.hashCode() * 31;
            result += getOutgoingEdges(node).hashCode() * 31;
        }
        return result;
    }
    
    

    /**
     * Returns the total number of nodes in the graph.
     */
    @Override
	public int getNumberOfNodes() {
		return getNodes().size();
	}    

    /**
     * Returns the total number of edges in the graph by summing the out degrees
     * of all nodes. The operation takes time linear in the number of nodes.
     */
    @Override
    public int getNumberOfEdges() {
        int result = 0;
        for (Iterator nodeIter = getNodes().iterator(); nodeIter.hasNext();) {
            Object node = nodeIter.next();
            Collection outgoingEdges = getOutgoingEdges(node);
            result += outgoingEdges.size();
        }
        return result;
    }

    /**
     * @see tvla.util.graph.Graph#getOutgoingNodes(java.lang.Object)
     */
    @Override
    public Collection getOutgoingNodes(Object node) {
        assert node != null && containsNode(node);
        return new EdgeCollectionHelper(getOutgoingEdges(node), true);
    }

    /**
     * @see tvla.util.graph.Graph#getIncomingNodes(java.lang.Object)
     */
    @Override
    public Collection getIncomingNodes(Object node) {
        assert node != null && containsNode(node);
        return new EdgeCollectionHelper(getIncomingEdges(node), false);
    }

    /**
     * Returns <code>true</code> whenever <code>getNodes().isEmpty()</code>
     * returns <code>true</code>.
     * 
     * @return <code>true</code> whenever <code>getNodes().isEmpty()</code>
     * returns <code>true</code>.
     */
    @Override
    public boolean isEmpty() {
        return getNodes().isEmpty();
    }

    /**
     * Returns <code>true</code> whenever
     * <code>getNodes().contains(node)</code> returns <code>true</code>.
     * 
     * @return <code>true</code> whenever
     * <code>getNodes().contains(node)</code> returns <code>true</code>.
     */
    @Override
    public boolean containsNode(Object node) {
        assert node != null;
        return getNodes().contains(node);
    }

    /**
     * Conducts a linear search over the outgoing nodes of <code>from</from>
     * for an edge with <code>to</code>.
     */
    @Override
    public boolean containsEdge(Object from, Object to) {
        assert from != null && to != null && containsNode(from)
                && containsNode(to);
        for (Iterator i = getOutgoingNodes(from).iterator(); i.hasNext();) {
            if (i.next().equals(to))
                return true;
        }
        return false;
    }

    /**
     * Conducts a linear search over the outgoing nodes of <code>from</from>
     * for an edge with <code>to</code> and <code>edgeLabel</code>.
     */
    @Override
    public boolean containsEdge(Object from, Object to, Object edgeLabel) {
        assert from != null && to != null && containsNode(from)
                && containsNode(to);
        for (Iterator i = getOutgoingEdges(from).iterator(); i.hasNext();) {
            Edge edge = (Edge) i.next();
            if (edge.getDestination().equals(to)) {
                return edgeLabel == null ? edge.getLabel() == null : edgeLabel
                        .equals(edge.getLabel());
            }
        }
        return false;
    }

    /**
     * @see tvla.util.graph.Graph#containsEdge(tvla.util.graph.Graph#Edge)
     */
    public boolean containsEdge(Edge edge) {
        assert edge != null;
        return getOutgoingEdges(edge.getSource()).contains(edge);
    }

    /**
     * Returns <code>getIncomingEdges(node).size()</code>.
     * 
     * @param node
     *            An object.
     * @return <code>getIncomingEdges(node).size()</code>.
     */
    @Override
    public int getInDegree(Object node) {
        return getIncomingEdges(node).size();
    }

    /**
     * Returns <code>getOutgoingEdges(node).size()</code>.
     * 
     * @param node
     *            An object.
     * @return <code>getOutgoingEdges(node).size()</code>.
     * 
     * Precondition: node != null
     */
    @Override
    public int getOutDegree(Object node) {
        return getOutgoingEdges(node).size();
    }

    /**
     * Returns <code>getIncomingEdges(node).size() + getOutgoingEdges(node).size()</code>.
     * 
     * @param node
     *            An object.
     * @return <code>getIncomingEdges(node).size() + getOutgoingEdges(node).size()</code>.
     * 
     * Precondition: node != null
     */
    @Override
    public int getDegree(Object node) {
        return getIncomingEdges(node).size() + getOutgoingEdges(node).size();
    }

    /** Same as <code>addEdge(from, to, null)</code>.
     */
    @Override
    public boolean addEdge(Object from, Object to) {
        return addEdge(from, to, null);
    }

    /**
     * @see tvla.util.graph.Graph#removeAllNodes(java.util.Collection)
     */
    @Override
    public boolean removeAllNodes(Collection c) {
        assert c != null;
        boolean change = false;
        // We take a snapshot of the collection to avoid concurrently
        // modifying the graph (in case c is a view of some internal
        // data structure used by the graph).
        Object[] snapshot = c.toArray();
        for (int i = 0; i < snapshot.length; ++i) {
            change |= removeNode(snapshot[i]);
        }
        return change;
    }

    @Override
    public boolean retainAllNodes(Collection c) {
        assert c != null;
        boolean change = false;
        // We take a snapshot of the collection to avoid concurrently
        // modifying the graph (in case c is a view of some internal
        // data structure used by the graph).
        Set snapshot = new HashSet(getNodes());
        snapshot.removeAll(c);
        for (Object node : snapshot) {
            change |= removeNode(node);
        }
        return change;
    }
    
    /**
     * @see tvla.util.graph.Graph#removeAllEdges(java.util.Collection)
     */
    @Override
    public boolean removeAllEdges(Collection c) {
        assert c != null;
        boolean change = false;
        // We take a snapshot of the collection to avoid concurrently
        // modifying the graph (in case c is a view of some internal
        // data structure used by the graph).
        Object[] snapshot = c.toArray();
        for (int i = 0; i < snapshot.length; ++i) {
            change |= removeEdge((Edge) snapshot[i]);
        }
        return change;
    }
    
    /**
     * @see tvla.util.graph.Graph#retainAllEdges(java.util.Collection)
     */
    @Override
    public boolean retainAllEdges(Collection c) {
        assert c != null;
        boolean change = false;
        // We take a snapshot of the collection to avoid concurrently
        // modifying the graph (in case c is a view of some internal
        // data structure used by the graph).
        Set snapshot = new HashSet(getEdges());
        snapshot.removeAll(c);
        for (Object edge : snapshot) {
            change |= removeNode(edge);
        }
        return change;
    } 

    /** Returns a string representation of the graph as a set of edges.
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer("{");
        int edgeCounter = getNumberOfEdges();
        for (Iterator nodeIter = getNodes().iterator(); nodeIter.hasNext(); ) {
            Object node = nodeIter.next();
            for (Iterator edgeIter = getOutgoingEdges(node).iterator(); edgeIter.hasNext(); --edgeCounter) {
                result.append(edgeIter.next());
                if (edgeCounter > 1)
                    result.append(", ");
            }
        }
        result.append("}");
        return result.toString();
    }

    /**
     * A helper class for supplying different views of edge collections.
     * 
     * @author Roman Manevich
     *  
     */
    protected class EdgeCollectionHelper extends AbstractCollection {
        /**
         * The collection of edges being viewed.
         */
        protected final Collection edges;

        /**
         * Specifies the part of the edge desired to be viewed: true for the
         * destination node and false for the source node.
         */
        protected final boolean outgoing;

        /** Used to avoid concurrent modifications to the underlying graph
         * while viewing it with this helper.
         */
        protected final int myVersion;

        /**
         * Constructs an edge collection helper from a collection of
         * <code>Edge</code> objects and a flag that specifies the part of the
         * edges that should be viewable.
         * 
         * @param edges
         *            A collection of <code>Edge</code>s.
         * @param outgoing
         *            Specifies the part of the edge desired to be viewed: true
         *            for the destination node and false for the source node.
         * 
         * Precondition: edges != null
         */
        public EdgeCollectionHelper(Collection edges, boolean outgoing) {
            this.edges = edges;
            this.outgoing = outgoing;
            myVersion = version;
        }

        /**
         * @see java.util.Collection#size()
         */
        @Override
        public int size() {
            return edges.size();
        }

        /**
         * @see java.util.Collection#iterator()
         */
        @Override
        public Iterator iterator() {
            return new EdgeIteratorWrapper(edges.iterator());
        }

        protected class EdgeIteratorWrapper implements Iterator {
            protected final Iterator edgeIter;

            public EdgeIteratorWrapper(Iterator transitionIter) {
                this.edgeIter = transitionIter;
            }

            @Override
            public boolean hasNext() {
                if (myVersion != version)
                    throw new ConcurrentModificationException("A concurrent "
                            + "modification to the graph has occurred!");
                return edgeIter.hasNext();
            }

            @Override
            public Object next() {
                if (myVersion != version)
                    throw new ConcurrentModificationException("A concurrent "
                            + "modification to the graph has occurred!");
                Edge edge = (Edge) edgeIter.next();
                Object result = null;
                if (outgoing) {
                    result = edge.getDestination();
                }
                else {
                    result = edge.getSource();
                }
                return result;
            }

            /**
             * Mutations to the underlying edge collection are not allowed.
             */
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
    }
}