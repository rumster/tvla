package tvla.transitionSystem;

import tvla.formulae.Formula;

/** A global action is an action which is location independent
 * @author Eran Yahav.
 */
public class GlobalAction extends Action {
	public void internalPrecondition(Formula formula) {
		throw new RuntimeException("Can not set the internal precondition for a global action");
	}
}