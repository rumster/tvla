package tvla.core.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import tvla.analysis.AnalysisStatus;
import tvla.core.HighLevelTVS;
import tvla.core.TVS;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashMapFactory;
import tvla.util.Pair;

/** A generic implementation of a TVSSet with hashing capabilities
 * that help reduce the number of isomorphism tests by avoiding
 * trivial cases where two structures are different (have different
 * signatures).
 * @since 6/9/2001 Adapted from AdvancedJoin.
 * @author Tal Lev-Ami.
 */
public class GenericHashTVSSet extends GenericTVSSet {
	/** Maps StructureSignature objects to collections of
	 * structures that share the same signature.
	 */
	public Map<Integer, Collection> joinHash = HashMapFactory.make(0);
	
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
		Integer signature = createSignature(candidate);
		Collection matching = joinHash.get(signature);
		if (matching != null) {
			for (Iterator structuresIt = matching.iterator(); structuresIt.hasNext(); ) {
				old = (HighLevelTVS) structuresIt.next();
				if (isomorphic())
					return null;					
			}
		}
		
		// no isomorphic structure was found
		addToHash(joinHash, signature, structure);
		structures.add(structure);
		return (HighLevelTVS) structure;
	}

	public boolean mergeWith(HighLevelTVS structure, Collection<Pair<HighLevelTVS, HighLevelTVS>> mergedWith) {
        if (structure.getStructureGroup() != null) throw new RuntimeException("Join doesn't support structure group");
		AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.BLUR_TIME);
		structure.blur();
		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.BLUR_TIME);

		cleanup();
		candidate = structure;
		Integer signature = createSignature(candidate);
		Collection matching = joinHash.get(signature);
		if (matching != null) {
			for (Iterator structuresIt = matching.iterator(); structuresIt.hasNext(); ) {
				old = (HighLevelTVS) structuresIt.next();
				if (isomorphic()) {
					mergedWith.add(new Pair<HighLevelTVS, HighLevelTVS>(structure,old));
					return false;
				}
			}
		}
		
		// no isomorphic structure was found
		addToHash(joinHash, signature, structure);
		structures.add(structure);
		return true;
	}
	
	
	
	public Integer createSignature(TVS structure) {
		int hashCode = 0;
		
        for (Predicate predicate : structure.getVocabulary().positiveArity()) {
			hashCode *= 3;
			hashCode += structure.numberSatisfy(predicate);
		}
		hashCode *= 31;
		hashCode += structure.nodes().size();
		
		return new Integer(hashCode);
	}

	protected Map<Integer, Collection> prepareHash(Collection structures) {
		Map<Integer, Collection> hash = HashMapFactory.make(structures.size());
		for (Iterator structuresIt = structures.iterator(); structuresIt.hasNext(); ) {
			TVS structure = (TVS) structuresIt.next();
			addToHash(hash, createSignature(structure), structure);
		}
		return hash;
	}

	protected void addToHash(Map<Integer, Collection> hash, Integer signature, TVS structure) {
		Collection<TVS> matching = hash.get(signature);
		if (matching == null) {
			matching = new ArrayList<TVS>();
			hash.put(signature, matching);
		}
		matching.add(structure);
	}
}
