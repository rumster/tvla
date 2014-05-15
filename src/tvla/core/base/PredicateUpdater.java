package tvla.core.base;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.base.concrete.ConcreteKAryPredicate;
import tvla.core.base.concrete.ConcretePredicate;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;

/**
 * Use to efficiently update many tuples over the same
 * tvs and predicate. 
 * @author tla
 */
public abstract class PredicateUpdater {
    protected static PredicateUpdater doNothing = new PredicateUpdater() {
        @Override
        public void update(NodeTuple tuple, Kleene value) {
            // Do nothing
        }
    };
    
    public static void reset() {
    	doNothing = new PredicateUpdater() {
            @Override
            public void update(NodeTuple tuple, Kleene value) {
                // Do nothing
            }
    	};
    }
    
    public void update(Node left, Node right, Kleene value) {
    	update(NodeTuple.createPair(left, right), value);
    }
    
    public abstract void update(NodeTuple tuple, Kleene value);
 
    public static PredicateUpdater updater(final Predicate predicate, final TVS tvs) {
        assert predicate.arity() > 0;
        
        if (!tvs.getVocabulary().contains(predicate)) {
            return doNothing;
        }
        if (tvs instanceof BaseTVS) {
            final BaseTVS baseTvs = (BaseTVS) tvs;
            return new PredicateUpdater() {
                boolean first = true;
                ConcretePredicate concrete = baseTvs.predicates.get(predicate);
                
                @Override
                public void update(NodeTuple tuple, Kleene value) {
                    if (value == Kleene.falseKleene) {
                        if (concrete != null) {
                            modify();
                            concrete.set(tuple, value);
                            if (concrete.isAllFalse()) {
                                baseTvs.predicates.remove(predicate);
                                baseTvs.mcache.remove(predicate);
                            }
                        }
                    } else {
                        modify();
                        if (concrete == null) {
                            concrete = new ConcreteKAryPredicate(predicate.arity());
                            baseTvs.predicates.put(predicate, concrete);
                            baseTvs.mcache.put(predicate, concrete);
                        } 
                        concrete.set(tuple, value);
                    }
                }
                private void modify() {
                    if (first) {
                        baseTvs.clearCanonic();
                        BaseTVSCache.modify(baseTvs, predicate);
                        if (concrete != null) {
                            concrete.modify();
                        }
                        first = false;
                    }
                }
            };
        } else {
            return new PredicateUpdater() {
                @Override
                public void update(NodeTuple tuple, Kleene value) {
                    tvs.update(predicate, tuple, value);
                }
            };
        }
    }
}

