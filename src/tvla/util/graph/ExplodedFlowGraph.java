/*
 * File: FlowGraph.java 
 * Created on: 18/10/2004
 */

package tvla.util.graph;

import java.util.Collection;


/** A directed graph with sources and sinks.
 * Each node need has a set of facts.
 * Each edge has a bipartite graph containing transitions from 
 * the elemnts of the set in the source to the edge to the 
 * set at the target of the edge.
 * 
 * Sources and sinkes are (disjoint) subsets of the graph nodes. 
 * @author maon
 */
public interface ExplodedFlowGraph  extends FlowGraph {
	/**
	 * Adds a fact to a specicific node in the graph
	 * @param node a node in the graph
	 * @param fact a fact
	 * @return whtehr the fact is new
	 */
	public boolean addFact(Object node, Object fact);
	
	/**
	 * Remove a fact from a node
	 * An optional method.
	 * @param node a node in the graph
	 * @param fact a fact
	 */
	public void removeFact(Object node, Object fact);

	/**
	 * Replaces oldFact by newFact.
	 * Transitions containing oldFact as the transition source
	 * resp. desitnation are replaced by transtions containg 
	 * newFact as the source resp. destination.
	 * An optional method.
	 * @param node a node in the graph
	 * @param oldFact a fact in node's fact set
	 * @param newFact a fact not in node's fact set
	 */
	public void replaceFact(Object node, Object oldFact, Object newFact);

	
	/**
	 * Returns whether fact is in nodes' fact set
	 * @param node a node in the graph
	 * @param fact
	 * @return
	 */
	public boolean containsFact(Object node, Object fact);
	
	/**
	 * Returns a coolection of all the facts in a node
	 * @param node a node in the graph
	 * @param fact
	 * @return a collection of facts. Never returns null.
	 */
	public Collection getFacts(Object node);

	
	/**
	 * Adds a transition between two facts in two specific nodes.
	 * pre: (srcNode,dstNode) is an edge in the graph
	 * @param srcNode a node in the graph
	 * @param srcFact a fact in srcNode
	 * @param dstNode a node in the graph
	 * @param dstFact a fact in dstNode
	 * @return whether the trnasition is new
	 */
	public boolean AddTranstion(Object srcNode, Object srcFact, Object dstNode, Object dstFact);
	
	/**
	 * Gets the change in the transitive closure fact-flow in the graph from the last time
	 * getForwardDelta was invoked. An optional method.
	 * @param source a source node in the graph
	 * @param sink a sink node in the graph
	 * @return null if there was no change since the last time ths method was invoked.
	 * Otherwise, a collection of all the fact-transitions from facts in the source node 
	 * to fact in the sink node that reflect the change in the forward transitive closure.
	 */
	public Collection getForwardDelta(Object source, Object sink);

	/**
	 * Gets the change in the backward transitive closure fact-flow in the graph from the last time
	 * getBackwardDelta was invoked. An optional method.
	 * @param source a source node in the graph
	 * @param sink a sink node in the graph
	 * @return null if there was no change since the last time ths method was invoked.
	 * Otherwise, a collection of all the fact-transitions from facts in the source node 
	 * to fact in the sink node that reflect the change in the backward transitive closure.
	 */
	
	public Collection getBackwardDelta(Object source, Object sink);

	/**
	 * Returns a Bipartite graph which maps facts at srcNode
	 * to facts at dstNode according to the transitive closure of the
	 * graph transition relation
	 * An optional method.
	 * 	  
	 * @param sourceNode a source node in the graph 
	 * @param sinkNode a sink node in the graph
	 * @return
	 */
	public BipartiteGraph getForwardTransitions(Object sourceNode, Object sinkNode);

	/**
	 * Returns a Bipartite graph which maps facts at srcNode
	 * to facts at dstNode according to the transitive closure of the
	 * graph transition relation
	 * An optional method.
	 * 	  
	 * @param sourceNode a source node in the graph 
	 * @param sinkNode a sink node in the graph
	 * @return
	 */
	public BipartiteGraph getBackwardTransitions(Object sourceNode, Object sinkNode);

	/**
	 * Returns a Bipartite graph which maps facts at srcNode
	 * to facts at dstNode according to the transition on the edge
	 * between them.
	 * 
	 * pre: (srcNode,dstNode) is an edge in the graph
	 * 
	 * @param srcNode a node in the graph 
	 * @param dstNode a node in the graph
	 * @return
	 */
	public BipartiteGraph getEdgeTransitions(Object srcNode, Object dstNode);
	
	/**
	 * Returns a collection of the facts in src
	 * @param srcNode
	 * @param dstNode
	 * @return
	 */
	 // public Collection getStuckFacts(Object node);
	
	/**
	 *  Returns a collection of all the facts at the sink node
	 * that are reachablef from fact fact at the source node.
	 * pre: fact is in srcNode
	 * @param srcNode a source node
	 * @param fact
	 * @param dstNode a sink   
	 */
	public Collection getFactsAtReachableSinks(
			Object srcNode, Object fact, Object dstNode);

	
	
	/**
	 *  Returns a collection of all the cached facts at the sink node
	 * that are reachable from fact fact at the source node.
	 * pre: fact is in srcNode
	 * @param srcNode a source node
	 * @param fact
	 * @param dstNode a sink   
	 */
	public Collection getCachedFactsAtReachableSinks(
			Object srcNode, Object fact, Object dstNode);
	
	public interface ExplodedFlowGraphEdge extends Graph.Edge {
		
	}
}
