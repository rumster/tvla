package tvla.language.TVP;

import tvla.formulae.Formula;
import tvla.formulae.ValueFormula;
import tvla.logic.Kleene;

public class ValueFormulaAST extends FormulaAST {
	Kleene value;

	public ValueFormulaAST(Kleene value) {
		this.type = "ValueFormula";
		this.value = value;
	}

	public ValueFormulaAST copy() {
		// No need to copy;
		return this;
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		// Do nothing.
	}

	public Formula getFormula() {
		return new ValueFormula(value);
	}

	/** Return a human readable representation of the formula. */
	public String toString() {
		return value.toString();
	}
}