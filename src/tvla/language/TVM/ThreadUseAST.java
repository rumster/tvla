package tvla.language.TVM;

import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;

public class ThreadUseAST extends AST {
	
	/** thread name  */
	private String name;
	/** thread definition */
	private ThreadDefAST threadDefinition;
	
	public ThreadUseAST(String threadName)
	{
		name = threadName;
	}
	
	public ThreadDefAST getThreadDef() {
		return threadDefinition;
	}
	
	public void generate() {
		threadDefinition = ThreadDefAST.getThreadDef(name);		
	}
	
	public AST copy() {
		// no need to copy
		return this;
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute thread use.");
	}
}
