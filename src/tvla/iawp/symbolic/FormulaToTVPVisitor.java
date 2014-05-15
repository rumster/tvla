/*
 * Created on Jun 22, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tvla.iawp.symbolic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.Logger;

/**
 * Returns a TVP representation of a formula.
* This visitor uses a StringBuffer which is initialized by the first visit, and
* appended-to by subsequent visits. The usage of StringBuffer is to aviod massive
* construction/destruction of String objects.
*
* @author gretay 
*/
public class FormulaToTVPVisitor extends FormulaVisitor {
	
	protected StringBuffer sb = new StringBuffer();
	/**
	 * initialize the StringBuffer to allow reuse.
	 */
	public void init() {
		// empty current buffer for reuse
		sb.setLength(0);
	}	
		private static final String opAnd = " & ";
		private static final String opOr = " | ";
		private static final String opTc = "TC";
		private static final String opNot = " ! ";
		private static final String opEqual = " == ";
		private static final String opEquiv = " <-> ";
		private static final String opImplies = " -> ";
		private static final String opForall = " A ";
		private static final String opExists = " E ";
		private static final String valTrue = "1";
		private static final String valFalse = "0";	
		
		
	private Formula flattenAll(Formula formula, List operands) {
		if (formula instanceof AllQuantFormula) {
			AllQuantFormula aF = (AllQuantFormula)formula;
			operands.add(aF.boundVariable());
			return flattenAll(aF.subFormula(), operands);
		}
		else {
			return formula;			
		}		
	}
	private Formula flattenExist(Formula formula, List operands) {
		if (formula instanceof ExistQuantFormula) {
			ExistQuantFormula aF = (ExistQuantFormula)formula;
			operands.add(aF.boundVariable());
			return flattenExist(aF.subFormula(), operands);
		}
		else {
			return formula;			
		}		
	}
	private void flattenAnd(Formula formula, List operands) {
		if (formula instanceof AndFormula) {
			AndFormula aF = (AndFormula)formula;
			flattenAnd(aF.left(), operands);
			flattenAnd(aF.right(), operands);
		}
		else {
			operands.add(formula);			
		}		
	}
	private void flattenOr(Formula formula, List operands) {
		if (formula instanceof OrFormula) {
			OrFormula oF = (OrFormula)formula;
			flattenOr(oF.left(), operands);
			flattenOr(oF.right(), operands);
		}
		else {
			operands.add(formula);			
		}		
	}	
	
	public Object accept(AndFormula f) {			
		List operands = new ArrayList();
		flattenAnd(f, operands);
							
		assert sb!=null;
		sb.append("(");			
		Formula operand;
		String sep = "";
		for (Iterator iter = operands.iterator(); iter.hasNext(); ){
			sb.append(sep);			
			operand = (Formula) iter.next();
			operand.visit(this);			
			sep = opAnd;			
		}			
		sb.append(")");
		return sb;
	}
	
	public Object accept(OrFormula f) {			
		List operands = new ArrayList();
		flattenOr(f, operands);
									
		assert sb!=null;
		sb.append("(");			
		Formula operand;
		String sep = "";
		for (Iterator iter = operands.iterator(); iter.hasNext(); ){
			sb.append(sep);			
			operand = (Formula) iter.next();
			operand.visit(this);
			sep = opOr;			
		}			
		sb.append(")");
		return sb;
	}
		
	public Object accept(ExistQuantFormula f) {
		List operands = new ArrayList();
		Formula subf = flattenExist(f, operands);
				
		assert sb!=null;
		sb.append("\n");
		sb.append("(");		
		sb.append(opExists);
		sb.append("(");	
		String sep = "";
		Var var;
		for (Iterator iter = operands.iterator(); iter.hasNext(); ){
			sb.append(sep);			
			var = (Var) iter.next();
			sb.append(var.toString());
			sep = ",";			
		}			
		sb.append(")");
		sb.append("(");
		subf.visit(this);
		sb.append(")");
		sb.append(")");
		return sb;
	}
		
	public Object accept(AllQuantFormula f) {
		List operands = new ArrayList();
		Formula subf = flattenAll(f, operands);
		
		assert sb!=null;
		sb.append("\n");
		sb.append("(");		
		sb.append(opForall);
		sb.append("(");	
		String sep = "";
		Var var;
		for (Iterator iter = operands.iterator(); iter.hasNext(); ){
			sb.append(sep);			
			var = (Var) iter.next();
			sb.append(var.toString());
			sep = ",";			
		}			
		sb.append(")");
		sb.append("(");
		subf.visit(this);
		sb.append(")");
		sb.append(")");
		return sb;
	}
		
	
	public Object accept(ImpliesFormula f) {
		assert sb!=null;			
		sb.append("(");
		f.left().visit(this);
		sb.append(opImplies);
		f.right().visit(this);
		sb.append(")");
		return sb;
	}
	
	/**
	 * @see tvla.formulae.FormulaVisitor#visitPredicateFormula(PredicateFormula)
	 */
	public Object accept(PredicateFormula f) {
		assert sb!=null; 
		Predicate p = f.predicate();
		Var[] vars = f.variables();	
			
		sb.append(p.name());
		sb.append("(");
		String sep = "";
		for (int i = 0; i < p.arity(); i++) {
			sb.append(sep);
			sb.append(vars[i].toString());
			sep = ",";
		}
		sb.append(")");
		return sb;
	}

	/**
	 * @see tvla.formulae.FormulaVisitor#visitEqualityFormula(EqualityFormula)
	 */
	public Object accept(EqualityFormula f) {
		assert sb!=null;			
		sb.append("(");
		sb.append(f.left().toString());
		sb.append(opEqual);
		sb.append(f.right().toString());
		sb.append(")");
		return sb;
	}

	/**
	 * @see tvla.formulae.FormulaVisitor#visitEquivalenceFormula(EquivalenceFormula)
	 */
	public Object accept(EquivalenceFormula f) {
		assert sb!=null;			
		sb.append("(");
		f.left().visit(this);
		sb.append(opEquiv);
		f.right().visit(this);
		sb.append(")");
		return sb;
	}

	/**
	 * @see tvla.formulae.FormulaVisitor#visitIfFormula(IfFormula)
	 */
	public Object accept(IfFormula f) {
		assert sb!=null;
		
		sb.append("(");
		f.condSubFormula().visit(this);
		sb.append("?");
		f.trueSubFormula().visit(this);
		sb.append(":");
		f.falseSubFormula().visit(this);
		sb.append(")");			
		return sb;
	}

	/**
	 * @see tvla.formulae.FormulaVisitor#visitNotFormula(NotFormula)
	 */
	public Object accept(NotFormula f) {
		assert sb!=null;			
		sb.append("(");
		sb.append(opNot);			
		f.subFormula().visit(this);
		sb.append(")");
		return sb;
		
	}

	

	/**
	 * @see tvla.formulae.FormulaVisitor#visitTransitiveFormula(TransitiveFormula)
	 */
	public Object accept(TransitiveFormula f) {
		assert sb!=null;		
		// TC LP var:l COMMA var:r RP LP var:sl COMMA var:sr RP formula:f 
		sb.append("(");
		sb.append(opTc);
		sb.append("(");	
		sb.append(f.left().toString());
		sb.append(",");
		sb.append(f.right().toString());
		sb.append(")");
		sb.append("(");	
		sb.append(f.subLeft().toString());
		sb.append(",");
		sb.append(f.subRight().toString());
		sb.append(")");
		f.subFormula().visit(this);
		sb.append(")");
		return sb;
	}

	/**
	 * @see tvla.formulae.FormulaVisitor#visitValueFormula(ValueFormula)
	 */
	public Object accept(ValueFormula f) {
		assert sb!=null;
		if (f.value() == Kleene.trueKleene)
			sb.append(valTrue);
		else if (f.value() == Kleene.falseKleene)
			sb.append(valFalse);
		else
			Logger.fatalError("encountered 1/2 value in translation");
		return sb;
	}

/*
	public Object accept(PathJoinFormula f) {
		throw new RuntimeException("FormulaToTVPVisitor - visitPathJoinFormula not supported");
	}
	*/
}
