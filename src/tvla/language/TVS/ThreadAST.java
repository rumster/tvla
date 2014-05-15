package tvla.language.TVS;

import tvla.analysis.Engine;
import tvla.analysis.multithreading.MultithreadEngine;
import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;

/** An abstract syntax node that represents threads.
 * @author Eran Yahav.
 */
public class ThreadAST extends AST {
	private String name;
	private String type;
	private String initialLabel;
	
	public ThreadAST(String threadName, String threadType, String label) {
		name = threadName;
		type = threadType;
		initialLabel = label;
	}
	
	public ThreadAST(String threadName, String threadType) {
		name = threadName;
		type = threadType;
		MultithreadEngine engine = (MultithreadEngine) Engine.activeEngine;
		initialLabel = engine.getEntryLabel(type);
	}
	
	public String threadName() {
		return name;
	}
	
	public String threadType() {
		return type;
	}
	
	public String entryLabel() {
		return initialLabel;
	}
	
	public AST copy() {
		throw new RuntimeException("Can't copy thread instances.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute thread instances.");
	}
}