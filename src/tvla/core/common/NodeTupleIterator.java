package tvla.core.common;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.logic.Kleene;
import tvla.util.SingleIterator;

/** An iterator over k-tuples of nodes.
 * @author Roman Manevich
 * @since tvla-2-alpha (May 16 2002) Initial creation.
 * @TODO optimize this implementation.
 */
public final class NodeTupleIterator implements Iterator<NodeTuple> {
    private final Node[] fixed;
	private final Node[] nodes;
	private int[] counters;
	private Node[] currentTuple;
	private final int numberOfTuples;
	private int tupleCounter;

	/** Creates an iterator for enumerating all k-tuples of nodes (arity specifies k)
	 * from a specified collection of nodes. 
	 * 
	 * @param nodes A collection of nodes to choose from.
	 * @param arity Specifies how many nodes will exist in each tuple. 
	 * @return An iterator over NodeTuple objects.
	 */
	public static Iterator<? extends NodeTuple> createIterator(Collection<Node> nodes, int arity) {
	    return createIterator(nodes, new Node[arity]);
    }        
    
    public static Iterator<? extends NodeTuple> createIterator(Collection<Node> nodes, Node[] fixed) {
        int unknown = 0;
        for (Node node : fixed) {
            if (node == null) {
                unknown++;
            }
        }
        if (unknown == fixed.length) {
            switch (unknown) {
    			case 0 :
    				return Collections.singleton(NodeTuple.EMPTY_TUPLE).iterator();
    			case 1 :
    				return nodes.iterator();
    			case 2:
    				if (nodes.size() == 0)
    					return Collections.EMPTY_LIST.iterator();
    				else
    					return new NodePairIterator(nodes);
    			default :
    				if (nodes.size() == 0)
    					return Collections.EMPTY_LIST.iterator();
    				else
    					return new NodeTupleIterator(nodes, fixed, unknown);
    		}
        } else if (unknown == 0) {
            NodeTuple tuple = NodeTuple.createTuple(fixed);
            return new SingleIterator<NodeTuple>(tuple);
        } else {
            return new NodeTupleIterator(nodes, fixed, unknown);            
        }
	}

	private NodeTupleIterator(Collection<Node> nodes, Node[] fixed, int unknown) {
		this.fixed = fixed;
        
        int size = nodes.size();
		Node[] _nodes = new Node[size];
		int i = 0;
		for (Iterator<Node> nodeIter = nodes.iterator(); nodeIter.hasNext(); ++i) {
			_nodes[i] = nodeIter.next();
		}

		this.nodes = _nodes;
		counters = new int[unknown];
		currentTuple = new Node[fixed.length];

		for (i = 0; i < currentTuple.length; i++) {
		    currentTuple[i] = fixed[i];
        }
        
		switch (unknown) {
		case 0:
			numberOfTuples = 1;
			break;
		case 1:
			numberOfTuples = size;
			break;
		case 2:
			numberOfTuples = size * size;
			break;
		default:
			numberOfTuples = (int) Math.pow(nodes.size(), unknown);
		}
	}

	public boolean hasNext() {
		return tupleCounter < numberOfTuples;
		//return counters != null;
	}

	public NodeTuple next() {
		if (currentTuple == null)
			throw new NoSuchElementException();
		findNext();
        NodeTuple answer = NodeTuple.createTuple(currentTuple);
		return answer;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	private void findNext() {
		int length = nodes.length;
		int[] counters = this.counters;
		int counter = 0;
		int i; 
		
		for (i = 0; i < counters.length; ++i) {
			counter = (counters[i] + 1) % length;
			counters[i] = counter;
			//System.out.println("Incrementing index " + i + " with " + counter);
			if (counter != 0)
				break;
		}

		// initialize current tuple array
		final Node[] nodes = this.nodes; 
		Node[] currentTuple = this.currentTuple;
        i = 0;
		for (int j = currentTuple.length; j-- != 0;) {
            if (fixed[j] == null) {
                currentTuple[j] = nodes[counters[i++]];
            }
		}
		++tupleCounter;

		//if (i == arity && counter == 0)  // no next
		//	counters = null;
	}

    public static Iterator<Entry<NodeTuple, Kleene>> createIterator(final Collection<Node> nodes,
            final int arity, final Kleene value) {
        return new Iterator<Entry<NodeTuple,Kleene>>() {
            Iterator<? extends NodeTuple> nodeTupleIterator = NodeTupleIterator.createIterator(nodes, arity);
            NodeTuple tuple = null;
            Map.Entry<NodeTuple, Kleene> result = new Map.Entry<NodeTuple, Kleene>() {
                public Kleene setValue(Kleene value) {
                    throw new UnsupportedOperationException();
                }
            
                public Kleene getValue() {
                    return value;
                }
            
                public NodeTuple getKey() {
                    return tuple;
                }
            
            };
            public void remove() {
                nodeTupleIterator.remove();
            }
        
            public Entry<NodeTuple, Kleene> next() {
                tuple = nodeTupleIterator.next();
                return result;
            }
        
            public boolean hasNext() {
                return nodeTupleIterator.hasNext();
            }
        };
    }
}