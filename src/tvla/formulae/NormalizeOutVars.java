package tvla.formulae;

/** For normalizing variables out of all quantifier
 * and transitive subformulas.
 * @author Alexey Loginov.
 * @since June 18 2003, Initial creation.
 */
public class NormalizeOutVars extends FormulaVisitor {
	/** Variables to normalize out.
	 */
	private static Var[] vars;
	
	/** Used to invoke traverse and accept.
	 */
	private static NormalizeOutVars instance = new NormalizeOutVars();

	/** Normalizes all quantifier and transitive formulae with
	 * bound variables in the vs array.  (The bound variables
	 * of such formulae are replaced with fresh variables.)
	 * @param formula A first-order formula.
	 * @param vs A variable array.
	 */
	public static void normalize(Formula formula, Var[] vs) {
	    vars = vs;
	    instance.traverse(formula);
	}	

	/** Normalizes the formula if bound variable is var.
	 */
	public Object accept(ExistQuantFormula quantFormula) {
	    boolean needToNormalize = false;
	    for (int i = 0; !needToNormalize && i < vars.length; i++)
		if (vars[i].equals(quantFormula.boundVariable()))
		    needToNormalize = true;

	    if (needToNormalize) quantFormula.normalize();
	    return null;
	}

	/** Normalizes the formula if bound variable is var.
	 */
	public Object accept(AllQuantFormula quantFormula) {
	    boolean needToNormalize = false;
	    for (int i = 0; !needToNormalize && i < vars.length; i++)
		if (vars[i].equals(quantFormula.boundVariable()))
		    needToNormalize = true;

	    if (needToNormalize) quantFormula.normalize();
	    return null;
	}

	/** Normalizes the formula if subLeft or subRight is var.
	 */
	public Object accept(TransitiveFormula transFormula) {
	    boolean needToNormalize = false;
	    for (int i = 0; !needToNormalize && i < vars.length; i++)
		if (vars[i].equals(transFormula.subLeft()) ||
		    vars[i].equals(transFormula.subRight()))
		    needToNormalize = true;

	    if (needToNormalize) transFormula.normalize();
	    return null;
	}
}
