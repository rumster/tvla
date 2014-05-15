package tvla.language.TVM;

import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;

public class TVMAST extends AST {
	public DeclarationsAST declarations;
	public MacroSectionAST macros;
	public ProgramAST program;
	public PropertiesAST properties;
	public OutputModifierSectionAST outputModifiers;
				  
				  
	public TVMAST(DeclarationsAST decls,
				  MacroSectionAST macros,
				  ProgramAST program,
				  PropertiesAST properties,
				  OutputModifierSectionAST outputModifiers) {
		
		this.declarations = decls;
		this.macros = macros;
		this.program = program;
		this.properties = properties;
		this.outputModifiers = outputModifiers;
	}
	
	public void generateProgram() {
		program.generate();
	}
	
	public void generateDeclarations() {
		declarations.generate();
	}
	
	public void compileProgram() {
		macros.generate();
		properties.generate();
		program.compile();
	}
	
	public void compileAll() {
		program.generate();
        declarations.generate();
        macros.generate();
        properties.generate();
        program.compile(); 
	}
	
	public AST copy() {
		// no need to copy
		return this;
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute thread use.");
	}
	
}
