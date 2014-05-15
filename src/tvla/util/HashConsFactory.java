package tvla.util;

import java.util.*;

/**
 * This class supports fast operations for classes via hash-consing and
 * memoization of operations.
 * 
 * @author Roman
 * @author tla
 * 
 */
public abstract class HashConsFactory<T, K> {

    /**
     * The hash-cons map where each element is stored uniquely.
     */
    protected Map<K, T> unique = new HashMap<K, T>(100);

    public T create(K key) {
        assert key != null;

        T existing = unique.get(key);
        if (existing == null) {
            existing = actualCreate(key);
            unique.put(key, existing);
        }
        return existing;

    }

    protected abstract T actualCreate(K key);

    public abstract class BinOp<R> {
        protected Map<Object, R> memoization = new WeakHashMap<Object, R>(100);

        protected T create(K key) {
            return HashConsFactory.this.create(key);
        }

        protected Object createKey(T left, T right) {
            return new OrderedPair<T>(left, right);
        }

        protected abstract R actualApply(T left, T right);

        /**
         * Returns the union of left and right.
         */
        public R apply(T left, T right) {
            assert left != null && right != null;
            Object key = createKey(left, right);
            R result = memoization.get(key);
            if (result == null) {
                result = actualApply(left, right);
                memoization.put(key, result);
                return result;
            } else {
                return result;
            }
        }
    }

    public abstract class CommBinOp<R> extends BinOp<R> {
        protected Object createKey(T left, T right) {
            return new UnorderedPair<T>(left, right);
        }
    }

    /**
     * An ordered pair of hash-consed elements.
     */
    protected static class OrderedPair<T> {
        private T element1;

        private T element2;

        private int hashCode;

        /**
         * We assume that element1 and element2 have already been hash-consed.
         * 
         * @param set1
         *            A hash-consed element
         * @param set2
         *            A hash-consed element
         */
        public OrderedPair(T element1, T element2) {
            this.element1 = element1;
            this.element2 = element2;
            this.hashCode = System.identityHashCode(element1) + System.identityHashCode(element2);
        }

        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof OrderedPair)) {
                return false;
            }
            OrderedPair<?> otherPair = (OrderedPair<?>) other;
            return element1 == otherPair.element1 && element2 == otherPair.element2;
        }
    }

    /**
     * An unordered pair of hash-consed elements.
     */
    private static class UnorderedPair<T> {
        private T element1;

        private T element2;

        private int hashCode1;

        private int hashCode2;

        public UnorderedPair(T element1, T element2) {
            this.element1 = element1;
            this.element2 = element2;
            this.hashCode1 = System.identityHashCode(element1);
            this.hashCode2 = System.identityHashCode(element2);
            if (hashCode2 < hashCode1) {
                T tmpT = element1;
                int hashCodeTmp = hashCode1;
                element1 = element2;
                element2 = tmpT;
                hashCode1 = hashCode2;
                hashCode2 = hashCodeTmp;
            }
        }

        public int hashCode() {
            return hashCode1 + hashCode2;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof UnorderedPair)) {
                return false;
            }
            UnorderedPair<?> otherPair = (UnorderedPair<?>) other;
            if (hashCode1 != otherPair.hashCode1 || hashCode2 != otherPair.hashCode2)
                return false;
            if (element1 == otherPair.element1) {
                return element2 == otherPair.element2;
            } else if (element2 == otherPair.element1) {
                return element1 == otherPair.element2;
            } else {
                return false;
            }
        }
    }
}