package tvla.iawp.symbolic;

import java.util.Map;

import tvla.formulae.Formula;
import tvla.formulae.PredicateFormula;

/**
 * Copy and replace visitor creates a new copy of the formula, replacing
 * predicateFormulae according to an update map mapping predicate-formula
 * to new formula.
 * Note: 
 * 1. the predicate-formula are identified by reference-equality. That is,
 * predicate-formulae to be replaced are given by references to actual predicate
 * formulae in the original formula.
 * 2. the replacement is low-level plugging of the new formula, no adjustment
 * of variable names is applied. Responsibility for adjusting the new formulae
 * with respect to variable names is on the caller of the visitor.
 * This is done to enable the use of this visitor for general replacement. 
 * 
 * @author Eran Yahav
 */
public class CopyAndReplaceVisitor extends CopyVisitor
{
	
	/**
	 * a Map from a predicateFormula -> newFormula
	 */
	protected Map updates;


	/**
	 * copy update formula? true by default
	 */
	protected boolean copyUpdate = true;

	/**
	 * assumes that the predicate formula is present in the visited formula
	 */
	public CopyAndReplaceVisitor(Map updates) {
		this.updates = updates;
	}
	
	public CopyAndReplaceVisitor(Map updates,boolean copyUpdate) {
		this.updates = updates;
		this.copyUpdate = copyUpdate;
	}
	
	/**
	 * replace predicate-formula according to updates map.
	 * if predicate-formula is not in the map, just return a copy
	 * of the current predicate-formula.
	 */
	public Formula accept(PredicateFormula f) {
		assert f!=null:"CopyAndReplaceVisitor: predicate formula is null";
		Formula result = null;
		if (updates.containsKey(f)) {
			return copyUpdate ? 
				((Formula)updates.get(f)).copy() 
				: (Formula)updates.get(f);
		} else 
			return f.copy();
	}
}
