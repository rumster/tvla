package tvla.language.TVM;

import tvla.analysis.Engine;
import tvla.analysis.multithreading.MultithreadEngine;
import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;
import tvla.transitionSystem.Action;

/** An abstract syntax node for global actions.
 * @author Eran Yahav.
 */
public class GlobalActionAST extends AST {
	public tvla.language.TVP.ActionDefAST def;
	
	public GlobalActionAST(tvla.language.TVP.ActionDefAST def) {
		this.def = def;
	}

	public tvla.language.TVP.AST copy() {
		return new GlobalActionAST((ActionDefAST) def.copy());
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		def.substitute(from, to);
	}
	
	public void generate() {
		Action action = ((ActionDefAST) def).getGlobalAction();
		MultithreadEngine engine = (MultithreadEngine) Engine.activeEngine;
		engine.addAction(action);
    }	
}