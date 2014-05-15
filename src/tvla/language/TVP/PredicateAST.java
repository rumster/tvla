package tvla.language.TVP;

import java.util.*;

import tvla.core.decompose.ParametricSet;
import tvla.exceptions.SemanticErrorException;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;

/**
 * @TODO:
 *	-	handle k-ary predicates
 *  -	support functional constraints (syntax defined in manual)
 */

/**
 * An abstract syntax node for predicates.
 * @author Tal Lev-Ami
 */
public class PredicateAST extends AST {
    public int arity = -1;
	protected String name;
	private List<PredicateAST> params;
	
	/**
	 * return a copy of the PredicateAST
	 * @return copy of the PredicateAST
	 */
	public PredicateAST copy() {
		return new PredicateAST(this);
	}

	/**
	 * generate predicate name
	 * @return full predicate name as string
	 */
	public String generateName() {
		return generateName(name, params);
	}

	public boolean equals(Object other) {
	    if (!(other instanceof PredicateAST)) return false;
	    PredicateAST otherP = (PredicateAST) other;
	    return this.name.equals(otherP.name) &&
	        this.params.equals(otherP.params);
	}
	public int hashCode() {
	    return name.hashCode() * 31 + params.hashCode();
	}
	
	/**
	 * substitute string from->to
	 * @param from - source string
	 * @param to - target string
	 */
	public void substitute(PredicateAST from, PredicateAST to) {
	    if (from.equals(this)) {
	        name = to.name;
	        params = new ArrayList<PredicateAST>();
	        for (PredicateAST param : to.params) {
	            params.add(param.copy());
	        }
	        return;
	    }
		if (from.isSimple() && name.equals(from.name)) {
		    assert to.isSimple();
			name = to.name;
		}
		for (int i = 0; i < params.size(); i++) {
			PredicateAST param = params.get(i);
			param.substitute(from, to);
		} 
	}

	public boolean isSimple() {
        return params.size() == 0;
    }

    /**
	 * return the predicate corresponding to this PredicateAST
	 * @return Predicate
	 */
	public Predicate getPredicate() {
		String name = generateName();
		Predicate predicate = Vocabulary.getPredicateByName(name);
		if (predicate == null) {
			throw new SemanticErrorException(
				"Predicate " + name + " was used but not declared.");
		}
		return predicate;
	}

	/**
	 * generate the predicate's full name (expanding predicate parameters)
	 * @param name - predicate name
	 * @param params - predicate parameter list
	 * @return string of full predicate name (with expanded parameters)
	 */
	public static String generateName(String name, List<PredicateAST> params) {
		StringBuffer result = new StringBuffer();
		String sep = "";
		for (PredicateAST param : params) {
			result.append(sep + param.generateName());
			sep = ",";
		}
		return name + (params.isEmpty() ? "" : "[" + result.toString() + "]");
	}

	/**
	 * return a new predicateAST
	 * @param name - predicate name
	 * @param params - predicate parameter list
	 * @return a new predicateAST
	 */
	public static PredicateAST getPredicateAST(String name, List<PredicateAST> params) {
		return new PredicateAST(name, params);
	}

	@SuppressWarnings("unchecked")
    public static PredicateAST getPredicateAST(String name) {
        return new PredicateAST(name, Collections.EMPTY_LIST);
    }

	/**
	 * constructor
	 * @param name - predicate name
	 * @param params - predicate paramaeter list
	 */
	protected PredicateAST(String name, List<PredicateAST> params) {
		this.name = name;
		this.params = params;
	}

	/**
	 * copy constructor
	 * @param other - predicateAST to be copied from
	 */
	protected PredicateAST(PredicateAST other) {
		this.name = other.name;
		this.params = new ArrayList<PredicateAST>();
		for (PredicateAST param : other.params) {
		    this.params.add(param.copy());
		}
		this.arity = other.arity;
	}

	/**
	 * Return a string of parameter name and parameters
	 * not including arguments.
	 * @return predicate name as string
	 */
	public String toNameString() {
		StringBuffer result = new StringBuffer();
		result.append(name);
		if (params != null && !params.isEmpty()) {
			String separator = "";
			result.append("[");
			for (PredicateAST param : params) {
				result.append(separator);
				result.append(param.toNameString());
				separator = ",";
			}
			result.append("]");
		}
		return result.toString();
	}

	/**
	 * human-readable representation of the predicate
	 * @return predicate as string
	 */
	public String toString() {
		return toNameString();
	}

	protected void checkParametric(Predicate predicate) {
        if (ParametricSet.getPSet(name) != null) {
            ParametricSet.addPredicate(name, predicate);
        } else {
            for (PredicateAST param : params) {
                param.checkParametric(predicate);
            }               
        }
    }
	
    /**
     * checks the arity of the given predicate
     * @param arity - assumed arity
     */
    public void checkArity(int arity) {
        if (this.arity == -1) {
            this.arity = arity;
        } else if (this.arity != arity) {
            throw new SemanticErrorException(
                "Error. Using predicate "
                    + name
                    + " which is "
                    + this.arity
                    + "-ary as in "
                    + arity
                    + "-ary context.");
        }
    }

    public static List<String> asStrings(List<PredicateAST> predicates) {
        List<String> flatMembers = new ArrayList<String>();
        for (PredicateAST predicate : predicates) {
            flatMembers.add(predicate.generateName());
        }
        return flatMembers;
    }

    public static List<PredicateAST> asPredicates(List<String> strings) {
        List<PredicateAST> predicates = new ArrayList<PredicateAST>();
        for (String string : strings) {
            predicates.add(getPredicateAST(string));
        }
        return predicates;
    }	
}
