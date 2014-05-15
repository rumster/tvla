package tvla.language.TVP;

public class MacroAST extends AST {
	public AST copy() {
		throw new RuntimeException("Can't copy macro definitions.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute macro definitions.");
	}
}
