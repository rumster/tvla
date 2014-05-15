package tvla.core;

import java.util.Iterator;

/** An interface for optimizing operations on TVS representations
 * that exploit sparsity of predicates.
 */
public interface SparseTVS {
	/** Returns an iterator over the non-zero predicates of this structure
	 * which are in its current vocabulary.
	 */
	public Iterator nonZeroPredicates();
}
