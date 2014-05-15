package tvla.core.common;

import java.util.List;

import tvla.core.Node;
import tvla.core.NodeTuple;

/** A generic implementation for tuples with arbitrary size.
 * @author Roman Manevich
 * @since tvla-2-alpha (May 18 2002) Initial creation.
 */
public class ArbitrarySizeNodeTuple extends NodeTuple {
	protected final Node [] nodes;

	/** Creates another tuple where each occurance of source is
	 * replaced with dest.
	 */
	public NodeTuple substitute(Node source, Node dest) {
		Node [] newArray = new Node[nodes.length];
		for (int index = 0; index < nodes.length; ++index) {
			Node thisNode = nodes[index];
			if (thisNode.equals(source))
				newArray[index] = dest;
			else
				newArray[index] = thisNode;
		}
		return new ArbitrarySizeNodeTuple(newArray);
	}
	
	/** Conducts a lexicographical comparison between this tuple and the
	 * specified tuple.
	 */
	public boolean equals(Object o) {
		NodeTuple other = (NodeTuple) o;
		int otherSize = other.size();
		
		if (nodes.length != otherSize)
			return false;
		
		for (int index = 0; index < otherSize; ++index) {
			Node thisNode = nodes[index];
			Node otherNode = (Node) other.get(index);
			if (!thisNode.equals(otherNode))
				return false;
		}
		return true;
	}
	
	public int compareTo(NodeTuple other) {
		int otherSize = other.size();
		
		if (nodes.length != otherSize)
			return nodes.length - otherSize;
		
		for (int index = 0; index < otherSize; ++index) {
			Node thisNode = nodes[index];
			Node otherNode = (Node) other.get(index);
			if (!thisNode.equals(otherNode))
				return thisNode.id() - otherNode.id();
		}
		return 0;
	}
	
	/** Computes the tuple's has code.
	 */
	public int hashCode() {
		int result = 0;
		for (int index = 0; index < nodes.length; ++index)
			result = result * 31 + nodes[index].hashCode();
		return result;
	}

	/** Constructs and initializes a tuple with the specified node array.
	 */
	public ArbitrarySizeNodeTuple(Node [] nodes) {
		this.nodes = new Node[nodes.length];
		System.arraycopy(nodes, 0, this.nodes, 0, nodes.length);
	}
	
	/** Constructs and initializes a tuple with the specified node array.
	 */
	public ArbitrarySizeNodeTuple(List<Node> nodeList) {
		nodes = new Node[nodeList.size()];
		for (int index = 0; index < nodes.length; ++index)
			nodes[index] = (Node) nodeList.get(index);
	}
	
	/** Retrieves the node in the specified position.
	 */
	public Node get(int index) {
		return nodes[index];
	}
	
	/** Returns the number of nodes in the tuple.
	 */
	public int size() {
		return nodes.length;
	}
	
	/** Checks whether the specified node is contained in the tuple.
	 */
	public boolean contains(Node n) {
		for (int index = 0; index < nodes.length; ++index) {
			if (n.equals(nodes[index]))
				return true;
		}
		return false;
	}
	
	/** Returns a human-readable representation of the tuple.
	 */
	public String toString() {
		StringBuffer result = new StringBuffer(8);
		result.append("(");
		for (int index = 0; index < size(); ++index) {
			result.append(nodes[index]);
			if (index + 1 < size())
				result.append(", ");
		}
		result.append(")");
		return result.toString();
	}
}