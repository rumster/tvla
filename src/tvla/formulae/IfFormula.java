package tvla.formulae;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.exceptions.SemanticErrorException;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.NoDuplicateLinkedList;

/** A if then else formula - defined by:
 * if(condSub, trueSub, falseSub) &lt;==&gt; (condSub & trueSub) | (!condSub & falseSub).
 * @author Tal Lev-Ami 
 */
public class IfFormula extends Formula {
	private Formula condSubFormula;    
	private Formula trueSubFormula;
	private Formula falseSubFormula;

	/** Create a new if then else formula from the given sub formulae. */
	public IfFormula(Formula condSubFormula, Formula trueSubFormula, Formula falseSubFormula) {
		super();
		this.condSubFormula = condSubFormula;
		this.trueSubFormula = trueSubFormula;
		this.falseSubFormula = falseSubFormula;
	}

	/** Create a copy of the formula */
	public Formula copy() {
		return new IfFormula(condSubFormula.copy(), trueSubFormula.copy(), falseSubFormula.copy());
	}

	/** Substitute the given variable name to a new name. */
	public void substituteVar(Var from, Var to) {
		condSubFormula.substituteVar(from, to);
		trueSubFormula.substituteVar(from, to);
		falseSubFormula.substituteVar(from, to);
		freeVars = null;
	}
	
	/** Substitute variables in parallel according to the sub map. */
	public void substituteVars(Map<Var, Var> sub) {
		condSubFormula.substituteVars(sub);
		trueSubFormula.substituteVars(sub);
		falseSubFormula.substituteVars(sub);
		freeVars = null;
	}

	/** Return the condition sub formula. */
	public Formula condSubFormula() {
		return condSubFormula;
	}

	/** Return the true sub formula. */
	public Formula trueSubFormula() {
		return trueSubFormula;
	}

	/** Return the false sub formula. */
	public Formula falseSubFormula() {
		return falseSubFormula;
	}

	/** Evaluate the formula on the given structure and assignment. */
	public Kleene eval(TVS s, Assign assign) {
		Kleene condResult = condSubFormula.eval(s, assign);
		if (condResult == Kleene.trueKleene)
			return trueSubFormula.eval(s, assign);
		else if (condResult == Kleene.falseKleene)
			return falseSubFormula.eval(s, assign);
		else
			return Kleene.join(trueSubFormula.eval(s, assign), falseSubFormula.eval(s, assign)); 
	}

	/** Prepare this formula for the new structure */
	public boolean askPrepare(TVS s) {
		boolean condPrepare = condSubFormula.askPrepare(s);
        boolean truePrepare = trueSubFormula.askPrepare(s);
        boolean falsePrepare = falseSubFormula.askPrepare(s);
        return condPrepare || truePrepare || falsePrepare;
	}

	/** Calculate and return the free variables for this formula. */
	public List<Var> calcFreeVars() {
		List<Var> result = new NoDuplicateLinkedList<Var>();
		result.addAll(condSubFormula.freeVars());
		result.addAll(trueSubFormula.freeVars());
		result.addAll(falseSubFormula.freeVars());
        for (Var bound : boundVars()) {
            if (result.contains(bound)) {
                throw new SemanticErrorException("Formula " + this + " has " + bound + " as both free & bound variable");
            }
        }
		return result;
	}

	/** Calculate and return variables bound in this formula or subformulae. */
	public List<Var> calcBoundVars() {
		List<Var> result = new NoDuplicateLinkedList<Var>();
		result.addAll(condSubFormula.boundVars());
		result.addAll(trueSubFormula.boundVars());
		result.addAll(falseSubFormula.boundVars());
		return result;
	}

	/** Return a human readable representation of the formula. */
	public String toString() {
		return "(" + condSubFormula + " ? " + trueSubFormula + " : " + 
			   falseSubFormula + ")";
	}

	/** Equate the this formula with the given fomula by structure. */
	public boolean equals(Object o) {
		if (!(o instanceof IfFormula))
			return false;
		IfFormula other = (IfFormula) o;
		return this.condSubFormula.equals(other.condSubFormula) &&
			   this.trueSubFormula.equals(other.trueSubFormula) &&
			   this.falseSubFormula.equals(other.falseSubFormula);
	}

	public int hashCode() {
		return condSubFormula.hashCode() * 167 + trueSubFormula.hashCode() * 31 + 
			   falseSubFormula.hashCode();
	}
	
	public int ignoreVarHashCode() {
		return condSubFormula.ignoreVarHashCode() * 167 
			+ trueSubFormula.ignoreVarHashCode() * 31 
			+ falseSubFormula.ignoreVarHashCode();
	}
	
	/** Calls the specific accept method, based on the type of this formula
	 * (Visitor pattern).
	 * @author Roman Manevich.
	 * @since tvla-2-alpha November 18 2002, Initial creation.
	 */
    @Override
    public <T> T visit(FormulaVisitor<T> visitor) {
		return visitor.accept(this);
	}

	public Set<Predicate> getPredicates() {
		if (predicates != null) {
			return predicates;
		}
		predicates = new LinkedHashSet<Predicate>();
		predicates.addAll(condSubFormula.getPredicates());
		predicates.addAll(trueSubFormula.getPredicates());
		predicates.addAll(falseSubFormula.getPredicates());
		return predicates;
	}
	
	public Formula pushBackNegations(boolean negated) {
		if (!negated) {
			condSubFormula = condSubFormula.pushBackNegations(false);
			trueSubFormula = trueSubFormula.pushBackNegations(false);
			falseSubFormula = falseSubFormula.pushBackNegations(false);
			return this;
		}
		else {
			Formula notA = trueSubFormula.pushBackNegations(true);
			Formula notB = falseSubFormula.pushBackNegations(true); 
			Formula f1 = new IfFormula(condSubFormula.pushBackNegations(false),
									   notA, notB);
			Formula f2 = new AndFormula(notA, notB);
			return new OrFormula(f1, f2);
		}
	}

	public Formula pushBackQuant(Var bound, boolean allQuant) {
		if (bound == null || !freeVars().contains(bound)) {
			condSubFormula = condSubFormula.pushBackQuant(null, false);
			trueSubFormula = trueSubFormula.pushBackQuant(null, false);
			falseSubFormula = falseSubFormula.pushBackQuant(null, false);
			freeVars = boundVars = null;
			return this;
		}
		else  {
			condSubFormula = condSubFormula.pushBackQuant(null, false);
			if (condSubFormula.freeVars().contains(bound) || 
					(trueSubFormula.freeVars().contains(bound) && 
				     falseSubFormula.freeVars().contains(bound))) {
				trueSubFormula = trueSubFormula.pushBackQuant(null, false);
				falseSubFormula = falseSubFormula.pushBackQuant(null, false);
				freeVars = boundVars = null;
				return allQuant ? new AllQuantFormula(bound, this) : 
								  new ExistQuantFormula(bound, this);
			}
			else if (!trueSubFormula.freeVars().contains(bound)) {
				trueSubFormula = trueSubFormula.pushBackQuant(null, false);
				falseSubFormula = falseSubFormula.pushBackQuant(bound, allQuant);
				freeVars = boundVars = null;
				return this;
			}
			else {
				falseSubFormula = falseSubFormula.pushBackQuant(null, false);
				trueSubFormula = trueSubFormula.pushBackQuant(bound, allQuant);
				freeVars = boundVars = null;
				return this;
			}
		}
	}
	
	public void rebalanceQuantified() {
		condSubFormula.rebalanceQuantified();
		trueSubFormula.rebalanceQuantified();
		falseSubFormula.rebalanceQuantified();
	}
	
	public void traversePostorder(FormulaTraverser t) {
		condSubFormula.traverse(t);
		trueSubFormula.traverse(t);
		falseSubFormula.traverse(t);
		t.visit(this);
	}

	public void traversePreorder(FormulaTraverser t) {
		t.visit(this);
		condSubFormula.traverse(t);
		trueSubFormula.traverse(t);
		falseSubFormula.traverse(t);
	}
	
/*	
	public Formula pushBackExistQuant(Var bound) {
		if (bound == null || !freeVars().contains(bound)) {
			condSubFormula = condSubFormula.pushBackExistQuant(null);
			trueSubFormula = trueSubFormula.pushBackExistQuant(null);
			falseSubFormula = falseSubFormula.pushBackExistQuant(null);
			freeVars = boundVars = null;
			return this;
		}
		else  {
			condSubFormula = condSubFormula.pushBackExistQuant(null);
			if (condSubFormula.freeVars().contains(bound) || 
					(trueSubFormula.freeVars().contains(bound) && 
				     falseSubFormula.freeVars().contains(bound))) {
				trueSubFormula = trueSubFormula.pushBackExistQuant(null);
				falseSubFormula = falseSubFormula.pushBackExistQuant(null);
				freeVars = boundVars = null;
				return new ExistQuantFormula(bound, this);
			}
			else if (!trueSubFormula.freeVars().contains(bound)) {
				trueSubFormula = trueSubFormula.pushBackExistQuant(null);
				falseSubFormula = falseSubFormula.pushBackExistQuant(bound);
				freeVars = boundVars = null;
				return this;
			}
			else {
				falseSubFormula = falseSubFormula.pushBackExistQuant(null);
				trueSubFormula = trueSubFormula.pushBackExistQuant(bound);
				freeVars = boundVars = null;
				return this;
			}
		}
	}

	public Formula pushBackAllQuant(Var bound) {
		if (bound == null || !freeVars().contains(bound)) {
			condSubFormula = condSubFormula.pushBackAllQuant(null);
			trueSubFormula = trueSubFormula.pushBackAllQuant(null);
			falseSubFormula = falseSubFormula.pushBackAllQuant(null);
			freeVars = boundVars = null;
			return this;
		}
		else  {
			condSubFormula = condSubFormula.pushBackAllQuant(null);
			if (condSubFormula.freeVars().contains(bound) || 
					(trueSubFormula.freeVars().contains(bound) && 
				     falseSubFormula.freeVars().contains(bound))) {
				trueSubFormula = trueSubFormula.pushBackAllQuant(null);
				falseSubFormula = falseSubFormula.pushBackAllQuant(null);
				freeVars = boundVars = null;
				return new AllQuantFormula(bound, this);
			}
			else if (!trueSubFormula.freeVars().contains(bound)) {
				trueSubFormula = trueSubFormula.pushBackAllQuant(null);
				falseSubFormula = falseSubFormula.pushBackAllQuant(bound);
				freeVars = boundVars = null;
				return this;
			}
			else {
				falseSubFormula = falseSubFormula.pushBackAllQuant(null);
				trueSubFormula = trueSubFormula.pushBackAllQuant(bound);
				freeVars = boundVars = null;
				return this;
			}
		}
	}
*/
}