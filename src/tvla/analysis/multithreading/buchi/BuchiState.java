package tvla.analysis.multithreading.buchi;

import tvla.predicates.Predicate;

/** Implements a state of a Buchi automaton.
 * @author Eran Yahav.
 */
public class BuchiState {
	/** name of the state */
	protected String name;
	/** predicate labeling the state */
	protected Predicate label;
	/** is the state an accepting state? */
	protected boolean accepting;
	
	/** create a new Buchi autmaton state
	 * @param stateName - the name of the state 
	 * @param isAccpting - is this state accepting?
	 * @param label - predicate labeling the state
	 */
	public BuchiState(String stateName, boolean isAccepting, Predicate label) {
		this.name = stateName;
		this.accepting = isAccepting;
		this.label = label;
	}
	
	/** is the state accepting?
	 * @return true if state is accepting, false otherwise 
	 */
	public boolean isAccepting() {
		return accepting;
	}
	/** return the name of the state
	 * @return name of state as a string 
	 * */
	public String name() {
		return name;
	}
	/** return the predicate labeling the state 
	 * @return predicate labeling this state
	 */
	public Predicate predicate() {
		return label;
	}
	/** 
	 * @return a human-readable string representing the state
	 */
	public String toString() {
		if (accepting) { 
			return "(" + name + ")";
		} else { 
			return name; 
		} 
	}
}
