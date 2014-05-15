package tvla.core.functional;

import tvla.predicates.Vocabulary;
import tvla.util.Logger;

class NPSpaceCounter {

	public int numStructures = 0;

	public int numNodesNoSharing = 0;
	public int nodesSqr = 0;

	public int numNodesWithSharing = 0;

	public int numLeaves = 0;
	public int numFliks = 0;
	public int numUniqueFliks = 0;

	public boolean visitingFliks = false;
	public int numNPTreeNodes = 0;
	public int numPKTreeNodes = 0;

	public void incrNumTreeNodes() {
		if (visitingFliks)
			numPKTreeNodes ++;
		else
			numNPTreeNodes ++;
	}

	public int size() {
		return 
			16 * numStructures +
			6 * numNodesWithSharing +
			8 * numLeaves +
			5 * numFliks +
			6 * numUniqueFliks +
			8 * numNPTreeNodes +
			8 * numPKTreeNodes
			;
	}

	public int numObjects() {
		return numNodesWithSharing + numLeaves + numFliks + numUniqueFliks +
			numNPTreeNodes + numPKTreeNodes;
	}

	// private HashSet visitedObjects = HashSetFactory.make();

	public static int nextVisitNum = 1;


	public void startNewVisit() {
		nextVisitNum++ ;
	}

	public boolean visited (Countable obj) {
	   // return visitedObjects.contains(obj);
		return obj.visitCount == nextVisitNum;
	}

	public void markVisited (Countable obj) {
	   // visitedObjects.add(obj);
		obj.visitCount = nextVisitNum;
	}

	// NodePredTVS can't inherit from Countable ...
	public boolean visited (NodePredTVS obj) {
		return obj.visitCount == nextVisitNum;
	}

	public void markVisited (NodePredTVS obj) {
		obj.visitCount = nextVisitNum;
	}

	void visit(Countable obj) {
		if ((obj != null) && (! visited(obj))) {
			markVisited(obj);
			obj.computeSpace(this);
		}
	}

	public void printStatistics() {
		Logger.println("Number of structures: " + numStructures);
		Logger.println("Total number of nodes (with normalization): " + numNodesWithSharing);
		Logger.println("Total number of nodes (with no sharing): " + numNodesNoSharing);
		long numbits =
			((long) numStructures) * ((long) Vocabulary.allNullaryPredicates().size());
		numbits +=
			((long) numNodesNoSharing ) * ((long) Vocabulary.allUnaryPredicates().size());
		numbits +=
			((long) nodesSqr) * ((long) Vocabulary.allBinaryPredicates().size());
		Logger.println("Number of tree nodes (in level 1 maps: Node -> (Pred -> Kleene)) : "
							+ numNPTreeNodes);
		Logger.println("Number of distinct (Pred -> Kleene) values (u) : " + numFliks);
		Logger.println("Number of distinct (Pred -> Kleene) values (n) : " + numUniqueFliks);
		Logger.println("Number of tree nodes (in level 2 maps: (Pred -> Kleene)) : " + numPKTreeNodes);
		Logger.println("Number of leaves (packed kleene vectors) : " + numLeaves);
		int hashTblSize =
			HashCons.globalTable.map.size() +
			NormalizedFourIntLeaf.numHashTblEntries();
		if (NodePredTVS.hashCons != null) hashTblSize += NodePredTVS.hashCons.generated();
		Logger.println("Number of entries in normalization hash tables: " + hashTblSize);
		Logger.println("Total number of objects (all the above) : " + numObjects());
		Logger.println("Estimated size of above objects (including Java object overhead) : " + (4 * size()) + " bytes" );
		Logger.println("Estimated size of dense matrix: " + (numbits / 8) + " bytes");
	}

}
