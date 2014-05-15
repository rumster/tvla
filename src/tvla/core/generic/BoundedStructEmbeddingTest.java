package tvla.core.generic;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;

/**
 * Test whether one bounded structure is embedded in another bounded structure.
 * 
 * @author romanm
 * 
 */
public class BoundedStructEmbeddingTest {
	/**
	 * 
	 * @param lhs
	 *            A bounded structure.
	 * @param rhs
	 *            A bounded structure.
	 * @return true if 'lhs' is embedded in 'rhs'.
	 */
	public static boolean isEmbedded(HighLevelTVS lhs, HighLevelTVS rhs) {
		assert lhs.getVocabulary() == rhs.getVocabulary();

		// Arrange it so lhs is the structure with the smaller universe.
		// The algorithm will attempt to find an embedding function of
		// rhs into lhs.
		if (lhs.nodes().size() > rhs.nodes().size()) {
			// Swap lhs and rhs.
			HighLevelTVS tmp = lhs;
			lhs = rhs;
			rhs = tmp;
		}

		// Check embedding with respect to nullary predicates.
		for (Predicate predicate : lhs.getVocabulary().nullaryRel()) {
			Kleene lhsVal = lhs.eval(predicate);
			Kleene rhsVal = rhs.eval(predicate);
			if (lhsVal == Kleene.unknownKleene
					|| rhsVal == Kleene.unknownKleene || lhsVal == rhsVal) {
				// Embedding condition is met for this binding.
			} else {
				// Embedding condition is violated by this binding.
				return false;
			}
		}

		// A map from the nodes of rhs onto the nodes of lhs.
		Map<Node, Node> embeddingFunction = HashMapFactory.make(rhs.nodes()
				.size());
		// lhsNodes is used to check that the embedding function is surjective.
		Collection<Node> unmatchedLhsNodes = HashSetFactory.make(lhs.nodes());

		// Attempt to match a node in lhs for every node of rhs,
		// since the function has to be total.
		for (Iterator<Node> rhsNodeIter = rhs.nodes().iterator(); rhsNodeIter
				.hasNext();) {
			Node rhsNode = rhsNodeIter.next();
			Node matchedNode = null;
			// Attempt to match any node of lhs.
			for (Iterator<Node> lhsNodeIter = lhs.nodes().iterator(); lhsNodeIter
					.hasNext();) {
				Node lhsNode = lhsNodeIter.next();

				// Check that the embedding condition is met for the
				// pair of nodes rhsNode and lhsNode, with respect to
				// the unary abstraction predicates.
				for (Predicate predicate : lhs.getVocabulary().unaryRel()) {
					Kleene lhsVal = lhs.eval(predicate, lhsNode);
					Kleene rhsVal = rhs.eval(predicate, rhsNode);
					if (lhsVal == Kleene.unknownKleene
							|| rhsVal == Kleene.unknownKleene
							|| lhsVal == rhsVal) {
						// The embedding condition is met for this binding,
						// and lhsNode is still a potential match.
					} else {
						// The embedding condition is violated by this binding,
						// and another matching node has to be found.
						lhsNode = null;
						break;
					}
				}
				// A match was found.
				if (lhsNode != null) {
					matchedNode = lhsNode;
					// Prefer unmatched nodes of lhs, because the embedding
					// function has
					// to be surjective.
					if (unmatchedLhsNodes.contains(lhsNode))
						break;
					// Otherwise keep looking for a matching node from the
					// unmatched node list.
				}
			}
			if (matchedNode != null) { // found a match for rhsNode
				embeddingFunction.put(rhsNode, matchedNode);
				unmatchedLhsNodes.remove(matchedNode);
			} else {
				return false;
			}
		}
		// The embedding function is surjective and total.
		if (unmatchedLhsNodes.isEmpty())
			return true;
		return false;
	}
}