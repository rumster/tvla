package tvla.termination;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import tvla.core.Node;
import tvla.util.graph.Graph;

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

	public static void SaveLevelGraphsToMultFiles(String pathPrefix, boolean satisfiable,
			Map<Integer, List<Graph>> graphs) {

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

	private static String GraphToDot(boolean satisfiable, Object... graphs) {

		StringBuffer result = new StringBuffer();

		result.append("digraph structs {\n");
		result.append("node [shape=none]\n");
		result.append("edge [arrowhead=vee]\n");

		for (Object gObj : graphs) {

			Graph g = (Graph) gObj;
			for (Object nodeObj : g.getNodes()) {
				RTNode node = (RTNode) nodeObj;

				node.ToDot(result);
				result.append("\n");
			}

			for (Object edgeObj : g.getEdges()) {

				Graph.Edge edge = (Graph.Edge) edgeObj;

				RTNode nodeSrc = (RTNode) edge.getSource();
				RTNode nodeDst = (RTNode) edge.getDestination();
				String nodeSrcStr = nodeSrc.toString() + ":";
				String nodeDstStr = nodeDst.toString() + ":";

				Map<Node, List<Node>> regionTransition = (Map<Node, List<Node>>) edge.getLabel();
				Map<Integer, RTSubNode> subNodesSrc = nodeSrc.GetSubNodes();
				Map<Integer, RTSubNode> subNodesDst = nodeDst.GetSubNodes();

				String[] colors = new String[] { " [color=\"#C8C142\"", " [color=\"#00FF00\"", " [color=\"#0000FF\"",
						" [color=\"#FFFF00\"", " [color=\"#FF00FF\"", " [color=\"#00FFFF\"", };

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
								// result.append(" [color=gray");
								// result.append(Math.min(60, i++ * 10));
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

	private static void SaveToFile(String str, String fileName) {
		try {
			File file = new File(fileName);

			FileWriter fw = new FileWriter(file);

			fw.write(str);
			fw.close();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
}
