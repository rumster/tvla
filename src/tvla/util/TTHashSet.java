package tvla.util;

import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;

import java.util.Collection;

class TTHashSet extends THashSet {
	static final long serialVersionUID = 2L;
	
    //protected static final int DEFAULT_INITIAL_CAPACITY = 3;
	
	public TTHashSet() {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
	}
	
    public TTHashSet(TObjectHashingStrategy strategy) {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, strategy);
    }

    /**
     * Creates a new <code>THashMap</code> instance with a prime
     * capacity equal to or greater than <tt>initialCapacity</tt> and
     * with the default load factor.
     *
     * @param initialCapacity an <code>int</code> value
     */
    public TTHashSet(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a new <code>THashMap</code> instance with a prime
     * capacity equal to or greater than <tt>initialCapacity</tt> and
     * with the default load factor.
     *
     * @param initialCapacity an <code>int</code> value
     * @param strategy used to compute hash codes and to compare objects.
     */
    public TTHashSet(int initialCapacity, TObjectHashingStrategy strategy) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, strategy);
    }

    /**
     * Creates a new <code>THashMap</code> instance with a prime
     * capacity equal to or greater than <tt>initialCapacity</tt> and
     * with the specified load factor.
     *
     * @param initialCapacity an <code>int</code> value
     * @param loadFactor a <code>float</code> value
     */
    public TTHashSet(int initialCapacity, float loadFactor) {
        _loadFactor = loadFactor;
        setUp((int)(initialCapacity / loadFactor ));
        this._hashingStrategy = this;
    }

    /**
     * Creates a new <code>THashMap</code> instance with a prime
     * capacity equal to or greater than <tt>initialCapacity</tt> and
     * with the specified load factor.
     *
     * @param initialCapacity an <code>int</code> value
     * @param loadFactor a <code>float</code> value
     * @param strategy used to compute hash codes and to compare objects.
     */
    public TTHashSet(int initialCapacity, float loadFactor, TObjectHashingStrategy strategy) {
        _loadFactor = loadFactor;
        setUp((int)(initialCapacity / loadFactor ));
        this._hashingStrategy = strategy;
    }

    /**
     * Creates a new <code>THashMap</code> instance which contains the
     * key/value pairs in <tt>map</tt>.
     *
     * @param map a <code>Map</code> value
     */
    public TTHashSet(Collection col) {
        this(col.size(), DEFAULT_LOAD_FACTOR);
        addAll(col);
    }

    /**
     * Creates a new <code>THashMap</code> instance which contains the
     * key/value pairs in <tt>map</tt>.
     *
     * @param map a <code>Map</code> value
     * @param strategy used to compute hash codes and to compare objects.
     */
    public TTHashSet(Collection col, TObjectHashingStrategy strategy) {
        this(col.size(), DEFAULT_LOAD_FACTOR, strategy);
        addAll(col);
    }
};
