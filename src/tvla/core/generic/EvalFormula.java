package tvla.core.generic;

import java.util.Iterator;
import java.util.Set;

import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignIterator;
import tvla.core.assignments.AssignKleeneIterator;
import tvla.formulae.Formula;
import tvla.logic.Kleene;
import tvla.util.HashSetFactory;

/** A formula evaluation algorithm.
 * @author Roman Manevich
 */
public final class EvalFormula extends AssignKleeneIterator {
	private static EvalFormula instance;
	private final Assign partial;
	private final Formula formula;
	private final TVS structure;
	private Iterator assignIterator = null;
	
	public static void reset() {
		instance = null;
	}
	
	/** @return An iterator to a set of assignments that satisfy the formula.
	 * @author Tal Lev-Ami
	 * @since 6/9/2001 Moved here from NaiveStructure.
	 */
	static public Iterator evalFormula(TVS structure, 
									   Formula formula, 
									   Assign partialAssignment) {
		instance = new EvalFormula(structure,
								   formula,
								   partialAssignment);
		return instance;
	}
	
	/** Returns true if there's another assignment.
	 * @author Tal Lev-Ami
	 * @since 6/9/2001 Moved here from NaiveStructure (Roman).
	 */
	public boolean hasNext() {
		if (hasResult) {
			return true;
		}

		if (assignIterator == null) {
			result.put(partial);
			formula.prepare(structure);
			Set stillFree = HashSetFactory.make(formula.freeVars());
			stillFree.removeAll(partial.bound());
			if (stillFree.isEmpty()) {
				assignIterator = new AssignIterator();
				result.kleene = formula.eval(structure, result);
				hasResult = result.kleene != Kleene.falseKleene;
				return hasResult;
			}
			else {
				assignIterator = Assign.getAllAssign(structure.nodes(), stillFree);
			}
		}

		while (assignIterator.hasNext()) {
			result.put((Assign) assignIterator.next());
			result.kleene = formula.eval(structure, result); 
			if (result.kleene != Kleene.falseKleene) {
                hasResult = true;
				return true;
			}
		}
		result = null;
		return false;
	}
	
	/** Singleton pattern.
	 */
	private EvalFormula(TVS structure,
						Formula formula, 
						Assign partialAssignment) {
      
		this.structure = structure;
		this.formula = formula;
		this.partial = partialAssignment;
	}
}
