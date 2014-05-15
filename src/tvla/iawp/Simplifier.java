package tvla.iawp;

import java.util.Set;

import tvla.formulae.Formula;

/**
 * The Simplifier interface supports simplification of FOTC formulae.
 * The general problem is the following:
 * - Input: 
 * 		(1) A FOTC formula \varphi
 * 		(2) A set of assumptions A = {a_1,...a_k} given as FOTC formulae
 * - Output:
 * 		(1) A simpler FOTC formula \psi such that \psi and \varphi are 
 * 			equivalent under the assumptions A.
 * 			That is: A => (\varphi <=> \psi)
 * 
 * Simpler usually means that the formula is _smaller_.
 * 
 * @author Eran Yahav (eyahav)
 */
public interface Simplifier {
	/**
	 * simplify a given FOTC formula
	 * @param f -- the formula to be simplified
	 * @return a simplified formula equivalent to f
	 */
	public Formula simplify(Formula f);
	
	/***
	 * add an assumption to the Assumption set.
	 */
	public void addAssumption(Formula f);
	
	/**
	 * return the set of currently used assumptions.
	 */
	public Set getAssumptions();
	
}
