package tvla.formulae;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.core.Node;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.EmptyIterator;
import tvla.util.NoDuplicateLinkedList;

/** An variable equality formula.
 * @author Tal Lev-Ami 
 */
final public class EqualityFormula extends AtomicFormula {
	private Var left;
	private Var right;

	/** Create a new equality formula between the two given variables. */
	public EqualityFormula(Var l, Var  r) {
		super();
		left = l; 
		right = r;
	}

	/** Create a copy of the formula */
	public Formula copy() {
		return new EqualityFormula(left, right);
	}

	/** Substitute the given variable name to a new name. */
	public void substituteVar(Var from, Var to) {
		if (left.equals(from))
			left = to;
		if (right.equals(from))
			right = to;
		freeVars = null;
	}
	
	/** Substitute variables in parallel according to the sub map. */
	public void substituteVars(Map<Var, Var> sub) {
		if (sub.containsKey(left))
			left = sub.get(left);
		if (sub.containsKey(right))
			right = sub.get(right);

		freeVars = null;
	}

	/** Return the left variable */
	public Var left() {
		return left;
	}
	
	/** Return the right variable */
	public Var right() {
		return right;
	}
	
	/** Evaluate the formula on the given structure and assignment. */
	public Kleene eval(TVS s, Assign assign) {
		if (left.equals(right))
			return Kleene.trueKleene;

		Node leftNode = assign.get(left);
		Node rightNode = assign.get(right);
		if (!leftNode.equals(rightNode)) {
			return Kleene.falseKleene;
		}
		
		return Kleene.not(s.eval(Vocabulary.sm, leftNode));
	}

	/** Calculate and return the free variables for this formula. */
	public List<Var> calcFreeVars() {
		List<Var> result = new NoDuplicateLinkedList<Var>();
		result.add(left);
		result.add(right);
		return result;
	}

	/** Return a human readable representation of the formula. */
	public String toString() {
		return left.toString() + "=" + right.toString();
	}

	/** Equate the this formula with the given fomula by structure. */
	public boolean equals(Object o) {
		if (!(o instanceof EqualityFormula))
			return false;
		EqualityFormula other = (EqualityFormula) o;
		return this.left.equals(other.left) && this.right.equals(other.right);
	}

	public int hashCode() {
		return left.hashCode() * 31 + right.hashCode();
	}
	
	public int ignoreVarHashCode() {
		return 37;
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
		predicates = new LinkedHashSet<Predicate>(2);
		predicates.add(Vocabulary.sm);
		return predicates;
	}
	
	public FormulaIterator assignments(TVS structure, Assign partial, Kleene value) {
		return new FormulaIterator(structure, this, partial, value) {

			public boolean step() {
				if (assignIterator == null) {
					stat_EqualityAssigns++;
			        //result.put(partial);

			        //formula.prepare(structure);

					// ROMAN: I changed the direct access to a guarded one,
					// since it fails on update formulae such as
					// "p(u,v) = u==v" as 'u' and 'v' are not in the partial
					// assignment.
			        //Node leftNode = partial.get(left);
			        //Node rightNode = partial.get(right);					
					Node leftNode = partial.contains(left) ? partial.get(left) : null;
					Node rightNode = partial.contains(right) ? partial.get(right) : null;
		
			        if ((leftNode != null) && (rightNode != null)) {
			          /*
			          if (partial instanceof AssignKleene) 
			        	  result = (AssignKleene)partial;
			          else {
			        	  result = new AssignKleene(partial, Kleene.falseKleene);
			          }
			          */
			          result = partial.instanceForIterator(freeVars, true);
			          assignIterator = EmptyIterator.instance();
			          result.kleene = eval(structure, result);
			          stat_Evals++;
			          stat_TotalEvals++;
			          return checkDesiredValue(result.kleene);
			        } else if ((leftNode == null) && (rightNode == null)) {
				      result = partial.instanceForIterator(freeVars, false);
		        	  //result = new AssignKleene(partial, Kleene.falseKleene);
		        	  result.put(left, null);
		        	  result.put(right, null);

			          stat_NonEvals++;
			          assignIterator = structure.nodes().iterator();
			        } else {
		        	  //result = new AssignKleene(partial, Kleene.falseKleene);
			          result = partial.instanceForIterator(freeVars, false);

			          assignIterator = EmptyIterator.instance();
			          if (leftNode == null) {	  
			            result.put(left, rightNode);
			          }
			          else if (rightNode == null) {
			        	result.put(right, leftNode);
			          }
			          result.kleene = eval(structure, result);
			          stat_Evals++;
			          stat_TotalEvals++;
			          return checkDesiredValue(result.kleene);
			        }
			    }
	
			    while (assignIterator.hasNext()) {
			        stat_TotalEvals++;
			  	  	Node node = (Node)assignIterator.next();
			  	  	result.putNode(left, node);
			  	  	result.putNode(right, node);
			        result.kleene = eval(structure, result);
			        if (checkDesiredValue(result.kleene)) {
			          return true;
			        }
			    }
			    return false;
			}
		};
	}
}