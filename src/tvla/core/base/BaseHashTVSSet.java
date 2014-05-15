package tvla.core.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tvla.analysis.AnalysisStatus;
import tvla.core.Canonic;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.StoresCanonicMaps;
import tvla.core.TVS;
import tvla.core.generic.GenericBlur;
import tvla.core.generic.GenericTVSSet;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.HashMapFactory;
import tvla.util.Pair;

/** An implementation of a relational TVSSet that uses hashing to reduce the
 * number of isomorphism checks.
 * @todo optimize isomorphism test (efficient evaluation of predicate formulae).
 */
public class BaseHashTVSSet extends GenericTVSSet {
	public static int hashAccessAttempts;
	public static int hashColisions;
	public static int redundantHashColisions;

	/** Maps StructureSignature objects to collections of
	 * structures that share the same signature.
	 */
	public Map<Object, Collection<HighLevelTVS>> joinHash = HashMapFactory.make(10);
	
	/** Applies the Join confluence operator.
	 * @return The difference between the updated set
	 * and the old set or null if there is no difference.
	 */
	@Override
	public HighLevelTVS mergeWith(HighLevelTVS structure) {
        if (structure.getStructureGroup() != null) throw new RuntimeException("Join doesn't support structure group");
		AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.BLUR_TIME);
		structure.blur();
		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.BLUR_TIME);

		cleanup();
		boolean found = false;
		Set<Canonic> signature = getCanonicSetForBlurred(structure);
		Collection<HighLevelTVS> matching = joinHash.get(signature);
		++hashAccessAttempts; // STATISTICS
		if (matching != null) {
			candidate = structure;
			for (Iterator<HighLevelTVS> structuresIt = matching.iterator(); 
				 structuresIt.hasNext() && !found; ) {
				old = structuresIt.next();
				++hashColisions; // STATISTICS
				if (isomorphic()) // no need to break - the condition takes care of this
					found = true;
				else
					++redundantHashColisions; // STATISTICS
			}
		}
		if (found) {
			return null;
		} 
		else {
			addToHash(joinHash, signature, structure);
			structures.add(structure);
			return (HighLevelTVS) structure;
		}
	}

	@Override
	public boolean mergeWith(HighLevelTVS structure, Collection<Pair<HighLevelTVS, HighLevelTVS>> mergedWith) {
        if (structure.getStructureGroup() != null) throw new RuntimeException("Join doesn't support structure group");
		AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.BLUR_TIME);
		structure.blur();
		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.BLUR_TIME);

		cleanup();
		boolean found = false;
		Set<Canonic> signature = getCanonicSetForBlurred(structure);
		Collection<HighLevelTVS> matching = joinHash.get(signature);
		++hashAccessAttempts; // STATISTICS
		if (matching != null) {
			candidate = structure;
			for (Iterator<HighLevelTVS> structuresIt = matching.iterator(); 
				 structuresIt.hasNext() && !found; ) {
				old = structuresIt.next();
				++hashColisions; // STATISTICS
				if (isomorphic()) // no need to break - the condition takes care of this
					found = true;
				else
					++redundantHashColisions; // STATISTICS
			}
		}
		if (found) {
			mergedWith.add(new Pair<HighLevelTVS, HighLevelTVS>(structure, old));
			return false;
		} 
		else {
			addToHash(joinHash, signature, structure);
			structures.add(structure);
			return true;
		}
	}	
	
	protected Map<Object, Collection<HighLevelTVS>> prepareHash(Collection<HighLevelTVS> structures) {
		Map<Object, Collection<HighLevelTVS>> hash = HashMapFactory.make(structures.size());
		for (HighLevelTVS structure : structures) {
			addToHash(hash, createSignature(structure), structure);
		}
		return hash;
	}

	protected Object createSignature(HighLevelTVS structure) {
        return getCanonicSetForBlurred(structure);
    }

    protected void addToHash(Map<Object, Collection<HighLevelTVS>> hash, Object signature, HighLevelTVS structure) {
		Collection<HighLevelTVS> matching = hash.get(signature);
		if (matching == null) {
			matching = new ArrayList<HighLevelTVS>();
			hash.put(signature, matching);
		}
		matching.add(structure);
	}
	
    protected Set<Canonic> getCanonicSetForBlurred(TVS structure) {
        if (!(structure instanceof StoresCanonicMaps)) {
            return makeCanonicSet(structure);
        }
        
        Map<Canonic,Node> invCanonicMap = ((StoresCanonicMaps)structure).getInvCanonic();
        if (invCanonicMap == null) {
            return makeCanonicSet(structure);
        }
        
        Collection<Node> nodes = structure.nodes();
        int size = nodes.size();
        Set<Canonic> canonicNames = new HashSet<Canonic>(size);
        
        Set<Predicate> nullaryRel = structure.getVocabulary().nullaryRel();
        Canonic nullaryCanonic = new Canonic(nullaryRel.size());
        for (Predicate predicate : nullaryRel) {
            Kleene value = structure.eval(predicate);
            nullaryCanonic.add(value);
        }
        canonicNames.add(nullaryCanonic);
        canonicNames.addAll(invCanonicMap.keySet());
        return canonicNames;
    }
    
    protected Set<Canonic> makeCanonicSet(TVS structure) {
        Set<Predicate> nullaryRel = structure.getVocabulary().nullaryRel();
        Canonic nullaryCanonic = new Canonic(nullaryRel.size());
        for (Predicate predicate : nullaryRel) {
            Kleene value = structure.eval(predicate);
            nullaryCanonic.add(value);
        }
        Set<Canonic> canonicNames = GenericBlur.getInstance().makeCanonicSet(structure);
        canonicNames.add(nullaryCanonic);
        return canonicNames;
    }
}
