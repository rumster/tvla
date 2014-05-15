package tvla.core.base.concrete;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.logic.Kleene;
import tvla.util.Filter;
import tvla.util.FilterIterator;

public final class ConcreteBinaryPredicate extends ConcretePredicate {
	/** a map from node tuples to the unknown and true values of the predicate.
	 */

	public Bitmap bitmap;
	public LinkedHashMap<NodeTuple, Kleene> values;
	//public Map values;
	static public int arity = 2;
	
	class Bitmap {
		private byte[] bitmap;
		private int maxNode;
		private int capacity;
		
		public Bitmap(int maxNode) {
			this.capacity = (maxNode*maxNode / 4) + 1;
			this.bitmap = new byte [capacity];
			this.maxNode = maxNode;
		}
		
		public Bitmap(Bitmap other) {
			this.capacity = other.capacity;
			this.maxNode = other.maxNode;
			this.bitmap = new byte [capacity];
			System.arraycopy(other.bitmap, 0, this.bitmap, 0, capacity);
		}
		
		public void grow(int maxNode) {
			int newMaxNode = maxNode > 2*this.maxNode ? maxNode : 2*this.maxNode;
			//System.out.println("binary grow to " + newMaxNode);
			Bitmap newBitmap = new Bitmap(newMaxNode);

			for (int i = 0; i < this.maxNode; ++i)
				for (int j = 0; j < this.maxNode; ++j) {
					newBitmap.set(i, j, get(i, j));
				}
			bitmap = newBitmap.bitmap;
			capacity = newBitmap.capacity;
			this.maxNode = newBitmap.maxNode;
		}
		
		public byte get(int i, int j) {
			if (i >= maxNode || j >= maxNode)
				return 0;
			int index = (maxNode * i + j) << 1;
			return (byte)((bitmap[index >> 3] >> (index & 7)) & 3);
		}
		
		public void set(int i, int j, byte val) {
			if (i >= maxNode || j >= maxNode)
				grow((i > j ? i : j)+1);
			int index = (maxNode * i + j) << 1;
			int x = (val ^ (bitmap[index >> 3] >> (index & 7))) & 3;
			bitmap[index >> 3] ^= x << (index & 7);
		}
	};

	/** Create a concrete binary predicate with a null predicate.
	 */
	public ConcreteBinaryPredicate() {
		super();
		//this.values = HashMapFactory.make(0);
		this.bitmap = new Bitmap(4);
		this.values = new LinkedHashMap<NodeTuple, Kleene>(0);
	}
	

	/** Constructs a concrete predicate, which shares its values with
	 * the specified concrete predicate instance.
	 */
	public ConcreteBinaryPredicate(ConcreteBinaryPredicate other) {
		this.isShared = other.isShared = true;		
		this.values = other.values;
		this.bitmap = other.bitmap;
	}

	/** Create a deep copy of the predicate.
	 */
    @Override
	public ConcreteBinaryPredicate copy() {
		//ConcreteBinaryPredicate newPredicate = new ConcreteBinaryPredicate(new Bitmap(this.bitmap), new LinkedHashMap(this.values));
		//return newPredicate;
		return new ConcreteBinaryPredicate(this);
	}

	/** Creates a fresh copy of the predicate's values (copy on write).
	 */
    @Override
	public void modify() {
		if (!isShared)
			return;
		isShared = false;
		//values = HashMapFactory.make(this.values);
		values = new LinkedHashMap<NodeTuple, Kleene>(this.values);
		bitmap = new Bitmap(this.bitmap);
	}

	/** Return an iterator to all the true and unknown values of the predicate. 
	 * The iterator returns Map.Entry with key NodeTuple and value Kleene.
	 */
	
    @Override
	public Iterator<Map.Entry<NodeTuple, Kleene>> iterator() {
		//return new SerialIterator(valuesTrue.entrySet().iterator(), valuesUnknown.entrySet().iterator());
		return values.entrySet().iterator();
	}
	
    /**
     * @param desirvedValue - unknown or true, does not support false.
     * @return iterator over NodeTuples with the desired Kleene value
     */
    @Override
    public Iterator<Map.Entry<NodeTuple, Kleene>> satisfyingTupleIterator(final Node[] partialNodes, final Kleene desiredValue) {
		 if (partialNodes == null)
			 return values.entrySet().iterator();
	     
	     return new FilterIterator<Map.Entry<NodeTuple, Kleene>>(values.entrySet().iterator(), new Filter<Map.Entry<NodeTuple, Kleene>>() {
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
	     });
    }

    @Override
    public Iterator<Map.Entry<NodeTuple, Kleene>> satisfyingTupleIterator(final Node node, final int position, final Kleene desiredValue) {
    	return null;
    }
    
    static class Entry<K,V> implements Map.Entry<K,V> {
    	V value;
    	K key;
    	
    	public Entry(K key, V value) {
    		this.value = value;
    		this.key = key;
    	}
    	
    	final public V setValue(V value){
    		this.value = value;
    		return value;
    	}
    	final public K setKey(K key){
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
    		Entry entry = (Entry)o;
    		return key.equals(entry.key);
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
		Kleene value = (Kleene)values.get(tuple);
		return value == null ? Kleene.falseKleene : value;
		//NodePair pair = (NodePair)tuple;
		//return Kleene.kleene(bitmap.get(pair.get(0).id(), pair.get(1).id()));
	}

	/** Set the value of the predicate for the given node pair.
	 */
    @Override
	public void set(NodeTuple tuple, Kleene value) {
		//NodePair pair = (NodePair)tuple;
		//bitmap.set(pair.get(0).id(), pair.get(1).id(), value.kleene());

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
				//bitmap.set(tuple.get(0).id(),tuple.get(1).id(), (byte)0);
				i.remove();
			}
		}
	}

	/** Remove the node tuple from the predicate with its the associated value.
	 */

}
