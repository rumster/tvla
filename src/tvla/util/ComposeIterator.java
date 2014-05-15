package tvla.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ComposeIterator<T> extends SimpleIterator<List<T>> {
    protected List<T> current;
    protected List<Iterator<T>> iterators = new ArrayList<Iterator<T>>();
    protected List<Iterable<T>> iterables = new ArrayList<Iterable<T>>();
    protected boolean done;
    
    public ComposeIterator(Iterable<? extends Iterable<T>> basis) {
        current = new ArrayList<T>();
        for (Iterable<T> iterable : basis) {
            iterables.add(iterable);
            Iterator<T> iterator = iterable.iterator();
            iterators.add(iterator);
            if (!iterator.hasNext()) {
                done = true;
                return;
            }
            current.add(iterator.next());
        }
        result = current;
    }

    @Override
    protected List<T> advance() {
        if (done) {
            return null;
        }
        int i;
        for (i = 0; i < current.size(); i++) {
            Iterator<T> iterator = iterators.get(i);
            if (iterator.hasNext()) {
                T next = iterator.next();
                current.set(i, next);
                return current;
            } else {
                iterator = iterables.get(i).iterator();
                assert iterator.hasNext();
                iterators.set(i, iterator);
                T next = iterator.next();
                current.set(i, next);
            }
        }
        done = true;
        return null;
    }

}
