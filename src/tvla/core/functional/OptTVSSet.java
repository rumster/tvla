package tvla.core.functional;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import tvla.core.HighLevelTVS;
import tvla.core.TVSSet;
import tvla.util.HashSetFactory;


/** 
 * An implementation of TVSSet that is usable for TVS implementations
 * that override hashCode() and equals() methods so that two TVS
 * are "equal" iff they are isomorphic. In this case, any implementation
 * of java.util.Set is directly usable as a TVSSet with a simple wrapper.
 * The implementation below uses a HashSet.
 * NOTE: Currently there are some problems using this TVSSet implementation
 * because the current TVLA implementation uses "Sets" in places where
 * collections that don't check for equality might be more appropriate.
 * Hence, overriding the equals() method in a TVS implementation to do
 * isomorphism checking can lead to performance problems, at the least.
 * @author G. Ramalingam
 */

public class OptTVSSet extends TVSSet {
	private Set<HighLevelTVS> structures = HashSetFactory.make();
	
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
		NodePredTVS structure = (NodePredTVS) S;

		structure.normalize();
		if (structures.add(structure))
			return structure;
		else
			return null; // set already contains an isomorphic structure
   }

    public boolean mergeWith(HighLevelTVS S, Collection mergedWith) {
    	throw new UnsupportedOperationException() ;
    }
}
