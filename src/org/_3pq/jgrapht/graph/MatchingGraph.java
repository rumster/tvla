package org._3pq.jgrapht.graph;

import org._3pq.jgrapht.Edge;
import org._3pq.jgrapht.EdgeFactory;
import org._3pq.jgrapht.Graph;
import org._3pq.jgrapht.graph.SimpleGraph;
import org._3pq.jgrapht.util.FibonacciHeap;
import org._3pq.jgrapht.util.RedundancyStats;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;

public class MatchingGraph extends SimpleGraph {
    private final boolean LOG_ENABLED = false;

    protected int m_defWMin;
    protected int m_defWMax;
    private Map m_WMin;
    private Map m_WMax;
    private RcDegMap m_rcDegMap;

    
	public MatchingGraph(  ) {
		this( 1, 1 );
	}

	public MatchingGraph( EdgeFactory ef ) {
		this( 1, 1, ef );
	}

    public MatchingGraph( int defWMin, int defWMax ) {
        super(  );
        WInit( defWMin, defWMax );
    }

	public MatchingGraph( int defWMin, int defWMax, EdgeFactory ef ) {
		super( ef );
        WInit( defWMin, defWMax );
	}

    // Special copy-constructor which assumes a valid reference graph as input.
    public MatchingGraph( MatchingGraph g ) {
        this( g.m_defWMin, g.m_defWMax );
        
        for( Iterator i = g.vertexSet(  ).iterator(  ); i.hasNext(  ); ) {
            Object v = i.next(  );
            addVertex( v, g.getWMin( v ), g.getWMax( v ) );
        }

        addAllEdges( g.edgeSet(  ) );
    }

    private void WInit( int defWMin, int defWMax ) {
        if( defWMin < 0 || defWMax < defWMin )
            throw new IllegalArgumentException( "invalid default WMin/WMax values" );

        m_defWMin = defWMin;
        m_defWMax = defWMax;
        m_WMin = new HashMap(  );
        m_WMax = new HashMap(  );
        m_rcDegMap = new RcDegMap(  );
    }

    
    
    private class RcDegMap {
        private FibonacciHeap m_heap;
        private Map m_entries;

        RcDegMap(  ) {
            m_heap = new FibonacciHeap(  );
            m_entries = new HashMap(  );
        }

        public boolean isEmpty(  ) {
            return m_heap.isEmpty(  );
        }

        public void add( Object v ) {
            int newRcDeg = rcDeg( v );
            rcDegEntry entry = new rcDegEntry( v, newRcDeg );
            m_entries.put( v, entry );
            m_heap.insert( entry, (double) newRcDeg );
        }

        public void update( Object v ) {
            int newRcDeg = rcDeg( v );
            rcDegEntry entry = (rcDegEntry) m_entries.get( v );
            int oldRcDeg = entry.getRcDeg(  );
            if( newRcDeg < oldRcDeg ) {
                m_heap.decreaseKey( entry, (double) newRcDeg );
            }
            else if( newRcDeg > oldRcDeg ) {
                m_heap.delete( entry );
                entry = new rcDegEntry( v, newRcDeg );
                m_entries.put( v, entry );
                m_heap.insert( entry, (double) newRcDeg );
            }
        }

        public int get( Object v ) {
            rcDegEntry entry = (rcDegEntry) m_entries.get( v );
            return ((int) entry.getKey(  ));
        }
        
        public Object getMin(  ) {
            rcDegEntry entry = (rcDegEntry) m_heap.min(  );
            return entry.getVertex(  );
        }

        public void remove( Object v ) {
            rcDegEntry entry = (rcDegEntry) m_entries.remove( v );
            m_heap.delete( entry );
        }

        public Object removeMin(  ) {
            rcDegEntry entry = (rcDegEntry) m_heap.removeMin(  );
            Object v = entry.getVertex(  );
            m_entries.remove( v );
            return v;
        }
    }

    private static class rcDegEntry extends FibonacciHeap.Node {
        private Object m_vertex;
        
        rcDegEntry( Object vertex, int rcDeg ) {
            super( (double) rcDeg );
            m_vertex = vertex;
        }

        Object getVertex (  ) {
            return m_vertex;
        }
        
        int getRcDeg(  ) {
            return ((int) getKey(  ));
        }
    }
    

    private int binom( int n, int m ) {
        if( n < 0 || m < 0 || n < m )
            return 0;

        int d = n - m;
        if( m > d )
            m = d;
        
        int b = 1;
        d = 1;
        while( m-- > 0 ) {
            b *= n--;
            b /= d++;
        }
        
        return b;
    }

    private int rcDeg( Object v ) {
        int WMin = getWMin( v );
        int WMax = getWMax( v );
        int deg = degreeOf( v );
        
        if( deg < WMin )
            return 0;
        if( deg < WMax )
            WMax = deg;
        
        int rcDeg = 0;
        for( int d = WMin; d <= WMax; d++ ) {
            rcDeg += binom( deg, d );
        }

        return rcDeg;
    }

    
    public boolean addVertex( Object v ) {
        return addVertex( v, m_defWMin, m_defWMax );
    }

    public boolean addVertex( Object v, int WMin, int WMax ) {
        if( ! containsVertex( v ) ) {
            if( WMin < 0 || WMax < WMin )
                throw new IllegalArgumentException( "invalid WMin/WMax values" );

            super.addVertex( v );
            m_WMin.put( v, new Integer( WMin ) );
            m_WMax.put( v, new Integer( WMax ) );
            m_rcDegMap.add( v );
            return true;
        }
        return false;
    }

    public boolean removeVertex( Object v ) {
        if( super.removeVertex( v ) ) {
            m_WMin.remove( v );
            m_WMax.remove( v );
            m_rcDegMap.remove( v );
            return true;
        }
        return false;
    }

    public Edge addEdge( Object sourceVertex, Object targetVertex ) {
        Edge e = super.addEdge( sourceVertex, targetVertex );
        if( e != null ) {
            m_rcDegMap.update( sourceVertex );
            m_rcDegMap.update( targetVertex );
        }
        return e;
    }

    public boolean addEdge( Edge e ) {
        if( super.addEdge( e ) ) {
            m_rcDegMap.update( e.getSource(  ) );
            m_rcDegMap.update( e.getTarget(  ) );
        }
        return false;
    }

    public Edge removeEdge( Object sourceVertex, Object targetVertex ) {
        Edge e = super.removeEdge( sourceVertex, targetVertex );
        if( e != null ) {
            m_rcDegMap.update( sourceVertex );
            m_rcDegMap.update( targetVertex );
        }
        return e;
    }

    public boolean removeEdge( Edge e ) {
        if( super.removeEdge( e ) ) {
            m_rcDegMap.update( e.getSource(  ) );
            m_rcDegMap.update( e.getTarget(  ) );
            return true;
        }
        return false;
    }

    public boolean setWMin( Object v, int WMin ) {
        if( ! m_WMin.containsKey( v ) )
            return false;
        
        if( WMin < 0 || getWMax( v ) < WMin )
            throw new IllegalArgumentException( "invalid WMin value" );
        
        m_WMin.put( v, new Integer( WMin ) );
        m_rcDegMap.update( v );
        return true;
    }

    public boolean setWMax( Object v, int WMax ) {
        if( ! m_WMax.containsKey( v ) )
            return false;
        
        if( WMax < getWMin( v ) )
            throw new IllegalArgumentException( "invalid WMax value" );
        
        m_WMax.put( v, new Integer( WMax ) );
        m_rcDegMap.update( v );
        return true;
    }

    public int getWMin( Object v ) {
        if( m_WMin.containsKey( v ) )
            return ((Integer) m_WMin.get( v )).intValue(  );
        return -1;
    }

    public int getWMax( Object v ) {
        if( m_WMax.containsKey( v ) )
            return ((Integer) m_WMax.get( v )).intValue(  );
        return -1;
    }

    private void print( String s ) {
        if( LOG_ENABLED )
            System.out.print( s );
    }
    
    private void println( String s ) {
        if( LOG_ENABLED )
            System.out.println( s );
    }
    
    protected boolean decreaseWMinMax( Object v ) {
        if( m_WMin.containsKey( v )) {
            int WMin = ((Integer) m_WMin.get( v )).intValue(  ) - 1;
            if( WMin < 0 )
                WMin = 0;
            int WMax = ((Integer) m_WMax.get( v )).intValue(  ) - 1;
            if( WMax < WMin )
                WMax = WMax;

            m_WMin.put( v, new Integer( WMin ) );
            m_WMax.put( v, new Integer( WMax ) );
            m_rcDegMap.update( v );

            return true;
        }
        return false;
    }


    protected Set AbMatchEnumHelper( Set M, int recdepth,
            int currWeight, RedundancyStats stats ) {
        // Initialize set of generalized perfect matchings.
        Set L = new HashSet(  );
        Object u = null;
        int uDeg = 0;
        int uWMin = 0;

//        println( "=> gapmh: start, recdepth = " + recdepth
//                 + ", currWeight = " + currWeight );

        // Eliminate isolated nodes whose match minimum is satisfied.
        while( ! vertexSet(  ).isEmpty(  ) ) {
            u = m_rcDegMap.getMin(  );
            uWMin = getWMin( u );
            uDeg = degreeOf( u );
            
            if( uDeg < uWMin || uDeg > 0 )
                break;

            removeVertex( u );
            currWeight++;
//            println( "=> gapmh: removed isolated vertex = " + u.toString(  ) );
        }

        if( vertexSet(  ).isEmpty(  ) ) {
//            println( "=> gapmh: graph empty, dumping: " + M.toString() );
            
            L.add( new HashSet( M ) );
        }
        else if( uDeg >= uWMin ) {
//            println( "=> gapmh: minimal vertex = " + u.toString(  )
//                     + ", rcdeg = " + m_rcDegMap.get( u ) );
            
            // Select an arbitrary edge of the RC-degree-minimal vertex.
            Edge e = (Edge) edgesOf( u ).iterator(  ).next(  );
            Object v = e.oppositeVertex( u );
            int vDeg = degreeOf( v );

//            println( "=> gapmh: selected edge = " + e.toString(  ) );
            
            // Recurse exclusive selected edge.
            if( uDeg > uWMin ) {
//                println( "=> gapmh: preparing exclusive recursion" );

                MatchingGraph GTag = new MatchingGraph( this );
                GTag.removeEdge( e );
                if( uDeg == 1 ) {
                    GTag.removeVertex( u );
//                    println( "=> gapmh: removed vertex = " + u.toString(  ) );
                }
                if( vDeg == 1 ) {
                    GTag.removeVertex( v );
//                    println( "=> gapmh: removed vertex = " + v.toString(  ) );
                }

//                println( "=> gapmh: exclusive recursion: " + GTag.toString(  ) );
                Set tmp = GTag.AbMatchEnumHelper( new HashSet( M ),
                                recdepth + 1, 0, stats );
                if ( stats != null )
                    stats.update( currWeight + 1, tmp.size(  ) == 0 );
                L.addAll( tmp );
//                println( "=> gapmh: exclusive recursion done" );
            }
            
            // Recurse inclusive selected edge.
//            println( "=> gapmh: preparing inclusive recursion" );
            
            MatchingGraph GTag = new MatchingGraph( this );
            GTag.removeEdge( e );
            int nextWeight = 0;

            List vertices = new LinkedList(  );
            vertices.add( u );
            vertices.add( v );
            for( Iterator i = vertices.iterator(  ); i.hasNext(  ); ) {
                Object w = i.next(  );
                GTag.decreaseWMinMax( w );

                if( GTag.getWMax( w ) == 0 ||
                    (GTag.degreeOf( w ) == 0 && GTag.getWMin( w ) == 0) )
                {
                    List edges = new LinkedList( GTag.edgesOf( w ) );
                    for( Iterator j = edges.iterator(  ); j.hasNext(  ); ) {
                        Edge edge = (Edge) j.next(  );
                        Object z = edge.oppositeVertex( w );

                        GTag.removeEdge( edge );
//                        println( "=> gapmh: removed edge = " + edge.toString(  ) );
                        if( GTag.degreeOf( z ) == 0 && GTag.getWMin( z ) == 0 )
                        {
                            GTag.removeVertex( z );
                            nextWeight++;
//                            println( "=> gapmh: removed isolated vertex = "
//                                     + z.toString(  ) );
                        }
                    }

                    GTag.removeVertex( w );
                    nextWeight++;
//                    println( "=> gapmh: removed vertex = " + w.toString(  ) );
                }
            }
                
            Set MTag = new HashSet( M );
            MTag.add( e );

//            println( "=> gapmh: inclusive recursion: " + GTag.toString(  ) );
            Set tmp = GTag.AbMatchEnumHelper( MTag, recdepth + 1,
			    nextWeight, stats );
            if ( stats != null )
                stats.update( currWeight + 1, tmp.size(  ) == 0 );
            L.addAll( tmp );
//            println( "=> gapmh: inclusive recursion done" );
        }
        else {
//            println( "=> gapmh: constraint violation, breaking recursion" );
            if ( stats != null )
                stats.update( currWeight, true );
        }

//        println( "=> gapmh: end" );

        return L;
            
    }
    
    public Set AbMatchEnum( RedundancyStats stats ) {
		// println( "=> gapm: this = " + toString() );

        return AbMatchEnumHelper( new HashSet(  ), 0, 0, stats );
    }

    public Set AbMatchEnum(  ) {
        return AbMatchEnum( null );
    }

	public String toString(  ) {
		return "<" + super.toString(  ) + ", " + m_WMin.toString(  )
			+ ", " + m_WMax.toString(  ) + ">";
	}
}
