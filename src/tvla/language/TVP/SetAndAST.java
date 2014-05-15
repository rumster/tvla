package tvla.language.TVP;

import java.util.Set;

public class SetAndAST extends SetAST {
	SetAST left;
	SetAST right;

	public SetAndAST(SetAST left, SetAST right) {
		this.left = left;
		this.right = right;
	}

	public Set<PredicateAST> getMembers() {
		Set<PredicateAST> leftMembers = left.getMembers();
		Set<PredicateAST> rightMembers = right.getMembers();
		leftMembers.retainAll(rightMembers);

		return leftMembers;
	}

	public SetAndAST copy() {
		return new SetAndAST(left.copy(), right.copy());
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		left.substitute(from, to);
		right.substitute(from, to);
	}

	public String toString() {
		StringBuffer result = new StringBuffer();

		result.append(left.toString());
		result.append(" & ");
		result.append(right.toString());

		return result.toString();
	}
}
