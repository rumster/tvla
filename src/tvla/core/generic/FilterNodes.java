package tvla.core.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import tvla.core.Node;
import tvla.core.TVS;
import tvla.predicates.Vocabulary;
import tvla.util.Filter;

/** An algorithm to add a node from one structure to another.
 * Based on DuplicateNode.
 * @author Noam Rinetzky.
 * @since 16/10/2004 Initial creation.
 */
public final class FilterNodes{
	/** The one and only instance of this class.
	 */
	private static FilterNodes instance = new FilterNodes();

	/** Returns the one and only instance of this class.
	 */
	public static FilterNodes getInstance() {
		return instance;
	}
	
	public static void reset() {
		instance = new FilterNodes();
	}

	/** Copies the nodes
	 * @author Noam Rinetzy
	 */
	public void filterNodes(TVS tvs, Filter<Node> filter) {
		assert(tvs != null);
		
		if (tvs.numOfNodes() == 0)
			return;

		Collection<Node> toRemove = new ArrayList<Node>();
		Iterator<Node> nodeItr = tvs.nodes().iterator();
		while (nodeItr.hasNext()) {
			Node node = (Node) nodeItr.next();
			if (!filter.accepts(node)) {
			    toRemove.add(node);
			}
		}
		
		Iterator<Node> delItr = toRemove.iterator();
		if (delItr.hasNext()) {
			tvs.modify(Vocabulary.active);
		}
		
		while (delItr.hasNext()) {
			Node node = (Node) delItr.next();
			tvs.removeNode(node);
		}
	}
}