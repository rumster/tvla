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
public class SymbClassAST extends  tvla.language.TVP.AST {	
	String className;	

	public SymbClassAST (String className){
		this.className = className;	
	}

	public AST copy() {
		throw new RuntimeException("Can't copy class decleration.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute class decleration.");
	}

}
