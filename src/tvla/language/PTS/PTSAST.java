package tvla.language.PTS;

import java.util.Iterator;
import java.util.List;

import tvla.analysis.Engine;
import tvla.analysis.interproc.InterProcEngine;
import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;

public class PTSAST extends AST {
	public DeclarationsAST declarations;
	public MacroSectionAST macros;
	public SymbAST symbs;
	public ProgramAST program;
	public List print_nodes;
	
	private static final boolean printJustInterProcNodes = false;
				  
	public PTSAST(SymbAST symbs,
				  DeclarationsAST decls,
				  MacroSectionAST macros,
				  ProgramAST program,
				  List print_nodes) {
		
		this.symbs = symbs;
		this.declarations = decls;
		this.macros = macros;
		this.program = program;
		this.print_nodes = print_nodes;
	}
		
	public void compileAll() {
		symbs.compile();
        declarations.generate();
        macros.generate();
		program.generate();
        program.compile(); 
        processPrintNodesList();
	}
	
	public AST copy() {
		// no need to copy
		return this;
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute thread use.");
	}
	
	private  void processPrintNodesList() {
		InterProcEngine eng = (InterProcEngine) Engine.activeEngine; 
		if (print_nodes.isEmpty()) {
			if (printJustInterProcNodes)
				eng.setPrintInterProcNodes();
			else
				eng.setPrintAllNodes();
			return;
		}
		
		Iterator printNodeItr = print_nodes.iterator();
		while (printNodeItr.hasNext()) {
			PrintNodeAST pn = (PrintNodeAST) printNodeItr.next();
			if (pn.getNodeLabel() != null)
				eng.setPrintNode(pn.getMethodSig(), pn.getNodeLabel());
			else
				eng.setPrintAllNodesOfMethod(pn.getMethodSig());
		}
	}

}
