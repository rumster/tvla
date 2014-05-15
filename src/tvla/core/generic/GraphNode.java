package tvla.core.generic;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import tvla.util.HashSetFactory;

public class GraphNode<T extends GraphNode<T>> implements Identifiable, Comparable<T> {
	Set<T> dependents = HashSetFactory.make();
	Set<T> dependsOn = HashSetFactory.make();
	Collection<T> strongDependents = java.util.Collections.emptyList();
	Collection<T> strongBackDependents = java.util.Collections.emptyList();
	Collection<T> nonStrongDependents = java.util.Collections.emptyList();
	
	int id;
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int compareTo(T other) {
	    return id - other.id;
	}
	
	public void setStrongDependents(Collection<T> deps) {
  	    strongDependents = new LinkedList<T>(deps);
		strongBackDependents = new LinkedList<T>();
		for (T dependent : strongDependents) {
			if (dependent.id < id)
				strongBackDependents.add(dependent);
		}	
		Set<T> nonStrongDependents = HashSetFactory.make(dependents);
		nonStrongDependents.removeAll(strongDependents);
		this.nonStrongDependents = new LinkedList<T>(nonStrongDependents);
	}

	public static <T extends GraphNode<T>> void DFS(T node, Collection<T> unvisited, LinkedList<T> rPostorder, boolean reverse) {	
		Iterator<T> it;
		if (reverse) it = node.dependsOn.iterator();
		else it = node.dependents.iterator();
		  
		unvisited.remove(node);
		for (; it.hasNext();) {
			T u = it.next();
			if (unvisited.contains(u)) {
				DFS(u, unvisited, rPostorder, reverse);
			}
		}
		rPostorder.addFirst(node);
	}

	public static <T extends GraphNode<T>> Collection<Collection<T>> getConnectedComponents(Set<T> constraints) {
		  Collection<T> unvisited = HashSetFactory.make();
		  
	      for (T constraint : constraints) {
		        unvisited.add(constraint);
	      }
	      
	      // DFS of the constraints graph
	      LinkedList<T> rPostorder = new LinkedList<T>();
	      while (!unvisited.isEmpty()) {
	    	  Iterator<T> it = unvisited.iterator();
	    	  T constraint = it.next();
	    	  DFS(constraint, unvisited, rPostorder, false);
	      }
	      
	      // Set nodes numbering in reverse postorder
	      int id = 0;
	      for (Iterator<T> it = rPostorder.iterator(); it.hasNext(); id++) {
	    	  T node = it.next();
	    	  node.id = id;
	      }
	      
	      // DFS of the transpose graph in reverse postorder
	      LinkedList<Collection<T>> components = new LinkedList<Collection<T>>();
	      while (!rPostorder.isEmpty()) {
	    	  LinkedList<T> temp = new LinkedList<T>();
	    	  T constraint = rPostorder.removeFirst();
	    	  DFS(constraint, rPostorder, temp, true);
	    	  SortedSet<T> component = new TreeSet<T>();
	    	  component.addAll(temp);
	    	  for (Iterator<T> it = component.iterator(); it.hasNext(); ) {
	    		  constraint = it.next();
	    		  SortedSet<T> tempSet = new TreeSet<T>();
	    		  for (Iterator<T> itDeps = constraint.dependents.iterator(); itDeps.hasNext();) {
	    			  T dependent = itDeps.next();
	    			  if (component.contains(dependent)) {
	    				  tempSet.add(dependent);
	    				  //constraint.strongDependents.add(dependent);
	    			  }
	    		  }
	    		  constraint.setStrongDependents(tempSet);
	    	  }
	    	  components.add(new LinkedList<T>(component));
	      }
		  return components;
	  }
}
