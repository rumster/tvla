package tvla.language.PTS;

import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;

public class SFTAST extends AST {
	public DeclarationsAST declarations;
	public MacroSectionAST macros;

	
	private static final boolean printJustInterProcNodes = false;
				  
	public SFTAST(DeclarationsAST decls,
				  MacroSectionAST macros) {
		
		this.declarations = decls;
		this.macros = macros;
	}
		
	public void compileAll() {
        declarations.generate();
        macros.generate();
	}
	
	public AST copy() {
		// no need to copy
		return this;
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute thread use.");
	}
}
