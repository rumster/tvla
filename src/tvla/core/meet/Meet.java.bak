package tvla.core.meet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org._3pq.jgrapht.Edge;
import org._3pq.jgrapht.graph.MatchingGraph;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.Engine;
import tvla.core.Canonic;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.StructureGroup;
import tvla.core.TVS;
import tvla.core.TVSFactory;
import tvla.core.TVSSet;
import tvla.core.base.PredicateEvaluator;
import tvla.core.base.PredicateUpdater;
import tvla.core.common.ModifiedPredicates;
import tvla.core.common.NodeTupleIterator;
import tvla.core.generic.GenericBlur;
import tvla.logic.Kleene;
import tvla.predicates.DynamicVocabulary;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.EmptyIterator;
import tvla.util.HashMapFactory;
import tvla.util.Pair;
import tvla.util.SimpleIterator;

/** An implementation of the meet, embedding, and isomorphism checking operations on 
 * general 3-valued structures, as described in the paper
 * "Combining Shape Analyses by Intersecting Abstractions"
 * in the proceedings of VMCAI'06.
 * 
 * @author Gilad Arnold
 * @since 2004-01-18
 */
public class Meet {
	// Used to create canonical maps.
	protected static GenericBlur genericBlur = new GenericBlur();

	// A reusable map from canonical names to sets of nodes with those names.
	private static Map<Canonic, Collection<Node>> dummy = new HashMap<Canonic, Collection<Node>>();

	// Statistics related fields
	public static int totalNumberOfTvsMeets;
	public static int successfullTvsMeets;
	
	/** Computes the meet of two structures.
	 * 
	 * @param lTvs A 3-valued structure.
	 * @param rTvs A 3-valued structure.
	 * @return A set of structures.
	 */
	public static TVSSet meet(final TVS lTvs, final TVS rTvs) {
		TVSSet meet = TVSFactory.getInstance().makeEmptySet(TVSFactory.JOIN_CONCRETE);
        DynamicVocabulary lVoc = lTvs.getVocabulary();
        DynamicVocabulary rVoc = rTvs.getVocabulary();
        DynamicVocabulary newVoc = lVoc.union(rVoc);
        DynamicVocabulary sharedValue = lVoc.intersection(rVoc);

		// Verify nullary predicate agreement.
		for (Predicate predicate : sharedValue.nullary()) {
			if (!Kleene.agree(rTvs.eval(predicate), lTvs.eval(predicate)))
				return meet;
		}
		
		
		// Build bipartite graph nodes with default min/max matching quotas
		MatchingGraph g = new MatchingGraph();
		Map<Node, NodeWrapper> lNodesToWrappers = new HashMap<Node, NodeWrapper>(
				rTvs.nodes().size());
		Map<Node, NodeWrapper> rNodesToWrappers = new HashMap<Node, NodeWrapper>(
				rTvs.nodes().size());

		for (Node lNode : lTvs.nodes()) {
			NodeWrapper lNodeWrapper = new NodeWrapper(0, lNode);
			lNodesToWrappers.put(lNode, lNodeWrapper);
			g.addVertex(lNodeWrapper, 1, 1);
		}

		for (Node rNode : rTvs.nodes()) {
			NodeWrapper rNodeWrapper = new NodeWrapper(1, rNode);
			rNodesToWrappers.put(rNode, rNodeWrapper);
			g.addVertex(rNodeWrapper, 1, 1);
		}

		// Build canonical maps for both structure.
		Map<Node, Canonic> rCanonic = new HashMap<Node, Canonic>(
				rTvs.nodes().size());
		genericBlur.makeExtendedCanonicMaps(rTvs, rCanonic, dummy, sharedValue);
		dummy.clear();
	      Map<Node, Canonic> lCanonic = new HashMap<Node, Canonic>(lTvs.nodes().size());
		genericBlur.makeExtendedCanonicMaps(lTvs, lCanonic, dummy, sharedValue);
		dummy.clear();

		// Build graph edges and update maximal matching quota for summaries
		Predicate sm = Vocabulary.sm;
		for (Node lNode : lTvs.nodes()) {
			Canonic lNodeCanonic = lCanonic.get(lNode);
			NodeWrapper lNodeWrapper = lNodesToWrappers.get(lNode);

			for (Node rNode : rTvs.nodes()) {
				Canonic rNodeCanonic = rCanonic
						.get(rNode);
				NodeWrapper rNodeWrapper = rNodesToWrappers
						.get(rNode);

				// Verify agreement and build edges.
				if (lNodeCanonic.agreesWith(rNodeCanonic)) {
					g.addEdge(lNodeWrapper, rNodeWrapper);

					// Update vertices' max matching bound following their degree.
					if (lTvs.eval(sm, lNode) != Kleene.falseKleene)
						g.setWMax(lNodeWrapper, g.degreeOf(lNodeWrapper));
					if (rTvs.eval(sm, rNode) != Kleene.falseKleene)
						g.setWMax(rNodeWrapper, g
								.degreeOf(rNodeWrapper));
				}
			}
		}

		// Generate all generalized perfect matchings of the graph.
		Set<Set<Edge>> matches = g.AbMatchEnum(null);

		Triple[] unaryTriples = null;
		Triple[] binaryTriples = null;
		
		// Iterate on each perfect matching, verify k-arity predicate
		// agreement and build Meet structure.
		for (Set<Edge> edges : matches) {
			// Build Meet TVS nodes and map them to corresponding match edges.
			HighLevelTVS meetTvs = TVSFactory.getInstance().makeEmptyTVS(newVoc);
			Map<Node, Edge> nodesToEdges = new HashMap<Node, Edge>(edges.size());
			for (Edge edge : edges) {
				Node node = meetTvs.newNode();
				nodesToEdges.put(node, edge);
			}
			
            // Iterate on all positive arity predicates, verifying agreement and
            // updating Meet predicate values.
            // TODO Consider sparse implementation
			
			
            int size = meetTvs.numOfNodes();
			if (unaryTriples == null || unaryTriples.length != size) {
			    unaryTriples = new Triple[size];
		        binaryTriples = new Triple[size * size];
			}
			
			// Build mappings between the new nodes and the old nodes
			Map<Node,Node> leftMapping = HashMapFactory.make();
			Map<Node,Node> rightMapping = HashMapFactory.make();
			
			// Build Unary
			{
                int i = 0;
                for (Node node : meetTvs.nodes()) {
                    Edge edge = (Edge) nodesToEdges.get(node);
                    Node left = getSource(edge);
					Node right = getTarget(edge);
					unaryTriples[i++] = new Triple(node, left, right);
					leftMapping.put(node, left);
					rightMapping.put(node, right);
                }   
			}
            if (!meetPredicatesWithTriples(lTvs, rTvs, lVoc, rVoc, meetTvs, unaryTriples, newVoc.unary())) {
                continue;
            }
            
            // Build Binary
            {
                int i = 0;
                for (Iterator<? extends NodeTuple> meetTupleIt = NodeTupleIterator
                        .createIterator(meetTvs.nodes(), 2); meetTupleIt
                        .hasNext();) {
                    NodeTuple meetTuple = meetTupleIt.next();
                    Edge oneEdge = nodesToEdges.get(meetTuple.get(0));
                    Edge twoEdge = nodesToEdges.get(meetTuple.get(1));
                    NodeTuple lTuple = NodeTuple.createPair(getSource(oneEdge), getSource(twoEdge));
                    NodeTuple rTuple = NodeTuple.createPair(getTarget(oneEdge), getTarget(twoEdge));
                    binaryTriples[i++] = new Triple(meetTuple, lTuple, rTuple);
                }
                
            }            
            if (!meetPredicatesWithTriples(lTvs, rTvs, lVoc, rVoc, meetTvs, binaryTriples, newVoc.binary())) {
                continue;
            }
            if (!meetPredicatesKary(lTvs, rTvs, lVoc, rVoc, newVoc, meetTvs, nodesToEdges)) {
                continue;
            }

			// Add nullary predicate Meet values.
			for (Predicate predicate : newVoc.nullary()) {
				meetTvs.update(predicate, Kleene.meet(lTvs.eval(predicate),
						rTvs.eval(predicate)));
			}

            // TODO Better granularity
            ModifiedPredicates.modify(meetTvs,Vocabulary.active);

            if (lTvs.getStructureGroup() != null || rTvs.getStructureGroup() != null) {
	            StructureGroup meetGroup = new StructureGroup(meetTvs);
	            meetGroup.addMember(lTvs.getStructureGroup(), leftMapping, null);
	            meetGroup.addMember(rTvs.getStructureGroup(), rightMapping, null);
	            meetTvs.setStructureGroup(meetGroup);
            }

            meet.mergeWith(meetTvs);
		}

		return meet;
	}

	static class Triple {
	    NodeTuple meet;
	    NodeTuple left;
	    NodeTuple right;
        public Triple(NodeTuple meet, NodeTuple left, NodeTuple right) {
            super();
            this.meet = meet;
            this.left = left;
            this.right = right;
        }
	}
	
    private static Node getSource(Edge edge) {
        NodeWrapper nodeWrapper = (NodeWrapper) edge.getSource();
        return nodeWrapper.getNode();        
    }

    private static Node getTarget(Edge edge) {
        NodeWrapper nodeWrapper = (NodeWrapper) edge.getTarget();
        return nodeWrapper.getNode();        
    }
    
    private static boolean meetPredicatesWithTriples(final TVS lTvs, final TVS rTvs,
            DynamicVocabulary lVoc, DynamicVocabulary rVoc, HighLevelTVS meetTvs,
            Triple[] triples, Set<Predicate> predicates) {
        boolean isMatch = true;
        for (Predicate predicate : predicates) {
            if (lTvs.numberSatisfy(predicate) == 0 && rTvs.numberSatisfy(predicate) == 0) {
                continue;
            }
            PredicateEvaluator lEval = PredicateEvaluator.evaluator(predicate, lTvs);
            PredicateEvaluator rEval = PredicateEvaluator.evaluator(predicate, rTvs);
            PredicateUpdater meetUpdate = PredicateUpdater.updater(predicate, meetTvs);            
            if (!rVoc.contains(predicate)) {
                for (Triple triple : triples) {
                    meetUpdate.update(triple.meet, lEval.eval(triple.left));
                }
            } else if (!lVoc.contains(predicate)) {
                for (Triple triple : triples) {
                    meetUpdate.update(triple.meet, rEval.eval(triple.right));
                }
            } else {
                for (Triple triple : triples) {
                    // Verify Kleene values agreement for tuples.
                    Kleene lValue = lEval.eval(triple.left);
                    Kleene rValue = rEval.eval(triple.right);
                    if (!Kleene.agree(lValue, rValue)) {
                        isMatch = false;
                        break;
                    }
        
                    // Update Meet value (Kleene) for new TVS node tuple.
                    meetUpdate.update(triple.meet, Kleene.meet(lValue, rValue));
                }
            }

            if (!isMatch)
                break;
        }
        return isMatch;
    }

    private static boolean meetPredicatesKary(final TVS lTvs, final TVS rTvs, 
            DynamicVocabulary lVoc, DynamicVocabulary rVoc, DynamicVocabulary newVoc,
            HighLevelTVS meetTvs, Map<Node, Edge> nodesToEdges) {
        boolean isMatch = true;        
        for (Predicate predicate : newVoc.kary()) {
            PredicateEvaluator lEval = PredicateEvaluator.evaluator(predicate, lTvs);
            PredicateEvaluator rEval = PredicateEvaluator.evaluator(predicate, rTvs);
            PredicateUpdater meetUpdate = PredicateUpdater.updater(predicate, meetTvs);
            
            boolean lOnly = !rVoc.contains(predicate);
            boolean rOnly = !lVoc.contains(predicate);
        	Node[] lTupleTmp = new Node[predicate.arity()];
        	Node[] rTupleTmp = new Node[predicate.arity()];
        	for (Iterator<? extends NodeTuple> meetTupleIt = NodeTupleIterator
        			.createIterator(meetTvs.nodes(), predicate.arity()); meetTupleIt
        			.hasNext();) {
        		// Create node tuples of predicate arity.
        	    // TODO Consider only building tuples if needed
        		NodeTuple meetTuple = (NodeTuple) meetTupleIt.next();

        		for (int i = 0; i < lTupleTmp.length; i++) {
        			Edge edge = (Edge) nodesToEdges.get(meetTuple.get(i));
        			NodeWrapper lNodeWrapper = (NodeWrapper) edge
        					.getSource();
        			lTupleTmp[i] = lNodeWrapper.getNode();
        			NodeWrapper rNodeWrapper = (NodeWrapper) edge
        					.getTarget();
        			rTupleTmp[i] = rNodeWrapper.getNode();
        		}
        		if (lOnly) {
                    NodeTuple lTuple = NodeTuple.createTuple(lTupleTmp);
                    meetUpdate.update(meetTuple, lEval.eval(lTuple));
                    continue;
        		}
                if (rOnly) {
                    NodeTuple rTuple = NodeTuple.createTuple(rTupleTmp);
                    meetUpdate.update(meetTuple, rEval.eval(rTuple));
                    continue;
                }
                NodeTuple lTuple = NodeTuple.createTuple(lTupleTmp);
        		NodeTuple rTuple = NodeTuple.createTuple(rTupleTmp);
                // Verify Kleene values agreement for tuples.
        		Kleene lValue = lEval.eval(lTuple);
                Kleene rValue = rEval.eval(rTuple);
                if (!Kleene.agree(lValue, rValue)) {
        			isMatch = false;
        			break;
        		}

        		// Update Meet value (Kleene) for new TVS node tuple.
        		meetUpdate.update(meetTuple, Kleene.meet(lValue, rValue));
        	}

        	if (!isMatch)
        		break;
        }
        return isMatch;
    }
	
    
    /** Computes the meet of two sets of structures by computing the meet of
     * each structure from the first set and each structure in the second set
     * and joining the result.
     * 
     * @param lSet A set of high-level structures.
     * @param rSet A set of high-level structures.
     * @return A set of high-level structures.
     */
    public static Iterable<HighLevelTVS> meet(final Iterable<HighLevelTVS> lSet, final Iterable<HighLevelTVS> rSet) {
        return new Iterable<HighLevelTVS>() {
            public Iterator<HighLevelTVS> iterator() {
                return meet(lSet.iterator(), rSet.iterator());
            }
        };
    }

    public static Iterator<HighLevelTVS> meet(Iterator<HighLevelTVS> lSetIt, Iterator<HighLevelTVS> rSetIt) {
        if (!lSetIt.hasNext()) {
            return EmptyIterator.instance();
        } else if (!rSetIt.hasNext()) {
            return EmptyIterator.instance();
        }
        TVSSet lSet = TVSFactory.getInstance().makeEmptySet(TVSFactory.JOIN_CONCRETE);
        lSet.mergeWith(lSetIt);
        TVSSet rSet = TVSFactory.getInstance().makeEmptySet(TVSFactory.JOIN_CONCRETE);
        rSet.mergeWith(rSetIt);
        
        Engine.activeEngine.getAnalysisStatus().startTimer(AnalysisStatus.MEET_TIME);
        
        // Assume all structures in the same set have the same vocabulary
        DynamicVocabulary lVoc = lSet.iterator().next().getVocabulary();
        DynamicVocabulary rVoc = rSet.iterator().next().getVocabulary();
        DynamicVocabulary shared = lVoc.intersection(rVoc);
        
        MeetSignatureStrategy strategy = new UniqueStrategy(lSet, rSet, shared);
        
        // Partition structures to buckets according to the definite nullary predicates
        final Map<Object, Pair<Collection<TVS>,Collection<TVS>>> match = HashMapFactory.make();       
        for (HighLevelTVS lTvs : lSet) {
            Object signature = strategy.sign(lTvs);
            Pair<Collection<TVS>, Collection<TVS>> bucket = getMatchBucket(match, signature);
            bucket.first.add(lTvs);
        }
        for (HighLevelTVS rTvs : rSet) {
            Object signature = strategy.sign(rTvs);
            Pair<Collection<TVS>, Collection<TVS>> bucket = getMatchBucket(match, signature);
            bucket.second.add(rTvs);
        }
        Engine.activeEngine.getAnalysisStatus().stopTimer(AnalysisStatus.MEET_TIME);
        
        return new SimpleIterator<HighLevelTVS>() {
            Iterator<Pair<Collection<TVS>, Collection<TVS>>> bucketIt = match.values().iterator();
            Pair<Collection<TVS>, Collection<TVS>> bucket = null;
            Iterator<HighLevelTVS> localResult = EmptyIterator.instance();
            Iterator<TVS> lIt = EmptyIterator.instance();
            Iterator<TVS> rIt = EmptyIterator.instance();
            TVS lTvs = null;
            TVS rTvs = null;
            @Override
            protected HighLevelTVS advance() {
                Engine.activeEngine.getAnalysisStatus().startTimer(AnalysisStatus.MEET_TIME);
                while (true) {
                    if (localResult.hasNext()) {
                        Engine.activeEngine.getAnalysisStatus().stopTimer(AnalysisStatus.MEET_TIME);
                        return localResult.next();
                    }
                    if (rIt.hasNext()) {
                        rTvs = rIt.next();
                        TVSSet meetResult = meet(lTvs, rTvs);
                        ++totalNumberOfTvsMeets;
                        if (!meetResult.isEmpty()) {
                            ++successfullTvsMeets;
                        }
                        localResult = meetResult.iterator();
                        continue;
                    }
                    if (lIt.hasNext()) {
                        lTvs = lIt.next();
                        rIt = bucket.second.iterator();
                        continue;
                    }
                    if (bucketIt.hasNext()) {
                        bucket = bucketIt.next();
                        lIt = bucket.first.iterator();
                        continue;
                    }
                    Engine.activeEngine.getAnalysisStatus().stopTimer(AnalysisStatus.MEET_TIME);
                    return null;
                }
            }
        };
    }

    private static Pair<Collection<TVS>, Collection<TVS>> getMatchBucket(
            Map<Object, Pair<Collection<TVS>, Collection<TVS>>> match, Object signature) {
        Pair<Collection<TVS>,Collection<TVS>> bucket = match.get(signature);
        if (bucket == null) {
            bucket = Pair.create((Collection<TVS>)new ArrayList<TVS>(), (Collection<TVS>)new ArrayList<TVS>());
            match.put(signature, bucket);
        }
        return bucket;
    }
	
	/** Checks whether <code>lTvs</code> is embedded in <code>rTvs</code>.
	 * 
	 * @param lTvs A general 3-valued structure.
	 * @param rTvs A general 3-valued structure.
	 * @return true iff <code>lTvs</code> is embedded in <code>rTvs</code>.
	 */
	public static boolean isEmbedded(final TVS lTvs, final TVS rTvs) {
		// Verify cardinality prerequisite.
		if (lTvs.nodes().size() < rTvs.nodes().size())
			return false;

		// Verify nullary predicate agreement.
		for (Predicate predicate : Vocabulary.allNullaryPredicates()) {
			if (!Kleene.less(lTvs.eval(predicate), rTvs.eval(predicate)))
				return false;
		}

		// Build canonical maps for both structure.
		Map<Node, Canonic> rTvsCanonic = new HashMap<Node, Canonic>(rTvs
				.nodes().size());
		genericBlur.makeCanonicMaps(rTvs, rTvsCanonic, dummy);
		dummy.clear();
		Map<Node, Canonic> lTvsCanonic = new HashMap<Node, Canonic>(lTvs
				.nodes().size());
		genericBlur.makeCanonicMaps(lTvs, lTvsCanonic, dummy);
		dummy.clear();

		// Build bipartite graph based on canonical name agreement,
		// including minimal/maximal pairing maps.
		// Note: we use "node wrappers" since node IDs are not
		// guaranteed to be unique across structures.
		MatchingGraph g = new MatchingGraph();
		Predicate sm = Vocabulary.sm;
		Map<Node, NodeWrapper> rNodesToWrappers = new HashMap<Node, NodeWrapper>(
				rTvs.nodes().size());
		boolean first_iter = true;

		for (Node lTvsNode : lTvs.nodes()) {
			Canonic lTvsNodeCanonic = lTvsCanonic.get(lTvsNode);
			NodeWrapper lTvsNodeWrapper = new NodeWrapper(0, lTvsNode);

			g.addVertex(lTvsNodeWrapper, 1, 1);

			for (Node rTvsNode : rTvs.nodes()) {
				Canonic rTvsNodeCanonic = rTvsCanonic.get(rTvsNode);
				NodeWrapper rTvsNodeWrapper;

				if (first_iter) {
					rTvsNodeWrapper = new NodeWrapper(1, rTvsNode);
					rNodesToWrappers.put(rTvsNode, rTvsNodeWrapper);

					g.addVertex(rTvsNodeWrapper, 1, 1);
				} else {
					rTvsNodeWrapper = (NodeWrapper) rNodesToWrappers
							.get(rTvsNode);
				}

				// Verify agreement and build edges.
				if (lTvsNodeCanonic.lessThanOrEqual(rTvsNodeCanonic)) {
					g.addEdge(lTvsNodeWrapper, rTvsNodeWrapper);

					// Update right-hand side vertex' max matching bound
					// following its degree.
					if (rTvs.eval(sm, rTvsNode) != Kleene.falseKleene)
						g.setWMax(rTvsNodeWrapper, g.degreeOf(rTvsNodeWrapper));
				}
			}

			first_iter = false;
		}

		// Generate all generalized perfect matchings of the graph.
		Set<Set<Edge>> matches = g.AbMatchEnum();

		// Iterate on each perfect matching and verify k-arity predicate agreement.
		for (Set<Edge> edges : matches) {
			// Map left nodes to corresponding matched right nodes.
			Map<Node, Node> leftToRightNodes = new HashMap<Node, Node>(edges
					.size());
			for (Edge edge : edges) {
				NodeWrapper lTvsNodeWrapper = (NodeWrapper) edge.getSource();
				NodeWrapper rTvsNodeWrapper = (NodeWrapper) edge.getTarget();
				leftToRightNodes.put(lTvsNodeWrapper.getNode(), rTvsNodeWrapper
						.getNode());
			}

			// Iterate on all positive arity predicates, verifying embedding.
			boolean isMatch = true;
			for (Predicate predicate : Vocabulary.allPositiveArityPredicates()) {
				for (Iterator<? extends NodeTuple> lNodeTupleIt = NodeTupleIterator
						.createIterator(lTvs.nodes(), predicate.arity()); lNodeTupleIt
						.hasNext();) {
					// Create right node tuple of predicate arity.
					NodeTuple lNodeTuple = (NodeTuple) lNodeTupleIt.next();
					Node[] rNodeTupleTmp = new Node[predicate.arity()];

					for (int i = 0; i < predicate.arity(); i++) {
						rNodeTupleTmp[i] = (Node) leftToRightNodes
								.get(lNodeTuple.get(i));
					}
					NodeTuple rNodeTuple = NodeTuple.createTuple(rNodeTupleTmp);

					// Verify Kleene values agreement for tuples.
					if (!Kleene.less(lTvs.eval(predicate, lNodeTuple), rTvs
							.eval(predicate, rNodeTuple))) {
						isMatch = false;
						break;
					}
				}

				if (!isMatch)
					break;
			}

			if (isMatch)
				return true;
		}

		return false;
	}
	
	/** Checks whether <code>lTvs</code> and <code>rTvs</code> are isomorphic.
	 * 
	 * @param lTvs A general 3-valued structure.
	 * @param rTvs A general 3-valued structure.
	 * @return true iff <code>lTvs</code> and <code>rTvs</code> are isomorphic.
	 */
	public static boolean isomorphic(final TVS lTvs, final TVS rTvs) {
		// A naive non-efficient implementation which simply applies
		// the embedding test twice.
		return isEmbedded(lTvs, rTvs) && isEmbedded(rTvs, lTvs);
	}

	private static class NodeWrapper {
		private Node m_node;
		private int m_wrapper;

		public NodeWrapper(int wrapper, Node node) {
			m_node = node;
			m_wrapper = wrapper;
		}

		public Node getNode() {
			return m_node;
		}

		public boolean equals(NodeWrapper other) {
			if (m_wrapper == other.m_wrapper && m_node.equals(other.m_node))
				return true;

			return false;
		}

		public String toString() {
			String str = "(" + m_node.toString() + ")" + m_wrapper;
			return str;
		}
	}
}
