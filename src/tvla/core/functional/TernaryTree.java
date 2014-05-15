package tvla.core.functional;

import java.util.Iterator;
import java.util.Map;

import tvla.logic.Kleene;
import tvla.util.IntObjectPair;

// class TernaryTree:
// An implementation of IntObjectMap (int->Object maps) based on ternary trees.

abstract class TernaryTree extends IntObjectMap {
   // We refer to the constant-valued  map that maps all integers 
   // to null as the "empty" map. All maps are created by applying
   // a finite number of updates to an empty map.

   // Externally, an empty map is represented by a real object, which
   // also serves as a factory for creating all maps.

   private static TernaryTree externalEmptyMap = new TernaryLeaf(null,null,null);

   public static IntObjectMap empty() { return externalEmptyMap; }

   // Internally, the null pointer is used to represent an empty-map,
   // which maps all integers to null
   protected static TernaryTree emptyMap = null;
   
   final protected Iterator iterator() {
	   return new TernaryTreeIterator(this);
   }

   protected static Object lookup (TernaryTree tree, int i) {
      if (tree == null)
         return null;
      else
         return tree.lookup(i);
   }
   
   protected static void lookupIncrements(int i, TernaryTree o1, TernaryTree o2, VisitorCombiner results) {
	   if (o1 == o2)
		   return;
	   if (o1 == null)
		   // FIXME: increments are asymmetric - maybe this is not needed?
		   o2.lookupNonZeroToDefault(i, results);
	   else if (o2 == null)
		   o1.lookupNonZero(i, results);
	   else o1.lookupIncrements(i, o2, results);
   }
   
   protected static TernaryTree update (TernaryTree tree, int point, Object value) {
      // first, consider the special case where a null tree has to be updated:
      if (tree == null) return mkPointTree (point, value);

      // For non-null trees, first "grow" the tree, if necessary, to ensure its size
      // is greater than the point whose value is to be updated:
      TernaryTree result = tree;
      while (result.size() <= point) 
         result = new TernaryNonLeaf (result.size(), result, emptyMap, emptyMap);

      // now, invoke the map's own restricted update operation:
      result = result.doUpdate(point, value);

      // Now, check result to see if its representation can be simplified:
      // We just check for the special case of a constant null-valued map.
		// ADDENDUM: Omit partial normalization now.
      // if (value == null) // First, a more efficient check.
         // if (result.isNull()) return null;

      return result;
   }

   // mkPointTree(point,value):
   //    creates a tree representing a singleton map [point->value].
   protected static TernaryTree mkPointTree (int point, Object value) {
      // Special case:
      if (value == null) return externalEmptyMap;

      // Build the tree bottom up, starting from the leaf:
      TernaryTree result = null;
      switch (point%3) {
        case 0: result = new TernaryLeaf (value, null, null); break;
        case 1: result = new TernaryLeaf (null, value, null); break;
        case 2: result = new TernaryLeaf (null, null, value); break;
      }
      // Now, grow the leaf up to a tree of the appropriate size:
      for (int i = point/3; i > 0; i = i/3) {
      	switch (i%3) {
         	case 0:
					// TODO: can optimize this away; but need to carry size separately.
            	result = new TernaryNonLeaf (result.size(), result, emptyMap, emptyMap);
	     			break;
         	case 1:
	     			result = new TernaryNonLeaf (result.size(), emptyMap, result, emptyMap);
	     			break;
          	case 2:
	     			result = new TernaryNonLeaf (result.size(), emptyMap, emptyMap, result);
	     			break;
        }
      }
      return result;
   }

   public IntObjectMap update(int i, Object value) {
      TernaryTree result = this.doUpdate (i, value);
      return result;
   }

	// public Iterator iterator() { return new TernaryTreeIterator(this); }

	public abstract TernaryTree join(TernaryTree other, Object defaultValue, TernaryTree defaultTree);

	public IntObjectMap join(IntObjectMap other, Object defaultValue) {
		TernaryTree defaultTree = externalEmptyMap;
		return this.join ( (TernaryTree) other, defaultValue, defaultTree);
	}

	public abstract TernaryTree normalize(Object defaultValue, TernaryTree defaultTree);

	public IntObjectMap normalize(Object defaultValue) {
      if (defaultValue instanceof Normalizable)
			defaultValue = ((Normalizable) defaultValue).normalize();
		TernaryTree defaultTree = externalEmptyMap.normalize(defaultValue,null);
		return this.normalize (defaultValue, defaultTree);
	}

   public abstract int size();
   // public abstract boolean isNull();
   public abstract TernaryTree doUpdate(int i, Object value); 
   
   public abstract TreeState getNonEmpty(TreeState prev, int i);
   
	public final void lookupAll(VisitorKleene results) {
		lookupAll(0, results);
	}
	
	public final void lookupNonZero(VisitorKleene results) {
		lookupNonZero(0, results);
	}

	public final void lookupNonZeroToDefault(VisitorKleene results) {
		lookupNonZeroToDefault(0, results);
	}
	
	abstract TernaryTree matchingBranch(int size, VisitorKleene results);
	abstract TernaryTree matchingBranch(int size);
	
	public final void lookupIncrements(IntObjectMap other, VisitorCombiner results) {
		TernaryTree obj1 = this;
		TernaryTree obj2 = (TernaryTree)other;
		
		obj1 = matchingBranch(obj2.size(), results);
		obj2 = obj2.matchingBranch(size());
		
		obj1.lookupIncrements(0, obj2, results);
	}

   
   abstract void lookupAll(int i, VisitorKleene results);
   abstract void lookupNonZero(int i, VisitorKleene results);
   abstract void lookupNonZeroToDefault(int i, VisitorKleene results);
   abstract void lookupIncrements(int i, IntObjectMap other, VisitorCombiner results);

}

class TernaryNonLeaf extends TernaryTree {
   protected int childSize;
   protected TernaryTree child0, child1, child2;

   TernaryNonLeaf(int cs, TernaryTree v0, TernaryTree v1, TernaryTree v2) {
      childSize = cs;
      child0 = v0; child1 = v1; child2 = v2;
   }

   TernaryNonLeaf(TernaryNonLeaf that) {
      childSize = that.childSize;
      child0 = that.child0; child1 = that.child1; child2 = that.child2;
   }

   public int size() { return 3*childSize; }

	/*
   public boolean isNull() {
      return (child0 == null) && (child1 == null) && (child2 == null);
   }
	*/

   public Object lookup (int i) {
      int j = i % childSize;
      switch (i / childSize) {
        case 0: return TernaryTree.lookup(child0, j);
        case 1: return TernaryTree.lookup(child1, j);
        case 2: return TernaryTree.lookup(child2, j);
        default: return null;
      }
   }
   

   public TernaryTree doUpdate(int i, Object value) {
      int j = i % childSize;
      switch (i / childSize) {
        case 0:
	   		return new TernaryNonLeaf (childSize, update(child0,j,value), child1, child2);
        case 1:
	   		return new TernaryNonLeaf (childSize, child0, update(child1,j,value), child2);
        case 2:
	   		return new TernaryNonLeaf (childSize, child0, child1, update(child2,j,value));
        default:
	   		return TernaryTree.update (this, i, value);
      }
   }

	// equals: for use during hash-consing; assumes children have been hash-consed.
	public boolean equals (Object other) {
		if (other instanceof TernaryNonLeaf) {
			TernaryNonLeaf that = (TernaryNonLeaf) other;
			return
				(that.childSize == childSize) &&
			   (that.child0 == child0) && (that.child1 == child1) && (that.child2 == child2);
		} else
			return false;
	}


	public int hashCode() {
		int hash = 0;
		// avoid recursive traversal of whole tree by calling "objectHashCode".
		if (child0 != null) hash ^= child0.objectHashCode();
		if (child1 != null) hash ^= child1.objectHashCode();
		if (child2 != null) hash ^= child2.objectHashCode();
		return hash;
	}

	private static TernaryTree join (TernaryTree x, TernaryTree y, Object defaultValue, TernaryTree defaultTree) {
		if (x == null) x = defaultTree;
		if (y == null) y = defaultTree;
		return x.join(y, defaultValue, defaultTree);
	}

	public TernaryTree join(TernaryTree other, Object defaultValue, TernaryTree defaultTree) {
		if (this.size() < other.size()) {
			return other.join (this, defaultValue, defaultTree);
		} else if (this.size() == other.size()) {
			TernaryNonLeaf that = (TernaryNonLeaf) other;
			TernaryTree v0 = join (this.child0, that.child0, defaultValue, defaultTree);
			TernaryTree v1 = join (this.child1, that.child1, defaultValue, defaultTree);
			TernaryTree v2 = join (this.child2, that.child2, defaultValue, defaultTree);
			return new TernaryNonLeaf(this.childSize,v0,v1,v2);
		} else {
			TernaryTree v0 = join (this.child0, other, defaultValue, defaultTree);
			TernaryTree v1 = join (this.child1, defaultTree, defaultValue, defaultTree);
			TernaryTree v2 = join (this.child2, defaultTree, defaultValue, defaultTree);
			return new TernaryNonLeaf(this.childSize,v0,v1,v2);
		}
	}

	public TernaryTree normalize(Object defaultValue, TernaryTree defaultTree) {
		if (child0 != null)
			child0 = (TernaryTree) child0.normalize(defaultValue, defaultTree);
		else
			child0 = defaultTree;
		if (child1 != null)
			child1 = (TernaryTree) child1.normalize(defaultValue, defaultTree);
		else
			child1 = defaultTree;
		if (child2 != null)
			child2 = (TernaryTree) child2.normalize(defaultValue, defaultTree);
		else
			child2 = defaultTree;

		return
			(child1 == defaultTree) && (child2 == defaultTree) ?
				child0 :
				UniqueTernaryNonLeaf.instance(this);
	}

	public void computeSpace(NPSpaceCounter data) {
		data.incrNumTreeNodes();
		if (child0 != null) data.visit(child0);
		if (child1 != null) data.visit(child1);
		if (child2 != null) data.visit(child2);
	}
	
	void lookupAll(int i, VisitorKleene results) {
		if (child0 != null) child0.lookupAll(i, results);
		if (child1 != null) child1.lookupAll(i + childSize, results);
		if (child2 != null) child2.lookupAll(i + 2 * childSize, results);
	}
	
	void lookupNonZero(int i, VisitorKleene results) {
		if (child0 != null) child0.lookupNonZero(i, results);
		if (child1 != null) child1.lookupNonZero(i + childSize, results);
		if (child2 != null) child2.lookupNonZero(i + 2 * childSize, results);
	}
	
	void lookupNonZeroToDefault(int i, VisitorKleene results) {
		if (child0 != null) child0.lookupNonZeroToDefault(0, results);
		if (child1 != null) child1.lookupNonZeroToDefault(i + childSize, results);
		if (child2 != null) child2.lookupNonZeroToDefault(i + 2 * childSize, results);
	}

	void lookupIncrements(int i, IntObjectMap other, VisitorCombiner results) {
    	TernaryNonLeaf that = (TernaryNonLeaf)other;
		lookupIncrements(i, child0, that.child0, results);
		lookupIncrements(i + childSize, child1, that.child1, results);
		lookupIncrements(i + 2 * childSize, child2, that.child2, results);
    }
	
	TernaryTree matchingBranch(int size) {
		if (size < size())
			return child0.matchingBranch(size);
		else return this;
	}
	
	TernaryTree matchingBranch(int size, VisitorKleene results) {
		if (size < size()) {
			child1.lookupNonZero(results);
			child2.lookupNonZero(results);
			return child0.matchingBranch(size, results);
		}
		else return this;
	}

	public TreeState getNonEmpty(TreeState prev, int i) {
		TreeState a, cur = prev;
		switch (i) {
		case 0:
			if (child0 != null) {
				cur = cur.push(this, 0);
				a = child0.getNonEmpty(cur, 0);
				if (a != null) {
					return a;
				}
				cur = cur.ret();
			}
		case 1:
			if (child1 != null) {
				cur = cur.push(this, 1);
				a = child1.getNonEmpty(cur, 0);
				if (a != null) {
					return a;
				}
				cur = cur.ret();
			}
		case 2:
			if (child2 != null) {
				cur = cur.push(this, 2);
				a = child2.getNonEmpty(cur, 0);
				if (a != null) {
					return a;
				}
				cur = cur.ret();
			}
		}
		return null;
	}
}



class TernaryLeaf extends TernaryTree {
   protected Object value0, value1, value2;

   TernaryLeaf(Object v0, Object v1, Object v2) {
      value0 = v0; value1 = v1; value2 = v2;
   }

   TernaryLeaf(TernaryLeaf that) {
      value0 = that.value0; value1 = that.value1; value2 = that.value2;
   }

   public int size() { return 3; }

	/*
   public boolean isNull() {
      return (value0 == null) && (value1 == null) && (value2 == null);
   }
	*/

   public Object lookup(int i) {
      switch (i) {
        case 0: return value0;
        case 1: return value1;
        case 2: return value2;
        default: return null;
      }
   }

    void lookupAll(int i, VisitorKleene results) {
		if (value0 != null) 
			results.visit(i, value0);
		if (value1 != null) 
			results.visit(i + 1, value1);
		if (value2 != null) 
			results.visit(i + 2, value2);
	}
   
	void lookupIncrements(int i, IntObjectMap other, VisitorCombiner results) {
    	TernaryLeaf that = (TernaryLeaf)other;
    	if (value0 != that.value0)
    		results.visit(i, value0, that.value0);
    	if (value1 != that.value1)
    		results.visit(i + 1, value1, that.value1);
    	if (value2 != that.value2)
    		results.visit(i + 2, value2, that.value2);
	}
	
	TernaryTree matchingBranch(int size) {
		if (size < size())
			return null;
		else return this;
	}
	
	TernaryTree matchingBranch(int size, VisitorKleene results) {
		if (size < size()) {
			lookupNonZero(results);
			return null;
		}
		else return this;
	}

	protected static final Object needIncrement(Object o1, Object o2) {
		if (o1 == o2)
			return null;
		if (o1 == null && o2 != Kleene.falseKleene)
			return Kleene.falseKleene;
		if (o1 == Kleene.falseKleene && o2 == null)
			return null;
		return o1;
	}
	
	void lookupNonZero(int i, VisitorKleene results) {
		if (value0 != null) 
			results.visitNonZero(i, value0);
		if (value1 != null) 
			results.visitNonZero(i + 1, value1);
		if (value2 != null) 
			results.visitNonZero(i + 2, value2);
	}

	void lookupNonZeroToDefault(int i, VisitorKleene results) {
		if (value0 != null) 
			results.visitSetDefault(i, value0);
		if (value1 != null) 
			results.visitSetDefault(i + 1, value1);
		if (value2 != null) 
			results.visitSetDefault(i + 2, value2);
	}

   public TernaryTree doUpdate(int i, Object value) {
      switch (i) {
        case 0: return new TernaryLeaf (value, value1, value2);
        case 1: return new TernaryLeaf (value0, value, value2);
        case 2: return new TernaryLeaf (value0, value1, value);
        default: return TernaryTree.update (this, i, value);
      }
   }

	// equals: for use during hash-consing; assumes "value"s have been hash-consed.
	public boolean equals (Object other) {
		if (other instanceof TernaryLeaf) {
			TernaryLeaf that = (TernaryLeaf) other;
			return (that.value0 == value0) && (that.value1 == value1) && (that.value2 == value2);
		} else
			return false;
	}

	public int hashCode() {
		int hash = 0;
		if (value0 != null) hash ^= value0.hashCode();
		if (value1 != null) hash ^= value1.hashCode();
		if (value2 != null) hash ^= value2.hashCode();
		return hash;
	}

	// objectHashCode: original hash-code, defined by class Object.
	public int objectHashCode() { return super.hashCode(); }

	private static Object join (Object x, Object y, Object defaultValue) {
		if (x == null) x = defaultValue;
		if (y == null) y = defaultValue;
		BoundedIntKleeneMap value1 = (BoundedIntKleeneMap) x;
		BoundedIntKleeneMap value2 = (BoundedIntKleeneMap) y;
		return value1.join(value2);
	}

	public TernaryTree join(TernaryTree other, Object defaultValue, TernaryTree defaultTree) {
		if (other instanceof TernaryLeaf) {
			TernaryLeaf that = (TernaryLeaf) other;
			Object v0 = join (this.value0, that.value0, defaultValue);
			Object v1 = join (this.value1, that.value1, defaultValue);
			Object v2 = join (this.value2, that.value2, defaultValue);
			return new TernaryLeaf(v0,v1,v2);
		} else
			return other.join (this, defaultValue, defaultTree);
	}

	public TernaryTree normalize(Object defaultValue, TernaryTree defaultTree) {
		Object v0, v1, v2;
		if (value0 == null)
			v0 = defaultValue;
      else if (value0 instanceof Normalizable)
			v0 = ((Normalizable) value0).normalize();
		else
			v0 = value0;
		if (value1 == null)
			v1 = defaultValue;
      else if (value1 instanceof Normalizable)
			v1 = ((Normalizable) value1).normalize();
		else
			v1 = value1;
		if (value2 == null)
			v2 = defaultValue;
      else if (value2 instanceof Normalizable)
			v2 = ((Normalizable) value2).normalize();
		else
			v2 = value2;

		return UniqueTernaryLeaf.instance(new TernaryLeaf(v0,v1,v2));
	}

	public void computeSpace(NPSpaceCounter data) {
		data.incrNumTreeNodes();
		if ((value0 != null) && (value0 instanceof Countable)) data.visit((Countable)value0);
		if ((value1 != null) && (value0 instanceof Countable)) data.visit((Countable)value1);
		if ((value2 != null) && (value0 instanceof Countable)) data.visit((Countable)value2);
	}
	
	public TreeState getNonEmpty(TreeState prev, int i) {
		switch (i) {
		case 0:
			if (value0 != null && value0 != Anchor.defaultValue)
				return prev.push(this, 0).setValue(value0);
		case 1:
			if (value1 != null && value1 != Anchor.defaultValue)
				return prev.push(this, 1).setValue(value1);
		case 2:
			if (value2 != null && value2 != Anchor.defaultValue)
				return prev.push(this, 2).setValue(value2);
		}
		return null;
	}
}


class UniqueTernaryNonLeaf extends TernaryNonLeaf {
	// constructor is private to ensure only unique instances are created.
	private UniqueTernaryNonLeaf(TernaryNonLeaf node) {
	   super(node);
	}

	public static TernaryNonLeaf instance(TernaryNonLeaf node) {
		return (TernaryNonLeaf) HashCons.instance(new UniqueTernaryNonLeaf(node));
	}

	// normalize: this is already a unique instance
	public TernaryTree normalize() { return this; }

}

class UniqueTernaryLeaf extends TernaryLeaf {
	// constructor is private to ensure only unique instances are created.
	private UniqueTernaryLeaf(TernaryLeaf leaf) {
	   super(leaf);
	}

	public static TernaryLeaf instance(TernaryLeaf leaf) {
		return (TernaryLeaf) HashCons.instance(new UniqueTernaryLeaf(leaf));
	}

	// normalize: this is already a unique instance
	public TernaryTree normalize() { return this; }
}

interface TreeState {
	abstract TreeState push(TernaryTree t, int i);
	
	abstract TreeState ret();

	abstract Object get();
	
	abstract TreeState setValue(Object value);
	
	abstract TreeState advance();
};

class Entry implements Map.Entry {
	Object value = null;
	Object key = null;
	public Entry() {}
	
	final public Object setValue(Object value){
		this.value = value;
		return value;
	}
	final public Object setKey(Object key){
		this.key = key;
		return key;
	}
	final public Object getValue() {
		return value;
	}
	final public Object getKey() {
		return key;
	}
};


final class Anchor implements TreeState {
	TernaryTree current;
	Anchor previous;
	int index;
	Object value;
	
	static Object defaultValue = Kleene.falseKleene;

	Anchor(TernaryTree current) {
		this.current = current;
		this.previous = null;
		this.index = 0;
		this.value = null;
	}
	
	Anchor(TernaryTree current, Anchor previous, int index) {
		this.current = current;
		this.previous = previous;
		this.index = index;
		this.value = null;
	}

	Anchor(TernaryTree current, Anchor previous, int index, Object value) {
		this.current = current;
		this.previous = previous;
		this.index = index;
		this.value = value;
	}
	
	public final TreeState push(TernaryTree t, int i) {
		return new Anchor(t, this, i);
	}
	
	public final TreeState ret() {
		return previous;
	}

	public final Object get() {
		return value;
	}
	
	public TreeState setValue(Object value) {
		this.value = value;
		return this;
	}
	
	public TreeState advance() {
		if (index < 2) {
			return current.getNonEmpty(this, index + 1);
		}
		else return previous.advance();
	}
}

final class TernaryTreeIterator implements Iterator {
	TreeState anchor;
	
	public TernaryTreeIterator(TernaryTree t) {
		anchor = t.getNonEmpty(new Stack(t), 0);
	}
	
	public boolean hasNext() {
		return anchor.get() != null;
	}
	
	public Object next() {
		Object o = anchor.get();
		anchor = anchor.advance();
		return o;
	}
	
	public void remove() {
		throw new UnsupportedOperationException();
	}
};

final class Stack implements TreeState {
	
	private static int stackSize = 16;
	private Object[] stack;
	private byte[] bStack;
	private int sp1 = 0;
	private int sp2 = 0;
	private Object value;
	private TernaryTree current;
	private int index;
	
	private void grow() {
		Object[] newStack = new Object[stack.length * 2];
		System.arraycopy(stack, 0, newStack, 0, stack.length);
		stack = newStack;
	}
	
	private void growByte() {
		byte[] newStack = new byte[bStack.length * 2];
		System.arraycopy(bStack, 0, newStack, 0, bStack.length);
		bStack = newStack;
	}

	final private void pushByte(int i) {
		try {
			bStack[++sp2] = (byte)i;
		}
		catch(ArrayIndexOutOfBoundsException e) {
			growByte();
			bStack[sp2] = (byte)i;
		}
	}
	
	final private int popByte() {
		return bStack[sp2--]; 
	}

	final private void push(Object o) {
		try {
			stack[++sp1] = o;
		}
		catch(ArrayIndexOutOfBoundsException e) {
			grow();
			stack[sp1] = o;
		}
	}
	
	final private Object pop() {
		return stack[sp1--]; 
	}

	public Stack(TernaryTree t) {
		stack = new Object[stackSize];
		bStack = new byte[stackSize];
		value = null;
		current = t;
		index = 0;
	}
	
	public final TreeState push(TernaryTree t, int i) {
		push(current);
		pushByte(index);
		current = t;
		index = i;
		return this;
	}
	
	public final TreeState ret() {
		index = popByte();
		current = (TernaryTree)pop();
		return this;
	}

	public final Object get() {
		return value;
	}
	
	public TreeState setValue(Object value) {
		this.value = value;
		return this;
	}
	
	public TreeState advance() {
		if (index < 2) {
			return current.getNonEmpty(this, index + 1);
		}
		else return ret().advance();
	}
};

