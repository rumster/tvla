package tvla.core.base.concrete;

import java.util.Iterator;
import java.util.Map;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.logic.Kleene;
import tvla.util.Filter;
import tvla.util.FilterIterator;
import tvla.util.IsvHashMap;

/** An arbitrary arity predicate interpretation.
 * @see tvla.predicates.Predicate
 * @author Roman Manevich
 */

public final class ConcreteKAryPredicate extends ConcretePredicate {
    /** a map from node tuples to the unknown and true values of the predicate.
     */
    public IsvHashMap<NodeTuple, Kleene, NodeTupleKleene> values;
    public int arity;
//    ConcreteKAryPredicateNew check;
    
    /** Create a concrete binary predicate with a null predicate.
     */
    @SuppressWarnings("unchecked")
    public ConcreteKAryPredicate(int arity) {
        super();
        this.values = new IsvHashMap<NodeTuple, Kleene, NodeTupleKleene>();
        this.arity = arity;
//        this.check = new ConcreteKAryPredicateNew(arity);
    }

    /** Constructs a concrete predicate, which shares its values with
     * the specified concrete predicate instance.
     */
    public ConcreteKAryPredicate(ConcreteKAryPredicate other) {
//        this.values = other.values.copy();
        this.values = other.values;
        this.values.share();
        this.arity = other.arity;
//        this.check = new ConcreteKAryPredicateNew(other.check);
    }

    /** Create a deep copy of the predicate.
     */
    @Override
    public ConcreteKAryPredicate copy() {
        return new ConcreteKAryPredicate(this);
    }

    /** Creates a fresh copy of the predicate's values (copy on write).
     */
    @Override
    public void modify() {
        values = this.values.modify();
//        check.modify();
    }

    /** Return an iterator to all the true and unknown values of the predicate. 
     * The iterator returns Map.Entry with key NodeTuple and value Kleene.
     */
    
    @SuppressWarnings("unchecked")
    @Override
    public Iterator<Map.Entry<NodeTuple, Kleene>> iterator() {
        return (Iterator) values.iterator();
//        return check.iterator();
    }
    
    /**
     * @param desirvedValue - unknown or true, does not support false.
     * @return iterator over NodeTuples with the desired Kleene value
     */
    @Override
    @SuppressWarnings("unchecked")
    public Iterator<Map.Entry<NodeTuple, Kleene>> satisfyingTupleIterator(Node[] partialNodes, Kleene desiredValue) {
//        return check.satisfyingTupleIterator(partialNodes, desiredValue);
        if (partialNodes == null)
             return (Iterator) values.iterator();
         
         return new FilterIterator<Map.Entry<NodeTuple, Kleene>>((Iterator) values.iterator(), 
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
//        assert check.numberSatisfy() == values.size();
        return values.size();
    }

    /** Return the value of the predicate for the given node pair.
     */
    @Override
    public Kleene get(NodeTuple tuple) {
        NodeTupleKleene value = values.get(tuple);
        Kleene result = value == null ? Kleene.falseKleene : value.getValue();
//        assert result == check.get(tuple);
        return result;
    }

    /** Set the value of the predicate for the given node pair.
     */
    @Override
    public void set(NodeTuple tuple, Kleene value) {
//        check.set(tuple, value);
        if (value == Kleene.falseKleene) {
            values.remove(tuple);
//            assert check.get(tuple) == get(tuple);
            return;
        }
        NodeTupleKleene entry = values.get(tuple);
        if (entry == null) {
            values.addAfterGet(NodeTupleKleene.Factory.createTuple(tuple, value));
        } else { 
            entry.setValue(value);
        }
//        assert check.get(tuple) == get(tuple);
    }

    /** Remove the node from the predicate with all the associated values.
     */
    @Override
    public void removeNode(Node node) {
    	if (arity == 1) {
    		values.remove(node);
    		return;
    	}
    	
        for (Iterator<NodeTupleKleene> i = values.iterator(); i.hasNext(); ) {
            NodeTuple tuple = i.next().getKey();
            if (tuple.contains(node)) {
                i.remove();
            }
        }
    }

    /** Remove the node from the predicate with all the associated values.
     */
    @Override
    public void removeNodes(Iterable<Node> toRemove) {
        if (arity == 1) {
            for (Node node : toRemove) {
                values.remove(node);
            }
            return;
        }
        
        TUPLE: for (Iterator<NodeTupleKleene> i = values.iterator(); i.hasNext(); ) {
            NodeTuple tuple = i.next().getKey();
            for (Node node : toRemove) {
                if (tuple.contains(node)) {
                    i.remove();
                    continue TUPLE;
                }
            }
        }
    }

    
    public boolean wasModified(ConcreteKAryPredicate orig) {
        return this.values != orig.values;
    }
}
