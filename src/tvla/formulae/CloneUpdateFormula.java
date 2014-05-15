package tvla.formulae;

/** A formula used for cloning a sub-structure.
 * The formula has exactly one free variable used to mark
 * the part of the univer that should be cloned.
 * @author Roman Manevich
 * @since tvla-2-alpha June 1 2002 Initial creation.
 */
public class CloneUpdateFormula extends UpdateFormula {
	public Var var;
	
	public CloneUpdateFormula(Formula formula) {
		super(formula);
	}
}