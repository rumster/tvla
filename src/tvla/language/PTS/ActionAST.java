package tvla.language.PTS;

import tvla.language.TVP.AST;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.AnalysisGraph;

/** An abstract syntax node for actions.
 * @author Tal Lev-Ami.
 */
public class ActionAST extends tvla.language.TVP.ActionAST {

	public ActionAST(String label, tvla.language.TVP.ActionDefAST def, String next) {
		super(label, def, next);
	}
	
	public void generate() {
		Action action = def.getAction();
		
		AnalysisGraph.activeGraph.addAction(label, action, next);
	}
	
	public AST copy() {
		return new ActionAST(label, (ActionDefAST) def.copy(), next);
	}
}