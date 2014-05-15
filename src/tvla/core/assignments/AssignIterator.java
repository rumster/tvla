package tvla.core.assignments;

import java.util.Iterator;

/** An iterator of assignments.
 * @see tvla.core.assignments.Assign
 * @author Tal Lev-Ami
 */
public class AssignIterator implements Iterator<Assign> {
	protected boolean hasResult = false;
	protected Assign result;
	
	public AssignIterator() {
		super();
		result = new Assign();
	}
	
	public AssignIterator(Assign partial) {
		super();
		if (partial != null)
			result = new Assign(partial);
		else
			result = new Assign();
	}
	
	public boolean hasNext() {
		return false;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}	
	
	public Assign next() {
		if (!hasNext()) {
			return null;
		}
		hasResult = false;
		return result;
	}		    
}
