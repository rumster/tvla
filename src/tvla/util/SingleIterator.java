package tvla.util;

import java.util.Iterator;

/** An iterator that returns exactly one element.
 * @author Tal Lev-Ami
 */
@SuppressWarnings("unchecked")
public class SingleIterator<T> implements Iterator<T> {
	protected T result;
	
	public SingleIterator(T result) {
		this.result = result;
	}
	
	public boolean hasNext() {
		return result != null;
	}

	public void remove() {
	}	
	
	public T next() {
		T tmp = result;
		result = null;
		return tmp;
	}
	
	public void setValue(T o) {
		result = o;
	}
	
	static public <T> SingleIterator<T> instance(T o) {
		instance.setValue(o);
		return instance;
	}
	
	static public SingleIterator instance = new SingleIterator(null); 
}