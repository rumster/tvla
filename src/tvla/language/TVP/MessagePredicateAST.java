package tvla.language.TVP;

public class MessagePredicateAST extends MessageAST {
	PredicateAST pred;
	public MessagePredicateAST(PredicateAST pred) {
		this.pred = pred;
	}

	public AST copy() {
		return new MessagePredicateAST((PredicateAST) pred.copy());
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		pred.substitute(from, to);
	}

	public String getMessage() {
		return pred.generateName();
	}
}
