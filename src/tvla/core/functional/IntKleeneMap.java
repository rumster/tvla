package tvla.core.functional;

import java.util.Iterator;

import tvla.logic.Kleene;

abstract public class IntKleeneMap extends Countable {
   // map.lookup(i) : retrieve value at point i
   public abstract Kleene lookup(int i);
   
   public abstract void lookupIncrements(IntKleeneMap other, VisitorCombiner results);

   // map.update(i,v): return a new map obtained by updating map at specified point.
   public abstract IntKleeneMap update(int i, Kleene value);

   // map.normalize() : returns a unique object equivalent to original map, as
   // in hash-consing.
   public abstract IntKleeneMap normalize();

	public abstract IntKleeneMap join(IntKleeneMap other);

	// map.uid() : returns a unique id.
	public int uid() { 
		throw new RuntimeException("IntKleeneMap::uid unimplemented.");
	}
	
   protected Iterator iterator() {
	   throw new RuntimeException("IntKleeneMap::iterator unimplemented");
   }

}
