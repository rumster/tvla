/*
 * File: HashExplodedFlowGraph.java 
 * Created on: 18/10/2004
 */

package tvla.util.graph;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import tvla.util.HashSetFactory;


/** An implementation of the ExplodedFlowGraph interface based on
 * hash graph. The implementation allows only for one source node
 * and one sink node.
 * 
 * @author maon
 */
public final class HashExplodedFlowGraph extends HashFlowGraph implements ExplodedFlowGraph {
	private final  FlowGraph factsGraph;
	private final  Map nodeToFacts;

	private Object sourceNode = null;
	private Object sinkNode = null;
	
	private BipartiteGraph  forwardSummary = new HashBipartiteGraph();  //summarized sourceFacts->sinkFacts
	private BipartiteGraph  backwardSummary = new HashBipartiteGraph(); //summarized sinkFacts->sourcesFacts
	private boolean forwardSummaryUptodate  = true;
	private boolean backwardSummaryUptodate  = true;

	private BipartiteGraph freezedForward = new HashBipartiteGraph();  
	private BipartiteGraph freezedBackward = new HashBipartiteGraph(); ;
	private boolean forwardFreezedUptodate  = true;
	private boolean backwardFreezedUptodate  = true;
	
	// FIXME a hack to handle partial join
	private ArrayList factsPropagatedToExit = new ArrayList(); 
	
	private static final boolean xdebug = true;
	private static final boolean xxdebug = true;
	private static final PrintStream out = System.out; 
		
	////////////////////////////////////////////////////
	//////            ExplodedFlowGraph           //////
	////////////////////////////////////////////////////


	////////////////////////////////////////////////////
	//////              Constructors              //////
	////////////////////////////////////////////////////

	HashExplodedFlowGraph() {
		super();
		factsGraph = new HashFlowGraph();
		nodeToFacts = new LinkedHashMap(); 
	}

	HashExplodedFlowGraph(int numOfNodes) {
		super(((5 * numOfNodes) / 4) + 1, 0.75f);
	
		int initialCapacity = ((5 * numOfNodes) / 4) + 1;
		float loadCapacity = 0.75f;

		factsGraph = new HashFlowGraph(numOfNodes, loadCapacity);
		nodeToFacts = new LinkedHashMap(numOfNodes, loadCapacity);
		
	}

	HashExplodedFlowGraph(int numOfNodes, float loadCapacity) {
		super(numOfNodes, loadCapacity);
		factsGraph = new HashFlowGraph(numOfNodes, loadCapacity);
		nodeToFacts = new LinkedHashMap(numOfNodes, loadCapacity);
		
	}
	
//	HashExplodedFlowGraph(
//			int numOfNodes, float loadCapacity,
//			int inDegree, int outDegree) {
//		super(numOfNodes, loadCapacity, inDegree, outDegree);
//		factsGraph = new HashFlowGraph(numOfNodes, loadCapacity, inDegree, outDegree);
//		nodeToFacts = new LinkedHashMap(numOfNodes, loadCapacity); 
//	}

	
	
	////////////////////////////////////////////////////
	//////                Mutators                //////
	////////////////////////////////////////////////////

	
	/* (non-Javadoc)
	 * @see tvla.util.graph.ExplodedFlowGraph#addFact(java.lang.Object, java.lang.Object)
	 */
	public boolean addFact(Object node, Object fact) {
		assert(this.containsNode(node));
		
		Set factsSet = (Set) nodeToFacts.get(node);
 		assert(factsSet != null);
 		boolean changed = factsSet.add(fact);
 		
		if (xxdebug) {
 			NodeFact tmp = new NodeFact(node,fact);
 			boolean inFactsGraph = factsGraph.containsNode(tmp);
 			assert(changed ^ inFactsGraph);
 		}
		
 		if (!changed) {
 			if (node == sinkNode)
 				factsPropagatedToExit.add(fact);

 			return false;
 		}
 			
 		NodeFact factNode = new NodeFact(node,fact);
 		factsGraph.addNode(factNode);
 		if (sources.contains(node))
 			factsGraph.addToSources(factNode);
 		if (sinks.contains(node))
 			factsGraph.addToSinks(factNode);
			
 		assert(this.containsFact(node,fact));
 		forwardSummaryUptodate = backwardSummaryUptodate = false;
		forwardFreezedUptodate = backwardFreezedUptodate = false;

		return true;
	}

	/* (non-Javadoc)
	 * @see tvla.util.graph.ExplodedFlowGraph#removeFact(java.lang.Object, java.lang.Object)
	 */
	public void removeFact(Object node, Object fact) {
		throw new UnsupportedOperationException();
/*		
		assert(this.containsNode(node));
		
		Set factsSet = (Set) nodeToFacts.get(node);
 		assert(factsSet != null);
 		assert(factsSet.contains(fact));
 		
 		factsSet.remove(fact);
 		factsGraph.removeNode(new NodeFact(node,fact));
 		forwardSummaryUptodate = backwardSummaryUptodate = false;
		forwardFreezedUptodate = backwardFreezedUptodate = false;
*/
	}


	/* (non-Javadoc)
	 * @see tvla.util.graph.ExplodedFlowGraph#AddTranstion(java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	public boolean AddTranstion(
			Object srcNode, Object srcFact, 
			Object dstNode, Object dstFact) {
		assert(srcNode != null && srcFact != null);
		assert(dstNode != null && dstFact != null);
		assert(containsNode(srcNode));
		assert(containsNode(dstNode));
		assert(containsEdge(srcNode,dstNode));
		assert(containsFact(srcNode,srcFact));
		assert(containsFact(dstNode,dstFact));
	
		NodeFact from = new NodeFact(srcNode, srcFact);
		NodeFact to = new NodeFact(dstNode, dstFact);
		
		assert(factsGraph.containsNode(from));
		assert(factsGraph.containsNode(to));

		if (factsGraph.containsEdge(from,to))
			return false;

		factsGraph.addEdge(from, to, null);
			
 		forwardSummaryUptodate = backwardSummaryUptodate = false;
 		forwardFreezedUptodate = backwardFreezedUptodate = false;

 		return true;
	}


	/* (non-Javadoc)
	 * @see tvla.util.graph.ExplodedFlowGraph#replaceFact(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	public void replaceFact(Object node, Object oldFact, Object newFact) {
		throw new UnsupportedOperationException();
/*
		assert(node != null && oldFact != null && newFact != null);
		assert(containsNode(node));
		
		Set factsSet = (Set) nodeToFacts.get(node);
 		assert(factsSet != null);
 		assert(factsSet.contains(oldFact));
		assert(!factsSet.contains(newFact));
		 		
 		Collection inComing = this.getIncoming(node);
 		Collection outGoing = this.getOutgoing(node);
 */		
	}
	
	
	////////////////////////////////////////////////////
	//////               Accessors                //////
	////////////////////////////////////////////////////
	
	/* (non-Javadoc)
	 * @see tvla.util.graph.ExplodedFlowGraph#containsFact(java.lang.Object, java.lang.Object)
	 */
	public boolean containsFact(Object node, Object fact) {
		assert(node != null && fact != null);
		assert(containsNode(node));
		
		Set factsSet = (Set) nodeToFacts.get(node);
 		assert(factsSet != null);
 		boolean res = factsSet.contains(fact);
 		
 		if (xxdebug) {
 			NodeFact tmp = new NodeFact(node,fact);
 			boolean inFactsGraph = factsGraph.containsNode(tmp);
 			assert(!(inFactsGraph ^ res));
 		}
 		
 		return res;
	}
	
	/* (non-Javadoc)
	 * @see tvla.util.graph.ExplodedFlowGraph#getFacts(java.lang.Object)
	 */
	public Collection getFacts(Object node) {
		assert(node != null);

		Set factsSet = (Set) nodeToFacts.get(node);
 		assert(factsSet != null);
		
 		return Collections.unmodifiableCollection(factsSet);
	}
	
	
	
	public Collection getForwardDelta(Object source, Object sink) {
		assert(source != null && sink != null);
		assert(sourceAndSinkAreSet());
		assert(containsNode(source));
		assert(containsNode(sink));
		assert(sources.contains(source));
		assert(sinks.contains(sink));

		
		if (forwardSummaryUptodate && forwardFreezedUptodate)
			return null;
	
		if (!forwardSummaryUptodate)
			updateForward();
				
		Collection summaryEdges = forwardSummary.getEdges(); 
		Collection freezedEdges = freezedForward.getEdges(); 
		
		// Remove all edges that point to fact in the factPropgatedToExit
		if (!factsPropagatedToExit.isEmpty()) {
			Iterator edgeItr = freezedEdges.iterator();
		
			while(edgeItr.hasNext()) {
				Edge edge = (Edge) edgeItr.next();
				Object dst = edge.getDestination();
				if (factsPropagatedToExit.contains(dst))
					edgeItr.remove();
			}
			
			factsPropagatedToExit.clear();
		}
		
		summaryEdges.removeAll(freezedEdges);
		if (summaryEdges.isEmpty())
			return null;
		
		freezedForward = forwardSummary.copy();
		forwardFreezedUptodate = true;
		return summaryEdges;		
	}

	
	public Collection getBackwardDelta(Object source, Object sink) {
		throw new UnsupportedOperationException();
	}
	
	/* Bug?
	public Collection getBackwardDelta(Object source, Object sink) {
		assert(source != null && sink != null);
		assert(sourceAndSinkAreSet());
		assert(containsNode(source));
		assert(containsNode(sink));
		assert(sources.contains(source));
		assert(sinks.contains(sink));

		
		if (backwardSummaryUptodate && backwardFreezedUptodate)
			return null;
	
		if (!backwardSummaryUptodate)
			updateBackward();
				
		Collection summaryEdges = backwardSummary.getEdges(); 
		Collection freezedEdges = freezedBackward.getEdges(); 
		
		summaryEdges.removeAll(freezedEdges);
		if (summaryEdges.isEmpty())
			return null;
		
		freezedBackward= backwardSummary.copy();
		backwardFreezedUptodate = false;
		return summaryEdges;		
	}
    *
    *
	/* (non-Javadoc)
	 * @see tvla.util.graph.ExplodedFlowGraph#getForwardTransitions(java.lang.Object, java.lang.Object)
	 */
	public BipartiteGraph getForwardTransitions(Object source, Object sink) {
		assert(source != null && sink != null);
		assert(sourceAndSinkAreSet());
		assert(containsNode(source));
		assert(containsNode(sink));
		assert(sources.contains(source));
		assert(sinks.contains(sink));

		if (!forwardSummaryUptodate)
			updateForward();

		return forwardSummary;
	}

	/* (non-Javadoc)
	 * @see tvla.util.graph.ExplodedFlowGraph#getBackwardTransitions(java.lang.Object, java.lang.Object)
	 */
	public BipartiteGraph getBackwardTransitions(Object source, Object sink) {
		throw new UnsupportedOperationException();
	}	
	/* BUG? NR
	public BipartiteGraph getBackwardTransitions(Object source, Object sink) {
		assert(source != null && sink != null);
		assert(sourceAndSinkAreSet());
		assert(containsNode(source));
		assert(containsNode(sink));
		assert(sources.contains(source));
		assert(sinks.contains(sink));
		
		if (!backwardSummaryUptodate)
			updateBackward();

		return backwardSummary;
	}
    */ 
	
	/* (non-Javadoc)
	 * @see tvla.util.graph.ExplodedFlowGraph#getEdgeTransitions(java.lang.Object, java.lang.Object)
	 */
	public BipartiteGraph getEdgeTransitions(Object srcNode, Object dstNode) {
		assert(srcNode != null);
		assert(dstNode != null);
		assert(containsNode(srcNode));
		assert(containsNode(dstNode));
		assert(containsEdge(srcNode,dstNode));
		
		
		BipartiteGraph transition = new HashBipartiteGraph();
		Set factsAtSrc = (Set) nodeToFacts.get(srcNode); 
		
		Iterator nodeItr = factsAtSrc.iterator();
		while (nodeItr.hasNext()) {
			Object srcFact = nodeItr.next();
			NodeFact nodeFact = new NodeFact(srcNode, srcFact);
			Collection followingNodeFacts = factsGraph.getOutgoingNodes(nodeFact);
			if (!followingNodeFacts.isEmpty()) {
				boolean addedSrc = false;
				Iterator nodeFactItr = followingNodeFacts.iterator();
				while (nodeFactItr.hasNext()) {
					NodeFact dstNodeFact = (NodeFact) nodeFactItr.next();
					if (dstNodeFact.node == dstNode) {
						if (!addedSrc) {
							transition.addSourceNode(srcFact);
							addedSrc = true;
						}
						Object dstFact = dstNodeFact.fact;
						transition.addDestinatonNode(dstFact);
						transition.addEdge(srcFact,dstFact);
					}
				}
			}
		}
		
		return transition;
	}
	
	
	/* (non-Javadoc)
	 * @see tvla.util.graph.ExplodedFlowGraph#getEdgeTransitions(java.lang.Object, java.lang.Object)
	 */
	public Collection getStuckFacts(Object node) {
		assert(node != null);
		assert(containsNode(node));		
		
		Set stuckFacts = HashSetFactory.make();
		Set facts = (Set) nodeToFacts.get(node); 
		
		Iterator nodeItr = facts.iterator();
		while (nodeItr.hasNext()) {
			Object srcFact = nodeItr.next();
			NodeFact nodeFact = new NodeFact(node, srcFact);
			Collection followingNodeFacts = factsGraph.getOutgoingNodes(nodeFact);
			if (followingNodeFacts.isEmpty()) 
				stuckFacts.add(srcFact);
		}
		
		return stuckFacts;
	}
	
	public Collection getCachedFactsAtReachableSinks(
			Object source, Object fact, Object sink) {
		assert(source != null && fact != null && sink != null);
		assert(sourceAndSinkAreSet());
		assert(containsNode(source));
		assert(containsNode(sink));
		assert(sources.contains(source));
		assert(sinks.contains(sink));
		
		Set factsAtSource = (Set) nodeToFacts.get(source); 
		assert(factsAtSource.contains(fact));
		
		return getCahchedClosure(fact);
	}
	
	
	public Collection getFactsAtReachableSinks(
			Object source, Object fact, Object sink) {
		throw new UnsupportedOperationException();
	}
		
	/*
	public Collection getFactsAtReachableSinks(
			Object source, Object fact, Object sink) {
		assert(source != null && fact != null && sink != null);
		assert(sourceAndSinkAreSet());
		assert(containsNode(source));
		assert(containsNode(sink));
		assert(sources.contains(source));
		assert(sinks.contains(sink));
		
		
		Set factsAtSource = (Set) nodeToFacts.get(sourceNode); 
		
		Iterator nodeItr = factsAtSource.iterator();
		while (nodeItr.hasNext()) {
			Object srcFact = nodeItr.next();
			NodeFact nodeFact = new NodeFact(sourceNode, srcFact);
			Collection reachableSinkFacts = factsGraph.getReachableSinks(nodeFact);
			if (!reachableSinkFacts.isEmpty()) {
				Iterator sinkNodeFactItr = reachableSinkFacts.iterator();
				forwardSummary.addSourceNode(srcFact);
				while (sinkNodeFactItr.hasNext()) {
					NodeFact sinkNodeFact = (NodeFact) sinkNodeFactItr.next();
					assert(sinkNodeFact.node == sinkNode);
					Object sinkFact = sinkNodeFact.fact;
					forwardSummary.addDestinatonNode(sinkFact);
					forwardSummary.addEdge(srcFact,sinkFact);
				}
			}
		}
		
		forwardSummaryUptodate = true;

		
	}
*/
	
	/**
	 * Internal Stuff
	 **/

	private Collection 	getCahchedClosure(Object fact) {
		if (forwardSummary.containsSource(fact))
			return forwardSummary.getOutgoingNodes(fact);
		
		return null;
	}
	
	private void updateForward() {
		assert (sourceAndSinkAreSet());
		
		forwardSummary.clear();
		
		Set factsAtSource = (Set) nodeToFacts.get(sourceNode); 
		
		Iterator nodeItr = factsAtSource.iterator();
		while (nodeItr.hasNext()) {
			Object srcFact = nodeItr.next();
			NodeFact nodeFact = new NodeFact(sourceNode, srcFact);
			Collection reachableSinkFacts = factsGraph.getReachableSinks(nodeFact);
			if (!reachableSinkFacts.isEmpty()) {
				Iterator sinkNodeFactItr = reachableSinkFacts.iterator();
				forwardSummary.addSourceNode(srcFact);
				while (sinkNodeFactItr.hasNext()) {
					NodeFact sinkNodeFact = (NodeFact) sinkNodeFactItr.next();
					assert(sinkNodeFact.node == sinkNode);
					Object sinkFact = sinkNodeFact.fact;
					forwardSummary.addDestinatonNode(sinkFact);
					forwardSummary.addEdge(srcFact,sinkFact);
				}
			}
		}
		
		forwardSummaryUptodate = true;
	}
	
	/*
	private void updateBackward() {
		assert (sourceAndSinkAreSet());
		
		backwardSummary.clear();
		
		Set factsAtSink = (Set) nodeToFacts.get(sinkNode); 
		
		Iterator nodeItr = factsAtSink.iterator();
		while (nodeItr.hasNext()) {
			Object sinkFact = nodeItr.next();
			NodeFact nodeFact = new NodeFact(sinkNode, sinkFact);
			Collection reachingSourceFacts = factsGraph.getReachingSources(nodeFact);
			if (!reachingSourceFacts.isEmpty()) {
				Iterator sourceNodeFactItr = reachingSourceFacts.iterator();
				backwardSummary.addDestinatonNode(sinkFact);
				while (sourceNodeFactItr.hasNext()) {
					NodeFact sourceNodeFact = (NodeFact) sourceNodeFactItr.next();
					assert(sourceNodeFact.node == sourceNode);
					Object sourceFact = sourceNodeFact.fact;
					forwardSummary.addSourceNode(sourceFact);
					forwardSummary.addEdge(sourceFact,sinkFact);
				}
			}
		}
		
		backwardSummaryUptodate = true;
	}
   */
	
	///////////////////////////////////////////////////
	/////                FlowGraph             ////////
	///////////////////////////////////////////////////

	public void addToSources(Object node) {
		assert(sources.isEmpty());
		assert(sourceNode == null);
		super.addToSources(node);
		sourceNode = node;
	}

	public void addToSinks(Object node){
		assert(sinks.isEmpty());
		assert(sinkNode == null);
		super.addToSinks(node);
		sinkNode = node;
	}
	
	
	///////////////////////////////////////////////////
	/////                  Graph               ////////
	///////////////////////////////////////////////////

	public boolean addNode(Object node) {
		boolean ret = super.addNode(node);
		nodeToFacts.put(node, HashSetFactory.make());
		
		return ret;
	}
	
//	public void addNode(Object node, Object nodeInfo) {
//		super.addNode(node, nodeInfo);
//		nodeToFacts.put(node, HashSetFactory.make());		
//	}

	public boolean addEdge(Object from, Object to) {
		boolean ret = super.addEdge(from, to);
		
		// checks if the nodes are new
		if (!nodeToFacts.containsKey(from))
			nodeToFacts.put(from, HashSetFactory.make());			
		if (!nodeToFacts.containsKey(to))
			nodeToFacts.put(to, HashSetFactory.make());
		
		return ret;
	}
	
	/** Adds an edge between <code>from</code> and <code>to</code>
	 * with optional information.
	 */
	public boolean addEdge(Object from, Object to, Object edgeInfo) {		
		boolean ret = super.addEdge(from, to, edgeInfo);
		
		// checks if the nodes are new
		if (!nodeToFacts.containsKey(from))
			nodeToFacts.put(from, HashSetFactory.make());			
		if (!nodeToFacts.containsKey(to))
			nodeToFacts.put(to, HashSetFactory.make());
		
		return ret;
	}

	
	public boolean removeNode(Object node) {
		throw new UnsupportedOperationException();
	}

	public boolean removeEdge(Edge edge) {
		throw new UnsupportedOperationException();
	}
	
	public boolean removeEdge(Object from, Object to) {
		throw new UnsupportedOperationException();
	}
	
	public boolean removeAllNodes(Collection nodes) {
		throw new UnsupportedOperationException();		
	}

	public boolean removeAllEdges(Collection edges) {
		throw new UnsupportedOperationException();		
	}

	public void clear() {
		super.clear();
		factsGraph.clear();
		nodeToFacts.clear();
		forwardSummary.clear();
		backwardSummary.clear();
	}

	///////////////////////////////////////////////////
	/////                  MISC                ////////
	///////////////////////////////////////////////////

	private boolean sourceAndSinkAreSet() {
		assert(sourceNode == null ^ !sources.isEmpty());
		assert(sinkNode == null ^ !sinks.isEmpty());
		assert(sourceNode == null || sources.contains(sourceNode));
		assert(sinkNode == null || sinks.contains(sinkNode));
				
		return sourceNode != null && sinkNode != null;
	}
	
	///////////////////////////////////////////////////
	/////              INNER CLASSESS          ////////
	///////////////////////////////////////////////////

	private class NodeFact {
		private final Object node;
		private final Object fact;
		public NodeFact(Object node, Object fact) {
			assert(node != null);
			assert(fact != null);
			this.node = node;
			this.fact = fact;
		}
		
		public int hashCode() {
			return node.hashCode() + 19 * fact.hashCode();
		}

		public boolean equals(Object other) {
			if (other == null)
				return false;
			if (!(other instanceof NodeFact))
				return false;
					
			NodeFact otherNodeFact = (NodeFact) other;
			
			return node.equals(otherNodeFact.node) &&
				   fact.equals(otherNodeFact.fact);
			
		}
	}
}
