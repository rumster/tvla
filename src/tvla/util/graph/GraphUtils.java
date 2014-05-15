/*
 * Created on 26/03/2004
 *
 */
package tvla.util.graph;

import gnu.trove.TObjectIntHashMap;

import java.util.*;

import tvla.util.HashSetFactory;
import tvla.util.SingleSet;



/** A class supplying different graph-related algorithms.
 * 
 * @author Roman Manevich
 */
public class GraphUtils {
	public static final boolean xdebug = true;
	
	/** A constant indicating a forward search.
	 */
	public static final boolean FORWARD = true;

	/** A constant indicating a backward search.
	 */
	public static final boolean BACKWARD = false;
	
	/** A simple graph search algorithm that returns the node reachable
	 * from the specified nodes.
	 * 
	 * @param graph A graph.
	 * @param initial A set of nodes from which the search is performd.
	 * @param direction Specifies whether the search should be a forward
	 * reachability or a backward reachability one.
	 * @param including Specifies whether the search should include the initial
	 * nodes in the result set unconditionally (reflexive transitive
	 * reachability) or only if they are discovered by the search (regular
	 * transitive reachability).
	 * @return The set of nodes reachable from (reach if direction=BACKWARD)
	 * the specified initial nodes.
	 */
	public static Set getReachableNodes(
		Graph graph,
		Collection initial,
		boolean direction,
		boolean including) {			
		Set result = HashSetFactory.make();
		if (including) {
			result.addAll(initial);
		}
		
		Set workSet = HashSetFactory.make(initial);
		while (!workSet.isEmpty()) {
			Iterator workSetIter = workSet.iterator();
			Object node = workSetIter.next();
			workSetIter.remove();
			
			Collection neighbors = null;
			if (direction) {
				neighbors = graph.getOutgoingNodes(node);					
			}
			else {
				neighbors = graph.getIncomingNodes(node);
			}
			for (Iterator neighborIter = neighbors.iterator();
			neighborIter.hasNext(); ) {
				Object neighbor = neighborIter.next();
				if (!result.contains(neighbor)) {
					result.add(neighbor);
					workSet.add(neighbor);
				}
			}
		}
				
		return result;
	}
	

	/**
	 * Iterates over the nodes in graph g that are reachable from node, starting 
	 * from node start in a dfs order if dfs is true of bfs order otherwise.
	 * Pre: all nodes in g a re reach
	 * @param g The input graph
	 * @param node The node from which the search starts
	 * @param dfs true means DFS and false means BFS
	 * @param forward true means forward search and false
	 * means backward search
	 * @return An iterator over the set of reachable nodes.
	 */
	public static Iterator orderedIterator(Graph g, Object start, boolean dfs, boolean forward) {
		assert(g.containsNode(start));
		assert(start != null);
		
		ArrayList output = new ArrayList(g.getNumberOfNodes());
		LinkedList worklist = new LinkedList();
		worklist.add(start);
		
		while (!worklist.isEmpty()) {
			Object current = dfs ? worklist.removeLast() : worklist.removeFirst();
			if (output.contains(current)) 
				continue;
			
			output.add(current);
			Collection nexts = forward ? g.getOutgoingNodes(current) : g.getIncomingNodes(current);
			Iterator nextItr = nexts.iterator();
			while (nextItr.hasNext()) {
				Object nextNode = nextItr.next();
				if (!output.contains(nextNode))
					worklist.addLast(nextNode);					
			}
		}
		
		if (xdebug) {
			Collection reachable = 
				GraphUtils.getReachableNodes(
					g,new SingleSet(true,start),true,true);
			assert(output.containsAll(reachable));
			assert(reachable.containsAll(output));
		}
		
		return output.iterator();
	}
	
	public static Iterator dfsIterator(Graph g, Object node) {
		return orderedIterator(g, node, true, FORWARD);
	}
	
	public static Iterator bfsIterator(Graph g, Object node) {
		return orderedIterator(g, node, false, FORWARD);
	}
	
	/** Conducts two BFS searches between <code>start</code> and
	 * <code>end</code> to find a shortest path.
	 * 
	 * @param g A directed graph.
	 * @param start A node in the graph <code>g</code>.
	 * @param end A node in the graph <code>g</code>.
	 * @return A list of nodes, which is the shortest path
	 * from <code>start</code> to <code>end</code>.
	 * In case there is no path between the nodes, the method
	 * returns an empty list.
	 */
	public static List shortestPath(Graph g, Object start, Object end) {
		assert(g.containsNode(start));
		assert(start != null);
		assert(g.containsNode(end));
		assert(end != null);
		
		// The map gives the number of edges from the start node to a given node.
		TObjectIntHashMap nodeToDist = new TObjectIntHashMap(10);
		ArrayList bfsSet = new ArrayList(g.getNumberOfNodes()/4);
		LinkedList worklist = new LinkedList();
		worklist.add(start);
		nodeToDist.put(start, 0);
		
		// Conduct a BFS from start until either hitting end
		// or exploring all nodes, assigning distances to all
		// nodes encountered along the way.
		while (!worklist.isEmpty()) {
			Object current = worklist.removeFirst();
			if (bfsSet.contains(current)) 
				continue;
						
			bfsSet.add(current);			
			if (current == end) // no need to explore far-away nodes.
				break;
			
			assert nodeToDist.contains(current);
			int currDist = nodeToDist.get(current);
			Collection suceesors = g.getOutgoingNodes(current);
			Iterator nextItr = suceesors.iterator();
			while (nextItr.hasNext()) {
				Object nextNode = nextItr.next();
				if (!bfsSet.contains(nextNode)) {
					worklist.addLast(nextNode);
				}
				
				if (!nodeToDist.contains(nextNode))
					nodeToDist.put(nextNode, currDist + 1);
			}
		}
		
		// If end is not in the map it is unreachable from start.
		if (!nodeToDist.containsKey(end))
			return bfsSet;
		
		// Now, dist stores the distance in terms of number
		// of edges from start to end.
		int dist = nodeToDist.get(end);
		
		// Now, select an arbitrary path by going backwards from
		// end to start choosing only nodes of nodes of decreasing
		// distance.
		int currDist = dist;
		Object currNode = end;
		Vector result = new Vector(dist + 1);
		result.setSize(dist + 1);
		while (currDist >= 0) {
			result.add(currDist, currNode);
			Collection incoming = g.getIncomingNodes(currNode);
			for (Object inNode : incoming) {
				if (nodeToDist.containsKey(inNode) &&
						nodeToDist.get(inNode) == currDist - 1) {
					currNode = inNode;
					break;
				}
			}
			--currDist;
		}		
		return result;		
	}

	/** Conducts two BFS searches between <code>start</code> and
	 * <code>end</code> to find a shortest path.
	 * 
	 * @param g A directed graph.
	 * @param start A node in the graph <code>g</code>.
	 * @param end A node in the graph <code>g</code>.
	 * @return A list of edges, which form the shortest path
	 * from <code>start</code> to <code>end</code>.
	 * In case there is no path between the nodes, the method
	 * returns an empty list.
	 */
	public static List<? extends Graph.Edge> shortestPathEdges(Graph g, Object start, Object end) {
		List pathNodes = shortestPath(g, start, end);
		return nodePathToEdgePath(g, pathNodes);
	}

	/** Converts a list of nodes to a list of edges.
	 * 
	 * @param g A graph containing the specified list of nodes.
	 * @param nodes A list of nodes forming a path.
	 * @return A list of edges forming a path.
	 */
	public static List<? extends Graph.Edge> nodePathToEdgePath(Graph g, List nodes) {
		List<Graph.Edge> edges = new Vector<Graph.Edge>(nodes.size());
		
		// Handle degenerate case
		if (nodes.isEmpty())
			return edges;
		
		for (int i = 0; i < nodes.size() - 1; ++i) {
			Graph.Edge e = g.getEdge(nodes.get(i), nodes.get(i+1));
			assert e != null : "GraphUtils.shortestPath did not return a proper path";
			edges.add(e);
		}
		
		return edges;
	}
	
	/** @see tvla.util.graph.Graph#transitiveClosure()
	 */	
	public static Graph transitiveClosure(Graph g) {
		Graph tc = g.shallowCopy();
		
		if (tc.isEmpty())
			return tc;
		
		// Iterating over all the nodes, 
		Iterator nodeItr = tc.getNodes().iterator();
		Set srcNodeWrapper = new SingleSet(true);
		while (nodeItr.hasNext()) {
			Object srcNode = nodeItr.next();
			srcNodeWrapper.add(srcNode);
			Set reachableNodes = GraphUtils.getReachableNodes(g,srcNodeWrapper,true,false);
			
			// populating the tc graph with edges to reachable nodes
			if (!reachableNodes.isEmpty()) {				
				Iterator dstNodeItr = reachableNodes.iterator();
				while (dstNodeItr.hasNext()) {
					Object dstNode = dstNodeItr.next();
					tc.addEdge(srcNode,dstNode);
				}
			}
			
			srcNodeWrapper.clear();
		}

		return tc;
	}
	
	
	public UnmodifiableGraph unmodifiableGraph(Graph g) {
		return new UnmodifiableGraph(g);
	}
	
	public UnmodifiableBipartiteGraph unmodifiableBipartiteGraph(BipartiteGraph g) {
		return new UnmodifiableBipartiteGraph(g);
	}

	
	/////////////////////////////////////////////////////////////
	////                   INNER  CLASSES                    ////    
	/////////////////////////////////////////////////////////////

	public static class UnmodifiableGraph implements Graph {
		protected final Graph backingGraph;
		
		public UnmodifiableGraph(Graph graph) {
			backingGraph = graph;
		}
		
		public boolean isEmpty() {
			return backingGraph.isEmpty();
		}

		public Collection getNodes() {
			return backingGraph.getNodes();
		}
		
		public Collection getEdges() {
			return backingGraph.getEdges();
		}
	
		public int getNumberOfNodes() {
			return backingGraph.getNumberOfNodes();
		}

		public int getNumberOfEdges() {
			return backingGraph.getNumberOfEdges();
		}

	    public int getInDegree(Object node) {
	    	return backingGraph.getInDegree(node);
	    }

	    public int getOutDegree(Object node) {
	    	return backingGraph.getOutDegree(node);
	    }
	    
	    public int getDegree(Object node) {
	    	return backingGraph.getDegree(node);
	    }
	    
		public Collection getOutgoingEdges(Object node) {
			return Collections.unmodifiableCollection(
					backingGraph.getOutgoingEdges(node));
		}
		
		public Collection getOutgoingNodes(Object node) {
			return Collections.unmodifiableCollection(
					backingGraph.getOutgoingNodes(node));
		}

		public Collection getIncomingEdges(Object node) {
			return Collections.unmodifiableCollection(
					backingGraph.getIncomingEdges(node));
		}

		public Collection getIncomingNodes(Object node) {
			return Collections.unmodifiableCollection(
					backingGraph.getIncomingNodes(node));
		}

		public boolean containsNode(Object node) {
			return backingGraph.containsNode(node);
		}
		
		public boolean containsEdge(Object from, Object to) {
			return backingGraph.containsEdge(from, to);
		}

		public boolean containsEdge(Object from, Object to, Object label) {
			return backingGraph.containsEdge(from, to, label);
		}

		public Edge getEdge(Object from, Object to) {
			return backingGraph.getEdge(from, to);
		}

		
		public Graph shallowCopy() {
			return new UnmodifiableGraph(backingGraph.shallowCopy());
		}
		
		public boolean addNode(Object node) {
		    throw new UnsupportedOperationException();
		}
		
		public boolean  addEdge(Object from, Object to) {
			throw new UnsupportedOperationException();
		}
		
		public boolean addEdge(Object from, Object to, Object edgeInfo) {
		    throw new UnsupportedOperationException();
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

		public boolean removeEdge(Object from, Object to, Object label) {
		    throw new UnsupportedOperationException();
		}

		public boolean removeAllNodes(Collection nodes) {
		    throw new UnsupportedOperationException();
		}

		public boolean removeAllEdges(Collection edges) {
		    throw new UnsupportedOperationException();
		}
		
		public boolean retainAllEdges(Collection c) {
			throw new UnsupportedOperationException();
		}
		
		public boolean retainAllNodes(Collection c) {
			throw new UnsupportedOperationException();
		}

		public Set getEdgesInfo(Object from, Object to) {
		    throw new UnsupportedOperationException();
		}
		
	    public void mergeInto(Object fromNode, Object toNode) {
			throw  new UnsupportedOperationException();    	
	    }
		
		
		public void clear() {
			throw new UnsupportedOperationException();
		}
	}

	public static class UnmodifiableBipartiteGraph 
			extends UnmodifiableGraph
			implements BipartiteGraph {		
		public UnmodifiableBipartiteGraph(Graph graph) {
			super(graph);
		}


//		public boolean edgeExists(Object from, Object to) {
//			return ((BipartiteGraph) backingGraph).edgeExists(from,to);
//		}

//		public Set getEdgesInfo(Object from, Object to) {
//			return Collections.unmodifiableSet(
//					((BipartiteGraph) backingGraph).getEdgesInfo(from,to));					
//		}
//
		public Collection getSources() {
			return Collections.unmodifiableCollection(
					((BipartiteGraph) backingGraph).getSources());					
		}

		public Collection getDestinations() {
			return Collections.unmodifiableCollection(
					((BipartiteGraph) backingGraph).getDestinations());					

		}
		
		public boolean containsSource(Object src) {
			return ((BipartiteGraph) backingGraph).containsSource(src);
		}

		public boolean containsDestination(Object dst) {
			return ((BipartiteGraph) backingGraph).containsDestination(dst);			
		}
		
		
		public BipartiteGraph copy() {
			return new UnmodifiableBipartiteGraph(
					((BipartiteGraph) backingGraph).copy());
		}		
		
		public boolean addSourceNode(Object node) {
		    throw new UnsupportedOperationException();
		}

		public boolean addDestinatonNode(Object node) {
		    throw new UnsupportedOperationException();
		}

		public boolean addSourceNode(Object node, Object info) {
		    throw new UnsupportedOperationException();
		}
		
		public boolean addDestinatonNode(Object node, Object info) {
		    throw new UnsupportedOperationException();
		}

		public boolean addEdge(Object from, Object to) {
		    throw new UnsupportedOperationException();
		}

		public boolean addEdge(Object from, Object to, Object edgeInfo) {
		    throw new UnsupportedOperationException();
		}
		
		public boolean subtract(BipartiteGraph other) {
		    throw new UnsupportedOperationException();			
		}

	}
}
