/*
 * File: MethodTS.java 
 * Created on: 22/09/2004
 */

package tvla.analysis.interproc.transitionsystem.method;

import java.util.Collection;
import java.util.Iterator;

import tvla.util.graph.FlowGraph;
import tvla.util.graph.Graph;
import tvla.util.graph.GraphFactory;
import tvla.util.graph.GraphUtils;


/** A *functional* TS of a specific method:
 * Contains only edges that corresponds to intraprocedural sttements + 
 * edges from call-site to return-site.
 * A similar construction is used in
 * (i)  [SP81]: edges of type E^0 and E^1 in the functional approach. 
 * (ii) [RHS95]: interprocedural-edges and call-to-return edges.
 *  
 * TODO add an analysisStarted flag like in InterProcTS
 * 
 * @author maon
 */
public final class CFG {
	private static boolean xdebug = false;
	private static java.io.PrintStream out = System.out;
	
	private final MethodTS ts;
	private CFGNode entrySite;
	private CFGNode exitSite;
	private final FlowGraph cfg;
	
	private boolean printAllNodes;	
	

	/**
	 * 
	 */
	public CFG(MethodTS ts, int numOfStmts) {
		super();
		assert(0 < numOfStmts);
		
		this.cfg = GraphFactory.newFlowGraph(numOfStmts);
		this.ts = ts;
		this.printAllNodes = false;	
	}

	//////////////////////////////////////////////////
	///   Construcitng the actions-annotated CFG   ///
    ///      with empty TVS sets at every node     ///   
	//////////////////////////////////////////////////

	public void setEntrySite(CFGNode entryNode) {
		assert(this.entrySite == null);
		assert(!cfg.containsNode(entryNode));
		assert(entryNode != this.exitSite);
		this.cfg.addNode(entryNode);
		this.cfg.addToSources(entryNode);
		this.entrySite = entryNode; 
	}

	public void setExitSite(CFGNode exitNode) {
		assert(this.exitSite == null);
		assert(!cfg.containsNode(exitNode));
		assert(exitNode != this.entrySite);
		this.cfg.addNode(exitNode);
		this.cfg.addToSinks(exitNode);
		this.exitSite = exitNode;	
	}

	
	public void addNode(CFGNode node) {
		if (xdebug)
			out.println("CFG.addNode : " + node.getLabel());
		
		assert(!cfg.containsNode(node));
		
		cfg.addNode(node);
	}

	
	public void addIntraEdge(CFGNode src , CFGNode dst, TSEdge edge) {
		assert(src.equals(edge.getSource()));
		assert(dst.equals(edge.getDestination()));
		assert(edge instanceof CFGEdgeIntra);

		if (xdebug) {
			String actionLabel = edge.getLabel();
			out.println("addIntraEdge: " + src.getLabel() + 
					     "->" + dst.getLabel() + ": " + actionLabel);		
		}

		// Bad assertion. can happen if there is an uninterpreted condition 
		// with an empty body
		// FIXME change to multi graph?
		//verifyEdgeDoesNotExist(src, dst);
		//cfg.addEdge(src,dst, edge);
		
		// Partial fix by adding a check that the new edge has the same action (and thus can be ommited)

		if (!cfg.containsNode(src) || !cfg.containsNode(dst) || !cfg.containsEdge(src,dst)) {	
			// Added fter reinsertion!
			if (!cfg.containsNode(src))
				cfg.addNode(src);
			if (!cfg.containsNode(dst))
				cfg.addNode(dst);
			// Added fter reinsertion!
			cfg.addEdge(src,dst, edge);
		}
		else {
			Graph.Edge existingEdge = cfg.getEdge(src,dst);

			TSEdgeIntra tsExistingEdge = (TSEdgeIntra) existingEdge.getLabel();
			TSEdgeIntra newEdge = (TSEdgeIntra) edge;
			
			if (newEdge.getActionId().equals(tsExistingEdge.getActionId())) {
				if (xdebug) {
					//String actionLabel = edge.getLabel();
					out.println("           addIntraEdge: omitting adding double edges with the same action (" + newEdge.getActionId() + ")");		
				}			
			}
			else {
				throw new Error("Attempt to get multiple edges with different actions");
			}
		}
	}
	
	public void addCallToRetrunEdge(
			CFGNode src, CFGNode dst, TSEdge edge) { 

		assert(src.equals(edge.getSource()));
		assert(dst.equals(edge.getDestination()));
		assert(edge instanceof TSEdgeCallToReturn);

		verifyEdgeDoesNotExist(src, dst);	
		
		if (xdebug) {
			String sig = edge.getLabel();
			out.println("addCallToReturnEdge: " + src.getLabel() + 
					     "->" + dst.getLabel() + ": "  + sig);
		}
		
		// Added fter reinsertion!
		if (!cfg.containsNode(src))
			cfg.addNode(src);
		if (!cfg.containsNode(dst))
			cfg.addNode(dst);
		// Added fter reinsertion!
		
		cfg.addEdge(src, dst, edge);
	}
		
	private void verifyEdgeDoesNotExist(CFGNode src, CFGNode dst) {		
		assert (src != null || dst != null);
			
		Collection col = cfg.getOutgoingNodes(src);
		if (col.contains(dst))
			throw new Error("Attempt to get multiple edges" +
					" from " + src.getLabel() + " to " + dst.getLabel() +
					" in CFG of method " + ts.getMethod().getSig());
	}
/*
	private static BipartiteGraph newBipartiteGraph() {
		return GraphFactory.newBipartiteGraph();
	}
*/
	
	////////////////////////////////////////////////////
	///                   Accessors                  ///
	////////////////////////////////////////////////////

	public boolean isMain() {
		return ts.isMain();
	}
	
	public String getSig() {
		return ts.getMethod().getSig();
	}
	
	public CFGNode getEntrySite() {
		return entrySite;
	}

	public CFGNode getExitSite() {
		return exitSite;
	}
	
	public MethodTS getMethodTS() {
		return ts;
	}

	public Collection getOutgoingEdges(CFGNode node) {
		return cfg.getOutgoingEdges(node);
	}

	public Collection getFollowingNodes(CFGNode node) {
		return cfg.getOutgoingNodes(node);
	}
	
	public Collection getNodes() {
		return cfg.getNodes();
	}
	
	public Iterator DFSIterator() {
		return GraphUtils.dfsIterator(cfg, entrySite);
	}
	
	public boolean containsSite(CFGNode node) {
		return cfg.containsNode(node);
	}
	
	public boolean containsEdge(CFGEdge edge) {
		return cfg.containsEdge(edge.getSource(),edge.getDestination());
	}
	
	public CFGNode getReturnSite(CFGNode node) {
		assert(node != null);
		assert(containsSite(node));
		assert(node.isCallSite());
		
		Collection next = cfg.getOutgoingNodes(node);
		int size = next.size(); 
		assert(size == 1);
		Iterator itr = next.iterator();
		assert(itr.hasNext());
		CFGNode result = (CFGNode) itr.next();
		assert (result.isRetSite());
		
		return result;	
	}	
	
	public long getId() {
		return ts.getId();
	}
	
	////////////////////////////////////////////////////
	///                  Output Setup                ///
	////////////////////////////////////////////////////

	public void setPrintAllNodes() {
		printAllNodes = true;
	}
	
	public void setPrintNode(CFGNode node) {
		assert(node != null);
		node.setShouldPrint(true);
	}
		
	boolean printAllNodes() {
		return printAllNodes;
	}
	
	public void setPrintInterProcNodes() {
		assert(entrySite != null && exitSite != null);
		entrySite.setShouldPrint(true);
		exitSite.setShouldPrint(true);
		Iterator itr = cfg.getNodes().iterator();
		while (itr.hasNext()) {
			CFGNode node = (CFGNode) itr.next();
			if (node.isCallSite() || node.isRetSite())
				node.setShouldPrint(true);
		}
	}
	
	//////////////////////////////////////////////////
	///              Anbalysis Phase               ///  
	//////////////////////////////////////////////////
	
	/**
	 * Returns a collection of the edges following node src.
	 * If src is a CallSite, returns a single CallToReturnEdge.  
	 * Otherwise, the method returns a collection with IntraEdges.
	 * The returned collection might be empty only if src is not 
	 * a CallSite.
	 */
	public Iterator followingEdges(CFGNode src) {
		Collection edges = cfg.getOutgoingEdges(src);
		return new EdgeInfoIerator(edges);
	}
	
	//////////////////////////////////////////////////////
	//////                  Misc                    //////
    //////////////////////////////////////////////////////
	
	public int hashCode() {
		String mySig = ts.getMethod().getSig(); 
		return mySig.hashCode();
	}
	
	public boolean equals(Object other) {
		if (other == null)
			return false;
		
		if (!(other instanceof CFG))
			return false;
		
		CFG otherCFG = (CFG) other;
		String mySig = ts.getMethod().getSig(); 
		String otherSig = otherCFG.ts.getMethod().getSig(); 
		return mySig.equals(otherSig);
	}
	
	public String toString() {
		return " Method CFG: " +  ts.getMethod().getSig();
	}
	
	//////////////////////////////////////////////////////
	//////              Inner Classes                /////
	//////////////////////////////////////////////////////

	private class EdgeInfoIerator implements Iterator {
		private final Iterator itr;
		
		EdgeInfoIerator(Collection col) {
			itr = col.iterator();
		}
		
		public boolean hasNext() {
			return itr.hasNext();
		}
		
		public Object next() {
			Graph.Edge edge = (Graph.Edge) itr.next();
			TSEdge edgeInfo = (TSEdge) edge.getLabel();
			assert(edgeInfo.getSource() == edge.getSource());
			assert(edgeInfo.getDestination() == edge.getDestination());
			
			return edgeInfo;
		}
		
		public void remove() {
			throw new UnsupportedOperationException("EdgeInfoIerator cannot remove infos");
		}
	}
}
