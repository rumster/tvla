package tvla.language.TVP;

import java.util.Collection;
import java.util.Set;

import tvla.util.HashSetFactory;

/** An abstract syntax node for sets containing user-specified elements.
 *  @author Tal Lev-Ami.
 */
public class SetConstantAST extends SetAST {
	Set<PredicateAST> ids;
	
	public SetConstantAST(Collection<PredicateAST> ids) {
		this.ids = HashSetFactory.make(ids);
	}

	public Set<PredicateAST> getMembers() {
		return HashSetFactory.make(ids);
	}

	public SetConstantAST copy() {
		return new SetConstantAST(ids);
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		if (ids.remove(from))
			ids.add(to);
	}
	
	public String toString() {
	    StringBuilder result = new StringBuilder();
	    result.append("{");
	    String sep = "";
	    for (Object id : ids) {
	        result.append(sep).append(id);
	        sep = ",";
	    }
	    result.append("}");
	    return result.toString();
	}
}
