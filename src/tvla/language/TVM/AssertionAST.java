package tvla.language.TVM;

import java.util.ArrayList;
import java.util.List;

import tvla.language.TVP.CompositeFormulaAST;
import tvla.language.TVP.FormulaAST;
import tvla.language.TVP.MessageStringAST;
import tvla.language.TVP.PredicateAST;

public class AssertionAST extends ActionAST {

	private boolean isHard;
	private FormulaAST assertionFormula;
	
	
	public AssertionAST(String label, FormulaAST f, boolean isHard , String next)
	{
		super(label,null,next);
		this.label = label;
		this.next = next;
		this.isHard = isHard;
		this.assertionFormula = new CompositeFormulaAST(f);
		
		List focusFormulae = new ArrayList();
		
		if (isHard) 
			focusFormulae.add(assertionFormula);
		
		this.def = new ActionDefAST(
									new MessageStringAST("assert"),
									focusFormulae
									, /*pre*/ null ,
									/*messages*/new ArrayList()
									  , /*new*/null ,
									  /*clone*/null,
									  /*updates*/new ArrayList(),
										/*retain*/null,
										/*postMessages*/new ArrayList(),
										/*start*/null
										  , /*wait*/null,
										  /*stop*/null,
											/*use*/null,
											/*halting*/assertionFormula,
											Boolean.FALSE);
	}
	
	public tvla.language.TVP.AST copy() {
		throw new RuntimeException("Can't copy assertion.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute assertion.");
	}
}
