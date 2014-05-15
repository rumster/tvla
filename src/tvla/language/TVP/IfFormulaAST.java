package tvla.language.TVP;

import tvla.exceptions.SemanticErrorException;
import tvla.formulae.Formula;
import tvla.formulae.IfFormula;

public class IfFormulaAST extends FormulaAST {
	FormulaAST condSubFormula;
	FormulaAST trueSubFormula;
	FormulaAST falseSubFormula;
	
	public IfFormulaAST(FormulaAST condSubFormula, FormulaAST trueSubFormula, 
						FormulaAST falseSubFormula) {
		this.type = "IfFormula";
		this.condSubFormula = condSubFormula;
		this.trueSubFormula = trueSubFormula;
		this.falseSubFormula = falseSubFormula;
	}

	public IfFormulaAST copy() {
		return new IfFormulaAST((FormulaAST) condSubFormula.copy(), 
								(FormulaAST) trueSubFormula.copy(), 
								(FormulaAST) falseSubFormula.copy());
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		condSubFormula.substitute(from, to);
		trueSubFormula.substitute(from, to);
		falseSubFormula.substitute(from, to);
	}

	public Formula getFormula() {
		try {
		return new IfFormula(condSubFormula.getFormula(), trueSubFormula.getFormula(),
							 falseSubFormula.getFormula());
		}
		catch (SemanticErrorException e) {
			e.append("while generating the if formula " + toString());
			throw e;
		}
	}
	
	public String toString() {
		return "(" + condSubFormula + " ? " + trueSubFormula + " : " + falseSubFormula + ")";
	}
}