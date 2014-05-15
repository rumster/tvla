package tvla.core.functional;

import java.util.Iterator;

import tvla.core.Node;

public class Nodelist extends Countable {
	public Node elem;
	public Nodelist next;

	public Nodelist(Nodelist l) {
		elem = l.elem;
		next = l.next;
	}

	public Nodelist(Node car, Nodelist cdr) {
		elem = car;
		next = cdr;
	}

	public Nodelist remove (Node n) {
		if (elem == n)
			return next;
		else if (next != null)
			return new Nodelist (elem, next.remove(n)) ;
		else
			return this;
	}

	public Iterator iterator() {
		return new NodelistIterator(this);
	}

	public void computeSpace (NPSpaceCounter data) {
		for (Nodelist p = this; p != null; p = p.next) {
			if (data.visited(p)) return;
			data.markVisited(p);
			data.numNodesWithSharing ++;
		}
	}

	public Nodelist normalize() {
		if (next != null) next = next.normalize();
		return UniqueNodelist.instance(this);
	}

	public boolean equals (Object other) {
		if (other instanceof Nodelist) {
			Nodelist that = (Nodelist) other;
			return (that.next == next) && (that.elem == elem);
		} else 
			return false;
	}

	public int hashCode() {
		int hash = 0;
		if (next != null) hash ^= next.objectHashCode();
		hash ^= elem.id();
		return hash;
	}

	// objectHashCode: original hash-code, defined by class Object.
	public int objectHashCode() { return super.hashCode(); }

}

class UniqueNodelist extends Nodelist {
	// private static int nextId = 1;
	// private int id = 0;

	private UniqueNodelist(Nodelist l) {
		super(l);
	}

	public static Nodelist instance (Nodelist l) {
		UniqueNodelist temp = new UniqueNodelist(l); 
		return (Nodelist) HashCons.instance(temp);
	}

	public Nodelist normalize() {
		return this;
	}
}
