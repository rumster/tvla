/*
 * File: FlowGraph.java 
 * Created on: 18/10/2004
 */

package tvla.util.graph;

import java.util.Collection;
import java.util.Map;

/** A directed graph with sources and sinks.
 * Sources and sinkes are (disjoint) subsets of the graph nodes. 
 * Sources cannot have incoming edges and sinks are not allowed 
 * to have putgoing edges.
 * @author maon
 */
public interface FlowGraph  extends Graph {
	/**
	 * Adds a graph node to the set of sources.
	 *
	 * @param node a node in the graph.
	 */
	public void addToSources(Object node);

	/**
	 * Adds a graph node to the set of sinks.
	 *
	 * @param node a node in the graph.
	 */	
	public void addToSinks(Object node);
	
	/**
	 * Retruns the set of sources.
	 * @return A set of nodes. Never returns null.
	 */
	public Collection getSources();
	
	/**
	 * Retruns the set of sources.
	 * @return A set of nodes. Never returns null.
	 */
	public Collection getSinks();

	/** Returns the sinks which are reachable from a given source node.
	 * 
	 * @param a source node
	 * @return
	 */
	public Collection getReachableSinks(Object source);
	
	/** Returns the sources that reach a given sink node.
	 * 
	 * @param a source node
	 * @return
	 */
	public Collection getReachingSources(Object sink);
	
	/** Returns a map from every source node src to 
	 * a collection of the sinks it reaches.
	 */
	public Map sourcesToSinks(); 
	
	/** Returns a map from every sink node snk to 
	 * a collection of the souces that reach it.
	 */
	public Map sinksToSources();

}
