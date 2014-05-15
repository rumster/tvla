package tvla.iawp.symbolic;

import tvla.formulae.AllQuantFormula;
import tvla.formulae.AndFormula;
import tvla.formulae.EqualityFormula;
import tvla.formulae.EquivalenceFormula;
import tvla.formulae.ExistQuantFormula;
import tvla.formulae.FormulaVisitor;
import tvla.formulae.IfFormula;
import tvla.formulae.ImpliesFormula;
import tvla.formulae.NotFormula;
import tvla.formulae.OrFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.TransitiveFormula;
import tvla.formulae.ValueFormula;

/**
 * @author Eran Yahav (eyahav)
 */
public class InPlaceVisitor extends FormulaVisitor {

	/**
	 * @see tvla.formulae.FormulaVisitor#visitAndFormula(AndFormula)
	 */
	public Object accept(AndFormula f) {
		f.left().visit(this);
		f.right().visit(this);
		return f;
	}
	/**
	 * @see tvla.formulae.FormulaVisitor#visitAllQuantFormula(AllQuantFormula)
	 */
	public Object accept(AllQuantFormula f) {
		f.subFormula().visit(this);
		return f;
	}

	/**
	 * @see tvla.formulae.FormulaVisitor#visitPredicateFormula(PredicateFormula)
	 */
	public Object accept(PredicateFormula f) {
		return f;
	}

	/**
	 * @see tvla.formulae.FormulaVisitor#visitEqualityFormula(EqualityFormula)
	 */
	public Object accept(EqualityFormula f) {
		return f;
	}

	/**
	 * @see tvla.formulae.FormulaVisitor#visitEquivalenceFormula(EquivalenceFormula)
	 */
	public Object accept(EquivalenceFormula f) {
		f.left().visit(this);
		f.right().visit(this);
		return f;
	}

	/**
	 * @see tvla.formulae.FormulaVisitor#visitExistQuantFormula(ExistQuantFormula)
	 */
	public Object accept(ExistQuantFormula f) {
		f.subFormula().visit(this);
		return f;
	}

	/**
	 * @see tvla.formulae.FormulaVisitor#visitIfFormula(IfFormula)
	 */
	public Object accept(IfFormula f) {
		f.condSubFormula().visit(this);
		f.trueSubFormula().visit(this);
		f.falseSubFormula().visit(this);
		return f;
	}

	/**
	 * @see tvla.formulae.FormulaVisitor#visitNotFormula(NotFormula)
	 */
	public Object accept(NotFormula f) {
		f.subFormula().visit(this);
		return f;
	}

	/**
	 * @see tvla.formulae.FormulaVisitor#visitOrFormula(OrFormula)
	 */
	public Object accept(OrFormula f) {
		f.left().visit(this);
		f.right().visit(this);
		return f;
	}
	/**
	 * @see tvla.formulae.FormulaVisitor#visitTransitiveFormula(TransitiveFormula)
	 */
	public Object accept(TransitiveFormula f) {
		f.subFormula().visit(this);
		return f;
	}
	/**
	 * @see tvla.formulae.FormulaVisitor#visitValueFormula(ValueFormula)
	 */
	public Object accept(ValueFormula f) {
		return f;
	}

	/**
	 * @see tvla.formulae.FormulaVisitor#visitPathJoinFormula(PathJoinFormula)
	 */
	/*
	public Object accept(PathJoinFormula f) {
		f.left().visit(this);
		f.right().visit(this);
		return f;
	}
	*/
	public Object accept(ImpliesFormula f) {
		f.left().visit(this);
		f.right().visit(this);
		return f;
	}

}
