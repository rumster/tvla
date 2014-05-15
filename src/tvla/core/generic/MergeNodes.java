package tvla.core.generic;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.StructureGroup;
import tvla.core.TVS;
import tvla.core.common.NodeTupleIterator;
import tvla.exceptions.SemanticErrorException;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.Filter;
import tvla.util.HashMapFactory;

/** A simple algorithm for merging a collection of structure
 * nodes in-place. The implementation uses only TVS interface
 * methods.
 * @author Roman Manevich
 * @since 6/9/2001 Initial creation.
 */
public final class MergeNodes {
	/** The one and only instance of this class.
	 */
	private static final MergeNodes instance = new MergeNodes();

	/** Returns the one and only instance of this class.
	 */
	public static MergeNodes getInstance() {
		return instance;
	}

	/** Merges the specified collection of nodes by joining their predicate values.
	 * This implementation merges all nodes into the first node in the specified
	 * collection and returns it as the answer.
	 * @author Roman Manevich
	 */
	public Node mergeNodes(TVS structure, Collection<Node> toMerge) {
		// merge the nodes one after another to the first node in the collection
		Node answerNode = (Node) toMerge.iterator().next();
		
		if (toMerge.size() == 1) // no need to merge
			return answerNode;

		updateStructureGroup(structure, toMerge, answerNode);
		
		if (toMerge.size() == structure.nodes().size()) {
		    mergeAll(structure, answerNode);
		} else {
		    mergeNodesImpl(structure, toMerge, answerNode);
		}
		
		// This predicate needs to be updated after all other unary predicates 
		// are updated to make sure that summary is set to 1/2.
		structure.update(Vocabulary.sm, answerNode, Kleene.unknownKleene);
		return answerNode;
	}

    protected void mergeAll(final TVS structure, final Node answerNode) {
        int numNodes = structure.numOfNodes();
        for (Predicate predicate : structure.getVocabulary().positiveArity()) {
            int allSatisfy = pow(numNodes, predicate.arity());
            int numberSatisfy = structure.numberSatisfy(predicate);
            if (numberSatisfy == 0) {
                continue; // false, no need to do anything as answer node tuple is one of them
            }
            if (numberSatisfy == allSatisfy) {
                // Maybe it's all true
                boolean hasUnknown = false;
                Iterator<Entry<NodeTuple, Kleene>> iterator = structure.iterator(predicate);
                while (iterator.hasNext()) {
                    if (iterator.next().getValue() == Kleene.unknownKleene) {
                        hasUnknown = true;
                        break;
                    }
                }
                if (!hasUnknown) {
                    continue; // true, no need to do anything as answer node tuple is one of them
                }
            }
            
            structure.update(predicate, createSelfLoop(answerNode, predicate.arity()), Kleene.unknownKleene);
        }       
        structure.filterNodes(new Filter<Node>() {
            public boolean accepts(Node node) {
                return node == answerNode;
            }
        });
        structure.modify(Vocabulary.active);
    }

    protected NodeTuple createSelfLoop(Node node, int arity) {
        if (arity == 1) {
            return node;
        } else if (arity == 2) {
            return NodeTuple.createPair(node, node);
        } else {        
            Node[] answerNodeArray = new Node[arity];
            for (int i = 0; i < answerNodeArray.length; i++) {
                answerNodeArray[i] = node;
            }
            return NodeTuple.createTuple(answerNodeArray);
        }
    }

    private int pow(int base, int pow) {
        int result = base;
        while (--pow > 0) {
            result *= base;
        }
        return result;
    }

    protected void updateStructureGroup(TVS structure, Collection<Node> toMerge, Node answerNode) {
        if (structure.getStructureGroup() != null) {
		    StructureGroup group = structure.getStructureGroup();
		    StructureGroup newGroup = new StructureGroup((HighLevelTVS) structure);
		    for (StructureGroup.Member member : group.getMembers()) {
		        Map<Node, Node> mapping = member.getMapping();
		        Node target = getMergeTarget(structure, toMerge, member, mapping);
		        Map<Node, Node> newMapping = HashMapFactory.make(mapping);
		        for (Node node : toMerge) {
		            newMapping.remove(node);
		        }
                newMapping.put(answerNode, target);
                newGroup.addMember(member.getStructure(), newMapping, member.getComponent());
		    }
		    structure.setStructureGroup(newGroup);
		}
    }

    private Node getMergeTarget(TVS structure, Collection<Node> toMerge, StructureGroup.Member member,
            Map<Node, Node> mapping) {
        Iterator<Node> toMergeIt = toMerge.iterator();
        Node target = mapping.get(toMergeIt.next());
        while (toMergeIt.hasNext()) {
            if (mapping.get(toMergeIt.next()) != target) {
                throw new SemanticErrorException("Trying to merge nodes with different target in structure group\n " + structure + "\n member: " + member);
            }
        }
        return target;
    }

	
	protected void mergeNodesImpl(TVS structure, Collection<Node> toMerge,
			Node answerNode) {
		Iterator<Node> nodeIter = toMerge.iterator();
		Node answerNode2 = nodeIter.next(); // Advance over the answer node
		assert answerNode2 == answerNode; // Make sure we are consistent
		while (nodeIter.hasNext()) {
			Node currentNode = (Node) nodeIter.next();
			for (Predicate predicate : structure.getVocabulary().positiveArity()) {
				if (predicate.arity() == 1) { // efficient treatment of unary predicates
					Kleene val1 = structure.eval(predicate, answerNode);
					Kleene val2 = structure.eval(predicate, currentNode);
					if (val1 != val2)
						structure.update(predicate, answerNode, Kleene.join(val1, val2));
				}
				else { // general treatment
					Iterator<? extends NodeTuple> tupleIterator = NodeTupleIterator.createIterator(structure.nodes(), predicate.arity());
					while (tupleIterator.hasNext()) {
						NodeTuple tuple = (NodeTuple) tupleIterator.next();
						if (!tuple.contains(answerNode))
							continue;
						
						Kleene val1 = structure.eval(predicate, tuple);
						Kleene val2 = structure.eval(predicate, tuple.substitute(currentNode, answerNode));
						Kleene val3 = structure.eval(predicate, tuple.substitute(answerNode, currentNode));
						Kleene val = Kleene.join(Kleene.join(val1, val2), val3);
						structure.update(predicate, tuple.substitute(currentNode, answerNode), val);
					}
				}
			}
			
			structure.removeNode(currentNode);
		}
	}
	
	/** Singleton pattern.
	 */
	private MergeNodes() {
	}
}
