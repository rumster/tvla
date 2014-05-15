package tvla.iawp.tp.spass;

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
import tvla.iawp.tp.CommonTranslation;
import tvla.iawp.tp.Translation;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.Logger;
import tvla.util.ProgramProperties;

/**
 * SPASS translation.
 * @author Eran Yahav (yahave)
 */
public class SpassTranslation implements Translation {

	private static SpassTranslation.SPASSTranslationVisitor spv;
	private static SpassTranslation instance;	
				
	public String translate(List fList) { 
		return "";
	}
	public static final String problemHeader =
		SpassStrings.getString("problem_header")
			+ SpassStrings.getString("problem_header_start_desc")
			+ SpassStrings.getString("problem_header_name")
			+ SpassStrings.getString("problem_header_author")
			+ SpassStrings.getString("problem_header_status")
			+ SpassStrings.getString("problem_header_desc")
			+ SpassStrings.getString("problem_header_end_desc");

	public static final String startPredicates =
		SpassStrings.getString("start_predicates");
	public static final String endPredicates =
		SpassStrings.getString("end_predicates");
		
	public static final String startFunctions =
		SpassStrings.getString("start_functions");
	public static final String endFunctions =
		SpassStrings.getString("end_functions");
		
	public static final String startSymbols =
		SpassStrings.getString("start_symbols");
	public static final String endSymbols =
		SpassStrings.getString("end_symbols");

	public static final String startAxioms =
		SpassStrings.getString("start_axioms");
	public static final String endAxioms = 
		SpassStrings.getString("end_axioms");

	public static final String startConjectures =
		SpassStrings.getString("start_conjectures");
	public static final String endConjectures =
		SpassStrings.getString("end_conjectures");

	public static final String problemFooter =
		SpassStrings.getString("problem_footer");

	private static final String startFormula = 
		SpassStrings.getString("start_formula");
	private static final String endFormula = 
		SpassStrings.getString("end_formula");
	private static final String opAnd = SpassStrings.getString("op_and");
	private static final String opOr = SpassStrings.getString("op_or");
	private static final String opTc = SpassStrings.getString("op_tc");
	private static final String opNot = SpassStrings.getString("op_not");
	private static final String opEqual = SpassStrings.getString("op_equal");
	private static final String opEquiv = SpassStrings.getString("op_equiv");
	private static final String opImplies = SpassStrings.getString("op_implies");
	private static final String opForall = SpassStrings.getString("op_forall");
	private static final String opExists = SpassStrings.getString("op_exists");
	private static final String valTrue = SpassStrings.getString("val_true");
	private static final String valFalse = SpassStrings.getString("val_false");	
	
	static {
		//spv = new tvla.iawp.tp.spass.SpassTranslation.SPASSTranslationVisitor();
		spv = new tvla.iawp.tp.spass.SpassTranslation.FlattenSpassVisitor();		
	}

	public static SpassTranslation getInstance() {
		if (instance == null)
			instance = new SpassTranslation();
		return instance;
	}

	/**
	 * @see tvla.iawp.tp.Translation#translate(Formula)
	 */
	public String translate(Formula f) {
		spv.init();
		return startFormula + f.visit(spv).toString() + endFormula;
	}

	/**
	 * @see tvla.iawp.tp.Translation#translate(Predicate)
	 */
	public String translate(Predicate p) {
		String predString = CommonTranslation.removeBrackets(p.toString());
		return predString;
	}

	public String tcPredicateName(TransitiveFormula f) {
		//return generateTCPredicateName(f).toString();
		StringBuffer result = new StringBuffer();
		result.append("_");
		result.append(opTc);
		result.append("_");
		String fStr;
		if (f.subFormula() instanceof PredicateFormula) {
			PredicateFormula pf = (PredicateFormula) f.subFormula();
			fStr = pf.predicate().name(); //baseName();
		}
		else {		
		fStr =
			f
				.subFormula()
				.toString()
				.replace('(', '_')
				.replace(')', '_')
				.replace(',', '_')
				.replace('[', '_')
				.replace(']', '_');
		}
		result.append(fStr);		
		return result.toString();
	}

	private StringBuffer generateTCPredicateName(TransitiveFormula f) {
		StringBuffer result = new StringBuffer();
		
		assert f.subFormula() instanceof PredicateFormula;
		result.append(tcPredicateName(f));
		
		/*
		result.append(opTc);
		result.append("_");
		String fStr =
			f
				.subFormula()
				.toString()
				.replace('(', '_')
				.replace(')', '_')
				.replace(',', '_')
				.replace('[', '_')
				.replace(']', '_');
		
		result.append(fStr);		
		*/
		result.append("(");
		result.append(f.left().toString());
		result.append(",");
		result.append(f.right().toString());
		result.append(")");
		
		return result;
	}

	/**
	* Returns an SPASS representation of a formula.
	* This visitor uses a StringBuffer which is initialized by the first visit, and
	* appended-to by subsequent visits. The usage of StringBuffer is to aviod massive
	* construction/destruction of String objects.
	* 
	* @author Eran Yahav (eyahav)
	*/
	private static class SPASSTranslationVisitor extends FormulaVisitor {
		private final static boolean useFunctions = ProgramProperties.getBooleanProperty(
				"tvla.tp.spass.useFunctions",
				false);
		protected StringBuffer sb = new StringBuffer();
		private SpassTranslation translator = SpassTranslation.getInstance();

		/**
		 * @see tvla.formulae.FormulaVisitor#visitAndFormula(AndFormula)
		 */
		public Object visit(AndFormula f) {
			assert (sb != null);
			sb.append(opAnd);
			sb.append("(");
			f.left().visit(this);
			sb.append(",");
			f.right().visit(this);
			sb.append(")");
			return sb;
		}
		public Object visit(ImpliesFormula f) {
					assert (sb != null);
					sb.append(" -> ");
					sb.append("(");
					f.left().visit(this);
					sb.append(",");
					f.right().visit(this);
					sb.append(")");
					return sb;
				}
		/**
		 * @see tvla.formulae.FormulaVisitor#visitAllQuantFormula(AllQuantFormula)
		 */
		public Object visit(AllQuantFormula f) {
			assert (sb != null);
			sb.append("\n");
			sb.append(opForall);
			sb.append("(");
			sb.append("[");
			sb.append(f.boundVariable().toString());
			sb.append("],");
			f.subFormula().visit(this);
			sb.append(")");
			return sb;
		}

		/**
		 * @see tvla.formulae.FormulaVisitor#visitPredicateFormula(PredicateFormula)
		 */
		public Object visit(PredicateFormula f) {
			assert (sb != null); 
			Predicate p = f.predicate();
			if ((p.name() == "runnable") || 
				(p.name() == "isthread") ||
				(p.name() == "ready") ||
				//(name() == "sm") ||
				(p.name() == "isNew") ||
				(p.name() == "eps") ||
				(p.name() == "instance")) {				
				sb.append(valFalse);
				return sb;
				}
			else if (p.name() == "ac") {
				sb.append(valTrue);
				return sb;
			}		
			
			int cachedArity = p.arity();
			Var[] vars = f.variables();
	
			if ((p.function()) && (useFunctions)) {			
				if (cachedArity != 2)
					throw new RuntimeException("Predicates symbol " + p.name() + " with arity < 2 cannot be defined as function");
				sb.append(opEqual);
				sb.append("(");	
				sb.append(translator.translate(f.predicate()));				
				sb.append("(");	
				sb.append(vars[0].toString());
				sb.append(")");
				sb.append(",");
				sb.append(vars[1].toString());
				sb.append(")");
		
			} else {
					
				sb.append(translator.translate(f.predicate()));
				if (cachedArity > 0) {
					sb.append("(");
					sb.append(vars[0]);
					for (int i = 1; i < cachedArity; i++) {
						sb.append(",");
						sb.append(vars[i].toString());
					}
					sb.append(")");
				}
		}
			
			return sb;
		}

		/**
		 * @see tvla.formulae.FormulaVisitor#visitEqualityFormula(EqualityFormula)
		 */
		public Object visit(EqualityFormula f) {
			assert (sb != null);
			sb.append(opEqual);
			sb.append("(");
			sb.append(f.left().toString());
			sb.append(",");
			sb.append(f.right().toString());
			sb.append(")");
			return sb;
		}

		/**
		 * @see tvla.formulae.FormulaVisitor#visitEquivalenceFormula(EquivalenceFormula)
		 */
		public Object visit(EquivalenceFormula f) {
			assert (sb != null);
			sb.append(opEquiv);
			sb.append("(");
			f.left().visit(this);
			sb.append(",");
			f.right().visit(this);
			sb.append(")");
			return sb;
		}

		/**
		 * @see tvla.formulae.FormulaVisitor#visitExistQuantFormula(ExistQuantFormula)
		 */
		public Object visit(ExistQuantFormula f) {
			assert (sb != null);
			sb.append(opExists);
			sb.append("(");
			sb.append("[");
			sb.append(f.boundVariable().toString());
			sb.append("],");
			f.subFormula().visit(this);
			sb.append(")");
			return sb;
		}

		/**
		 * @see tvla.formulae.FormulaVisitor#visitIfFormula(IfFormula)
		 */
		public Object visit(IfFormula f) {
			assert (sb != null);
			// if P then Q else R = 
			// (P AND Q) OR (!P AND R)
			sb.append(opOr);
			sb.append("(");
			sb.append(opAnd);
			sb.append("(");
			f.condSubFormula().visit(this);
			sb.append(",");
			f.trueSubFormula().visit(this);
			sb.append("), ");
			sb.append(opAnd);
			sb.append("(");
			sb.append(opNot);
			sb.append("(");
			f.condSubFormula().visit(this);
			sb.append("), ");
			f.falseSubFormula().visit(this);
			sb.append("))");
			return sb;
		}

		/**
		 * @see tvla.formulae.FormulaVisitor#visitNotFormula(NotFormula)
		 */
		public Object visit(NotFormula f) {
			assert (sb != null);
			/*
			if (f.subFormula() instanceof PredicateFormula) {
				PredicateFormula predFormula = (PredicateFormula) f.subFormula();
				if (predFormula.predicate().function()) {
					// !f(v1,v2)  translated into: Av3 f(v1,v3) -> v3 != v2
					Predicate p = predFormula.predicate();
					Var newVar = Var.allocateVar();									 			
											 				
					//sb.append("(");
					sb.append(opForall);
					sb.append("(");
					sb.append("[");
					sb.append(newVar.toString());								
					sb.append("],");
					
					sb.append(opImplies);
					sb.append("(");
					Formula newFormula = new  PredicateFormula(p, 
									 			predFormula.variables()[0],
									 			newVar);
					newFormula.visit(this);
					
					
					sb.append(",");
					newFormula = new EqualityFormula(newVar, predFormula.variables()[1]);					
					newFormula.visit(this);
					sb.append(")");
					sb.append(")");
					//sb.append(")");		
					return sb;			
				}				
			} 
			*/
			sb.append(opNot);
			sb.append("(");
			f.subFormula().visit(this);
			sb.append(")");
			return sb;
			
		}

		/**
		 * @see tvla.formulae.FormulaVisitor#visitOrFormula(OrFormula)
		 */
		public Object visit(OrFormula f) {
			assert (sb != null);
			sb.append(opOr);
			sb.append("(");
			f.left().visit(this);
			sb.append(",");
			f.right().visit(this);
			sb.append(")");
			return sb;
		}

		/**
		 * @see tvla.formulae.FormulaVisitor#visitTransitiveFormula(TransitiveFormula)
		 */
		public Object visit(TransitiveFormula f) {
			assert (sb != null);
			sb.append(translator.generateTCPredicateName(f));
			return sb;
		}

		/**
		 * @see tvla.formulae.FormulaVisitor#visitValueFormula(ValueFormula)
		 */
		public Object visit(ValueFormula f) {
			assert (sb != null);
			if (f.value() == Kleene.trueKleene)
				sb.append(valTrue);
			else if (f.value() == Kleene.falseKleene)
				sb.append(valFalse);
			else
				Logger.fatalError("encountered 1/2 value in translation");
			return sb;
		}
		/*
		public Object visit(PathJoinFormula f) {
			assert (sb != null);
			sb.append(opExists);
			sb.append("(");
			sb.append("[");
			sb.append(f.boundVariable().toString());
			sb.append("],");
			sb.append(opAnd);
			sb.append("(");
			f.left().visit(this);
			sb.append(",");
			f.right().visit(this);
			sb.append(")");
			sb.append(")");
			return sb;
		}
		*/

		/**
		 * initialize the StringBuffer to allow reuse.
		 * @author Eran Yahav
		 */
		private void init() {
			// empty current buffer for reuse
			sb.setLength(0);
		}
	}
	private static class FlattenSpassVisitor extends SPASSTranslationVisitor {
		
		public Object visit(AndFormula f) {			
			List operands = new ArrayList();
			flattenAnd(f, operands);
										
			assert (sb != null);
						
			sb.append(opAnd);
			sb.append("(");			
			Formula operand;
			String sep = "";
			for (Iterator iter = operands.iterator(); iter.hasNext(); ){
				sb.append(sep);			
				operand = (Formula) iter.next();
				operand.visit(this);
				sep = ",";			
			}			
			sb.append(")");
			return sb;
		}
		
		public Object visit(OrFormula f) {			
			List operands = new ArrayList();
			flattenOr(f, operands);
										
			assert (sb != null);						
			sb.append(opOr);
			sb.append("(");			
			Formula operand;
			String sep = "";
			for (Iterator iter = operands.iterator(); iter.hasNext(); ){
				sb.append(sep);			
				operand = (Formula) iter.next();
				operand.visit(this);
				sep = ",";			
			}			
			sb.append(")");
			return sb;
		}
		
		public Object visit(ExistQuantFormula f) {
			List operands = new ArrayList();
			Formula subf = flattenExist(f, operands);
			
			assert (sb != null);
			sb.append(opExists);
			sb.append("(");
			sb.append("[");
			String sep = "";
			Var var;
			for (Iterator iter = operands.iterator(); iter.hasNext(); ){
				sb.append(sep);			
				var = (Var) iter.next();
				sb.append(var.toString());
				sep = ",";			
			}			
			sb.append("],");
			subf.visit(this);
			sb.append(")");
			return sb;
		}
		
		public Object visit(AllQuantFormula f) {
			List operands = new ArrayList();
			Formula subf = flattenAll(f, operands);
			
			assert (sb != null);
			sb.append("\n");
			sb.append(opForall);
			sb.append("(");
			sb.append("[");
			String sep = "";
			Var var;
			for (Iterator iter = operands.iterator(); iter.hasNext(); ){
				sb.append(sep);			
				var = (Var) iter.next();
				sb.append(var.toString());
				sep = ",";			
			}			
			sb.append("],");
			subf.visit(this);
			sb.append(")");
			return sb;
		}
		
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
		
	}
}
