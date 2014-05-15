package tvla.core.base.concrete;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.generic.PredicateNode;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.EmptyIterator;
import tvla.util.Filter;
import tvla.util.FilterIterator;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;

/** An arbitrary arity predicate interpretation.
 * @see tvla.predicates.Predicate
 * @author Roman Manevich
 */

public final class ConcreteKAryPredicateNew extends ConcretePredicate {
    /** a map from node tuples to the unknown and true values of the predicate.
     */
    public Map<NodeTuple, Kleene> values;
    public int arity;

    /** Create a concrete binary predicate with a null predicate.
     */
    @SuppressWarnings("unchecked")
    public ConcreteKAryPredicateNew(int arity) {
        super();
        this.values = HashMapFactory.make(0);
        //this.values = new LinkedHashMap<NodeTuple, Kleene>(1, (float)0.5);
        //this.values = new QuickHashMap();
        this.arity = arity;
    }

    /** Constructs a concrete predicate, which shares its values with
     * the specified concrete predicate instance.
     */
    public ConcreteKAryPredicateNew(ConcreteKAryPredicateNew other) {
        this.isShared = other.isShared = true;      
        this.values = other.values;
        this.arity = other.arity;
    }

    /** Create a deep copy of the predicate.
     */
    @Override
    public ConcreteKAryPredicateNew copy() {
        return new ConcreteKAryPredicateNew(this);
    }

    /** Creates a fresh copy of the predicate's values (copy on write).
     */
    @Override
    public void modify() {
        if (!isShared)
            return;
        isShared = false;
        values = HashMapFactory.make(this.values);
    }

    /** Return an iterator to all the true and unknown values of the predicate. 
     * The iterator returns Map.Entry with key NodeTuple and value Kleene.
     */
    
    @Override
    public Iterator<Map.Entry<NodeTuple, Kleene>> iterator() {
        //return new SerialIterator(valuesTrue.entrySet().iterator(), valuesUnknown.entrySet().iterator());
        return values.entrySet().iterator();
    }
    
    public Iterator<Map.Entry<NodeTuple, Kleene>> iterator(Node[] partialNodes, Kleene desiredValue) {
        //return new SerialIterator(valuesTrue.entrySet().iterator(), valuesUnknown.entrySet().iterator());
        //return values.entrySet().iterator();
         if (partialNodes == null)
             return values.entrySet().iterator();
         
         return new FilterIterator<Map.Entry<NodeTuple, Kleene>>(values.entrySet().iterator(), 
                 new PredicateFilter(partialNodes, desiredValue));
    }

    /**
     * @param desirvedValue - unknown or true, does not support false.
     * @return iterator over NodeTuples with the desired Kleene value
     */
    @Override
    public Iterator<Map.Entry<NodeTuple, Kleene>> satisfyingTupleIterator(Node[] partialNodes, Kleene desiredValue) {
         if (partialNodes == null)
             return values.entrySet().iterator();
         
         return new FilterIterator<Map.Entry<NodeTuple, Kleene>>(values.entrySet().iterator(), 
                 new PredicateFilter(partialNodes, desiredValue));
    }
    
    final class PredicateFilter implements Filter<Map.Entry<NodeTuple, Kleene>> {
        private final Node[] partialNodes;
        private final Kleene desiredValue;
        public PredicateFilter(Node[] _partialNodes, Kleene _desiredValue) {
            partialNodes = _partialNodes;
            desiredValue = _desiredValue;
        }
        public boolean accepts(Map.Entry<NodeTuple, Kleene> entry) {
            Kleene tupleValue = entry.getValue(); 
            if ((tupleValue == desiredValue) || (desiredValue == null)) {
                NodeTuple nt = entry.getKey();
                // make sure tuple matches the partial assignment, otherwise its invalid
                for (int i=0; i < partialNodes.length; i++) {
                    if (partialNodes[i] != null) {
                        if (!partialNodes[i].equals(nt.get(i)))
                            return false;
                    }
                }
                return true;
            }
            return false;
        }
    };

    /** Return the number of assignments satisfying the predicate.
     */
    @Override
    public int numberSatisfy() {
        return values.size();
    }

    /** Return the value of the predicate for the given node pair.
     */
    @Override
    public Kleene get(NodeTuple tuple) {
        Kleene value = values.get(tuple);
        return value == null ? Kleene.falseKleene : value;
    }

    /** Set the value of the predicate for the given node pair.
     */
    @Override
    public void set(NodeTuple tuple, Kleene value) {
        if (value == Kleene.falseKleene) {
            values.remove(tuple);
            return;
        }

        values.put(tuple, value);
    }
    
    /** Remove the node from the predicate with all the associated values.
     */
    @Override
    public void removeNode(Node node) {
        for (Iterator<NodeTuple> i = values.keySet().iterator(); i.hasNext(); ) {
            NodeTuple tuple = i.next();
            if (tuple.contains(node)) {
                i.remove();
            }
        }
    }

}
