/**
 * Contains all the macro (action) definition in the analyzed program. 
 */
package tvla.language.PTS;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tvla.analysis.Engine;
import tvla.analysis.interproc.InterProcEngine;
import tvla.language.TVP.AST;
import tvla.language.TVP.MacroAST;
import tvla.language.TVP.PredicateAST;

public class MacroSectionAST extends AST {	
	protected List macroList;
	
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
			InterProcEngine eng = (InterProcEngine) Engine.activeEngine;
			ActionMacroAST macro = (ActionMacroAST) i.next();
			eng.addActionDefinition(macro);
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
