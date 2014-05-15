package tvla.core.functional;

import java.util.Iterator;
import java.util.Collection;

import tvla.logic.Kleene;

// class PackedIntKleeneMap:
// An implementation of interface IntKleeneMap (int->Kleene map) based
// on a tree (implemented using an IntObjectMap) whose leafs (leaves) are
// BoundedIntKleeneMaps of the same size (i.e., each leaf is itself a
// map from integers in the range [0..size-1] to Kleene).

class PackedIntKleeneMap extends IntKleeneMap {

	// ************ STATIC INFO ************
	// This implementation utilizes FourIntLeaf objects as leaves.
	// The following must be changed to change the leaf representation.

   static private BoundedIntKleeneMap defaultMinorMap = FourIntLeaf.zero;
   static private int leafSize = defaultMinorMap.size();

   public static IntKleeneMap zero =
		new PackedIntKleeneMap ( TernaryTree.empty() );
   	// (new PackedIntKleeneMap ( TernaryTree.empty() )).normalize();

	public static void init() {
	/*
		zero = new PackedIntKleeneMap ( TernaryTree.empty() );
		// zero = zero.normalize();
		System.err.println("Created zero.");
		if (zero == null)
			System.err.println("zero is null!");
	   else
			System.err.println("zero is non-null!");
		if (defaultMinorMap == null)
			System.err.println("dmm is null!");
	   else
			System.err.println("dmm is non-null!");
	*/
	}

	public static int uniqueGenerated() {
		return UniquePIKMap.generated();
	}

	// ************ OBJECT INFO ************

   protected IntObjectMap majorMap;
   
   final protected Iterator iterator() {
	   return majorMap.iterator();
   }

   public PackedIntKleeneMap (IntObjectMap major) {
      majorMap = major;
   }

   public PackedIntKleeneMap (PackedIntKleeneMap that) {
      majorMap = that.majorMap;
   }

   public Kleene lookup  (int i) {
      BoundedIntKleeneMap leaf = (BoundedIntKleeneMap) majorMap.lookup (i / leafSize);
      if (leaf == null) return Kleene.falseKleene; 
      return leaf.lookup (i % leafSize);
   }
   
   public void lookupIncrements(IntKleeneMap other, VisitorCombiner results) {
	   majorMap.lookupIncrements(((PackedIntKleeneMap)other).majorMap, results);
   }

   public IntKleeneMap update (int i, Kleene k) {
      int leafNum = i / leafSize;
      BoundedIntKleeneMap leaf = (BoundedIntKleeneMap) majorMap.lookup (leafNum);
      if (leaf == null) {
			if (k == Kleene.falseKleene) return this;
			leaf = defaultMinorMap;
		}
      leaf = leaf.update (i % leafSize, k);
      if (leaf == defaultMinorMap) leaf = null;
      return new PackedIntKleeneMap (majorMap.update(leafNum, leaf));
   }

	public IntKleeneMap join(IntKleeneMap other) {
		PackedIntKleeneMap that = (PackedIntKleeneMap) other;
		return new PackedIntKleeneMap(
							majorMap.join(that.majorMap, defaultMinorMap)
					 );
	}

	public IntKleeneMap normalize() {
		majorMap = majorMap.normalize(defaultMinorMap);
		return UniquePIKMap.instance(this);
	}

	public boolean equals (Object other) {
		if (other instanceof PackedIntKleeneMap) {
			PackedIntKleeneMap that = (PackedIntKleeneMap) other;
			return (that.majorMap == majorMap);
		} else 
			return false;
	}

	public int hashCode() {
		int hash = 0;
		if (majorMap != null) hash ^= majorMap.objectHashCode();
		return hash;
	}

	public void computeSpace(NPSpaceCounter data) {
		data.numFliks += 1 ;
		data.visitingFliks = true ;
		data.visit(majorMap);
		data.visitingFliks = false ;
	}
}

class UniquePIKMap extends PackedIntKleeneMap {

	// ************ STATIC INFO ************
	// Every UniquePIKMap is assigned a distinct integer id, starting from 1 on.
	private static int nextId = 1;
	public static int generated() {
		return nextId-1;
	}

	public static IntKleeneMap instance (PackedIntKleeneMap map) {
		UniquePIKMap temp = new UniquePIKMap(map); 
		UniquePIKMap result = (UniquePIKMap) HashCons.instance(temp);
		if (result.id == 0) {
			// New unique instance; not already in table.
			result.id = nextId++;
		}
		return result;
	}

	// ************ OBJECT INFO ************
	private int id = 0;

	private UniquePIKMap(PackedIntKleeneMap map) {
		super(map);
	}

	public IntKleeneMap normalize() {
		return this;
	}

	public int uid() {
		return id;
	}

	public void computeSpace(NPSpaceCounter data) {
		data.numUniqueFliks += 1 ;
		data.visitingFliks = true ;
		data.visit(majorMap);
		data.visitingFliks = false ;
	}

}
