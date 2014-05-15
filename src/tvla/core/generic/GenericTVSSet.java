package tvla.core.generic;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import tvla.analysis.AnalysisStatus;
import tvla.core.Canonic;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.StoresCanonicMaps;
import tvla.core.TVSSet;
import tvla.core.common.NodeTupleIterator;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.MapInverter;
import tvla.util.Pair;

/** A generic implementation of a TVSSet.
 * @since 6/9/2001 Adapted from NaiveJoin.
 * @author Tal Lev-Ami.
 */
public class GenericTVSSet extends TVSSet {
	protected Collection<HighLevelTVS> structures = HashSetFactory.make();
	//protected Collection structures = new TreeSet();
	protected Map<Node, Canonic> oldCanonic;
	protected Map candidateInvCanonic;
	protected HighLevelTVS candidate;
	protected HighLevelTVS old;
	
	// Used to create canonic maps.
	protected static GenericBlur genericBlur = new GenericBlur();
	
	// A reusable map.
	protected static Map<Canonic, Collection<Node>> dummy = HashMapFactory.make();
	
	public static void reset() {
		genericBlur = new GenericBlur();
	}
	
	public Collection<HighLevelTVS> getStructures() {
		return structures;
	}
	
	public Collection<HighLevelTVS> checkSelfIsomorphism() {
		HighLevelTVS pivot;
		Iterator<HighLevelTVS> it, it2;
		Collection<HighLevelTVS> col = tvla.util.HashSetFactory.make();
		for (it = structures.iterator(); it.hasNext();) {
			pivot = it.next();
			cleanup();
			candidate = pivot;
			for (it2 = structures.iterator(); it2.hasNext();) {
				old = it2.next();
				if (old == pivot)
					continue;
				if (oldContainsCandidate()) {
					col.add(pivot);
					break;
				}
			}
		}
		return col;
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
		candidate = structure;
		for (Iterator<HighLevelTVS> structuresIt = structures.iterator(); 
			 structuresIt.hasNext(); ) {
			old = structuresIt.next();
			if (isomorphic())
				return null;
		}
		structures.add(candidate);
		return (HighLevelTVS) candidate;
	}
	

	public boolean mergeWith(HighLevelTVS structure, Collection<Pair<HighLevelTVS, HighLevelTVS>> mergureMap) {
        if (structure.getStructureGroup() != null) throw new RuntimeException("Join doesn't support structure group");
		AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.BLUR_TIME);
		structure.blur();
		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.BLUR_TIME);

		cleanup();
		candidate = structure;
		for (Iterator<HighLevelTVS> structuresIt = structures.iterator(); 
			 structuresIt.hasNext(); ) {
			old = structuresIt.next();
			if (isomorphic()) {
				mergureMap.add(new Pair<HighLevelTVS, HighLevelTVS>(candidate,old));
				return false;
			}
		}
		structures.add(candidate);
		return true;
    }
	
	
	/** The current number of states in this set.
	 */
	public int size() {
		return structures.size();
	}
	
	/** Return an iterator to the states this set 
	 * represents - TVS objects.
	 */
	public Iterator<HighLevelTVS> iterator() {
		return structures.iterator();
	}
	
	/** Checks that the two structures identify on all the predicates.
	 */
	protected boolean isomorphic() {
        assert old.getVocabulary() == candidate.getVocabulary();
		if (old.nodes().size() != candidate.nodes().size())
			return false;

		// Make sure that all the nullary predicates are the same.
		// This is just an optimization to avoid unnecessarily creating the canonic maps.
        for (Predicate predicate : old.getVocabulary().nullary()) {
			if (candidate.eval(predicate) != old.eval(predicate))
				return false;
		}

        if (candidateInvCanonic == null && candidate instanceof StoresCanonicMaps) {
            candidateInvCanonic = ((StoresCanonicMaps) candidate).getInvCanonic();
        }

		if (candidateInvCanonic == null) {
			Map<Node, Canonic> candidateCanonicName = HashMapFactory.make(candidate.nodes().size());
			genericBlur.makeCanonicMaps(candidate, candidateCanonicName, dummy);
			candidateInvCanonic = HashMapFactory.make(candidate.nodes().size());
			MapInverter.invertMap(candidateCanonicName, candidateInvCanonic);
			candidateCanonicName = null;
			dummy.clear();
		}
		
		oldCanonic = null;
        if (old instanceof StoresCanonicMaps) {
            oldCanonic = ((StoresCanonicMaps) old).getCanonic();
        }
        if (oldCanonic == null) {
            oldCanonic = HashMapFactory.make(old.nodes().size());
    		genericBlur.makeCanonicMaps(old, oldCanonic, dummy);
    		dummy.clear();
        }
        
		Map<Node, Node> match = HashMapFactory.make();
		// Make sure that each node has a matching node
		for (Node oldNode : old.nodes()) {
			Node candidateNode = getMatchingNode(oldNode);
			if (candidateNode == null)
				return false;
			match.put(oldNode, candidateNode);
		}
		
        for (Predicate predicate : old.getVocabulary().positiveArity()) {
		    if (old.numberSatisfy(predicate) != candidate.numberSatisfy(predicate)) {
		        return false;
		    }
		    Iterator<Entry<NodeTuple, Kleene>> tupleIter = old.iterator(predicate);
			Node [] candidateTupleTmp = new Node[predicate.arity()];
			
			while (tupleIter.hasNext()) {
			    Entry<NodeTuple, Kleene> entry = tupleIter.next();
				NodeTuple oldTuple = entry.getKey();
				Kleene oldValue = entry.getValue();
			
				// map the tuple to the corresponding tuple in the candidate structure
				for (int index = 0; index < candidateTupleTmp.length; ++index) {
					candidateTupleTmp[index] = match.get( oldTuple.get(index) );
				}
				NodeTuple candidateTuple = NodeTuple.createTuple(candidateTupleTmp);
				if (oldValue != candidate.eval(predicate, candidateTuple))
					return false;
			}
		}

		return true;
	}
	
	protected boolean oldContainsCandidate() {
		if (old.nodes().size() != candidate.nodes().size())
			return false;

		// Make sure that all the nullary predicates are the same.
		// This is just an optimization to avoid unnecessarily creating the canonic maps.
        for (Predicate predicate : old.getVocabulary().nullary()) {
			Kleene oldValue = old.eval(predicate);
			if (Kleene.join(candidate.eval(predicate),oldValue) != oldValue)
				return false;
		}

		if (candidateInvCanonic == null) {
			Map<Node, Canonic> candidateCanonicName = HashMapFactory.make(candidate.nodes().size());
			genericBlur.makeCanonicMaps(candidate, candidateCanonicName, dummy);
			candidateInvCanonic = HashMapFactory.make(candidate.nodes().size());
			MapInverter.invertMap(candidateCanonicName, candidateInvCanonic);
			candidateCanonicName = null;
			dummy.clear();
		}
		
		oldCanonic = HashMapFactory.make(old.nodes().size());
		genericBlur.makeCanonicMaps(old, oldCanonic, dummy);
		dummy.clear();
		
		// Make sure that each node has a matching node
		for (Iterator<Node> oldIt = old.nodes().iterator(); oldIt.hasNext(); ) {
			Node oldNode = oldIt.next();
			Node candidateNode = getMatchingNode(oldNode);
			if (candidateNode == null)
				return false;
		}
		
		for (Predicate predicate : old.getVocabulary().positiveArity()) {
			Iterator<? extends NodeTuple> tupleIter = NodeTupleIterator.createIterator(old.nodes(), predicate.arity());
			Node [] candidateTupleTmp = new Node[predicate.arity()];
			
			while (tupleIter.hasNext()) {
				NodeTuple oldTuple = (NodeTuple) tupleIter.next();
			
				// map the tuple to the corresponding tuple in the candidate structure
				for (int index = 0; index < candidateTupleTmp.length; ++index) {
					candidateTupleTmp[index] = getMatchingNode( oldTuple.get(index) );
				}
				NodeTuple candidateTuple = NodeTuple.createTuple(candidateTupleTmp);
				Kleene oldValue = old.eval(predicate, oldTuple);
				if (Kleene.join(candidate.eval(predicate, candidateTuple),oldValue) != oldValue)
					return false;

			}
		}

		return true;
	}

	protected Node getMatchingNode(Node oldNode) {
		Canonic canonicName = oldCanonic.get(oldNode);
		Node candidateNode = (Node) candidateInvCanonic.get(canonicName);
		return candidateNode;
	}

	protected void cleanup() {
		oldCanonic			= null;
		candidateInvCanonic	= null;
		candidate			= null;
		old					= null;
	}
	
	public String toString() {
	    return structures.toString();
	}
}
