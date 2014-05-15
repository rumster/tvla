package tvla.core.functional;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.Vector;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.common.NodePair;
import tvla.predicates.Predicate;
import tvla.core.generic.PredicateNode;
import tvla.logic.Kleene;
import tvla.util.Filter;
import tvla.util.FilterIterator;
import tvla.util.EmptyIterator;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.SerialIterator;
import tvla.core.base.concrete.ConcretePredicate;

/** An arbitrary arity predicate interpretation.
 * @see tvla.predicates.Predicate
 * @author Roman Manevich
 */

public final class ConcreteKAryPredicateFlik extends ConcretePredicate {
	/** a map from node tuples to the unknown and true values of the predicate.
	 */
	public Map values;
	public int arity;
	private IntKleeneMap flik;
	private int numberSatisfy;
	private FnUniverse U;

	/** Create a concrete binary predicate with a null predicate.
	 */
	public ConcreteKAryPredicateFlik(int arity) {
		super();
		//this.values = HashMapFactory.make(0);
		values = null;
		flik = PackedIntKleeneMap.zero;
		//list = null;
		//this.values = new LinkedHashMap(1, (float)0.5);
		//this.values = new QuickHashMap();
		this.arity = arity;
		this.numberSatisfy = 0;
	}
	
	public void setUniverse(FnUniverse U) {
		this.U = U;
	}

	/** Constructs a concrete predicate, which shares its values with
	 * the specified concrete predicate instance.
	 */
	public ConcreteKAryPredicateFlik(ConcreteKAryPredicateFlik other) {
		if (other.values != null) {
			this.isShared = other.isShared = true;		
		}
		this.values = other.values;
		this.flik = other.flik;
		this.arity = other.arity;
		this.numberSatisfy = other.numberSatisfy;
	}

	/** Create a deep copy of the predicate.
	 */
	public ConcretePredicate copy() {
		//ConcreteKAryPredicate newPredicate = new ConcreteKAryPredicate(HashMapFactory.make(this.values), copyIndexes());
		//ConcreteKAryPredicate newPredicate = new ConcreteKAryPredicate(new LinkedHashMap(this.values), null, arity);
		return new ConcreteKAryPredicateFlik(this);
	}
	
	public void modify() {
		if (!isShared || values == null)
			return;
		isShared = false;
		values = new LinkedHashMap(this.values);
	}
	
	/** Return an iterator to all the true and unknown values of the predicate. 
	 * The iterator returns Map.Entry with key NodeTuple and value Kleene.
	 */
	
	//Entry list;
	
	public Iterator iterator() {
		//return flik.iterator();
		if (values != null) {
			return values.entrySet().iterator();
		}
		else {
			values = new LinkedHashMap(1, (float)0.5);
			switch (arity) {
			case 1:
				for (Iterator<Node> it = U.iterator(); it.hasNext(); ) {
					Node n = it.next();
					Kleene val = flik.lookup(id(n));
					if (val.kleene() != 0) {
						values.put(n, val);
					}
				}
				break;
			case 2:
				for (Iterator<Node> it1 = U.iterator(); it1.hasNext(); ) {
					Node n1 = it1.next();
					for (Iterator<Node> it2 = U.iterator(); it2.hasNext(); ) {
						Node n2 = it2.next();
						Kleene val = getFlik(n1, n2);
						if (val.kleene() != 0) {
							values.put(new NodePair(n1,n2), val);
						}
					}
				}
				break;
			}
			return values.entrySet().iterator();
		}
	}
	
	public void pack() {
		isShared = false;
		values = null;
	}
	
    /**
     * @param desirvedValue - unknown or true, does not support false.
     * @return iterator over NodeTuples with the desired Kleene value
     */
    public Iterator satisfyingTupleIterator(final Node[] partialNodes, final Kleene desiredValue) {
		 if (partialNodes == null)
			 return iterator();
	     
	     return new FilterIterator(iterator(), new Filter() {
	       public boolean accepts(Object o) {
	    	   Map.Entry entry = (Map.Entry)o;
	           Kleene tupleValue = (Kleene)entry.getValue(); 
	           if ((tupleValue == desiredValue) || (desiredValue == null)) {
			       NodeTuple nt = (NodeTuple)entry.getKey();
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
	     });
    }

    class Entry implements Map.Entry {
    	Object value = null;
    	Object key = null;
    	Entry next = null;
    	
    	public Entry() {}
    	
    	final public Object setValue(Object value){
    		this.value = value;
    		return value;
    	}
    	final public Object setKey(Object key){
    		this.key = key;
    		return key;
    	}
    	final public Object getValue() {
    		return value;
    	}
    	final public Object getKey() {
    		return key;
    	}
    	public Iterator iterator() {
    		return new EntryIterator(this);
    	}
    };
    
    class EntryIterator implements Iterator {
    	Entry head;

    	public EntryIterator (Entry l) { head = l; }

    	public boolean hasNext() { return head != null; }

    	public Object next() {
    		Object result = head;
    		head = head.next;
    		return result;
    	}
    	
    	public void remove() {
    	   throw new UnsupportedOperationException("NodelistIterator::remove");
    	}
    }

	/** Return the number of assignments satisfying the predicate.
	 */
	public int numberSatisfy() {
		return numberSatisfy;
		//return values.size();
	}

	private static int id(Node n) { return n.id(); }

	// The implementation of binary predicates requires an encoding from
	// pairs of nodes into integers: Node * Node -> int
	protected static int encode(Node first, Node second) {
		return encodeIntPair ( id(first), id(second) );
	}

	protected static int encodeIntPair(int n1, int n2) {
		// diagonal encoding:
		//    0 1 3 6 ...
		//    2 4 7 ...
		//    5 8 ...
		//    9 ...
		//    ...
		int diagonal = n1 + n2; 
		// must check for possible overflow and throw new Error("Node numbers are too big");
		int result = ( (diagonal * (diagonal+1))/2 ) + n1;
		return result;
	}
	
	private final int hash(Object tuple) {
		switch (arity) {
		case 1:
			return ((Node)tuple).id();
		case 2:
			NodePair pair = (NodePair)tuple;
			return encode(pair.first(), pair.second());
		}
		return 0;
	}
	
	public Kleene get(Object tuple) {
		if (values != null) {
			Kleene value = (Kleene) values.get(tuple);
			return value == null ? Kleene.falseKleene : value;
		}
		else {
			switch (arity) {
			case 1:
				return flik.lookup(((Node)tuple).id());
			case 2:
				NodePair pair = (NodePair)tuple;
				return flik.lookup(encodeIntPair(pair.first().id(), pair.second().id()));
			default:
				return null;
			}
		}
	}
	
	/** Return the value of the predicate for the given node pair.
	 */
	public final Kleene getFlik(Object tuple) {
		return flik.lookup(hash(tuple));
	}

	public final Kleene getFlik(Node n) {
		return flik.lookup(id(n));
	}

	public final Kleene getFlik(Node n1, Node n2) {
		return flik.lookup(encode(n1, n2));
	}
	
	/** Set the value of the predicate for the given node pair.
	 */
	public void set(Object tuple, Kleene value) {
		int hash = hash(tuple);
		Kleene previous = flik.lookup(hash);
		if (previous != value) {
			flik = flik.update(hash, value);
			if (values != null) {
				if (value == Kleene.falseKleene) {
					values.remove(tuple);
				}
				else {
					values.put(tuple, value);
				}
				numberSatisfy = values.size();
			}
			else {
				if (previous == Kleene.falseKleene)
					++numberSatisfy;
				else if (value == Kleene.falseKleene)
					--numberSatisfy;
			}
		}
	}
	/*
	public void getIntersection(Predicate p, ConcreteKAryPredicateFlik other, Map results) {
		if (values == other.values)
			return;
		
		for (Iterator it2 = values.entrySet().iterator(); it2.hasNext();) {
			Map.Entry entry = (Map.Entry)it2.next();
			NodeTuple tuple = (NodeTuple)entry.getKey();
			Kleene val = (Kleene)entry.getValue();
			if (other.values.get(tuple) == val)
					continue;
			else results.put(new PredicateNode(p, tuple), val);
		}

		for (Iterator it2 = other.iterator(); it2.hasNext();) {
			Map.Entry entry = (Map.Entry)it2.next();
			NodeTuple tuple = (NodeTuple)entry.getKey();
			if (values.containsKey(tuple))
				continue;
		    results.put(new PredicateNode(p, tuple), Kleene.falseKleene);
		}
	}
	*/

	/** Remove the node from the predicate with all the associated values.
	 */
	public void removeNode(Node node) {
		for (Iterator i = iterator(); i.hasNext(); ) {
			Map.Entry entry = (Map.Entry)i.next();
			NodeTuple tuple = (NodeTuple)entry.getKey();
			if (tuple.contains(node)) {
				i.remove();
				flik = flik.update(hash(tuple), Kleene.falseKleene);
				--numberSatisfy;
			}
		}
	}
}
