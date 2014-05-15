package org._3pq.jgrapht.graph;

import org._3pq.jgrapht.Edge;
import org._3pq.jgrapht.EdgeFactory;
import org._3pq.jgrapht.graph.SimpleGraph;
import java.util.List;
import java.util.Iterator;

public class BipartiteGraph extends SimpleGraph {
	public BipartiteGraph( EdgeFactory ef ) {
		super( ef );
	}

	public BipartiteGraph(  ) {
		super(  );
	}

	public Edge addEdge( Object sourceVertex, Object targetVertex ) {
        // Verify no triangles created by new edge.
        if( containsVertex( sourceVertex ) && containsVertex( targetVertex ) ) { 
            List edges = edgesOf( sourceVertex );
            Iterator i = edges.iterator(  );
            while( i.hasNext(  ) ) {
                Edge edge = (Edge) i.next(  );
                if( containsEdge( edge.oppositeVertex( sourceVertex ), targetVertex ) )
                    throw new IllegalArgumentException( "bipartite property violated" );
            }
        }
        
		// Add edge with superclass method.
		return super.addEdge( sourceVertex, targetVertex );
	}

}
