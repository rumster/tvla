package tvla.iawp.tp;

/**
 * @author Eran Yahav (yahave)
 */
public class TheoremProverStatus {
	public long numberOfCalls;

	public TheoremProverStatus(long numberOfCalls) {
		this.numberOfCalls = numberOfCalls;
	}
	
	/**
	 * Returns the numberOfCalls.
	 * @return long
	 */
	public long getNumberOfCalls() {
		return numberOfCalls;
	}

	/**
	 * Sets the numberOfCalls.
	 * @param numberOfCalls The numberOfCalls to set
	 */
	public void setNumberOfCalls(long numberOfCalls) {
		this.numberOfCalls = numberOfCalls;
	}

}
