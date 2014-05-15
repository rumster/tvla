package tvla.formulae;

import java.util.List;

import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.logic.Kleene;

/** A formula that is used to update a structure.
 * @author Roman Manevich.
 * @since 4.9.2001 Initial creation.
 */
public abstract class UpdateFormula {
	/** The right-hand side of the update formula.
	 */
	protected Formula formula;
	protected List<Var> freeVars;
	
	public UpdateFormula(Formula updateFormula) {
		// TODO: FIX BACK?
		this.formula = updateFormula.copy();
		this.formula = this.formula.optimizeForEvaluation();
		//this.formula = updateFormula;
		//this.formula = updateFormula.optimizeForEvaluation();
	}
	
	public Formula getFormula() {
		return formula;
	}

	public List<Var> freeVars() {
		// Copy list here because it may be changed in a call to
		// addAdditionalFreeVars.  Without the copy, this change
		// messes up formula's freeVars without recomputing them.
		//return new NoDuplicateLinkedList(formula.freeVars());
		return formula.freeVars();
	}
    
	public void prepare(TVS s) {
		formula.prepare(s);
	}
	
    public String toString() {
      return formula.toString();
    }
    
	public final FormulaIterator assignments(TVS structure, Assign partial) {
		return formula.assignments(structure, partial, null);
	}

	public FormulaIterator assignments(TVS structure, Assign partial, Kleene value) {
		return formula.assignments(structure, partial, value);
	}

}