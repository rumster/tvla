package tvla.core.common;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import tvla.core.Node;
import tvla.core.NodeTuple;

public final class NodePairIterator implements Iterator {
	private final Node[] nodes;
	private int counter1;
	private int counter2;
	private Node node1;
	private Node node2;
	private final int size;

	/** Creates an iterator for enumerating all k-tuples of nodes (arity specifies k)
	 * from a specified collection of nodes. 
	 * 
	 * @param nodes A collection of nodes to choose from.
	 * @param arity Specifies how many nodes will exist in each tuple. 
	 * @return An iterator over NodeTuple objects.
	 */

	NodePairIterator(Collection nodes) {
		int size = nodes.size();
		Node[] _nodes = new Node[size];
		int i = size;
		for (Iterator nodeIter = nodes.iterator(); nodeIter.hasNext(); ) {
			_nodes[--i] = (Node) nodeIter.next();
		}
		counter1 = counter2 = size;
		node1 = _nodes[--counter1];
		this.nodes = _nodes;
		this.size = size;
	}

	public boolean hasNext() {
		return counter1 >= 0;
	}

	public Object next() {
		Node _node1 = node1;
		findNext();
		Object answer = NodeTuple.createPair(_node1, node2);
		return answer;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	private void findNext() {
		final Node[] nodes = this.nodes; 
		node2 = nodes[--counter2];
		if (counter2 == 0) {
			counter2 = size;
			if (counter1-- == 0)
				return;
			node1 = nodes[counter1];
		}
	}
}