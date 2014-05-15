package tvla.core.functional;

import java.util.AbstractCollection;
import java.util.Iterator;

import tvla.core.Node;

// class FnUniverse:

public class FnUniverse extends AbstractCollection<Node> {
	Nodelist elems;
	Nodelist freelist;
	int size;
	
	public static void reset() {
		NodeAllocator.reset();
	}
	
	public int size() { return size; }
	
	public Iterator<Node> iterator() { return new NodelistIterator(elems); }
	
	public Nodelist elements() { return elems; }
	
	public void addFirst(Node n) {
		elems = new Nodelist(n, elems);
		size++;
	}
	
	public void addFree(Node n) {
		freelist = new Nodelist(n, freelist);
	}
	
	public Node newElement() {
		Node n;
		if (freelist == null) {
			// n = Node.allocateNode();
			n = NodeAllocator.allocate(elems, size);
		} else {
			n = freelist.elem;
			freelist = freelist.next;
		}
		elems = new Nodelist(n, elems);
		size++;
		return n;
	}
	
	public void remove(Node n) {
		elems = elems.remove(n);
		size--; // assuming for now that n must be an element of universei: should we check?
		freelist = new Nodelist(n, freelist);
	}
	
	FnUniverse() {
		elems = null;
		freelist = null;
		size = 0;
	}
	
	FnUniverse(FnUniverse that ) {
		elems = that.elems;
		freelist = that.freelist; 
		size = that.size;
	}
	
	public FnUniverse copy() {
		return new FnUniverse (this);
	}
	
	public FnUniverse emptyCopy() {
		FnUniverse result = new FnUniverse ();
		result.freelist = this.freelist;
		return result;
	}
	
	public static FnUniverse create() {
		return new FnUniverse();
	}
	
	public void normalize () {
		if (elems != null) elems = elems.normalize();
	}
	
	public void computeSpace (NPSpaceCounter data) {
		if (elems != null) elems.computeSpace(data);
	}
	
	FnUniverse(int numNodes) {
		freelist = null;
		size = numNodes;
		if (numNodes > 0)
			elems = NodeAllocator.nodelist(numNodes-1);
		else
			elems = null;
		
		// for (int i = 0; i < numNodes; i++) {
		// elems = new Nodelist (NodeAllocator.node(i), elems);
		// }
	}
	
	// deallocate: this object should no longer be used; however,
	// the size() method is still valid and may be invoked (for
	// purposes of computing space statistics).
	
	public void deallocate() {
		freelist = null;
		elems = null;
	}
}

class NodeAllocator {
	private static final int max = 1000;
	private static Node nodearray[] = new Node[max];
	private static Nodelist nodelistarray[] = new Nodelist[max];
	private static int lastNodeNumber = 0;
	private static Nodelist prev = null;
	
	public static void reset() {
		nodearray = new Node[max];
		nodelistarray = new Nodelist[max];
		lastNodeNumber = 0;
		prev = null;	}
	
	public static Node node(int n) {
		if ((n < nodearray.length) && (nodearray[n] != null)) 
			return nodearray[n];
		else
			throw new RuntimeException("Node numbering invariant violation.");
	}
	
	public static Nodelist nodelist(int n) {
		if (nodearray[0] == null)
			throw new RuntimeException("Node numbering did not start from zero.");
		if ((n < nodelistarray.length) && (nodelistarray[n] != null)) 
			return nodelistarray[n];
		else
			throw new RuntimeException("Node numbering invariant violation.");
	}
	
	public static Node allocateNew() {
		Node next = Node.allocateNode();
		lastNodeNumber = next.id();
		if (lastNodeNumber >= nodearray.length) {
			Node newarray[] = new Node[2 * nodearray.length];
			System.arraycopy(nodearray, 0, newarray, 0, nodearray.length);
			nodearray = newarray;
			Nodelist newlarray[] = new Nodelist[2 * nodelistarray.length];
			System.arraycopy(nodelistarray, 0, newlarray, 0, nodelistarray.length);
			nodelistarray = newlarray;
		}
		nodearray[lastNodeNumber] = next;
		prev = new Nodelist(next, prev);
		nodelistarray[lastNodeNumber] = prev;
		return next;
	}
	
	public static Node allocate(Nodelist used, int size) {
		if (size < lastNodeNumber) {
			int checklist[] = new int[nodearray.length];
			
			for (Nodelist p = used; p != null; p  = p.next) {
				int num = p.elem.id();
				if (num < checklist.length)
					checklist[num] = num;
			}
			
			for (int i = 1; i < checklist.length; i++) {
				if (checklist[i] != i)
					return nodearray[i];
			}
		}
		
		return allocateNew();
	}
}
