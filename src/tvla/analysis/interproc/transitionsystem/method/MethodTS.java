/*
 * File: MethodTS.java 
 * Created on: 22/09/2004
 */

package tvla.analysis.interproc.transitionsystem.method;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tvla.analysis.interproc.Method;
import tvla.analysis.interproc.semantics.ActionInstance;
import tvla.analysis.interproc.transitionsystem.AbstractState;
import tvla.analysis.interproc.transitionsystem.TVSRepository;
import tvla.analysis.interproc.transitionsystem.AbstractState.Fact;
import tvla.analysis.interproc.util.Output;
import tvla.core.HighLevelTVS;
import tvla.util.SingleSet;
import tvla.util.graph.BipartiteGraph;
import tvla.util.graph.ExplodedFlowGraph;
import tvla.util.graph.Graph;
import tvla.util.graph.GraphFactory;
//import tvla.language.XML.*;

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
public final class MethodTS {
	private static boolean xdebug = false;
	private static java.io.PrintStream out = System.out;
	
	private final Method mtd;

	private final TSNode entrySite;
	private final TSNode exitSite;
	private final Map labelToNode;
	private final CFG cfg;
	private final ExplodedFlowGraph explodedGraph;
	//private final MethodSummary summary;
	private final TVSRepository repository;
	
	/// MethodTS unique identiification
	private long id;
	
	//  Next node id
	private long nodeId;
	
	//  Next edge id
	private long edgeId;

	//////////////////////////////////////////////////
	///                CONSTRUCTORS                ///
	//////////////////////////////////////////////////

	private MethodTS(Method mtd, long id, int numOfStmts, String entryLabel, String exitLabel) {
		super();
		
		assert(0 < numOfStmts);
		if (entryLabel.equals(exitLabel))
			throw new Error(
					"Entry label [" + entryLabel + 
					"] is the same as exit label " + 
					" in method " + mtd.getSig());

		this.mtd = mtd;
		this.id = id;
		this.labelToNode = new LinkedHashMap((5*numOfStmts)/2 + 2, 0.75f);
		this.repository = TVSRepository.newTVSRepository();
		this.cfg = new CFG(this, numOfStmts);
		this.entrySite =  obtainNode(CFGNode.ENTRY_SITE, entryLabel);
		this.exitSite =  obtainNode(CFGNode.EXIT_SITE, exitLabel);
		cfg.setEntrySite(this.entrySite);
		cfg.setExitSite(this.exitSite);
		this.explodedGraph = GraphFactory.newExplodedFlowGraph(numOfStmts);
		this.explodedGraph.addNode(entrySite);
		this.explodedGraph.addToSources(entrySite);
		this.explodedGraph.addNode(exitSite);
		this.explodedGraph.addToSinks(exitSite);
		
		//this.summary = new MethodSummary(explodedGraph);

		this.nodeId = 0;
		this.edgeId = 0;
		
		labelToNode.put(entryLabel,entrySite);
		labelToNode.put(exitLabel,exitSite);
	}
	
	/**
	 * Constructing mthods TS according to the Factory paptern.
	 */
	public static MethodTS newMethodTS(Method mtd, long methodTSId, int numOfStmts, String entryLabel, String exitLabel) {
		return new MethodTS(mtd, methodTSId, numOfStmts, entryLabel, exitLabel);
	}
		
	/////////////////////////////////////////////////
	///                 MUTATORS                  ///
	/////////////////////////////////////////////////

		
	public void addIntraStmt(
			String from, String to,
			ActionInstance iAction) {
		if (xdebug)
			out.println("!addIntraStmt: " + from + "->" + to + ": " + iAction.getMacroName(true));		

		TSNode src = obtainNode(CFGNode.INTRA, from);
		TSNode dst = obtainNode(CFGNode.INTRA, to);
		TSEdge edge = allocateEdge(src,dst,iAction,null,null);
		cfg.addIntraEdge(src,dst,edge);
		
		// Added after reinsertion
		if (!explodedGraph.containsNode(src))
			explodedGraph.addNode(src);
		if (!explodedGraph.containsNode(dst))
			explodedGraph.addNode(dst);
		// Added after Reinsertion
		
		explodedGraph.addEdge(src,dst,null);
	}
	
	public void addConstructorInvocation(String from, String to,
										 MethodTS invokedMtd, List args) { 
		if (xdebug)
			out.println("addConstructorInvocation: " + from + "->" + to + ": " + mtd.getSig());

		TSNode src = obtainNode(CFGNode.CONSTRUCTOR_CALL_SITE, from);
		TSNode dst = obtainNode(CFGNode.RET_SITE, to);
		verifyEdgeDoesNotExist(src, dst);	
		
		TSEdge edge = allocateEdge(src,dst,null,invokedMtd.getMethod().getName(), args);
		cfg.addCallToRetrunEdge(src, dst, edge);
		
		// Added after reinsertion
		if (!explodedGraph.containsNode(src))
			explodedGraph.addNode(src);
		if (!explodedGraph.containsNode(dst))
			explodedGraph.addNode(dst);
		// Added after Reinsertion

		explodedGraph.addEdge(src,dst);
	}

	public void addStaticInvocation(String from, String to,
									MethodTS invokedMtd, List args) {
		if (xdebug)
			out.println("addStaticInvocation: " + from + "->" + to + ": " + mtd.getSig());	

		TSNode src = obtainNode(CFGNode.STATIC_CALL_SITE, from);
		TSNode dst = obtainNode(CFGNode.RET_SITE, to);
		verifyEdgeDoesNotExist(src, dst);	
		
		TSEdge edge = allocateEdge(src,dst,null,invokedMtd.getMethod().getName(), args);
		cfg.addCallToRetrunEdge(src, dst, edge);

		// Added after reinsertion
		if (!explodedGraph.containsNode(src))
			explodedGraph.addNode(src);
		if (!explodedGraph.containsNode(dst))
			explodedGraph.addNode(dst);
		// Added after Reinsertion
		
		explodedGraph.addEdge(src,dst,null);
	}

	public void addVirtualInvocation(String from, String to,
									 MethodTS invokedMtd, List args) {
		if (xdebug)
			out.println("addVirtualInvocation: " + from + "->" + to + ": " + mtd.getSig());		

		TSNode src = obtainNode(CFGNode.VIRTUAL_CALL_SITE, from);
		TSNode dst = obtainNode(CFGNode.RET_SITE, to);

		assert (src != null || dst != null);
		
		Collection col = cfg.getFollowingNodes(src);
		if (!col.isEmpty()) {
			assert(col.size() == 1);
			assert(col.contains(dst));
			Collection edges = cfg.getOutgoingEdges(src);
			Iterator itr = edges.iterator();
			assert(itr.hasNext());
			Graph.Edge e = (Graph.Edge) itr.next();
			assert (e.getLabel() instanceof TSEdgeCallToReturn);
			if (xdebug)
				out.println(" a CallToReturn exdge already exists in the graph, nothing to do.");
			return;
		}
		
		TSEdge edge = allocateEdge(src,dst,null,invokedMtd.getMethod().getName(), args);
		cfg.addCallToRetrunEdge(src, dst, edge);

		// Added after reinsertion
		if (!explodedGraph.containsNode(src))
			explodedGraph.addNode(src);
		if (!explodedGraph.containsNode(dst))
			explodedGraph.addNode(dst);
		// Added after Reinsertion
		
		explodedGraph.addEdge(src,dst,null);
	}	

	public TSNode getNode(String label) {
		assert(label != null);
		return (TSNode) labelToNode.get(label);
	}

	private TSNode obtainNode(int type, String label) {
		TSNode node = (TSNode) labelToNode.get(label);
		
		if (node != null) {
			if ((node.getType() == type) ||
				(node.isRetSite() && TSNode.isIntraStmtType(type)) ||
				(node.isExitSite() && TSNode.isIntraStmtType(type)) ||
				(node.isEntrySite() && TSNode.isIntraStmtType(type)))
				return node;
			
			
			if (! (node.isIntraStmtSite() && TSNode.isCallType(type)))   	
				throw new InternalError("Node " + label + 
						                " has conflicting types: was " + node.getType() + 
										" and now " + type);

			if (node.isIntraStmtSite()) {
				assert(TSNode.isCallType(type));
				node.setAsCallSite(type);
			}
			
			return node;
		}
		
		node = TSNode.newMethodTSNode(type, cfg, ++nodeId, label, repository.allocateAbstractState());
		labelToNode.put(label,node);
		
		return node;
	}
	
	private TSEdge allocateEdge(
			TSNode src, TSNode dst,
			ActionInstance iAction,
			String calleeName,
			List args) {
		assert (src != null);
		assert (dst != null);

		//AbstractState srcState = src.getAbstractState();
		//AbstractState dstState = dst.getAbstractState();
				
		if (iAction != null) {
			// an intraprocedural edge
			assert(!src.isCallSite());
			assert(calleeName == null);
			++edgeId;
			return new TSEdgeIntra(src,dst,edgeId,iAction);
		}
		else {
			// a call=to-return edge
			assert(src.isCallSite());
			assert(calleeName != null);
			assert(args!=null);
			String invcationString = Output.invocationString(calleeName,args);
			return new TSEdgeCallToReturn(src,dst,++edgeId,invcationString);
		}
	}
	
	private void verifyEdgeDoesNotExist(CFGNode src, CFGNode dst) {		
		assert (src != null || dst != null);
			
		Collection col = cfg.getFollowingNodes(src);
		if (col.contains(dst))
			throw new Error("Attempt to get multiple edges" +
					" from " + src.getLabel() + " to " + dst.getLabel() +
					" in CFG of method " + mtd.getSig());
	}

//	private static BipartiteGraph newBipartiteGraph() {
///		return GraphFactory.newBipartiteGraph();
//	}
//	

	//////////////////////////////////////////////////
	///                 ACCESSORS                  ///
	//////////////////////////////////////////////////

	public boolean isMain() {
		return mtd.isMain();
	}
	
	public String getSig() {
		return mtd.getSig();
	}

	public Method getMethod() {
		return mtd;
	}

	public TSNode getEntrySite() {
		return entrySite;
	}

	public TSNode getExitSite() {
		return exitSite;
	}
		
	public CFG getCFG() {
		return this.cfg;
	}
	
	public long getId() {
		return this.id;
	}
	
	////////////////////////////////////////////////////
	///                  Output Setup                ///
	////////////////////////////////////////////////////

	public void setPrintAllNodes() {
		cfg.setPrintAllNodes();
	}
	
	public void setPrintInterProcNodes() {
		cfg.setPrintInterProcNodes();
	}
	
	public void setPrintNode(String nodeLabel) {
		assert(nodeLabel != null);
		
		CFGNode node =  (CFGNode) labelToNode.get(nodeLabel);
		if (node == null)
			throw new Error(
					"Attempt to print a non existing node " + nodeLabel + 
					" in method " + mtd.getSig());
		 
		cfg.setPrintNode(node);
	}
		
//	boolean printAllNodes() {
//		return cfg.printAllNodes();
//	}
	/*

	public void PrintMessages(TVLAIO io) {
		String msgBanner = "Messages (using focused strucutres)";
		boolean printedBanner = false;
		
		Iterator nodeItr = cfg.DFSIterator();
		assert(nodeItr != null);
		while (nodeItr.hasNext()) {
			TSNode node = (TSNode) nodeItr.next();
			
			Map msgs = node.getAbstractState().getMessages();
			assert(msgs != null);
			if (msgs.isEmpty())
				continue;
			
			if (!printedBanner) {
				String banner = msgBanner;
				io.printMessageBanner(banner);
				printedBanner = true; 
			}
			
			String banner = "Messages for node type " + node.getTypeString()  +
							" label: " + node.getLabel(); 
			io.printMessageBanner(banner);
			
			Iterator tvsItr = msgs.values().iterator();
			while (tvsItr.hasNext()) {
				Map focusedTVSToMsgs = (Map) tvsItr.next();
				Iterator focusedTVSItr = focusedTVSToMsgs.keySet().iterator();
				while (focusedTVSItr.hasNext()) {
					Object tvs = focusedTVSItr.next();
					Collection focusedTVSMsgs = (Collection) focusedTVSToMsgs.get(tvs);
					io.printStrucutreWithMessages("Node: " + node.getLabel(), tvs, focusedTVSMsgs);
				}
			}
		}
	}

	
	public void PrintNodes(TVLAIO io) {
		String fixpointBanner = "Fixpoint (using propagated strucutres)";
		io.printBanner(fixpointBanner);

		printEntryAndExitSites(io);
		
		Iterator nodeItr = cfg.DFSIterator();
		assert(nodeItr != null);
		while (nodeItr.hasNext()) {
			TSNode node = (TSNode) nodeItr.next();
			if (node == this.entrySite || node == this.exitSite)
				continue;
			
			if (!node.getShouldPrint()) 
				continue;
			String banner = "Structures for node type " + node.getTypeString()  +
							" label: " + node.getLabel(); 
			io.printBanner(banner);
			
			Iterator tvsItr = node.getAbstractState().getTVSsItr();
			while (tvsItr.hasNext()) {
				Object tvs = tvsItr.next();
				io.printStructure(tvs, "Node: " + node.getLabel());
			}
		}
	}
	
	private void printEntryAndExitSites(TVLAIO io) {		
		if (entrySite.getShouldPrint()) {
			String banner = 
				"ENTRY: Structures for node type " + 
			    entrySite.getTypeString()  +
				" label: " + entrySite.getLabel(); 
			io.printBanner(banner);

			Iterator tvsItr = entrySite.getAbstractState().getTVSsItr();
			while (tvsItr.hasNext()) {
				Object tvs = tvsItr.next();
				io.printStructure(tvs, "ENTRY Node: " + entrySite.getLabel());
			}
		}
		
		if (exitSite.getShouldPrint()) {
			String banner = 
				"EXIT: Structures for node type " + 
            	exitSite.getTypeString()  +
				" label: " + exitSite.getLabel(); 
			io.printBanner(banner);

			Iterator tvsItr = exitSite.getAbstractState().getTVSsItr();
			while (tvsItr.hasNext()) {
				Object tvs = tvsItr.next();
				io.printStructure(tvs, "EXIT Node: " + exitSite.getLabel());
			}
		
		}
	}

	
	public void printResults(TVLAIO io) {		
		io.printAnalysisState(this);
	}

//	public void printResults(TVLAIO io) {		
//		String outStreamName = io.genValidStreamName(getSig());
//		io.redirectOutput(outStreamName);
//		io.printBanner("Analysis Results for method " + getSig()  + " " +
//				       " to file " + outStreamName);
//		
//		io.printProgram(new SingleSet(true,this));
//		
//		PrintMessages(io);
//		PrintNodes(io);
//	}
*/
	
	/*
	public void saveResults(TVLAIO io, XML xml) {
		String fileName = io.genValidStreamName(getSig());
		java.io.Writer writeTo = io.getFileWriter("xml", fileName, "xml"); 
		
		xml.saveResults(this, writeTo);
		try {
			writeTo.close();
		}
		catch (Exception e) {
			throw new Error(e.getMessage());
		}
	}
	*/
	
	public Collection getAllTVSs() {
		ArrayList allTVSs = new ArrayList();
		Iterator nodeItr = cfg.DFSIterator();
		assert(nodeItr != null);
		while (nodeItr.hasNext()) {
			TSNode node = (TSNode) nodeItr.next();
			Iterator tvsItr = node.getAbstractState().getTVSsItr();
			while (tvsItr.hasNext()) {
				Object tvs = tvsItr.next();
				allTVSs.add(tvs);
			}
		}
		
		return allTVSs;
	}
	
	public BipartiteGraph getTransitions(TSNode src, TSNode trg) {
		return explodedGraph.getEdgeTransitions(src,trg);
	}
	
	//////////////////////////////////////////////////
	///              Anbalysis Phase               ///  
	//////////////////////////////////////////////////
	
	/**
	 * Returns a collection of the edges following  src.
	 * If src is a CallSite, returns a single CallToReturnEdge.  
	 * Otherwise, the method returns a collection with IntraEdges.
	 * The returned collection might be empty only if src is not 
	 * a CallSite.
	 */
	public Iterator followingEdges(CFGNode src) {
		Collection edges = cfg.getOutgoingEdges(src);
		return new EdgeInfoIerator(edges);
	}
	
	
	public TSNode getMatchingReturnNode(CFGNode node) {
		assert(node != null);
		assert(containsSite(node));
		assert(node.isCallSite());
		
		return (TSNode) cfg.getReturnSite(node);
	}
	
	/**
	 * Adds a given TVS at a given node.
	 */
	public boolean addTVS(
			TSNode tsNode, 
			HighLevelTVS tvs, 
			SingleSet mergedTo) {
		assert(tsNode != null);
		assert(tvs != null);
		assert(mergedTo != null);
		
		AbstractState as = tsNode.getAbstractState();
		boolean asChanged = as.addTVS(tvs, mergedTo);
		
		if (asChanged) {
			Object fact = mergedTo.get();
			assert(fact != null);
			assert(fact instanceof Fact);
			Fact tvsHandle = (Fact) fact;
			// FIXME might be a problem with partial-join 
			HighLevelTVS newTVS = tvsHandle.getTVS();
			explodedGraph.addFact(tsNode, newTVS);
		}
		
		return asChanged;
	}
	
	public boolean addTransition(
			TSNode fromNode, 
			Fact fromFact,
			TSNode toNode,
			Fact toFact) {
		HighLevelTVS fromTVS = fromFact.getTVS();
		HighLevelTVS toTVS = toFact.getTVS();
		boolean newTransition = 
			explodedGraph.AddTranstion(fromNode,fromTVS,toNode,toTVS);
		
		return newTransition;
	}
	
	public Collection getKnownEffect(
			Fact entryFact) {
		Collection res = explodedGraph.getCachedFactsAtReachableSinks(
				entrySite, entryFact.getTVS(), exitSite);
		
		if (res == null)
			res = Collections.EMPTY_SET;
		
		return res;
	}
	
	public Fact getFactForTVS(
			TSNode node,
			HighLevelTVS tvsAtNode) {
		return node.getAbstractState().getFactForExistingTVS(tvsAtNode);
	}
	
	public Collection updateSummary() {
		Collection change = explodedGraph.getForwardDelta(entrySite, exitSite);
		return change;
	}

	
	
	//////////////////////////////////////////////////
	///                 Debuggig                   ///  
	//////////////////////////////////////////////////
	
	public boolean containsSite(CFGNode site) {
		return site.getCFG() == cfg;
	}

	public boolean containsEdge(CFGEdge edge) {
		return cfg.containsEdge(edge);
	}
	
	//////////////////////////////////////////////////////
	//////                  Misc                    //////
    //////////////////////////////////////////////////////
	
	public int hashCode() {
		String mySig = mtd.getSig(); 
		return mySig.hashCode();
	}
	
	public boolean equals(Object other) {
		if (other == null)
			return false;
		
		if (!(other instanceof MethodTS))
			return false;
		
		MethodTS otherTS = (MethodTS) other;
		String mySig = mtd.getSig(); 
		String otherSig = otherTS.mtd.getSig(); 
		return mySig.equals(otherSig);
	}
	
	public String toString() {
		return " MethodTS: " +  mtd.getSig();
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
