package tvla.formulae;

import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.core.TVS;
import tvla.exceptions.SemanticErrorException;
import tvla.predicates.Predicate;
import tvla.util.HashMapFactory;
import tvla.util.NoDuplicateLinkedList;

/** A abstract class representing a quanitified sub formula.
 * @author Tal Lev-Ami 
 */
public abstract class QuantFormula extends Formula {
	protected Var boundVariable;
	protected Formula subFormula;

	/** Create a new quantified formula. */
	public QuantFormula(Var boundVariable, Formula subFormula) {
		super();
		this.boundVariable = boundVariable;
		this.subFormula = subFormula;
	}

	/** Substitute the given variable name to a new name. Ignores the substitution if the
	 * old name is the same as the bound variable name.
	 */
	public void substituteVar(Var from, Var to) {
		if (boundVariable.equals(from))
			return;
		if (boundVariable.equals(to)) {
			throw new SemanticErrorException("Error. Substitution of " + from + " to " +
				to + " in subformula " + this + 
				" violates binding.");	    
		}
		subFormula.substituteVar(from, to);
		freeVars = null;
	}

	/** 
	 * Substitute variables in parallel according to the sub map.
	 * Ignore the substitution of a name if the
	 * old name is the same as the bound variable name.
	 */
	public void substituteVars(Map<Var, Var> sub) {
		if (sub.containsKey(boundVariable)) {
			if (sub.size() == 1) return;

			// Do not alter the original map because it is used in the caller!
			sub = HashMapFactory.make(sub);
			sub.remove(boundVariable);
		}

		if (sub.containsValue(boundVariable)) {
			throw new SemanticErrorException("Error. Substitution " +
			sub + " in subformula "  + this + " violates binding.");	    
		}
		subFormula.substituteVars(sub);
		freeVars = null;
	}

	/** Rename the bound variable so is wil be possible to move the quantification. */
	public void normalize() {
		Var newVar = Var.allocateVar();
		subFormula.substituteVar(boundVariable, newVar);
		boundVariable = newVar;
		boundVars = null;
	}

	/** Return the variable bound be this quantification. */
	public Var boundVariable() {
		return boundVariable;
	}

	/** Return the quantified sub formula. */
	public Formula subFormula() {
		return subFormula;
	}

	/** Prepare this formula for the new structure */
	public boolean askPrepare(TVS s) {
		return subFormula.askPrepare(s);
	}
	
	public void traversePostorder(FormulaTraverser t) {
		subFormula.traverse(t);
		t.visit(this);
	}

	public void traversePreorder(FormulaTraverser t) {
		t.visit(this);
		subFormula.traverse(t);
	}


	/** Calculate and return the free variables for this formula. */
	public List<Var> calcFreeVars() {
		List<Var> result = new NoDuplicateLinkedList<Var>(subFormula.freeVars());
		result.remove(boundVariable);
		return result;
	}

	/** Calculate and return variables bound in this formula or subformulae. */
	public List<Var> calcBoundVars() {
		List<Var> result = new NoDuplicateLinkedList<Var>(subFormula.boundVars());
		// Make sure boundVariable goes at the start.
		result.remove(boundVariable);
		result.add(0, boundVariable);
		return result;
	}

	/** Equate the this formula with the given fomula by structure. */
	public boolean equals(Object o) {
		if (!(o instanceof QuantFormula))
			return false;
		QuantFormula other = (QuantFormula) o;
		if (this.boundVariable.equals(other.boundVariable))
		    // If the bound var matches then alpha renaming has no effect.
			return this.subFormula.equals(other.subFormula);

		// Bound var didn't match, try renaming it.
		if (alphaRenamingEquals) {
		    // See if formulae are equal up to alpha renaming.
		    // Make a copy of our subformula and replace our bound var
		    // with that of other.  See if subformulae are then equal.
		    Formula thisSubCopy = this.subFormula.copy();
		    if (!thisSubCopy.boundVars().contains(other.boundVariable)) { 
		    	thisSubCopy.substituteVar(this.boundVariable, other.boundVariable);
		    	return thisSubCopy.equals(other.subFormula);
		    } else {
		    	// both sides have to change their variable
		    	Formula otherSubCopy = other.subFormula.copy();
		    	Var freshVar = Var.allocateVar();
		    	otherSubCopy.substituteVar(other.boundVariable,freshVar);
		    	thisSubCopy.substituteVar(this.boundVariable,freshVar);
		    	return thisSubCopy.equals(otherSubCopy);
		    }
		}
		return false;
	}

	public int hashCode() {
		throw new RuntimeException("Should only use AllQuantFormula/ExistQuantFormula hashCode");
	}
	
	public Set<Predicate> getPredicates() {
		if (predicates != null) {
			return predicates;
		}
		predicates = subFormula.getPredicates();
		return predicates;
	}
	
	public void rebalanceQuantified() {
		subFormula.rebalanceQuantified();
	}

}
