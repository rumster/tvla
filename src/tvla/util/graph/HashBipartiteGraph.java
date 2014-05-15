/*
 * Created on Mar 21, 2004
 *
 */
package tvla.util.graph;

import java.util.Collection;
import java.util.Set;

import tvla.util.HashSetFactory;

/** 
 * An implementation of the mutable graph interface based on hash tables
 * for bipartite graphs.
 * 
 * FIXME inherits implementation hack! 
 * TODO check that the graph is really bipartaite.
 * TODO improve implementation.
 * 
 * @author Noam Rinetzky 
 */
public final class HashBipartiteGraph extends HashGraph implements BipartiteGraph {
	private final Set sources; 
	private final Set destinations; 

	///////////////////////////////////////////////
	//////            CONSTRUCTORS          ///////
	///////////////////////////////////////////////

	HashBipartiteGraph() {
		super();
		sources = HashSetFactory.make();
		destinations = HashSetFactory.make();		
	}

	protected HashBipartiteGraph(HashBipartiteGraph graph) {
		super(graph);
		sources = HashSetFactory.make(graph.sources);
		destinations = HashSetFactory.make(graph.destinations);
	}	
	

	///////////////////////////////////////////////
	//////             MUTETORS             ///////
	///////////////////////////////////////////////
	
	public boolean addSourceNode(Object node) {
		boolean ret = super.addNode(node);
		assert(!destinations.contains(node));
		sources.add(node);
		
		return ret;
	}

	public boolean  addDestinatonNode(Object node) {
		boolean ret = super.addNode(node);
		assert(!sources.contains(node));
		destinations.add(node);
		
		return ret;
	}

	public boolean addNode(Object node) {
		throw new UnsupportedOperationException();
	}

	/** @see tvla.util.graph.Graph#addNode(java.lang.Object,java.lang.Object)
	 */
//	public void addSourceNode(Object node, Object info) {
//		super.addNode(node,info);
//		assert(!destinations.contains(node));
//		sources.add(node);
//	}

//	public void addDestinatonNode(Object node, Object info) {
//		super.addNode(node,info);
//		assert(!sources.contains(node));
//		destinations.add(node);
//	}

	public void addNode(Object node, Object info) {
		throw new UnsupportedOperationException();
	}
	

	/** @see tvla.util.graph.Graph#addEdge(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	public boolean addEdge(Object from, Object to, Object edgeInfo) {
		assert(from != null && to != null);
		assert(!destinations.contains(from));
		assert(!sources.contains(to));
		
		HashEdge newEdge = new HashEdge(from, to, edgeInfo);
		return addEdge(newEdge);
	}
	
	public boolean  addEdge(Object from, Object to) {
		assert(from != null && to != null);
		assert(!destinations.contains(from));
		assert(!sources.contains(to));

		return addEdge(from,to,null);
	}
		
	/** @see tvla.util.graph.Graph#removeNode(java.lang.Object)
	 */
	public boolean removeNode(Object node) {
		throw new UnsupportedOperationException();
	}
		
	
	/** @see tvla.util.graph.Graph#removeEdge(tvla.util.graph.Graph.Edge)
	 */
	public boolean removeEdge(Edge edge) {
		throw new UnsupportedOperationException();
	}

	/** @see tvla.util.graph.Graph#removeEdge(java.lang.Object, java.lang.Object)
	 */
	public boolean removeEdge(Object from, Object to) {
		throw new UnsupportedOperationException();
	}

	/** @see tvla.util.graph.Graph#removeAllNodes(java.util.Collection)
	 */
	public boolean removeAllNodes(Collection nodes) {
		sources.removeAll(nodes);
		destinations.removeAll(nodes);
		
		return super.removeAllNodes(nodes);
	}

//	/** @see tvla.util.graph.Graph#removeAllEdges(java.util.Collection)
//	 */
//	public boolean removeAllEdges(Collection edges) {
//		return super.removeAllEdges(edges);
//	}

	protected boolean addEdge(Edge edge) {
		assert(sources.contains(edge.getSource()));
		assert(destinations.contains(edge.getDestination()));

		return super.addEdge(edge);
	}

	public void clear() {
		super.clear();
		sources.clear();	
		destinations.clear();
	}
	
	/*
	public boolean subtract(BipartiteGraph other) {
		boolean changed = false;
		
		if (other == this)
			if (!isEmpty()) {
				clear();
				return true;
			}
			else {
				assert(sources.isEmpty());
				assert(sinks.isEmpty());
				return false;
			}

		if (!nodes.con)
		removeAllEdges(other.getEdges());
		
		
		other.
		sources.
		if (sources.)
	}
	*/
	
	///////////////////////////////////////////////
	//////             ACCESSORS            ///////
	///////////////////////////////////////////////

//	public boolean edgeExists(Object from, Object to) {
//		return (null != getEdgesInfo(from,to));
//	}

	public Collection getSources() {
		return sources;
	}

	public boolean containsSource(Object src){ 
		return sources.contains(src);
	}

	public Collection getDestinations() {
		return destinations;
	}

	public boolean containsDestination(Object dst) {
		return destinations.contains(dst);
	}


	public BipartiteGraph copy() {
		return new HashBipartiteGraph(this);
	}


}
