package tvla.api;

/**
 * A utlity class for handling 3 valuded logic
 * @author maon
 *
 */

public interface ITVLAKleene {
	/**
	 * An interface to canonic Kleene values (either 0, 1/2, or 1)
	 * @author maon
	 *
	 */
	public static interface ITVLAKleeneValue {
		boolean isTrue();
		boolean isFalse();
		boolean isUnknown();
	}
	
	/**
	 * returns a true ITVLAKleeneValue
	 * @return
	 */
	public ITVLAKleeneValue trueVal();

	/**
	 * returns a false ITVLAKleeneValue
	 * @return
	 */
	public ITVLAKleeneValue falseVal();
	
	/**
	 * returns an unknown (1/2) ITVLAKleeneValue
	 * @return
	 */
	public ITVLAKleeneValue unknownVal();

	/**
	 * Joins the set of kleene values in the array 
	 * @param val1 an interface Kleene value
	 * @param val2 an interface Kleene value
	 * @return
	 */
	public ITVLAKleeneValue join(ITVLAKleeneValue val1, ITVLAKleeneValue val2);

	/**
	 * Meets the set of kleene values in the array 
	 * @param val1 an interface Kleene value
	 * @param val2 an interface Kleene value
	 * @return
	 */
	public ITVLAKleeneValue meet(ITVLAKleeneValue val1, ITVLAKleeneValue val2);
}