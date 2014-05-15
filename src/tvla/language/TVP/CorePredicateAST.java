package tvla.language.TVP;

import java.util.List;
import java.util.Set;

import tvla.exceptions.SemanticErrorException;
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;

/** An abstract syntax node for core predicates.
 * @author Tal Lev-Ami.
 */
public class CorePredicateAST extends PredicateDefAST {

	protected List<Var> args;

	public CorePredicateAST(
		String name,
		List<PredicateAST> params,
		List<Var> args,
		int arity,
		PredicatePropertiesAST type,
		Set<Kleene> attr) {
		super(name, params, type, attr, arity);
		this.args = args;
	}

	private CorePredicateAST(CorePredicateAST other) {
		super(other);
		this.args = other.args;
	}

	public CorePredicateAST copy() {
		return new CorePredicateAST(this);
	}

	public void generate() {
		try {
			Predicate predicate = Vocabulary.createPredicate(generateName(),
					arity, properties.abstraction());
			generatePredicate(predicate);
			
			checkParametric(predicate);
		} catch (SemanticErrorException e) {
			e.append("while generating core predicate " + toString());
			throw e;
		}
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		String separator = "";
		result.append("%p ");

		// name and params
		result.append(super.toString());

		separator = "";
		result.append("(");
		if (args != null && !args.isEmpty()) {
			for (Var arg : args) {
				result.append(separator);
				result.append(arg);
				separator = ",";
			}
		}
		result.append(") ");
		result.append(properties.toParserString());
		result.append(" ");
		result.append(showAttrToString());

		return result.toString();
	}
}
