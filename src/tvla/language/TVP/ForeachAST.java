package tvla.language.TVP;

import java.util.ArrayList;
import java.util.List;

/** An abstract syntax node of a 'foreach' statement.
 * @author Tal Lev-Ami.
 */
public class ForeachAST extends AST {
	String id;
	SetAST set;
	List<? extends AST> asts;

	public ForeachAST(String id, SetAST set, List asts) {
		this.id = id;
		this.set = set;
		this.asts = asts;
	}

	public List evaluate() {
		List result = new ArrayList();
		for (PredicateAST member : set.getMembers()) {
			for (AST ast : asts) {
			    ast = ast.copy();
				ast.substitute(id, member);
				if (ast instanceof ForeachAST) {
					result.addAll(((ForeachAST) ast).evaluate());
				} 
				else {
					result.add(ast);
				}
			}
		}
		return result;
	}

	public AST copy() {
		return new ForeachAST(id, (SetAST) set.copy(), copyList(asts));
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		if (from.equals(id))
			throw new RuntimeException("Trying to substitute the variable of a foreach (" +
				id + ") in " + set);
		set.substitute(from, to);
		substituteList(asts, from, to);
	}
}