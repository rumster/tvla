package tvla.formulae;

/** A formula used to add new nodes to a structure's universe.
 * @author Roman Manevich
 * @since tvla-2-alpha September 4 2001 Initial creation.
 */
public class NewUpdateFormula extends UpdateFormula {
	public Var newVar;
	
	public NewUpdateFormula(Formula formula) {
		super(formula);
	}
    
    public String toString() {
      String set = ((newVar == null) ? "" : newVar.toString()) + " new ";
      String at = super.toString();
      String ret = set + at;
      
      return ret;
    }
}