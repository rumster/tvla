package tvla.api;

/**
 * An iterator over the TVS that were generated and stored in the repository
 * Bsically, this is an interface of a  standard Iterator where next() returns
 * indexes of strucutres, and remove is not supported.
 *
 * @author maon
 */
public interface ITVLATVSIndexIterator {
	/*
	 * Are there any more strcutres in the repository
	 */
	public boolean hasNext();

	/*
	 * Give me next structre and advance
	 * May invoke client.handleException with Exception NoSuchElementException
	 * if iterated out of the repository
	 */
	public int next();
}
