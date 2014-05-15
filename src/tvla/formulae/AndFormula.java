package tvla.formulae;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignKleene;
import tvla.exceptions.SemanticErrorException;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.EmptyIterator;
import tvla.util.NoDuplicateLinkedList;
import tvla.util.SingleIterator;

/** A logical conjunction formula.
 * @author Tal Lev-Ami.
 */
final public class AndFormula extends Formula {
	private Formula leftSubFormula;    
	private Formula rightSubFormula;

	/** Create a new logical and formula from the given sub formulae. */
	public AndFormula(Formula leftSubFormula, Formula rightSubFormula) {
		super();
		this.leftSubFormula = leftSubFormula;
		this.rightSubFormula = rightSubFormula;
	}

	/** Create a copy of the formula */
	public Formula copy() {
		return new AndFormula(leftSubFormula.copy(), rightSubFormula.copy());
	}

	/** Substitute the given variable name to a new name. */
	public void substituteVar(Var from, Var to) {
		leftSubFormula.substituteVar(from, to);
		rightSubFormula.substituteVar(from, to);
		freeVars = null;
	}

	/** Substitute variables in parallel according to the sub map. */
	public void substituteVars(Map<Var,Var> sub) {
		leftSubFormula.substituteVars(sub);
		rightSubFormula.substituteVars(sub);
		freeVars = null;		
	}

	/** Return the left sub formula */
	public Formula left() {
		return leftSubFormula;
	}

	/** Return the right sub formula */
	public Formula right() {
		return rightSubFormula;
	}

	/** Evaluate the formula on the given structure and assignment. */
	public Kleene eval(TVS s, Assign assign) {
		Kleene result = leftSubFormula.eval(s, assign);
		if (result == Kleene.falseKleene)
			return Kleene.falseKleene;
		return Kleene.and(result, rightSubFormula.eval(s, assign));
	}

    /** Prepare this formula for the new structure */
	public boolean askPrepare(TVS s) {
		boolean leftPrepare = leftSubFormula.askPrepare(s);
        boolean rightPrepare = rightSubFormula.askPrepare(s);
        return leftPrepare || rightPrepare;
	}

	/** Calculate and return the free variables for this formula. */
	public List<Var> calcFreeVars() {
		List<Var> result = new NoDuplicateLinkedList<Var>();
		result.addAll(leftSubFormula.freeVars());
		result.addAll(rightSubFormula.freeVars());
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
		result.addAll(leftSubFormula.boundVars());
		result.addAll(rightSubFormula.boundVars());
		return result;
	}

	/** Return a human readable representation of the formula. */
	public String toString() {
		return "(" + leftSubFormula.toString() + " & " + rightSubFormula.toString() + ")";
	}

	/** Equate the this formula with the given fomula by structure. */
	public boolean equals(Object o) {
		if (!(o instanceof AndFormula))
			return false;
		AndFormula other = (AndFormula) o;
		return this.leftSubFormula.equals(other.leftSubFormula) &&
			   this.rightSubFormula.equals(other.rightSubFormula);
	}

	public int hashCode() {
		return leftSubFormula.hashCode() * 31 + rightSubFormula.hashCode();
	}
	
	public int ignoreVarHashCode() {
		return leftSubFormula.ignoreVarHashCode() * 31 + 
			rightSubFormula.ignoreVarHashCode();
	}

	public Set<Predicate> getPredicates() {
		if (predicates != null) {
			return predicates;
		}
		predicates = new LinkedHashSet<Predicate>();
		predicates.addAll(leftSubFormula.getPredicates());
		predicates.addAll(rightSubFormula.getPredicates());
		return predicates;
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
	
	public void toCNFArray(Collection<Formula> partial) {
		leftSubFormula.toCNFArray(partial);
		rightSubFormula.toCNFArray(partial);
	}
	
	public Formula pushBackNegations(boolean negated) {
		if (!negated) {
			leftSubFormula = leftSubFormula.pushBackNegations(false);
			rightSubFormula = rightSubFormula.pushBackNegations(false);
			return this;
		}
		else {
			return new OrFormula(leftSubFormula.pushBackNegations(true),
								 rightSubFormula.pushBackNegations(true));
			
		}
	}
	
	public Formula pushBackQuant(Var bound, boolean allQuant) {
		if (bound == null || !freeVars().contains(bound)) {
			leftSubFormula = leftSubFormula.pushBackQuant(null, false);
			rightSubFormula = rightSubFormula.pushBackQuant(null, false);
			freeVars = boundVars = null;
			return this;
		}
		else  {
			if (leftSubFormula.freeVars().contains(bound) && 
				rightSubFormula.freeVars().contains(bound)) {
				leftSubFormula = leftSubFormula.pushBackQuant(null, false);
				rightSubFormula = rightSubFormula.pushBackQuant(null, false);
				freeVars = boundVars = null;
				return allQuant? new AllQuantFormula(bound, this) : 
								 new ExistQuantFormula(bound, this);
			}
			else if (!leftSubFormula.freeVars().contains(bound)) {
				rightSubFormula = rightSubFormula.pushBackQuant(bound, allQuant);
				leftSubFormula = leftSubFormula.pushBackQuant(null, false);
				freeVars = boundVars = null;
				return this;
			}
			else {
				Formula newRight = leftSubFormula.pushBackQuant(bound, allQuant); 
				Formula newLeft = rightSubFormula.pushBackQuant(null, false);
				rightSubFormula = newRight;
				leftSubFormula = newLeft;
				freeVars = boundVars = null;
				return this;
			}
		}
	}
	
	public void rebalanceQuantified() {
		leftSubFormula.rebalanceQuantified();
		rightSubFormula.rebalanceQuantified();
		if (leftSubFormula.boundVars().size() > rightSubFormula.boundVars().size()) {
			Formula temp = leftSubFormula;
			leftSubFormula = rightSubFormula;
			rightSubFormula = temp;
		}
	}
	
	public void traversePostorder(FormulaTraverser t) {
		leftSubFormula.traverse(t);
		rightSubFormula.traverse(t);
		t.visit(this);
	}

	public void traversePreorder(FormulaTraverser t) {
		t.visit(this);
		leftSubFormula.traverse(t);
		rightSubFormula.traverse(t);
	}
	
	/*
	public FormulaIterator assignments(TVS structure, Assign partial, Kleene value) {
		return new FormulaIterator(structure, this, partial, value) {
			Map rightSet;
			Set commonVars = null;
		    public boolean step() {
		    	Assign assign2;
		        assign2 = new Assign();
				if (assignIterator == null) {
			        result.put(partial);

			        formula.prepare(structure);
			        Set stillFree = HashSetFactory.make(formula.freeVars());
			        stillFree.removeAll(partial.bound());
			        if (stillFree.isEmpty()) {
			          assignIterator = EmptyIterator.instance();
			          result.kleene = eval(structure, result);
			          return checkDesiredValue(result.kleene);
			        }
			        else {
			          commonVars = HashSetFactory.make(leftSubFormula.freeVars());
			          commonVars.retainAll(rightSubFormula.freeVars());
			          rightSet = new TreeMap();
			          for (Iterator it=rightSubFormula.assignments(structure, partial); it.hasNext();) {
			        	  AssignKleene assign = (AssignKleene)it.next();
			        	  assign2.put(assign);
			        	  assign2.project(commonVars);
			        	  rightSet.put(assign2, assign.kleene);
			          }
			          assignIterator = leftSubFormula.assignments(structure, partial);
			        }
				}
			        
		        while (assignIterator.hasNext()) {
		        	AssignKleene assignLeft = (AssignKleene)assignIterator.next();
		        	assign2.put(assignLeft);
		        	assign2.project(commonVars);
		        	Kleene rightKleene = (Kleene)rightSet.get(assign2);
		        	if (rightKleene != null) {
		        		result.put(assignLeft);
		        		result.kleene = Kleene.and(assignLeft.kleene, rightKleene);
		        		if (checkDesiredValue(result.kleene))
		        			return true;
		        	}
		        	
		        }
		        return false;
		    }
		};
	}
	*/
	/*	
	public FormulaIterator assignments(TVS structure, Assign partial, Kleene value) {
		return new FormulaIterator(structure, this, partial, value) {
			int currentStep;
		    Iterator[] stepIt = new Iterator[2];
		    Kleene prevKleene;
		    Formula left = leftSubFormula;
		    Formula right = rightSubFormula;
		    
		    public boolean step() {
				if (assignIterator == null) {
			        result.put(partial);

			        formula.prepare(structure);
			        Set stillFree = HashSetFactory.make(formula.freeVars());
			        stillFree.removeAll(partial.bound());
			        if (stillFree.isEmpty()) {
			          assignIterator = stepIt[0] = EmptyIterator.instance();
  			          currentStep = -1;
			          result.kleene = eval(structure, result);
			          return checkDesiredValue(result.kleene);
			        } else {
			          currentStep = 0;
			          assignIterator = stepIt[0] = left.assignments(structure, partial);
			        }
			    }
	
			    while (currentStep >= 0) {
			    	if (stepIt[currentStep].hasNext()) {
				  	  	AssignKleene assign = (AssignKleene)stepIt[currentStep].next();
				    	if (currentStep == 1) {
					  	  	result.put(assign);
					        result.kleene = Kleene.and(prevKleene, assign.kleene);
					        if (checkDesiredValue(result.kleene)) {
					          return true;
					        }
				    	}
				    	else {
				    		currentStep++;
				    		stepIt[currentStep] = right.assignments(structure, assign);
				    		prevKleene = assign.kleene;
				    	}
			    	}
			    	else {
			    		currentStep--;
			    	}
			    }
			    return false;
			}
		};
	}
	*/

	
    public FormulaIterator assignments(TVS structure, Assign partial, Kleene value) {
        return new FormulaIterator(structure, this, partial, value) {
            int currentStep;
            Iterator[] stepIt;
            List<Formula> conjuncts;
            long unknown = 0;
            
            public boolean step() {
                if (assignIterator == null) {
                    if (buildFullAssignment()) {
                      assignIterator = EmptyIterator.instance();
                      currentStep = -1;
                      result.kleene = eval(structure, result);
                      return checkDesiredValue(result.kleene);
                    } else {
                      conjuncts = new ArrayList<Formula>();
                      Formula.getAnds(formula, conjuncts);
                      stepIt = new Iterator[conjuncts.size()+1];
                      assignIterator = stepIt[0] = SingleIterator.instance(result);
                      currentStep = 0;
                    }
                }
    
                while (currentStep >= 0) {
                    if (stepIt[currentStep].hasNext()) {
                        AssignKleene assign = (AssignKleene)stepIt[currentStep].next();
                        if (assign.kleene == Kleene.unknownKleene && desiredValue == Kleene.trueKleene) {
                            continue; // Already unknown, no chance to become true.
                        }
                        if (currentStep == conjuncts.size()) {
                            result.put(assign);
                            Kleene prevKleene = unknown == 0 ? Kleene.trueKleene : Kleene.unknownKleene;  
                            result.kleene = Kleene.and(prevKleene, assign.kleene);
                            if (checkDesiredValue(result.kleene)) {
                                return true;
                            }
                        }
                        else {
                            if (assign.kleene == Kleene.unknownKleene) {
                                unknown |= 1 << currentStep;
                            }
                            Kleene wantedValue = desiredValue == Kleene.unknownKleene ? null : desiredValue;
                            stepIt[currentStep+1] = conjuncts.get(currentStep).assignments(structure, assign, wantedValue);
                            currentStep++;
                        }
                    }
                    else {
                        currentStep--;
                        unknown &= (1 << currentStep) - 1;
                    }
                }
                return false;
            }
        };
    }
}