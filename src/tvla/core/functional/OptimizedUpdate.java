package tvla.core.functional;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import tvla.core.Node;
import tvla.core.assignments.Assign;
import tvla.formulae.Formula;
import tvla.formulae.IfFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.PredicateUpdateFormula;
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.HashMapFactory;

public class OptimizedUpdate {

	// Representation of optimized update strategies for an update to
	// a single unary or binary predicate:

	private static class OptimizedUnary {
		public PredicateUpdateFormula updateFormula;
		public Predicate filter;
		public OptimizedUnary (PredicateUpdateFormula uf, Predicate p) {
			updateFormula = uf;
			filter = p;
		}
	}

	private static class OptimizedBinary {
		public PredicateUpdateFormula updateFormula;
		public Predicate leftFilter, rightFilter;
		public OptimizedBinary (PredicateUpdateFormula uf, Predicate left, Predicate right) {
			updateFormula = uf;
			leftFilter = left;
			rightFilter = right;
		}
	}

	// Representation of an optimized update strategy for an action:

	private static class OptimizedUpdateRep {
		public Predicate index;
		public PredicateUpdateFormula nullaryUpdates[];
		public OptimizedUnary unaryUpdates[];
		public OptimizedBinary binaryUpdates[];

		public OptimizedUpdateRep (
								   PredicateUpdateFormula n[], OptimizedUnary u[], OptimizedBinary b[],
								   Predicate P
								   ) {
			nullaryUpdates = n;
			unaryUpdates = u;
			binaryUpdates = b;
			index = P;
		}
	}

	// Implementing an optimized update action:

	public static void updatePredicates(NodePredTVS S, OptimizedUpdateRep updateFormulae, Assign assignment) {
		// System.err.println("Applying optimized update formulae");

		// If an index predicate is to be used in the optimized evaluation,
		// determine set of nodes satisfying the index predicate.
		Predicate index = updateFormulae.index;
		Nodelist filter = null;
		if (index != null)  {
			filter = S.nonFalse(index);
			/*
			for (Iterator it = oldNodes.iterator(); it.hasNext(); ) {
			Node n = (Node) it.next();
			Kleene k = oldVersion.eval (index, n);
			if (k != Kleene.falseKleene)
			filter = new Nodelist (n, filter);
			}
			*/
		}

		NodePredTVS oldVersion = (NodePredTVS) S.copy();
		Nodelist oldNodes = oldVersion.U.elements();


		// Perform all nullary predicate updates:
		for (int i = 0; i < updateFormulae.nullaryUpdates.length - 1; i++) {
			PredicateUpdateFormula updateFormula = updateFormulae.nullaryUpdates[i];
			Formula formula = updateFormula.getFormula(); // the formula's right-hand side
			Predicate predicate = updateFormula.getPredicate();
            
			// Attempt to solve TC cache bug by calling prepare on eval
            formula.prepare(oldVersion);
            
			Kleene newValue = formula.eval(oldVersion, assignment);
			S.update(predicate, newValue);
		}

		// Perform all unary predicate updates:
		for (int i = 0; i < updateFormulae.unaryUpdates.length - 1; i++) {
			OptimizedUnary optimizedUnary = updateFormulae.unaryUpdates[i];
			PredicateUpdateFormula updateFormula = optimizedUnary.updateFormula;
			Formula formula = updateFormula.getFormula(); // the formula's right-hand side
			Predicate predicate = updateFormula.getPredicate();

			Assign resultAssign = new Assign(assignment);
			Var var = updateFormula.getVariable(0);
			formula.prepare(oldVersion);
			// If filter applies to this update, use smaller filter set;
			// otherwise, update all nodes.
			Nodelist candidates = ((optimizedUnary.filter == index) && (index != null)) ? filter : oldNodes;

			for (Iterator it = new NodelistIterator(candidates); it.hasNext(); ) {
				Node n = (Node) it.next();
				resultAssign.put(var, n);
				Kleene rval = formula.eval(oldVersion, resultAssign); 
				S.update(predicate, n, rval);
			}
		}

		// Perform all binary predicate updates:
		for (int i = 0; i < updateFormulae.binaryUpdates.length - 1; i++) {
			OptimizedBinary optimizedBinary = updateFormulae.binaryUpdates[i];
			PredicateUpdateFormula updateFormula = optimizedBinary.updateFormula;
			Formula formula = updateFormula.getFormula(); // the formula's right-hand side
			Predicate predicate = updateFormula.getPredicate();

			Assign resultAssign = new Assign(assignment);
			Var leftVar = updateFormula.getVariable(0);
			Var rightVar = updateFormula.getVariable(1);
			formula.prepare(oldVersion);
			// Use filter for left and right candidates, as appropriate:
			Nodelist leftCandidates = 
									 ((optimizedBinary.leftFilter == index) && (index != null)) ? filter : oldNodes;
			Nodelist rightCandidates = 
									  ((optimizedBinary.rightFilter == index) && (index != null)) ? filter : oldNodes;
			for (Iterator it1 = new NodelistIterator(leftCandidates); it1.hasNext(); ) {
				Node n1 = (Node) it1.next();
				resultAssign.put(leftVar, n1);
				for (Iterator it2 = new NodelistIterator(rightCandidates); it2.hasNext(); ) {
					Node n2 = (Node) it2.next();
					resultAssign.put(rightVar, n2);
					Kleene rval = formula.eval(oldVersion, resultAssign); 
					S.update(predicate, n1, n2, rval);
				}
			}
		}
	}

	static private Predicate findGuard(PredicateUpdateFormula updateFormula) {
		// Given an update of the form Q(x) = rhs,
		Predicate Q = updateFormula.getPredicate();
		Var x = updateFormula.getVariable(0);
		Formula rhs = updateFormula.getFormula();

		// Check if rhs is of the form (guard ? tf : ff) ...
		if (rhs instanceof IfFormula) {
			IfFormula ifFormula = (IfFormula) rhs;
			Formula guard = ifFormula.condSubFormula();
			Formula ff = ifFormula.falseSubFormula();

			// and check if guard is of the form P(x), where x is as above:
			if (guard instanceof PredicateFormula &&
				((PredicateFormula) guard).predicate().arity() == 1) {
				PredicateFormula upf = (PredicateFormula) guard;
				Predicate P = upf.predicate();
				Var y = upf.getVariable(0);

				if (y.equals(x)) {
					// Check if ff is the same as the lhs, i.e. Q(x) :
					if (ff instanceof PredicateFormula &&
						((PredicateFormula) guard).predicate().arity() == 1) {
						PredicateFormula upf2 = (PredicateFormula) ff;
						Predicate R = upf2.predicate();
						Var z = upf2.getVariable(0);
						
						if (z.equals(x) && (R == Q)) {
							// Then, P is a guard-filter for the update:
							return P;
						}
					}
				}
			}
		}
		return null;
	}

	static private Predicate findGuard(PredicateUpdateFormula updateFormula, Var fv) {
		// Given an update of the form Q(w,x) = rhs,
		Predicate Q = updateFormula.getPredicate();
		Var w = updateFormula.getVariable(0);
		Var x = updateFormula.getVariable(1);
		Formula rhs = updateFormula.getFormula();

		// Check if rhs is of the form (guard ? tf : ff) ...
		if (rhs instanceof IfFormula) {
			IfFormula ifFormula = (IfFormula) rhs;
			Formula guard = ifFormula.condSubFormula();
			Formula ff = ifFormula.falseSubFormula();

			// and check if guard is of the form P(fv), where fv is as above:
			if (guard instanceof PredicateFormula &&
				((PredicateFormula) guard).predicate().arity() == 1) {
				PredicateFormula upf = (PredicateFormula) guard;
				Predicate P = upf.predicate();
				Var y = upf.getVariable(0);

				if (y.equals(fv)) {
					// Check if ff is the same as the lhs, i.e. Q(w,x) :
					if (ff instanceof PredicateFormula &&
						((PredicateFormula) ff).predicate().arity() == 2) {
						PredicateFormula bpf = (PredicateFormula) ff;
						Predicate R = bpf.predicate();
						Var w2 = bpf.getVariable(0);
						Var x2 = bpf.getVariable(1);
						
						if (x2.equals(x) && w2.equals(w) && (R == Q)) {
							// Then, P is a guard-filter for the update:
							return P;
						}
					}
				}
			}
		}
		return null;
	}

	static private class Heap {
		Predicate candidate = null;
		int weight = 0;

		public void add(Predicate P) {
			if (candidate == null)
				candidate = P;
			else if (P == candidate)
				weight++;
			// else if (P != null)
			// System.err.println("Ignoring guard " + P.name());
		}

		public Predicate max() {
			// if (candidate != null) System.err.println("Guard " + candidate.name() + " wt = " + weight);
			return candidate;
		}
	}

	static private OptimizedUpdateRep optimize(Collection updateFormulae) {
		// System.err.println("Optimizing update formulae");

		Heap heap = new Heap();
		int numNullary = 0;
		int numUnary = 0;
		int numBinary = 0;

		LinkedList optimized = new LinkedList();

		for (Iterator updates = updateFormulae.iterator(); updates.hasNext(); ) {
			PredicateUpdateFormula updateFormula = (PredicateUpdateFormula) updates.next();
			if (updateFormula.predicateArity() == 0) {
				numNullary++;
				optimized.add(updateFormula);
			} else if (updateFormula.predicateArity() == 1) {
				numUnary++;
				Predicate guard = findGuard(updateFormula);
				heap.add(guard);
				optimized.add(new OptimizedUnary (updateFormula, guard));
			} else if (updateFormula.predicateArity() == 2) {
				numBinary++;
				Predicate lguard = findGuard(updateFormula, updateFormula.getVariable(0));
				Predicate rguard = findGuard(updateFormula, updateFormula.getVariable(1));
				heap.add(lguard);
				heap.add(rguard);
				optimized.add(new OptimizedBinary (updateFormula, lguard, rguard));
			} else {
				throw new IllegalArgumentException("Update formula for unsupported arity :"
					+ updateFormula);
			}
		}

		PredicateUpdateFormula nullaryUpdates[] = new PredicateUpdateFormula[numNullary+1];
		OptimizedUnary unaryUpdates[] = new OptimizedUnary[numUnary+1];
		OptimizedBinary binaryUpdates[] = new OptimizedBinary[numBinary+1];

		numNullary = 0;
		numUnary = 0;
		numBinary = 0;

		for (Iterator it = optimized.iterator(); it.hasNext(); ) {
			Object update = it.next();
			if (update instanceof PredicateUpdateFormula &&
				((PredicateUpdateFormula) update).predicateArity() == 0) {
				nullaryUpdates[numNullary++] = (PredicateUpdateFormula) update;
			} else if (update instanceof OptimizedUnary) {
				unaryUpdates[numUnary++] = (OptimizedUnary) update;
			} else {
				binaryUpdates[numBinary++] = (OptimizedBinary) update;
			}
		}

		return new OptimizedUpdateRep (nullaryUpdates, unaryUpdates, binaryUpdates,
									   heap.max()
									   );

	}

	private static class Ref {
		Collection updates;
		int hash;

		public Ref (Collection u, int h) {
			updates = u;
			hash = h;
		}

		public int hashCode() { return hash; }

		public boolean equals(Object other) {
			if (other instanceof Ref) {
				Ref that = (Ref) other;
				return (this.updates == that.updates);
			} else
				return false;
		}

	}

	private static Map optimized = HashMapFactory.make();

	public static void updatePredicates( NodePredTVS S, Collection updateFormulae, Assign assignment, int hash) {
		Ref r = new Ref (updateFormulae, hash);
		OptimizedUpdateRep optimizedUpdate = (OptimizedUpdateRep) optimized.get(r);
		if (optimizedUpdate == null) {
			optimizedUpdate = optimize(updateFormulae); 
			optimized.put(r, optimizedUpdate);
		}
		updatePredicates(S, optimizedUpdate, assignment);
	}
}
