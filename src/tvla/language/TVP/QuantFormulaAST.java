package tvla.language.TVP;

import java.util.Iterator;
import java.util.List;

import tvla.exceptions.SemanticErrorException;
import tvla.formulae.AllQuantFormula;
import tvla.formulae.ExistQuantFormula;
import tvla.formulae.Formula;
import tvla.formulae.Var;

public class QuantFormulaAST extends FormulaAST {
	List<Var> bound;
	FormulaAST subFormula;

	public QuantFormulaAST(List<Var> bound, FormulaAST subFormula, String type) {
		this.type = type;
		this.bound = bound;
		this.subFormula = subFormula;
	}

	public QuantFormulaAST copy() {
		return new QuantFormulaAST(bound, (FormulaAST) subFormula.copy(), type);
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		subFormula.substitute(from, to);
	}

	private static Formula buildAllQuant(Iterator<Var> iterator, FormulaAST subFormula) {
		if (iterator.hasNext()) {
			Var var = (Var) iterator.next();
			return new AllQuantFormula(var, buildAllQuant(iterator, subFormula));
		} else {
			try {
				Formula subFOrmula = subFormula.getFormula();
				return subFOrmula;
			}
			catch (SemanticErrorException e) {
				e.append("while generating the formula " + subFormula.toString());
				throw e;
			}
		}
	}

	private static Formula buildExistQuant(Iterator<Var> iterator, FormulaAST subFormula) {
		if (iterator.hasNext()) {
			Var var = (Var) iterator.next();
			return new ExistQuantFormula(var, buildExistQuant(iterator, subFormula));
		} else {
			try {
				Formula subFOrmula = subFormula.getFormula();
				return subFOrmula;
			}
			catch (SemanticErrorException e) {
				e.append("while generating the formula " + subFormula.toString());
				throw e;
			}
		}
	}

	public Formula getFormula() {
		if (type.equals("AllQuantFormula")) {
			return buildAllQuant(bound.iterator(), subFormula);
		} else if (type.equals("ExistQuantFormula")) {
			return buildExistQuant(bound.iterator(), subFormula);
		} else {
			throw new SemanticErrorException("Formula type (" + type + " unknown.");
		}
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		String op = (type.equals("AllQuantFormula")) ? "A" : "E";
		String separator = ""; 
		result.append(op);
		result.append("(");
		for(Iterator<Var> it=bound.iterator();it.hasNext();) {
			result.append(separator);
			result.append(it.next().toString());
			separator = ",";
		}
		result.append(") ");
		result.append(subFormula.toString());
		
		return result.toString();
	}
}
