package tvla.util;

import java.util.Iterator;

public class ConcatIterator<T> implements Iterator<T> {
    private final Iterator<T> first;
    private final Iterator<T> second;
    private boolean inFirst = true;

    public ConcatIterator(Iterator<T> first, Iterator<T> second) {
        this.first = first;
        this.second = second;
    }
    
    public boolean hasNext() {
        return first.hasNext() || second.hasNext();
    }

    public T next() {
        if (first.hasNext()) {
            return first.next();
        }
        inFirst = false;
        if (second.hasNext()) {
            return second.next();
        }
        throw new RuntimeException("Should have called hasNext first");
    }

    public void remove() {
        if (inFirst)
            first.remove();
        else
            second.remove();
    }

}
