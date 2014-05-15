/*
 * Created on Mar 21, 2004
 *
 */
package tvla.util.graph;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.SingleSet;


public class HashFlowGraph extends HashGraph implements FlowGraph { 
	protected final Set sources;
	protected final Set sinks;
	
	///////////////////////////////////////////////
	//////            CONSTRUCTORS          ///////
	///////////////////////////////////////////////
	
	HashFlowGraph() {
		super();
		sources = HashSetFactory.make();
		sinks = HashSetFactory.make();
	}

	protected HashFlowGraph(HashFlowGraph graph) {
		super(graph);
		sources = HashSetFactory.make(graph.sources);
		sinks = HashSetFactory.make(graph.sinks);		
	}

	
	/** Constructs an empty graph with an hint for the number of nodes.
	 */
	public HashFlowGraph(int numOfNodes, float loadCapacity) {
		super(numOfNodes, loadCapacity);
		sources = HashSetFactory.make();
		sinks = HashSetFactory.make();
	}

	/** Constructs an empty graph with an hints for the number of nodes
	 * and expected in-degree and out-degree.
	 */
//	public HashFlowGraph(int numOfNodes, float loadCapacity, int inDegree, int outDegree) {
//		super(numOfNodes, loadCapacity, inDegree, outDegree);
//		sources = HashSetFactory.make();
//		sinks = HashSetFactory.make();
//	}

	
	///////////////////////////////////////////////
	//////             MUTETORS             ///////
	///////////////////////////////////////////////
	
	public void addToSources(Object node) {
		assert(containsNode(node));
		assert(!sinks.contains(node));
		sources.add(node);
	}

	public void addToSinks(Object node) {
		assert(containsNode(node));
		assert(!sources.contains(node));
		sinks.add(node);		
	}
	

	///////////////////////////////////////////////
	//////             ACCESSORS            ///////
	///////////////////////////////////////////////
	
	public Collection getSources() {
		return sources;
	}
	
	public Collection getSinks() {
		return sinks;		
	}

	public Collection getReachableSinks(Object source) {
		assert(source != null);
		assert(sources.contains(source));
		
		SingleSet initial = new SingleSet(true, source);
		Collection col = GraphUtils.getReachableNodes(
				this, initial, true, false); // forward, do not include initial node
		
		col.retainAll(sinks);
		
		return col;
	}
	
	public Collection getReachingSources(Object sink) {
		assert(sink != null);
		assert(sinks.contains(sink));
		
		SingleSet initial = new SingleSet(true, sink);
		Collection col = GraphUtils.getReachableNodes(
				this, initial, false, false); // backward, do not include initial node
		
		col.retainAll(sources);

		return col;		
	}
	
	public Map sourcesToSinks() {
		Map map = HashMapFactory.make(sources.size());
		Iterator srcItr = sources.iterator();
		while (srcItr.hasNext()) {
			Object source = srcItr.next();
			Collection reachesSinks = getReachableSinks(source);
			map.put(source, reachesSinks);
		}
		
		return map;		
	}

	public Map sinksToSources() {
		Map map = HashMapFactory.make(sources.size());
		Iterator sinkItr = sinks.iterator();
		while (sinkItr.hasNext()) {
			Object sink = sinkItr.next();
			Collection reachingSources= getReachableSinks(sink);
			map.put(sink, reachingSources);
		}
		
		return map;		
	}
	
	
	///////////////////////////////////////////////
	//////                MISC              ///////
	///////////////////////////////////////////////
	
	public HashFlowGraph copy() {
		return new HashFlowGraph(this); 
	}

	///////////////////////////////////////////////
	//////            HashGraph              //////
	///////////////////////////////////////////////


	/** @see tvla.util.graph.Graph#addNode(java.lang.Object,java.lang.Object)
	 */

//	public void addNode(Object node, Object info) {
//		if (!nodeToOutgoing.containsKey(node)) {
//			nodeToOutgoing.put(node, new LinkedHashSet(initialOutDegree));
//			nodeToIncoming.put(node, new LinkedHashSet(initialInDegree));
//			nodeToInfo.put(node,info);
//		}
//	}
	

	/** @see tvla.util.graph.Graph#addEdge(java.lang.Object, java.lang.Object)
	 */
	public boolean addEdge(Object from, Object to) {
		return addEdge(from, to, null);
	}
	
	/** @see tvla.util.graph.Graph#addEdge(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	public boolean addEdge(Object from, Object to, Object edgeInfo) {
		assert from != null && to != null;
		assert(!sources.contains(to));
		assert(!sinks.contains(from));
		
		return super.addEdge(from,to,edgeInfo);
	}

	/** @see tvla.util.graph.Graph#removeNode(java.lang.Object)
	 */
	public boolean removeNode(Object node) {
		sources.remove(node);
		sinks.remove(node);
		return super.removeNode(node);
	}
		
	
	/** @see tvla.util.graph.Graph#removeAllNodes(java.util.Collection)
	 */
	public boolean  removeAllNodes(Collection nodes) {
		sources.removeAll(nodes);
		sinks.removeAll(nodes);
		return super.removeAllNodes(nodes);
	}


	protected boolean addEdge(Edge edge) {
		assert(!sources.contains(edge.getDestination()));
		assert(!sinks.contains(edge.getSource()));
		
		return super.addEdge(edge);
	}
		
	public void clear() {
		super.clear();
		sources.clear();	
		sinks.clear();
	}
}
