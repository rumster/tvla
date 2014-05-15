package tvla.language.TVM;

import tvla.exceptions.SemanticErrorException;
import tvla.formulae.Formula;
import tvla.language.TVP.AST;
import tvla.language.TVP.FormulaAST;
import tvla.language.TVP.PredicateAST;

public class OutputModifierAST extends AST {

	private boolean positive;
	private FormulaAST modifierFormula;
	
	public OutputModifierAST(FormulaAST aFormula, boolean inclusive)
	{
		positive = inclusive;
		modifierFormula = aFormula;
	}
	
	public boolean inclusive() {
		return positive;
	}
	
	public Formula getFormula() {
		try {
			return modifierFormula.getFormula();
		}
		catch (SemanticErrorException e) {
			e.append("while generating the output modifier " + toString());
			throw e;
		}
	}
	
	public void generate() {
	}
	
	public void compile() {
	}
	
	public AST copy() {
		throw new RuntimeException("Can't copy output modifier.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute output modifier.");
	}
}
