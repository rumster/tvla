package tvla.language.PTS;

/** AST for an invocation of a method. 
 *  Can be either a virtual method or a static method. 
 * 
 * @author maon
 */

import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;

public abstract class MethodUseAST extends AST {
	
	/** thread name  */
	private String name;
	/** method definition */
	private MethodDefAST methodDefinition;
	
	public MethodUseAST(String threadName)
	{
		name = threadName;
	}
	
	public MethodDefAST getMethodDef() {
		return methodDefinition;
	}
	
	public AST copy() {
		// no need to copy
		return this;
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute thread use.");
	}
}
