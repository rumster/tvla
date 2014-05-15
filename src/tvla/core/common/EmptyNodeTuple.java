package tvla.core.common;

import tvla.core.Node;
import tvla.core.NodeTuple;

/** A specialized implementation of NodeTuple for empty node tuples.
 * @author Roman Manevich
 * @since tvla-2-alpha (18 May 2002) Initial creation.
 */
public final class EmptyNodeTuple extends NodeTuple {
	/** The one and only instance of this class.
	 */
	public static final EmptyNodeTuple theEmptyTuple = new EmptyNodeTuple();
																		  
	/** Creates another tuple where each occurance of source is
	 * replaced with dest.
	 */
	public NodeTuple substitute(Node source, Node dest) {
		return this;
	}
	
	/** Conducts a lexicographical comparison between this tuple and the
	 * specified tuple.
	 */
	public boolean equals(Object o) {
		return ((NodeTuple)o).size() == 0;
	}
	
	/** Computes the tuple's has code.
	 */
	public int hashCode() {
		return 0;
	}
	
	public int compareTo(NodeTuple other) {
		return -other.size();
	}
	
	/** Retrieves the node in the specified position.
	 */
	public Node get(int index) {
		throw new RuntimeException("Illegal call to get() on the empty node tuple!");
	}
	
	/** Returns the number of nodes in the tuple.
	 */
	public int size() {
		return 0;
	}
	
	public boolean contains(Node n) {
		return false;
	}
	
	/** Returns a human-readable representation of the tuple.
	 */
	public String toString() {
		return "()";
	}
	
	/** Singleton pattern.
	 */
	private EmptyNodeTuple() {
	}
}