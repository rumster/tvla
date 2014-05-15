/*
 * Created on Dec 23, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package tvla.iawp.symbolic;

import java.util.Map;

import tvla.formulae.Formula;

/**
 * @author gretay
 *
 */
public class WP {
	public static Formula compute(Formula f, Map updateFormulaMap) {	
		RecursiveUpdateVisitor substitutor =
			new RecursiveUpdateVisitor(updateFormulaMap);
		return (Formula) f.visit(substitutor);		
	}
}
