package tvla.core.functional;

import java.util.Iterator;

import tvla.core.HighLevelTVS;
import tvla.core.TVSFactory;
import tvla.core.TVSSet;
import tvla.core.base.BaseHighLevelTVS;
import tvla.core.generic.GenericSingleTVSSet;
import tvla.predicates.DynamicVocabulary;
import tvla.util.Logger;
import tvla.util.ProgramProperties;

public class NodePredTVSFactory extends TVSFactory {
	public NodePredTVSFactory() {
		super();
		opt = ProgramProperties.getBooleanProperty("tvla.flik.opt", true);
	}

	public HighLevelTVS makeEmptyTVS () {
		return new NodePredTVS();
	}

    /** Returns a new empty structure.
     */
    public HighLevelTVS makeEmptyTVS (DynamicVocabulary vocabulary) {
        throw new UnsupportedOperationException();
    }
	
	private boolean opt = true;

	public TVSSet makeEmptySet () {
		switch (super.joinMethod) {
		case JOIN_CANONIC:
			return new PartRelationalTVSSet();
		case JOIN_INDEPENDENT_ATTRIBUTES: 
			return new GenericSingleTVSSet();
		case JOIN_RELATIONAL:
			// NOTE: OptTVSSet can be used only if the TVS implementation can
			// implement equals() to do an isomorphism check. This is currently
			// turned off.
			// if (NodePredTVS.renumber && opt)
		   // 	return new OptTVSSet();
			// else
				return new NormalizedTVSSet();
		default:
			throw new RuntimeException("Requested join not supported by functional TVS.");
		}
	}

	public void init() {
		NodePredTVS.init();
	}
	
	private NPSpaceCounter maxSpace =  new NPSpaceCounter();
	// private NPSpaceCounter maxActiveSpace =  new NPSpaceCounter();

	/** Collect runtime space statistics.
	 * @param structureIter An iterator over the set of all the structure
	 * that exist during one point of time in the analysis.
	 * @author Ramalingam.
	 * @since 4.1.2002 Initial creation.
	 */
	public void collectTVSSizeInfo(Iterator structureIter) {
		NPSpaceCounter spaceStats = new NPSpaceCounter();
		spaceStats.startNewVisit();
		for (Iterator it = structureIter; it.hasNext();) {
			NodePredTVS tvs = (NodePredTVS) it.next();
			if (! spaceStats.visited(tvs)) {
				spaceStats.markVisited(tvs);
				spaceStats.numStructures++;
				tvs.computeSpace(spaceStats);
			}
		}
		if (spaceStats.size() > maxSpace.size())
			maxSpace = spaceStats;

		/*
		NPSpaceCounter activeStats = new NPSpaceCounter();
		activeStats.startNewVisit();
		for (Iterator it = activeStructures.iterator(); it.hasNext();) {
		NodePredTVS tvs = (NodePredTVS) it.next();
		if (!activeStats.visited(tvs)) {
		activeStats.markVisited(tvs);
		activeStats.numStructures++;
		tvs.computeSpace(activeStats);
		}
		}
		if (activeStats.size() > maxActiveSpace.size())
		maxActiveSpace = activeStats;
		*/

	}

	protected void dumpStatistics() {
		Logger.println("Number of unique fliks generated: " +
					   PackedIntKleeneMap.uniqueGenerated());
		FourIntLeaf.printStatistics();
		maxSpace.printStatistics();
		// long javaMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		// Logger.println("Currently used memory : " + javaMem);
		// System.gc();
		// Logger.println("Currently used memory [after gc] : " + javaMem);
		// System.gc();
		// Logger.println("Currently used memory [after 1 more gc] : " + javaMem);
	}
}
