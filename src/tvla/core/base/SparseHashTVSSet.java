package tvla.core.base;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import tvla.analysis.AnalysisStatus;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignKleene;
import tvla.formulae.PredicateFormula;
import tvla.formulae.Var;
import tvla.predicates.Predicate;
import tvla.util.HashSetFactory;

/** A set of structures containing optimizations for very sparse structures.
 */
public class SparseHashTVSSet extends BaseHashTVSSet {
	protected static Set nullaryNonZeroPredicates = HashSetFactory.make();
	protected static Set unaryNonZeroPredicates   = HashSetFactory.make();
	protected static Set binaryNonZeroPredicates  = HashSetFactory.make();

	public static void reset() {
		nullaryNonZeroPredicates = HashSetFactory.make();
		unaryNonZeroPredicates   = HashSetFactory.make();
		binaryNonZeroPredicates  = HashSetFactory.make();
	}
	
	/** Applies the Join confluence operator.
	 * @return The difference between the updated set
	 * and the old set or null if there is no difference.
	 */
	public HighLevelTVS mergeWith(HighLevelTVS structure) {
        if (structure.getStructureGroup() != null) throw new RuntimeException("Join doesn't support structure group");
		AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.BLUR_TIME);
		structure.blur();
		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.BLUR_TIME);

		cleanup();
		Object signature = createSignature(structure);
		Collection matching = (Collection) joinHash.get(signature);
		++hashAccessAttempts; // STATISTICS
		if (matching != null) {
			candidate = structure;

			for (Iterator structuresIt = matching.iterator(); structuresIt.hasNext(); ) {
				old = (HighLevelTVS) structuresIt.next();
				++hashColisions; // STATISTICS
				if (isomorphic())
					return null;
			}
		}
		
		// no isomorphic structure was found
		addToHash(joinHash, signature, structure);
		structures.add(structure);
		return (HighLevelTVS) structure;
	}

	public boolean mergeWith(HighLevelTVS S, Collection mergedWith) {
		throw new UnsupportedOperationException() ;
	}
	
	
	public boolean isomorphic() {
		if (old.nodes().size() != candidate.nodes().size()) {
			return false;
		}

		nullaryNonZeroPredicates = HashSetFactory.make();
		unaryNonZeroPredicates   = HashSetFactory.make();
		binaryNonZeroPredicates  = HashSetFactory.make();
		updatePredicateSets(old);
		updatePredicateSets(candidate);
		
		// Make sure that all the nullary predicates are the same.
		for (Iterator oldPredIt = nullaryNonZeroPredicates.iterator(); oldPredIt.hasNext(); ) {
			Predicate predicate = (Predicate) oldPredIt.next();
			if (!candidate.eval(predicate).equals(old.eval(predicate)))
				return false;
		}

		// Make sure that each node has a matching node. 
		//  And that all unary predicates match on it.
		for (Iterator oldIt = old.nodes().iterator(); oldIt.hasNext(); ) {
			Node oldNode = (Node) oldIt.next();
			Node candidateNode = getMatchingNode(oldNode);

			if (candidateNode == null) {
				return false;
			}
			for (Iterator predIt = unaryNonZeroPredicates.iterator(); predIt.hasNext(); ) {
				Predicate predicate = (Predicate) predIt.next();
				if (!candidate.eval(predicate,
									candidateNode).equals(old.eval(predicate, oldNode)))
					return false;		
			}
		}
		
		Var v1 = new Var("v1");
		Var v2 = new Var("v2");

		/* Make sure that the binary predicates match on each of the node pairs. */
		for (Iterator bpIt = binaryNonZeroPredicates.iterator(); bpIt.hasNext(); ) {
			Predicate predicate = (Predicate) bpIt.next();
			if (old.numberSatisfy(predicate) != candidate.numberSatisfy(predicate))
				return false;

			Iterator oldIt = old.evalFormula(new PredicateFormula(predicate, 
																  v1,
																  v2),
											 Assign.EMPTY);
			while (oldIt.hasNext()) {
				AssignKleene oldAssign = 
										(AssignKleene) oldIt.next();
				Node oldLeftNode = oldAssign.get(v1);
				Node oldRightNode = oldAssign.get(v2);
				Node candidateLeftNode = getMatchingNode(oldLeftNode);
				Node candidateRightNode = getMatchingNode(oldRightNode);
				if (!candidate.eval(predicate, candidateLeftNode, 
									candidateRightNode).equals(oldAssign.kleene))
					return false;
			}
		}
		return true;
	}

	/** Creates a hash code used for efficiently joining the structure into a set
	 * of structures by avoiding many of the isomorphism tests.
	 */
	public Object createSignature(HighLevelTVS structure) {
		nullaryNonZeroPredicates = HashSetFactory.make();
		unaryNonZeroPredicates   = HashSetFactory.make();
		binaryNonZeroPredicates  = HashSetFactory.make();
		updatePredicateSets(structure);
		int hashCode = 0;

		TreeSet sortedBinary = new TreeSet();
		for (Iterator i = binaryNonZeroPredicates.iterator(); i.hasNext(); ) {
			Predicate binary = (Predicate) i.next();
			sortedBinary.add(binary);
		}
		for (Iterator i = sortedBinary.iterator(); i.hasNext(); ) {
			Predicate binary = (Predicate) i.next();
			hashCode *= 3;
			hashCode += structure.numberSatisfy(binary);
		}
		hashCode *= 31;
		TreeSet sortedUnary = new TreeSet();
		for (Iterator i = unaryNonZeroPredicates.iterator(); i.hasNext(); ) {
			Predicate unary = (Predicate) i.next();
			sortedUnary.add(unary);
		}
		for (Iterator i = sortedUnary.iterator(); i.hasNext(); ) {
			Predicate unary = (Predicate) i.next();
			hashCode *= 3;
			hashCode += structure.numberSatisfy(unary);
		}
		hashCode *= 31;
		hashCode += structure.nodes().size();
		
		return hashCode;
	}
	
	protected void updatePredicateSets(TVS structure) {
		BaseTVS base = (BaseTVS) structure;
		for (Iterator i = base.predicates.keySet().iterator(); i.hasNext(); ) {
			Predicate predicate = (Predicate) i.next();
			switch (predicate.arity()) {
			case 0:
				nullaryNonZeroPredicates.add(predicate);
				break;
			case 1:
				unaryNonZeroPredicates.add(predicate);
				break;
			case 2:
				binaryNonZeroPredicates.add(predicate);
				break;
			default:
				throw new RuntimeException("Encountered a predicate with" +
					" unsupported arity: " + predicate);
			}
		}
	}
}
