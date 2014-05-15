package tvla.core.common;

import tvla.core.NodeTuple;
import tvla.logic.Kleene;

public class NodeValue {
	  public NodeTuple tuple;
	  public Kleene value;
	  public boolean added = false;
	  
	  public NodeValue() {
		  tuple = null;
		  value = null;
	  }

	  public NodeValue(NodeValue nv) {
		  tuple = nv.tuple;
		  value = nv.value;
	  }
	  
	  public NodeValue(NodeTuple tuple, Kleene value) {
		  this.tuple = tuple;
		  this.value = value;
	  }
	  
	  public NodeValue(NodeTuple tuple, Kleene value, boolean added) {
		  this.tuple = tuple;
		  this.value = value;
		  this.added = added;
	  }

	  public int hashCode() {
		  return tuple.hashCode();
	  }
	  
	  public boolean equals(Object o) {
		  NodeValue nv = (NodeValue)o;
		  return tuple.equals(nv.tuple);
	  }
	  
	  public int compareTo(Object o) {
		  NodeValue nv = (NodeValue)o;
		  return tuple.compareTo(nv.tuple);
	  }
	  
	  public String toString() {
	      return tuple + "=" + value;
	  }
}
