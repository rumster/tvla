package tvla.core.generic;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import tvla.analysis.AnalysisStatus;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVSSet;
import tvla.core.common.NodeTupleIterator;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Pair;

/** A TVSSet (j3) that attempts to detect pairs of structures such that
 * one is embedded in the other with respect to a subset of their
 * predicates (abstraction predicates).
 * @author Roman Manevich.
 * @since August 25, 2002 Initial creation.
 */
public class GenericEmbeddingTVSSet extends TVSSet {
	/** The structures stored in this set.
	 */
	protected Collection<HighLevelTVS> structures = HashSetFactory.make();

	/** A map from the nodes of rhs onto the nodes of lhs.
	 */
	protected Map<Node, Node> embeddingFunction;

	/** The structure into which rhs may be merged (embedding structure).
	 */
	protected HighLevelTVS lhs;
	
	/** The structure which may be merge into lhs (embedded structure).
	 */
	protected HighLevelTVS rhs;
	
	/** Applies the Join confluence operator.
	 * NOTE: the current algorithm implementation is not very efficient,
	 * since it performs a linear search looking for matching structures.
	 * Some mechanism for prunning the set should be found to avoid this.
	 * @return The difference between the updated set
	 * and the old set or null if there is no difference.
	 */
	public HighLevelTVS mergeWith(HighLevelTVS structure) {
        if (structure.getStructureGroup() != null) throw new RuntimeException("Join doesn't support structure group");
		AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.BLUR_TIME);
		EmbeddingBlur.defaultEmbeddingBlur.blur(structure);
		//structure.blur();
		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.BLUR_TIME);
		for (Iterator<HighLevelTVS> structureIter = structures.iterator(); structureIter.hasNext(); ) {
            HighLevelTVS savedStructure = structureIter.next();
			
			if (mergeCondition(savedStructure, structure)) {
				structureIter.remove();
				boolean change = mergeStructures() ||
								 (structure.nodes().size() < savedStructure.nodes().size());
				structures.add(lhs);
				HighLevelTVS result = (HighLevelTVS) (change ? lhs : null);
				return result;
			}
		}
		structures.add(structure);
		return structure;
	}

	
    public boolean mergeWith(HighLevelTVS structure, Collection<Pair<HighLevelTVS, HighLevelTVS>> mergedWith) {
        if (structure.getStructureGroup() != null) throw new RuntimeException("Join doesn't support structure group");
		AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.BLUR_TIME);
		EmbeddingBlur.defaultEmbeddingBlur.blur(structure);
		//structure.blur();
		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.BLUR_TIME);
		for (Iterator<HighLevelTVS> structureIter = structures.iterator(); structureIter.hasNext(); ) {
            HighLevelTVS savedStructure = structureIter.next();
			
			if (mergeCondition(savedStructure, structure)) {
				structureIter.remove();
				boolean change = mergeStructures() ||
								 (structure.nodes().size() < savedStructure.nodes().size());
				structures.add(lhs);
				mergedWith.add(new Pair<HighLevelTVS, HighLevelTVS>(structure,lhs));
				return change;
			}
		}
		structures.add(structure);
		// mergedWith.add(new Pair(structure,structure));
		return true;
    }
	
	
	/** Returns the current number of structures in this set.
	 */
	public int size() {
		return structures.size();
	}
	
	/** Merges rhs into lhs using the embedding mapping.
	 * NOTE: this is not a very efficient way to merge structures,
	 * since it does not exploit sparsity of predicates.
	 * @return true if any change was done to lhs.
	 */
	protected boolean mergeStructures() {	    
		// rhs is merged into lhs by modifying lhs.
		
		boolean change = false;
		
		// Merge the values of nullary predicates.
        for (Predicate predicate : lhs.getVocabulary().nullary()) {
			Kleene lhsVal = lhs.eval(predicate);
			Kleene rhsVal = rhs.eval(predicate);
			Kleene joinedValue = Kleene.join(lhsVal, rhsVal);
			lhs.update(predicate, joinedValue);
			change |= (lhsVal != rhsVal) && (lhsVal != Kleene.unknownKleene);
		}
		
		// Merge the values of predicates with arity > 0.
        for (Predicate predicate : lhs.getVocabulary().positiveArity()) {
			Iterator<? extends NodeTuple> tupleIter = NodeTupleIterator.createIterator(rhs.nodes(), predicate.arity());
			while (tupleIter.hasNext()) {
				NodeTuple rhsTuple = tupleIter.next();
				Node [] lhsNodesTmp = new Node[rhsTuple.size()];
				for (int index = 0; index < lhsNodesTmp.length; ++index) {
					lhsNodesTmp[index] = embeddingFunction.get(rhsTuple.get(index));
				}
				NodeTuple lhsTuple = NodeTuple.createTuple(lhsNodesTmp);
				
				Kleene lhsVal = lhs.eval(predicate, lhsTuple);
				Kleene rhsVal = rhs.eval(predicate, rhsTuple);
				Kleene joinedValue = Kleene.join(lhsVal, rhsVal);
				lhs.update(predicate, lhsTuple, joinedValue);
				change |= (lhsVal != rhsVal) && (lhsVal != Kleene.unknownKleene);
			}
		}
		embeddingFunction = null; // Release resources (opportunity for compile-time GC.).
		return change;
	}
	
	/** Tests whether one of the specified structures can be
	 * embedded in the other one, with respect to a subset of
	 * their predicates (abstraction predicates).
	 */
	protected boolean mergeCondition(HighLevelTVS lhs, HighLevelTVS rhs) {
	    assert lhs.getVocabulary() == rhs.getVocabulary();
	    
		// Arrange it so lhs is the structure with the smaller universe.
		// The algorithm will attempt to find an embedding function of
		// rhs into lhs.
		if (lhs.nodes().size() > rhs.nodes().size()) {
			// Swap lhs and rhs.
            HighLevelTVS tmp = lhs;
			lhs = rhs;
			rhs = tmp;
		}

		this.lhs = lhs;
		this.rhs = rhs;
		
		// Check embedding with respect to nullary predicates.
		for (Predicate predicate : lhs.getVocabulary().nullaryRel()) {
			Kleene lhsVal = lhs.eval(predicate);
			Kleene rhsVal = rhs.eval(predicate);
			if (lhsVal == Kleene.unknownKleene || 
				rhsVal == Kleene.unknownKleene ||
				lhsVal == rhsVal) {
				// Embedding condition is met for this binding.
			}
			else {
				// Embedding condition is violated by this binding.
				return false;
			}
		}
				
		embeddingFunction = HashMapFactory.make(rhs.nodes().size());
		// lhsNodes is used to check that the embedding function is surjective.
		Collection<Node> unmatchedLhsNodes = HashSetFactory.make(lhs.nodes());

		// Attempt to match a node in lhs for every node of rhs,
		// since the function has to be total.
		for (Iterator<Node> rhsNodeIter = rhs.nodes().iterator(); rhsNodeIter.hasNext(); ) {
			Node rhsNode = rhsNodeIter.next();
			Node matchedNode = null;
			// Attempt to match any node of lhs.
			for (Iterator<Node> lhsNodeIter = lhs.nodes().iterator();
				 lhsNodeIter.hasNext(); ) {
				Node lhsNode = lhsNodeIter.next();
				
				// Check that the embedding condition is met for the
				// pair of nodes rhsNode and lhsNode, with respect to 
				// the unary abstraction predicates.
		        for (Predicate predicate : lhs.getVocabulary().unaryRel()) {
					Kleene lhsVal = lhs.eval(predicate, lhsNode);
					Kleene rhsVal = rhs.eval(predicate, rhsNode);
					if (lhsVal == Kleene.unknownKleene ||
						rhsVal == Kleene.unknownKleene ||
						lhsVal == rhsVal) {
						// The embedding condition is met for this binding,
						// and lhsNode is still a potential match.
					}
					else {
						// The embedding condition is violated by this binding,
						// and another matching node has to be found.
						lhsNode = null;
						break;
					}
				}
				// A match was found.
				if (lhsNode != null) {
					matchedNode = lhsNode;
					// Prefer unmatched nodes of lhs, because the embedding function has
					// to be surjective.
					if (unmatchedLhsNodes.contains(lhsNode))
						break;
					// Otherwise keep looking for a matching node from the 
					// unmatched node list.
				}
			}
			if (matchedNode != null) { // found a match for rhsNode
				embeddingFunction.put(rhsNode, matchedNode);
				unmatchedLhsNodes.remove(matchedNode);
			}
			else {
				return false;
			}
		}
		// The embedding function is surjective and total.
		if (unmatchedLhsNodes.isEmpty())
			return true;
		return false;
	}

	/** Returns an iterator to the structures contained in this set 
	 */
	public Iterator<HighLevelTVS> iterator() {
		return structures.iterator();
	}
}
