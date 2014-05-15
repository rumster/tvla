package tvla.util;



/** This class represents a pair of objects.
 */
public class Pair<F,S> {
	public F first;
	public S second;
	
    public static <F,S> Pair<F, S> create(F first, S second) {
        return new Pair<F,S>(first, second);
    }

    /** Constructs a pair from two objects.
	 * 
	 * @param first The first object in the pair.
	 * @param second The second object in the pair.
	 */
	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}

    public Pair(Pair<F,S> other) {
        this.first = other.first;
        this.second = other.second;
    }
	
	/** Use the specified objects to update this pair.
	 * 
	 * @param first The updated first object.
	 * @param second The updated second object.
	 */
	public void set(F first, S second) {
		this.first = first;
		this.second = second;
	}
	

	@Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Pair))
            return false;
        Pair<?,?> other = (Pair<?,?>) obj;
        if (first == null) {
            if (other.first != null)
                return false;
        } else if (!first.equals(other.first))
            return false;
        if (second == null) {
            if (other.second != null)
                return false;
        } else if (!second.equals(other.second))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((first == null) ? 0 : first.hashCode());
        result = prime * result + ((second == null) ? 0 : second.hashCode());
        return result;
    }
	
	public String toString() {
		return "(" + first + "," + second + ")";
	}
}
