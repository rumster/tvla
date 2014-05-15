package tvla.core.base;

import java.util.Collection;
import java.util.Iterator;

import tvla.analysis.AnalysisStatus;
import tvla.core.Canonic;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.StoresCanonicMaps;
import tvla.core.generic.GenericTVSSet;
import tvla.util.Pair;

/** A base class for a set of structures with the Base representation.
 */
public class BaseTVSSet extends GenericTVSSet {
	/** Applies the Join confluence operator.
	 * @return The difference between the updated set
	 * and the old set or null if there is no difference.
	 */
	public HighLevelTVS mergeWith (HighLevelTVS structure) {
        if (structure.getStructureGroup() != null) throw new RuntimeException("Join doesn't support structure group");
		AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.BLUR_TIME);
		structure.blur();
		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.BLUR_TIME);

		cleanup();
		boolean found = false;
		candidate = structure;

		for (Iterator structuresIt = structures.iterator(); 
			 structuresIt.hasNext(); ) {
			old = (HighLevelTVS) structuresIt.next();
			if (isomorphic())
				return null;
		}
		
		// no isomorphic structure was found
		structures.add(structure);
		return (HighLevelTVS) structure;
	}

	protected Node getMatchingNode(Node oldNode) {
		Canonic canonicName = (Canonic)((StoresCanonicMaps)old).getCanonic().get(oldNode);
		Node candidateNode = (Node)((StoresCanonicMaps)candidate).getInvCanonic().get(canonicName);
		return candidateNode;
	}
	
	public boolean mergeWith(HighLevelTVS S, Collection<Pair<HighLevelTVS, HighLevelTVS>> mergedWith) {
		throw new UnsupportedOperationException() ;
	}

}