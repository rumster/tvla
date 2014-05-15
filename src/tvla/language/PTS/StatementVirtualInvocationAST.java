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
public class StatementVirtualInvocationAST extends StatementInvocationAST {
	String receiver;
	String macroNameGuard;
	List macroArgsGuard;
	
	public StatementVirtualInvocationAST(String label, String sigOfCallee, String receiver, List args, String retArg, String next) {
		super(label, sigOfCallee, args, retArg, next);
		this.receiver = receiver;
	}

	public void setGuardMacro(String guard, List guardArgs) {
		this.macroNameGuard = guard;
		this.macroArgsGuard = guardArgs;		
	}

	public void generate(String curentMethodSig) {
		InterProcEngine eng = (InterProcEngine) Engine.activeEngine;
		String macChk = checkMacros();
		if (macChk != null)
			throw new Error("Error in method: [" + curentMethodSig + "]: " + macChk); 
		if (macroNameGuard == null || macroArgsGuard == null)
			throw new InternalError("Error in virtual method: [" + curentMethodSig + "]: bad guard action");
		
		eng.addVirtualInvocation(curentMethodSig,
					            from(),to(),
								calleeSig,args,retArg,receiver,
								macroNameCall,macroArgsCall,
								macroNameRet,macroArgsRet,
								macroNameGuard,macroArgsGuard);
	}	
	
	public AST copy() {
		return new StatementVirtualInvocationAST(label, calleeSig, receiver, args, retArg, next);
	}
	
	public void dump(PrintStream out) {
		super.dump(out, " CALL TO VIRTUAL METHOD receiver = " + receiver + " ");
	}	
}