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
public class SymbImplementsAST extends  tvla.language.TVP.AST {	
	String className;
	String interfaceName;	

	public SymbImplementsAST (String className,
							 String interfaceName){
		this.className = className;
		this.interfaceName = interfaceName;	
	}

	public AST copy() {
		throw new RuntimeException("Can't copy implementes definitions.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute implementes definitions.");
	}

}
