package tvla.formulae;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.base.BaseTVSCache;
import tvla.core.common.NodeValue;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.EmptyIterator;
import tvla.util.HashSetFactory;
import tvla.util.NoDuplicateLinkedList;

/** A logical negation formula.
 * @author Tal Lev-Ami
 */
final public class NotFormula extends Formula {
	private Formula subFormula;    

	/** Create a negation formula of the given sub formula. */
	public NotFormula(Formula subFormula) {
		super();
		this.subFormula = subFormula;
	}

	public void traversePostorder(FormulaTraverser t) {
		subFormula.traverse(t);
		t.visit(this);
	}

	public void traversePreorder(FormulaTraverser t) {
		t.visit(this);
		subFormula.traverse(t);
	}

	/** Create a copy of the formula */
	public Formula copy() {
		return new NotFormula(subFormula.copy());
	}

	/** Substitute the given variable name to a new name. */
	public void substituteVar(Var from, Var to) {
		subFormula.substituteVar(from, to);
		freeVars = null;
	}
	
	/** Substitute variables in parallel according to the sub map. */
	public void substituteVars(Map<Var, Var> sub) {
		subFormula.substituteVars(sub);
		freeVars = null;
	}

	/** Return the negated sub formula. */
	public Formula subFormula() {
		return subFormula;
	}

	/** Evaluate the formula on the given structure and assignment. */
	public Kleene eval(TVS s, Assign assign) {
		return Kleene.not(subFormula.eval(s, assign));
	}

	/** Prepare this formula for the new structure */
	public boolean askPrepare(TVS s) {
		return subFormula.askPrepare(s);
	}

	/** Calculate and return the free variables for this formula. */
	public List<Var> calcFreeVars() {
		return new NoDuplicateLinkedList<Var>(subFormula.freeVars());
	}

	/** Calculate and return variables bound in this formula or subformulae. */
	public List<Var> calcBoundVars() {
		return new NoDuplicateLinkedList<Var>(subFormula.boundVars());
	}

	/** Return a human readable representation of the formula. */
	public String toString() {
		return "!(" + subFormula.toString() + ")";
	}

	/** Equate the this formula with the given fomula by structure. */
	public boolean equals(Object o) {
		if (!(o instanceof NotFormula))
			return false;
		NotFormula other = (NotFormula) o;
		return this.subFormula.equals(other.subFormula);
	}

	public int hashCode() {
		return subFormula.hashCode();
	}
	
	public int ignoreVarHashCode() {
		return subFormula.ignoreVarHashCode();
	}
	
	public Set<Predicate> getPredicates() {
		if (predicates != null) {
			return predicates;
		}
		predicates = subFormula.getPredicates();
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
	
	public Formula pushBackNegations(boolean negated) {
		return subFormula.pushBackNegations(!negated);
	}
	
	public FormulaIterator assignments(TVS structure, Assign partial, Kleene value) {
		if (subFormula instanceof PredicateFormula && value != Kleene.falseKleene) {
			return new FormulaIterator(structure, this, partial, value) {
				private Node[] partialNodes = null;
				private Var[] variables = null;
				
				public boolean step() {
				  if (assignIterator == null) {
					  stat_DefaultAssigns++;
					  if (buildFullAssignment()) {
						  assignIterator = EmptyIterator.instance();
						  result.kleene = formula.eval(structure, result);
						  stat_Evals++;
						  stat_TotalEvals++;
						  return checkDesiredValue(result.kleene);
					  } else {
						  stat_PredicateNegationAssigns++;
						  PredicateFormula pf = (PredicateFormula)subFormula;
						  variables = pf.variables();
				          partialNodes = new Node[variables.length];
				          
				          for (int i = 0; i < variables.length; i++) {
				        	// FIXME: should check with .contain first...
				            partialNodes[i] = partial.get(variables[i]);
				            if (partialNodes[i] == null) {
				            	result.addVar(variables[i]);
				            }
				          }

						  Collection<NodeValue> values = BaseTVSCache.getValues(structure, pf.predicate(), partialNodes);
						  if (values == null) {
							  values = new LinkedList<NodeValue>();
							  Set<Var> freeVars = HashSetFactory.make(formula.freeVars());
							  freeVars.removeAll(partial.bound());
							  assignIterator = Assign.getAllAssign(structure.nodes(), freeVars, partial);
							  
							  while (assignIterator.hasNext()) {
								  Assign assign = (Assign) assignIterator.next();
								  stat_Evals++;
								  Kleene value = formula.eval(structure, assign);
								  if (value != Kleene.falseKleene) {
									  values.add(new NodeValue(pf.makeTuple(assign), value));
								  }
							  }
							  BaseTVSCache.setValues(structure, pf.predicate(), partialNodes, values);
						  }
						  assignIterator = values.iterator();
					  }
				  }

			      OUTER: while (assignIterator.hasNext()) {
				    	stat_TotalEvals += .17;
				    	NodeValue nv = (NodeValue) assignIterator.next();
				        Kleene tupleValue = (Kleene)nv.value;

				        if (!checkDesiredValue(tupleValue))
				        	continue;

				        NodeTuple nt = nv.tuple;				        
				        for (int i = 0; i < variables.length; i++) {
					      	for (int j = 0; j < i; j++) {
					      		if ((variables[i] == variables[j]) && (nt.get(i) != nt.get(j)))
					      			continue OUTER;
					      	}
					      	result.putNode(variables[i], nt.get(i));
				        }
				        result.kleene = tupleValue;
				        return true;
				      }
				      return false;
				}

			};
		}
		else return super.assignments(structure, partial, value);
	}
}