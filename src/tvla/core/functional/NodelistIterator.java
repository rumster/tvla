package tvla.core.functional;

import java.util.Iterator;

import tvla.core.Node;

public class NodelistIterator implements Iterator<Node> {
	Nodelist head;

	public NodelistIterator (Nodelist l) { head = l; }

	public boolean hasNext() { return head != null; }

	public Node next() {
        Node result = head.elem;
		head = head.next;
		return result;
	}
	
	public void remove() {
	   throw new UnsupportedOperationException("NodelistIterator::remove");
	}
}