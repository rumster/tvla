package tvla.language.PTS;

import java.io.PrintStream;
import java.util.List;

import tvla.analysis.Engine;
import tvla.analysis.interproc.InterProcEngine;
import tvla.language.TVP.AST;
//import tvla.transitionSystem.*;
import tvla.language.TVP.PredicateAST;

/** An abstract syntax node for actions.
 * @author Tal Lev-Ami.
 */
public class StatementIntraAST extends StatementAST {
	String macroName;
	List macroArgs;
	public StatementIntraAST(String label, String macroName, List macroArgs, String next) {
		super(label, next);
		// this.def = def; //!! copy? - no need - creates a new one for every parsed action use 
		this.macroName = macroName;
		this.macroArgs = macroArgs;
	}
		
	public void generate(String sig) {
		InterProcEngine eng = (InterProcEngine) Engine.activeEngine;
		eng.addIntraStmt(sig,
						from(),to(),
						macroName,macroArgs);
	}	
	
	public void substitute(PredicateAST from, PredicateAST to) {
//		def.substitute(from, to);
		throw new InternalError("Cannot substitute intra-procedural statements");
	}
	
	public AST copy() {
		return new StatementIntraAST(label, macroName, macroArgs, next);
	}
	
	public void dump(PrintStream out) {
		out.print(super.from() + " " + macroName + "(");
		for (int i=0; i<macroArgs.size(); i++) {
			out.print((String) macroArgs.get(i));
			if (i < macroArgs.size() -1 )
				out.print(",");	
		}					
		out.println(") " + super.to());
	}
}