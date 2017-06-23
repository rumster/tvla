package tvla.termination.carteq;

import tvla.util.Pair;
import tvla.util.graph.Graph;
import tvla.util.graph.GraphFactory;

import javax.swing.plaf.nimbus.State;
import java.util.*;

/**
 * Created by BorisD on 7/2/2016.
 */
public class TerminationVerifier {

  private static String s_StartNodePrefix = "S1";

  public static void Verify( ){
    //Graph g1 = DotUtils.LoadGraphFromSimpleFormat("C:\\Temp\\carteq\\A_search6_1.simple");
    //Graph g2 = DotUtils.LoadGraphFromSimpleFormat("C:\\Temp\\carteq\\B_search6_1.simple");
    Graph g1 = DotUtils.LoadGraphFromSimpleFormat("C:\\Temp\\carteq\\Сoncurrency\\e2_A.simple");
    Graph g2 = DotUtils.LoadGraphFromSimpleFormat("C:\\Temp\\carteq\\Сoncurrency\\e2_B.simple");

    Graph g3 = ProduceCartesian(g1, g2);

    List<Graph> l1 = GetUnSat(g1);
    List<Graph> l2 = GetUnSat(g2);

    //boolean result = AreSetsDisjoint(l1, l2);
    boolean result = AreSetsPartiallyDisjoint(l1, l2);

    //DotUtils.GraphSimpleToDot("C:\\Temp\\graphs\\1.dt", g1);
    //DotUtils.GraphSimpleToDot("C:\\Temp\\graphs\\2.dt", g2);
    DotUtils.GraphCarteqToDot2("C:\\Temp\\graphs\\3.dt", g1, g2);
  }

  private static Graph ProduceCartesian(Graph gA, Graph gB) {

    Graph result = GraphFactory.newGraph();

    SNode nodeA = null, nodeB = null;

    for (Object n : gA.getNodes()) {
      if (((SNode)n).Name.startsWith(s_StartNodePrefix)) {
        nodeA = (SNode)n;
        break;
      }
    }

    for (Object n : gB.getNodes()) {
      if (((SNode)n).Name.startsWith(s_StartNodePrefix)) {
        nodeB = (SNode)n;
        break;
      }
    }

    assert nodeA.Name == nodeB.Name;

    result.addNode(nodeA);
    result.addNode(nodeB);
    result.addEdge(nodeA, nodeB);

    Map<SSubNode, List<SSubNode>> rtA2B = new HashMap<>();
    Map<SSubNode, List<SSubNode>> rtB2A = new HashMap<>();
    nodeA.EqualityTransition.put(nodeB, rtA2B);
    nodeB.EqualityTransition.put(nodeB, rtB2A);

    Object[] subNodesA = nodeA.SubNodes.values().toArray();
    Object[] subNodesB = nodeB.SubNodes.values().toArray();

    for (int i = 0; i < subNodesA.length; i++) {
      List<SSubNode> listA2B = new ArrayList<>();
      List<SSubNode> listB2A = new ArrayList<>();

      listA2B.add((SSubNode)subNodesB[i]);
      listB2A.add((SSubNode)subNodesA[i]);

      rtA2B.put((SSubNode)subNodesA[i], listA2B);
      rtB2A.put((SSubNode)subNodesB[i], listB2A);
    }

    HashSet<SNode> marked = new HashSet<>();
    Queue<SNode> queue = new LinkedList<>();
    queue.add(nodeA);
    marked.add(nodeA);

    while (queue.size() > 0) {
      nodeA = queue.poll();

      for (Map.Entry<SNode, Map<SSubNode, List<SSubNode>>> tA2B : nodeA.EqualityTransition.entrySet()) {
        nodeB = tA2B.getKey();

        for (Map.Entry<SNode, Map<SSubNode, List<SSubNode>>> tA2A : nodeA.RegionTransition.entrySet()) {
          SNode nodeANext = tA2A.getKey();

          if (!result.containsNode(nodeANext)) result.addNode(nodeANext);
          if (!result.containsEdge(nodeA, nodeANext)) result.addEdge(nodeA, nodeANext);

          if (marked.add(nodeANext)) {
            queue.add(nodeANext);
          }

          for (Map.Entry<SNode, Map<SSubNode, List<SSubNode>>> tB2B : nodeB.RegionTransition.entrySet()) {
            SNode nodeBNext = tB2B.getKey();
            Map<SSubNode, List<SSubNode>> tNextA2B = new HashMap<>();

            if (!result.containsNode(nodeBNext)) result.addNode(nodeBNext);
            if (!result.containsEdge(nodeB, nodeBNext)) result.addEdge(nodeB, nodeBNext);
            if (!result.containsEdge(nodeANext, nodeBNext)) result.addEdge(nodeANext, nodeBNext);

            for (SSubNode subA : nodeA.SubNodes.values()) {
              FillSubEquality(subA, tA2A.getValue(), tA2B.getValue(), tB2B.getValue(), tNextA2B);
            }

            AddEq(nodeANext, nodeBNext, tNextA2B);
          }
        }
      }
    }

    return result;
  }

  private static void AddEq(SNode nodeSrc, SNode nodeDst, Map<SSubNode, List<SSubNode>> eq) {

    if (!nodeSrc.EqualityTransition.containsKey(nodeDst) && IsEqValid(eq)) {
      nodeSrc.EqualityTransition.put(nodeDst, eq);
    }
  }

  private static void FillSubEquality(SSubNode subA,
                                      Map<SSubNode, List<SSubNode>> tA2NextA,
                                      Map<SSubNode, List<SSubNode>> tA2B,
                                      Map<SSubNode, List<SSubNode>> tB2NextB,
                                      Map<SSubNode, List<SSubNode>> tNextA2B) {

    Map<SSubNode, List<SSubNode>> result = new HashMap<>();

    List<SSubNode> nextAs  = tA2NextA.get(subA);
    List<SSubNode> eqBs    = tA2B.get(subA);
    List<SSubNode> nextEqB = new ArrayList<>();

    for (SSubNode subEqB : eqBs) {
      nextEqB.addAll(tB2NextB.get(subEqB));
    }
    for (SSubNode nextSubA : nextAs) {
      List<SSubNode> nextEqBNew = tNextA2B.getOrDefault(nextSubA, null);

      if (nextEqBNew == null) {
        nextEqBNew = new ArrayList<>();
        tNextA2B.put(nextSubA, nextEqBNew);
      }

      nextEqBNew.addAll(nextEqB);
    }
  }

  private static Map<SSubNode, List<SSubNode>> Reverse(Map<SSubNode, List<SSubNode>> eq) {
    Map<SSubNode, List<SSubNode>> result = new HashMap<>();

    for (Map.Entry<SSubNode, List<SSubNode>> entry : eq.entrySet()) {
      for (SSubNode subNode : entry.getValue()) {

        if (!result.containsKey(subNode)) {
          result.put(subNode, new ArrayList<SSubNode>());
        }

        result.get(subNode).add(entry.getKey());
      }
    }

    return result;
  }

  private static boolean IsEqValid(Map<SSubNode, List<SSubNode>> eq) {

    Map<SSubNode, List<SSubNode>> eqR = Reverse(eq);
    boolean result = true;

    for (Map.Entry<SSubNode, List<SSubNode>> entry : eqR.entrySet()) {

      if (entry.getKey().IsSummary()) {
        boolean hasS = false;

        for (SSubNode subNode : entry.getValue()) {

          if (subNode.IsSummary()) {
            hasS = true;
            break;
          }
        }

        if (!hasS) {
          result = false;
          break;
        }
      }
    }

    return result;
  }

  private static List<Graph> GetUnSat(Graph graph) {

    List<Graph> sccList = new ArrayList<>();
    List<Graph> unSat = new ArrayList<>();

    GraphSCC.ComputeNotTrivialSCC(graph, sccList);

    for (Graph g : sccList) {
      if (IsSatisfiable(g, unSat)) {
      }
    }

    return unSat;
  }

  private static boolean IsSatisfiable(Graph g, List<Graph> unSatSCC) {

    HashSet<Object> trivialScc    = new HashSet<>();
    List<Graph>     nonTrivialScc = new ArrayList<>();
    HashSet<Object> cutPoints     = new HashSet<>();

    Graph rtGraph = CreateRegionTransitionGraph(g);
    GraphSCC.ComputeSCCAll(rtGraph, nonTrivialScc, trivialScc, null);

    assert rtGraph.getNumberOfNodes() > 0;
    assert rtGraph.getNumberOfEdges() > 0;
    assert (nonTrivialScc.size() > 0 || trivialScc.size() > 0);

    ApplyMarking(rtGraph, trivialScc, cutPoints);

    int unSatCount = unSatSCC.size();
    GraphSCC.ComputeNotTrivialSCC(g, unSatSCC, cutPoints);

    /*
    if (unSatSCC.size() > 0) {
      List<Graph> g1 = new ArrayList<>();
      g1.add(g);
      List<Graph> g2 = new ArrayList<>();
      g2.add(unSatSCC.get(unSatSCC.size() - 1));

      DotUtils.SaveGraphsToSingleFile("C:\\temp\\graphs\\1.dt", true, g1);
      DotUtils.SaveGraphsToSingleFile("C:\\temp\\graphs\\2.dt", true, g2);
    }
    */

    boolean result = unSatSCC.size() == unSatCount;

    return result;
  }

  private static Graph CreateRegionTransitionGraph(Graph transitionGraph) {

    Graph result = GraphFactory.newGraph();

    for (Object edgeObj : transitionGraph.getEdges()) {
      Graph.Edge edge = (Graph.Edge)edgeObj;

      SNode nodeSrc = (SNode)edge.getSource();
      SNode nodeDst = (SNode)edge.getDestination();

      for (SSubNode subNode : nodeSrc.SubNodes.values())
        result.addNode(subNode);

      for (SSubNode subNode : nodeDst.SubNodes.values())
        result.addNode(subNode);

      Map<SSubNode, List<SSubNode>> transition = nodeSrc.RegionTransition.get(nodeDst);

      for (Map.Entry<SSubNode, List<SSubNode>> entry : transition.entrySet()) {
        for (SSubNode subNodeTvsDst : entry.getValue()) {
          result.addEdge(entry.getKey(), subNodeTvsDst);
        }
      }
    }

    return result;
  }

  private static void ApplyMarking(Graph rtGraph, HashSet<Object> trivialScc, HashSet<Object> cutPoints) {
    Map<Object, Integer> refCount = new HashMap<>();

    // Mark the cutpoints
    for (Object subNodeObj : trivialScc) {

      SSubNode subNode = (SSubNode)subNodeObj;
      cutPoints.add(subNode.Parent);

      /*
      if (subNode.Type == RTSubNode.RTSubNodeType.Default) {
        //subNode.Type = RTSubNode.RTSubNodeType.CutPoint;
        cutPoints.add(subNode.Parent);
      }
      */
    }
  }

  private static boolean AreSetsDisjoint(List<Graph> set1, List<Graph> set2) {
    Collection nodes1 = GetAllNodes(set1);
    Collection nodes2 = GetAllNodes(set2);

    boolean result = AreSetsDisjoint(nodes1, nodes2);

    if (result)
      result = AreSetsDisjoint(nodes2, nodes1);

    return result;
  }

  private static boolean AreSetsPartiallyDisjoint(List<Graph> set1, List<Graph> set2) {
    Collection nodes1 = GetAllNodes(set1);
    Collection nodes2 = GetAllNodes(set2);

    boolean result = AreSetsPartiallyDisjoint(set1, nodes2) && AreSetsPartiallyDisjoint(set2, nodes1);

    return result;
  }

  // TODO Wrong name
  private static boolean AreSetsDisjoint(Collection nodes1, Collection nodes2) {
    boolean result = true;

    for (Object nObj1 : nodes1) {
      SNode sNode = (SNode)nObj1;

      Set<SNode> eqNodes = sNode.EqualityTransition.keySet();
      for (SNode sNode2 : eqNodes) {
        result = !nodes2.contains(sNode2);

        if (!result)
          break;
      }

      if (!result)
        break;
    }

    return result;
  }

  private static boolean AreSetsPartiallyDisjoint(List<Graph> setsA, Collection nodesB) {

    for (Graph g : setsA) {
      boolean currResult = false;

      for (Object nObj1 : g.getNodes()) {
        SNode sNode = (SNode)nObj1;
        Set<SNode> eqNodes = sNode.EqualityTransition.keySet();

        Character prefix1 = sNode.Name.charAt(0);

        boolean allEqNodesNotContained = true;
        SNode contained = null;
        for (SNode sNode2 : eqNodes) {
          Character prefix2 = sNode2.Name.charAt(0);
          if ((prefix1 == 'L' || prefix2 == 'L') && prefix1 != prefix2)
            continue;


          allEqNodesNotContained = !nodesB.contains(sNode2);

          if (!allEqNodesNotContained)
            contained = sNode2;
            break;
        }

        if (allEqNodesNotContained) {
          currResult = true;
          break;
        }
        else {
          System.out.println(contained);
        }
      }

      if (!currResult)
        return false;
    }

    return true;
  }

  private static Collection GetAllNodes(List<Graph> set) {
    Collection result = new HashSet();

    for (Graph g : set) {
      result.addAll(g.getNodes());
    }

    return result;
  }
}
