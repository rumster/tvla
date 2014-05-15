package tvla.language.TVP;

import tvla.transitionSystem.Action;
import tvla.transitionSystem.AnalysisGraph;

/** An abstract syntax node for actions.
 * @author Tal Lev-Ami.
 */
public class ActionAST extends AST {
	protected ActionDefAST def;
	protected String next;
	protected String label;

	public ActionAST(String label, ActionDefAST def, String next) {
		this.label = label;
		this.def = def;
		this.next = next;
	}

	public AST copy() {
		return new ActionAST(label, (ActionDefAST) def.copy(), next);
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		def.substitute(from, to);
	}

	public void generate() {
		Action action = def.getAction();
		AnalysisGraph.activeGraph.addAction(label, action, next);
	}
	
	public String label() {
		return this.label;
	}
	
	public String next() {
		return this.next;
	}
	
	public ActionDefAST def() {
		return this.def;
	}
}