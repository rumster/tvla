package tvla.iawp.symbolic;

import tvla.formulae.AllQuantFormula;
import tvla.formulae.ExistQuantFormula;
//import tvla.formulae.PathJoinFormula;

/**
 * non-copying visitor, returning the original formula with fresh variable 
 * for quantifiers. This uses the normalize() method provided by quantified formulae.
 * non-quantified formula propagate the visit to their subformulae.
 * @author Eran Yahav (eyahav)
 */
public class RefreshBoundedVarVisitor extends InPlaceVisitor {

	public Object accept(AllQuantFormula f) {
		f.normalize();
		f.subFormula().visit(this);
		return f;
	}

	public Object accept(ExistQuantFormula f) {
		f.normalize();
		f.subFormula().visit(this);
		return f;
	}

/*
	public Object accept(PathJoinFormula f) {
		f.normalize();
		f.left().visit(this);
		f.right().visit(this);
		return f;
	}
*/
}
