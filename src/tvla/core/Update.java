package tvla.core;

import java.util.Collection;

import tvla.core.assignments.Assign;
import tvla.formulae.CloneUpdateFormula;
import tvla.formulae.NewUpdateFormula;
import tvla.formulae.PredicateUpdateFormula;
import tvla.formulae.RetainUpdateFormula;

/** This class represents an algorithm used to update a structure
 * according to update formulae specified by the user.
 */
public abstract class Update {
	/** Updates the structure's predicate interpretations according to the specified
	 * update formulae and a partial assignment to their variables.
	 * @param structure The structure to update.
	 * @param updateFormulae A collection of PredicateUpdateFormula objects.
	 * @param assignment A partial assignment to the variables of the right-hand side
	 * of the formulae.
	 * @see tvla.formulae.PredicateUpdateFormula
	 */
	public abstract void updatePredicates(TVS structure,
								 Collection<PredicateUpdateFormula> updateFormulae, 
								 Assign assignment);

	/** Applies the specified <tt> new </tt> formula to add new nodes to the structure.
	 * @param formula A NewUpdateFormula object.
	 * @param assignment A partial assignment to the variables of the formula.
	 * @return A collection containing the new nodes.
	 * @see tvla.formulae.NewUpdateFormula
	 */
	public abstract Collection<Node> applyNewUpdateFormula(TVS structure,
									  NewUpdateFormula formula, 
									  Assign assignment);
	
	/** Applies the specified <tt> clone </tt> formula to clone a sub-structure.
	 * @param formula A formula with one free variable that's used to mark
	 * the part of the universe that should be cloned. The nodes in the cloned
	 * part have isNew = true.
	 * @param assignment A partial assignment to the variables of the formula.
	 * @return A collection containing the new nodes.
	 * @see tvla.formulae.NewUpdateFormula
	 */
	public abstract Collection<Node> applyCloneUpdateFormula(TVS structure, 
												 CloneUpdateFormula formula, 
												 Assign assignment);
	
	/** Applies the specified <tt> retain </tt> formula to determine which nodes
	 * to reatin structure.
	 * @param structure The structure to update.
	 * @param formula A NewUpdateFormula object.
	 * @param assignment A partial assignment to the variables of the formula.
	 * @param refStructure The structure on which the formula is evaluated
	 */
	public abstract void applyRetainUpdateFormula(TVS structure,
										 RetainUpdateFormula formula, 
										 Assign assignment,
										 TVS refStructure);
}