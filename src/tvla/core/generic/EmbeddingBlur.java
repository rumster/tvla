package tvla.core.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import tvla.core.Blur;
import tvla.core.Node;
import tvla.core.TVS;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.HashSetFactory;

/** An implementation of the Blur algorithm that merges nodes
 * such that one of them embeds the other, as suggested by
 * Ramalingam.
 * 
 * @author Roman Manevich.
 * @since tvla-2-alpha October 1 2002.
 */
public class EmbeddingBlur extends Blur {
	/** A convenience instance.
	 */
	protected static Blur defaultEmbeddingBlur = new EmbeddingBlur();
	
	protected TVS structure;

	public static void reset() {
		defaultEmbeddingBlur = new EmbeddingBlur();
	}
	
	/** Blurs the specified structure in-place.
	 */
	public void blur(TVS structure) {
		this.structure = structure;
		
		Collection<Node> workSet = HashSetFactory.make(structure.nodes());
		while (!workSet.isEmpty()) {
			Iterator<Node> nodeIter = workSet.iterator();
			Node node = nodeIter.next();
			nodeIter.remove();
			while (nodeIter.hasNext()) {
				Node candidate = nodeIter.next();
				if (compatibleNodes(node, candidate)) {
					nodeIter.remove();
					Collection<Node> toMerge = new ArrayList<Node>(2);
					toMerge.add(node);
					toMerge.add(candidate);
					node = structure.mergeNodes(toMerge);
				}
			}
		}
		
		this.structure = null; // Release unneeded resources (opportunity for compile-time GC).
	}
	
	protected boolean compatibleNodes(Node n1, Node n2) {
		for (Predicate predicate : structure.getVocabulary().unaryRel()) {
			Kleene val1 = structure.eval(predicate, n1);
			Kleene val2 = structure.eval(predicate, n2);
			if (val1 == val2 ||
				val1 == Kleene.unknownKleene ||
				val2 == Kleene.unknownKleene) {
				// Compatibility holds for this predicate.
			}
			else {
				return false;
			}
		}
		return true;
	}
}
