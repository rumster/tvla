/*
 * File: MethodSummary.java 
 * Created on: 22/09/2004
 */

package tvla.analysis.interproc.transitionsystem.method;

import tvla.util.graph.ExplodedFlowGraph;

/** The summarized transfer function of a specific method.
 * Contains a map from input strucutre-ids to output strucutre-ids.
 *  
 * @author maon
 */
public final class MethodSummary {
	final ExplodedFlowGraph tvsFlowGraph;
	/**
	 * 
	 */
	public MethodSummary(ExplodedFlowGraph tvsFlowGraph) {
		super();
		this.tvsFlowGraph = tvsFlowGraph;
	}

}
