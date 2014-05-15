package tvla.analysis.multithreading.buchi;

import tvla.differencing.Differencing;
import tvla.formulae.AndFormula;
import tvla.formulae.Formula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.ValueFormula;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.transitionSystem.Action;

/** Implements a transition of a Buchi automaton
 * the transition automatically generates an action corresponding to the
 * semantics of the transition.
 * @author Eran Yahav.
 */
public class BuchiTransition {
	/** source of the transition */
	protected BuchiState source;
	/** transition target */
	protected BuchiState target;
	/** prediacte labeling the transition */
	protected Predicate label;
	/** transition action */
	protected Action action;
	
	/** construct a new transition from a source, target, and label 
	 * @param src - source of the transition (buchi state)
	 * @param tgt - target of the transition (buchi state)
	 * @param edgeLabel - predicate labeling the transition edge.
	 */
	public BuchiTransition(BuchiState src, BuchiState tgt, Predicate edgeLabel) {
		source = src;
		target = tgt;
		label = edgeLabel;
		generateAction();
	}
	
	/** generate an action for this transition */
	protected void generateAction() {
		
		action = new Action();
		
		// create a focus formula
		//// no focus is created since currently we only
		//// handle nullary predicates in the precondition
		//// and these can not have indefinite values
		//// FUTURE: in the future we will have to add a focus formula here
		// create a precondition formula
		Formula precond;
		precond = new AndFormula(
			new PredicateFormula(source.predicate()),
			new PredicateFormula(label));	
		
		action.internalPrecondition(precond);
		
		// create update formulae 
		Formula disableSource = new ValueFormula(Kleene.falseKleene);
		Formula enableTarget = new ValueFormula(Kleene.trueKleene);
		action.setPredicateUpdateFormula(source.predicate(), disableSource);
		action.setPredicateUpdateFormula(target.predicate(), enableTarget);
															
		// set the title of the action
		action.setTitle("Buchi " + source.name() + " ->(" + label.toString() + ") " + target.name());
		Differencing.registerAction(action);
	}
	
	/** @return the action for the transition */
	public Action action() {
		return action;
	}
	
	/** return a string representation of the buchi transition
	 * @return buchi transition as string */
	public String toString() {
		return source.toString() + "-(" + label.toString() + ")->" + target.toString();
	}
}
