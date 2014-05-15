package tvla.core.base;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import tvla.core.Node;
import tvla.core.TVS;
import tvla.core.common.NodeValue;
import tvla.predicates.Predicate;
import tvla.util.HashMapFactory;

public class BaseTVSCache {
	static Map<Predicate, Map<NullableNodeTuple, Collection<NodeValue>>> map = null;
	static TVS current = null;

	public static void reset() {
		map = null;
		current = null;
	}
	
	private static class NullableNodeTuple {
	    Node[] nodes;
	    public NullableNodeTuple(Node[] nodes, boolean copy) {
	        if (copy) {
	            this.nodes = new Node[nodes.length];
	            System.arraycopy(nodes, 0, this.nodes, 0, nodes.length);
	        } else {
	            this.nodes = nodes;
	        }
	    }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(nodes);
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final NullableNodeTuple other = (NullableNodeTuple) obj;
            if (!Arrays.equals(nodes, other.nodes))
                return false;
            return true;
        }
	    
	}
	
	public static void setValues(TVS structure, Predicate pred, Node[] other, Collection<NodeValue> values) {
		if (map == null) {
			map = HashMapFactory.make();
		}
		if (structure != current) {
			map.clear();
			current = structure;
		}
		Map<NullableNodeTuple, Collection<NodeValue>> other2values = map.get(pred);
		if (other2values == null) {
			other2values = HashMapFactory.make();
		}
		other2values.put(new NullableNodeTuple(other, true), values);
		map.put(pred, other2values);
	}
	
	public static void modify(TVS structure, Predicate pred) {
		if (structure == current) {
			map.remove(pred);
		}
	}
	
	public static Collection<NodeValue> getValues(TVS structure, Predicate pred, Node[] other) {
		if (map == null)
			return null;
		
		if (structure != current)
			return null;
		
        Map<NullableNodeTuple, Collection<NodeValue>> other2values = map.get(pred);
        if (other2values == null) {
			return null;
        }
		
		return other2values.get(new NullableNodeTuple(other, false));
	}
}
