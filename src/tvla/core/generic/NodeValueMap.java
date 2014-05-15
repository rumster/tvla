package tvla.core.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.common.NodeValue;
import tvla.logic.Kleene;
import tvla.predicates.DynamicVocabulary;
import tvla.predicates.Predicate;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;

final public class NodeValueMap {
	  protected Map<Predicate, Collection<NodeValue>> map;
	  protected Collection<Collection<Node>> inequalities;
	  protected DynamicVocabulary deltaPredicates = DynamicVocabulary.empty();
	  
	  public NodeValueMap() {
	  }

	  public NodeValueMap(int size) {
		  map = HashMapFactory.make(size);
	  }
	  
	  public void addInequality(Collection<Node> nodes) {
	      if (inequalities == null) {
	          inequalities = new ArrayList<Collection<Node>>();
	      }
	      inequalities.add(nodes);
	  }
	  
	  private Collection<NodeValue> newCollection() {
		  return new LinkedList<NodeValue>();
	  }
	  
	  private Collection<NodeValue> newCollection(Collection<NodeValue> col) {
		  return new LinkedList<NodeValue>(col);
	  }
	  
	  public void put(PredicateAssign pa) {
		  put(pa.predicate, new NodeValue(pa.tuple, pa.value));
	  }
	  
	  public boolean putAndCheck(PredicateAssign pa) {
		  NodeValue newAssign = new NodeValue(pa.tuple, pa.value);
		  Collection<NodeValue> newAssigns;
		  Predicate predicate = pa.predicate;

		  if (map == null)
			  map = HashMapFactory.make(5);
	      	
		  if (!map.containsKey(predicate)) {
	      		newAssigns = newCollection();
	      		map.put(predicate, newAssigns);
	      		newAssigns.add(newAssign);
	      		return false;
	      }

		  boolean exists = false;
		  newAssigns = map.get(predicate);
	      	
		  for (Iterator<NodeValue> it = newAssigns.iterator(); it.hasNext();) {
			  NodeValue nv = it.next();
			  if (nv.equals(newAssign)) {
				  if (nv.value == newAssign.value)
					  exists = true;
				  it.remove();
				  break;
			  }
		  }
		  newAssigns.add(newAssign);
		  return exists;
	  }

	  public void put(Predicate predicate, NodeTuple tuple, Kleene value) {
		  put(predicate, new NodeValue(tuple, value));
	  }

	  // Used for fast sequential elements addition. Assumes no duplicates
	  public void fastPut(Predicate predicate, NodeTuple tuple, Kleene value) {
		  fastPut(predicate, new NodeValue(tuple, value));
	  }
	  
	  // Used for fast sequential elements addition. Assumes no duplicates
	  public void fastPut(Predicate predicate, NodeValue newAssign) {
		  	Collection<NodeValue> newAssigns;
	      	if (!map.containsKey(predicate)) {
	      		newAssigns = newCollection();
	      		map.put(predicate, newAssigns);
	      	}
	      	else newAssigns = map.get(predicate);
      		newAssigns.add(newAssign);
	  }
	  
	  public void put(Predicate predicate, NodeValue newAssign) {
	      	Collection<NodeValue> newAssigns;
	      	if (map == null)
	      		map = HashMapFactory.make(5);
	      	
	      	if (!map.containsKey(predicate)) {
	      		newAssigns = newCollection();
	      		map.put(predicate, newAssigns);
	      		newAssigns.add(newAssign);
	      		return;
	      	}

	      	newAssigns = map.get(predicate);
	      	
			for (Iterator<NodeValue> it = newAssigns.iterator(); it.hasNext();) {
			  NodeValue nv = it.next();
			  if (nv.equals(newAssign)) {
				  it.remove();
				  break;
			  }
			}
	      	newAssigns.add(newAssign);
	  }

	  public Set<Predicate> modifiedPredicates() {
	      Set<Predicate> result = HashSetFactory.make(map.keySet());
	      result.addAll(deltaPredicates.all());
	      return result;
	  }
	  
	  public int size() {
		  if (map == null)
			  return 0;
		  
		  int s = 0;
		  for (Iterator<Collection<NodeValue>> it = map.values().iterator(); it.hasNext();) {
			  s += it.next().size();
		  }
		  return s;
	  }
	  
	  public float rankedSize() {
		  if (map == null)
			  return 0;
		  
		  int s = 0;
		  for (Map.Entry<Predicate,Collection<NodeValue>> entry : map.entrySet()) {
			  Predicate p = entry.getKey();
			  Collection<NodeValue> col = entry.getValue();
			  s += col.size() * p.rank;
		  }
		  return s;
	  }

	  public boolean isEmpty() {
		  return map == null;
	  }
	  
	  public void addAll(NodeValueMap other) {
		  if (other.isEmpty())
			  return;
		  
		  if (map == null)
			  map = HashMapFactory.make(other.map.size());
			  
		  for (Map.Entry<Predicate,Collection<NodeValue>> entry : other.map.entrySet()) {
			  Collection<NodeValue> col = entry.getValue();
			  Predicate pred = entry.getKey();
			  if (!map.containsKey(pred)) {
				  map.put(pred, newCollection(col));
			  }
			  else {
				  Collection<NodeValue> this_col = map.get(pred);
				  this_col.removeAll(col);
				  this_col.addAll(col);
			  }
		  }
	  }

	  public void addAll(Predicate pred, Collection<NodeValue> col) {
		  if (col.isEmpty())
			  return;
		  
		  if (map == null)
			  map = HashMapFactory.make(5);
			  
		  if (!map.containsKey(pred)) {
			  map.put(pred, newCollection(col));
		  }
		  else {
			  Collection<NodeValue> this_col = map.get(pred);
			  this_col.removeAll(col);
			  this_col.addAll(col);
		  }
	  }
	  
	  public Collection<NodeValue> get(Predicate key) {
		  if (map == null)
			  return null;
		  
		  return map.get(key);
	  }
	  
	  public boolean containsKey(Predicate key) {
		  if (map == null)
			  return false;
		  return map.containsKey(key);
	  }
	  
	  public void clear() {
		  map = null;
	  }
	  
	  public void removeNode(Predicate key, Node node) {
		  Collection<NodeValue> col = get(key);
		  if (col == null)
			  return;
		  for (Iterator<NodeValue> it = col.iterator(); it.hasNext();) {
			  NodeValue nv = it.next();
			  if (nv.tuple.contains(node))
				  it.remove();
		  }
	  }

	  public boolean equals(NodeValueMap other) {
		  return this.containedIn(other) && other.containedIn(this);
	  }
	  
	  public boolean containedIn(NodeValueMap other) {
		  if (map == null || map.isEmpty())
			  return true;
		  if (other.map == null)
			  return false;
		  
		  for (Map.Entry<Predicate,Collection<NodeValue>> entry : map.entrySet()) {
			  Predicate p = entry.getKey();
			  if (!other.map.containsKey(p))
				  return false;
			  Collection<NodeValue> c1 = entry.getValue();
			  Collection<NodeValue> c2 = other.get(p);
			  for (NodeValue nv1 : c1) {
				  boolean found = false;
                  for (NodeValue nv2 : c2) {
					  if (nv1.equals(nv2.tuple)) {
						  if (nv1.value != nv2.value)
							  return false;
						  found = true;
						  break;
					  }
				  }
				  if (!found)
					  return false;
			  }
		  }
		  return true;
	  }
	  
	  public String toString() {
		  String s = "Incremental map:\n";
		  if (map == null)
			  return s;
		  
		  for (Map.Entry<Predicate,Collection<NodeValue>> entry : map.entrySet()) {
			  Predicate p = entry.getKey();
			  Collection<NodeValue> c = entry.getValue();
			  s = s + "predicate: " + p.toString() + "\n";
			  for (NodeValue nv : c) {
				  s = s + "   " + nv.tuple.toString() + ":" + nv.value.toString() + ",";
			  }
			  s = s + "\n";
		  }
		  if (inequalities != null) {
		      s = s + "Inequalities: " + inequalities + "\n";
		  }
		  if (deltaPredicates != DynamicVocabulary.empty()) {
		      s = s + "Added predicates: " + deltaPredicates + "\n";
		  }
		  return s;
	  }

    public Collection<Collection<Node>> getInequalities() {
        return inequalities;
    }
    

    public void addDeltaPredicates(DynamicVocabulary addedPredicates) {
        deltaPredicates = deltaPredicates.union(addedPredicates);
    }

    public DynamicVocabulary getDeltaPredicates() {
        return deltaPredicates;
    }
	  
};
