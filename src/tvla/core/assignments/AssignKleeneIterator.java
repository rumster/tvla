package tvla.core.assignments;

import java.util.Iterator;

import tvla.logic.Kleene;

/** An iterator of assignments and their values.
 * @see tvla.core.assignments.Assign
 * @see tvla.core.assignments.AssignKleene
 * @author Tal Lev-Ami
 */
public class AssignKleeneIterator implements Iterator {
	protected boolean hasResult = false;
	protected AssignKleene result = new AssignKleene(Kleene.falseKleene);
	
	public boolean hasNext() {
		return false;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}	
	
	public Object next() {
		if (!hasResult) {
			if (!hasNext()) {
				return null;
			}
		}
		hasResult = false;
		return result;
	}		    
}