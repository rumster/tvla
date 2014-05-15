/*
 * File: PrintNodeAST.java 
 * Created on: 23/09/2004
 */

package tvla.language.PTS;

import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;

/** AST for a definition of a static method
 * @author maon
 */
public class PrintNodeAST extends  tvla.language.TVP.AST {	
	public final String methodSig;
	public final String nodeLabel;	

	public PrintNodeAST (String sig, String label){
		this.methodSig = sig;
		this.nodeLabel = label;	
	}

	public String getMethodSig() {
		return methodSig;
	}

	public String getNodeLabel() {
		return nodeLabel;
	}

	public AST copy() {
		throw new RuntimeException("Can't copy implementes definitions.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute implementes definitions.");
	}

}
