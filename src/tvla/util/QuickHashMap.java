package tvla.util;

import gnu.trove.*;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.AbstractSet;
import java.util.Iterator; 
import java.util.Arrays;

final public class QuickHashMap extends THashMap {
	static final long serialVersionUID = 1L;
	
    protected static final int DEFAULT_INITIAL_CAPACITY = 3;
	
	public QuickHashMap() {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
	}
	
    public QuickHashMap(TObjectHashingStrategy strategy) {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, strategy);
    }

    /**
     * Creates a new <code>THashMap</code> instance with a prime
     * capacity equal to or greater than <tt>initialCapacity</tt> and
     * with the default load factor.
     *
     * @param initialCapacity an <code>int</code> value
     */
    public QuickHashMap(int initialCapacity) {
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
    public QuickHashMap(int initialCapacity, TObjectHashingStrategy strategy) {
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
    public QuickHashMap(int initialCapacity, float loadFactor) {
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
    public QuickHashMap(int initialCapacity, float loadFactor, TObjectHashingStrategy strategy) {
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
    public QuickHashMap(Map map) {
        this(map.size(), DEFAULT_LOAD_FACTOR);
        putAll(map);
    }

    /**
     * Creates a new <code>THashMap</code> instance which contains the
     * key/value pairs in <tt>map</tt>.
     *
     * @param map a <code>Map</code> value
     * @param strategy used to compute hash codes and to compare objects.
     */
    public QuickHashMap(Map map, TObjectHashingStrategy strategy) {
        this(map.size(), DEFAULT_LOAD_FACTOR, strategy);
        putAll(map);
    }
    
	Entry dummy = Entry.dummy();

	static final class Entry implements Map.Entry {
		Object key;
		Object value;
		int index;
		Entry prev;
		Entry next;
		
		Entry(Object k, Object v, int i) {
			key = k;
			value = v;
			index = i;
		}
		
		static Entry dummy() {
			Entry entry = new Entry(null, null, -1);
			entry.prev = entry.next = entry;
			return entry;
		}
		
		final public Object getValue() {
			return value;
		}
		
		final public Object getKey() {
			return key;
		}
		
		final public void setValue0(Object v) {
			value = v;
		}

		final public Object setValue(Object v) {
			Object prev = value;
			value = v;
			return prev;
		}
		
		final public Object setKey(Object k) {
			Object prev = key;
			key = k;
			return prev;
		}
		
        public boolean equals(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry e1 = this;
                Map.Entry e2 = (Map.Entry) o;
                return (e1.getKey()==null ? e2.getKey()==null : e1.getKey().equals(e2.getKey()))
                    && (e1.getValue()==null ? e2.getValue()==null : e1.getValue().equals(e2.getValue()));
            }
            return false;
        }

        public int hashCode() {
            return (getKey()==null ? 0 : getKey().hashCode()) ^ (getValue()==null ? 0 : getValue().hashCode());
        }

	}
	
    /**
     * @return a shallow clone of this collection
     */
    public QuickHashMap clone() {
    	// Shallow clone is not supported for now. 
        QuickHashMap m = new QuickHashMap((Map)this);
        return m;
    }
    
    public Object put(Object key, Object value) {
        Object previous = null;
        Object oldKey;
        Object[] values = _values;
        Object[] set = _set;
        int index = insertionIndex(key);
        boolean isNewMapping = true;
        if (index < 0) {
            index = -index -1;
            Entry entry = (Entry)values[index]; 
    		entry.prev.next = entry.next;
    		entry.next.prev = entry.prev;
    		previous = entry.value;
            isNewMapping = false;
        }
        Entry newEntry = new Entry(key, value, index);
        oldKey = set[index];
        set[index] = key;
        values[index] = newEntry;
    	dummy.prev.next = newEntry;
    	newEntry.prev = dummy.prev;
    	newEntry.next = dummy;
    	dummy.prev = newEntry;

        if (isNewMapping) {
            postInsertHook(oldKey == FREE);
        }
        return previous;
    }

/*    
    public Object put(Object key, Object value) {
    	Entry newEntry = new Entry(key, value);
    	Object previous = super.put(key, newEntry);
    	if (previous != null) {
    		Entry entry = (Entry)previous;
    		entry.prev.next = entry.next;
    		entry.next.prev = entry.prev;
    		previous = entry.value;
    	}
    	dummy.next.prev = newEntry;
    	newEntry.prev = dummy;
    	newEntry.next = dummy.next;
    	dummy.next = newEntry;
    	return previous;
    }
*/    
    /**
     * Executes <tt>procedure</tt> for each value in the map.
     *
     * @param procedure a <code>TObjectProcedure</code> value
     * @return false if the loop over the values terminated because
     * the procedure returned false for some value.
     */
    public boolean forEachValue(TObjectProcedure procedure) {
        Object[] values = _values;
        Object[] set = _set;
        for (int i = values.length; i-- > 0;) {
            if (set[i] != FREE
                && set[i] != REMOVED
                && ! procedure.execute(((Entry)values[i]).value)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Executes <tt>procedure</tt> for each key/value entry in the
     * map.
     *
     * @param procedure a <code>TObjectObjectProcedure</code> value
     * @return false if the loop over the entries terminated because
     * the procedure returned false for some entry.
     */
    public boolean forEachEntry(TObjectObjectProcedure procedure) {
        Object[] keys = _set;
        Object[] values = _values;
        for (int i = keys.length; i-- > 0;) {
            if (keys[i] != FREE
                && keys[i] != REMOVED
                && ! procedure.execute(keys[i],((Entry)values[i]).value)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Retains only those entries in the map for which the procedure
     * returns a true value.
     *
     * @param procedure determines which entries to keep
     * @return true if the map was modified.
     */
    public boolean retainEntries(TObjectObjectProcedure procedure) {
        boolean modified = false;
        Object[] keys = _set;
        Object[] values = _values;
        for (int i = keys.length; i-- > 0;) {
            if (keys[i] != FREE
                && keys[i] != REMOVED
                && ! procedure.execute(keys[i],((Entry)values[i]).value)) {
                removeAt(i);
                modified = true;
            }
        }
        return modified;
    }
    
    /**
     * Transform the values in this map using <tt>function</tt>.
     *
     * @param function a <code>TObjectFunction</code> value
     */
    public void transformValues(TObjectFunction function) {
        Object[] values = _values;
        Object[] set = _set;
        for (int i = values.length; i-- > 0;) {
            if (set[i] != FREE && set[i] != REMOVED) {
            	Entry entry = (Entry)values[i];
                entry.value = function.execute(entry.value);
            }
        }
    }
    
    /**
     * rehashes the map to the new capacity.
     *
     * @param newCapacity an <code>int</code> value
     */
    protected void rehash(int newCapacity) {
        int oldCapacity = _set.length;
        Object oldKeys[] = _set;
        Object oldVals[] = _values;

        Object[] _set = new Object[newCapacity];
        Arrays.fill(_set, FREE);
        Object[] _values = new Object[newCapacity];
        this._values = _values;
        this._set = _set;

        for (int i = oldCapacity; i-- > 0;) {
            if(oldKeys[i] != FREE && oldKeys[i] != REMOVED) {
                Object o = oldKeys[i];
                int index = insertionIndex(o);
                if (index < 0) {
                    throwObjectContractViolation(_set[(-index -1)], o);
                }
                _set[index] = o;
                ((Entry)oldVals[i]).index = index;
                _values[index] = oldVals[i];
            }
        }
    }
    
    /**
     * Empties the map.
     *
     */
    public void clear() {
        if (size() == 0) return; // optimization

        super.clear();
        Arrays.fill(_set, FREE);
        Arrays.fill(_values, null);
        dummy = Entry.dummy();
    }



    /**
     * retrieves the value for <tt>key</tt>
     *
     * @param key an <code>Object</code> value
     * @return the value of <tt>key</tt> or null if no such mapping exists.
     */
    public Object get(Object key) {
        int index = index(key);
        return index < 0 ? null : ((Entry)_values[index]).value;
    }

    /**
     * Deletes a key/value pair from the map.
     *
     * @param key an <code>Object</code> value
     * @return an <code>Object</code> value
     */
    public Object remove(Object key) {
        Object prev = null;
        int index = index(key);
        if (index >= 0) {
            prev = ((Entry)_values[index]).value;
            removeAt(index);    // clear key,state; adjust size
        }
        return prev;
    }
    
    protected void removeAt(int index) {
        Entry entry = (Entry)_values[index];
		entry.prev.next = entry.next;
		entry.next.prev = entry.prev;
        _values[index] = null;
        super.removeAt(index);  // clear key, state; adjust size
    }

    /**
     * Returns a view on the values of the map.
     *
     * @return a <code>Collection</code> value
     */
    public Collection values() {
        return new ValueView();
    }
    
    /**
     * Returns a Set view on the entries of the map.
     *
     * @return a <code>Set</code> value
     */
    public Set<Map.Entry> entrySet() {
        return new EntryView();
    }
    
    /**
     * returns a Set view on the keys of the map.
     *
     * @return a <code>Set</code> value
     */
    public Set keySet() {
        return new KeyView();
    }


    /**
     * checks for the presence of <tt>val</tt> in the values of the map.
     *
     * @param val an <code>Object</code> value
     * @return a <code>boolean</code> value
     */
    public boolean containsValue(Object val) {
        Object[] set = _set;
        Object[] vals = _values;

        // special case null values so that we don't have to
        // perform null checks before every call to equals()
        if (null == val) {
            for (int i = vals.length; i-- > 0;) {
                if ((set[i] != FREE && set[i] != REMOVED) &&
                    val == ((Entry)vals[i]).value) {
                    return true;
                }
            }
        } else {
            for (int i = vals.length; i-- > 0;) {
            	Entry entry = (Entry)vals[i];
                if ((set[i] != FREE && set[i] != REMOVED) &&
                    (val == entry.value || val.equals(entry.value))) {
                    return true;
                }
            }
        } // end of else
        return false;
    }
    
    /**
     * a view onto the values of the map.
     *
     */
    
    protected int index(Object obj) {
        int hash, probe, index, length;
        Object[] set;
        Object cur;

        set = _set;
        length = set.length;
        hash = _hashingStrategy.computeHashCode(obj) & 0x7fffffff;
        index = hash % length;
        cur = set[index];

        if (cur != FREE
            && (cur == REMOVED || ! strategyEquals(cur, obj))) {
            probe = 1 + ((hash*9 & 0x7FFFFFFF) % (length - 2));

            do {
                index -= probe;
                if (index < 0) {
                    index += length;
                }
                cur = set[index];
            } while (cur != FREE
                     && (cur == REMOVED || ! strategyEquals(cur, obj)));
        }

        return cur == FREE ? -1 : index;
    }

    public final boolean strategyEquals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    protected int insertionIndex(Object obj) {
        int hash, probe, index, length;
        Object[] set;
        Object cur;

        set = _set;
        length = set.length;
        hash = _hashingStrategy.computeHashCode(obj) & 0x7fffffff;
        index = hash % length;
        cur = set[index];

        if (cur == FREE) {
            return index;       // empty, all done
        } else if (_hashingStrategy.equals(cur, obj)) {
            return -index -1;   // already stored
        } else {                // already FULL or REMOVED, must probe
            // compute the double hash
            probe = 1 + ((hash*9 & 0x7FFFFFFF) % (length - 2));

            // if the slot we landed on is FULL (but not removed), probe
            // until we find an empty slot, a REMOVED slot, or an element
            // equal to the one we are trying to insert.
            // finding an empty slot means that the value is not present
            // and that we should use that slot as the insertion point;
            // finding a REMOVED slot means that we need to keep searching,
            // however we want to remember the offset of that REMOVED slot
            // so we can reuse it in case a "new" insertion (i.e. not an update)
            // is possible.
            // finding a matching value means that we've found that our desired
            // key is already in the table
            if (cur != REMOVED) {
                // starting at the natural offset, probe until we find an
                // offset that isn't full.
                do {
                    index -= probe;
                    if (index < 0) {
                        index += length;
                    }
                    cur = set[index];
                } while (cur != FREE
                         && cur != REMOVED
                         && ! _hashingStrategy.equals(cur, obj));
            }

            // if the index we found was removed: continue probing until we
            // locate a free location or an element which equal()s the
            // one we have.
            if (cur == REMOVED) {
                int firstRemoved = index;
                while (cur != FREE
                       && (cur == REMOVED || ! _hashingStrategy.equals(cur, obj))) {
                    index -= probe;
                    if (index < 0) {
                        index += length;
                    }
                    cur = set[index];
                }
                // NOTE: cur cannot == REMOVED in this block
                return (cur != FREE) ? -index -1 : firstRemoved;
            }
            // if it's full, the key is already stored
            // NOTE: cur cannot equal REMOVE here (would have retuned already (see above)
            return (cur != FREE) ? -index -1 : index;
        }
    }

    /**
     * a view onto the entries of the map.
     *
     */
    
    protected class ListIterator implements Iterator {
    	private Entry current;
    	private Entry result;
    	protected QuickHashMap instance;
    	
    	ListIterator(QuickHashMap instance) {
    		this.instance = instance;
    		reset();
    	}

    	final public boolean hasNext() {
    		return current.key != null;
    	}

    	public void remove() {
   			if (result == null) {
   				throw new ArrayIndexOutOfBoundsException();
   			}
    		instance.removeAt(result.index);
    	}	
    		
    	final public Object next() {
   			if (!hasNext()) {
   				throw new ArrayIndexOutOfBoundsException();
   			}
    		result = current;
    		current = result.next;
    		return operation(result);
    	}		    

    	final public void reset() {
    		current = dummy.next;
    	}
    	
    	protected Object operation(Entry entry) {
    		return entry;
    	}
    }
    
    /**
     * a view onto the keys of the map.
     */
    protected class KeyView extends MapBackedView {
        public Iterator iterator() {
            return new ListIterator(QuickHashMap.this) {
            	protected Object operation(Entry e) {
            		return e.key;
            	}
            };
        }

        public boolean removeElement(Object key) {
            return null != QuickHashMap.this.remove(key);
        }

        public boolean containsElement(Object key) {
            return QuickHashMap.this.contains(key);
        }
    }

    protected class ValueView extends MapBackedView {
        public Iterator iterator() {
            return new ListIterator(QuickHashMap.this) {
                protected Object operation(Entry entry) {
                    return entry.value;
                }
            };
        }

        public boolean containsElement(Object value) {
            return containsValue(value);
        }

        public boolean removeElement(Object value) {
            Object[] values = _values;
            Object[] set = _set;
            
            for (int i = values.length; i-- > 0;) {
            	Entry entry = (Entry)values[i];
                if ((set[i] != FREE && set[i] != REMOVED) &&
                    value == entry.value ||
                    (null != entry.value && entry.value.equals(value))) {
                    removeAt(i);
                    return true;
                }
            }
            return false;
        }
    }

    protected class EntryView extends MapBackedView<Map.Entry> {

        public Iterator<Map.Entry> iterator() {
            return new ListIterator(QuickHashMap.this);
        }

        public boolean removeElement(Map.Entry entry) {
            // Note that the deletion is only legal if
            // both the key and the value match.
            Object val;
            int index;

            Object key = keyForEntry(entry);
            index = index(key);
            assert(index == ((Entry)entry).index);
            if (index >= 0) {
                val = valueForEntry(entry);
                if (val == ((Entry)_values[index]).value ||
                    (null != val && val.equals(((Entry)_values[index]).value))) {
                    removeAt(index);    // clear key,state; adjust size
                    return true;
                }
            }
            return false;
        }

        public boolean containsElement(Map.Entry entry) {
            Object val = get(keyForEntry(entry));
            Object entryValue = entry.getValue();
            return entryValue == val ||
                (null != val && val.equals(entryValue));
        }

        protected Object valueForEntry(Map.Entry entry) {
            return entry.getValue();
        }

        protected Object keyForEntry(Map.Entry entry) {
            return entry.getKey();
        }
    }
    
    final static private Object DEFAULT = new Object();

    private abstract class MapBackedView<E> extends AbstractSet<E>
        implements Set<E> {
        
        public abstract Iterator<E> iterator();

        public abstract boolean removeElement(E key);

        public abstract boolean containsElement(E key);

        public boolean contains(Object key) {
            return containsElement((E) key);
        }

        public boolean remove(Object o) {
            return removeElement((E) o);
        }

        public boolean containsAll(Collection<?> collection) {
            for (Iterator i = collection.iterator(); i.hasNext();) {
                if (! contains(i.next())) {
                    return false;
                }
            }
            return true;
        }

        public void clear() {
            QuickHashMap.this.clear();
        }

        
        public boolean add(E obj) {
        	return put(obj, DEFAULT) != DEFAULT;
            //throw new UnsupportedOperationException();
        }
        
        public boolean addAll(Collection<? extends E> collection) {
        	boolean result = true;
        	for (Iterator<?> it = collection.iterator(); it.hasNext();) {
        		result = result && add((E)it.next());
        	}
        	return result;
            //throw new UnsupportedOperationException();
        }

        public int size() {
            return QuickHashMap.this.size();
        }

        public Object[] toArray() {
            Object[] result = new Object[size()];
            Iterator e = iterator();
            for (int i=0; e.hasNext(); i++)
                result[i] = e.next();
            return result;
        }

          public <T> T[] toArray(T[] a) {
            int size = size();
            if (a.length < size)
                a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);

            Iterator<E> it = iterator();
            Object[] result = a;
            for (int i=0; i<size; i++) {
                result[i] = it.next();
            }

            if (a.length > size) {
                a[size] = null;
            }

            return a;
        }

        public boolean isEmpty() {
            return QuickHashMap.this.isEmpty();
        }


        public boolean retainAll(Collection<?> collection) {
            boolean changed = false;
            Iterator i = iterator();
            while (i.hasNext()) {
                if (! collection.contains(i.next())) {
                    i.remove();
                    changed = true;
                }
            }
            return changed;
        }
    }
    
    public void ensureCapacity(int desiredCapacity) {
        if (desiredCapacity > (_maxSize - size())) {
            rehash(PrimeFinder.nextPrime((int)(desiredCapacity + size() /
                                                        _loadFactor + 1) + 1));
            computeMaxSize(capacity());
        }
    }
    
    public void compact() {
        // need at least one free spot for open addressing
        rehash(PrimeFinder.nextPrime((int)(size()/_loadFactor + 1) + 1));
        computeMaxSize(capacity());
    }
    
    private final void computeMaxSize(int capacity) {
        // need at least one free slot for open addressing
    	int s1 = capacity - 1;
    	int s2 = (int)(capacity * _loadFactor);
        _maxSize = s1 <= s2? s1 : s2;
        _free = capacity - _size; // reset the free element count
    }
}
