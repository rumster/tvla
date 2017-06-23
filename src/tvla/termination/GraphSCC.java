package tvla.termination;

import tvla.util.graph.Graph;
import tvla.util.graph.GraphFactory;

import java.util.*;

class DfsRec {
	public Object node;
	public Object[] dataArray;
	public int currIndex;
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

	public static void ComputeSCCAll(Graph graph, Collection<Graph> sccNonTrivial, Collection sccTrivial,
			Collection<Object> nodeToSkip) {
		HashSet<Object> nodesLeft = new HashSet<>(graph.getNodes());

		if (nodeToSkip != null)
			nodesLeft.removeAll(nodeToSkip);

		while (nodesLeft.size() > 0) {
			LinkedList<Object> stack = new LinkedList<>();
			HashSet<Object> marked = new HashSet<>();

			double t = System.currentTimeMillis();
			Dfs2(graph, nodesLeft.iterator().next(), stack, nodesLeft);
			// System.out.println("Dfs " + (System.currentTimeMillis() - t) /
			// 1000.0 + " sec. edges count: " + graph.getEdges().size() + ",
			// nodes count: " + graph.getNodes().size());

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
						// if (t > 100)
						// System.out.println("Back dfs " + t / 1000.0 + "
						// sec");
						sccNonTrivial.add(scc);
					} else
						sccTrivial.add(scc.getNodes().iterator().next());
				}
			}

			t2 = System.currentTimeMillis() - t2;
			// System.out.println("All back dfs " + t2 / 1000.0 + " sec");
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
			} else {
				stack.add(node);
				queue.remove(queue.size() - 1);
			}
		}

		// Collection outgoingNodes = graph.getOutgoingNodes(node);
		// nodesLeft.remove(node);
		//
		// for (Object next : outgoingNodes) {
		// if (nodesLeft.contains(next))
		// Dfs(graph, next, stack, nodesLeft);
		// }
		//
		// stack.add(node);
	}

	private static void BackDfs(Graph graph, Object node, Graph sccGraph, HashSet<Object> marked) {
		Collection incomingEdges = graph.getIncomingEdges(node);
		sccGraph.addNode(node);
		marked.remove(node);

		for (Object incomingEdgeObj : incomingEdges) {
			Graph.Edge edge = (Graph.Edge) incomingEdgeObj;
			Object incomingNode = edge.getSource();

			if (marked.remove(incomingNode)) {

				sccGraph.addNode(incomingNode);

				assert !sccGraph.containsEdge(incomingNode, node);
				sccGraph.addEdge(incomingNode, node, edge.getLabel());

				BackDfs(graph, incomingNode, sccGraph, marked);
			} else if (sccGraph.containsNode(incomingNode)) {
				assert !sccGraph.containsEdge(incomingNode, node);
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

					assert !sccGraph.containsEdge(incomingNode, node);
					sccGraph.addEdge(incomingNode, node, edge.getLabel());

					DfsRec newRec = new DfsRec();
					newRec.node = incomingNode;
					newRec.dataArray = graph.getIncomingEdges(incomingNode).toArray();
					newRec.currIndex = 0;

					queue.add(newRec);

					// BackDfs(graph, incomingNode, sccGraph, marked);
				} else if (sccGraph.containsNode(incomingNode)) {
					assert !sccGraph.containsEdge(incomingNode, node);
					sccGraph.addEdge(incomingNode, node, edge.getLabel());
				}
			} else {
				queue.remove(queue.size() - 1);
			}

			// Collection dataArray = graph.getIncomingEdges(node);
			// for (Object incomingEdgeObj : dataArray) {
			// Graph.Edge edge = (Graph.Edge) incomingEdgeObj;
			// Object incomingNode = edge.getSource();
			//
			// if (marked.remove(incomingNode)) {
			//
			// sccGraph.addNode(incomingNode);
			//
			// Assert.assertTrue(!sccGraph.containsEdge(incomingNode, node));
			// sccGraph.addEdge(incomingNode, node, edge.getLabel());
			//
			// BackDfs(graph, incomingNode, sccGraph, marked);
			// } else if (sccGraph.containsNode(incomingNode)) {
			//
			// Assert.assertTrue(!sccGraph.containsEdge(incomingNode, node));
			// sccGraph.addEdge(incomingNode, node, edge.getLabel());
			// }
			// }
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
}