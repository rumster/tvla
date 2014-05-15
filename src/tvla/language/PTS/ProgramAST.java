package tvla.language.PTS;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tvla.analysis.Engine;
import tvla.analysis.interproc.InterProcEngine;
import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;

public class ProgramAST extends AST {
	static final boolean xdebug = false;
	static final java.io.PrintStream out = System.out;
	
	/** a list of MethodDefASTs */
	private List methods = new ArrayList();	
	
	public void addMethod(MethodDefAST methodDef) {
		methods.add(methodDef);
	}
	

	public void generate() {
		InterProcEngine eng = (InterProcEngine) Engine.activeEngine; 
		
		if (xdebug) {
			out.println("\n*** GENERATING PROGRAM ***");
			dump(out);
		}
		
		for (Iterator i = methods.iterator(); i.hasNext();) {
			MethodDefAST md = (MethodDefAST)i.next();
			md.generate();
		}
	}
	
	public void compile() {
		for (Iterator i = methods.iterator(); i.hasNext();) {
			MethodDefAST md = (MethodDefAST)i.next();
			md.compile();
		}
	}
	
	public AST copy() {
		throw new RuntimeException("Can't copy program definitions.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute program definitions.");
	}
	
	public void dump(java.io.PrintStream out) {
		out.println();
		out.println("==========================");
		out.println("= Method Definition Dump =");
		out.println("==========================");
	
		for (int i=0; i<methods.size(); i++) 
			((MethodDefAST) methods.get(i)).dump(out);
	}
}
