package tvla.language.PTS;

/** AST for a definition of a method. 
 *  Can be either a virtual method or a static method.
 *  
 * @author maon
 */

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tvla.analysis.Engine;
import tvla.analysis.interproc.InterProcEngine;
import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;

public abstract class MethodDefAST extends AST {
	private final static boolean xdebug = false;
	private final static java.io.PrintStream out = System.out;
	
	/** The method declaration (signatire + parameters)*/
	private String sig;
	/** a list of ActionASTs */
	protected List stmts;
		
	
	public MethodDefAST(String sig, List methodStmts)  
	{
		this.sig = sig;
		this.stmts = methodStmts;
		
		if (xdebug)
			out.println("MethodDefAST created for: " + sig);
	}
			

	public void generate() {
		InterProcEngine eng = (InterProcEngine) Engine.activeEngine;
		eng.addMethodDefinition(sig,stmts.size());	
	}
	
	/*
	 * Generating the code of all the methods except of
	 * the static initializers because they invoke
	 * wierd things.
	 * FIXME ! 
	 */
	

	public void compile() {
		InterProcEngine eng = (InterProcEngine) Engine.activeEngine;
		

		if (0 < sig.indexOf("<clinit>")) {
			// this is a static initializer !
			StatementIntraAST unmodeled = 
				new StatementIntraAST(
						"CLINIT_entry", "Unmodeled", new ArrayList(), "CLINIT_exit");
			unmodeled.generate(sig);
			
			System.err.println("Code for static initializer: " + sig + " not modeled !");
			return;
		}

		System.err.println("Compiling method: " + sig);
		
		for(Iterator itr = stmts.iterator(); itr.hasNext(); ) {
			StatementAST stmt = (StatementAST) itr.next();
			stmt.generate(sig);			
		}
	}
	
	public AST copy() {
		throw new RuntimeException("Can't copy method definitions.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute method definitions.");
	}
	
	public void dump(PrintStream out) {
		out.println("MethodDefAST DUMP:\n" + sig );
		Iterator itr = stmts.iterator();
		while (itr.hasNext()) {
			StatementAST stmt = (StatementAST) itr.next();
			stmt.dump(out);
		}
	}
}
