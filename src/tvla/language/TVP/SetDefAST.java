package tvla.language.TVP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.core.decompose.ParametricSet;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;

/** An abstract-syntax class for a set defined by the user.
 * @author Tal Lev-Ami.
 */
public class SetDefAST extends AST {
	/** Maps set names to the lists of set members.
	 */
	public static Map<String, List<PredicateAST>> allSets = HashMapFactory.make();

	/** A label identifying the set.
	 */
	protected String name;

	/** The strings representing the set members.
	 */
	protected List<PredicateAST> members;

	public static void reset() {
		allSets = HashMapFactory.make();
	}
	
	public SetDefAST(String name, List<PredicateAST> members) {
	    this(name, members, false);
	}

	public SetDefAST(String name, List<PredicateAST> members, boolean parametric) {
		this.name = name;
		this.members = members;

		boolean setExists = allSets.containsKey(name);
		if (setExists) {
			List<PredicateAST> currMembers = allSets.get(name);
			if (currMembers != null)  {
				Set<PredicateAST> newSet = HashSetFactory.make(currMembers);
				newSet.addAll(members);
				members = new ArrayList<PredicateAST>(newSet);
			}
		} 

		allSets.put(name, members);
		if (parametric) {
		    ParametricSet.add(name, PredicateAST.asStrings(members));
		}
	}

    public AST copy() {
		throw new RuntimeException("Can't copy set definitions.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute set definitions.");
	}

	public SetDefAST getSet(String name) {
		return (SetDefAST) allSets.get(name);
	}

	//set_def ::= SET ID:name set_expr:elements
	//				{: RESULT = new SetDefAST(name, new ArrayList(elements.getMembers())) ; :} 
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("%s ");
		result.append(name);
		result.append(' ');

		return result.toString();
	}
}
