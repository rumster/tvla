package tvla.core.common;

import java.util.Iterator;
import java.util.Map;
import java.util.Collection;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashMapFactory;

/** A generic algorithm for building an isomorphic structure from
 * another structure.
 * @author Roman Manevich.
 */
public class TVSAssign {
	/** Destructively updates destination to be isomorphic to source. 
	 * precondition: destination.nodes().size() == 0
	 * postcondition: destination.nodes().size() == source.nodes().size()
	 */
	public static void assign(TVS destination, TVS source) {
	    assert destination.getVocabulary() == source.getVocabulary();
	    
		assert(destination.nodes().size() == 0);
		Collection<Node> source_nodes = source.nodes();
		Map<Node, Node> sourceNodeToDestinationNode = HashMapFactory.make(source_nodes.size());
		for (Iterator<Node> nodeIter = source_nodes.iterator(); nodeIter.hasNext(); ) {
			Node sourceNode = nodeIter.next();
			Node destinationNode = destination.newNode();
			sourceNodeToDestinationNode.put(sourceNode, destinationNode);
		}
		
		for (Predicate predicate : destination.getVocabulary().nullary()) {
			destination.update(predicate, source.eval(predicate));
		}
		
        for (Predicate predicate : destination.getVocabulary().unary()) {
			for (Iterator<Node> nodeIter = source_nodes.iterator(); nodeIter.hasNext(); ) {
				Node sourceNode = nodeIter.next();
				Node destinationNode = sourceNodeToDestinationNode.get(sourceNode);
				Kleene sourceVal = source.eval(predicate, sourceNode);
				if (sourceVal == Kleene.falseKleene) // an optimization for sparse predicates
					continue;				
				destination.update(predicate, destinationNode, sourceVal);
			}
		}

		for (Predicate predicate : destination.getVocabulary().positiveArity()) {
			Iterator<? extends NodeTuple> tupleIter = NodeTupleIterator.createIterator(source_nodes, predicate.arity());
			Node [] destNodesTmp = new Node[predicate.arity()];
			while (tupleIter.hasNext()) {
				NodeTuple sourceTuple = tupleIter.next();
				Kleene sourceVal = source.eval(predicate, sourceTuple);
				
				if (sourceVal == Kleene.falseKleene) // an optimization for sparse predicates
					continue;				
				for (int i = 0; i < destNodesTmp.length; ++i)
					destNodesTmp[i] = sourceNodeToDestinationNode.get(sourceTuple.get(i));
				
				NodeTuple destTuple = NodeTuple.createTuple(destNodesTmp);
				destination.update(predicate, destTuple, sourceVal);				
			}
		}
		
		assert(destination.nodes().size() == source_nodes.size());
	}
}
