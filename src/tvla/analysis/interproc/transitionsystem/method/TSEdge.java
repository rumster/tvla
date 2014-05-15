/*
 * File: TSEdge.java 
 * Created on: 22/09/2004
 */

package tvla.analysis.interproc.transitionsystem.method;


/** A MethodTS edge in a specific MethodTS
 * Can be either an intrarocedural edge or an interprocedural call-site->returnsite edge.
 * @author maon
 */
public abstract class TSEdge implements CFGEdge {	
	protected final TSNode src;
	protected final TSNode dst;

	// Edge unique idnetification (within the scope of a MethodTS)
	protected final long id;
	
	public TSEdge(TSNode src, TSNode dst, long edgeId) { 
		super();
		this.src = src;
		this.dst = dst;
		this.id = edgeId;
	}
	
	public CFGNode getSource() {
		return src;
	}

	public CFGNode getDestination() {
		return dst;
	}
	
	public abstract String getLabel();
	
	public String toString() {
		return "TSEdge: " + 
			   src.toString() + "->" + dst.toString() + 
			   " label: " + getLabel();
	}
	
	public long getId() {
		return id;
	}
}
