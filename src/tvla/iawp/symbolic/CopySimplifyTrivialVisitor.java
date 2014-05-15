package tvla.iawp.symbolic;

import tvla.formulae.AllQuantFormula;
import tvla.formulae.AndFormula;
import tvla.formulae.EqualityFormula;
import tvla.formulae.EquivalenceFormula;
import tvla.formulae.ExistQuantFormula;
import tvla.formulae.Formula;
import tvla.formulae.IfFormula;
import tvla.formulae.NotFormula;
import tvla.formulae.OrFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.TransitiveFormula;
import tvla.formulae.ValueFormula;

/**
 * returns a copy of the formula performing trivial simplifications
 * P & 1 =r=> P
 * P & 0 =r=> 0
 * P | 1 =r=> 1
 * P | 0 =r=> P
 * P & P =r=> P
 * 
 * @author Eran Yahav (eyahav)
 */
public class CopySimplifyTrivialVisitor extends CopyVisitor {

	public Formula accept(PredicateFormula f) {
		return new PredicateFormula(f.predicate(),f.variables());
	}
	
	/**
	 * lf & T  =r=> lf
	 * T  & rf =r=> rf
	 * lf & F  =r=> F
	 * F  & rf =r=> F
	 * f  & f  =r=> f
	 */	
	public Formula accept(AndFormula f) {
		Formula newLeft = (Formula)f.left().visit(this);
		Formula newRight = (Formula)f.right().visit(this);
		
		if (newRight.equals(ValueFormula.kleeneTrueFormula))
			return newLeft;
		if (newLeft.equals(ValueFormula.kleeneTrueFormula))
			return newRight;
		if (newRight.equals(ValueFormula.kleeneFalseFormula) ||
			newLeft.equals(ValueFormula.kleeneFalseFormula))
			return ValueFormula.kleeneFalseFormula;
		
		if (newRight.equals(newLeft))
			return newRight;
		
		
		return new AndFormula(newLeft,newRight);
	}
	
	/**
	 * A true =r=> true
	 * other cases depend on underlying structure
	 */
	public Formula accept(AllQuantFormula f) {
		Formula newSubFormula = (Formula)f.subFormula().visit(this);
		if (newSubFormula.equals(ValueFormula.kleeneTrueFormula))
			return ValueFormula.kleeneTrueFormula;	
		
		return new AllQuantFormula(f.boundVariable(),newSubFormula);
	}
	
	/**
	 * if same variable, then this is true
	 */
	public Formula accept(EqualityFormula f) {
		if ((f.left()).equals(f.right()))
			return ValueFormula.kleeneTrueFormula;
			
		return new EqualityFormula(f.left(),f.right());
	}
	
	/**
	 * F  <=> rf 	=r=> !rf
	 * lf <=> F 	=r=> !lf
	 * T  <=> rf 	=r=> rf
	 * lf <=> T 	=r=> lf
	 * f <=> f 		=r=> f
	 */
	public Formula accept(EquivalenceFormula f) {
		Formula newLeft = (Formula)f.left().visit(this);
		Formula newRight = (Formula)f.right().visit(this);
		if (newLeft.equals(ValueFormula.kleeneFalseFormula))
			return (new NotFormula(newRight)).visit(this);
		if (newRight.equals(ValueFormula.kleeneFalseFormula))
			return (new NotFormula(newLeft)).visit(this);
		if (newLeft.equals(ValueFormula.kleeneTrueFormula))
			return newRight;
		if (newRight.equals(ValueFormula.kleeneTrueFormula))
			return newLeft;
		if (newRight.equals(newLeft)) 
			return newLeft;
		
		return new EquivalenceFormula(newLeft,newRight);
	}
	
	/**
	 * E false =r=> false
	 */
	public Formula accept(ExistQuantFormula f) {
		Formula newSubFormula = (Formula)f.subFormula().visit(this);
		if (newSubFormula.equals(ValueFormula.kleeneFalseFormula))
			return ValueFormula.kleeneFalseFormula;
		
		return new ExistQuantFormula(f.boundVariable(),newSubFormula);	
	}
	
	/**
	 * T  ? tf : ff 	=r=> tf
	 * F  ? tf : ff 	=r=> ff
	 * cf ? T  : ff 	=r=> cf  | ff  
	 * cf ? F  : ff 	=r=> !cf & ff
	 * cf ? tf : T 		=r=> !cf | tf
	 * cf ? tf : F 		=r=> cf  & tf
	 */
	public Formula accept(IfFormula f) {
		Formula newCond =  (Formula)f.condSubFormula().visit(this);
		Formula newTrue = (Formula)f.trueSubFormula().visit(this);
		Formula newFalse = (Formula)f.falseSubFormula().visit(this);
		
		if (newCond.equals(ValueFormula.kleeneTrueFormula))
			return newTrue;
		if (newCond.equals(ValueFormula.kleeneFalseFormula))
			return newFalse;
		if (newTrue.equals(ValueFormula.kleeneTrueFormula))
			return (new OrFormula(newCond,newFalse)).visit(this);
		if (newTrue.equals(ValueFormula.kleeneFalseFormula))
			return (new AndFormula(
				new NotFormula(newCond),newFalse)).visit(this);
		if (newFalse.equals(ValueFormula.kleeneTrueFormula))
			return (new OrFormula(
				new NotFormula(newCond),newTrue)).visit(this);
		if (newFalse.equals(ValueFormula.kleeneFalseFormula))
			return (new AndFormula(newCond,newTrue)).visit(this);
			
		return new IfFormula(newCond,newTrue,newFalse);
	}
	
	/**
	 * !T =r=> F
	 * !F =r=> T
	 */
	public Formula accept(NotFormula f) {
		Formula newSubFormula = (Formula)f.subFormula().visit(this); 
		
		if (newSubFormula.equals(ValueFormula.kleeneTrueFormula))
			return ValueFormula.kleeneFalseFormula;
		if (newSubFormula.equals(ValueFormula.kleeneFalseFormula))
			return ValueFormula.kleeneTrueFormula;	
		
		return new NotFormula(newSubFormula);
	}
	
	/**
	 * left | T 		=r=> T
	 * T 	| right 	=r=> T
	 * left | F 		=r=> left
	 * F 	| right 	=r=> right
	 * f    | f			=r=> f
	 */	
	public Formula accept(OrFormula f) {
		Formula newLeft = (Formula)f.left().visit(this);
		Formula newRight = (Formula)f.right().visit(this);
		
		if(newLeft.equals(ValueFormula.kleeneTrueFormula) ||
			newRight.equals(ValueFormula.kleeneTrueFormula))
				return ValueFormula.kleeneTrueFormula;
		if (newLeft.equals(ValueFormula.kleeneFalseFormula))
			return newRight;
		if (newRight.equals(ValueFormula.kleeneFalseFormula))
			return newLeft;
		if (newRight.equals(newLeft)) 
			return newLeft;
		
		return new OrFormula(newLeft,newRight);		
	}
	
	/**
	 * TC v1,v2 : F (v3,v4) =r=> F
	 * other cases depend on underlying structure 
	 */
	public Formula accept(TransitiveFormula f) {
		Formula newSubFormula = (Formula)f.subFormula().visit(this);
		
		if (newSubFormula.equals(ValueFormula.kleeneFalseFormula))
			return ValueFormula.kleeneFalseFormula;
		
		return new TransitiveFormula(f.left(),f.right(),
			f.subLeft(),f.subRight(),newSubFormula);
	}
	
	/**
	 * nothing to do here, just copy
	 */
	public Formula accept(ValueFormula f) {
		return new ValueFormula(f.value());
	}

	/**
	 * Ev T  & lf  =r=> could be simplified, leave it for now
	 * Ev F  & lf  =r=> F (see existential quantifier)
	 * Ev rf & T   =r=> could be simplified, leave it for now
	 * Ev rf & F   =r=> F (see existential quantifier)
	 */
	/*
	public Formula accept(PathJoinFormula f) {
		Formula newLeft = (Formula)f.left().visit(this);
		Formula newRight = (Formula)f.right().visit(this);
		
		if (newLeft.equals(ValueFormula.kleeneFalseFormula))
			return ValueFormula.kleeneFalseFormula;
		
		if (newRight.equals(ValueFormula.kleeneFalseFormula))
			return ValueFormula.kleeneFalseFormula;
		
		return new PathJoinFormula(	
						f.boundVariable(),newLeft,newRight);						
	}
	*/

}
