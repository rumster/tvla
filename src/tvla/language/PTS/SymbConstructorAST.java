/*
 * File: MethodDefStaticAST.java 
 * Created on: 23/09/2004
 */

package tvla.language.PTS;

import java.util.List;

/** AST for a definition of a static method
 * @author maon
 */
public class SymbConstructorAST extends  SymbMethodAST {	
	public SymbConstructorAST(			String signature,
			List formalArgs,
		    String entry,
			String exit) {
		super(signature,formalArgs,entry,exit);
	}
	
	public String getDesc() {		
		return "CONSTRUCTOR :: " + super.toString();
	}		
	
	public String getRetTypeString() {
		return "";
	}

}
