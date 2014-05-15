package tvla.language.TVP;

import java.util.ArrayList;
import java.util.List;

import tvla.exceptions.SemanticErrorException;
import tvla.formulae.AndFormula;
import tvla.formulae.EquivalenceFormula;
import tvla.formulae.Formula;
import tvla.formulae.ImpliesFormula;
import tvla.formulae.NotFormula;
import tvla.formulae.OrFormula;

public class CompositeFormulaAST extends FormulaAST {
	List<FormulaAST> subFormulas = new ArrayList<FormulaAST>();

	public CompositeFormulaAST(FormulaAST subFormula) {
		// Not is the only unary composite
		type = "NotFormula";
		subFormulas.add(subFormula);
	}

	public CompositeFormulaAST(FormulaAST leftSubFormula, FormulaAST rightSubFormula, String type) {
		this.type = type;
		subFormulas.add(leftSubFormula);
		subFormulas.add(rightSubFormula);
	}

	private CompositeFormulaAST(CompositeFormulaAST other) {
		this.subFormulas = copyList(other.subFormulas);
		this.type = other.type;
	}

	public CompositeFormulaAST copy() {
		return new CompositeFormulaAST(this);
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		substituteList(subFormulas, from, to);
	}

	public Formula getFormula() {
		FormulaAST subformula0;
		FormulaAST subformula1;
		try {
			subformula0 = subFormulas.get(0);
			subformula1 = subFormulas.size() > 1 ? subFormulas.get(1) : null;
		}
		catch (SemanticErrorException e) {
			e.append("while generating the formula " + toString());
			throw e;
		}

		if (type.equals("NotFormula")) {
			return new NotFormula(subformula0.getFormula());
		} 
		else if (type.equals("OrFormula")) {
			return new OrFormula(subformula0.getFormula(), subformula1.getFormula());
		} 
		else if (type.equals("AndFormula")) {
			return new AndFormula(subformula0.getFormula(), subformula1.getFormula());
		} 
		else if (type.equals("ImpliesFormula")) {
			return new ImpliesFormula(subformula0.getFormula(), subformula1.getFormula());
		}
		else if (type.equals("EquivalenceFormula")) {
			return new EquivalenceFormula(subformula0.getFormula(), subformula1.getFormula());
		} 
		else {
			throw new SemanticErrorException("Formula type " + type + " unknown.");
		}
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		if (type.equals("NotFormula")) {
			result.append("!(" + subFormulas.get(0).toString() + ")");
		} else if (type.equals("OrFormula")) {
			result.append("(");
			result.append(subFormulas.get(0).toString());
			result.append(" | ");
			result.append(subFormulas.get(1).toString());
			result.append(")");
		} else if (type.equals("AndFormula")) {
			result.append("(");
			result.append(subFormulas.get(0).toString());
			result.append(" & ");
			result.append(subFormulas.get(1).toString());
			result.append(")");
		} else if (type.equals("EquivalenceFormula")) {
			result.append("(");
			result.append(subFormulas.get(0).toString());
			result.append(" <-> ");
			result.append(subFormulas.get(1).toString());
			result.append(")");
		} else if (type.equals("ImpliesFormula")) {
			result.append("(");
			result.append(subFormulas.get(0).toString());
			result.append(" -> ");
			result.append(subFormulas.get(1).toString());
			result.append(")");
		} else {
			throw new SemanticErrorException(
				"Formula type (" + type + " unknown.");
		}
		return result.toString();
	}
}