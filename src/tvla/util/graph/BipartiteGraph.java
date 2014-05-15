/*
 * Created on Mar 21, 2004
 *
 */
package tvla.util.graph;

import java.util.Collection;

/** 
 * An implementation of the mutable graph interface based on hash tables
 * for bipartite graphs.
 * 
 * TODO check that the graph is really bipartaite.
 * TODO improve implementation.
 * 
 * @author Noam Rinetzky 
 */
public interface BipartiteGraph extends Graph {
	////////////////////////////////////////
	////////        MUTATORS         /////// 
	////////////////////////////////////////

	public boolean addSourceNode(Object node);

	public boolean addDestinatonNode(Object node);

//	public void addSourceNode(Object node, Object info); 
	
//	public void addDestinatonNode(Object node, Object info); 

	public boolean addEdge(Object from, Object to); 

	public boolean addEdge(Object from, Object to, Object edgeInfo);
		
	////////////////////////////////////////
	////////        ACCESSORS        /////// 
	////////////////////////////////////////

//	public boolean edgeExists(Object from, Object to);

//	public Set getEdgesInfo(Object from, Object to);

	public Collection getSources();

	public Collection getDestinations();

	public boolean containsSource(Object src);

	public boolean containsDestination(Object dst);
	
	
	public BipartiteGraph copy();
}