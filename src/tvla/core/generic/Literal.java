package tvla.core.generic;

import tvla.formulae.AtomicFormula;
import tvla.formulae.Formula;
import tvla.formulae.NotFormula;

/** A class that represents a literal in a formula. 
 * @author Tal Lev-Ami
 */
public class Literal {
	public AtomicFormula atomic;
	public boolean negated;
	public boolean complex = false;
	
	/** Constructs a literal from an atomic formula.
	 * @param formula An atomic formula or a negation of an atomic formula.
	 */
	public Literal(Formula formula) {
		Formula origFormula = formula;
		if (formula instanceof NotFormula) {
			NotFormula nformula = (NotFormula) formula;
			formula = nformula.subFormula();
			negated = true;
		} 
		else {
			negated = false;
		}

		if (formula instanceof AtomicFormula)
			atomic = (AtomicFormula) formula;
		else
			throw new RuntimeException("Atomic formula or negated atomic formula expected "
				+ "but got " + origFormula);
	}

	public Literal(AtomicFormula atomic, boolean negated) {
		this.atomic = atomic;
		this.negated = negated;
	}

	public boolean equals(Object o) {
		if (!(o instanceof Literal))
			return false;
		Literal other = (Literal) o;
		if (this.negated != other.negated)
			return false;
		return this.atomic.equals(other.atomic);
	}

	public int hashCode() {
		return atomic.hashCode() * 2 + (negated ? 0 : 1);
	}

	public String toString() {
		return (negated ? "!" : "") + atomic.toString();
	}
}