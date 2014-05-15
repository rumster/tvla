/*
 * Created on 05/01/2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tvla.iawp.symbolic;

import java.util.Iterator;
import java.util.Map;

import tvla.formulae.Formula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.Var;

/**
 * @author guyerez
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CopyReplaceAndRenameVisitor extends CopyAndReplaceVisitor {

	/**
	 * @param updates
	 */
	public CopyReplaceAndRenameVisitor(Map updates) {
		super(updates);
		copyUpdate = true;
	}
	
	public Formula accept(PredicateFormula f) {
		assert f!=null :"CopyRenameAndReplaceVisitor: predicate formula is null";
		Formula result = null;
		if (updates.containsKey(f.predicate().name())) {
			result = ((Formula)updates.get(f.predicate().name())).copy();
					
			Iterator fFreeIter = f.freeVars().iterator();
			Iterator resultFreeIter = result.freeVars().iterator();			
			while (fFreeIter.hasNext()) {
				result.substituteVar((Var)resultFreeIter.next(),
									 (Var)fFreeIter.next());
			}
			return result;
		} else {
			return f;
 
		}
	}


}
