package tvla.core.generic;

import tvla.core.common.NodeValue;
import tvla.core.NodeTuple;
import tvla.predicates.Predicate;

public class PredicateNode {
	  public Predicate predicate;
	  public NodeTuple tuple;
	  public boolean added = false;
	  
	  public PredicateNode() {
		  predicate = null;
		  tuple = null;
	  }

	  public PredicateNode(PredicateNode pn) {
		  predicate = pn.predicate;
		  tuple = pn.tuple;
	  }
	  
	  public PredicateNode(Predicate pred, NodeTuple tuple) {
		  this.predicate = pred;
		  this.tuple = tuple;
	  }
	  
	  public PredicateNode(Predicate pred, NodeTuple tuple, boolean added) {
		  this.predicate = pred;
		  this.tuple = tuple;
		  this.added = added;
	  }

	  public int hashCode() {
		  return predicate.hashCode()*135743 + tuple.hashCode();
	  }
	  
	  public boolean equals(Object o) {
		  PredicateNode pn = (PredicateNode)o;
		  return tuple.equals(pn.tuple) && predicate.equals(pn.predicate);
	  }
	  
	  public int compareTo(Object o) {
		  PredicateNode pn = (PredicateNode)o;
		  int r = predicate.compareTo(pn.predicate);
		  if (r != 0)
			  return r;
		  else
			  return tuple.compareTo(pn.tuple);
	  }
}
