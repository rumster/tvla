package tvla.language.TVP;

import java.util.Set;

import tvla.exceptions.SemanticErrorException;
import tvla.util.HashSetFactory;

/** @author Tal Lev-Ami.
 */
public class SetUseAST extends SetAST {
	String setName;

	public SetUseAST(String setName) {
		this.setName = setName;
	}

	public Set<PredicateAST> getMembers() {
		if (!SetDefAST.allSets.containsKey(setName))
			throw new SemanticErrorException("Unknown set " + setName);
		return HashSetFactory.make(SetDefAST.allSets.get(setName));
	}

	public SetUseAST copy() {
		return new SetUseAST(setName);
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		if (from.isSimple() && from.equals(setName)) {
		    assert to.isSimple();
			setName = to.name;
		}
	}

	public String toString() {
		return setName;
	}
}
