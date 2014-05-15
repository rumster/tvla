package tvla.core;

import java.util.Collection;
import java.util.Map;

import tvla.core.Combine.INullaryCombiner;
import tvla.core.assignments.Assign;
import tvla.core.generic.AdvancedCoerce;
import tvla.core.generic.GenericBlur;
import tvla.core.generic.GenericCombine;
import tvla.core.generic.GenericFocus;
import tvla.core.generic.GenericUpdate;
import tvla.formulae.CloneUpdateFormula;
import tvla.formulae.Formula;
import tvla.formulae.NewUpdateFormula;
import tvla.formulae.RetainUpdateFormula;
import tvla.predicates.Predicate;

/** A high-level interface for three-valued structures.
 * This interface provides high-level functionalities that can be used by
 * analysis engines. 
 * 
 * @author Ganesan Ramalingam.
 * @author Roman Manevich.
 * @author Deepak Goyal.
 * @author John Field.
 * @author Mooly Sagiv.
 */
public abstract class HighLevelTVS extends TVS implements Comparable<HighLevelTVS> {
	/** the default implementation of the Blur operation.
	 */
	protected static Blur genericBlur = new GenericBlur();

	/** the advanced implementation of the Coerce operation.
	 */
	public static Coerce advancedCoerce = new AdvancedCoerce(Constraints.getInstance().constraints());
	
	/** the default implementation of the Focus operation.
	 */
	protected static Focus  genericFocus = new GenericFocus();

	/** the default implementation of the Update operation.
	 */
	protected static Update genericUpdate = new GenericUpdate();

	/** the default implementation of the Combine operation.
	 */
	protected static Combine genericCombine = new GenericCombine();
	/** Bounds the structure in-place.
	 */
	
	public static void reset() {
		genericBlur = new GenericBlur();
		advancedCoerce = new AdvancedCoerce(Constraints.getInstance().constraints());
		genericFocus = new GenericFocus();
		genericUpdate = new GenericUpdate();
		genericCombine = new GenericCombine();
	}
    
    /**
     * Covariant type for copy
     */
    public abstract HighLevelTVS copy();
    
	public void blur() {
		genericBlur.blur(this);
	}

	/** Applies a constraint-solver to the structure.
	 * @return true is the structure is feasible and false otherwise.
	 */
	public boolean coerce() {
		return advancedCoerce.coerce(this);
	}
	
	/** Applies the focus algorithm with the specified formula.
	 * @param focusFormula A Formula object.
	 * @return The collection of focused structures.
	 */
	public Collection<HighLevelTVS> focus(Formula focusFormula) {
		return genericFocus.focus(this, focusFormula);
	}

	/** Updates the structure's predicate interpretations according to the specified
	 * update formulae and a partial assignment to their variables.
	 * @param updateFormulae A collection of PredicateUpdateFormula objects.
	 * @param assignment A partial assignment to the variables of the right-hand side
	 * of the formulae.
	 * @see tvla.formulae.PredicateUpdateFormula
	 */
	public void updatePredicates(Collection updateFormulae, Assign assignment) {
		genericUpdate.updatePredicates(this, updateFormulae, assignment);
	}

	/** Applies the specified <tt> new </tt> formula to add new nodes to the structure.
	 * @param formula A NewUpdateFormula object.
	 * @param assignment A partial assignment to the variables of the formula.
	 * @return A collection containing the new nodes.
	 * @see tvla.formulae.NewUpdateFormula
	 */
	public Collection applyNewUpdateFormula(NewUpdateFormula formula, Assign assignment) {
		return genericUpdate.applyNewUpdateFormula(this, formula, assignment);
	}
	
	/** Applies the specified <tt> clone </tt> formula to clone a sub-structure.
	 * @param formula A formula with one free variable that's used to mark
	 * the part of the universe that should be cloned. The nodes in the cloned
	 * part have isNew = true.
	 * @param assignment A partial assignment to the variables of the formula.
	 * @return A collection containing the new nodes.
	 * @see tvla.formulae.NewUpdateFormula
	 */
	public Collection applyCloneUpdateFormula(CloneUpdateFormula formula, Assign assignment) {
		return genericUpdate.applyCloneUpdateFormula(this, formula, assignment);
	}

	/** Applies the specified <tt> retain </tt> formula to remove nodes from 
	 * the structure's universe.
	 * @param formula A RetainUpdateFormula object.
	 * @param assignment A partial assignment to the variables of the formula.
	 * @see tvla.formulae.RetainUpdateFormula
	 */
	public void applyRetainUpdateFormula(RetainUpdateFormula formula, Assign assignment, TVS refStructure) {
		genericUpdate.applyRetainUpdateFormula(this, formula, assignment, refStructure);
	}
 	public  int numOfNodes() {
		return nodes().size();
	}

 	/**
 	 * Applies the generic combine operation.
 	 * @author maon
 	 */  
    protected TVS combineWith(INullaryCombiner nullaryCombiner, TVS tvsR) {
        return  genericCombine.combine(nullaryCombiner, this, tvsR);
    }
    
    public HighLevelTVS permute(Map<Predicate, Predicate> mapping) {
        throw new UnsupportedOperationException();        
    }
    
    
    /********************************************************
     * Verify that the hashCode and Equals are never called 
     ********************************************************/
    
    public boolean equals(Object o) {
      return super.equals(o);
    }
    
    public int hashCode() {
      return super.hashCode();
    }
    
    public int compareTo(HighLevelTVS o) {
    	return hashCode() - o.hashCode();
    }
}