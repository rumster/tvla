/*
 * File: GraphFactory.java 
 * Created on: 19/10/2004
 */

package tvla.util.graph;


/** A Factory of graphs
 * @author maon
 */
public class GraphFactory {
	private static GraphGenerator graphGenerator = HashGraphFactory.getInstance();
	private static BipartiteGraphGenerator bipartiteGraphGenerator = HashGraphFactory.getInstance();
	private static FlowGraphGenerator flowGraphGenerator = HashGraphFactory.getInstance();
	private static ExplodedFlowGraphGenerator explodedFlowGraphGenerator = HashGraphFactory.getInstance();
	
	public static Graph newGraph() {
		return  graphGenerator.newGraph();
	}
	
	public static Graph newGraph(int numOfNodes) {
		return  graphGenerator.newGraph(numOfNodes);
	}

	public static BipartiteGraph newBipartiteGraph() {
		return  bipartiteGraphGenerator.newBipartiteGraph();
	}
	
	public static FlowGraph newFlowGraph(int numOfNodes) {
		return  flowGraphGenerator.newFlowGraph(numOfNodes);
	}
	
	public static ExplodedFlowGraph newExplodedFlowGraph(int numOfNodes) {
		return  explodedFlowGraphGenerator.newExplodedFlowGraph(numOfNodes);
	}
	
	
	///////////////////////////////////////////////////////////////
	////             Specific Graph Generators             ////////
	///////////////////////////////////////////////////////////////

	public static interface GraphGenerator {
		/** Constructs an empty graph.
		 */
		public Graph newGraph(); 

		/** Constructs an empty graph with an hint for the number of nodes.
		 */
		public Graph newGraph(int numOfNodes);
	}

	public  static interface BipartiteGraphGenerator {
		/** Constructs an empty graph.
		 */
		public BipartiteGraph newBipartiteGraph(); 
	}

	public  static interface FlowGraphGenerator {
		/** Constructs an empty graph.
		 */
		public FlowGraph newFlowGraph(); 

		/** Constructs an empty graph with an hint for the number of nodes.
		 */
		public FlowGraph newFlowGraph(int numOfNodes);
	}

	public  static interface ExplodedFlowGraphGenerator {
		/** Constructs an empty graph.
		 */
		public ExplodedFlowGraph newExplodedFlowGraph(); 

		/** Constructs an empty graph with an hint for the number of nodes.
		 */
		public ExplodedFlowGraph newExplodedFlowGraph(int numOfNodes);
	}
	
	
	public  static interface UniformGraphGenerator extends 		
		GraphGenerator, 
		BipartiteGraphGenerator, 
		FlowGraphGenerator, 
		ExplodedFlowGraphGenerator  {
		
	}
	
	
	public interface HashGraphGenerator extends UniformGraphGenerator {
	}
	
	///////////////////////////////////////////////////////////////
	////             Specific Graph Factories              ////////
	////                    Singeltons                     ////////
	///////////////////////////////////////////////////////////////

	private static class HashGraphFactory implements 
          HashGraphGenerator
	{
		private final static HashGraphGenerator theInstance = new HashGraphFactory(); 
		
		public static HashGraphGenerator getInstance() {
			return theInstance;
		}
		
		private HashGraphFactory(){
			// empty body
		}

		
		public Graph newGraph() {
			return  new HashGraph();
		}
		public Graph newGraph(int numOfNodes) {
			return  new HashGraph(1 + (5 * numOfNodes) / 4 , 0.75f);
		}		
		
		public BipartiteGraph newBipartiteGraph() {
			return  new HashBipartiteGraph();
		}
		
		
		public FlowGraph newFlowGraph() {
			return  new HashFlowGraph();
		}
		public FlowGraph newFlowGraph(int numOfNodes) {
			return  new HashFlowGraph(1 + (5 * numOfNodes) / 4 , 0.75f);
		}
		
		
		public ExplodedFlowGraph newExplodedFlowGraph() {
			return  new HashExplodedFlowGraph();
		}
		public ExplodedFlowGraph newExplodedFlowGraph(int numOfNodes) {
			return  new HashExplodedFlowGraph(1 + (5 * numOfNodes) / 4 , 0.75f);
		}
		
	}
	
}
