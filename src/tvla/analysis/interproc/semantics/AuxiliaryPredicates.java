/*
 * File: AuxiliaryPredicates.java 
 * Created on: 19-Apr-2005
 */

package tvla.analysis.interproc.semantics;

import tvla.predicates.Predicate;

/** 
 * @author maon
 */
public class AuxiliaryPredicates {
	   /** The "isObj" predicate.
	 * @author Noam Rinetzky.
	 */
	public static final Predicate isObj = 
		tvla.predicates.Vocabulary.createPredicate("isObj", 1, true);

	/** The "isOLabel" predicate.
	 * @author Noam Rinetzky.
	 */
	public static final Predicate isOLabel = 
		tvla.predicates.Vocabulary.createPredicate("isOLabel", 1, true);

	/** The "isCPLabel" predicate.
	 * @author Noam Rinetzky.
	 */
	public static final Predicate isCPLabel = 
		tvla.predicates.Vocabulary.createPredicate("isCPLabel", 1, true);

	/** The "inUc" predicate.
	 * @author Noam Rinetzky.
	 */
	public static final Predicate inUc = 
		tvla.predicates.Vocabulary.createPredicate("inUc", 1, true);

	/** The "inUx" predicate.
	 * @author Noam Rinetzky.
	 */
	public static final Predicate inUx = 
		tvla.predicates.Vocabulary.createPredicate("inUx", 1, true);
	
	/** The "lbl" predicate.
	 * @author Noam Rinetzky.
	 */
	public static final Predicate lbl = 
		tvla.predicates.Vocabulary.createBinaryPredicate("lbl", true,true,true,true);
	
	/** An auxiliary predicates that marks the nodes that need to be removed
	 * from a combined strucutre.
	 */
	public static final Predicate kill = 
		tvla.predicates.Vocabulary.createPredicate("kill", 1, true);

	/**
	 * A dummy method that enforces initializtion of predicates
	 */
	public static void init() {
		// empty body
	}
	
	/**
	 * 
	 */
	public AuxiliaryPredicates() {
		super();
		// TODO Auto-generated constructor stub
	}

}
