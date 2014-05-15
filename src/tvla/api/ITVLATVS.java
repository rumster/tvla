package tvla.api;


public interface ITVLATVS {	
	/**
	 *  return whehter the tvs is feasible acording to the consistncy rules.
	 */	
	boolean feasible();
	
	/**
	 * Returns a string with a DOT reprrsentation of the tvs 
	 * @param heading
	 * @return
	 */
	String toDOT(String heading);
	
	/**
	 * returns a .tvs like textual represenation of the structure
	 * @return
	 */
	String toString();
}
