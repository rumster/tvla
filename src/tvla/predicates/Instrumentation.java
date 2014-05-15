package tvla.predicates;

import java.util.List;

import tvla.formulae.Formula;
import tvla.formulae.Var;

/** An instrumentation predicate. Never unique.
 * @see tvla.predicates.Predicate
 * @author Tal Lev-Ami
 */
public class Instrumentation extends Predicate {
	/** The defining formula of this instrumentation predicate.
	 */
	protected Formula formula;

	/** The variables (if predicate is non-nullary) in the order they appear as args.
	 */
	protected List<Var> vars;

	/** Constructs a new instrumentation predicate with the given name
	 * and abstraction property.
	 */
	Instrumentation(String name, int arity, boolean abstraction, Formula formula, List<Var> vars) {
		super(name, arity, abstraction);
		this.formula = formula;
		this.vars = vars;
	}

	public Formula getFormula() {
		return formula;
	}

	// Temporary (for abstraction refinement).  Should just fix the case of
	// existing predicate name in Vocabulary.createInstrumentationPredicate.
	public void setFormula(Formula formula) {
		this.formula = formula;
	}

	public List<Var> getVars() {
		return vars;
	}
}