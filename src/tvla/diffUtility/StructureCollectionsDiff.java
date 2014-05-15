package tvla.diffUtility;

import java.util.Collection;
import java.util.Iterator;

import tvla.core.HighLevelTVS;
import tvla.core.TVSSet;

/** Computes the mutual difference between two TVSSets.
 * @author Roman Manevich.
 * @since tvla-0.91 (December 19 2000) Initial creation.
 */
public class StructureCollectionsDiff {
	public void diff(TVSSet refStructures, TVSSet newStructures,
					 Collection diffRef, Collection diffNew) {
		Iterator iter = null;
		iter = newStructures.iterator();
		while (iter.hasNext()) {
			HighLevelTVS comparedStructure = (HighLevelTVS) iter.next();
			HighLevelTVS delta = refStructures.mergeWith(comparedStructure);
			if (delta != null)
				diffNew.add(delta);
		}

		iter = refStructures.iterator();
		while (iter.hasNext()) {
			HighLevelTVS comparedStructure = (HighLevelTVS) iter.next();
			// ignore structures that were not originaly in the reference set 
			// but are now there after they were merged
			if (diffNew.contains(comparedStructure))
				continue;
			HighLevelTVS delta = newStructures.mergeWith(comparedStructure);
			if (delta != null)
				diffRef.add(delta);
		}
	}
}