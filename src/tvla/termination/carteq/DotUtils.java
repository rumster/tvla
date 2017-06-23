package tvla.termination.carteq;

import junit.framework.Assert;
import tvla.core.Node;
import tvla.util.graph.Graph;
import tvla.util.graph.GraphFactory;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by BorisD on 11/16/2015.
 */
public final class DotUtils {

  public static void SaveGraphToSingleFile(String path, boolean satisfiable, Graph graph) {

    String graphDot = GraphToDot(satisfiable, graph);
    SaveToFile(graphDot, path);
  }

  public static void SaveGraphsToSingleFile(String path, boolean satisfiable, List<Graph> graphs) {

    String graphDot = GraphToDot(satisfiable, graphs.toArray());
    SaveToFile(graphDot, path);
  }

  public static void SaveGraphsToMultFiles(String pathPrefix, boolean satisfiable, List<Graph> graphs) {

    for (int i = 0; i < graphs.size(); i++) {
      Graph g = graphs.get(i);

      String pathDt = pathPrefix + Integer.toString(i) + (g.getNodes().size() < 50 ? "_small.dt" : "_big.dt");

      String graphDot = GraphToDot(satisfiable, g);
      SaveToFile(graphDot, pathDt);
    }
  }

  public static void SaveLevelGraphsToMultFiles(String pathPrefix, boolean satisfiable, Map<Integer, List<Graph>> graphs) {

    for (Map.Entry<Integer, List<Graph>> entry : graphs.entrySet()) {

      String currPrefix = pathPrefix + "_Level" + Integer.toString(entry.getKey()) + "_";

      for (int i = 0; i < entry.getValue().size(); i++) {
        Graph g = entry.getValue().get(i);
        String pathDt = currPrefix + Integer.toString(i) + (g.getNodes().size() < 50 ? "_small.dt" : "_big.dt");

        String graphDot = GraphToDot(satisfiable, g);
        SaveToFile(graphDot, pathDt);
      }
    }
  }

  public static void SaveGraphToSimpleFormat(String fileName, Graph graph) {

    StringBuffer result = new StringBuffer();

    for (Object nodeObj : graph.getNodes()) {
      RTNode node = (RTNode)nodeObj;

      node.ToSimpleFormat(result);
      result.append("\n");
    }

    for (Object edgeObj : graph.getEdges()) {

      Graph.Edge edge = (Graph.Edge) edgeObj;

      RTNode nodeSrc = (RTNode) edge.getSource();
      RTNode nodeDst = (RTNode) edge.getDestination();
      String nodeSrcStr = nodeSrc.toStringFull() + ".";
      String nodeDstStr = nodeDst.toStringFull() + ".";


      Map<Node, List<Node>> regionTransition = (Map<Node, List<Node>>) edge.getLabel();
      Map<Integer, RTSubNode> subNodesSrc = nodeSrc.GetSubNodes();
      Map<Integer, RTSubNode> subNodesDst = nodeDst.GetSubNodes();

      assert regionTransition.size() > 0;

      for (Map.Entry<Node, List<Node>> entry : regionTransition.entrySet()) {

        Node tvsNodeSrc = entry.getKey();
        RTSubNode subNodeSrc = subNodesSrc.get(tvsNodeSrc.id());

        for (Node tvlNodeDst : entry.getValue()) {

          result.append(nodeSrcStr);
          result.append(subNodeSrc.NamePrefixed);
          result.append(":");

          result.append(nodeDstStr);
          result.append(subNodesDst.get(tvlNodeDst.id()).NamePrefixed);
          result.append("\n");
        }
      }
    }

    SaveToFile(result.toString(), fileName);
  }

  private static String GraphToDot(boolean satisfiable, Object... graphs) {

    StringBuffer result = new StringBuffer();

    result.append("digraph structs {\n");
    result.append("node [shape=none]\n");
    result.append("edge [arrowhead=vee]\n");

    for (Object gObj : graphs) {

      Graph g = (Graph)gObj;
      for (Object nodeObj : g.getNodes()) {
        RTNode node = (RTNode)nodeObj;

        node.ToDot(result);
        result.append("\n");
      }

      for (Object edgeObj : g.getEdges()) {

        Graph.Edge edge = (Graph.Edge) edgeObj;

        RTNode nodeSrc = (RTNode)edge.getSource();
        RTNode nodeDst = (RTNode)edge.getDestination();
        String nodeSrcStr = nodeSrc.toString() + ":";
        String nodeDstStr = nodeDst.toString() + ":";

        Map<Node, List<Node>> regionTransition = (Map<Node, List<Node>>)edge.getLabel();
        Map<Integer, RTSubNode> subNodesSrc = nodeSrc.GetSubNodes();
        Map<Integer, RTSubNode> subNodesDst = nodeDst.GetSubNodes();

        String[] colors = new String[] {" [color=\"#C8C142\"", " [color=\"#00FF00\"", " [color=\"#0000FF\"", " [color=\"#FFFF00\"", " [color=\"#FF00FF\"", " [color=\"#00FFFF\"", };

        if (regionTransition.size() > 0) {
          int i = 0;
          for (Map.Entry<Node, List<Node>> entry : regionTransition.entrySet()) {

            Node tvsNodeSrc = entry.getKey();
            RTSubNode subNodeSrc = subNodesSrc.get(tvsNodeSrc.id());

            if (!satisfiable || subNodeSrc.Type != RTSubNode.RTSubNodeType.Frame) {
              for (Node tvlNodeDst : entry.getValue()) {

                result.append(nodeSrcStr);
                result.append(subNodeSrc.NamePrefixed);
                result.append(" -> ");

                result.append(nodeDstStr);
                result.append(subNodesDst.get(tvlNodeDst.id()).NamePrefixed);
                //result.append(" [color=gray");
                //result.append(Math.min(60, i++ * 10));
                result.append(colors[i++ % colors.length]);
                result.append("];\n");
              }
            }
          }
        } else {
          result.append(nodeSrc.toString());
          result.append(" -> ");
          result.append(nodeDst.toString());
          result.append(";\n");
        }
      }
    }

    result.append("}");

    return result.toString();
  }

  public static Graph LoadGraphFromSimpleFormat(String fileName) {

    Graph result = GraphFactory.newGraph();
    Map<String, SNode> nodes = new HashMap<>();

    try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {

      for (String line; (line = br.readLine()) != null; ) {

        String[] parts = line.split(",");

        if (parts.length > 1) {
          String name = parts[0].trim();
          if (!nodes.containsKey(name)) {
            SNode node = new SNode(name);

            for (int i = 1; i < parts.length; i++) {

              String subName = parts[i].trim();

              SSubNode subNode = new SSubNode(subName, node);
              node.SubNodes.put(subName, subNode);
            }

            nodes.put(name, node);
            result.addNode(node);
          }

          continue;
        }

        parts = line.split(":");

        if (parts.length == 2) {
          String[] src = parts[0].split("\\.");
          String[] dst = parts[1].split("\\.");

          SNode nodeSrc = nodes.get(src[0].trim());
          SNode nodeDst = nodes.get(dst[0].trim());

          SSubNode subNodeSrc = nodeSrc.SubNodes.get(src[1].trim());
          SSubNode subNodeDst = nodeDst.SubNodes.get(dst[1].trim());

          if (!result.containsEdge(nodeSrc, nodeDst))
            result.addEdge(nodeSrc, nodeDst);

          Map<SSubNode, List<SSubNode>> rt = nodeSrc.RegionTransition.getOrDefault(nodeDst, null);

          if (rt == null) {
            rt = new HashMap<SSubNode, List<SSubNode>>();
            nodeSrc.RegionTransition.put(nodeDst, rt);
          }

          List<SSubNode> subRt = rt.getOrDefault(subNodeSrc, null);

          if (subRt == null) {
            subRt = new ArrayList<>();
            rt.put(subNodeSrc, subRt);
          }

          subRt.add(subNodeDst);
        }
      }
    }
    catch (Exception e) {
      System.err.println(e.getMessage()); // handle exception
    }

    return result;
  }

  public static void GraphSimpleToDot(String fileName, Graph graph) {

    StringBuffer result = new StringBuffer();

    result.append("digraph structs {\n");
    result.append("node [shape=none]\n");
    result.append("edge [arrowhead=vee]\n");

    for (Object nodeObj : graph.getNodes()) {
      SNode node = (SNode)nodeObj;

      node.ToDot(result, null);
      result.append("\n");
    }

    for (Object edgeObj : graph.getEdges()) {

      Graph.Edge edge = (Graph.Edge)edgeObj;

      SNode nodeSrc = (SNode)edge.getSource();
      SNode nodeDst = (SNode)edge.getDestination();
      String nodeSrcStr = Integer.toString(nodeSrc.ID);
      String nodeDstStr = Integer.toString(nodeDst.ID);

      Map<SSubNode, List<SSubNode>> regionTransition   = nodeSrc.RegionTransition.get(nodeDst);
      Map<SSubNode, List<SSubNode>> equalityTransition = nodeSrc.EqualityTransition.get(nodeDst);

      Map<SSubNode, List<SSubNode>> transition = equalityTransition;

      String[] colors = new String[] {" [color=\"#C8C142\"", " [color=\"#00FF00\"", " [color=\"#0000FF\"", " [color=\"#FFFF00\"", " [color=\"#FF00FF\"", " [color=\"#00FFFF\"", };

      if (regionTransition != null && regionTransition.size() > 0) {
        result.append(nodeSrcStr);
        result.append(" -> ");

        result.append(nodeDstStr);
        //result.append(" [color=gray");
        //result.append(Math.min(60, i++ * 10));
        result.append(colors[0]);
        result.append("];\n");
      }

      if (equalityTransition != null && equalityTransition.size() > 0) {
        result.append(nodeSrcStr);
        result.append(" -> ");

        result.append(nodeDstStr);
        //result.append(" [color=gray");
        //result.append(Math.min(60, i++ * 10));
        result.append(colors[1]);
        result.append("];\n");
      }
      /*
      if (transition != null && transition.size() > 0) {
        int i = 0;
        for (Map.Entry<SSubNode, List<SSubNode>> entry : transition.entrySet()) {

          SSubNode subNodeSrc = entry.getKey();

          //if (!satisfiable || subNodeSrc.Type != RTSubNode.RTSubNodeType.Frame) {
            for (SSubNode subNodeDst : entry.getValue()) {

              result.append(nodeSrcStr);
              result.append(subNodeSrc.Name);
              result.append(" -> ");

              result.append(nodeDstStr);
              result.append(subNodeDst.Name);
              //result.append(" [color=gray");
              //result.append(Math.min(60, i++ * 10));
              result.append(colors[i++ % colors.length]);
              result.append("];\n");
            }
          //}
        }
      } else {
        //result.append(nodeSrcStr);
        //result.append(" -> ");
        //result.append(nodeDstStr);
        //result.append(";\n");
      }
      */
    }

    result.append("}");

    SaveToFile(result.toString(), fileName);
  }

  public static void GraphCarteqToDot(String fileName, Graph graphA, Graph graphB) {

    String[] colors = new String[] {" [color=\"#C8C142\"", " [color=\"#00FF00\"", " [color=\"#0000FF\"", " [color=\"#FFFF00\"", " [color=\"#FF00FF\"", " [color=\"#00FFFF\"", };

    StringBuffer result = new StringBuffer();
    StringBuffer dotA = new StringBuffer();
    StringBuffer dotB = new StringBuffer();
    StringBuffer dotAB = new StringBuffer();

    result.append("digraph structs {\n");
    result.append("node [shape=none]\n");
    result.append("edge [arrowhead=vee]\n");

    dotA.append("subgraph cluster_0 {\n label = \"shape1\";\n");
    dotB.append("subgraph cluster_1 {\n label = \"shape2\";\n");

    for (Object nodeObj : graphA.getNodes()) {
      SNode node = (SNode)nodeObj;

      node.ToDot(result, "A_");
      result.append("\n");

      if (node.EqualityTransition != null) {

        for (SNode dstB : node.EqualityTransition.keySet())
        {
          dotAB.append("A_" + Integer.toString(node.ID));
          dotAB.append(" -> ");
          dotAB.append("B_" + Integer.toString(dstB.ID));
          dotAB.append(colors[2]);
          dotAB.append("];\n");
        }
      }
    }

    for (Object nodeObj : graphB.getNodes()) {
      SNode node = (SNode)nodeObj;

      node.ToDot(result, "B_");
      result.append("\n");
    }

    for (Object edgeObj : graphA.getEdges()) {

      Graph.Edge edge = (Graph.Edge)edgeObj;

      SNode nodeSrc = (SNode)edge.getSource();
      SNode nodeDst = (SNode)edge.getDestination();
      String nodeSrcStr = "A_" + Integer.toString(nodeSrc.ID);
      String nodeDstStr = "A_" + Integer.toString(nodeDst.ID);

      Map<SSubNode, List<SSubNode>> regionTransition   = nodeSrc.RegionTransition.get(nodeDst);

      if (regionTransition.size() > 0) {

        dotA.append(nodeSrcStr);
        dotA.append(" -> ");

        dotA.append(nodeDstStr);
        dotA.append(colors[1]);
        dotA.append("];\n");
      }
    }

    for (Object edgeObj : graphB.getEdges()) {

      Graph.Edge edge = (Graph.Edge)edgeObj;

      SNode nodeSrc = (SNode)edge.getSource();
      SNode nodeDst = (SNode)edge.getDestination();
      String nodeSrcStr = "B_" + Integer.toString(nodeSrc.ID);
      String nodeDstStr = "B_" + Integer.toString(nodeDst.ID);

      Map<SSubNode, List<SSubNode>> regionTransition = nodeSrc.RegionTransition.get(nodeDst);

      if (regionTransition.size() > 0) {

        dotB.append(nodeSrcStr);
        dotB.append(" -> ");

        dotB.append(nodeDstStr);
        dotB.append(colors[1]);
        dotB.append("];\n");
      }
    }

    dotA.append("}\n");
    dotB.append("}\n");

    result.append(dotA.toString());
    result.append(dotB.toString());
    result.append(dotAB.toString());

    result.append("}");

    SaveToFile(result.toString(), fileName);
  }

  public static void GraphCarteqToDot2(String fileName, Graph graphA, Graph graphB) {

    String[] colors = new String[] {" [color=\"#C8C142\"", " [color=\"#00FF00\"", " [color=\"#0000FF\"", " [color=\"#FFFF00\"", " [color=\"#FF00FF\"", " [color=\"#00FFFF\"", };

    StringBuffer result = new StringBuffer();
    StringBuffer dotA = new StringBuffer();
    StringBuffer dotB = new StringBuffer();
    StringBuffer dotAB = new StringBuffer();

    result.append("digraph structs {\n");
    result.append("node [shape=none]\n");
    result.append("edge [arrowhead=vee]\n");

    dotA.append("subgraph cluster_0 {\n label = \"shape1\";\n");
    dotB.append("subgraph cluster_1 {\n label = \"shape2\";\n");

    for (Object nodeObj : graphA.getNodes()) {
      SNode node = (SNode)nodeObj;

      node.ToDot(result, "A_");
      result.append("\n");

      if (node.EqualityTransition != null) {

        String nodeAStr = "A_" + Integer.toString(node.ID);

        for (Map.Entry<SNode, Map<SSubNode, List<SSubNode>>> entry : node.EqualityTransition.entrySet())
        {
          String nodeBStr = "B_" + Integer.toString(entry.getKey().ID);

          //if (!IsIdentity(entry.getValue())) {
          if (false) {

            for (Map.Entry<SSubNode, List<SSubNode>> subEntry : entry.getValue().entrySet()) {

              for (SSubNode subNodeB : subEntry.getValue()) {

                dotAB.append(nodeAStr + ":");
                dotAB.append(subEntry.getKey().Name);
                dotAB.append(" -> ");
                dotAB.append(nodeBStr+ ":");
                dotAB.append(subNodeB.Name);
                dotAB.append(colors[2]);
                dotAB.append("];\n");
              }
            }
          }
          else {
            dotAB.append(nodeAStr);
            dotAB.append(" -> ");
            dotAB.append(nodeBStr);
            dotAB.append(colors[4]);
            dotAB.append("];\n");
          }
        }
      }
    }

    for (Object nodeObj : graphB.getNodes()) {
      SNode node = (SNode)nodeObj;

      node.ToDot(result, "B_");
      result.append("\n");
    }

    for (Object edgeObj : graphA.getEdges()) {

      Graph.Edge edge = (Graph.Edge)edgeObj;

      SNode nodeSrc = (SNode)edge.getSource();
      SNode nodeDst = (SNode)edge.getDestination();
      String nodeSrcStr = "A_" + Integer.toString(nodeSrc.ID);
      String nodeDstStr = "A_" + Integer.toString(nodeDst.ID);

      Map<SSubNode, List<SSubNode>> regionTransition   = nodeSrc.RegionTransition.get(nodeDst);

      if (regionTransition.size() > 0) {

        dotA.append(nodeSrcStr);
        dotA.append(" -> ");

        dotA.append(nodeDstStr);
        dotA.append(colors[1]);
        dotA.append("];\n");
      }
    }

    for (Object edgeObj : graphB.getEdges()) {

      Graph.Edge edge = (Graph.Edge)edgeObj;

      SNode nodeSrc = (SNode)edge.getSource();
      SNode nodeDst = (SNode)edge.getDestination();
      String nodeSrcStr = "B_" + Integer.toString(nodeSrc.ID);
      String nodeDstStr = "B_" + Integer.toString(nodeDst.ID);

      Map<SSubNode, List<SSubNode>> regionTransition = nodeSrc.RegionTransition.get(nodeDst);

      if (regionTransition.size() > 0) {

        dotB.append(nodeSrcStr);
        dotB.append(" -> ");

        dotB.append(nodeDstStr);
        dotB.append(colors[1]);
        dotB.append("];\n");
      }
    }

    dotA.append("}\n");
    dotB.append("}\n");

    result.append(dotA.toString());
    result.append(dotB.toString());
    result.append(dotAB.toString());

    result.append("}");

    SaveToFile(result.toString(), fileName);
  }

  private static boolean IsIdentity(Map<SSubNode, List<SSubNode>> tr) {
    boolean result = true;

    for (Map.Entry<SSubNode, List<SSubNode>> entry : tr.entrySet()) {

      if (entry.getValue().size() != 1 || !entry.getValue().get(0).Name.equals(entry.getKey().Name)) {
        result = false;
        break;
      }
    }

    return result;
  }

  private static void SaveToFile(String str, String fileName) {
    try {
      File file = new File(fileName);

      FileWriter fw = new FileWriter(file);

      fw.write(str);
      fw.close();
    }
    catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }
}
