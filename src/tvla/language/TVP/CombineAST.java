package tvla.language.TVP;

import tvla.exceptions.SemanticErrorException;
import tvla.formulae.Formula;
import tvla.formulae.ValueFormula;
import tvla.logic.Kleene;

/** An abstract syntax node for a combination of formulae.
 * @author Tal Lev-Ami.
 */
public class CombineAST extends FormulaAST {
	String id;
	SetAST set;
	FormulaAST formula;
	String operator;

	public CombineAST(String operator, FormulaAST formula, String id, SetAST set) {
		this.id = id;
		this.set = set;
		this.operator = operator;
		this.formula = formula;
	}

	public Formula getFormula() {
		FormulaAST result = null;
		for (PredicateAST member : set.getMembers()) {
			FormulaAST term = (FormulaAST) formula.copy();
			term.substitute(id, member);
			if (result == null)
				result = term;
			else
				result = new CompositeFormulaAST(result, term, operator);
		}
		if (result == null) {
			return operator.equals("OrFormula") ? 
				   new ValueFormula(Kleene.falseKleene) : 
				   new ValueFormula(Kleene.trueKleene);
		}
		Formula resultFormula;
		try {
			resultFormula = result.getFormula();
		}
		catch (SemanticErrorException e) {
			e.append("while generating the combine formula " + toString());
			throw e;
		}
		return resultFormula;
	}

	public CombineAST copy() {
		return new CombineAST(operator, (FormulaAST) formula.copy(), id, (SetAST) set.copy());
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		if (from.isSimple() && from.name.equals(id))
			throw new RuntimeException("Trying to substitute the variable of a foreach (" +
				id + ")");
		set.substitute(from, to);
		formula.substitute(from, to);
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		String binaryOp = operator.equals("OrFormula") ? "|" : "&";
		result.append(binaryOp);
		result.append("/{");
		result.append(formula.toString());
		result.append(" : ");
		result.append(id);
		result.append(" in ");
		result.append(set.toString());
		result.append("}");
		return result.toString();
	}
}