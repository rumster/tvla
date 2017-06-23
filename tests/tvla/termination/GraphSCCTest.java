package tvla.termination;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;
import tvla.util.graph.Graph;
import tvla.util.graph.GraphFactory;

public class GraphSCCTest extends TestCase {
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

		GraphSCC.ComputeNotTrivialSCC(g, sccList);

		Assert.assertEquals(sccList.size(), 3);

		g = sccList.get(0);
		for (int i = 1; i <= 3; i++)
			Assert.assertTrue(g.containsNode(i));
		Assert.assertTrue(g.containsEdge(1, 2));
		Assert.assertTrue(g.containsEdge(2, 3));
		Assert.assertTrue(g.containsEdge(3, 1));
		Assert.assertTrue(GraphSCC.IsSCCSimpleCycle(g));

		g = sccList.get(1);
		for (int i = 9; i <= 11; i++)
			Assert.assertTrue(g.containsNode(i));
		Assert.assertTrue(g.containsEdge(9, 10));
		Assert.assertTrue(g.containsEdge(10, 11));
		Assert.assertTrue(g.containsEdge(11, 9));
		Assert.assertTrue(GraphSCC.IsSCCSimpleCycle(g));

		g = sccList.get(2);
		for (int i = 6; i <= 8; i++)
			Assert.assertTrue(g.containsNode(i));
		Assert.assertTrue(g.containsEdge(6, 7));
		Assert.assertTrue(g.containsEdge(7, 8));
		Assert.assertTrue(g.containsEdge(8, 6));
		Assert.assertTrue(GraphSCC.IsSCCSimpleCycle(g));

		// Test 2
		g = GraphFactory.newGraph(3);
		for (int i = 1; i <= 4; i++)
			g.addNode(i);

		g.addEdge(1, 2);
		g.addEdge(2, 3);
		g.addEdge(3, 4);

		sccList.clear();
		GraphSCC.ComputeNotTrivialSCC(g, sccList);

		Assert.assertEquals(sccList.size(), 0);

		g.addEdge(4, 1);

		sccList.clear();
		GraphSCC.ComputeNotTrivialSCC(g, sccList);

		Assert.assertEquals(sccList.size(), 1);
		g = sccList.get(0);

		for (int i = 1; i <= 4; i++)
			Assert.assertTrue(g.containsNode(i));

		Assert.assertTrue(g.containsEdge(1, 2));
		Assert.assertTrue(g.containsEdge(2, 3));
		Assert.assertTrue(g.containsEdge(3, 4));
		Assert.assertTrue(g.containsEdge(4, 1));
		Assert.assertTrue(GraphSCC.IsSCCSimpleCycle(g));

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
		GraphSCC.ComputeNotTrivialSCC(g, sccList);

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
		Assert.assertTrue(!GraphSCC.IsSCCSimpleCycle(g));

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
		GraphSCC.ComputeNotTrivialSCC(g, sccList);

		Assert.assertEquals(sccList.size(), 1);

		g = sccList.get(0);

		for (int i = 2; i <= 4; i++)
			Assert.assertTrue(g.containsNode(i));

		Assert.assertTrue(g.containsEdge(4, 3));
		Assert.assertTrue(g.containsEdge(3, 2));
		Assert.assertTrue(g.containsEdge(2, 4));
		Assert.assertTrue(GraphSCC.IsSCCSimpleCycle(g));
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

		GraphSCC.ComputeNotTrivialSCC(g, sccList);

		Assert.assertEquals(sccList.size(), 3);

		g = sccList.get(0);
		for (int i = 1; i <= 3; i++)
			Assert.assertTrue(g.containsNode(i));
		Assert.assertTrue(g.containsEdge(1, 2));
		Assert.assertTrue(g.containsEdge(2, 3));
		Assert.assertTrue(g.containsEdge(3, 1));
		Assert.assertTrue(GraphSCC.IsSCCSimpleCycle(g));

		g = sccList.get(1);
		for (int i = 9; i <= 11; i++)
			Assert.assertTrue(g.containsNode(i));
		Assert.assertTrue(g.containsEdge(9, 10));
		Assert.assertTrue(g.containsEdge(10, 11));
		Assert.assertTrue(g.containsEdge(11, 9));
		Assert.assertTrue(GraphSCC.IsSCCSimpleCycle(g));

		g = sccList.get(2);
		for (int i = 6; i <= 8; i++)
			Assert.assertTrue(g.containsNode(i));
		Assert.assertTrue(g.containsEdge(6, 7));
		Assert.assertTrue(g.containsEdge(7, 8));
		Assert.assertTrue(g.containsEdge(8, 6));
		Assert.assertTrue(GraphSCC.IsSCCSimpleCycle(g));

		sccList.clear();
		GraphSCC.ComputeTrivialSCC(gOrg, trivialSccList);

		Assert.assertEquals(trivialSccList.size(), 2);
		Assert.assertTrue(trivialSccList.contains(5));
		Assert.assertTrue(trivialSccList.contains(4));

		List<Object> nodesToSkip = Arrays.asList((Object) 7, 2);
		sccList.clear();
		GraphSCC.ComputeNotTrivialSCC(gOrg, sccList, nodesToSkip);

		Assert.assertEquals(sccList.size(), 1);
		g = sccList.get(0);
		for (int i = 9; i <= 11; i++)
			Assert.assertTrue(g.containsNode(i));
		Assert.assertTrue(g.containsEdge(9, 10));
		Assert.assertTrue(g.containsEdge(10, 11));
		Assert.assertTrue(g.containsEdge(11, 9));
		Assert.assertTrue(GraphSCC.IsSCCSimpleCycle(g));

		nodesToSkip = Arrays.asList((Object) 5, 4);
		trivialSccList.clear();
		GraphSCC.ComputeTrivialSCC(gOrg, trivialSccList, nodesToSkip);

		Assert.assertEquals(trivialSccList.size(), 0);
	}
}