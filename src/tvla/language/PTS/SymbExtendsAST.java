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
public class SymbExtendsAST extends  tvla.language.TVP.AST {	
	String subTypeName;
	String superTypeName;	

	public SymbExtendsAST (String subTypeName,
						   String superTypeName){
		this.subTypeName = subTypeName;
		this.superTypeName = superTypeName;	
	}

	public AST copy() {
		throw new RuntimeException("Can't copy extends definitions.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute extends definitions.");
	}

}
