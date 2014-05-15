package tvla.core.assignments;

import java.util.Collection;

import tvla.logic.Kleene;

/** An assignment with a kleene value.
 * @see tvla.core.assignments.Assign
 * @see tvla.logic.Kleene
 * @author Tal Lev-Ami
 */
public class AssignKleene extends Assign {
    private final static AssignKleene EMPTY = new AssignKleene(Kleene.trueKleene);
    private static boolean acquired = false;

	public Kleene kleene;
	
	public static AssignKleene acquireInstance(Kleene value) {
		if (!acquired) {
			EMPTY.kleene = value;
			acquired = true;
			return EMPTY;
		}
		else {
			throw new Error("Tried to acquire shared AssignKleene instance");
		}
	}
	
	public static final void releaseInstance() {
		acquired = false;
	}

	/** Creates an AssignKleene with given assignment and given kleene value.
	 */
	public AssignKleene(Assign assign, Kleene kleene) {
		super(assign);
		this.kleene = kleene;
	}
	
	/** Creates an AssignKleene with empty assignment and given kleene value.
	 */
	public AssignKleene(Kleene kleene) {
		super();
		this.kleene = kleene;
	}
	
	public AssignKleene instanceForIterator(Collection freeVars, boolean isFull) {
		// IMPORTANT: freeVars are not added here. We rely on the iterators to
		// do this. This is done for the sake of performance, but breaks the 
		// convention, since it differs from the AssignPrecomputed behavior.
		if (isFull)
			return this;
		else
			return new AssignKleene(this, Kleene.falseKleene);
	}

}