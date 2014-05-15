package tvla.core.generic;

import java.util.Collection;
import java.util.Iterator;

import tvla.core.Canonic;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.common.NodeTupleIterator;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;

/** A generic implementation of a TVSSet that merges structures with the same
 * set of canonic names such that one of them embeds the other.
 * @since tvla-2-alpha, August 15, 2002.
 * @author Roman Manevich.
 */
public class GenericCanonicEmbeddingTVSSet extends GenericPartialJoinTVSSet {
	public GenericCanonicEmbeddingTVSSet() {
		super();
	}

	protected boolean mergeCondition(TVS first, TVS second) {
	    assert first.getVocabulary() == second.getVocabulary();
		if (!super.mergeCondition(first, second))
			return false;
		
		boolean firstEmbeddedInSecond = true;
		boolean secondEmbeddedInFirst = true;
		
        for (Predicate predicate : first.getVocabulary().all()) {
			Iterator tupleIter = NodeTupleIterator.createIterator(first.nodes(), predicate.arity());
			while (tupleIter.hasNext()) {
				NodeTuple firstTuple = (NodeTuple) tupleIter.next();
				Kleene firstVal = first.eval(predicate, firstTuple);
				
				// map the tuple to the corresponding tuple in the universe of newStructure
				Node [] secondNodesTmp = new Node[firstTuple.size()];
				for (int index = 0; index < firstTuple.size(); ++index) {
					Canonic singleCanonic = (Canonic) singleCanonicName.get(firstTuple.get(index));
					secondNodesTmp[index] = (Node) newInvCanonicName.get(singleCanonic);
				}
				NodeTuple secondTuple = NodeTuple.createTuple(secondNodesTmp);
				Kleene secondVal = second.eval(predicate, secondTuple);
				
				if (firstVal == secondVal || secondVal == Kleene.unknownKleene) {
					// do nothing
				}
				else {
					firstEmbeddedInSecond = false;
				}

				if (secondVal == firstVal || firstVal == Kleene.unknownKleene) {
					// do nothing
				}
				else {
					secondEmbeddedInFirst = false;
				}
			}
			if (!firstEmbeddedInSecond && !secondEmbeddedInFirst)
				break;
		}
		
		return (firstEmbeddedInSecond || secondEmbeddedInFirst);
	}
	
	public boolean mergeWith(HighLevelTVS S, Collection mergedWith) {
		throw new UnsupportedOperationException() ;
	}

}