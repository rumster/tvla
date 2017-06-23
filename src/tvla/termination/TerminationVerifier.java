package tvla.termination;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.analysis.AnalysisStatus;
import tvla.core.Node;
import tvla.util.graph.Graph;
import tvla.util.graph.GraphFactory;
import tvla.util.graph.GraphUtils;

/**
 * Created by BorisD on 10/19/2015.
 */
public class TerminationVerifier {

	public static final TerminationVerifier defaultInstance = new TerminationVerifier();

	public void Analyze(TerminationAnalysisInput terminationAnalysisInput) {

		String mainDir = terminationAnalysisInput.OutputDir;

		List<Graph> graphsSat = new ArrayList<>();
		List<Graph> graphsUnSat = new ArrayList<>();
		Map<Integer, List<Graph>> summaries = new HashMap<>();

		AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.TD_TIME);

		boolean satisfiable = Analyze(terminationAnalysisInput, graphsSat, graphsUnSat, summaries);

		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.TD_TIME);

		if (mainDir != null) {

			DotUtils.SaveGraphsToSingleFile(mainDir + "graph_scc_sat.dt", satisfiable, graphsSat);
			DotUtils.SaveGraphsToSingleFile(mainDir + "graph_scc_unsat.dt", satisfiable, graphsUnSat);
			DotUtils.SaveGraphToSingleFile(mainDir + "graph_main.dt", satisfiable,
					terminationAnalysisInput.RegionTransitionGraph);

			DotUtils.SaveGraphsToMultFiles(mainDir + "sat_scc_", satisfiable, graphsSat);
			DotUtils.SaveGraphsToMultFiles(mainDir + "unsat_scc_", satisfiable, graphsUnSat);
			DotUtils.SaveLevelGraphsToMultFiles(mainDir + "summary_", satisfiable, summaries);
		}

		AnalysisStatus.getActiveStatus().TerminationAnalysisResult = satisfiable;
	}

	private boolean Analyze(TerminationAnalysisInput analysisInput, List<Graph> graphsSatOut,
			List<Graph> graphsUnSatOut, Map<Integer, List<Graph>> summaries) {

		AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.TD_SUM_TIME);

		assert analysisInput.RegionTransitionGraph.getNumberOfNodes() > 0;
		assert analysisInput.RegionTransitionGraph
				.getNumberOfEdges() > analysisInput.RegionTransitionGraph.getNumberOfNodes() * 0.9;

		Map<Integer, List<Graph>> graphs = SummarizeGraph(analysisInput.RegionTransitionGraph, analysisInput.EntryNodes,
				analysisInput.NestingDepth);

		assert graphs.size() > 0;

		summaries.putAll(graphs);
		List<Graph> sccList = new ArrayList<>();

		for (Map.Entry<Integer, List<Graph>> entry : graphs.entrySet()) {
			for (Graph g : entry.getValue())
				GraphSCC.ComputeNotTrivialSCC(g, sccList);
		}

		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.TD_SUM_TIME);

		AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.TD_SAT_TIME);

		for (Graph g : sccList) {
			if (IsSatisfiable(g, graphsUnSatOut)) {
				graphsSatOut.add(g);
			}
		}

		/*
		 * List<Graph> oldUnSat = graphsUnSatOut; List<Graph> newSat = new
		 * ArrayList<>(); List<Graph> newUnSat = null;
		 * 
		 * for (int i = 0; i < 1; i++) { newUnSat = new ArrayList<>(); for
		 * (Graph g : oldUnSat) { if (IsSatisfiable(g, newUnSat)) {
		 * newSat.add(g); } } oldUnSat = newUnSat; }
		 * 
		 * Map<Integer, List<Graph>> mapSat = new HashMap<>(); Map<Integer,
		 * List<Graph>> mapUnSat = new HashMap<>(); mapSat.put(1, newSat);
		 * mapUnSat.put(1, newUnSat);
		 * 
		 * DotUtils.SaveLevelGraphsToMultFiles("C:\\temp\\graphs\\new_sat_",
		 * true, mapSat);
		 * DotUtils.SaveLevelGraphsToMultFiles("C:\\temp\\graphs\\new_unsat_",
		 * true, mapUnSat);
		 */

		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.TD_SAT_TIME);

		return graphsUnSatOut.size() == 0;
		// return newUnSat.size() == 0;
	}

	private boolean IsReachable(Graph graph, Graph subGraph, List<RTNode> nodes) {
		Set set = GraphUtils.getReachableNodes(graph, nodes, true, true);

		for (Object node : subGraph.getNodes()) {
			if (set.contains(node))
				return true;
		}

		return false;
	}

	private boolean IsSatisfiable(Graph g, List<Graph> unSatSCC) {

		HashSet<Object> trivialScc = new HashSet<>();
		List<Graph> nonTrivialScc = new ArrayList<>();
		HashSet<Object> cutPoints = new HashSet<>();

		Graph rtGraph = CreateRegionTransitionGraph(g);
		GraphSCC.ComputeSCCAll(rtGraph, nonTrivialScc, trivialScc, null);

		assert rtGraph.getNumberOfNodes() > 0;
		assert rtGraph.getNumberOfEdges() > 0;
		assert (nonTrivialScc.size() > 0 || trivialScc.size() > 0);

		ApplyMarking(rtGraph, trivialScc, cutPoints);

		for (Graph nonTrivialSCCRT : nonTrivialScc) {

			boolean isFrame = true;

			for (Object node : nonTrivialSCCRT.getNodes()) {

				if (rtGraph.getOutDegree(node) != 1 || rtGraph.getInDegree(node) != 1) {
					isFrame = false;
					break;
				}
			}

			if (isFrame) {
				for (Object nodeObj : nonTrivialSCCRT.getNodes()) {
					RTSubNode subNode = (RTSubNode) nodeObj;
					subNode.Type = RTSubNode.RTSubNodeType.Frame;
				}
			}
		}

		int unSatCount = unSatSCC.size();
		GraphSCC.ComputeNotTrivialSCC(g, unSatSCC, cutPoints);

		/*
		 * if (unSatSCC.size() > 0) { List<Graph> g1 = new ArrayList<>();
		 * g1.add(g); List<Graph> g2 = new ArrayList<>();
		 * g2.add(unSatSCC.get(unSatSCC.size() - 1));
		 * 
		 * DotUtils.SaveGraphsToSingleFile("C:\\temp\\graphs\\1.dt", true, g1);
		 * DotUtils.SaveGraphsToSingleFile("C:\\temp\\graphs\\2.dt", true, g2);
		 * }
		 */

		boolean result = unSatSCC.size() == unSatCount;

		return result;
	}

	private void ApplyMarking(Graph rtGraph, HashSet<Object> trivialScc, HashSet<Object> cutPoints) {
		Map<Object, Integer> refCount = new HashMap<>();

		// Mark just allocated nodes
		for (Object subNodeObj : trivialScc) {

			RTSubNode subNode = (RTSubNode) subNodeObj;

			if (rtGraph.getInDegree(subNodeObj) == 0) {

				int markVersion = RTSubNode.s_MarkVersion++;
				LinkedList<RTSubNode> allocated = new LinkedList<>();
				allocated.add(subNode);

				while (allocated.size() > 0) {
					// Pop
					RTSubNode node = allocated.removeFirst();
					node.MarkVersion = markVersion;
					node.RefCount++;

					for (Object nodeDstObj : rtGraph.getOutgoingNodes(node)) {

						RTSubNode nodeDst = (RTSubNode) nodeDstObj;

						if (nodeDst.MarkVersion < markVersion) {
							allocated.add(nodeDst);
						}
					}
				}
			}
		}

		// Mark all allocated descendants
		for (Object subNodeObj : rtGraph.getNodes()) {

			RTSubNode subNode = (RTSubNode) subNodeObj;

			if (subNode.DegreeIn < subNode.RefCount) {
				subNode.Type = RTSubNode.RTSubNodeType.NewNode;
			}
		}

		// Mark the cutpoints
		for (Object subNodeObj : trivialScc) {

			RTSubNode subNode = (RTSubNode) subNodeObj;

			if (subNode.Type == RTSubNode.RTSubNodeType.Default) {
				subNode.Type = RTSubNode.RTSubNodeType.CutPoint;
				cutPoints.add(subNode.Parent);
			}
		}
	}

	private Map<Integer, List<Graph>> SummarizeGraph(final Graph g, List<RTNode> entryNodes, int nestingDepth) {

		Map<Integer, List<Graph>> result = new HashMap<>();

		if (nestingDepth > 1) {
			for (RTNode entryNode : entryNodes) {
				SummarizeGraph(g, entryNode, result);
			}
		} else {
			result.put(1, new ArrayList<Graph>() {
				{
					add(g);
				}
			});
		}

		return result;
	}

	private Graph CreateRegionTransitionGraph(Graph transitionGraph) {

		Graph result = GraphFactory.newGraph();

		for (Object edgeObj : transitionGraph.getEdges()) {
			Graph.Edge edge = (Graph.Edge) edgeObj;

			RTNode nodeSrc = (RTNode) edge.getSource();
			RTNode nodeDst = (RTNode) edge.getDestination();
			Map<Node, List<Node>> transition = (Map<Node, List<Node>>) edge.getLabel();

			for (RTSubNode subNode : nodeSrc.GetSubNodes().values())
				result.addNode(subNode);

			for (RTSubNode subNode : nodeDst.GetSubNodes().values())
				result.addNode(subNode);

			for (Map.Entry<Node, List<Node>> entry : transition.entrySet()) {

				RTSubNode subNodeSrc = nodeSrc.GetSubNodes().get(entry.getKey().id());

				for (Node subNodeTvsDst : entry.getValue()) {
					RTSubNode subNodeDst = nodeDst.GetSubNodes().get(subNodeTvsDst.id());

					result.addEdge(subNodeSrc, subNodeDst);
				}
			}
		}

		return result;
	}

	private void SummarizeGraph(Graph g, RTNode node, Map<Integer, List<Graph>> summaries) {

		HashSet<RTNode> visited = new HashSet<>();
		LinkedList<RTNode> workList = new LinkedList<>();

		Map<Integer, Graph> result = new HashMap<>();
		Map<Integer, List<RTNode>> parentNodes = new HashMap<>();

		workList.add(node);

		// Split the graphs
		while (!workList.isEmpty()) {

			RTNode currNode = workList.removeLast();
			Graph currNodeGraph = result.getOrDefault(currNode.LoopIndex, null);

			if (currNodeGraph == null) {
				currNodeGraph = GraphFactory.newGraph();

				result.put(currNode.LoopIndex, currNodeGraph);
				parentNodes.put(currNode.LoopIndex, new ArrayList<RTNode>());

				summaries.putIfAbsent(currNode.LoopIndex, new ArrayList<Graph>());
			}

			currNodeGraph.addNode(currNode);

			for (Object edgeObj : g.getOutgoingEdges(currNode)) {

				Graph.Edge edge = (Graph.Edge) edgeObj;
				RTNode nodeDst = (RTNode) edge.getDestination();

				// loop enter nodes
				if (currNode.LoopIndex < nodeDst.LoopIndex) {
					nodeDst.Type = RTNode.RTNodeType.ParentLoopEnter;
					currNode.Type = RTNode.RTNodeType.NestedLoopEnter;

					parentNodes.get(currNode.LoopIndex).add(currNode);
				}

				if (currNode.LoopIndex == nodeDst.LoopIndex) {

					currNodeGraph.addNode(nodeDst);
					currNodeGraph.addEdge(currNode, nodeDst, edge.getLabel());
				}

				if (visited.add(nodeDst))
					workList.add(nodeDst);
			}
		}

		for (int i = result.size() - 1; i >= 1; i--) {
			Graph graphParent = result.get(i);
			Graph graphNested = result.get(i + 1);

			summaries.get(i + 1).add(graphNested);

			for (Object nodeParentObj : parentNodes.get(i)) {

				List<List> paths = GetAllPaths(nodeParentObj, g, graphParent, graphNested, new HashSet<>());
				HashSet<String> hash = new HashSet<>();

				for (List path : paths) {

					RTNode nodeExit = (RTNode) ((Graph.Edge) path.get(path.size() - 1)).getDestination();
					Map<Node, List<Node>> tr = ComposeTransitionRelation(path);

					String trHash = GetTRHash((RTNode) nodeParentObj, nodeExit, tr);

					if (hash.add(trHash)) {
						RTNode summaryNode = new RTNode(nodeExit.Location, nodeExit.Structure);
						summaryNode.Label = "Summary";
						summaryNode.Type = RTNode.RTNodeType.NestedLoopExit;
						nodeExit.Type = RTNode.RTNodeType.NestedLoopExit;

						graphParent.addNode(summaryNode);
						graphParent.addEdge(nodeParentObj, summaryNode, tr);
						graphParent.addEdge(summaryNode, nodeExit, summaryNode.GetIdentityRT());
					}
				}
			}
		}

		summaries.get(1).add(result.get(1));
	}

	private List<List> GetAllPaths(Object nodeSrc, Graph mainGraph, Graph outerGraph, Graph nestedGraph,
			HashSet<Object> marked) {

		marked.add(nodeSrc);

		List<List> result = new ArrayList<>();

		for (Object edgeObj : mainGraph.getOutgoingEdges(nodeSrc)) {
			Graph.Edge edge = (Graph.Edge) edgeObj;
			Object dst = edge.getDestination();

			if (marked.contains(dst))
				continue;

			if (nestedGraph.containsNode(dst)) {

				List<List> paths = GetAllPaths(dst, mainGraph, outerGraph, nestedGraph, marked);

				for (List path : paths) {
					path.add(0, edge);
				}

				result.addAll(paths);
			} else if (outerGraph.containsNode(dst)) {
				List resultList = new ArrayList();
				resultList.add(edge);

				result.add(resultList);
			}
		}

		marked.remove(nodeSrc);

		return result;
	}

	private Map<Node, List<Node>> ComposeTransitionRelation(List path) {

		Graph.Edge edge = (Graph.Edge) path.get(0);

		Map<Node, List<Node>> currTr = (Map<Node, List<Node>>) edge.getLabel();

		for (int i = 1; i < path.size(); i++) {

			edge = (Graph.Edge) path.get(i);

			if (edge == null)
				continue;

			Map<Node, List<Node>> newTr = new HashMap<>();
			Map<Node, List<Node>> nextTr = (Map<Node, List<Node>>) edge.getLabel();

			for (Map.Entry<Node, List<Node>> entry : currTr.entrySet()) {

				Node currNode = entry.getKey();
				newTr.putIfAbsent(currNode, new ArrayList<Node>());

				HashSet<Node> nextNodes = new HashSet<>(newTr.get(currNode));

				for (Node node : entry.getValue()) {
					nextNodes.addAll(nextTr.get(node)); // TODO some more
														// efficient hashing
				}

				newTr.put(currNode, new ArrayList(nextNodes));
			}

			currTr = newTr;
		}

		return currTr;
	}

	private String GetTRHash(RTNode nodeSrc, RTNode nodeDst, Map<Node, List<Node>> tr) {
		StringBuffer result = new StringBuffer();

		result.append(nodeSrc.ID);
		result.append("+");
		result.append(nodeDst.ID);
		result.append("+");

		for (Map.Entry<Node, List<Node>> entry : tr.entrySet()) {

			// TODO deal with order
			result.append(entry.getKey().id());
			result.append(":");

			for (Node node : entry.getValue()) {

				result.append(node.id());
				result.append(",");
			}
			result.append("+");
		}

		return result.toString();
	}
}