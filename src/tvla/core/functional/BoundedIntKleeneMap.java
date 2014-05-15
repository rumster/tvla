package tvla.core.functional;

import tvla.logic.Kleene;

// interface BoundedIntKleeneMap: 
// This is the interface for functional maps of type [0..S] -> Kleene
// (i.e., maps from integers in some range 0 to S to Kleene).
// We refer to S+1 as the size of the map.

abstract class BoundedIntKleeneMap extends Normalizable {
   public abstract int size();

   // map.lookup(i) : retrieve value at point i
   public abstract Kleene lookup(int i);
   
   public abstract void filter(Visitor results, int bound);
   
   public abstract void combine(BoundedIntKleeneMap other, VisitorCombiner results, int bound);

   // map.update(i,v): return a new map obtained by updating map at specified point.
   public abstract BoundedIntKleeneMap update(int i, Kleene value);

   public abstract BoundedIntKleeneMap join (BoundedIntKleeneMap other);
	
   // public abstract BoundedIntKleeneMap normalize();
}

