package tvla.core;

import tvla.logic.Kleene;
import tvla.predicates.Predicate;



/** An abstract class representing the Combine operation. 
 * @see tvla.core.TVS
 * @see tvla.formulae.Formula
 * @author Noam Rinetzkt
 */
public abstract class Combine { 
    public static interface INullaryCombiner{
      Kleene combineNumarryPredicate(Predicate pred, Kleene firstVal, Kleene secondVal);
    }
    
    /** combines 2 srucutres into 1.
     * 
     * Nullaries are combined using a user supplied nullary combiner which must not be null!.
     * 
     * @param structure the structure to combine.
     * @param firstStructure the 1st TVS (i.e.,., from the call site)
     * @param secondStructure the 2nd TVS (i.e.,., from the exit site)
     */
    public abstract TVS combine(INullaryCombiner nullaryCombiner, TVS firstStructure, TVS secondStructure);
}