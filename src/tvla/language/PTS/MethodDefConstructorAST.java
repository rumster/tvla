/*
 * File: MethodDefConstructorAST.java 
 * Created on: 23/09/2004
 */

package tvla.language.PTS;

import java.io.PrintStream;
import java.util.List;

/** AST for a definition of a virtual method
 * @author maon
 */
public class MethodDefConstructorAST extends MethodDefAST {

	/**
	 * @param methodName
	 * @param methodActions
	 */
	public MethodDefConstructorAST(String sig, List methodActions) {
		super(sig, methodActions);
	}
	
	public void dump(PrintStream out) {
		out.println("** Constructor Def **");
		super.dump(out);
	}
}
