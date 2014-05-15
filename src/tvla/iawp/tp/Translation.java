package tvla.iawp.tp;

import tvla.formulae.Formula;
import tvla.formulae.TransitiveFormula;
import tvla.predicates.Predicate;

/**
 * Translation Interface
 * Interface for theorem prover translation facilities.
 * @author Eran Yahav (yahave)
 */
public interface Translation {
	/**
	 * return a translation of a formula
	 */
	public String translate(Formula f);
	
	/**
	 * return a translation of a predicate. 
	 * this is required for taking care of special characters used
	 * in predicates names and not supported by the theorem prover 
	 */
	public String translate(Predicate p);

	/**
	 * generates a name for a TC formula, allows to pack a TC
	 * formula into a predicate that could be handled to the 
	 * theorem prover for further handling.
	 * Related wrapper methods may be used to create TC-related 
	 * axioms that allow the theorem prover to handle TC predicate
	 * in a sound manner.
	 */
	public String tcPredicateName(TransitiveFormula f);
}
