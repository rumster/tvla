package tvla.language.TVM;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;

public class ProgramAST extends AST {
	/** a list of ThreadDefASTs */
	private List threads = new ArrayList();
	/** a list of MethodDefASTs */
	private List methods = new ArrayList();
	
	
	public void addThread(ThreadDefAST threadDef) {
		threads.add(threadDef);
	}
	
	public void addMethod(MethodDefAST methodDef) {
		methods.add(methodDef);
	}

	public void generate() {
		for (Iterator i = threads.iterator(); i.hasNext();) {
			ThreadDefAST td = (ThreadDefAST)i.next();
			td.generate();
		}
		
		for (Iterator i = methods.iterator(); i.hasNext();) {
			MethodDefAST md = (MethodDefAST)i.next();
			md.generate();
		}
	}
	
	public void compile() {
		for (Iterator i = threads.iterator(); i.hasNext();) {
			ThreadDefAST td = (ThreadDefAST)i.next();
			td.compile();
		}
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
}
