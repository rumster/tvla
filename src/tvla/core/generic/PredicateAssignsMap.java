package tvla.core.generic;

import java.util.Collection;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.Map;

import tvla.core.common.NodeValue;
import tvla.predicates.Predicate;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.logic.Kleene;
import tvla.util.NoDuplicateLinkedList;
import tvla.predicates.Vocabulary;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;

public class PredicateAssignsMap {
	  Map map = null;
	  
	  public PredicateAssignsMap() {
		  //map = new TreeMap();
	  }

	  public PredicateAssignsMap(PredicateAssignsMap other) {
		  //map = new TreeMap();
		  map = HashMapFactory.make(other.map.size());
		  for (Iterator it = other.map.entrySet().iterator(); it.hasNext();) {
			  Map.Entry entry = (Map.Entry)it.next();
			  Predicate pred = (Predicate)entry.getKey();
			  Collection col = (Collection)entry.getValue();
			  //map.put(pred, new ArrayList(col));
			  //map.put(pred, new TreeSet(col));
			  map.put(pred, new LinkedList(col));
			  //Map col = (Map)entry.getValue();
			  //map.put(pred, HashMapFactory.make(col));
		  }
	  }
	  
	  public void put(PredicateAssign newAssign) {
	      	Collection newAssigns;
		    //Map newAssigns;

	      	if (map == null)
	      		map = HashMapFactory.make(5);
	      	
	      	if (!map.containsKey(newAssign.predicate)) {
	      		//newAssigns = new ArrayList();
	      		//newAssigns = new TreeSet();
	      		newAssigns = new LinkedList();
	      		//newAssigns = HashSetFactory.make(1);
	      		//newAssigns = HashMapFactory.make(1);
	      		map.put(newAssign.predicate, newAssigns);
	      		newAssigns.add(newAssign);
	      		//newAssigns.put(newAssign.tuple, newAssign);
	      		return;
	      	}

	      	newAssigns = (Collection)map.get(newAssign.predicate);
	      	//newAssigns = (Map)map.get(newAssign.predicate);
	      	
			for (Iterator it = newAssigns.iterator(); it.hasNext();) {
			  PredicateAssign pa = (PredicateAssign)it.next();
			  if (pa.tuple.equals(newAssign.tuple)) {
				  //pa.value = newAssign.value;
				  //return;
				  it.remove();
				  break;
			  }
			}
			
	      	//if (newAssigns.contains(newAssign))
	      	//	newAssigns.remove(newAssign);
	      	
	      	//if (newAssigns.contains(newAssign.tuple))
	      	//	newAssigns.remove(newAssign.tuple);
	      	//newAssigns.put(newAssign.tuple, newAssign);
	      	newAssigns.add(newAssign);
	  }
	  
	  public void remove(Predicate p, NodeTuple tuple) {
		  	if (map == null)
		  		return;
		  	
	      	Collection newAssigns = (Collection)map.get(p);
	      	
			for (Iterator it = newAssigns.iterator(); it.hasNext();) {
			  PredicateAssign pa = (PredicateAssign)it.next();
			  if (pa.tuple.equals(tuple)) {
				  it.remove();
				  break;
			  }
			}
	  }
	  
	  public boolean isEmpty() {
		  return map == null;
	  }
	  
	  public void addAll(PredicateAssignsMap other) {
		  if (other.isEmpty())
			  return;
		  
		  if (map == null)
			  map = HashMapFactory.make(5);
			  
		  for (Iterator it = other.map.entrySet().iterator(); it.hasNext();) {
			  Map.Entry entry = (Map.Entry)it.next();
			  Collection col = (Collection)entry.getValue();
			  Predicate pred = (Predicate)entry.getKey();
			  if (!map.containsKey(pred)) {
				  map.put(pred, new LinkedList(col));
			  }
			  else {
				  Collection this_col = (Collection)map.get(pred);
				  this_col.removeAll(col);
				  this_col.addAll(col);
				  //for (Iterator it2 = col.iterator(); it2.hasNext();) {
					//  put((PredicateAssign)it2.next());
				  //}
			  }
		  }
	  }
	  
	  public Collection get(Predicate key) {
		  if (map == null)
			  return null;
		  
		  return (Collection)map.get(key);
		  /*
		  Map col = (Map)map.get(key);
		  if (col == null)
			  return null;
		  else
			  return col.values();
		  */
	  }
	  
	  public boolean containsKey(Predicate key) {
		  if (map == null)
			  return false;
		  return map.containsKey(key);
	  }
	  
	  public void removeNode(Predicate key, Node node) {
		  Collection col = get(key);
		  if (col == null)
			  return;
		  for (Iterator it = col.iterator(); it.hasNext();) {
			  PredicateAssign pa = (PredicateAssign)it.next();
			  if (pa.tuple.contains(node))
				  it.remove();
		  }
	  }

	  public void clearPredicate(Predicate key) {
	
		  Collection col = get(key);
		  if (col == null)
			  return;
		 
		  //Collection temp = new ArrayList();
		  Collection temp = new LinkedList();
		  for (Iterator it = col.iterator(); it.hasNext();) {
			  PredicateAssign pa = (PredicateAssign)it.next();
			  if (pa.value != Kleene.falseKleene) {
				  it.remove();
				  temp.add(new PredicateAssign(pa.structure, pa.predicate, pa.tuple, Kleene.falseKleene));
			  }
		  }
			  
		  col.addAll(temp);

/*
		  Collection col = (Collection)get(key);
		  if (col == null)
			  return;
		  Collection temp = new LinkedList();
		  for (Iterator it = col.iterator(); it.hasNext();) {
			  PredicateAssign pa = (PredicateAssign)it.next();
			  if (pa.value != Kleene.falseKleene) {
				  it.remove();
				  temp.add(new PredicateAssign(pa.structure, pa.predicate, pa.tuple, Kleene.falseKleene));
			  }
		  }
		  for (Iterator it = temp.iterator(); it.hasNext();) {
			  put((PredicateAssign)it.next());
		  }
*/
	  }
	  
	  public boolean equals(PredicateAssignsMap other) {
		  return this.containedIn(other) && other.containedIn(this);
	  }
	  
	  public boolean containedIn(PredicateAssignsMap other) {
		  if (map == null || map.isEmpty())
			  return true;
		  if (other.map == null)
			  return false;
		  
		  for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
			  Map.Entry entry = (Map.Entry)it.next();
			  Predicate p = (Predicate)entry.getKey();
			  if (!other.map.containsKey(p))
				  return false;
			  Collection c1 = (Collection)entry.getValue();
			  Collection c2 = other.get(p);
			  for (Iterator it2 = c1.iterator(); it2.hasNext();) {
				  PredicateAssign pa1 = (PredicateAssign)it2.next();
				  boolean found = false;
				  for (Iterator it3 = c2.iterator(); it3.hasNext();) {
					  PredicateAssign pa2 = (PredicateAssign)it3.next();
					  if (pa1.tuple.equals(pa2.tuple)) {
						  if (pa1.value != pa2.value)
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
		  
		  for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
			  Map.Entry entry = (Map.Entry)it.next();
			  Predicate p = (Predicate)entry.getKey();
			  Collection c = (Collection)entry.getValue();
			  s = s + "predicate: " + p.toString() + "\n";
			  for (Iterator it2 = c.iterator(); it2.hasNext();) {
				  PredicateAssign pa = (PredicateAssign)it2.next();
				  s = s + "   " + pa.tuple.toString() + ":" + pa.value.toString() + ",";
			  }
			  s = s + "\n";
		  }
		  return s;
	  }
	  
};

