/*
 * Created on Dec 17, 2003
 *
 * @author Alexey Loginov
 */
package tvla.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author alexey
 * Extension to type LinkedList that keeps elements unique.
 * All element adding methods have been overriden in a trivial way.
 */
public class NoDuplicateLinkedList<T> extends LinkedList<T> {

    public NoDuplicateLinkedList() {
        super();
    }

    public NoDuplicateLinkedList(Collection<T> c) {
        super(c);
    }

	public boolean add(T o) {
		return !contains(o) && super.add(o);
	}

	public void add(int i, T o) {
		if (!contains(o))
			super.add(i, o);
	}

	public boolean addAll(Collection<? extends T> c) {
		boolean result = false;
		for (Iterator<? extends T> iter = c.iterator(); iter.hasNext();) {
			T o = iter.next();
			result |= add(o);
		}
		return result;
	}

	public boolean addAll(int i, Collection<? extends T> c) {
		int numInserted = 0;
		for (Iterator<? extends T> iter = c.iterator(); iter.hasNext();) {
			T o = iter.next();
			if (!contains(o)) {
				add(i+numInserted, o);
				numInserted++;
			}
		}
		return numInserted > 0;
	}

	public void addFirst(T o) {
		add(0, o);
	}

	public void addLast(T o) {
		add(o);
	}
}
