package tvla.absRef;

import java.util.Set;

import tvla.formulae.Formula;
import tvla.formulae.FormulaVisitor;
import tvla.formulae.PredicateFormula;
import tvla.predicates.Instrumentation;
import tvla.predicates.Predicate;
import tvla.util.HashSetFactory;

/** For collecting the set of core predicates that
 *  appear in the formula's core normal form.
 * @author Alexey Loginov.
 * @since June 18 2003, Initial creation.
 */
public class GetFormulaCorePreds extends FormulaVisitor {
	/** Stores intermediate results during traversal of the formula.
	 */
	private static Set predicates;
	
	/** Used to invoke traverse and accept.
	 */
	private static GetFormulaCorePreds instance = new GetFormulaCorePreds();

	/** Returns the set of predicates used by the specified formula.
	 * @param formula A first-order formula.
	 * @return A set of predicates used by the formula.
	 */
	public static Set get(Formula formula) {
		predicates = HashSetFactory.make();
		instance.traverse(formula);
		Set result = predicates;
		predicates = null; // Opportunity for compile-time GC.
		return result;
	}	

	/** Stores the formula's predicate in the result set.
	 */
	public Object accept(PredicateFormula predFormula) {
	    Predicate pred = predFormula.predicate();

	    if (pred instanceof Instrumentation) {
		/* Add in all core preds in the core normal form
		   of the defining formula. */
		Instrumentation instrumPred = (Instrumentation) pred;
		instance.traverse(instrumPred.getFormula());
	    }
	    else {
		predicates.add(pred);
	    }
	    return null;
	}
}
