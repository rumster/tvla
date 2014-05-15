package tvla.language.TVP;

public class MessageCompositeAST extends MessageAST {
	MessageAST left;
	MessageAST right;

	public MessageCompositeAST(MessageAST left, MessageAST right) {
		this.left = left;
		this.right = right;
	}

	public AST copy() {
		return new MessageCompositeAST((MessageAST) left.copy(), 
									   (MessageAST) right.copy());
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		left.substitute(from, to);
		right.substitute(from, to);
	}

	public String getMessage() {
		return left.getMessage() + right.getMessage();
	}
	
	@Override
	public String toString() {
		return left.getMessage() + right.getMessage();
	}
}