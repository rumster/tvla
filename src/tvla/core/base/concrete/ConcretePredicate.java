package tvla.core.base.concrete;

import java.util.Iterator;
import java.util.Map;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.logic.Kleene;

/**
 * An abstract base class for predicate interpretations.
 * 
 * @see tvla.predicates.Predicate
 * @author Tal Lev-Ami
 */
public abstract class ConcretePredicate implements Iterable<Map.Entry<NodeTuple, Kleene>>{
    /**
     * A flag indicatin whether the set of bindings is shared by another
     * concrete predicate instance (used for copy on write).
     */
    protected boolean isShared;

    /**
     * Create a concrete with a null predicate
     */
    public ConcretePredicate() {
        isShared = false;
    }

    public ConcretePredicate copy() {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the predicate's interpretation for the specified tuple.
     */
    public Kleene get(NodeTuple tuple) {
        throw new UnsupportedOperationException();
    }

    /**
     * Assigns a new interpretation to the specified tuple.
     */
    public void set(NodeTuple tuple, Kleene val) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns true when the predicate evaluates all tuples to false.
     */
    public final boolean isAllFalse() {
        return numberSatisfy() == 0;
    }

    /**
     * Creates a fresh copy of the predicate's values (copy on write).
     */
    public void modify() {
    }

    /**
     * Clear all the true and unknown values of the predicate.
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Return an iterator to all the true and unknown values of the predicate.
     */
    public Iterator<Map.Entry<NodeTuple, Kleene>> iterator() {
        throw new UnsupportedOperationException();
    }

    /**
     * Return an iterator over NodeTuples with the desired value of the
     * predicate
     * 
     * @param partialNodes
     */
    public Iterator<Map.Entry<NodeTuple, Kleene>> satisfyingTupleIterator(Node[] partialNodes,
            Kleene desiredValue) {
        throw new UnsupportedOperationException();
    }

    public Iterator<Map.Entry<NodeTuple, Kleene>> satisfyingTupleIterator(Node node, int position,
            Kleene desiredValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove the node from the predicate with all the associated values.
     */
    public void removeNode(Node n) {
        throw new UnsupportedOperationException();
    }

    /**
     * Return the number of assignments satisfying the predicate.
     */
    public int numberSatisfy() {
        throw new UnsupportedOperationException();
    }

    public void pack() {

    }

    protected static class Entry<K, V> implements Map.Entry<K, V> {
        V value;

        K key;

        public Entry() {
        }

        public Entry(K key, V value) {
            this.value = value;
            this.key = key;
        }

        final public V setValue(V value) {
            this.value = value;
            return value;
        }

        final public K setKey(K key) {
            this.key = key;
            return key;
        }

        final public V getValue() {
            return value;
        }

        final public K getKey() {
            return key;
        }

        public int hashCode() {
            return key.hashCode();
        }

        public boolean equals(Object o) {
            Map.Entry entry = (Map.Entry) o;
            return key.equals(entry.getKey());
        }
    }

    public void removeNodes(Iterable<Node> toRemove) {
        for (Node node : toRemove) {
            removeNode(node);
        }       
    };
}