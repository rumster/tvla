package tvla.core.base;

import gnu.trove.PrimeFinder;

import java.util.Collection;
import java.util.Iterator;

import tvla.core.generic.BitSet;
import tvla.predicates.Predicate;
import tvla.logic.Kleene;
import tvla.core.Node;
import tvla.core.NodeTuple;

final public class BaseTVSBloomSet {

	private BitSet bits;
	private int capacity;
	private int size;
	private BitSet emptyBits;
	
	public BaseTVSBloomSet() {
		this(64000);
	}

	public BaseTVSBloomSet(int size) {
		size = PrimeFinder.nextPrime(size);
		bits = new BitSet(3 * size + 32);
		emptyBits = new BitSet(3 * size + 32);
		capacity = size; 
		this.size = 0;
	}
	
	public void clear() {
		bits.setAll(emptyBits);
		size = 0;
	}
	
	public void add(Predicate p, NodeTuple tuple, Kleene value) {
		int h = hash(p, tuple);
		bits.set(3 * h + value.kleene());
		++size;
	}
	
	public Kleene eval(BaseTVS s, Predicate p, NodeTuple tuple) {
		int h = hash(p, tuple);
		int result = bits.wordAtPosition(3 * h) & 7;
		switch (result) {
		case 1:
			//if (v != Kleene.falseKleene) {
			//	System.out.println("oops");
			//}
			return Kleene.falseKleene;
		case 2:
			//if (v != Kleene.unknownKleene) {
			//	System.out.println("oops");
			//}
			return Kleene.unknownKleene;
		case 4:
			//if (v != Kleene.trueKleene) {
			//	System.out.println("oops");
			//}
			return Kleene.trueKleene;
		default:
			Kleene value = s.evalInternal(p, tuple);
			add(p, tuple, value);
			return value;
		}
	}
	
	private final int hash(Predicate p, NodeTuple tuple) {
		int h = p.hashCode() + 1000003 * tuple.hashCode();
		//h = 1000003 * h + s.hashCode();
		return (h & 0x7FFFFFFF) % capacity;
	}
}
