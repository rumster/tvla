package tvla.formulae;

/** A formula used to remove nodes from a structure's universe.
 * @author Roman Manevich.
 * @since 4.9.2001 Initial creation.
 */
public class RetainUpdateFormula extends UpdateFormula {
	public Var retainVar;
	
	public RetainUpdateFormula(Formula formula) {
		super(formula);
	}
}