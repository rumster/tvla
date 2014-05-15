package tvla.language.TVP;

import java.util.ArrayList;
import java.util.List;

/** The base class for all abstract syntax tree nodes.
 * @author Tal Lev-Ami.
 */
public abstract class AST {
	MemLogger logger = new MemLogger();

	public abstract AST copy();

	public abstract void substitute(PredicateAST from, PredicateAST to);

	public void substitute(String from, PredicateAST to) {
	    substitute(PredicateAST.getPredicateAST(from), to);
	}

	public void substitute(String from, String to) {
        substitute(PredicateAST.getPredicateAST(from), PredicateAST.getPredicateAST(to));
    }

	public void generate() {
	}

	public static List copyList(List orig) {
		List<AST> result = new ArrayList<AST>();
		for (AST ast : (List<AST>) orig) {
			result.add(ast.copy());
		}
		return result;
	}

	public static <T extends AST> void substituteList(List<T> orig, PredicateAST from, PredicateAST to) {
		for (T ast : orig) {
			ast.substitute(from, to);
		}
	}

	static public int allocated() {
		return MemLogger.allocated;
	}
}

class MemLogger {
	static int allocated = 0;

	public MemLogger() {
		allocated++;
	}
}