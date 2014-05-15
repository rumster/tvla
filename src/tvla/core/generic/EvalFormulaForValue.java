package tvla.core.generic;

import java.util.Iterator;
import java.util.Set;

import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignKleene;
import tvla.core.assignments.AssignIterator;
import tvla.core.assignments.AssignKleeneIterator;
import tvla.formulae.Formula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.EqualityFormula;
import tvla.logic.Kleene;
import tvla.util.HashSetFactory;

/**
 * A formula evaluation algoritm used to track a specific Kleene value.
 * 
 * @author Roman Manevich
 * @since 6/9/2001 Initial creation.
 */
public class EvalFormulaForValue extends AssignKleeneIterator {

  private static final boolean USE_SPECIALIZED_EVAL = true; 

  protected final Assign partial;

  protected final Formula formula;

  protected final TVS structure;

  protected final Kleene desiredValue;

  protected Iterator assignIterator = null;

  /**
   * @return An iterator to a set of assignments that satisfy the formula.
   * @author Tal Lev-Ami
   * @since 6/9/2001 Moved here from NaiveStructure.
   */
  static public Iterator evalFormulaForValue(TVS structure, Formula formula, Assign partialAssignment, Kleene desiredValue) {
	// We will allow for desiredValue to be null, in which case both 1 and 1/2 are accepted.
    if (USE_SPECIALIZED_EVAL && (formula instanceof PredicateFormula) && desiredValue != Kleene.falseKleene) {
      // evaluation is only efficient for potentially satisfying assignments
      //System.out.println("Using special eval");
      return new EvalPredicateFormulaForValue(structure, formula, partialAssignment, desiredValue);
    } else 
	if (USE_SPECIALIZED_EVAL && (formula instanceof EqualityFormula) && desiredValue != Kleene.falseKleene) {
      return new EvalEqualityFormulaForValue(structure, formula, partialAssignment, desiredValue);
    } else {
      return new EvalFormulaForValue(structure, formula, partialAssignment, desiredValue);
    }
  }

  /**
   * @author Tal Lev-Ami
   */
  public boolean hasNext() {
    if (hasResult) {
      return true;
    }

    if (assignIterator == null) {
      result.put(partial);
      // TODO: prepare is an expensive way to erase TC cache.
      formula.prepare(structure);

      if (partial.containsAll(formula.freeVars())) {
        assignIterator = new AssignIterator();
        result.kleene = formula.eval(structure, result);
        hasResult = checkDesiredValue(result.kleene);
        return hasResult;
      } else {
        Set stillFree = HashSetFactory.make(formula.freeVars());
        stillFree.removeAll(partial.bound());
        assignIterator = Assign.getAllAssign(structure.nodes(), stillFree);
      }
    }

    while (assignIterator.hasNext()) {
      result.put((Assign) assignIterator.next());
      result.kleene = formula.eval(structure, result);
      if (checkDesiredValue(result.kleene)) {
        hasResult = true;
        return true;
      }
    }
    result = null;
    return false;
  }
  
  final protected boolean checkDesiredValue(Kleene k) {
    if (desiredValue == null) {
    	return k != Kleene.falseKleene;
    } else {
      	return k == desiredValue;
    }
  }

  public EvalFormulaForValue(TVS structure, Formula formula, Assign partialAssignment, Kleene desiredValue) {
    this.structure = structure;
    this.formula = formula;
    this.partial = partialAssignment;
    this.desiredValue = desiredValue;
  }
}

