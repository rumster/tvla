package tvla.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * An instrusive hash map - the elements in the hash map are part of the data structure
 * NOTICE: Each entry can reside in at most ONE map at a time
 * @author tla
 */
public class IsvHashMap<K, V, E extends IsvEntry<K, V, E>> implements Iterable<E> {
    
    @SuppressWarnings("unchecked")
    protected IsvEntry[] entries;
    int size;
    float loadFactor;

    int shareCount = 0;
    
    public IsvHashMap() {
        this(1, 0.75f);
    }

    public IsvHashMap(int capacity, float loadFactor) {
        entries = new IsvEntry[capacity];
        this.loadFactor = loadFactor;
        this.size = 0;
    }

    @SuppressWarnings("unchecked")
    public IsvHashMap<K, V, E> copy() {
        IsvHashMap<K, V, E> copy = new IsvHashMap<K, V, E>(this.entries.length, this.loadFactor);
        copy.size = this.size;        
        for (int i = 0; i < copy.entries.length; i++) {
            E copyPrev = null;
            E thisCur = (E) this.entries[i];
            while (thisCur != null) {
                E copyCur = thisCur.copy();
                if (copyPrev == null) {
                    copy.entries[i] = copyCur;
                } else {
                    copyPrev.setNext(copyCur);
                }
                thisCur = thisCur.getNext();
                copyPrev = copyCur;
            }
        }
        return copy;
    }

    public void share() {
        shareCount++;
    }
    
    public void unshare() {
        if (shareCount > 0) {
            shareCount--;
        }
    }
    
    public IsvHashMap<K, V, E> modify() {
        if (shareCount > 0) {
            shareCount--;
            return copy();
        } else {
            return this;
        }
    }
    
    protected int hash(K key) {
        int h = key.hashCode();

        // Spread the bits around a bit ;)
//        h += ~(h << 9);
//        h ^=  (h >>> 14);
//        h +=  (h << 4);
//        h ^=  (h >>> 10);

        // normalize to the range of the array
        return Math.abs(h % entries.length);
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    protected E prevGet = null;
    
    @SuppressWarnings("unchecked")
    public E get(K key) {
        E cur = (E) entries[hash(key)];
        prevGet = null;
        while (cur != null) {
            if (cur.getKey().equals(key)) {
                return cur;
            }
            prevGet = cur;
            cur = cur.getNext();
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public void addAfterGet(E value) {
        assert shareCount == 0;
        boolean needRehash = size > entries.length * loadFactor;
        assert value.getNext() == null : "cannot add: maps may have collided, check code.";
        if (prevGet == null) {
            int pos = hash(value.getKey());
            value.setNext((E) entries[pos]);
            entries[pos] = value;
        } else {
            value.setNext(prevGet.getNext());
            prevGet.setNext(value);
        }
        size++;
        if (needRehash) {
            rehash();
        }
    }

    @SuppressWarnings("unchecked")
    public E add(E value) {
        assert shareCount == 0;
        if (value.getNext() != null) {
            throw new RuntimeException(
                "cannot add: maps may have collided, check code.");
        }
        if (size > entries.length * loadFactor ) {
            rehash();
        }
        K key = value.getKey();
        int pos = hash(key);
        E cur = (E) entries[pos];
        while (cur != null) {
            if (cur.getKey().equals(key)) {
                cur.setValue(value.getValue());
                return cur;
            }
            // Move ahead
            cur = cur.getNext();
        }
        // Not found, add it to the front of the list
        value.setNext((E) entries[pos]);
        entries[pos] = value;
        size++;
        return null;
    }

    @SuppressWarnings("unchecked")
    protected void rehash() {
        IsvEntry[] oldEntries = entries;
        entries = new IsvEntry[entries.length * 2];
        // Go over all old entries and insert to new array
        for (IsvEntry entry : oldEntries) {
            E cur = (E) entry;
            E next = null;
            while (cur != null) {
                next = cur.getNext();
                int pos = hash(cur.getKey());
                cur.setNext((E) entries[pos]);
                entries[pos] = cur;
                cur = next;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public E remove(K key) {
        assert shareCount == 0;

        int pos = hash(key);
        E cur = (E) entries[pos]; // The bean the entry should be in
        E prev = null;
        while (cur != null) {
            if (cur.getKey().equals(key)) {
                if (prev == null) {
                    // This is the first entry, change head pointer
                    entries[pos] = cur.getNext();
                } else {
                    // This is not the first entry, splice it out.
                    prev.setNext(cur.getNext());
                }
                cur.setNext(null);
                size--;
                return cur;
            }
            prev = cur;
            cur = cur.getNext();
        }
        return null;
    }

    /**
     * Notice that no ConcurrentModificationException will be thrown in case of
     * changing the map while iterating. You should NOT try to iterate and change
     * concurrently as it can create very nasty bugs
     */
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            int pos = 0;
            E cur = null;
            E prev = null;
            E result = null;

            @SuppressWarnings("unchecked")
            private void advance() {
                if (cur != null) {
                    prev = cur;
                    cur = cur.getNext();
                    if (cur != null) {
                        result = cur;
                        return;
                    }
                }
                while (pos < entries.length) {
                    cur = (E) entries[pos];
                    prev = null;
                    pos++;
                    if (cur != null) {
                        result = cur;
                        return;
                    }
                }
            }

            public boolean hasNext() {
                // If we don't have a result waiting ask for one
                if (result == null) {
                    advance();
                }
                // If we still don't have a result, we are done
                return result != null;
            }

            public E next() {                
                hasNext(); // Make sure he have a result waiting if there is one
                E res = result;
                result = null; // Nullify the result so a new one will be returned next time
                return res;
            }

            @SuppressWarnings("unchecked")
            public void remove() {
                assert shareCount == 0;

                if (prev == null) {
                    // The first element in the list was removed update the head
                    entries[pos - 1] = cur.getNext();
                } else {
                    // Splice the element out
                    prev.setNext(cur.getNext());
                }                
                // Save the old value so we can nullify its next and move to the next element
                IsvEntry save = cur;
                if (cur.getNext() == null) {
                    // Original implementation is fine
                    advance();
                } else {
                    result = cur = cur.getNext();
                    // Prev should stay put.
                }
                save.setNext(null);
                size--;
            }

        };
    }


    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IsvHashMap)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        
        IsvHashMap<K,V,E> other = (IsvHashMap<K,V,E>)obj;

        if (this.size != other.size) {
            return false;
        }
        if (this.size == 0) {
            return true;
        }
        Set<E> thisEntries = new HashSet<E>();
        Iterator<E> iter = this.iterator();
        while (iter.hasNext()) {
            E value = iter.next();
            thisEntries.add(value);
        }

        iter = other.iterator();
        while (iter.hasNext()) {
            E value = iter.next();
            if (!thisEntries.remove(value)) {
                return false;
            }
        }

        return thisEntries.size() == 0;
    }

    public int size() {
        return size;
    }

    public String toString() {
        StringBuffer result = new StringBuffer("{");
        String sep = "";
        for (Map.Entry<K, V> entry : this) {
            result.append(sep).append("(").append(entry.getKey()).append(",").append(entry.getValue()).append(")");
            sep = ",";
        }
        result.append("}");
        return result.toString();
    }
}
