package tvla.language.TVM;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import tvla.formulae.AndFormula;
import tvla.formulae.Formula;
import tvla.formulae.NotFormula;
import tvla.formulae.OrFormula;
import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;

public class OutputModifierSectionAST extends AST {

	private Collection modifiers;
	
	public OutputModifierSectionAST()
	{
		modifiers = new ArrayList();
	}
	
	public void add(OutputModifierAST modifier) {
		modifiers.add(modifier);
	}
	
	public void generate() {
		Formula positive = null;
		Formula negative = null;
		Formula totalEffect;
		for (Iterator i=modifiers.iterator(); i.hasNext(); ) {
			OutputModifierAST om = (OutputModifierAST)i.next();
			Formula f = om.getFormula();
			if (om.inclusive()) {
				if (positive == null) 
					positive = f;
				else
					positive = new OrFormula(positive,f);
			}
			else
			{
				if (negative == null) 
					negative = f;
				else
					negative = new OrFormula(negative,f);
			}
		}
		
		if ((positive != null) && (negative != null)) 
			totalEffect = new AndFormula(positive, 
								new NotFormula(negative));
		else if (positive != null) 
			totalEffect = positive;
		else if (negative != null)
			totalEffect = new NotFormula(negative);
	}
	
	public void compile() {
	}
	
    public AST copy() {
	throw new RuntimeException("Can't copy output modifier section.");
    }

    public void substitute(PredicateAST from, PredicateAST to) {
	throw new RuntimeException("Can't substitute output modifier section.");
    }
}
