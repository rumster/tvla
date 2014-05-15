package tvla.formulae;

import java.util.List;

import tvla.util.NoDuplicateLinkedList;

/** An abstract base class for all atomic formulae. 
 * @author Tal Lev-Ami 
 */
public abstract class AtomicFormula extends Formula {
	public AtomicFormula() {
		super();
	}

	/** Calculates and returns variables bound in this formula or subformulae.
	 */
	public List<Var> calcBoundVars() {
		return new NoDuplicateLinkedList<Var>();
	}
	
	public void traversePostorder(FormulaTraverser t) {
		t.visit(this);
	}

	public void traversePreorder(FormulaTraverser t) {
		t.visit(this);
	}

}