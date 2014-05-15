package tvla.language.TVP;

import tvla.core.Constraints;
import tvla.exceptions.SemanticErrorException;

/** An abstract syntax node for a user-defined constraint.
 * @author Tal Lev-Ami.
 */
public class ConstraintAST extends AST {
	FormulaAST body;
	FormulaAST head;
	
	public ConstraintAST(FormulaAST body, FormulaAST head) {
		this.body = body;
		this.head = head;
	}	

	public AST copy() {
		return new ConstraintAST((FormulaAST) body.copy(), (FormulaAST) head.copy());
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		body.substitute(from, to);
		head.substitute(from, to);
	}

	public void generate() {
		try {
			Constraints.getInstance().addConstraint(body.getFormula(),
					head.getFormula());
		} catch (SemanticErrorException e) {
			e.append("while generating the constraint " + toString());
			throw e;
		}
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("%r ");
		result.append(body.toString());
		result.append(" ==> ");
		result.append(head.toString());
		return result.toString();
	}
}