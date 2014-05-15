package tvla.language.TVP;

public class MessageStringAST extends MessageAST {
	private final String str;
	
	public MessageStringAST(String str) {
		this.str = str;
	}

	@Override
	public AST copy() {
		// No need to copy;
		return this;
	}

	@Override
	public void substitute(PredicateAST from, PredicateAST to) {
		// Do nothing.
	}

	@Override
	public String getMessage() {
		return str;
	}
	
	@Override
	public String toString() {
		return str;
	}
}
