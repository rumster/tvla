package tvla.core.functional;

import tvla.logic.Kleene;
import tvla.util.Logger;

// class FourIntLeaf:
// This is an implementation of the interface BoundedIntKleeneMap that
// uses four integers to pack kleene values.

class FourIntLeaf extends BoundedIntKleeneMap {

   // ******** Extension to Interface ************

   // Constant-valued maps:
   public final static FourIntLeaf zero =
		NormalizedFourIntLeaf.instance(new FourIntLeaf(Kleene.falseKleene));
   // public final static FourIntLeaf one = new FourIntLeaf(Kleene.trueKleene);
   // public final static FourIntLeaf oneHalf = new FourIntLeaf(Kleene.unknownKleene);

   // ******** The Implementation ************

   protected static final int kleenesPerInt = KleenePacking.kleenesPerInt;
   protected int word0, word1, word2, word3;

   public int size() { return 4 * kleenesPerInt; }

   public Kleene lookup(int i) {
      switch (i / kleenesPerInt) {
        case 0: return KleenePacking.lookup (word0, i % kleenesPerInt);
        case 1: return KleenePacking.lookup (word1, i % kleenesPerInt);
        case 2: return KleenePacking.lookup (word2, i % kleenesPerInt);
        case 3: return KleenePacking.lookup (word3, i % kleenesPerInt);
	default: return Kleene.kleene((byte) 0);
      }
   }
   
   public void filter(Visitor results, int i) {
	  int j = (i % kleenesPerInt) + kleenesPerInt;
	  switch (i / kleenesPerInt) {
	  case 3:
		  KleenePacking.filter(word3, results, j);
		  j = kleenesPerInt;
	  case 2:
		  KleenePacking.filter(word2, results, j);
		  j = kleenesPerInt;
	  case 1:
		  KleenePacking.filter(word1, results, j);
		  j = kleenesPerInt;
	  case 0:
		  KleenePacking.filter(word0, results, j);
	  }
   }
   
   public void combine(BoundedIntKleeneMap other, VisitorCombiner results, int i) {
	   FourIntLeaf that = (FourIntLeaf)other;
	   int j = (i % kleenesPerInt) + kleenesPerInt;
	   switch (i / kleenesPerInt) {
	   case 3:
		   KleenePacking.combine(word3, that.word3, results, j);
		   j = kleenesPerInt;
	   case 2:
		   KleenePacking.combine(word2, that.word2, results, j);
		   j = kleenesPerInt;
	   case 1:
		   KleenePacking.combine(word1, that.word1, results, j);
		   j = kleenesPerInt;
	   case 0:
		   KleenePacking.combine(word0, that.word0, results, j);
	   }
   }

   public BoundedIntKleeneMap update (int i, Kleene k) {
      FourIntLeaf result = new FourIntLeaf (this);
      switch (i / kleenesPerInt) {
        case 0:
           result.word0 = KleenePacking.update (word0, i % kleenesPerInt, k);
	   		return result;
        case 1:
	   		result.word1 = KleenePacking.update (word1, i % kleenesPerInt, k);
	   		return result;
        case 2:
	   		result.word2 = KleenePacking.update (word2, i % kleenesPerInt, k);
	   		return result;
        case 3:
	   		result.word3 = KleenePacking.update (word3, i % kleenesPerInt, k);
	   		return result;
			default:
	   		return this;
      }

	  // Note: turn off special normalization; not very usual, as full
	  // normalization will be done soon.
      // Special normalization, currently done only when result is zero.
	  /*
      if (k.kleene() == 0) {
        return result.isEqualTo(zero) ? zero : result;
      }
      else return result;
      */
   }

   public BoundedIntKleeneMap join (BoundedIntKleeneMap other) {
		FourIntLeaf that = (FourIntLeaf) other;
		int w0 = KleenePacking.join(this.word0, that.word0);
		int w1 = KleenePacking.join(this.word1, that.word1);
		int w2 = KleenePacking.join(this.word2, that.word2);
		int w3 = KleenePacking.join(this.word3, that.word3);
		return new FourIntLeaf (w0,w1,w2,w3);
	}

	public Object normalize() {
		return NormalizedFourIntLeaf.instance(this);
	}

   private boolean isEqualTo(FourIntLeaf that) {
      return (this.word0 == that.word0) && (this.word1 == that.word1) &&
             (this.word2 == that.word2) && (this.word3 == that.word3);
   }

   public FourIntLeaf (int w0, int w1, int w2, int w3) {
      word0 = w0;
      word1 = w1;
      word2 = w2;
      word3 = w3;
   }

   public FourIntLeaf (FourIntLeaf that) {
      word0 = that.word0;
      word1 = that.word1;
      word2 = that.word2;
      word3 = that.word3;
   }

   private FourIntLeaf (Kleene k) {
      word0 = KleenePacking.constant(k);
      word1 = word0;
      word2 = word0;
      word3 = word0;
   }
 
	/*
	public int hashCode() {
		return (word0+word1+word2+word3);
	}

	public boolean equals(Object other) {
		if (other instanceof FourIntLeaf) {
			FourIntLeaf that = (FourIntLeaf) other;
      	return (word0 == that.word0) && (word1 == that.word1) &&
             (word2 == that.word2) && (word3 == that.word3);
		} else
			return false;
	}
	*/

	public static void printStatistics() {
		Logger.println("Number of unique int-pairs generated: " +
			NormalizedFourIntLeaf.intpair.generated());
	}

	public void computeSpace(NPSpaceCounter data) {
		data.numLeaves += 1 ;
	}
}

class NormalizedFourIntLeaf extends FourIntLeaf {
	protected static IntHashCons intpair = new IntHashCons(10001);
	protected static LeafHashCons map = new LeafHashCons(10001);

	private NormalizedFourIntLeaf(FourIntLeaf tuple) {
		super(tuple);
	}

	static public FourIntLeaf instance(FourIntLeaf tuple) {
		// create tentative normalized form of tuple:
		FourIntLeaf newobj = new NormalizedFourIntLeaf(tuple);

		// check if it has already been created:
	   int pair1 = intpair.instance( tuple.word0, tuple.word1);
	   int pair2 = intpair.instance( tuple.word2, tuple.word3);
		return (FourIntLeaf) map.instance(pair1, pair2, newobj);
	}

	public Object normalize() {
		// already in normal form
		return this;
	}

	static public int numHashTblEntries() { return intpair.size() + map.size(); }

}

class LeafHashCons {
	private Entry table[];
	private int numEntries;

	public LeafHashCons (int tblsize) {
		table = new Entry[tblsize];
		numEntries = 0;
	}

	public FourIntLeaf instance(int i, int j, FourIntLeaf leaf) {
	   int hash = ( ((j & 255) << 8) + (i & 255) ) % table.length;

		for (Entry entry = table[hash]; entry != null; entry = entry.next) {
			if ((entry.fst == i) && (entry.snd == j))
				return entry.val;
		}

		table[hash] = new Entry (i, j, leaf, table[hash]);
		numEntries ++;
		return leaf;
	}

	public int size() { return numEntries; }

	private static class Entry {
		public int fst, snd;
		FourIntLeaf val;
		public Entry next;

		public Entry(int f, int s, FourIntLeaf v, Entry n) {
			fst = f;
			snd = s;
			val = v;
			next = n;
		}

	}

}

