package tvla.core.generic;

import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;

final public class PredicateAssign implements Comparable {
	  public NodeTuple tuple = null;
	  public Predicate predicate = null;
	  public TVS structure = null;
	  public Kleene value = null;
	  
	  public PredicateAssign() { }
	  
	  public PredicateAssign(PredicateAssign pa) {
		  copy(pa.structure, pa.predicate, pa.tuple, pa.value);
	  }

	  public PredicateAssign(TVS structure, Predicate predicate, NodeTuple tuple, Kleene value) {
		  copy(structure, predicate, tuple, value);
	  }
	  
	  public void copy(TVS structure, Predicate predicate, NodeTuple tuple, Kleene value) {
		  this.tuple = tuple;
		  this.predicate = predicate;
		  this.structure = structure;
		  this.value = value;
	  }
	  
	  public boolean equals(PredicateAssign other) {
		  return tuple.equals(other.tuple) && predicate.equals(other.predicate);
	  }
	  
	  public int compareTo(Object o) {
		  if (!(o instanceof PredicateAssign))
			  return -1;
		  PredicateAssign pa = (PredicateAssign)o;
		  if (!predicate.equals(pa.predicate))
			  return predicate.compareTo(pa.predicate);
		  if (tuple.size() != pa.tuple.size())
			  return tuple.size() - pa.tuple.size();
		  for (int i=0; i<tuple.size(); ++i) {
			  if (tuple.get(i) != pa.tuple.get(i))
				  return tuple.get(i).id() - pa.tuple.get(i).id();
		  }
		  return 0;
	  }
	  
	  public int hashCode() {
		  return (1 + 2*tuple.hashCode())*(1+predicate.hashCode());
	  }
	  
	  public void apply() {
		  structure.update(predicate, tuple, value);
	  }
};
