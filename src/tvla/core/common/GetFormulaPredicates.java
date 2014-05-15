package tvla.core.common;

import java.util.Set;

import tvla.formulae.Formula;
import tvla.formulae.FormulaVisitor;
import tvla.formulae.PredicateFormula;
import tvla.predicates.Predicate;
import tvla.util.HashSetFactory;

/** An algorithm that collects the predicates that are used
 * by a formula in a set.
 * @author Roman Manevich.
 * @since tvla-2-alpha November 18 2002, Initial creation.
 */
public class GetFormulaPredicates extends FormulaVisitor<Object> {
	/** Stores intermediate results during traversal of the formula.
	 */
	private static Set<Predicate> predicates;
	
	/** Used to invoke traverse and accept.
	 */
	private static GetFormulaPredicates instance = new GetFormulaPredicates();

	/** Returns the set of predicates used by the specified formula.
	 * @param formula A first-order formula.
	 * @return A set of predicates used by the formula.
	 */
	public static Set<Predicate> get(Formula formula) {
		predicates = HashSetFactory.make();
		instance.traverse(formula);
		Set<Predicate> result = predicates;
		predicates = null; // Opportunity for compile-time GC.
		return result;
	}	

	/** Stores the formula's predicate in the result set.
	 */
	public Object accept(PredicateFormula formula) {
		predicates.add(formula.predicate());
		return null;
	}
}
