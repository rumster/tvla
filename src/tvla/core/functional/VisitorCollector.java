package tvla.core.functional;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;

import tvla.core.Node;
import tvla.util.IntObjectPair;
import tvla.logic.Kleene;

public class VisitorCollector implements VisitorKleene {

	Collection nodes;
	Collection results;
	int bound;
	
	Visitor allCollector = null;
	Visitor nonZeroCollector = null;
	Visitor setDefaultCollector = null;
	
	public VisitorCollector(Collection nodes, int maxNode) {
		this.nodes = nodes;
		results = new LinkedList();
		bound = maxNode + 1; 
	}
	
	public void visit(int i, Object o) {
		if (allCollector == null) {
			allCollector = new LeafCollector();
		}
		BoundedIntKleeneMap leaf = (BoundedIntKleeneMap)o;
		leaf.filter(allCollector, bound);
	}
	
	public void visitNonZero(int i, Object o) {
		if (nonZeroCollector == null) {
			nonZeroCollector = new LeafCollectorNonZero();
		}
		BoundedIntKleeneMap leaf = (BoundedIntKleeneMap)o;
		leaf.filter(nonZeroCollector, bound);
	}
	
	public void visitSetDefault(int i, Object o) {
		if (setDefaultCollector == null) {
			setDefaultCollector = new LeafCollectorSetDefault();
		}
		BoundedIntKleeneMap leaf = (BoundedIntKleeneMap)o;
		leaf.filter(setDefaultCollector, bound);
	}

	final class LeafCollector implements Visitor {
		public void visit(int i, Object o) {
			results.add(new IntObjectPair(i, o));
		}
	}

	final class LeafCollectorNonZero implements Visitor {
		public void visit(int i, Object o) {
			if ((Kleene)o != Kleene.falseKleene) {
				results.add(new IntObjectPair(i, o));
			}
		}
	}

	final class LeafCollectorSetDefault implements Visitor {
		public void visit(int i, Object o) {
			if ((Kleene)o != Kleene.falseKleene) {
				results.add(new IntObjectPair(i, Kleene.falseKleene));
			}			
		}
	}
}
