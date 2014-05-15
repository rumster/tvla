package tvla.analysis.interproc.api.javaanalysis;

import tvla.analysis.interproc.api.tvlaadapter.TVLAAPI;
import tvla.api.ITVLATVS;
import tvla.api.ITVLATVSIndexIterator;


/**
 * The bridge connecting the DOMO engine with TVLA.
 * The bridge translates domoPrimitives (e.g., classes, methods, variables, VariableKeys, flowfunctions, factoid numerals etc.) 
 * to TVLA primitives (prediacates, actions, TVSs, and TVSSets) and vice versa
 * @author noam rinetzky
 */
public class TVLAJavaAnalysisTVSRepositry implements ITVSRepository {
	private TVLAAPI tvlaapi; 						// A reference to the (one and only) TVLA backend 
	
	/////////////////////////////////////////
	///  Initializtion of the JavaAdapter ///
	/////////////////////////////////////////
	
	public TVLAJavaAnalysisTVSRepositry(TVLAAPI tvlaAPI){
		this.tvlaapi = tvlaAPI;
    }
	
	//////////////////////////////////////////////////////////////////
	///  The abstract domain: A Facade to the underlying TVLAAPI   ///
	//////////////////////////////////////////////////////////////////

	
	public int addTVSToRepository(ITVLATVS tvs) {
		int id = tvlaapi.addTVSToRepository(tvs);
		
		return id;
	}

	/* (non-Javadoc)
	 * @see tvla.api.TVLAJavaAdapter#addTVSsToRepository(java.lang.String)
	 */
	public int[] loadTVSsIntoRepository(String tvsFile) {
		int[] initialIds = tvlaapi.loadTVSs(tvsFile);
		
		return initialIds;
	}
	
	public int[] join(int[] input, int[] inputToOutputMap) {
		return tvlaapi.join(input, inputToOutputMap);
	}
	
	public ITVLATVSIndexIterator iterator() {
		return tvlaapi.iterator();
	}
	
	public ITVLATVS getTVS(int indx) {
		ITVLATVS tvsapi = tvlaapi.getTVS(indx);
		return tvsapi;
	}
	
	public int getMaxIndex() {
		return tvlaapi.getMaxIndex();
	}
	
    public int getRepositorySize() {
        return tvlaapi.getRepositorySize();
    }

    public int getMappedIndex(ITVLATVS tvs) {
		return tvlaapi.getMappedIndex(tvs);
	}
}
