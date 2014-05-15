package tvla.core.decompose;

import java.util.Map;

import tvla.formulae.Formula;
import tvla.predicates.Predicate;
import tvla.util.HashMapFactory;

public class CloseCycle {
    static Map<Predicate,Formula> closeCycleComponents = HashMapFactory.make();
    
    public static void addCloseCycle(Predicate predicate, Formula dname) {
       closeCycleComponents.put(predicate, dname);
    }    
    
    public static Formula getDName(Predicate predicate) {
        return closeCycleComponents.get(predicate);
    }
}
