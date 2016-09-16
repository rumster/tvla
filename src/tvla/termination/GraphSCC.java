package tvla.termination;

import junit.framework.Assert;
import tvla.util.graph.Graph;
import tvla.util.graph.GraphFactory;

import java.util.*;

class DfsRec {

  public Object   node;
  public Object[] dataArray;
  public int      currIndex;
}
/**
 * Created by BorisD on 10/27/2015.
 */
public final class GraphSCC {

  public static void ComputeTrivialSCC(Graph graph, Collection<Object> sccList) {
    ComputeTrivialSCC(graph, sccList, null);
  }

  public static void ComputeTrivialSCC(Graph graph, Collection<Object> sccList, Collection<Object> nodesToSkip) {

    List<Graph> sccRest = new ArrayList<>();
    ComputeSCCAll(graph, sccRest, sccList, nodesToSkip);
  }

  public static void ComputeNotTrivialSCC(Graph graph, Collection<Graph> sccList) {
    ComputeNotTrivialSCC(graph, sccList, null);
  }

  public static void ComputeNotTrivialSCC(Graph graph, Collection<Graph> sccList, Collection<Object> nodesToSkip) {
    List<Graph> sccRest = new ArrayList<>();
    ComputeSCCAll(graph, sccList, sccRest, nodesToSkip);
  }

  public static void ComputeSCCAll(Graph graph, Collection<Graph> sccNonTrivial, Collection sccTrivial, Collection<Object> nodeToSkip) {
    HashSet<Object> nodesLeft = new HashSet<>(graph.getNodes());

    if (nodeToSkip != null)
      nodesLeft.removeAll(nodeToSkip);

    while (nodesLeft.size() > 0) {
      LinkedList<Object> stack = new LinkedList<>();
      HashSet<Object> marked = new HashSet<>();

      double t = System.currentTimeMillis();
      Dfs2(graph, nodesLeft.iterator().next(), stack, nodesLeft);
      //System.out.println("Dfs " + (System.currentTimeMillis() - t) / 1000.0 + " sec. edges count: " + graph.getEdges().size() + ", nodes count: " + graph.getNodes().size());

      marked.addAll(stack);

      double t2 = System.currentTimeMillis();

      while (!stack.isEmpty()) {

        Object last = stack.removeLast();

        if (marked.remove(last)) {
          Graph scc = GraphFactory.newGraph();
          t = System.currentTimeMillis();
          BackDfs2(graph, last, scc, marked);
          t = System.currentTimeMillis() - t;


          if (IsNotTrivialScc(scc)) {
            //if (t > 100)
              //System.out.println("Back dfs " + t / 1000.0 + " sec");
            sccNonTrivial.add(scc);
          } else
            sccTrivial.add(scc.getNodes().iterator().next());
        }
      }

      t2 = System.currentTimeMillis() - t2;
      //System.out.println("All back dfs " + t2 / 1000.0 + " sec");
    }
  }

  private static void Dfs(Graph graph, Object node, List<Object> stack, HashSet<Object> nodesLeft) {
    Collection outgoingNodes = graph.getOutgoingNodes(node);
    nodesLeft.remove(node);

    for (Object next : outgoingNodes) {
      if (nodesLeft.contains(next))
        Dfs(graph, next, stack, nodesLeft);
    }

    stack.add(node);
  }

  private static void Dfs2(Graph graph, Object node, List<Object> stack, HashSet<Object> nodesLeft) {

    List<DfsRec> queue = new ArrayList<>();

    DfsRec currRec = new DfsRec();
    currRec.node = node;
    currRec.dataArray = graph.getOutgoingNodes(node).toArray();
    currRec.currIndex = 0;

    queue.add(currRec);

    while (!queue.isEmpty()) {
      currRec = queue.get(queue.size() - 1);
      node = currRec.node;
      Object[] outgoingNodes = currRec.dataArray;

      if (currRec.currIndex == 0) {
        nodesLeft.remove(node);
      }

      if (currRec.currIndex < outgoingNodes.length) {
        Object next = outgoingNodes[currRec.currIndex];
        currRec.currIndex++;

        if (nodesLeft.contains(next)) {
          DfsRec newRec = new DfsRec();
          newRec.node = next;
          newRec.dataArray = graph.getOutgoingNodes(next).toArray();
          newRec.currIndex = 0;

          queue.add(newRec);
        }
      }
      else {
        stack.add(node);
        queue.remove(queue.size() - 1);
      }
    }

    //Collection outgoingNodes = graph.getOutgoingNodes(node);
    //nodesLeft.remove(node);
//
    //for (Object next : outgoingNodes) {
    //  if (nodesLeft.contains(next))
    //    Dfs(graph, next, stack, nodesLeft);
    //}
//
    //stack.add(node);
  }

  private static void BackDfs(Graph graph, Object node, Graph sccGraph, HashSet<Object> marked) {
    Collection incomingEdges = graph.getIncomingEdges(node);
    sccGraph.addNode(node);
    marked.remove(node);

    for (Object incomingEdgeObj : incomingEdges) {
      Graph.Edge edge = (Graph.Edge)incomingEdgeObj;
      Object incomingNode = edge.getSource();

      if (marked.remove(incomingNode)) {

        sccGraph.addNode(incomingNode);

        Assert.assertTrue(!sccGraph.containsEdge(incomingNode, node));
        sccGraph.addEdge(incomingNode, node, edge.getLabel());

        BackDfs2(graph, incomingNode, sccGraph, marked);
      }
      else if (sccGraph.containsNode(incomingNode)) {

        Assert.assertTrue(!sccGraph.containsEdge(incomingNode, node));
        sccGraph.addEdge(incomingNode, node, edge.getLabel());
      }
    }
  }

  private static void BackDfs2(Graph graph, Object node, Graph sccGraph, HashSet<Object> marked) {

    List<DfsRec> queue = new ArrayList<>();

    DfsRec currRec = new DfsRec();
    currRec.node = node;
    currRec.dataArray = graph.getIncomingEdges(node).toArray();
    currRec.currIndex = 0;

    queue.add(currRec);

    while (!queue.isEmpty()) {

      currRec = queue.get(queue.size() - 1);
      node = currRec.node;
      Object[] incomingEdges = currRec.dataArray;

      if (currRec.currIndex == 0) {
        sccGraph.addNode(node);
        marked.remove(node);
      }

      if (currRec.currIndex < incomingEdges.length) {
        Object incomingEdgeObj = incomingEdges[currRec.currIndex];
        currRec.currIndex++;

        Graph.Edge edge = (Graph.Edge) incomingEdgeObj;
        Object incomingNode = edge.getSource();

        if (marked.remove(incomingNode)) {
          sccGraph.addNode(incomingNode);

          Assert.assertTrue(!sccGraph.containsEdge(incomingNode, node));
          sccGraph.addEdge(incomingNode, node, edge.getLabel());

          DfsRec newRec = new DfsRec();
          newRec.node = incomingNode;
          newRec.dataArray = graph.getIncomingEdges(incomingNode).toArray();
          newRec.currIndex = 0;

          queue.add(newRec);

          //BackDfs(graph, incomingNode, sccGraph, marked);
        } else if (sccGraph.containsNode(incomingNode)) {

          Assert.assertTrue(!sccGraph.containsEdge(incomingNode, node));
          sccGraph.addEdge(incomingNode, node, edge.getLabel());
        }
      }
      else {
        queue.remove(queue.size() - 1);
      }

      //Collection dataArray = graph.getIncomingEdges(node);
      //for (Object incomingEdgeObj : dataArray) {
      //  Graph.Edge edge = (Graph.Edge) incomingEdgeObj;
      //  Object incomingNode = edge.getSource();
//
      //  if (marked.remove(incomingNode)) {
//
      //    sccGraph.addNode(incomingNode);
//
      //    Assert.assertTrue(!sccGraph.containsEdge(incomingNode, node));
      //    sccGraph.addEdge(incomingNode, node, edge.getLabel());
//
      //    BackDfs(graph, incomingNode, sccGraph, marked);
      //  } else if (sccGraph.containsNode(incomingNode)) {
//
      //    Assert.assertTrue(!sccGraph.containsEdge(incomingNode, node));
      //    sccGraph.addEdge(incomingNode, node, edge.getLabel());
      //  }
      //}
    }
  }

  public static boolean IsSCCSimpleCycle(Graph graphSCC) {

    boolean result = true;

    for (Object node : graphSCC.getNodes()) {

      if (graphSCC.getOutDegree(node) != 1 || graphSCC.getInDegree(node) != 1) {
        result = false;
        break;
      }
    }

    return result;
  }

  private static boolean IsNotTrivialScc(Graph scc) {

    boolean result = scc.getNumberOfNodes() > 1;

    // Loop to itself
    if (!result && scc.getNumberOfNodes() == 1) {

      Object node = scc.getNodes().iterator().next();
      result = scc.containsEdge(node, node);
    }

    return result;
  }

  public static void TestSCC() {

    List<Graph> sccList = new ArrayList<>();

    // Test 1
    Graph g = GraphFactory.newGraph(5);
    for (int i = 1; i <= 11; i++)
      g.addNode(i);

    g.addEdge(1, 2);
    g.addEdge(2, 3);
    g.addEdge(3, 1);
    g.addEdge(3, 5);
    g.addEdge(5, 6);
    g.addEdge(6, 7);
    g.addEdge(7, 8);
    g.addEdge(8, 6);

    g.addEdge(5, 9);
    g.addEdge(9, 10);
    g.addEdge(10, 11);
    g.addEdge(11, 9);

    ComputeNotTrivialSCC(g, sccList);

    Assert.assertEquals(sccList.size(), 3);

    g = sccList.get(0);
    for (int i = 1; i <= 3; i++)
      Assert.assertTrue(g.containsNode(i));
    Assert.assertTrue(g.containsEdge(1, 2));
    Assert.assertTrue(g.containsEdge(2, 3));
    Assert.assertTrue(g.containsEdge(3, 1));
    Assert.assertTrue(IsSCCSimpleCycle(g));

    g = sccList.get(1);
    for (int i = 9; i <= 11; i++)
      Assert.assertTrue(g.containsNode(i));
    Assert.assertTrue(g.containsEdge(9, 10));
    Assert.assertTrue(g.containsEdge(10, 11));
    Assert.assertTrue(g.containsEdge(11, 9));
    Assert.assertTrue(IsSCCSimpleCycle(g));

    g = sccList.get(2);
    for (int i = 6; i <= 8; i++)
      Assert.assertTrue(g.containsNode(i));
    Assert.assertTrue(g.containsEdge(6, 7));
    Assert.assertTrue(g.containsEdge(7, 8));
    Assert.assertTrue(g.containsEdge(8, 6));
    Assert.assertTrue(IsSCCSimpleCycle(g));

    // Test 2
    g = GraphFactory.newGraph(3);
    for (int i = 1; i <= 4; i++)
      g.addNode(i);

    g.addEdge(1, 2);
    g.addEdge(2, 3);
    g.addEdge(3, 4);

    sccList.clear();
    ComputeNotTrivialSCC(g, sccList);

    Assert.assertEquals(sccList.size(), 0);

    g.addEdge(4, 1);

    sccList.clear();
    ComputeNotTrivialSCC(g, sccList);

    Assert.assertEquals(sccList.size(), 1);
    g = sccList.get(0);

    for (int i = 1; i <= 4; i++)
      Assert.assertTrue(g.containsNode(i));

    Assert.assertTrue(g.containsEdge(1, 2));
    Assert.assertTrue(g.containsEdge(2, 3));
    Assert.assertTrue(g.containsEdge(3, 4));
    Assert.assertTrue(g.containsEdge(4, 1));
    Assert.assertTrue(IsSCCSimpleCycle(g));

    // Test 3
    g = GraphFactory.newGraph(6);
    for (int i = 1; i <= 5; i++)
      g.addNode(i);

    g.addEdge(1, 2);
    g.addEdge(2, 3);
    g.addEdge(3, 1);
    g.addEdge(3, 4);
    g.addEdge(4, 5);
    g.addEdge(5, 3);

    sccList.clear();
    ComputeNotTrivialSCC(g, sccList);

    Assert.assertEquals(sccList.size(), 1);

    g = sccList.get(0);

    for (int i = 1; i <= 5; i++)
      Assert.assertTrue(g.containsNode(i));

    Assert.assertTrue(g.containsEdge(1, 2));
    Assert.assertTrue(g.containsEdge(2, 3));
    Assert.assertTrue(g.containsEdge(3, 1));
    Assert.assertTrue(g.containsEdge(3, 4));
    Assert.assertTrue(g.containsEdge(4, 5));
    Assert.assertTrue(g.containsEdge(5, 3));
    Assert.assertTrue(!IsSCCSimpleCycle(g));

    // Test 4
    g = GraphFactory.newGraph(4);
    for (int i = 1; i <= 4; i++)
      g.addNode(i);

    g.addEdge(4, 3);
    g.addEdge(2, 4);
    g.addEdge(3, 2);
    g.addEdge(3, 1);
    g.addEdge(2, 1);

    sccList.clear();
    ComputeNotTrivialSCC(g, sccList);

    Assert.assertEquals(sccList.size(), 1);

    g = sccList.get(0);

    for (int i = 2; i <= 4; i++)
      Assert.assertTrue(g.containsNode(i));

    Assert.assertTrue(g.containsEdge(4, 3));
    Assert.assertTrue(g.containsEdge(3, 2));
    Assert.assertTrue(g.containsEdge(2, 4));
    Assert.assertTrue(IsSCCSimpleCycle(g));
  }

  public static void TestSCC2() {

    List<Graph> sccList = new ArrayList<>();
    List<Object> trivialSccList = new ArrayList<>();

    // Test 1
    Graph g = GraphFactory.newGraph(5);
    Graph gOrg = g;

    for (int i = 1; i <= 11; i++)
      g.addNode(i);

    g.addEdge(1, 2);
    g.addEdge(2, 3);
    g.addEdge(3, 1);
    g.addEdge(3, 5);
    g.addEdge(5, 6);
    g.addEdge(6, 7);
    g.addEdge(7, 8);
    g.addEdge(8, 6);

    g.addEdge(5, 9);
    g.addEdge(9, 10);
    g.addEdge(10, 11);
    g.addEdge(11, 9);

    ComputeNotTrivialSCC(g, sccList);

    Assert.assertEquals(sccList.size(), 3);

    g = sccList.get(0);
    for (int i = 1; i <= 3; i++)
      Assert.assertTrue(g.containsNode(i));
    Assert.assertTrue(g.containsEdge(1, 2));
    Assert.assertTrue(g.containsEdge(2, 3));
    Assert.assertTrue(g.containsEdge(3, 1));
    Assert.assertTrue(IsSCCSimpleCycle(g));

    g = sccList.get(1);
    for (int i = 9; i <= 11; i++)
      Assert.assertTrue(g.containsNode(i));
    Assert.assertTrue(g.containsEdge(9, 10));
    Assert.assertTrue(g.containsEdge(10, 11));
    Assert.assertTrue(g.containsEdge(11, 9));
    Assert.assertTrue(IsSCCSimpleCycle(g));

    g = sccList.get(2);
    for (int i = 6; i <= 8; i++)
      Assert.assertTrue(g.containsNode(i));
    Assert.assertTrue(g.containsEdge(6, 7));
    Assert.assertTrue(g.containsEdge(7, 8));
    Assert.assertTrue(g.containsEdge(8, 6));
    Assert.assertTrue(IsSCCSimpleCycle(g));

    sccList.clear();
    ComputeTrivialSCC(gOrg, trivialSccList);

    Assert.assertEquals(trivialSccList.size(), 2);
    Assert.assertTrue(trivialSccList.contains(5));
    Assert.assertTrue(trivialSccList.contains(4));

    List<Object> nodesToSkip = Arrays.asList((Object)7, 2);
    sccList.clear();
    ComputeNotTrivialSCC(gOrg, sccList, nodesToSkip);

    Assert.assertEquals(sccList.size(), 1);
    g = sccList.get(0);
    for (int i = 9; i <= 11; i++)
      Assert.assertTrue(g.containsNode(i));
    Assert.assertTrue(g.containsEdge(9, 10));
    Assert.assertTrue(g.containsEdge(10, 11));
    Assert.assertTrue(g.containsEdge(11, 9));
    Assert.assertTrue(IsSCCSimpleCycle(g));

    nodesToSkip = Arrays.asList((Object)5, 4);
    trivialSccList.clear();
    ComputeTrivialSCC(gOrg, trivialSccList, nodesToSkip);

    Assert.assertEquals(trivialSccList.size(), 0);
  }
}
