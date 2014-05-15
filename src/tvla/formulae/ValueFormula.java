package tvla.formulae;

import java.util.List;

import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.logic.Kleene;
import tvla.util.NoDuplicateLinkedList;

/** A constant kleene value formula.
 * @author Tal Lev-Ami 
 */
final public class ValueFormula extends AtomicFormula {
	public static ValueFormula kleeneTrueFormula = 
		new ValueFormula(Kleene.trueKleene);
	public static ValueFormula kleeneFalseFormula =
		new ValueFormula(Kleene.falseKleene);
	public static ValueFormula kleeneUnknownFormula =
		new ValueFormula(Kleene.unknownKleene);

	private Kleene value;
	
	/** Create a new value formula with the given value. */
	public ValueFormula(Kleene value) {
		super();
		this.value = value; 
	}

	/** Create a copy of the formula */
	public Formula copy() {
		return new ValueFormula(value);
	}

	/** Return the value. */
	public Kleene value() {
		return value;
	}
	
	/** Evaluate the formula on the given structure and assignment. */
	public Kleene eval(TVS s, Assign assign) {
		return value;	
	}

	/** Return a human readable representation of the formula. */
	public String toString() {
		return value.toString();
	}

	/** Calculate and return the free variables for this formula. */
	public List<Var> calcFreeVars() {
		return new NoDuplicateLinkedList<Var>();
	}

	/** Equate the this formula with the given fomula by structure. */
	public boolean equals(Object o) {
		if (!(o instanceof ValueFormula))
			return false;
		ValueFormula other = (ValueFormula) o;
		return this.value.equals(other.value);
	}

	public int hashCode() {
		return value.hashCode();
	}
	
	public int ignoreVarHashCode() {
		return value.hashCode();
	}
	

	/** Calls the specific accept method, based on the type of this formula
	 * (Visitor pattern).
	 * @author Roman Manevich.
	 * @since tvla-2-alpha November 18 2002, Initial creation.
	 */
    @Override
    public <T> T visit(FormulaVisitor<T> visitor) {
		return visitor.accept(this);
	}
}