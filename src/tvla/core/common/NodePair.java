package tvla.core.common;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.util.SimpleIterator;

/** A specialized implementation of NodeTuple for pairs of nodes.
 * @author Roman Manevich
 * @since tvla-2-alpha (18 May 2002) Initial creation.
 */
public class NodePair extends NodeTuple {
	protected final Node first;
	protected final Node second;

	/** Creates another tuple where each occurance of source is
	 * replaced with dest.
	 */
	public NodeTuple substitute(Node source, Node dest) {
		Node newFirst = first.equals(source) ? dest : first;
		Node newSecond = second.equals(source) ? dest : second;
		return new NodePair(newFirst, newSecond);
	}
	
	/** Conducts a lexicographical comparison between this tuple and the
	 * specified tuple.
	 */
	public boolean equals(Object o) {
		if (o instanceof NodePair) {
			NodePair pair = (NodePair)o;
			return first.id() == pair.first.id() && second.id() == pair.second.id();
		}
		
		NodeTuple other = (NodeTuple) o;
		if (other.size() != 2)
			return false;
		
		return first.equals(other.get(0)) && second.equals(other.get(1));
	}
	
	public int compareTo(NodeTuple o) {
		NodeTuple other = (NodeTuple) o;
		if (other.size() != 2)
			return 2 - other.size();
		Node otherFirst = other.get(0);
		if (!first.equals(otherFirst))
			return first.id() - otherFirst.id();
		return second.id() - other.get(1).id();
	}
	
	/** Computes the tuple's has code.
	 */
	public int hashCode() {
		return first.hashCode() * 257 + second.hashCode();
	}

	/** Constructs and initializes a tuple with the specified node array.
	 */
	public NodePair(Node first, Node second) {
		this.first = first;
		this.second = second;
		assert first != null && second != null : "Attempt to create a node pair with null argumets!";
	}
	
	/** Retrieves the node in the specified position.
	 */
	public Node get(int index) {
		switch (index) {
		case 0: return first;
		case 1: return second;
		default: throw new IllegalArgumentException("An illegal index was passed " +
					"to NodePair.get() : " + index);
		}
	}
	
	final public Node first() {
		return first;
	}
	
	final public Node second() {
		return second;
	}

	/** Returns the number of nodes in the tuple.
	 */
	public final int size() {
		return 2;
	}
	
	/** Checks whether the specified node is contained in the tuple.
	 */
	public boolean contains(Node n) {
		return n.equals(first) || n.equals(second);
	}
	
	/** Returns a human-readable representation of the tuple.
	 */
	public String toString() {
		return "(" + first + ", " + second + ")";
	}
	
    public Iterator<? extends NodeTuple> matchingTuples(final Map<Node, Set<Node>> mapping) {
        return new SimpleIterator<NodeTuple>() {
            Iterator<Node> leftIter = mapping.get(first).iterator();
            Node leftNode = leftIter.next();
            Set<Node> rightSet = mapping.get(second);
            Iterator<Node> rightIter = rightSet.iterator();
            @Override
            protected NodeTuple advance() {
                if (!rightIter.hasNext()) {
                    if (!leftIter.hasNext()) {
                        return null; // Done
                    }
                    leftNode = leftIter.next();
                    rightIter = rightSet.iterator();
                }
                Node rightNode = rightIter.next();
                return createPair(leftNode, rightNode);
            }
        };        
    }	
}