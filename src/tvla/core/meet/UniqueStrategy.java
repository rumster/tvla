package tvla.core.meet;

import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import tvla.core.Canonic;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVSSet;
import tvla.logic.Kleene;
import tvla.predicates.DynamicVocabulary;
import tvla.predicates.Predicate;
import tvla.util.HashSetFactory;

public class UniqueStrategy implements MeetSignatureStrategy {

    private static final Node[] EMPTY_NODE_ARRAY = new Node[0];
    private Set<Predicate> nullary;
    private Set<Predicate> unique;
    private Set<Predicate> unary;

    public UniqueStrategy(TVSSet lSet, TVSSet rSet, DynamicVocabulary shared) {
        nullary = HashSetFactory.make(shared.nullary());
        unique = HashSetFactory.make(shared.unique());
        unary = HashSetFactory.make(shared.unary());
        for (HighLevelTVS lTvs : lSet) {
            filterUnknownNullary(lTvs);
            filterUnknownUnique(lTvs);
        }       
        for (HighLevelTVS rTvs : rSet) {
            filterUnknownNullary(rTvs);
            filterUnknownUnique(rTvs);
        }       
    }

    public Object sign(HighLevelTVS structure) {
            Canonic signature = new Canonic(nullary.size() + unique.size());
            for (Predicate predicate : nullary) {
                Kleene value = structure.eval(predicate);
                signature.add(value);
            }
            for (Predicate uniqueP : unique) {
                Iterator<Entry<NodeTuple, Kleene>> iterator = structure.iterator(uniqueP);
                if (iterator.hasNext()) {
                    Entry<NodeTuple, Kleene> entry = iterator.next();
                    assert entry.getValue() == Kleene.trueKleene; // uniqueP was chosen this way.
                    Node node = (Node) entry.getKey();
                    for (Predicate unaryP : unary) {
                        Kleene value = structure.eval(unaryP, node);
                        signature.add(value);
                    }
                } else {
                    signature.add(Kleene.falseKleene);
                }
            }
            return signature;        
    }
    
    protected void filterUnknownNullary(HighLevelTVS tvs) {
        for (Iterator<Predicate> iterator = nullary.iterator(); iterator.hasNext();) {
            Predicate predicate = iterator.next();
            assert predicate.arity() == 0;
            if (tvs.eval(predicate) == Kleene.unknownKleene) {
                iterator.remove();
            }
        }        
    }

    protected void filterUnknownUnique(HighLevelTVS tvs) {
        for (Iterator<Predicate> uniqueIter = unique.iterator(); uniqueIter.hasNext();) {
            Predicate uniqueP = uniqueIter.next();
            assert uniqueP.arity() == 1 && uniqueP.unique(); 
            int numberSatisfy = tvs.numberSatisfy(uniqueP);
            if (numberSatisfy == 0) {
                // Good
                continue;
            } else if (numberSatisfy > 1) {
                // Bad
                uniqueIter.remove();
                continue;
            }
            // Check it is true
            Iterator<Entry<NodeTuple, Kleene>> nodeIter = tvs.iterator(uniqueP);
            if (!nodeIter.hasNext()) {
                uniqueIter.remove();
                continue;
            }
            Entry<NodeTuple, Kleene> entry = nodeIter.next();
            if (entry.getValue() == Kleene.unknownKleene) {
                uniqueIter.remove();
                continue;
            }
            Node node = (Node) entry.getKey();
            for (Iterator<Predicate> unaryIter = unary.iterator(); unaryIter.hasNext();) {
                Predicate unaryP = unaryIter.next();
                if (tvs.eval(unaryP, node) == Kleene.unknownKleene) {
                    unaryIter.remove();
                }
            }
        }        
    }    
}
