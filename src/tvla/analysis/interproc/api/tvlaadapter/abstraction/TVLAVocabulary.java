package tvla.analysis.interproc.api.tvlaadapter.abstraction;

import tvla.analysis.interproc.api.tvlaadapter.TVLAPredicate;
import tvla.analysis.interproc.api.utils.TVLAAPIAssert;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;

/**
 *  Facade for vocabulary
 */
public class TVLAVocabulary implements ITVLAVocabulary {	
  public final TVLAPredicate sm;
  public final TVLAPredicate inUc;
  public final TVLAPredicate inUx;
  public final TVLAPredicate kill;
  public final TVLAPredicate[] preds;         
  
  
  public Predicate tvlaPredSM;
  public Predicate tvlaPredInUc;
  public Predicate tvlaPredInUx;
  public Predicate tvlaPredKill;
  
  
  // A singeleton patterns
////  protected static TVLAVocabulary theVoc = null;
  
 // public static TVLAVocabulary getVocabulary() {
   // if (theVoc != null) 
    //  return theVoc;
  //  
 //   theVoc = new TVLAVocabulary();
    
 //   return theVoc;
 // }
  
  protected TVLAVocabulary() {
    sm = new TVLAPredicate("sm", 1);
    inUc = new TVLAPredicate("inUc", 1);
    inUx = new TVLAPredicate("inUx", 1);
    kill = new TVLAPredicate("kill", 1);
    preds = new TVLAPredicate[]{sm, inUc, inUx, kill};
    
    tvlaPredSM = Vocabulary.getPredicateByName(sm.getPredId());
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(tvlaPredSM != null);
    
    tvlaPredInUc = Vocabulary.createPredicate(inUc.getPredId(), inUc.getArity());
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(tvlaPredInUc != null);
    
    tvlaPredInUx = Vocabulary.createPredicate(inUx.getPredId(), inUx.getArity());
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(tvlaPredInUx != null);
    
    tvlaPredKill = Vocabulary.createPredicate(kill.getPredId(), kill.getArity());
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(tvlaPredKill != null);
    
  }
  
  
  /**************************************************
   * Initialization
   **************************************************/
  
  public final void init() {
    TVLAPredicate[] internals = getInternalPredicates();
    
    if (internals != null) {
      for (int i=0; i<internals.length; i++) {
        TVLAPredicate pred = internals[i];
        
        Vocabulary.createPredicate(pred.getPredId(), pred.getArity());      
      }
    }
  }
  
  
  
  /**************************************************
   * Java modeling: "Builtin" predicates
   **************************************************/
  
  public TVLAPredicate[] getInternalPredicates() {
    return null;
  }
  
  public TVLAPredicate[] getTVLAInternalPredicates() {
    return preds;
  }
  
  public IPredicate getSM() {
    return sm;
  }
  
  public IPredicate getInUc() {
    return inUc;
  }
  
  public IPredicate getInUx() {
    return inUx;
  }
  
  public IPredicate getKill() {
    return kill;
  }
  
  public IPredicate getPredicate(String id) {
    if (id == null)
      return null;
    
    // TODO imporve stupiut imp
    for (int i=0; i<preds.length; i++)
      if (id.equals(preds[i].getPredId()))
        return preds[i];
    
    return null;
  }
  
  
  /**************************************************
   * Java modeling: Acces to the TVLA preds
   **************************************************/
 
  public Predicate getTVLASM() {
    return tvlaPredSM;
  }
  
  public Predicate getTVLAInUc() {
    return tvlaPredInUc;
  }
  
  public Predicate getTVLAInUx() {
    return tvlaPredInUx;
  }
  
  public Predicate getTVLAKill() {
    return tvlaPredKill;
  }
  
  
}