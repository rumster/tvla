package tvla.core;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import tvla.core.base.PredicateEvaluator;
import tvla.core.common.NodePair;
import tvla.exceptions.SemanticErrorException;
import tvla.logic.Kleene;
import tvla.predicates.DynamicVocabulary;
import tvla.predicates.Predicate;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;

/**
 * Build equivalence classes of structures according to the isomorphism equivalence
 * relation.
 */
public class IsomorphismEquivalenceClassCreator {

	private static long totalInputStructures = 0;
	private static long totalEqClasses = 0;
	private static long totalNonIsomorphic = 0;
	
	/** 
	 * Represents the signature of a node - only nodes with the same signature
	 * can match in the isomorphism. 
	 * (implemented as a mutable long)
	 */
	protected static class NodeSignature {
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (value ^ (value >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NodeSignature other = (NodeSignature) obj;
			if (value != other.value)
				return false;
			return true;
		}

		public long value = 0;

		public String toString() {
			return "" + value;
		}
	}

	/**
	 * Signature for structures, only structures with the "equal" signatures
	 * can be isomorphic. Also maintains mapping and reverse mapping between
	 * nodes and their signatures to allow matching nodes of different structures. 
	 */
	protected static class TVSSignature {
		Map<Node, NodeSignature> nodeSignatures = HashMapFactory.make();
		Map<NodeSignature, Collection<Node>> nodeBuckets = HashMapFactory.make();
		/** Signature of the nullary predicates */
		long nullarySignature = 0;
		/** Vocabulary of the structure in question */
		private DynamicVocabulary vocabulary;
		
		/**
		 * Build a tvs signature for the given structure.
		 */
		public TVSSignature(HighLevelTVS structure) {
			vocabulary = structure.getVocabulary();
			// Computation relies on the fact that the vocabulary is the same for
			// all structures that may be isomorphic, thus the order of the predicates 
			// in the vocabulary is fixed and can be relied on in the signature. 
			
			// Compute signature for the nullary predicates. 
			for (Predicate predicate : vocabulary.nullary()) {
				nullarySignature = (nullarySignature * 3) + structure.eval(predicate).kleene();
			}
			
			initialColoring(structure);						
			iterativeColoring(structure);
			//iterativeColoring(structure);
			
			// Collect nodes into buckets according to their signatures
			// hopefully, just one node per signature.
			for (Node node : structure.nodes()) {
				NodeSignature signature = nodeSignatures.get(node);
				Collection<Node> bucket = nodeBuckets.get(signature);
				if (bucket == null) {
					bucket = new ArrayList<Node>();
					nodeBuckets.put(signature, bucket);
				}
				bucket.add(node);
			}			
		}

		/**
		 *  Compute initial node signature - based on local information for the nodes
		 *  unary predicates and self-loop of binaries.
		 */
		private void initialColoring(HighLevelTVS structure) {
			for (Node node : structure.nodes()) {
				NodeSignature signature = new NodeSignature();
				nodeSignatures.put(node, signature);
			}
			for (Predicate predicate : vocabulary.unary()) {
				for (Iterator<Entry<NodeTuple, Kleene>> iterator = structure.iterator(predicate); iterator.hasNext(); ) {
					Entry<NodeTuple, Kleene> entry = iterator.next();
					NodeSignature signature = nodeSignatures.get((Node) entry.getKey());
					signature.value = (signature.value * 3) + entry.getValue().kleene();
				}
			}
			NodePair[] selfloops = createSelfloops(nodeSignatures);
			for (Predicate predicate : vocabulary.binary()) {
				PredicateEvaluator evaluator = PredicateEvaluator.evaluator(predicate, structure);
				int i = 0;
				for (NodeSignature signature : nodeSignatures.values()) {
					signature.value = (signature.value * 3) + evaluator.eval(selfloops[i++]).kleene();
				}
			}
		}

		private NodePair[] createSelfloops(Map<Node, NodeSignature> nodeSignatures) {
			NodePair[] selfloops = new NodePair[nodeSignatures.size()];
			int i = 0;
			for (Node node : nodeSignatures.keySet()) {
				selfloops[i++] = (NodePair) NodePair.createPair(node, node);
			}
			return selfloops;
		}

		/**
		 * Recompute the node signatures using the nodes neighbors - 
		 * count how many neighbors each nodes has for each edge (predicate, value, direction)
		 * and neighbor's node signature in the previous iteration.
		 */
		private void iterativeColoring(HighLevelTVS structure) {
			Map<Node, NodeSignature> previousNodeSignatures = nodeSignatures;
			nodeSignatures = HashMapFactory.make();			
			for (Node node : structure.nodes()) {
				NodeSignature signature = new NodeSignature();
				signature.value = previousNodeSignatures.get(node).value;
				nodeSignatures.put(node, signature);
			}
			// Predicates are ordered - can rely on order
			long predId = 1; 
			for (Predicate predicate : vocabulary.binary()) {
				predId *= 31;
				// Nodes are in arbitrary order - signature must be commutative
				for (Iterator<Entry<NodeTuple, Kleene>> iterator = structure.iterator(predicate); iterator.hasNext(); ) {
					Entry<NodeTuple, Kleene> entry = iterator.next();
					NodeTuple tuple = entry.getKey();
					Kleene kleene = entry.getValue();
					Node leftNode = tuple.get(0);
					Node rightNode = tuple.get(1);
					if (leftNode == rightNode) continue; // Self-loop were taken care of already.
					NodeSignature leftSig = nodeSignatures.get(leftNode);
					NodeSignature leftSigPrev = previousNodeSignatures.get(leftNode);
					NodeSignature rightSig = nodeSignatures.get(rightNode);
					NodeSignature rightSigPrev = previousNodeSignatures.get(rightNode);
					leftSig.value += kleene.kleene() * rightSigPrev.value * predId;
					rightSig.value += kleene.kleene() * leftSigPrev.value * predId * 3;
				}
			}
		}
		
		private void initialColoringV2(HighLevelTVS structure) {
			for (Node node : structure.nodes()) {
				NodeSignature signature = new NodeSignature();
				nodeSignatures.put(node, signature);
				for (Predicate predicate : vocabulary.unary()) {
					signature.value = (signature.value * 3) + structure.eval(predicate, node).kleene();
				}
				for (Predicate predicate : vocabulary.binary()) {
					signature.value = (signature.value * 3) + structure.eval(predicate, node, node).kleene();
				}
			}
		}

		private void iterativeColoringV2(HighLevelTVS structure) {
			Map<Node, NodeSignature> previousNodeSignatures = nodeSignatures;
			nodeSignatures = HashMapFactory.make();			
			for (Node node : structure.nodes()) {
				NodeSignature signature = new NodeSignature();
				signature.value = previousNodeSignatures.get(node).value;
				nodeSignatures.put(node, signature);
				for (Predicate predicate : vocabulary.binary()) {
					for (Node other: structure.nodes()) {
						if (node == other) continue; // Self-loop were taken care of already.
						NodeSignature otherSignature = previousNodeSignatures.get(other);
						signature.value += 
							structure.eval(predicate, node, other).kleene() * otherSignature.value * 3 +
							structure.eval(predicate, other, node).kleene() * otherSignature.value; 
					}
				}
			}
		}

		/**
		 * Pretty print signature s.t. string equality means object equality.
		 */
		public String toString() {
			long[] nodeSigs = new long[nodeBuckets.keySet().size()];
			int i = 0;
			for (NodeSignature signature : nodeBuckets.keySet()) {
				nodeSigs[i++] = signature.value;
			}
			Arrays.sort(nodeSigs);
			StringBuilder builder = new StringBuilder();
			String sep = "";
			for (long sig : nodeSigs) {
				builder.append(sep).append(sig);
				sep = ",";
			}
			return builder.toString();
		}
		
		public NodeSignature getSignature(Node node) {
			return nodeSignatures.get(node);
		}
		
		public Collection<Node> getNodesForSignature(NodeSignature signature) {
			return nodeBuckets.get(signature);
		}
		
		/**
		 * hashCode matching the equals below.
		 */
		public int hashCode() {
			int hash = (int) nullarySignature;
			// The nodeBuckets are not sorted, make sure the hash is commutative
			for (Map.Entry<NodeSignature, Collection<Node>> entry : nodeBuckets.entrySet()) {
				hash += entry.getKey().value * entry.getValue().size();
			}
			return hash;
		}

		/**
		 * Equality between signatures is required for possible isomorphism between structures.
		 * Only signatures are compared, not the actual nodes.
		 */
		public boolean equals(Object o) {
			if (!(o instanceof TVSSignature))
				return false;
			// compare as multiset
			TVSSignature other = (TVSSignature) o;
			if (this.nullarySignature != other.nullarySignature) return false;
			if (this.nodeSignatures.size() != other.nodeSignatures.size()) return false;
			if (this.nodeBuckets.size() != other.nodeBuckets.size()) return false;
			// Multiset compare
			for (Map.Entry<NodeSignature, Collection<Node>> entry : nodeBuckets.entrySet()) {
				NodeSignature mySig = entry.getKey();
				Collection<Node> otherNodes = other.nodeBuckets.get(mySig);
				if (otherNodes == null) return false;
				if (otherNodes.size() != entry.getValue().size()) return false;
			}
			return true;
		}
	}

	/**
	 * Compute the equivalence classes of the given groups.
	 * @param groups
	 * @return
	 */
	public static Collection<StructureGroup> compute(Iterable<HighLevelTVS> structures) {
		Map<HighLevelTVS, TVSSignature> structureSignatures = HashMapFactory
				.make();
		Map<TVSSignature, Collection<HighLevelTVS>> candidateBuckets = HashMapFactory
				.make();
		// Go over all structures, compute signature and gather in buckets according to signature.
		for (HighLevelTVS structure : structures) {
			TVSSignature signature = new TVSSignature(structure);
			structureSignatures.put(structure, signature);
			Collection<HighLevelTVS> bucket = candidateBuckets.get(signature);
			if (bucket == null) {
				bucket = new ArrayList<HighLevelTVS>();
				candidateBuckets.put(signature, bucket);
			}
			bucket.add(structure);
			totalInputStructures++;
		}

		Collection<StructureGroup> result = new ArrayList<StructureGroup>();
		Map<Node, Node> nodeMapping = HashMapFactory.make();
		for (Collection<HighLevelTVS> bucket : candidateBuckets.values()) {
			// For each bucket, try to match as many structures as possible to each representative
			// Can be quadratic in the number of structures in each bucket if the signatures
			// are not good indicators of isomorphism.
			while (!bucket.isEmpty()) {
				// Choose the first structure as the current representative
				Iterator<HighLevelTVS> bucketIter = bucket.iterator();
				HighLevelTVS representative = bucketIter.next();
				bucketIter.remove(); // Structure is already dealt with
				StructureGroup eqClass = new StructureGroup(representative);
				eqClass.addMember(representative, StructureGroup.Member.buildIdentityMapping(representative), null);
				// Try to match other isomorphic structures
				while (bucketIter.hasNext()) {
					HighLevelTVS candidate = bucketIter.next();
					nodeMapping.clear();
					if (isomorphic(representative, structureSignatures
							.get(representative), candidate,
							structureSignatures.get(candidate), nodeMapping)) {
						// Found match, remove from bucket and add to result
						bucketIter.remove();
						Map<Node, Node> nodeMappingCopy = HashMapFactory.make(nodeMapping);
						eqClass.addMember(candidate, nodeMappingCopy, null);
					}
				}
				result.add(eqClass);
			}
		}
		totalEqClasses += result.size();
		return result;
	}

	/**
	 * Check for isomorphism between left and right structures using the signatures
	 * to find nodes with matching signatures. Fill the nodeMapping with the found mapping.
	 */
	private static boolean isomorphic(HighLevelTVS left, TVSSignature leftSig,
			HighLevelTVS right, TVSSignature rightSig, Map<Node, Node> nodeMapping) {
		if (left.getVocabulary() != right.getVocabulary()) {
			throw new SemanticErrorException("Vocabulary mismatch in isomorphism check");
		}
		// Check for nullary predicates matching.
        for (Predicate predicate : left.getVocabulary().nullary()) {
        	if (left.eval(predicate) != right.eval(predicate)) {
        		totalNonIsomorphic++;
        		return false;
        	}
        }
        
        Set<Node> mappedNodes = HashSetFactory.make();
		for (Node leftNode : left.nodes()) {
			NodeSignature leftNodeSig = leftSig.getSignature(leftNode);
			Collection<Node> rightNodes = rightSig.getNodesForSignature(leftNodeSig);
			if (rightNodes.size() > 1) {
				/* No exact match. We have 3 options:
				 * 1) Check for all options of all non-tight matching - exponential
				 * 2) Fail - safe approximation 
					  return false;
				 * 3) Heuristically try one match and fail otherwise.
				 *    This is the option currently implemented:
				 *    Effectively try the first one... 
				*/
			}
			boolean foundCandidate = false;
			for (Node rightNode : rightNodes) {
				if (mappedNodes.add(rightNode)) {
					nodeMapping.put(leftNode, rightNode);
					foundCandidate = true;
					break;
				}
			}
			if (!foundCandidate) {
				// Multiple nodes in left are mapped to the same node in right, fail!
	    		totalNonIsomorphic++;
				return false;
			}
		}

		/** 
		 * Check for isomorphism according to all positive arity predicates.
		 */
        for (Predicate predicate : left.getVocabulary().positiveArity()) {
        	// Check that the number of satisfying tuple match
            if (left.numberSatisfy(predicate) != right.numberSatisfy(predicate)) {
        		totalNonIsomorphic++;
                return false;
            }
            // Check that each satisfying tuple has a matching tuple with same value.
            PredicateEvaluator otherEval = PredicateEvaluator.evaluator(predicate, right);
            Iterator<Entry<NodeTuple, Kleene>> thisIt = left.iterator(predicate);
            while (thisIt.hasNext()) {
                Entry<NodeTuple, Kleene> thisEntry = thisIt.next();
                NodeTuple thisTuple = thisEntry.getKey();
                NodeTuple otherTuple = thisTuple.mapNodeTuple(nodeMapping);
                if (thisEntry.getValue() != otherEval.eval(otherTuple)) {
            		totalNonIsomorphic++;
                    return false;
                }
            }
        }		        
		return true;
	}
	
	public static void printStatistics(PrintStream stream) {
        if (totalInputStructures > 0) { 
            stream.println("Frame: " + totalInputStructures + " => " + totalEqClasses + ". nonIso=" + totalNonIsomorphic);
        }
    }
}
