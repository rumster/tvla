package tvla.util;

import java.util.Iterator;


public abstract class SimpleIterator<T> implements Iterator<T> {
    T result = null;
    
    protected abstract T advance();
    
    public boolean hasNext() {
        if (result == null) {
            result = advance();
        }
        return result != null;
    }
    
    public T next() {
        if (result == null) {
            result = advance();
        }
        T retVal = result;
        result = null;
        return retVal;
    }
    
    public void remove() {
        throw new UnsupportedOperationException("No remove");
    }        
}
