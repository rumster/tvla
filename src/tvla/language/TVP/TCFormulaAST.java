package tvla.language.TVP;

import tvla.exceptions.SemanticErrorException;
import tvla.formulae.Formula;
import tvla.formulae.TransitiveFormula;
import tvla.formulae.Var;

public class TCFormulaAST extends FormulaAST {
	Var leftSub;
	Var rightSub;
	Var left;
	Var right;
	FormulaAST subFormula;

	public TCFormulaAST(
		Var left,
		Var right,
		Var leftSub,
		Var rightSub,
		FormulaAST subFormula) {
		this.type = "TransitiveFormula";
		this.leftSub = leftSub;
		this.rightSub = rightSub;
		this.left = left;
		this.right = right;
		this.subFormula = subFormula;
	}

	public TCFormulaAST copy() {
		return new TCFormulaAST(
			left,
			right,
			leftSub,
			rightSub,
			(FormulaAST) subFormula.copy());
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		subFormula.substitute(from, to);
	}

	public Formula getFormula() {
		try {
			TransitiveFormula transitiveFormula = new TransitiveFormula(left,
					right, leftSub, rightSub, subFormula.getFormula());
			return transitiveFormula;
		} catch (SemanticErrorException e) {
			e.append("while generating TC formula " + toString());
			throw e;
		}
	}
	
	// TC LP var:l COMMA var:r RP LP var:sl COMMA var:sr RP formula:f
	//				{: RESULT = new TCFormulaAST(l, r, sl, sr, f); :}
	public String toString() {
		StringBuffer result = new StringBuffer();

		result.append("TC (");
		result.append(left.toString());
		result.append(",");
		result.append(right.toString());
		result.append(") (");
		result.append(leftSub);
		result.append(",");
		result.append(rightSub);
		result.append(") ");
		result.append(subFormula);
		return result.toString();
	}
}