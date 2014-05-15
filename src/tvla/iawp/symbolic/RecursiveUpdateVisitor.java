package tvla.iawp.symbolic;

import java.util.Map;

import tvla.formulae.Formula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.PredicateUpdateFormula;
import tvla.formulae.Var;

public class RecursiveUpdateVisitor extends CopyVisitor
{
	
	protected Map predicateUpdateFormulae;


	public RecursiveUpdateVisitor(Map updateFormulae) {
		predicateUpdateFormulae = updateFormulae;
	}
	
	/**
	 * replace the predicate formula by its predicate-update formula
	 * NOTE: should make sure that you only replace predicate-formula of the original formula
	 * this is done by visiting the original formula, which is not modified
	 * Other things to note:
	 * 1. substitute actual variables to formal variables
	 * 2. rename bound variables in the update formula to avoid clashes with existing quantifiers
	 * in the enclosing formula.
	 */
	public Formula accept(PredicateFormula f) {
		Formula result = null;
		if (predicateUpdateFormulae.containsKey(f.predicate())) {
			PredicateUpdateFormula update = (PredicateUpdateFormula)predicateUpdateFormulae.get(f.predicate());
			result = update.getFormula().copy();
			// refresh bounded variables of update formula to avoid clashes
//			System.out.println("Update before:"+result);
			result.visit(new RefreshBoundedVarVisitor());
//			System.out.println("Update after:"+result);
			// adjust free variables of update formula
			Var[] actualVars = f.variables();
			Var[] formalVars = update.variables;
			for(int i=0,n=actualVars.length; i < n; i++) {
				result.substituteVar(formalVars[i],actualVars[i]);
			}
				
		} else {
			result = f.copy();
		}
		return result;
	}
}
