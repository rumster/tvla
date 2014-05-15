package tvla.core.functional;

import java.util.Iterator;
import java.util.Collection;

// interface IntObjectMap: denotes an immutable map from integer to Object.
abstract class IntObjectMap extends Countable {

   // map.lookup(i) : retrieve value at point i
   public abstract Object lookup(int i);

   public abstract void lookupAll(VisitorKleene results);
   
   public abstract void lookupNonZero(VisitorKleene results);

   public abstract void lookupNonZeroToDefault(VisitorKleene results);

   public abstract void lookupIncrements(IntObjectMap other, VisitorCombiner results);

   // map.update(i,v): return a new map obtained by updating map at specified point.
   public abstract IntObjectMap update(int i, Object value);

	public static class Entry {
	   public int key;
		public Object value;
		public Entry (int k, Object v) {
		   key = k;
			value = v;
		}
	}

	// iterator: returns all Entries (i,o) in map where o is non-null.
	// public abstract Iterator iterator(); 

	public abstract IntObjectMap normalize(Object defaultValue);

	public abstract IntObjectMap join(IntObjectMap other, Object defaultValue);

	// objectHashCode: original hash-code, defined by class Object.
	public int objectHashCode() { return super.hashCode(); }

   public Object lookup(int i, Object defaultValue) {
      Object result = this.lookup(i);
      return (result == null) ? defaultValue : result;
   }
   
   protected Iterator iterator() {
	   throw new UnsupportedOperationException();
   }
}
