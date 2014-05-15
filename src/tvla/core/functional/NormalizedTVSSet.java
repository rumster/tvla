package tvla.core.functional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tvla.analysis.AnalysisStatus;
import tvla.core.HighLevelTVS;
import tvla.core.TVSSet;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Pair;


public class NormalizedTVSSet extends TVSSet {
	private Set<HighLevelTVS> structures = HashSetFactory.make();
	private Map hashMap = HashMapFactory.make();
	
	/** The current number of states in this lattice.  */
	public int size() {
		return structures.size();
	}
	
	/** Return an iterator to the states this lattice represents - TVS objects. */
	public Iterator<HighLevelTVS> iterator() {
		return structures.iterator();
	}

	/** Applies the Join confluence operator.
	 * @return The difference between the updated lattice
	 * and the old lattice or null if there is no difference.
	 */
	public HighLevelTVS mergeWith(HighLevelTVS S) {
        if (S.getStructureGroup() != null) throw new RuntimeException("Join doesn't support structure group");
		AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.BLUR_TIME);
		S.blur();
		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.BLUR_TIME);

		NodePredTVS structure = (NodePredTVS) S;
		
		structure.normalize();

		Object sig = new Integer (  structure.signature() );
		if (NodePredTVS.normalizeStructure) {
			// S1 and S2 will have same signature iff they are isomorphic
			Object prev = hashMap.get(sig);
			if (prev == null) {
				hashMap.put(sig, structure);
				structures.add(structure);
				return structure;
			} else
				return null;
		} else {
			// Isomorphic structures will have same signature but 
			// structures with same signature may or may not be isomorphic.
			Collection bucket = (Collection) hashMap.get(sig);
			if (bucket == null) {
				bucket = new ArrayList();
				hashMap.put(sig, bucket);
			}

			for (Iterator structuresIt = bucket.iterator(); structuresIt.hasNext(); ) {
				HighLevelTVS oldStructure = (HighLevelTVS) structuresIt.next();
				if (structure.isomorphic (oldStructure))
					return null;
			}

			structures.add(structure);
			bucket.add(structure);

			return structure;
		}
	}
	
    public boolean mergeWith(HighLevelTVS S, Collection mergedWith) {
        if (S.getStructureGroup() != null) throw new RuntimeException("Join doesn't support structure group");
    	assert(mergedWith!= null && mergedWith.size() == 0);
    	boolean changed = false;
    	
    	
		AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.BLUR_TIME);
		S.blur();
		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.BLUR_TIME);

		NodePredTVS structure = (NodePredTVS) S;
		
		structure.normalize();

		Object sig = new Integer (  structure.signature() );
		if (NodePredTVS.normalizeStructure) {
			// S1 and S2 will have same signature iff they are isomorphic
			Object prev = hashMap.get(sig);
			if (prev == null) {
				hashMap.put(sig, structure);
				structures.add(structure);
				changed = true;
			} else {
				mergedWith.add(new Pair(structure,prev));
				changed = false;
			}
		
			assert(!(changed ^ mergedWith.isEmpty())); 

			return changed;
		} 
			
		// Isomorphic structures will have same signature but 
		// structures with same signature may or may not be isomorphic.
		Collection bucket = (Collection) hashMap.get(sig);
		if (bucket == null) {
			bucket = new ArrayList();
			hashMap.put(sig, bucket);
			changed = true;
			structures.add(structure);
			bucket.add(structure);
			
			assert (mergedWith.isEmpty());
			return true;

		}
		
		for (Iterator structuresIt = bucket.iterator(); structuresIt.hasNext(); ) {
			HighLevelTVS oldStructure = (HighLevelTVS) structuresIt.next();
			if (structure.isomorphic (oldStructure)) {
				mergedWith.add(new Pair(structure,oldStructure));
				changed = false;
				assert(mergedWith.size() == 1); 
				return false;
			}
		}
		
			
		assert(!changed);
		
		structures.add(structure);
		bucket.add(structure);

		changed = true;
		
		assert(mergedWith.isEmpty());
		
		return changed;
    }
}
