package tvla.core.base;

import java.util.Map;
import java.util.Iterator;
import tvla.predicates.Predicate;
import tvla.core.TVS;
import tvla.core.base.concrete.ConcretePredicate;

final class PredicateCache {
	final private Map<Predicate, ConcretePredicate> predicates;
	final private TVS structure;
	
	public PredicateCache(Map<Predicate, ConcretePredicate> predicates, TVS structure) {
		this.predicates = predicates;
		this.structure = structure;
	}
	
	final ConcretePredicate get(Predicate p) {

		if (p.cachedStructure == structure)
			return (ConcretePredicate) p.cachedReference;
		else {

		    ConcretePredicate o = predicates.get(p);
			p.cachedStructure = structure;
			p.cachedReference = o;
			return o;
		}
	}
	
	//final void putCurrent(Object o) {
		//cur.putConcrete(o);
	//}

	final void put(Predicate p, ConcretePredicate o) {
		p.cachedStructure = structure;
		p.cachedReference = o;
	}
	
	final void remove(Predicate p) {
		p.cachedStructure = structure;
		p.cachedReference = null;
	}
	
	final void clear() {
		for (Iterator<Predicate> it = predicates.keySet().iterator(); it.hasNext();) {
			it.next().cachedStructure = null;
		}
	}

    public void clear(Predicate p) {
        p.cachedStructure = null;
    }
}

