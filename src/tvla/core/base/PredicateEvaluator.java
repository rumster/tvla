package tvla.core.base;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.base.concrete.ConcretePredicate;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;

/**
 * Use to efficiently evaluate many tuples over the same
 * tvs and predicate. Assumes that the tvs DOES NOT CHANGE during
 * operation.
 * @author tla
 */
public abstract class PredicateEvaluator {
    static ValuePredicateEvaluator alwaysFalse = new ValuePredicateEvaluator(Kleene.falseKleene);
    static ValuePredicateEvaluator alwaysTrue = new ValuePredicateEvaluator(Kleene.trueKleene);
    static ValuePredicateEvaluator alwaysUnknown = new ValuePredicateEvaluator(Kleene.unknownKleene);
    
    public abstract Kleene eval(NodeTuple tuple);
	public Kleene eval(Node left, Node right) {
		return eval(NodeTuple.createPair(left, right));
	}
 
    protected static class ValuePredicateEvaluator extends PredicateEvaluator {
        
        Kleene value;

        protected ValuePredicateEvaluator(Kleene value) {
            super();
            this.value = value;
        }

        @Override
        public Kleene eval(NodeTuple tuple) {
            return value;
        }
        
    }
    public static PredicateEvaluator evaluator(final Predicate predicate, final TVS tvs) {
        assert predicate.arity() > 0;

        if (!tvs.getVocabulary().contains(predicate)) {
            return alwaysUnknown;
        }
        if (tvs instanceof BaseTVS) {
            BaseTVS baseTvs = (BaseTVS) tvs;
            final ConcretePredicate concrete = baseTvs.predicates.get(predicate);
            if (concrete == null) {
                return alwaysFalse;
            }
            return new PredicateEvaluator() {
                @Override
                public Kleene eval(NodeTuple tuple) {
                    return concrete.get(tuple);
                }            
            };
        } else {
            return new PredicateEvaluator() {
                @Override
                public Kleene eval(NodeTuple tuple) {
                    return tvs.eval(predicate, tuple);
                }
            };
        }
    }
}

