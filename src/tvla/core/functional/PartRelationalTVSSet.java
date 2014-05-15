package tvla.core.functional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tvla.core.HighLevelTVS;
import tvla.core.TVSSet;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Pair;

public class PartRelationalTVSSet extends TVSSet {
	private Set<HighLevelTVS> structures = HashSetFactory.make();
	private Map hashMap = HashMapFactory.make();
	
	/** The current number of states in this lattice.
	 */
	public int size() {
		return structures.size();
	}
	
	/** Return an iterator to the states this lattice 
	 * represents - TVS objects.
	 */
	public Iterator<HighLevelTVS> iterator() {
		return structures.iterator();
	}

	// possibleMatches(S): return a subset of "structures" guaranteed
	// to include any structures partially isomorphic to S.
	protected Collection possibleMatches(HighLevelTVS S) {
		Object sig = new Integer ( ((NodePredTVS) S).partialSignature() );
		Collection bucket = (Collection) hashMap.get(sig);
		if (bucket != null) return bucket;
		bucket = new ArrayList();
		hashMap.put(sig, bucket);
		return bucket;
	}

	/** Applies the Join confluence operator.
	 * @return The difference between the updated lattice
	 * and the old lattice or null if there is no difference.
	 */
	public HighLevelTVS mergeWith(HighLevelTVS S) {
        if (S.getStructureGroup() != null) throw new RuntimeException("Join doesn't support structure group");
		NodePredTVS structure = (NodePredTVS) S;

		structure.partNormalize();

		Collection bucket = possibleMatches(structure);

		for (Iterator structuresIt = bucket.iterator(); structuresIt.hasNext(); ) {
			NodePredTVS oldStructure = (NodePredTVS) structuresIt.next();
			if (structure.partiallyIsomorphic (oldStructure)) {
				// if (oldStructure.subsumes(oldStructure)) return null;
				if (oldStructure.mergeWith(structure)) {
					// oldStructure.normalize();
			   	return oldStructure;
				} else
					return null; // subsumed by existing structure
			}
		}

		structure.normalize();
	    structures.add(structure);
		bucket.add(structure);

		return structure;
	}
	
	/** Applies the Join confluence operator.
	 * @return The difference between the updated lattice
	 * and true if adding the the new value changed the set.
	 * Returns in mergedWith the strcutre which subsumes S.
	 */
	public boolean mergeWith(HighLevelTVS S, Collection mergedWith) {
        if (S.getStructureGroup() != null) throw new RuntimeException("Join doesn't support structure group");
    	assert(mergedWith!= null && mergedWith.size() == 0);
    	
    	boolean changed = false;
		NodePredTVS structure = (NodePredTVS) S;

		structure.partNormalize();

		Collection bucket = possibleMatches(structure);

		for (Iterator structuresIt = bucket.iterator(); structuresIt.hasNext(); ) {
			NodePredTVS oldStructure = (NodePredTVS) structuresIt.next();
			if (structure.partiallyIsomorphic (oldStructure)) {
				// if (oldStructure.subsumes(oldStructure)) return null;
				mergedWith.add(new Pair(structure,oldStructure));
				if (oldStructure.mergeWith(structure)) {
					// oldStructure.normalize();
					return true;
				} else
					return false; // subsumed by existing structure
			}
		}

		structure.normalize();
	    structures.add(structure);
		bucket.add(structure);

		return true;
	}

}
