package tvla.language.PTS;

import java.io.PrintStream;
import java.util.List;

import tvla.analysis.Engine;
import tvla.analysis.interproc.InterProcEngine;
import tvla.language.TVP.AST;
//import tvla.transitionSystem.*;

/** An abstract syntax node for actions.
 * @author Tal Lev-Ami.
 */
public class StatementStaticInvocationAST extends StatementInvocationAST {

	public StatementStaticInvocationAST(String label, String calleeSig, List args, String retArg, String next) {
		super(label, calleeSig, args, retArg, next);
	}
	
	public void generate(String curentMethodSig) {
		InterProcEngine eng = (InterProcEngine) Engine.activeEngine;
		String macChk = checkMacros();		
		if (macChk != null)
			throw new Error("Error in method: [" + curentMethodSig + "]: " + macChk); 
		
		eng.addStaticInvocation(curentMethodSig,
					            from(),to(),
								calleeSig,args,retArg,
								macroNameCall,macroArgsCall,
								macroNameRet,macroArgsRet);
	}	
	
	public AST copy() {
		return new StatementStaticInvocationAST(label, calleeSig, args, retArg, next);
	}
		
	public void dump(PrintStream out) {
		super.dump(out, " CALL TO STAIC METHOD ");
	}
}