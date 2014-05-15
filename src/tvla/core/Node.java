package tvla.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A node in the universe 
 * @see tvla.core.TVS
 * @author Tal Lev-Ami
 * @since 26/12/2000 added implementation of the Comparable interface (Roman).
 */
public class Node extends NodeTuple  {
	private static int idCounter = 0;

    protected int id;
	
	protected static List<Node> canonicNodes = new ArrayList<Node>(); 

	public static void reset() {
		idCounter = 0;
		canonicNodes = new ArrayList<Node>(); 
	}
	
	public static Node nodeForID(int id) {
		return canonicNodes.get(id);
	}

    public static int getMaxId() {
        return idCounter;
    }

	/** Allocate a new never before used node name.
	 */
	public static Node allocateNode() {
		if (idCounter == Integer.MAX_VALUE)
			throw new InternalError("Node ID overflow");
		Node node = new Node(idCounter++);
		canonicNodes.add(node);
		assert canonicNodes.size() == idCounter;
		return node;
	}

	/** Retrieves the node in the specified position.
	 */
	public Node get(int index) {
		if (index == 0)
			return this;
		else
			throw new IllegalArgumentException("An illegal index was passed to " +
				"Node.get() : + " + index);
	}
	
	/** Returns the number of nodes in the tuple.
	 */
	public final int size() {
		return 1;
	}
	
	/** Creates another tuple where each occurance of source is
	 * replaced by dest.
	 */
	public NodeTuple substitute(Node source, Node dest) {
		if (this.equals(source))
			return dest;
		else
			return this;
	}

	/** Checks whether the specified node is contained in the tuple.
	 */
	public boolean contains(Node n) {
		return this.equals(n);
	}
	
	/** Return a unique name of the node.
	 */
	public final String name() {
		return "" + id();
	}

	/** Return a human readable representation of the node.
	 */
	public final String toString() {
		return "" + id();
	}

	/** Compare this node with the given node by name.
	 */
	public boolean equals(Object other) {
		if (other instanceof Node) {
			return this.id == ((Node) other).id;
		}
		else
			return super.equals(other);
	}
	
	/** Uses the nodes's id for hashing.
	 */
	public int hashCode() {
		return id;
	}
	
	/** Returns the node's id. The id is unique per structures.
	 */
	public int id() {
		return id;
	}
	
	/** Compares this node with the specified node for order.
	 * @author Roman Manevich
	 * @since 26/12/2000 Initial creation.
	 */
	public int compareTo(NodeTuple o) {
		return ( this.id() - ((Node)o).id() );
	}

	protected Node(int id) {
		this.id = id;
	}
	
    public Node mapNodeTuple(Map<Node, Node> nodeMapping) {
        return nodeMapping.get(this);
    }	
    
    public Iterator<? extends NodeTuple> matchingTuples(Map<Node, Set<Node>> mapping) {
        return mapping.get(this).iterator();
    }
}