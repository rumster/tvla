package tvla.language.PTS;

import java.io.PrintStream;
import java.util.List;

import tvla.analysis.Engine;
import tvla.analysis.interproc.InterProcEngine;
import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;

/** An abstract syntax node for invoking constructors.
 * @author Tal Lev-Ami.
 */
public class StatementConstructorInvocationAST extends StatementInvocationAST {
	public StatementConstructorInvocationAST(String label, String sigOfCallee, List args, String retArg, String next) {
		super(label, sigOfCallee, args, retArg, next);
	}
		
	public void generate(String curentMethodSig) {
		InterProcEngine eng = (InterProcEngine) Engine.activeEngine;
		String macChk = checkMacros();		
		if (macChk != null)
			throw new Error("Error in method: [" + curentMethodSig + "]: " + macChk); 

		eng.addConstructorInvocation(curentMethodSig,
									 from(),to(),
									 calleeSig,args,
									 macroNameCall,macroArgsCall,
									 macroNameRet,macroArgsRet);
	}
	
	public AST copy() {
		return new StatementConstructorInvocationAST(label, calleeSig, args, retArg, next);
	}
	
	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute function invocation.");
	}

	public void dump(PrintStream out) {
		super.dump(out, " CALL TO CONSTRUCTOR ");
	}	
}