package tvla.core.generic;

import tvla.core.NodeTuple;
import tvla.predicates.Predicate;
import tvla.core.common.NodeValue;
import tvla.logic.Kleene;

public class PredicateNodeValue {
	  public Predicate predicate;
	  public NodeValue nvalue;
	  
	  public PredicateNodeValue() {
		  predicate = null;
		  nvalue = null;
	  }

	  public PredicateNodeValue(PredicateNodeValue pnv) {
		  predicate = pnv.predicate;
		  nvalue = pnv.nvalue;
	  }
	  
	  public PredicateNodeValue(Predicate pred, NodeValue nvalue) {
		  this.predicate = pred;
		  this.nvalue = nvalue;
	  }
	  
	  public int hashCode() {
		  return predicate.hashCode()*135743 + nvalue.hashCode();
	  }
	  
	  public boolean equals(Object o) {
		  PredicateNodeValue pnv = (PredicateNodeValue)o;
		  return nvalue.equals(pnv.nvalue) && predicate.equals(pnv.predicate);
	  }
	  
	  public int compareTo(Object o) {
		  PredicateNodeValue pnv = (PredicateNodeValue)o;
		  int r = predicate.compareTo(pnv.predicate);
		  if (r != 0)
			  return r;
		  else
			  return nvalue.compareTo(pnv.nvalue);
	  }
}
