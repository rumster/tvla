package tvla.core.functional;

/* Countable: This is the common base class of all objects used in the
 * functional implementation of the TVS structure; this was added to
 * enable computation of statistics about the number of objects used
 * by different implementations.
 */

abstract class Countable {
	public int visitCount = 0;

	abstract void computeSpace(NPSpaceCounter data);
}
