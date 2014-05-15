package tvla.core.generic;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.common.NodeTupleIterator;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashMapFactory;

/** An algorithm to add a node from one structure to another.
 * Based on DuplicateNode.
 * @author Noam Rinetzky.
 * @since 16/10/2004 Initial creation.
 */
public final class AddUniverse{
	/** The one and only instance of this class.
	 */
	private static final AddUniverse instance = new AddUniverse();

	/** Returns the one and only instance of this class.
	 */
	public static AddUniverse getInstance() {
		return instance;
	}

	/** Copies the nodes
	 * @author Noam Rinetzy
	 */
	public void addUniverse(TVS to, TVS from) {
		assert(to != null);
		assert(from != null);
		assert(to != from);
		
		if (from.numOfNodes() == 0)
			return;
		
		Map fromToTo = HashMapFactory.make((5 * from.numOfNodes()) / 4 + 1); //0.75f
		Collection fromNodes = from.nodes();
		
		// Adds new nodes to the new strucutre and constructs a mapping from
		// old nodes to new nodes.
		for  (Iterator nodeItr = fromNodes.iterator(); nodeItr.hasNext();) {
			Node newNode = to.newNode();
			fromToTo.put(nodeItr.next(), newNode);
		}
		
		for (Predicate predicate : to.getVocabulary().all()) {
			if (predicate.arity() == 0)
				continue;
			else if (predicate.arity() == 1) {
				Iterator nodeIter = fromNodes.iterator();
				while(nodeIter.hasNext()) {
					Node candidate = (Node) nodeIter.next();
					Kleene val = from.eval(predicate, candidate);
					Node mirror = (Node) fromToTo.get(candidate);
					to.update(predicate,NodeTuple.createSingle(mirror),val);
				}				
			}
			else if (predicate.arity() == 2) {
				Iterator tupleIter = NodeTupleIterator.createIterator(fromNodes, 2);
				while (tupleIter.hasNext()) {
					NodeTuple tuple = (NodeTuple) tupleIter.next();
					Kleene val = from.eval(predicate, tuple);
					Node src = tuple.get(0);
					assert(src != null);
					Node dst = tuple.get(1);
					assert(dst != null);
					Node srcMirror = (Node) fromToTo.get(src);
					assert(srcMirror != null);
					Node dstMirror = (Node) fromToTo.get(dst);
					assert(dstMirror != null);
					
					NodeTuple tupleMirror = NodeTuple.createPair(srcMirror, dstMirror);
					to.update(predicate, tupleMirror, val);
				}
			}
			else {
				throw new InternalError("Strucutres has a predicate with an unsupported arity " + predicate.arity());
			}
		}
	}
}
