package tvla.language.TVP;

import tvla.formulae.EqualityFormula;
import tvla.formulae.Formula;
import tvla.formulae.Var;

/** An abstract syntax node for an equality formula.
 * @author Tal Lev-Ami.
 */
public class EqualityAST extends FormulaAST {
	Var left;
	Var right;
	
	public EqualityAST(Var left, Var right) {
		this.type = "EqualityFormula";
		this.left = left;
		this.right = right;
	}

	public EqualityAST copy() {
		// No need to copy;
		return this;
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		// Do nothing.
	}

	public Formula getFormula() {
		return new EqualityFormula(left, right);
	}
	
	public String toString() {
		return left + "==" + right;
	}
}