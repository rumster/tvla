package tvla.analysis.interproc.api.tvlaadapter;

import tvla.api.ITVLAAPI.IVocabulary.IPredicate;

/**
 *  Facade for predicate
 */
public class TVLAPredicate  implements IPredicate {
  protected final String 	predId;
  protected final int 	arity;
  
  public TVLAPredicate(String id, int arity) {
    predId = id;
    this.arity = arity;
  }
  
  public int getArity() {
    return arity;
  }
  
  public String getPredId() {
    return predId;
  }
}