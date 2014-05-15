/*
 * File: TSEdgeCallToReturn.java 
 * Created on: 22/09/2004
 */

package tvla.analysis.interproc.transitionsystem.method;


/** An edge corresponding to a function invocation statement in a specific MethodTS.
 * Connects the call-site to the return-site.
 * @author maon
 */
public class TSEdgeCallToReturn extends TSEdge implements CFGEdgeCallToReturn {
	private final String calleeName;
	public TSEdgeCallToReturn(
			TSNode src, TSNode dst, long edgeId, String calleeName) {
		super(src, dst, edgeId); //, transition);
		this.calleeName = calleeName;
	}

	public String getLabel() {
		return src.getTypeString() +  " " + calleeName;
	}

	public String title() {
		return getLabel();
	}
}
