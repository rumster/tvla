package tvla.util;

import java.util.Iterator;

public class ApplyIterable<T> implements Iterable<T> {
    protected final Iterable<T> base;
    protected final Apply<T> applicator;

    public ApplyIterable(Iterable<T> base, Apply<T> applicator) {
        this.base = base;
        this.applicator = applicator;        
    }
    
    public Iterator<T> iterator() {
        return new SimpleIterator<T>() {
            Iterator<T> iterator = base.iterator();
            @Override
            protected T advance() {
                T result = null;
                while (result == null && iterator.hasNext()) {
                    result = iterator.next();
                    result = applicator.apply(result);
                }
                return result;
            }
        };
    }

}
