package tvla.core.generic;

import java.util.Iterator;

import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.common.NodeTupleIterator;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;

/** A simple algorithm for counting the numebr of potentially satisfying
 * assignments for a specified predicate and a specified structure.
 * The implementation uses only TVS interface methods.
 * @author Roman Manevich
 * @since 6/9/2001 Initial creation.
 */
public final class NumberSatisfy {
	/** The one and only instance of this class.
	 */
	private static final NumberSatisfy instance = new NumberSatisfy();

	/** Returns the one and only instance of this class.
	 */
	public static NumberSatisfy getInstance() {
		return instance;
	}
	
	/** Returns the number of (potentially) satisfying assignments for the 
	 * specified predicate for this structure.
	 * The implementation is not very efficient, since it conducts iteration
	 * over all of the structure nodes/node pairs.
	 */
	public int numberSatisfy(TVS structure, Predicate predicate) {
		if (predicate.arity() == 0)
			throw new IllegalArgumentException("A nullary predicate does not have " +
				"satisfying assignments!");
	
		int result = 0;
		Iterator tupleIterator = NodeTupleIterator.createIterator(structure.nodes(), predicate.arity());
		while (tupleIterator.hasNext()) {
			NodeTuple tuple = (NodeTuple) tupleIterator.next();
			Kleene value = structure.eval(predicate, tuple);
			if (value != Kleene.falseKleene)
				++result;				
		}
		return result;
	}
	
	/** Singleton pattern.
	 */
	private NumberSatisfy() {
	}
}