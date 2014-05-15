/*
 * File: MethodDefStaticAST.java 
 * Created on: 23/09/2004
 */

package tvla.language.PTS;

import java.util.List;

/** AST for a definition of a static method
 * @author maon
 */
public class SymbMethodStaticAST extends  SymbMethodAST {
	public SymbMethodStaticAST (
			String signature,
			List formalArgs,
		    String entry,
			String exit) {
		super(signature,formalArgs,entry,exit);
		
		int preRetType = signature.indexOf(' ') + 1;
		int postRetType = signature.indexOf(' ',preRetType);
		assert(preRetType < postRetType);
		this.retType = signature.substring(preRetType+1,postRetType);
	}
	
	public String getDesc() {		
		return "STATIC METHOD :: " + super.getDesc();
	}
	
	public String getRetTypeString() {
		return retType + " ";
	}

}
