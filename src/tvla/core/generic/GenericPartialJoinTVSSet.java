package tvla.core.generic;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import tvla.analysis.AnalysisStatus;
import tvla.core.Canonic;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.base.PredicateEvaluator;
import tvla.core.base.PredicateUpdater;
import tvla.core.common.NodeTupleIterator;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.Pair;

/** A generic implementation of a TVSSet that merges structures with the same
 * set of canonic names.
 * @since tvla-2-alpha.
 * @author Roman Manevich.
 */
public class GenericPartialJoinTVSSet extends GenericSingleTVSSet {
	public GenericPartialJoinTVSSet() {
		super();
	}
	
	public GenericPartialJoinTVSSet(Collection<HighLevelTVS> s) {
		structures = tvla.util.HashSetFactory.make(s);
	}

	/** Applies the Join confluence operator.
	 * @return The difference between the updated set
	 * and the old set or null if there is no difference.
	 */
	public HighLevelTVS mergeWith(HighLevelTVS newStructure) {
	    assert shareCount == 0;
	    
		AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.BLUR_TIME);
		newStructure.blur();
		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.BLUR_TIME);

		cleanup();
		
		for (Iterator<HighLevelTVS> iterator = structures.iterator(); iterator.hasNext(); ) {
			HighLevelTVS singleStructure = iterator.next();
			
			if (!mergeCondition(singleStructure, newStructure))
				continue;
			
			iterator.remove();
			boolean change = mergeStructures(singleStructure, newStructure);
			
			structures.add(singleStructure);
			HighLevelTVS result = (HighLevelTVS) (change ? singleStructure : null);
			return result;
		}
		structures.add(newStructure);
		return (HighLevelTVS) newStructure;
	}
	
	public boolean mergeWith(HighLevelTVS newStructure, Collection<Pair<HighLevelTVS, HighLevelTVS>> mergedWith) {
        assert shareCount == 0;

    AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.BLUR_TIME);
		newStructure.blur();
		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.BLUR_TIME);

		cleanup();
		
		for (Iterator<HighLevelTVS> iterator = structures.iterator(); iterator.hasNext(); ) {
			HighLevelTVS singleStructure = iterator.next();
			
			if (!mergeCondition(singleStructure, newStructure))
				continue;
			
			iterator.remove();
			boolean change = mergeStructures(singleStructure, newStructure);
			
			structures.add(singleStructure);
			mergedWith.add(new Pair<HighLevelTVS,HighLevelTVS>(newStructure, singleStructure));
			return change;
		}
		structures.add(newStructure);
		return true;
	}

	protected boolean mergeStructures(TVS singleStructure, TVS newStructure) {
	    assert singleStructure.getVocabulary() == newStructure.getVocabulary();
		boolean change = false;
		
		Collection<Predicate> nullaryNonRel = singleStructure.getVocabulary().nullaryNonRel();
		Collection<Predicate> unaryNonRel = singleStructure.getVocabulary().unaryNonRel();
		Collection<Predicate> binary = singleStructure.getVocabulary().binary();

		if (!nullaryNonRel.isEmpty()) {
			NodeTuple tuple = NodeTuple.EMPTY_TUPLE;
			for (Predicate predicate : nullaryNonRel) {
				Kleene singleVal = singleStructure.eval(predicate, tuple);
				if (singleVal == Kleene.unknownKleene)
					continue;
				Kleene newVal = newStructure.eval(predicate, tuple);
				if (singleVal != newVal) {
					change = true;
					singleStructure.update(predicate, tuple, Kleene.unknownKleene);
				}
			}
		}
		
    for (Predicate predicate : unaryNonRel) {
        change = mergeStructures(singleStructure, newStructure, predicate) || change;
    }
    for (Predicate predicate : binary) {
        change = mergeStructures(singleStructure, newStructure, predicate) || change;
    }
        
    recomputeStructureGroup(singleStructure, newStructure);

    return change;
	}

    protected boolean mergeStructures(TVS singleStructure, TVS newStructure, Predicate predicate) {
        boolean change = false;
        PredicateEvaluator newEval = PredicateEvaluator.evaluator(predicate, newStructure);
        PredicateUpdater singleUpdater = PredicateUpdater.updater(predicate, singleStructure);
        
        Iterator<Entry<NodeTuple, Kleene>> singleIt = singleStructure.iterator(predicate);
        while (singleIt.hasNext()) {
            Entry<NodeTuple, Kleene> entry = singleIt.next();
            if (entry.getValue() != Kleene.unknownKleene) {
            	// Must be trueKleene
                NodeTuple tuple = entry.getKey();
                NodeTuple newTuple = mapNodeTuple(predicate, tuple, singleCanonicName, newInvCanonicName);
                if (newEval.eval(newTuple) != Kleene.trueKleene) {
                    change = true;
                    singleUpdater.update(tuple, Kleene.unknownKleene);
                }
            }
        }
        
        boolean wasAllFalse = singleStructure.numberSatisfy(predicate) == 0;
        if (wasAllFalse) {
        	// Can't use PredicateEvaluator...
	        Iterator<Entry<NodeTuple, Kleene>> newIt = newStructure.iterator(predicate);        
            change = newIt.hasNext();
	        while (newIt.hasNext()) {
	            Entry<NodeTuple, Kleene> entry = newIt.next();
	            NodeTuple newTuple = entry.getKey();
	            NodeTuple tuple = mapNodeTuple(predicate, newTuple, newCanonicName, singleInvCanonicName);
                singleUpdater.update(tuple, Kleene.unknownKleene);
	        }
        } else {
	        PredicateEvaluator singleEval = PredicateEvaluator.evaluator(predicate, singleStructure);
	        Iterator<Entry<NodeTuple, Kleene>> newIt = newStructure.iterator(predicate);        
	        while (newIt.hasNext()) {
	            Entry<NodeTuple, Kleene> entry = newIt.next();
	            NodeTuple newTuple = entry.getKey();
	            NodeTuple tuple = mapNodeTuple(predicate, newTuple, newCanonicName, singleInvCanonicName);
	            Kleene value = singleEval.eval(tuple);
	            if (value != Kleene.unknownKleene) {
	                if (entry.getValue() != value) {
	                    change = true;
	                    singleUpdater.update(tuple, Kleene.unknownKleene);
	                }
	            }
	        }
        }
        return change;
    }
	
    protected boolean mergeStructures_old(TVS singleStructure, TVS newStructure) {
        assert singleStructure.getVocabulary() == newStructure.getVocabulary();
        boolean change = false;
        
        // Assume nodes collection doesn't change during iteration.
        Collection<Node> nodes = singleStructure.nodes();
        
        Collection<Predicate> nullaryNonRel = singleStructure.getVocabulary().nullaryNonRel();
        Collection<Predicate> unaryNonRel = singleStructure.getVocabulary().unaryNonRel();
        Collection<Predicate> binary = singleStructure.getVocabulary().binary();

        if (!nullaryNonRel.isEmpty()) {
            NodeTuple tuple = NodeTuple.EMPTY_TUPLE;
            for (Predicate predicate : nullaryNonRel) {
                Kleene singleVal = singleStructure.eval(predicate, tuple);
                if (singleVal == Kleene.unknownKleene)
                    continue;
                Kleene newVal = newStructure.eval(predicate, tuple);
                if (singleVal != newVal) {
                    change = true;
                    singleStructure.update(predicate, tuple, Kleene.unknownKleene);
                }
            }
        }
        
        if (!unaryNonRel.isEmpty()) {
            Iterator<Node> tupleIter = nodes.iterator();
            while (tupleIter.hasNext()) {
                Node tuple = tupleIter.next();

                // map the tuple to the corresponding tuple in the universe of newStructure
                Canonic singleCanonic = singleCanonicName.get(tuple);
                Node newTuple = (Node) newInvCanonicName.get(singleCanonic);

                for (Predicate predicate : unaryNonRel) {
                    Kleene singleVal = singleStructure.eval(predicate, tuple);
                    if (singleVal == Kleene.unknownKleene)
                        continue;

                    Kleene newVal = newStructure.eval(predicate, newTuple);
                    if (singleVal != newVal) {
                        change = true;
                        singleStructure.update(predicate, tuple, Kleene.unknownKleene);
                    }
                }
            }
        }
        
/*      
        if (!unaryNonRel.isEmpty()) {
            for (Iterator predIter = unaryNonRel.iterator();
             predIter.hasNext(); ) {
                Predicate predicate = (Predicate) predIter.next();
                Iterator tupleIter = nodes.iterator();
                while (tupleIter.hasNext()) {
                    Node tuple = (Node) tupleIter.next();
                    Kleene singleVal = singleStructure.eval(predicate, tuple);
                    if (singleVal == Kleene.unknownKleene)
                        continue;

                    // map the tuple to the corresponding tuple in the universe of newStructure
                    Canonic singleCanonic = (Canonic) singleCanonicName.get(tuple);
                    Node newTuple = (Node) newInvCanonicName.get(singleCanonic);

                    Kleene newVal = newStructure.eval(predicate, newTuple);
                    if (singleVal != newVal) {
                        change = true;
                        singleStructure.update(predicate, tuple, Kleene.unknownKleene);
                    }
                }
            }
        }
*/
        
        if (!binary.isEmpty()) {
            Node [] newNodesTmp = new Node[2];
            Iterator<? extends NodeTuple> tupleIter = NodeTupleIterator.createIterator(nodes, 2);
            while (tupleIter.hasNext()) {
                NodeTuple tuple = (NodeTuple) tupleIter.next();
                
                for (int index = 0; index < tuple.size(); ++index) {
                    Canonic singleCanonic = singleCanonicName.get(tuple.get(index));
                    newNodesTmp[index] = (Node) newInvCanonicName.get(singleCanonic);
                }
                NodeTuple newTuple = NodeTuple.createTuple(newNodesTmp);
                
                for (Predicate predicate : binary) {
                    Kleene singleVal = singleStructure.eval(predicate, tuple);
                    if (singleVal == Kleene.unknownKleene)
                        continue;

                    Kleene newVal = newStructure.eval(predicate, newTuple);
                    if (singleVal != newVal) {
                        change = true;
                        singleStructure.update(predicate, tuple, Kleene.unknownKleene);
                    }
                }
            }
        }
/*
        if (!binary.isEmpty()) {
            Node [] newNodesTmp = new Node[2];
            for (Iterator predIter = binary.iterator();
             predIter.hasNext(); ) {
                Predicate predicate = (Predicate) predIter.next();
                Iterator tupleIter = NodeTupleIterator.createIterator(nodes, 2);
                while (tupleIter.hasNext()) {
                    NodeTuple tuple = (NodeTuple) tupleIter.next();
                    Kleene singleVal = singleStructure.eval(predicate, tuple);
                    if (singleVal == Kleene.unknownKleene)
                        continue;
                    
                    for (int index = 0; index < tuple.size(); ++index) {
                        Canonic singleCanonic = (Canonic) singleCanonicName.get(tuple.get(index));
                        newNodesTmp[index] = (Node) newInvCanonicName.get(singleCanonic);
                    }
                    NodeTuple newTuple = NodeTuple.createTuple(newNodesTmp);
                    Kleene newVal = newStructure.eval(predicate, newTuple);
                    if (singleVal != newVal) {
                        change = true;
                        singleStructure.update(predicate, tuple, Kleene.unknownKleene);
                    }
                }
            }
        }
*/
        return change;
    }

	/** Merges newStructure into singleStructure.
	 */
	private boolean mergeStructures2(TVS singleStructure, TVS newStructure) {
	    assert singleStructure.getVocabulary() == newStructure.getVocabulary();
		boolean change = false;
		
		// Assume nodes collection doesn't change during iteration.
		Collection<Node> nodes = singleStructure.nodes();
		
		// should actually iterate only over the non-relational predicates
        for (Predicate predicate : singleStructure.getVocabulary().all()) {
			// ADDED
			if (predicate.arity() < 2 && predicate.abstraction())
				continue;
			
			Iterator<? extends NodeTuple> tupleIter = NodeTupleIterator.createIterator(nodes, predicate.arity());
			while (tupleIter.hasNext()) {
				NodeTuple tuple = tupleIter.next();
				Kleene singleVal = singleStructure.eval(predicate, tuple);
				if (singleVal == Kleene.unknownKleene)
					continue;
				
				// map the tuple to the corresponding tuple in the universe of newStructure
				Node [] newNodesTmp = new Node[tuple.size()];
				for (int index = 0; index < tuple.size(); ++index) {
					Canonic singleCanonic = singleCanonicName.get(tuple.get(index));
					newNodesTmp[index] = (Node) newInvCanonicName.get(singleCanonic);
				}
				NodeTuple newTuple = NodeTuple.createTuple(newNodesTmp);
				
				Kleene newVal = newStructure.eval(predicate, newTuple);
				if (singleVal != newVal) {
					change = true;
					singleStructure.update(predicate, tuple, Kleene.unknownKleene);
				}
			}
		}
		return change;
	}
	
	protected void makeCanonicMaps(TVS singleStructure, TVS newStructure) {
		genericBlur.makeCanonicMapsForBlurred(singleStructure, 
											  singleCanonicName,
											  singleInvCanonicName);
		genericBlur.makeCanonicMapsForBlurred(newStructure,
											  newCanonicName,
											  newInvCanonicName);
	}

	/** A test to check whether the two structures are compatible for merging.
	 */
	protected boolean mergeCondition(TVS old, TVS candidate) {
		if (old.nodes().size() != candidate.nodes().size())
			return false;
		
		if (!super.mergeCondition(old, candidate))
			return false;

		makeCanonicMaps(old, candidate);
		
		// Make sure that each node has a matching node,
		// and that all unary abstraction predicates match on them.
		for (Node oldNode : old.nodes()) {
			Canonic canonic = singleCanonicName.get(oldNode);
			Node candidateNode = (Node) newInvCanonicName.get(canonic);
			if (candidateNode == null)
				return false;
			Canonic newCanonic = newCanonicName.get(candidateNode);
			if (!canonic.equals(newCanonic))
				return false;
		}
		return true;
	}

	static Node[][] tempTuples = new Node[][] 
	    { {}, {null}, {null,null}, {null, null, null}, {null,null,null,null}};
	
    protected static NodeTuple mapNodeTuple(Predicate predicate, NodeTuple thisTuple, Map<Node, Canonic> thisMap, Map<Canonic, Node> otherInvMap) {
        NodeTuple otherTuple;
        if (predicate.arity() == 1) {
            otherTuple = otherInvMap.get(thisMap.get((Node) thisTuple));
        } else {
            Node[] otherTupleNodes = tempTuples[predicate.arity()];
            for (int i = 0; i < otherTupleNodes.length; i++) {
                otherTupleNodes[i] = otherInvMap.get(thisMap.get(thisTuple.get(i)));
            }
            otherTuple = NodeTuple.createTuple(otherTupleNodes);
        }
        return otherTuple;
    }
}