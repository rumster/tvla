package tvla.core.generic;

import java.util.Iterator;

import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.common.NodeTupleIterator;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;

/** A simple algorithm for resetting the values of a specified predicate
 * for a specified structure.
 * The implementation uses only TVS interface methods.
 * @author Roman Manevich.
 * @since 6/9/2001 Initialcreation.
 */
public final class ClearPredicate {
	/** The one and only instance of this class.
	 */
	private static final ClearPredicate instance = new ClearPredicate();

	/** Returns the one and only instance of this class.
	 */
	public static ClearPredicate getInstance() {
		return instance;
	}
	
	/** Clears the specified predicate in the specified structure.
	 * The implementation is not very efficient, since it conducts
	 * an iteration over the structure's nodes/node pairs.
	 */
	public void clearPredicate(TVS structure, Predicate predicate) {
		Iterator tupleIter = NodeTupleIterator.createIterator(structure.nodes(), predicate.arity());
		while (tupleIter.hasNext()) {
			NodeTuple tuple = (NodeTuple) tupleIter.next();
			structure.update(predicate, tuple, Kleene.falseKleene);
		}
	}
	
	/** Singleton pattern.
	 */
	private ClearPredicate() {
	}
}
