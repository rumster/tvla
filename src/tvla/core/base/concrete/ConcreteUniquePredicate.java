package tvla.core.base.concrete;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;

import tvla.logic.Kleene;
import tvla.util.EmptyIterator;
import tvla.core.Node;
import tvla.core.NodeTuple;

final public class ConcreteUniquePredicate extends ConcretePredicate {

	Node node;
	Kleene value;
	Set<Entry> entryset;
	
	public ConcreteUniquePredicate() {
		super();
		node = null;
		value = Kleene.falseKleene;
		entryset = Collections.singleton(new Entry());
	}
	
	public ConcreteUniquePredicate(ConcreteUniquePredicate other) {
		super();
		node = other.node;
		value = other.value;
		entryset = other.entryset;
	}
	
    @Override
	public void modify() {
		if (!isShared)
			return;
		isShared = false;
		entryset = Collections.singleton(new Entry());
	}

    @Override
	public ConcreteUniquePredicate copy() {
		return new ConcreteUniquePredicate(this);
	}

	/**
	 * Returns the Kleene value associated with this prediacte.
	 */
    @Override
	public Kleene get(NodeTuple tuple) {
		return node.equals(tuple) ? value : Kleene.falseKleene;
	}
	
    @Override
	public void set(NodeTuple tuple, Kleene val) {
		node = (Node)tuple;
		value = val; 
	}
	
    @Override
	public void removeNode(Node n) {
		if (node.equals(n)) {
			value = Kleene.falseKleene;
		}
	}

    @SuppressWarnings("unchecked")
    @Override
	public Iterator satisfyingTupleIterator(final Node[] partialNodes, final Kleene desiredValue) {
		if (partialNodes == null)
			return iterator();
		Node partialNode = partialNodes[0];
	    if (value == desiredValue && 
	    	(partialNode == null || partialNode.equals(node))) {
	    	return entryset.iterator();
	    } else {
	    	return EmptyIterator.instance();
	    }
	}
	
    @SuppressWarnings("unchecked")
    @Override
	public Iterator iterator() {
	    if (value != Kleene.falseKleene) {
	    	return entryset.iterator();
	    } else {
	    	return EmptyIterator.instance();
	    }
	}

    protected class Entry implements Map.Entry<NodeTuple, Kleene> {
    	final public Kleene setValue(Kleene value){
            ConcreteUniquePredicate.this.value = value;
    		return value;
    	}
    	final public NodeTuple setKey(NodeTuple key){
            ConcreteUniquePredicate.this.node = (Node)key;
    		return key;
    	}
    	final public Kleene getValue() {
    		return ConcreteUniquePredicate.this.value;
    	}
    	final public NodeTuple getKey() {
    		return ConcreteUniquePredicate.this.node;
    	}
    };
    
    @Override
	public int numberSatisfy() {
		return value == Kleene.falseKleene ? 0 : 1;
	}

}
