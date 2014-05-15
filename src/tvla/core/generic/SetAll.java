package tvla.core.generic;

import java.util.Collection;
import java.util.Iterator;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;

/** An algorithm to set the value of an unary predicate to all
 * the nodes in the structure.
 * Based on DuplicateNode.
 * @author Noam Rinetzky.
 * @since 16/10/2004 Initial creation.
 */
public final class SetAll{
	/** The one and only instance of this class.
	 */
	private static final SetAll instance = new SetAll();

	/** Returns the one and only instance of this class.
	 */
	public static SetAll getInstance() {
		return instance;
	}

	/** Set the preidcate.
	 * @author Noam Rinetzy
	 */
	public void setAll(TVS structure, Predicate uniPred, Kleene val) {
		assert(uniPred != null);
		assert(uniPred.arity() == 1);
		
		structure.clearPredicate(uniPred);
		
		Collection allNodes = structure.nodes();
		Iterator nodeItr = allNodes.iterator();
		while (nodeItr.hasNext()) {
			Node node = (Node) nodeItr.next();
			structure.update(uniPred,NodeTuple.createSingle(node),val);
		}		
	}
}