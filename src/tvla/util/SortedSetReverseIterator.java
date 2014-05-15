package tvla.util;

import java.util.Iterator;
import java.util.SortedSet;

/** An iterator that traverses the elements of a sorted set from
 * end to beginning.
 * @author Roman Manevich.
 * @since December 12 2001 Initial creation.
 */
public class SortedSetReverseIterator<T> implements Iterator<T> {
	private SortedSet<T> s;
	private T lastElement;
	
	public SortedSetReverseIterator(SortedSet<T> s) {
		this.s = s;
		lastElement = s.last();
	}
	
	public boolean hasNext() {
		return !s.isEmpty();
	}
	
	public T next() {
		T result = lastElement;
		s = s.subSet(s.first(), lastElement);
		if (s.isEmpty())
			lastElement = null;
		else
			lastElement = s.last();
		return result;
	}
	
	public void remove() {
		s.remove(lastElement);
	}
}
