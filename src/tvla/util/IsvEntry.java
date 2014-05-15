package tvla.util;

import java.util.Map;

/**
 * Interface for entries that can be stored in the IsvHashMap
 * @author tla
 */
public interface IsvEntry<K, V, E> extends Map.Entry<K, V> {
    void setNext(E next);
    E getNext();
    
    E copy();
}
