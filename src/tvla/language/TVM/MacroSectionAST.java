package tvla.language.TVM;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tvla.language.TVP.AST;
import tvla.language.TVP.MacroAST;
import tvla.language.TVP.PredicateAST;

public class MacroSectionAST extends AST {
	
	private List macroList;
	
	public MacroSectionAST()
	{
		macroList = new ArrayList();
	}
	
	public MacroSectionAST(List list)
	{
		macroList = list;
	}
	
	public void addMacro(MacroAST macro) {
		macroList.add(macro);
	}
	
	public void generate() {
		for (Iterator i = macroList.iterator(); i.hasNext(); ) {
			AST ast = (AST) i.next();
			ast.generate();
		}
	}
	
	public void compile() {		
	}
	
	public AST copy() {
		throw new RuntimeException("Can't copy declarations.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute declarations.");
	}
}
