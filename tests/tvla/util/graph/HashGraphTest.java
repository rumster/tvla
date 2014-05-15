/*
 * Created on 26/11/2004
 *
 */
package tvla.util.graph;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

/**
 * A unit test for HashGraph.
 * 
 * TODO decpule this test from the actual implementation of the graph
 * (HashGraph in this case).
 * 
 * @author Roman Manevich
 *  
 */
public class HashGraphTest extends TestCase {
    protected static Collection testGraphInfos;

    static {
        createTestGraphInfoSuite();
    }

    /** Creates a collection of graphs of various sizes and properties and
     * stores the result in <code>testGraphInfos</code>.
     */
    protected static void createTestGraphInfoSuite() {
        testGraphInfos = new ArrayList(10);
        
        // a full graph with 5 nodes
        testGraphInfos.add(new GraphInfo(new BigInteger("33554431")));
        // an arbitrary graph
        testGraphInfos.add(new GraphInfo(new BigInteger("43434545444443414")));
        // an arbitrary graph
        testGraphInfos.add(new GraphInfo(new BigInteger("0001000000100001")));
        // an arbitrary graph
        testGraphInfos.add(new GraphInfo(new BigInteger(
                "99999999999999999999999999999999999")));
        // a random graph with 8 nodes
        testGraphInfos.add(new GraphInfo(new BigInteger(64, new Random(
                235462614))));
        // a random graph with 9 nodes
        testGraphInfos.add(new GraphInfo(new BigInteger(81, new Random(7346))));
        // a random graph with 10 nodes
        testGraphInfos
                .add(new GraphInfo(new BigInteger(100, new Random(7346))));
    }

//    public void testRemoveAllEdges() {
//        Graph graph = new GraphInfo(new BigInteger(100, new Random(7346))).graph;
//
//        for (Iterator nodeIter = graph.getNodes().iterator(); nodeIter
//                .hasNext();) {
//            Object node = nodeIter.next();
//
//            graph.removeAllEdges(graph.getIncomingEdges(node));
//            assertTrue("Incoming edges of node " + node
//                    + " should be empty but getIncomingEdges returned: "
//                    + graph.getIncomingEdges(node), graph
//                    .getIncomingEdges(node).isEmpty());
//            assertTrue("In degree of node " + node + " should be 0 but getInDegree returned: "
//                    + graph.getInDegree(node), graph.getInDegree(node) == 0);
//
//            graph.removeAllEdges(graph.getOutgoingEdges(node));
//            assertTrue("Outgoing edges of node " + node
//                    + " should be empty but getOutgoingEdges returned: "
//                    + graph.getOutgoingEdges(node), graph
//                    .getOutgoingEdges(node).isEmpty());
//            assertTrue("out degree of node " + node + " should be 0 but getOutDegree returned: "
//                    + graph.getOutDegree(node), graph.getOutDegree(node) == 0);
//        }
//    }

    public void testHashGraph() {
        assertTrue(equivalentCollections(createEmptyGraph().getNodes(),
                new HashSet()));
        assertTrue(createEmptyGraph().getNumberOfEdges() == 0);
    }

    public void testGetNodes() {
        for (Iterator testGraphIter = testGraphInfos.iterator(); testGraphIter
                .hasNext();) {
            GraphInfo testGInfo1 = (GraphInfo) testGraphIter.next();
            Graph graph = testGInfo1.graph;
            assertTrue("The graph nodes are: " + graph.getNodes()
                    + " but should be: " + testGInfo1.getNodes(),
                    equivalentCollections(graph.getNodes(), testGInfo1
                            .getNodes()));
            //System.out.println(testGInfo1.graph);
        }
    }

    public void testGetOutgoingEdges() {
        Graph g = createEmptyGraph();
        Integer node1 = new Integer(1);
        Integer node2 = new Integer(2);
        Integer node3 = new Integer(3);
        
        Integer edgeLabel1 = new Integer(1);
        Integer edgeLabel2 = new Integer(2);
        Integer edgeLabel3 = new Integer(3);
        
        // {1->2:1, 1->3:2, 3->2:3}
        g.addNode(node1);
        g.addNode(node2);
        g.addNode(node3);
        g.addEdge(node1, node2, edgeLabel1);
        g.addEdge(node1, node3, edgeLabel2);
        g.addEdge(node3, node1, edgeLabel3);
        
        { // 1
            Collection out = g.getOutgoingEdges(node1);
            assertTrue(out.size() == 2);
            Iterator outIter = out.iterator();
            Graph.Edge edge = (Graph.Edge) outIter.next();
            assertTrue(edge.getSource().equals(node1));
            assertTrue(edge.getDestination().equals(node2)
                    && edge.getLabel().equals(edgeLabel1)
                    || edge.getDestination().equals(node3)
                    && edge.getLabel().equals(edgeLabel2));
            edge = (Graph.Edge) outIter.next();
            assertTrue(edge.getSource().equals(node1));
            assertTrue(edge.getDestination().equals(node2)
                    && edge.getLabel().equals(edgeLabel1)
                    || edge.getDestination().equals(node3)
                    && edge.getLabel().equals(edgeLabel2));
        }

        { // 2
            Collection out = g.getOutgoingEdges(node2);
            assertTrue(out.size() == 0);
        }

        { // 3
            Collection out = g.getOutgoingEdges(node3);
            assertTrue(out.size() == 1);
            Iterator outIter = out.iterator();
            Graph.Edge edge = (Graph.Edge) outIter.next();
            assertTrue(edge.getSource().equals(node3));
            assertTrue(edge.getDestination().equals(node1)
                    && edge.getLabel().equals(edgeLabel3));
        }
    }

    public void testGetIncomingEdges() {
        Graph g = createEmptyGraph();
        Integer node1 = new Integer(1);
        Integer node2 = new Integer(2);
        Integer node3 = new Integer(3);
        
        Integer edgeLabel1 = new Integer(1);
        Integer edgeLabel2 = new Integer(2);
        Integer edgeLabel3 = new Integer(3);
        
        // {1->2:1, 3->2:2, 1->3:3}
        g.addNode(node1);
        g.addNode(node2);
        g.addNode(node3);
        g.addEdge(node1, node2, edgeLabel1);
        g.addEdge(node3, node2, edgeLabel2);
        g.addEdge(node1, node3, edgeLabel3);
        
        { // 2
            Collection in = g.getIncomingEdges(node2);
            assertTrue(in.size() == 2);
            Iterator inIter = in.iterator();
            Graph.Edge edge = (Graph.Edge) inIter.next();
            assertTrue(edge.getDestination().equals(node2));
            assertTrue(edge.getSource().equals(node1)
                    && edge.getLabel().equals(edgeLabel1)
                    || edge.getSource().equals(node3)
                    && edge.getLabel().equals(edgeLabel2));
            edge = (Graph.Edge) inIter.next();
            assertTrue(edge.getDestination().equals(node2));
            assertTrue(edge.getSource().equals(node1)
                    && edge.getLabel().equals(edgeLabel1)
                    || edge.getSource().equals(node3)
                    && edge.getLabel().equals(edgeLabel2));
        }

        { // 1
            Collection in = g.getIncomingEdges(node1);
            assertTrue(in.size() == 0);
        }

        { // 3
            Collection in = g.getIncomingEdges(node3);
            assertTrue(in.size() == 1);
            Iterator inIter = in.iterator();
            Graph.Edge edge = (Graph.Edge) inIter.next();
            assertTrue(edge.getDestination().equals(node3));
            assertTrue(edge.getSource().equals(node1)
                    && edge.getLabel().equals(edgeLabel3));
        }
    }

    public void testAddNode() {
        Graph g = createEmptyGraph();
        Integer node1 = new Integer(1);
        Integer node2 = new Integer(2);
        Integer node3 = new Integer(3);
        assertTrue(g.addNode(node1) == false);
        assertTrue(g.getNodes().size() == 1);
        assertTrue(g.addNode(node2) == false);
        assertTrue(g.getNodes().size() == 2);
        assertTrue(g.addNode(node3) == false);
        assertTrue(g.getNodes().size() == 3);
        
        assertTrue(g.addNode(node1) == true);
        assertTrue(g.addNode(node2) == true);
        assertTrue(g.addNode(node3) == true);
        
        // make sure that adding an existing node does not invalidate
        // edges incident on the node.
        g.addEdge(node1, node2);
        g.addEdge(node3, node1);
        g.addNode(node1);
        assertTrue(g.containsEdge(node1, node2));
        assertTrue(g.containsEdge(node3, node1));
    }

    public void testAddEdgeObjectObjectObject() {
        Graph g = createEmptyGraph();
        Integer node1 = new Integer(1);
        Integer node2 = new Integer(2);
        Integer node3 = new Integer(3);
        
        Integer edgeLabel1 = new Integer(1);
        Integer edgeLabel2 = new Integer(2);
        Integer edgeLabel3 = new Integer(3);
        
        // {1->2:1, 1->3:2, 3->2:3}
        g.addNode(node1);
        g.addNode(node2);
        g.addNode(node3);
        g.addEdge(node1, node2, edgeLabel1);
        g.addEdge(node1, node3, edgeLabel2);
        g.addEdge(node3, node1, edgeLabel3);
        
        assertTrue(g.containsEdge(node1, node2, edgeLabel1));
        assertTrue(g.containsEdge(node1, node3, edgeLabel2));
        assertTrue(g.containsEdge(node3, node1, edgeLabel3));
        
        assertTrue(!g.containsEdge(node1, node2, edgeLabel2));
        assertTrue(!g.containsEdge(node1, node2, edgeLabel3));
        assertTrue(!g.containsEdge(node1, node3, edgeLabel1));
        assertTrue(!g.containsEdge(node1, node2, edgeLabel3));
        assertTrue(!g.containsEdge(node3, node2, edgeLabel1));
        assertTrue(!g.containsEdge(node3, node2, edgeLabel2));
    }

    public void testRemoveNode() {
        Graph g = createEmptyGraph();
        Integer node1 = new Integer(1);
        Integer node2 = new Integer(2);
        Integer node3 = new Integer(3);
        
        g.addNode(node1);
        g.addNode(node2);
        g.addNode(node3);
        
        assertTrue(g.removeNode(node1) == true);
        assertTrue(g.getNodes().size() == 2);
        assertTrue(g.removeNode(node2) == true);
        assertTrue(g.getNodes().size() == 1);
        assertTrue(g.removeNode(node3) == true);
        assertTrue(g.getNodes().size() == 0);
        
        assertTrue(g.removeNode(node1) == false);
        assertTrue(g.removeNode(node2) == false);
        assertTrue(g.removeNode(node3) == false);
    }

    /*
     * Class under test for void removeEdge(Edge)
     */
    public void testRemoveEdgeEdge() {
        Graph g = createEmptyGraph();
        Integer node1 = new Integer(1);
        Integer node2 = new Integer(2);
        Integer node3 = new Integer(3);
        
        Integer edgeLabel1 = new Integer(1);
        Integer edgeLabel2 = new Integer(2);
        Integer edgeLabel3 = new Integer(3);
        
        // {1->2:1, 3->2:2, 1->3:3}
        g.addNode(node1);
        g.addNode(node2);
        g.addNode(node3);
        assertTrue(g.addEdge(node1, node2, edgeLabel1));
        assertTrue(g.addEdge(node3, node2, edgeLabel2));
        assertTrue(g.addEdge(node1, node3, edgeLabel3));
        assertTrue(g.addEdge(node1, node2, edgeLabel2));
        
        Iterator out1 = g.getOutgoingEdges(node1).iterator();
        Graph.Edge edge1 = (Graph.Edge) out1.next();
        Graph.Edge edge2 = (Graph.Edge) out1.next();
        Graph.Edge edge3 = (Graph.Edge) out1.next();
        Graph.Edge edge4 = (Graph.Edge) g.getOutgoingEdges(node3).iterator().next();
        
        assertTrue(g.containsEdge(node1, node2, edgeLabel1));
        assertTrue(g.containsEdge(node3, node2, edgeLabel2));
        assertTrue(g.containsEdge(node1, node3, edgeLabel3));
        
        assertTrue(g.getOutDegree(node1) == 3);
        assertTrue(g.removeEdge(edge1));
        assertTrue(g.getOutDegree(node1) == 2);
        assertTrue(g.removeEdge(edge2));
        assertTrue(g.getOutDegree(node1) == 1);
        assertTrue(g.removeEdge(edge3));
        assertTrue(g.getOutDegree(node1) == 0);
        assertTrue(g.removeEdge(edge4));
        
        assertFalse(g.removeEdge(node1, node2, edgeLabel2));
    }

    public void testGetNumberOfEdges() {
        for (Iterator testGraphIter = testGraphInfos.iterator(); testGraphIter
                .hasNext();) {
            GraphInfo testGInfo1 = (GraphInfo) testGraphIter.next();
            Graph graph = testGInfo1.graph;
            assertTrue("The number of graph edges is: "
                    + graph.getNumberOfEdges() + " but getNumberOfEdges returned: "
                    + testGInfo1.numberOfEdges,
                    graph.getNumberOfEdges() == testGInfo1.numberOfEdges);
        }
    }

    public void testGetOutgoingNodes() {
        for (Iterator testGraphIter = testGraphInfos.iterator(); testGraphIter
                .hasNext();) {
            GraphInfo testGInfo = (GraphInfo) testGraphIter.next();
            Graph graph = testGInfo.graph;
            for (Iterator nodeIter = graph.getNodes().iterator(); nodeIter
                    .hasNext();) {
                Object node = nodeIter.next();
                Collection outgoingNodes = graph.getOutgoingNodes(node);
                Collection ginfoOutgoing = testGInfo.getOutgoingNodes(((Integer) node).intValue());
                assertTrue("The outgoing nodes are: " + outgoingNodes
                        + " but should be " + ginfoOutgoing, equivalentCollections(
                                outgoingNodes, ginfoOutgoing));
            }
        }
    }

    public void testGetIncomingNodes() {
        for (Iterator testGraphIter = testGraphInfos.iterator(); testGraphIter
                .hasNext();) {
            GraphInfo testGInfo = (GraphInfo) testGraphIter.next();
            Graph graph = testGInfo.graph;
            for (Iterator nodeIter = graph.getNodes().iterator(); nodeIter
                    .hasNext();) {
                Object node = nodeIter.next();
                Collection incomingNodes = graph.getIncomingNodes(node);
                Collection ginfoIncoming = testGInfo
                        .getIncomingNodes(((Integer) node).intValue());
                assertTrue("The incoming nodes are: " + incomingNodes
                        + " but should be " + ginfoIncoming,
                        equivalentCollections(incomingNodes, ginfoIncoming));
            }
        }
    }

    public void testIsEmpty() {
        assertTrue(new HashGraph().isEmpty());

        Graph graph = new GraphInfo(new BigInteger(100, new Random(7346))).graph;
        graph.removeAllNodes(graph.getNodes());
        assertTrue(graph.isEmpty());
    }

    public void testContainsNode() {
        Graph emptyGraph = createEmptyGraph();
        List integers = createIntegerList(20);
        for (Iterator i = integers.iterator(); i.hasNext(); ) {
            assertTrue(!emptyGraph.containsNode(i.next()));
        }

        Graph graph = new GraphInfo(new BigInteger(100, new Random(7346))).graph;
        int j = 0;
        Iterator i = integers.iterator();
        for (; i.hasNext() && j <10; ++j) {
            assertTrue(graph.containsNode(i.next()));
        }
        while (i.hasNext()) {
            assertTrue(!graph.containsNode(i.next()));
        }
    }

    /*
     * Class under test for boolean containsEdge(Object, Object)
     */
    public void testContainsEdgeObjectObject() {
        Graph g = createEmptyGraph();
        Integer node1 = new Integer(1);
        Integer node2 = new Integer(2);
        Integer node3 = new Integer(3);

        // {1->2:1, 3->2:2, 1->3:3}
        g.addNode(node1);
        g.addNode(node2);
        g.addNode(node3);
        
        g.addEdge(node1, node2);
        assertTrue(g.containsEdge(node1, node2));
        g.addEdge(node3, node2);
        assertTrue(g.containsEdge(node3, node2));
        g.addEdge(node1, node3);
        assertTrue(g.containsEdge(node1, node3));
        
        assertTrue(!g.containsEdge(node1, node1));
        assertTrue(!g.containsEdge(node3, node1));
        assertTrue(!g.containsEdge(node2, node1));
        assertTrue(!g.containsEdge(node2, node2));
        assertTrue(!g.containsEdge(node2, node3));
    }

    /*
     * Class under test for boolean containsEdge(Object, Object, Object)
     */
    public void testContainsEdgeObjectObjectObject() {
        Graph g = createEmptyGraph();
        Integer node1 = new Integer(1);
        Integer node2 = new Integer(2);
        Integer node3 = new Integer(3);
        
        Integer edgeLabel1 = new Integer(1);
        Integer edgeLabel2 = new Integer(2);
        Integer edgeLabel3 = new Integer(3);
        
        // {1->2:1, 3->2:2, 1->3:3}
        g.addNode(node1);
        g.addNode(node2);
        g.addNode(node3);
        assertTrue(g.addEdge(node1, node2, edgeLabel1));
        assertTrue(g.addEdge(node3, node2, edgeLabel2));
        assertTrue(g.addEdge(node1, node3, edgeLabel3));
        assertTrue(g.addEdge(node1, node2, edgeLabel2));
        assertTrue(g.removeEdge(node1, node2, edgeLabel2));
        
        assertTrue(g.containsEdge(node1, node2, edgeLabel1));
        assertTrue(g.containsEdge(node3, node2, edgeLabel2));
        assertTrue(g.containsEdge(node1, node3, edgeLabel3));
        
        assertTrue(!g.containsEdge(node1, node1, edgeLabel2));
        assertTrue(!g.containsEdge(node3, node2, edgeLabel1));
        assertTrue(!g.containsEdge(node3, node2, edgeLabel3));        
    }

    /*
     * Class under test for boolean containsEdge(Edge)
     */
    public void testContainsEdgeEdge() {
        Graph g = createEmptyGraph();
        Integer node1 = new Integer(1);
        Integer node2 = new Integer(2);
        Integer node3 = new Integer(3);
        
        Integer edgeLabel1 = new Integer(1);
        Integer edgeLabel2 = new Integer(2);
        Integer edgeLabel3 = new Integer(3);
        
        // {1->2:1, 3->2:2, 1->3:3}
        g.addNode(node1);
        g.addNode(node2);
        g.addNode(node3);
        assertTrue(g.addEdge(node1, node2, edgeLabel1));
        assertTrue(g.addEdge(node3, node2, edgeLabel2));
        assertTrue(g.addEdge(node1, node3, edgeLabel3));
        assertTrue(g.addEdge(node1, node2, edgeLabel2));
        assertTrue(g.removeEdge(node1, node2, edgeLabel2));
        
        assertTrue(g.containsEdge(node1, node2, edgeLabel1));
        assertTrue(g.containsEdge(node3, node2, edgeLabel2));
        assertTrue(g.containsEdge(node1, node3, edgeLabel3));
        assertTrue(!g.containsEdge(node1, node2, edgeLabel2));        
    }

    public void testGetOutDegree() {
        for (Iterator testGraphIter = testGraphInfos.iterator(); testGraphIter
                .hasNext();) {
            GraphInfo testGInfo = (GraphInfo) testGraphIter.next();
            Graph graph = testGInfo.graph;
            for (Iterator nodeIter = graph.getNodes().iterator(); nodeIter
                    .hasNext();) {
                Object node = nodeIter.next();
                Collection ginfoOutgoing = testGInfo
                        .getOutgoingNodes(((Integer) node).intValue());
                assertTrue("The out degree of " + node + " should be: " + ginfoOutgoing.size()
                        + " but getOutDegree returned: " + graph.getOutDegree(node),
                        graph.getOutDegree(node) == ginfoOutgoing.size());
            }
        }
    }

    public void testGetInDegree() {
        for (Iterator testGraphIter = testGraphInfos.iterator(); testGraphIter
                .hasNext();) {
            GraphInfo testGInfo = (GraphInfo) testGraphIter.next();
            Graph graph = testGInfo.graph;
            for (Iterator nodeIter = graph.getNodes().iterator(); nodeIter
                    .hasNext();) {
                Object node = nodeIter.next();
                Collection ginfoIncoming = testGInfo
                        .getIncomingNodes(((Integer) node).intValue());
                assertTrue("The in degree of " + node + " should be: " + ginfoIncoming.size()
                        + " but getInDegree returned: " + graph.getInDegree(node), graph.getInDegree(node) == ginfoIncoming.size());
            }
        }
    }

    public void testGetDegree() {
        for (Iterator testGraphIter = testGraphInfos.iterator(); testGraphIter
                .hasNext();) {
            GraphInfo testGInfo = (GraphInfo) testGraphIter.next();
            Graph graph = testGInfo.graph;
            for (Iterator nodeIter = graph.getNodes().iterator(); nodeIter
                    .hasNext();) {
                Object node = nodeIter.next();
                Collection ginfoIncoming = testGInfo
                        .getIncomingNodes(((Integer) node).intValue());
                Collection ginfoOutgoing = testGInfo
                        .getOutgoingNodes(((Integer) node).intValue());
                assertTrue("The degree of " + node + " should be: " + ginfoIncoming.size()
                        + ginfoOutgoing.size()
                        + " but getDegree returned: " + graph
                        .getDegree(node)  , graph
                        .getDegree(node) == ginfoIncoming.size()
                        + ginfoOutgoing.size());
            }
        }
    }

    /*
     * Class under test for Edge addEdge(Object, Object)
     */
    public void testAddEdgeObjectObject() {
        Graph g = createEmptyGraph();
        Integer node1 = new Integer(1);
        Integer node2 = new Integer(2);
        Integer node3 = new Integer(3);

        // {1->2:1, 3->2:2, 1->3:3}
        g.addNode(node1);
        g.addNode(node2);
        g.addNode(node3);
        g.addEdge(node1, node2);
        g.addEdge(node3, node2);
        g.addEdge(node1, node3);

        assertTrue(g.containsEdge(node1, node2));
        assertTrue(g.containsEdge(node3, node2));
        assertTrue(g.containsEdge(node1, node3));
    }

    public void testRemoveEdgeObjectObject() {
        Graph g = createEmptyGraph();
        Integer node1 = new Integer(1);
        Integer node2 = new Integer(2);
        Integer node3 = new Integer(3);

        Integer edgeLabel1 = new Integer(1);
        Integer edgeLabel2 = new Integer(2);
        Integer edgeLabel3 = new Integer(3);

        // {1->2:1, 1-2:2, 3->2:2, 1->3:3}
        g.addNode(node1);
        g.addNode(node2);
        g.addNode(node3);
        g.addEdge(node1, node2, edgeLabel1);
        g.addEdge(node3, node2, edgeLabel2);
        g.addEdge(node1, node3, edgeLabel3);
        g.addEdge(node1, node2, edgeLabel2);
        
        g.removeEdge(node1, node2);
        assertTrue(!g.containsEdge(node1, node2));
        g.removeEdge(node1, node3);
        assertTrue(!g.containsEdge(node1, node3));
        g.removeEdge(node3, node2);
        assertTrue(!g.containsEdge(node3, node2));        
    }

    
    /////////////////////////////////
    // Utility classes and methods //
    /////////////////////////////////
    
    protected Graph createEmptyGraph() {
        return new HashGraph();
    }
    
    /** TODO export this method to a new class tvla.util.CollectionUtils.
     */
    protected static boolean equivalentCollections(Collection c1, Collection c2) {
        if (c1.size() != c2.size())
            return false;

        Iterator c1Iter = c1.iterator();
        Iterator c2Iter = c2.iterator();
        while (c1Iter.hasNext()) {
            if (!c1Iter.next().equals(c2Iter.next())) {
                return false;
            }
        }

        return true;
    }

    /** Creates a list of Integer obejcts consecutively numbered
     * from 0 to <code>size-1</code>.
     * 
     * @param size The number of elements in the output list.
     * @return A list of Integer obejcts consecutively numbered
     * from 0 to <code>size-1</code>.
     */
    protected static List createIntegerList(int size) {
        List result = new ArrayList(size);
        for (int i = 0; i < size; ++i) {
            result.add(i, new Integer(i));
        }
        return result;
    }

//    public static Graph createFullGraph(int numberOfNodes) {
//        Graph result = new HashGraph();
//        List nodes = createIntegerList(numberOfNodes);
//        // Add all integers in the array to the graph as nodes.
//        for (int i = 0; i < numberOfNodes; ++i) {
//            result.addNode(nodes.get(i));
//        }
//        // Add an edge between every two nodes.
//        for (int i = 0; i < numberOfNodes; ++i) {
//            for (int j = 0; j < numberOfNodes; ++j) {
//                result.addEdge(nodes.get(i), nodes.get(j));
//            }
//        }
//        return result;
//    }

    /** A helper class for associating information with a graph, in order to
     * test its implementation. 
     * 
     * @author Roman Manevich
     */
    protected static class GraphInfo {
        public int numberOfNodes;

        public int numberOfEdges;

        public Graph graph;

        public Object[] nodes;

        /** The adjacency matrix of the graph.
         */
        public int[][] matrix;

        public GraphInfo(BigInteger number) {
            assert number.signum() == 1;
            graph = new HashGraph();
            int bitLength = number.bitLength();
            numberOfNodes = (int) Math.floor(Math.sqrt(bitLength));
            assert numberOfNodes > 0;
            matrix = new int[numberOfNodes][numberOfNodes];
            nodes = createIntegerList(numberOfNodes).toArray();
            for (int i = 0; i < numberOfNodes; ++i) {
                graph.addNode(nodes[i]);
            }
            for (int i = 0; i < numberOfNodes; ++i) {
                for (int j = 0; j < numberOfNodes; ++j) {
                    if (number.testBit(i * numberOfNodes + j)) {
                        graph.addEdge(nodes[i], nodes[j]);
                        matrix[i][j] = 1;
                        ++numberOfEdges;
                    }
                }
            }
        }

        public Collection getNodes() {
            List result = new ArrayList(nodes.length);
            for (int i = 0; i < nodes.length; ++i)
                result.add(nodes[i]);
            return result;
        }

        public Collection getOutgoingNodes(int nodeIndex) {
            List result = new ArrayList(nodes.length);
            for (int i = 0; i < nodes.length; ++i) {
                if (matrix[nodeIndex][i] > 0) {
                    result.add(nodes[i]);
                }
            }
            return result;
        }

        public Collection getIncomingNodes(int nodeIndex) {
            List result = new ArrayList(nodes.length);
            for (int i = 0; i < nodes.length; ++i) {
                if (matrix[i][nodeIndex] > 0) {
                    result.add(nodes[i]);
                }
            }
            return result;
        }
        
        public boolean testEdge(int from, int to) {
            return matrix[from][to] > 0;
        }

        public boolean testEdge(Integer from, Integer to) {
            return matrix[from.intValue()][to.intValue()] > 0;
        }
    }
}