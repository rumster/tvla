package tvla.iawp.symbolic;

import tvla.formulae.AllQuantFormula;
import tvla.formulae.AndFormula;
import tvla.formulae.EqualityFormula;
import tvla.formulae.EquivalenceFormula;
import tvla.formulae.ExistQuantFormula;
import tvla.formulae.Formula;
import tvla.formulae.FormulaVisitor;
import tvla.formulae.IfFormula;
import tvla.formulae.ImpliesFormula;
import tvla.formulae.NotFormula;
import tvla.formulae.OrFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.TransitiveFormula;
import tvla.formulae.ValueFormula;

/**
 * Copy visitor
 * returns a copy of the underlying formula
 * serves as base-visitor implementation for other copying visitors.
 * @author Eran Yahav (eyahav)
 */
public class CopyVisitor extends FormulaVisitor<Formula> {


	public Formula accept(PredicateFormula f) {
		return new PredicateFormula(f.predicate(),f.variables());
	}
	
	/**
	 * propagate visit
	 */	
	public Formula accept(AndFormula f) {
		return new AndFormula(
			f.left().visit(this),
			f.right().visit(this));
	}
	
	/**
	 * propagate visit
	 */
	public Formula accept(AllQuantFormula f) {
		return new AllQuantFormula(
			f.boundVariable(),
			f.subFormula().visit(this));
	}
	
	/**
	 * propagate visit
	 */
	public Formula accept(EqualityFormula f) {
		return new EqualityFormula(f.left(),f.right());
	}
	
	/**
	 * propagate visit
	 */
	public Formula accept(EquivalenceFormula f) {
		return new EquivalenceFormula(
			f.left().visit(this),
			f.right().visit(this));
	}
	
	/**
	 * propagate visit
	 */
	public Formula accept(ExistQuantFormula f) {
		return new ExistQuantFormula(
			f.boundVariable(),
			f.subFormula().visit(this));	
	}
	
	/**
	 * propagate visit
	 */
	public Formula accept(IfFormula f) {
		return new IfFormula(
			f.condSubFormula().visit(this),
			f.trueSubFormula().visit(this),
			f.falseSubFormula().visit(this));
	}
	
	/**
	 * propagate visit
	 */
	public Formula accept(NotFormula f) {
		return new NotFormula(f.subFormula().visit(this));
	}
	
	/**
	 * propagate visit
	 */
	public Formula accept(OrFormula f) {
		return new OrFormula(
			f.left().visit(this),
			f.right().visit(this));
	}
	
	/**
	 * propagate visit
	 */
	public Formula accept(TransitiveFormula f) {
		return new TransitiveFormula(f.left(),f.right(),
			f.subLeft(),
			f.subRight(),
			f.subFormula().visit(this));
	}
	
	/**
	 * propagate visit
	 */
	public Formula accept(ValueFormula f) {
		return new ValueFormula(f.value());
	}
/*
	public Formula accept(PathJoinFormula f) {
		return new PathJoinFormula(	
						f.boundVariable(),
						f.left().visit(this),
						f.right().visit(this));						
	}
*/
	public Formula accept(ImpliesFormula f) {
		return new ImpliesFormula(
			f.left().visit(this),
			f.right().visit(this));
	}
}
