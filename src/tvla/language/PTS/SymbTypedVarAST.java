/*
 * File: MethodDefStaticAST.java 
 * Created on: 23/09/2004
 */

package tvla.language.PTS;

import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;

/** AST for a definition of a static method
 * @author maon
 */
public class SymbTypedVarAST extends  tvla.language.TVP.AST {	
	String typeName;
	String varName;


	public SymbTypedVarAST (String typeName, String varName){
		this.typeName = typeName;
		this.varName = varName;
	}

	public AST copy() {
		throw new RuntimeException("Can't copy typed var decleration.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute typed var decleration.");
	}

}
