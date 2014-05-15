package tvla.formulae;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.core.Node;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.generic.AdvancedCoerceOld;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.NoDuplicateLinkedList;
import tvla.util.Pair;

/** A transitive closure formula. It is possible to compute the transitive 
 * closure externally and set it using the setCalculatedTC formula.
 * If you want to do this make sure to set explicitRecalc so that the formula
 * won't recaluate the TC internally. 
 * @author Tal Lev-Ami
 */
final public class TransitiveFormula extends Formula {
	private Formula subFormula;
	private Var left;
	private Var right;
	private Var subLeft;
	private Var subRight;
	private TCCache cache;
	private boolean explicitRecalc = false;

	/** Create one temp var for equality tests requiring substitution with a temporary.
	 * This way we avoid creating a new temp for each test that needs it.
	 */
	private static final Var tempVar = Var.allocateVar();

	/** Determines whether to use a TC algorithm
	 * that operates in O(|V|*|E|) time worst-case (the old one takes Theta(V^3) ).
	 */
	private static final boolean newTCAlgorithm = true;
	
	private Assign tempAssign = null;

	/** Create a new transitive closure formula.
	 * @param left the left variable of the calculated TC.
	 * @param right the right variable of the calculated TC.
	 * @param subLeft the free variable of the sub formula the is considered as left by the TC.
	 * @param subRight the free variable of the sub formula the is considered as right by the TC.
	 * @param subFormula the sub formula on which the transitive closure is performed.
	 * Must have at least subLeft and subRight as free variables.
	 */ 
	public TransitiveFormula(Var left, Var right, Var subLeft, Var subRight, 
							 Formula subFormula) {
		super();
		this.left = left;
		this.right = right;
		this.subLeft = subLeft;
		this.subRight = subRight;
		this.tempAssign = new Assign();
		tempAssign.put(subLeft, null);
		tempAssign.put(subRight, null);
		List<Var> freeVars = subFormula.freeVars();
		if (!(freeVars.contains(subLeft)) ||
			!(freeVars.contains(subRight))) {
			throw new RuntimeException("TC's formula must have 2 free variables (" +
				subFormula + ")");
		}
		this.subFormula = subFormula;
	}

	/** Create a copy of the formula.
	 */
	public Formula copy() {
		return new TransitiveFormula(left, right, subLeft, subRight, subFormula.copy());
	}

	/** Substitute the given variable name to a new name. 
	 * Notice that if the old variable equals one of the sub formula bound
	 * variable it will not be substituted.
	 */
	public void substituteVar(Var from, Var to) {
		freeVars = null;
		if (left.equals(from))
			left = to;
		if (right.equals(from))
			right = to;

		if (subLeft.equals(to) || subRight.equals(to)) {
			throw new RuntimeException("Error. Substitution of " + from + " to " +
				to + " in subformula " + this + 
				" violates binding.");	    
		}

		if (subLeft.equals(from) || subRight.equals(from))
			return;

		subFormula.substituteVar(from, to);
	}

	/** Substitute variables in parallel according to the sub map. */
	public void substituteVars(Map<Var, Var> sub) {
		freeVars = null;

		if (sub.containsKey(left))
			left = sub.get(left);
		
		if (sub.containsKey(right))
			right = sub.get(right);
		
		if (sub.containsValue(subLeft) || sub.containsValue(subRight)) {
			throw new RuntimeException("Error. Substitution " + sub + 
				" in subformula " + this + " violates binding.");	    
		}

		if (sub.containsKey(subLeft)) {
			if (sub.size() == 1) return;

			// Do not alter the original map because it is used in the caller!
			sub = HashMapFactory.make(sub);
			sub.remove(subLeft);
		}

		if (sub.containsKey(subRight)) {
			if (sub.size() == 1) return;

			// Do not alter the original map because it is used in the caller!
			sub = HashMapFactory.make(sub);
			sub.remove(subRight);
		}

		subFormula.substituteVars(sub);
	}

	/** Rename the bound variables. 
	 * @author Alexey Loginov.
	 */
	public void normalize() {
		Var newVar1 = Var.allocateVar();
		Var newVar2 = Var.allocateVar();
		subFormula.substituteVar(subLeft, newVar1);
		subFormula.substituteVar(subRight, newVar2);
		subLeft = newVar1;
		subRight = newVar2;
		boundVars = null;
	}

	/** Substitute the left variable with the supplied one. 
	 * Used in differentiation.  Throws an exception if left occurs free in subformula.
	 */
	public void substituteLeft(Var to) {
		if (subFormula.freeVars().contains(left))
			throw new RuntimeException("Error. Substitution of left var " + left + " with " +
				to + " in subformula with left var free.");	    
		freeVars = null;
		left = to;
	}

	/** Substitute the right variable with the supplied one. 
	 * Used in differentiation.  Throws an exception if right occurs free in subformula.
	 */
	public void substituteRight(Var to) {
		if (subFormula.freeVars().contains(right))
			throw new RuntimeException("Error. Substitution of right var " + right + " with " +
				to + " in subformula with right var free.");	    
		freeVars = null;
		right = to;
	}

	/** Set the explicit recalculation flag.
	 */
	public void explicitRecalc() {
		explicitRecalc = true;
	}
	
	/** Set the externally calculated value for the formula.
	 * It is the caller responsibility to recalculate the TC as needed.
	 */
	public void setCalculatedTC(TCCache calculatedTC) {
		cache = calculatedTC;
	}

	/** Return the internally or externally calculated TC.
	 */
	public TCCache getCalculatedTC() {
		return cache;
	}

	/** Return the sub formula on which the transitive closure is performed.
	 */
	public Formula subFormula() {
		return subFormula;
	}

	/** Return the left variable of the TC.
	 */
	public Var left() {
		return left;
	}

	/** Return the right variable of the TC.
	 */
	public Var right() {
		return right;
	}

	/** Return the free variable of the sub formula the is considered as left
	 * by the TC.
	 */
	public Var subLeft() {
		return subLeft;
	}

	/** Return the free variable of the sub formula the is considered as 
	 * right by the TC.
	 */
	public Var subRight() {
		return subRight;
	}

	/** Prepare this formula for the new structure.
	 */
	public boolean askPrepare(TVS s) {
		subFormula.prepare(s);
		if (!explicitRecalc)
			cache = null;
		return true;
	}
	
	/** Calculates the transitive closure of the sub formula on the given
	 * structure.
	 * @since tvla-2-alpha Added ability to keep outside assignments (Alexey).
	 */
	public void calculateTC(TVS s, Assign assign) {
		long time = System.currentTimeMillis();

		Assign localAssign;
		if (assign == null) {
			localAssign = tempAssign;
		}
		else {
			localAssign = assign.copy();
			localAssign.put(subLeft, null);
			localAssign.put(subRight, null);
		}

		if (!newTCAlgorithm)
			calculateTC1(s, localAssign);
		else
			calculateTC3(s, localAssign);
		cache.validate();
		AdvancedCoerceOld.time_coerceTC += System.currentTimeMillis() - time;
	}
	
	public void invalidateTC() {
		if (cache != null)
			cache.invalidate();
	}
	
	/**
	 * This is the standard O(|V|^3) algorithm
	 */
	public void calculateTC1(TVS s, Assign localAssign) {
		// Keep outside assignments
		//Assign localAssign = (assign == null ? tempAssign : new Assign(assign));
		
		cache.clear();
		// compute the formula for all node pairs - the initial graph
		for (Node leftNode : s.nodes()) {
			for (Node rightNode : s.nodes()) {
				localAssign.putNode(subLeft, leftNode);
				localAssign.putNode(subRight, rightNode);
				cache.set(leftNode, rightNode, subFormula.eval(s, localAssign));
			}
		}
		
		// compute the transitive-closure of the graph
        for (Node node : s.nodes()) {
			Kleene nodeActive = s.eval(Vocabulary.active, node);
	        for (Node leftNode : s.nodes()) {
	            for (Node rightNode : s.nodes()) {
					cache.set(leftNode, rightNode,
							  Kleene.or(cache.get(leftNode, rightNode), 
										Kleene.and(Kleene.and(cache.get(leftNode, node),
															  cache.get(node, rightNode)),
												   nodeActive)
										));
				}
			}
		}
	}

	/** Calculates the transitive closure of the sub formula on the given structure.
	 * This is an O(|V|*|E|) algorithm.
	 * WARNING: this implementation still contains bugs.
	 * @author Roman Manevich.
	 * @since tvla-2-alpha Added ability to keep outside assignments (Alexey).
	 */
	public void calculateTC2(TVS s, Assign localAssign) {
		// Keep outside assignments
		//Assign localAssign = (assign == null ? tempAssign : new Assign(assign));

		// build the input graph
		cache.clear();
		Map<Node, Set<Pair<Node, Kleene>>> inGraph = HashMapFactory.make(s.nodes().size());
        for (Node leftNode : s.nodes()) {
			Set<Pair<Node, Kleene>> edges = HashSetFactory.make(2);
			inGraph.put(leftNode, edges);
	        for (Node rightNode : s.nodes()) {
				localAssign.putNode(subLeft, leftNode);
				localAssign.putNode(subRight, rightNode);
				Kleene edgeValue = subFormula.eval(s, localAssign);
				if (edgeValue != Kleene.falseKleene) {
					edges.add(new Pair<Node, Kleene>(rightNode, edgeValue));
					cache.set(leftNode, rightNode, edgeValue);
				}
			}
		}
		
		// build the output graph
		//cache.clear();
		Set<Node> visitedNodes = HashSetFactory.make(s.nodes().size());
		LinkedList<Node> workSet1 = new LinkedList<Node>();
		Set<Node> workSet2 = HashSetFactory.make();
        for (Node node : s.nodes()) {
			//Kleene nodeActive = s.eval(Vocabulary.active, node);
			// do a DFS-visit from this node
			workSet1.add(node);
			while (!workSet1.isEmpty()) {
				Node nextNode = workSet1.removeFirst();
				Kleene nextNodeActive = s.eval(Vocabulary.active, nextNode);
				Collection<Pair<Node, Kleene>> neighbors = inGraph.get(nextNode);
				Kleene pathValue = cache.get(node, nextNode);
				for (Pair<Node, Kleene> pair : neighbors) {
					Node neighbour = (Node) pair.first;
					if (visitedNodes.contains(neighbour)) {
						continue;
					}
					Kleene newValue = null;
					if (cache.get(node, neighbour) == Kleene.trueKleene) {
						newValue = Kleene.trueKleene;
					}
					else {
						Kleene edgeValue = (Kleene) pair.second;
						//Kleene active = Kleene.join(nodeActive, s.eval(Vocabulary.active, neighbour));
						Kleene active = Kleene.join(nextNodeActive, s.eval(Vocabulary.active, neighbour));
						//Kleene active = join(nodeActive, s.eval(Vocabulary.inac, neighbour));
						newValue = Kleene.join(Kleene.join(edgeValue, pathValue), active);
					}
					
					if (newValue == Kleene.unknownKleene) {
						workSet2.add(nextNode);
					}
					else {
						visitedNodes.add(neighbour);
						workSet1.addLast(neighbour); // a simulation of a queue
						cache.set(node, neighbour, Kleene.trueKleene);
					}
				}
			}
			while (!workSet2.isEmpty()) {
				Iterator<Node> setIter = workSet2.iterator();
				Node nextNode = setIter.next();
				setIter.remove();
				Collection<Pair<Node, Kleene>> neighbors = inGraph.get(nextNode);
				for (Pair<Node, Kleene> pair : neighbors) {
					Node neighbour = pair.first;
					if (visitedNodes.contains(neighbour) || cache.get(node, neighbour) != Kleene.falseKleene)
						continue;
					visitedNodes.add(neighbour);
					workSet2.add(neighbour); // a simulation of a queue
					cache.set(node, neighbour, Kleene.unknownKleene );
				}
			}
			
			// cleanup before next iteration
			visitedNodes.clear();
		}
	}	
	/** Calculates the transitive closure of the sub formula on the given structure.
	 * This is the new O(|V|*|E|) algorithm - hopefully not buggy anymore.
	 * @author Igor
	 */
	
	public void calculateTC3(TVS s, Assign localAssign) {
		// Keep outside assignments
		//Assign localAssign = (assign == null ? tempAssign : new Assign(assign));
		Collection<Node> nodes = s.nodes();

		// build the input graph
		cache.clear();
		Map<Node,Collection<Node>> inGraph = HashMapFactory.make(nodes.size());
		for (Iterator<Node> leftIter = nodes.iterator(); leftIter.hasNext(); ) {
			Node leftNode = leftIter.next();
			//Set edges = HashSetFactory.make(2);
            Collection<Node> edges = new LinkedList<Node>();
			//Set edges = new LinkedHashSet(2);
			inGraph.put(leftNode, edges);
			for (Iterator<Node> rightIter = nodes.iterator(); rightIter.hasNext(); ) {
				Node rightNode = rightIter.next();
				localAssign.putNode(subLeft, leftNode);
				localAssign.putNode(subRight, rightNode);
				Kleene edgeValue = subFormula.eval(s, localAssign);
				if (edgeValue != Kleene.falseKleene) {
					edges.add(rightNode);
					cache.set(leftNode, rightNode, edgeValue);
				}
			}
		}
		
		// build the output graph
		Map<Node,Kleene> visitedNodes = HashMapFactory.make(nodes.size());
		LinkedList<Node> workSet1 = new LinkedList<Node>();
		//Set workSet2 = HashSetFactory.make();
		for (Node node : nodes) {
			workSet1.add(node);
			while (!workSet1.isEmpty()) {
				Node nextNode = workSet1.removeFirst();
				Kleene nextNodeActive = s.eval(Vocabulary.active, nextNode);
				Collection<Node> neighbors = inGraph.get(nextNode);
				Kleene pathValue = cache.get(node, nextNode);
				for (Node neighbour : neighbors) {
					Kleene prevValue = visitedNodes.get(neighbour);
                    if (prevValue == Kleene.trueKleene) {
						continue;
					}
                    Kleene newValue = 
                        Kleene.or(cache.get(node, neighbour), 
                                  Kleene.and(Kleene.and(pathValue,
                                                        cache.get(nextNode, neighbour)),
                                             nextNodeActive)
                                 );
                    if (newValue == prevValue) {
                        continue;
                    }
					cache.set(node, neighbour, newValue);
					
					visitedNodes.put(neighbour, newValue);
					workSet1.addLast(neighbour); // a simulation of a queue
				}
			}
			
			// cleanup before next iteration
			visitedNodes.clear();
		}
	}	

	/** Evaluate the formula on the given structure and assignment. 
	 */
	public Kleene eval(TVS s, Assign assign) {
		if (cache == null) {
			cache = new TCCache();
			//calculateTC(s, assign);
		}
		
		// FIXME: BUG! Must pass assign, not null, and invalidate
		// the cache, if it contains some of subformula free vars...
		if (!cache.isValid() || !noOtherFreeVars) {
			// calculateTC(s, null);
			calculateTC(s, assign);
		}

		Node leftNode = assign.get(left);	
		Node rightNode = assign.get(right);

		assert leftNode != null : "Variable " + left.name() + " missing from assignment";
		assert rightNode != null : "Variable " + right.name() + " missing from assignment";
		assert cache != null : "Must call prepare before retrieving " + "values from a TC";

		return cache.get(leftNode, rightNode);
	}

	/** Calculate and return the free variables for this formula.
	 */
	protected boolean noOtherFreeVars = true;
	
	public List<Var> calcFreeVars() {
		List<Var> result = new NoDuplicateLinkedList<Var>(subFormula.freeVars());	
		result.remove(subLeft);
		result.remove(subRight);
		if (result.isEmpty()) {
			noOtherFreeVars = true;
		}
		else {
			noOtherFreeVars = false;
		}

		// Now add left and right at the end of the list,
		// (if they don't already occur free in subFormula).
		result.add(left);
		result.add(right);
		return result;
	}

	/** Calculate and return variables bound in this formula or subformulae. */
	public List<Var> calcBoundVars() {
		List<Var> result = new NoDuplicateLinkedList<Var>(subFormula.boundVars());
		// Make sure subLeft and subRight go at the start.
		result.remove(subLeft);
		result.remove(subRight);
		result.add(0, subLeft);
		result.add(1, subRight);
		return result;
	}

	/** Return a human readable representation of the formula.
	 */
	public String toString() {
		return "TC(" + subLeft + "," + subRight + ": " + subFormula + ")(" + left + "," + right + ")";
	}

	/** Equate the this formula with the given fomula by structure. 
	 */
	public boolean equals(Object o) {
		if (!(o instanceof TransitiveFormula))
			return false;
		TransitiveFormula other = (TransitiveFormula) o;
		if (this.left.equals(other.left) &&
		    this.right.equals(other.right)) {

		    if (this.subLeft.equals(other.subLeft) &&
			this.subRight.equals(other.subRight))
			// If the bound vars match then alpha renaming has no effect.
			return this.subFormula.equals(other.subFormula);

		    // Bound vars didn't match, try renaming them.
		    if (alphaRenamingEquals) {
			// See if formulae are equal up to alpha renaming.
			// Make a copy of our subformula and replace our bound vars
			// with those of other.  See if subformulae are then equal.
			Formula thisSubCopy = this.subFormula.copy();

			// subLeft and subRight are always distinct in a transitive formula.
			// However, we may have the same two vars in opposite order in the
			// two formulae.  In that case we need a temp variable.
			if (this.subRight.equals(other.subLeft)) {
			    // Can't just replace subLeft and then subRight.
			    thisSubCopy.substituteVar(this.subLeft, tempVar);
			    thisSubCopy.substituteVar(this.subRight, other.subRight);
			    thisSubCopy.substituteVar(tempVar, other.subLeft);
			} else {
			    // No substitution conflicts: do simple substitions.
			    thisSubCopy.substituteVar(this.subLeft, other.subLeft);
			    thisSubCopy.substituteVar(this.subRight, other.subRight);
			}
			return thisSubCopy.equals(other.subFormula);
		    }
		}
		return false;
	}

	public int hashCode() {
		if (!alphaRenamingEquals) 
			return subFormula.hashCode() * 167 + left.hashCode() * 31 + 
			   right.hashCode();
		else
			return ignoreVarHashCode();
	}

	public int ignoreVarHashCode() {
		return subFormula.ignoreVarHashCode() * 167;	
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
	
	public void traversePostorder(FormulaTraverser t) {
		subFormula.traverse(t);
		t.visit(this);
	}

	public void traversePreorder(FormulaTraverser t) {
		t.visit(this);
		subFormula.traverse(t);
	}

	
	public Set<Predicate> getPredicates() {
		if (predicates != null) {
			return predicates;
		}
		predicates = subFormula.getPredicates();
		return predicates;
	}
	
    /** A cache for transitive closures.
	 * @author Roman Manevich.
	*/
	public static class TCCache {
		/** a map from node pairs to the unknown and true values of the predicate.
		 */		
		private Map<tvla.formulae.TransitiveFormula.TCCache.Pair, Kleene> values;
		
		/** A reusable node pair.
		 */
		private static final Pair pair = new Pair(null, null);
		
		/** Create a concrete binary predicate with a null predicate.
		 */
		public TCCache() {
			this.values = HashMapFactory.make(11);
		}

		/** Return the value of the predicate for the given node pair.
		 */
		public Kleene get(Node l, Node r) {
			pair.set(l, r);
			Kleene value = values.get(pair);
			return value == null ? Kleene.falseKleene : value;
		}

		/** Set the value of the predicate for the given node pair.
		 */
		public void set(Node l, Node r, Kleene value) {
			pair.set(l, r);
			if (value == Kleene.falseKleene) {
				values.remove(pair);
				return;
			}

			values.put(new Pair(pair), value);
		}

		/** Clear all the true and unknown values of the predicate.
		 */
		public void clear() {
			values.clear();
		}
		
		private boolean valid = false;

		public boolean isValid() {
			return valid;
		}
		
		public void validate() {
			valid = true;
		}
		
		public void invalidate() {
			valid = false;
		}

		/** A pair of nodes.
		 */
		private static final class Pair {
			public Node left;
			public Node right;

			public Pair(Node left, Node right) {
				this.left = left;
				this.right = right;
			}
			
			public Pair(Pair other) {
				this.left = other.left;
				this.right = other.right;
			}

			public void set(Node left, Node right) {
				this.left = left;
				this.right = right;
			}

			public boolean equals(Object o) {
				if (!(o instanceof Pair))
					return false;
				Pair other = (Pair) o;
				return this.left.equals(other.left) &&
					   this.right.equals(other.right);
			}

			public int hashCode() {
				return left.hashCode() * 31 + right.hashCode();
			}
		}
	}	 
}
