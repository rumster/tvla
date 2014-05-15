package tvla.language.PTS;

import java.io.PrintStream;
import java.util.List;

import tvla.language.TVP.PredicateAST;

/** An abstract syntax node for actions.
 * @author Tal Lev-Ami.
 */
public abstract class StatementInvocationAST extends StatementAST {
	protected String calleeSig;
	protected String retArg;
	protected List args; 
	protected String macroNameCall;
	protected List macroArgsCall;
	protected String macroNameRet;
	protected List macroArgsRet;
	
	
	public StatementInvocationAST(String label, String calleeSig, List args, String retArg, String next) {
		super(label,  next);
		this.calleeSig = calleeSig;
		this.retArg = retArg;
		this.args = args;
	}

	public void setCallMacro(String call, List callArgs) {
		this.macroNameCall = call;
		this.macroArgsCall = callArgs;
	}

	public void setRetMacro(String ret, List retArgs) {
		this.macroNameRet = ret;
		this.macroArgsRet = retArgs;		
	}

	protected String checkMacros() {	
		String ret = null;
		if (macroNameCall == null || macroArgsCall == null)
			ret = new String("Call macro is null at " + label);

		if (macroNameRet == null || macroArgsRet == null)
			ret = new String("Ret macro is null at " + label);

		
	/*	Removed any restriction from the macros!
		if (2 * args.size() != macroArgsCall.size() - 2)
			ret = new String("Call macro has " + macroArgsCall.size() + 
					         " parameters, but should have " + args.size() + " at" + label);

		if (macroArgsRet.size() != macroArgsCall.size() + (retArg == null ? 0 : 2))
			ret = new String("Ret macro has " + macroArgsRet.size() + 
					         " parameters, but should have " + macroArgsRet.size() + " at " + label);
	*/
		return ret;         
	}

	
	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute function invocation.");
	}

	public abstract void dump(PrintStream out);
	
	public void dump(PrintStream out, String typeOfCall) {
		out.print(super.from() +  " " + 
				  typeOfCall +
				  (retArg!= null ? retArg + " = " : " ") +
				  calleeSig + " (");

		for (int i=0; i<args.size(); i++) {
			out.print((String) args.get(i));
			if (i < args.size()-1)
				out.print(",");
		}
			
		out.println(") " + super.to());
	}	

}