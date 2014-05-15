package tvla.util;

import java.util.Iterator;

public class SerialIterator implements Iterator {
	public Iterator first;
	public Iterator second;
	boolean firstCompleted;
	
	public SerialIterator() {

	}
	
	public SerialIterator(Iterator first, Iterator second) {
		this.first = first;
		this.second = second;
		this.firstCompleted = false;
	}
	
	public void set(Iterator first, Iterator second) {
		this.first = first;
		this.second = second;
		this.firstCompleted = false;
	}
	
	public boolean hasNext() {
		if (!firstCompleted) {
			if (first.hasNext())
				return true;
			firstCompleted = true;
		}
		if (second.hasNext())
			return true;
		return false;
	}
	
	public Object next() {
		if (!firstCompleted) {
			if (first.hasNext())
				return first.next();
			firstCompleted = true;
		}
		return second.next();
	}
	
	public void remove() {
		throw new java.lang.UnsupportedOperationException();
	}
}
