package tvla.logic;

/** Propositional Three-Valued Logic with Join and Partial Meet.
 * @author Mooly Sagiv
 * @author Tal Lev-Ami
 */
public class Kleene {
	private final static byte trueVal = 2;
	private final static byte unknownVal = 1;
	private final static byte falseVal = 0;

	/** A true value.
	 */
	public final static Kleene trueKleene = new Kleene(trueVal);    
	
	/** An unknown value.
	 */
	public final static Kleene unknownKleene = new Kleene(unknownVal);    
	
	/** A false value.
	 */
	public final static Kleene falseKleene = new Kleene(falseVal);

	private final static Kleene[] values = { falseKleene, 
											 unknownKleene, 
											 trueKleene };

	private final byte value;

	/** A human readable representation of the kleene value.
	 */
	public String toString() {
		switch(value) {
		case trueVal:
			return "1";
		case unknownVal:
			return "1/2";
		case falseVal:
			return "0";
		default: 
			return "invalid(" + value + ")";
		}
	}

	/** Is left before right in the information order.
	 */
	public static boolean less(Kleene left, Kleene right) {
		if (right.value == unknownVal)
			return true;
		return right.value == left.value;
	}
	
    /** Do left and right agree in information order.
     * @since 2004-01-18
     * @author Gilad Arnold
     */
    public static boolean agree(Kleene left, Kleene right) {
        return (less(left, right) || less(right, left));
    }
    
	/** Return the join of left and right.
	 */
	public static Kleene join(Kleene left, Kleene right) {
		if (left.value==right.value) 
			return left;
		else 
			return unknownKleene;
	}

	/** Return the meet of left and right.
	 */
	public static Kleene meet(Kleene left, Kleene right) {
		if (left.value == right.value)
			return left;
		else if (left.value == unknownVal)
			return right;
		else if (right.value == unknownVal)
			return left;
		else {
			assert false : "Encountered attempt to apply the Kleene meet operator "
				+ "to 0 and 1, for which the result is currently undefined!";
			return null;
		}
	}
	
	/** Return the three valued logical conjunction of left and right.
	 */
	public static Kleene and(Kleene left, Kleene right) {
		if (left.value<=right.value) 
			return left;
		else 
			return right;
	}
	/** Return the three valued logical disjunction of left and right.
	 */
	public static Kleene or(Kleene left, Kleene right) {
		if (left.value <= right.value) 
			return right;
		else 
			return left;
	}

	/** Return the three valued logical negation of arg.
	 */
	public static Kleene not(Kleene arg) {
		return values[2 - arg.value];
	}

	public static Kleene kleene(byte k) {
		return values[k];
	}

	public byte kleene() {
		return value;
	}

	private Kleene(byte val) { 
		value = val;
	}

    public static Kleene meet2(Kleene left, Kleene right) {
        if (left.value == right.value)
            return left;
        else if (left.value == unknownVal)
            return right;
        else if (right.value == unknownVal)
            return left;
        else {
            return null;
        }
   }
}