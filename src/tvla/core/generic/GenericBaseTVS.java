package tvla.core.generic;

import java.util.Collection;

import tvla.core.TVS;
import tvla.core.base.BaseTVS;
import tvla.formulae.Formula;
import tvla.predicates.DynamicVocabulary;

/** An implementation of the high-level TVS interface based on NaiveTVS
 * and that uses generic algortihms.
 */
public class GenericBaseTVS extends BaseTVS {
	/** Constructs and initializes an empty NaiveHighLevelTVS.
	 */
	public GenericBaseTVS() {
		super();
	}
	
	/** Conversion constructor.
	 * @author Roman Manevich.
	 * @since 16.9.2001 Initial creation.
	 */
	public GenericBaseTVS(TVS other) {
		super(other);
	}

	public GenericBaseTVS(DynamicVocabulary vocabulary) {
	    super(vocabulary);
	}
	
	/** Returns a copy of this structure.
	 * @author Roman Manevich.
	 */
	public GenericBaseTVS copy() {
		GenericBaseTVS newValue = new GenericBaseTVS(this);
		return newValue;
	}

	/** Bounds the structure in-place.
	 */
	public void blur() {
		GenericBlur.defaultGenericBlur.blur(this);
	}

	/** Applies a constraint-solver to this structure.
	 */
	public boolean coerce() {
		return GenericCoerce.defaultGenericCoerce.coerce(this);
	}
	
	/** Applies the focus algorithm with the specified formula.
	 * @param focusFormula A Formula object.
	 * @return The collection of focused structures.
	 */
	public Collection focus(Formula focusFormula) {
		return GenericFocus.defaultGenericFocus.focus(this, focusFormula);
	}
}