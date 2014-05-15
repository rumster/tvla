package tvla.language.TVP;

import java.util.Iterator;
import java.util.Set;

public class SetPlusAST extends SetAST {
	private SetAST left;
	private SetAST right;

	public SetPlusAST(SetAST left, SetAST right) {
		this.left = left;
		this.right = right;
	}

	public Set<PredicateAST> getMembers() {
		Set<PredicateAST> leftMembers = left.getMembers();
		Set<PredicateAST> rightMembers = right.getMembers();
		leftMembers.addAll(rightMembers);
		return leftMembers;
	}

	public SetPlusAST copy() {
		return new SetPlusAST((SetAST) left.copy(), (SetAST) right.copy());
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		left.substitute(from, to);
		right.substitute(from, to);
	}

	public String toString() {
		StringBuffer result = new StringBuffer();

		result.append(left.toString());
		result.append(" + ");
		result.append(right.toString());
		return result.toString();
	}

}