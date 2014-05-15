package tvla.language.TVM;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;

public class AtomicBlockAST extends AST {

	/** a list of ActionASTs */
	private List actions;

	public AtomicBlockAST(List blockActions) {
		Set sources = new HashSet();
		this.actions = blockActions;
		int size = actions.size();
		// get sourcs
		for (int actionIt = 0; actionIt < size; actionIt++) {
			ActionAST action = (ActionAST) actions.get(actionIt);
			sources.add(action.label());
		}
		// any action with target inside the block should set unschedule to
		// false actions with target outside the block should remain as
		// unschedule=true (default)
		for (int actionIt = 0; actionIt < size; actionIt++) {
			ActionAST action = (ActionAST) actions.get(actionIt);
			String target = action.next();
			if (sources.contains(target)) {
				action.performUnschedule(false);
			}
		}
		// actions = blockActions;
		// int size = actions.size();
		// for (int actionIt = 0; actionIt < size - 1; actionIt++) {
		// ActionAST action = (ActionAST)actions.get(actionIt);
		// action.performUnschedule(false);
		// }
	}

	public List getActions() {
		return actions;
	}

	public AST copy() {
		throw new RuntimeException("Can't copy atomic blocks.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute atomic blocks.");
	}
}
