package tvla.core.generic;

import java.util.Iterator;
import java.util.Map;

import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.StructureGroup;
import tvla.core.TVS;
import tvla.core.common.NodeTupleIterator;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;

/** A node duplication algorithm.
 * @author Roman Manevich.
 * @since 6/9/2001 Initial creation.
 */
public final class DuplicateNode {
	/** The one and only instance of this class.
	 */
	private static final DuplicateNode instance = new DuplicateNode();

	/** Returns the one and only instance of this class.
	 */
	public static DuplicateNode getInstance() {
		return instance;
	}

	/** Bifurcates the specified node.
	 * @author Roman Manevich
	 */
	public Node duplicateNode(TVS structure, Node node) {
		Node newNode = structure.newNode();
		
		if (structure.getStructureGroup() != null) {
			Map<Node,Node> newMapping = StructureGroup.Member.buildIdentityMapping((HighLevelTVS) structure);
			newMapping.put(newNode, node);
			StructureGroup newGroup = new StructureGroup((HighLevelTVS) structure);
			newGroup.addMember(structure.getStructureGroup(), newMapping, null);
			structure.setStructureGroup(newGroup);
		}
		
        for (Predicate predicate : structure.getVocabulary().all()) {
			if (predicate.arity() == 0)
				continue;
			Iterator tupleIter = NodeTupleIterator.createIterator(structure.nodes(), predicate.arity());
			while (tupleIter.hasNext()) {
				NodeTuple tuple = (NodeTuple) tupleIter.next();
				if (!tuple.contains(newNode))
					continue;
				NodeTuple destTuple = tuple.substitute(newNode, node);
				structure.update(predicate, tuple, structure.eval(predicate, destTuple));
			}
		}
		return newNode;
	}
	
	/** Singleton pattern.
	 */
	private DuplicateNode() {
	}
}