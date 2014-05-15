package tvla.util;

import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;


/** A set that can contain exactly one element.
 * @author maon
 */
public class SingleSet extends AbstractSet {
	protected Object elm;
	boolean bounded; 
	
	public SingleSet(boolean bounded) {
		super();
		this.elm = null;
		this.bounded = bounded;
	}
	
	public SingleSet(boolean bounded, Object obj) {
		super();
		this.elm = obj;
		this.bounded = bounded;
	}
	
    public boolean add(Object o) {
    	if (this.elm == o)
    		return false;
    	
    	if (this.elm != null && bounded)
    		throw new InternalError("Attempt to add more than one object into a bounded SingleContainer");
    	
    	elm = o;
    	return true;
    }
	
    public Object get() {
    	return elm;
    }
    
    public void clear() {
    	elm = null;
    }

    public Object extract() {
    	Object tmp = elm;
		elm = null;
    	return tmp;
    }
  
    
    public boolean isBounded() {
    	return bounded;
    }
    
	public int size() {
		return elm == null ? 0 : 1;
	}
	
	public java.util.Iterator iterator() {
		return new SingleCollectionIterator();
	}
	   
	public class SingleCollectionIterator implements Iterator{
		Object pointsTo;
		public SingleCollectionIterator() {
			pointsTo = null;
		}
		
		public boolean hasNext() {
			return (pointsTo == null && SingleSet.this != null);
		}

		public void remove() {
			if (pointsTo == null) 
				throw new IllegalStateException();
			if (pointsTo != SingleSet.this.elm)
				throw new ConcurrentModificationException();
			
			SingleSet.this.elm = null;
		}	
	
		public Object next() {
			pointsTo = SingleSet.this.elm;
			return pointsTo;
		}
	}		    
}