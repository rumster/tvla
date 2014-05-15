package tvla.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.core.common.ArbitrarySizeNodeTuple;
import tvla.core.common.EmptyNodeTuple;
import tvla.core.common.NodePair;
import tvla.util.ComposeIterator;
import tvla.util.SimpleIterator;

/** This class represents an immutable ordered set of nodes.
 * @author Eran Yahav
 * @author Roman Manevich
 * @since tvla-2-alpha (May 12 2002)
 */
public abstract class NodeTuple implements Comparable<NodeTuple> {
	/** An empty node tuple (used in case of nullary predicates).
	 */
	public static final NodeTuple EMPTY_TUPLE = EmptyNodeTuple.theEmptyTuple;
	
	/** Creates and returns a tuple with a single node.
	 */
	public static NodeTuple createSingle(Node n) {
		assert n != null;
		return n;
	}

	/** Creates and returns a tuple with a pair of nodes.
	 */
	public static NodeTuple createPair(Node first, Node second) {
		assert first != null && second != null;
		return new NodePair(first, second);
	}

	/** Creates and returns a tuple of nodes.
	 */
	public static NodeTuple createTuple(List<Node> nodeList) {
		switch (nodeList.size()) {
			case 0 :
				return EMPTY_TUPLE;
			case 1 :
				return (Node) nodeList.get(0);
			case 2 :
				return new NodePair(
					(Node) nodeList.get(0),
					(Node) nodeList.get(1));
			default :
				return new ArbitrarySizeNodeTuple(nodeList);
		}
	}

	/** Creates and returns a tuple of nodes.
	 */
	public static NodeTuple createTuple(Node[] nodes) {
		switch (nodes.length) {
			case 0 :
				return EMPTY_TUPLE;
			case 1 :
				return nodes[0];
			case 2 :
				return new NodePair(
						nodes[0], nodes[1]);
			default :
				return new ArbitrarySizeNodeTuple(nodes);
		}
	}

	/** Retrieves the node in the specified position.
	 */
	public abstract Node get(int index);

	/** Returns the number of nodes in the tuple.
	 */
	public abstract int size();

	/** Creates another tuple where each occurance of source is
	 * replaced by dest.
	 */
	public NodeTuple substitute(Node source, Node dest) {
		assert source != null && dest != null;
		
		Node[] nodesTmp = new Node[size()];
		for (int index = 0; index < size(); ++index) {
			Node thisNode = get(index);
			if (thisNode.equals(source))
				nodesTmp[index] = dest;
			else
				nodesTmp[index] = thisNode;
		}
		
		return new ArbitrarySizeNodeTuple(nodesTmp);
	}

	/** Conducts a lexicographical comparison between this tuple and the
	 * specified tuple.
	 */
	public boolean equals(Object o) {
	    if (o == null) {
	        return false;
	    }
		NodeTuple other = (NodeTuple) o;
		int otherSize = other.size();

		if (this.size() != otherSize)
			return false;

		for (int index = 0; index < otherSize; ++index) {
			Node thisNode = (Node) get(index);
			Node otherNode = (Node) other.get(index);
			if (!thisNode.equals(otherNode))
				return false;
		}
		return true;
	}
	
	public abstract int compareTo(NodeTuple o);

	/** Computes the tuple's has code.
	 */
	public int hashCode() {
		int result = 0;
		for (int i = 0; i < size(); ++i) {
			Node n = (Node) get(i);
			result = result * 257 + n.hashCode();
		}
		return result;
	}

	/** Checks whether the specified node is contained in the tuple.
	 */
	public boolean contains(Node n) {
		for (int i = 0; i < size(); ++i) {
			if (n.equals((Node) get(i)))
				return true;
		}
		return false;
	}

	/** Returns a human-readable representation of the tuple.
	 */
	public String toString() {
		StringBuffer result = new StringBuffer(8);
		result.append("(");
		for (int i = 0; i < size(); ++i) {
			Node n = (Node) get(i);
			result.append(n);
			if (i + 1 < size())
				result.append(", ");
		}
		result.append(")");
		return result.toString();
	}

    static Node[][] tempTuples = new Node[][] { {}, {null}, {null,null}, {null, null, null}, {null,null,null,null}};    
    /**
     * Map the given tuple according to the node mapping.
     */
    public NodeTuple mapNodeTuple(Map<Node, Node> nodeMapping) {
        Node[] otherTupleNodes = tempTuples[this.size()];
        for (int i = 0; i < otherTupleNodes.length; i++) {
            otherTupleNodes[i] = nodeMapping.get(this.get(i));
        }
        return createTuple(otherTupleNodes);
    }

    /**
     * Nice generic version of matchingTuples. Slower :(
     */
    public Iterator<? extends NodeTuple> matchingTuples(Map<Node, Set<Node>> mapping) {			
    	final List<Set<Node>> buckets = new ArrayList<Set<Node>>();
    	for (int i = 0; i < size(); i++) {
    		buckets.add(mapping.get(get(i)));
    	}
    	return new Iterator<NodeTuple>() {
    		Iterator<List<Node>> matchingNodesIter = new ComposeIterator<Node>(buckets);
    		public boolean hasNext() {
    			return matchingNodesIter.hasNext();
    		}
    
    		public NodeTuple next() {
    			List<Node> next = matchingNodesIter.next();
    			return createTuple(next);
    		}
    
    		public void remove() {
    			matchingNodesIter.remove();
    		}			
    	};
    }
}