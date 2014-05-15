package tvla.language.TVP;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import tvla.exceptions.SemanticErrorException;
import tvla.util.HashMapFactory;
import tvla.util.StringUtils;

/** An abstract syntax node for an action macro (an action instantiation).
 * @author Tal Lev-Ami.
 */
public class ActionMacroAST extends MacroAST {
	protected String name;
	protected List<String> args;
	protected ActionDefAST def;
	protected static Map<String,ActionMacroAST> actionMacros = HashMapFactory.make();

	public static void reset() {
		actionMacros = HashMapFactory.make();
	}
	
	public ActionMacroAST(String name, List<String> args, ActionDefAST def) {
		this.name = name;
		this.args = args;
		this.def = def;
		def.myMacro = this;
		actionMacros.put(name, this);
	}

	public static ActionMacroAST get(String name) {
		ActionMacroAST macro = (ActionMacroAST) actionMacros.get(name);
		if (macro == null)
			throw new SemanticErrorException("Unknown macro " + name);
		return macro;
	}

	public ActionDefAST expand(List<String> actualArgs) {
		if (actualArgs.size() != args.size()) {
			throw new SemanticErrorException(
				"For action "
					+ name
					+ " need "
					+ args.size()
					+ " args, but got "
					+ actualArgs.size());
		}
		try {
		ActionDefAST newDef = (ActionDefAST) def.copy();
		newDef.myMacro = this;
		for (int i = 0; i < args.size(); i++) {
			newDef.substitute(args.get(i), actualArgs.get(i));
		}
		newDef.evaluate();
		return newDef;
		}
		catch (SemanticErrorException e) {
			e.append("while expanding the macro " + toString());
			throw e;
		}
	}

	/**
	* @return actionMacro arguments
	*/
	public List<String> getArgs() {
		return args;
	}

	/**
	 * @return actionMacro definition
	 */
	public ActionDefAST getDef() {
		return def;
	}

	/**
	 * @return actionMacro name
	 */
	public String getName() {
		return name;
	}

	/**
	 * does this actionMacro allocate new individuals (via new or clone)
	 * @return true when actionMacro allocates new individuals
	 */
	public boolean isAllocating() {
		return def.isAllocating();
	}
	/**
	 * does this actionMacro have no effect?
	 * @return true when actionMacro has no effect.
	 */
	public boolean isSkipActionMacro() {
		return def.isSkipActionDef();
	}

	/**
	 * return a human-readable representation of the ActionMacroAST
	 * @return actionMacroAST as string
	 */
	public String toString() {
		StringBuffer result = new StringBuffer();
		String separator = "";

		result.append("%action ");
		result.append(name);
		result.append("(");
		if (!args.isEmpty()) {
			for (Iterator<String> it = args.iterator(); it.hasNext();) {
				result.append(separator);
				result.append(it.next().toString());
				separator = ",";
			}
		}
		result.append(")");
		result.append(" {\n");
		result.append(def.toString());
		result.append(" }");

		return result.toString();
	}

	static public int allocated() {
		return MemLogger2.allocated;
	}
	
	private MemLogger2 logger = new MemLogger2();
}

class MemLogger2 {
	static int allocated = 0;
	public MemLogger2() {
		allocated++;
	}
}

