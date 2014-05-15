package tvla.core;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Set;

import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashSetFactory;
import tvla.util.Logger;
import tvla.util.ProgramProperties;

/** A canonical name for a node. Notice that to reach the same name,
 * the predicate values must be added in the same order.
 * @see tvla.logic.Kleene
 * @see tvla.core.Blur
 * @author Tal Lev-Ami
 */
final public class Canonic implements Comparable<Canonic> {
	
	private static final int width = 2;
	private static final BigInteger widthMask = BigInteger.valueOf((1 << width) - 1);

	private MutableBigInteger name;

	/** Should the canonicalization be done using all three values or 
	 * only using true and false ignoring unknown.
	 */
	public static boolean threeWay = ProgramProperties.getBooleanProperty("tvla.blur.threeWay", true);
	final static int threeWayMask = ProgramProperties.getBooleanProperty("tvla.blur.threeWay", true) ? 3 : 2;

	/** Create an empty canonical name.
	 */
	public Canonic() {
		name = new MutableBigInteger();
	}
	
	public Canonic(int size, boolean allFalse) {
		int miSize = size >> (MutableBigInteger.SHIFT - 1);
		if (miSize == 0 || (size & ((MutableBigInteger.WIDTH >> 1) - 1)) != 0)
			miSize++;
		name = new MutableBigInteger(miSize);
		if (allFalse) {
		    name.position = size;
		}
	}
	
	/** Copy the given canonical name.
	 * @param other the canonical name to copy.
	 */
	public Canonic(Canonic other) {
		name = new MutableBigInteger(other.name);
	}
	
	public Canonic(int pred_size) {
	    this(pred_size, false);
    }

    public boolean equals(Object o) {
		return ((Canonic) o).name.equals(this.name);
	}

	public int hashCode() {
		return name.hashCode();
	}

	/** Add the value to the canonical name.<BR>
	 * <B>NOTICE:</B> The values must always be added in the same order for 
	 * the canonicalization to be correct! 
	 * @param value The value to add.
	 */
	public void add(Kleene value) {
		name.addTwoBit(value.kleene() & threeWayMask);
		name.modified = true;
	}

    public void set(int index, Kleene value) {
        name.setTwoBit(index << 1, value.kleene() & threeWayMask);
        name.modified = true;
    }
	
	public int compareTo(Canonic other) {
		return this.name.compareTo(other.name);
	}

    /** Check that the meet of each Kleene value in the given canonical name
     * and the corresponding Kleene value of the current name is not bottom.
     */
    public boolean agreesWith(Canonic canonic) {
    	return name.agreesWith(canonic.name);
    }

    /** Check that the Kleene value in the current canonical name is less
     * than or equal to the corresponding Kleene value in the given canonical
     * w.r.t the information order. 
     */
    public boolean lessThanOrEqual(Canonic canonic) {
    	return name.lessThanOrEqual(canonic.name);
    }
    
	/** Returns a human-readable representation of the canonical name.
	 * @author Roman Manevich
	 * @since tvla-2-alpha 28.12.2001 Initial creation.
	 */
    // @note Unfortunately because of dynamic vocabulary there is no way
    // of doing this without knowing for which structure it is ment.
//	public String toString() {
//        Set<Predicate> predicates = Vocabulary.allUnaryRelPredicates();
//		return toString(predicates);
//	}

    public String toString() {
        return name.toString();
    }

    public long signature() {
        return name.signature();
    }
    
    public String toString(Set<Predicate> predicates) {
        StringBuffer result = new StringBuffer("(");

		boolean firstIteration = true;
		String kleeneName = null;
        Iterator<Predicate> revPredIter = predicates.iterator();
        int  position = 0;
		while (revPredIter.hasNext()) {
			Predicate predicate = revPredIter.next();
			int k = name.getTwoBit(position);
			switch (k) {
			case 0: kleeneName = "=0";
					break;
			case 1: kleeneName = "=1/2";
					break;
			case 2: kleeneName = "=1";
					break;
			default:
					throw new RuntimeException("Encountered an illegal truth value: " + k);
			}
			if (!kleeneName.equals("=0")) {
				if (!firstIteration)
					result.append(",");
				else
					firstIteration = false;
				result.append(predicate + kleeneName);
			}
			position += 2;
		}
		result.append(")");
		return result.toString();
    }

	/** This class is used to collect run-time information about the canonical
	 * names created by the analysis.
	 * @author Roman Manevich.
	 * @since tvla-2-alpha 28.12.2001 Initial creation.
	 */
	public static class CanonicNamesStatistics {
		public static boolean doStatistics;
		public static Set<Canonic> allCanonicNames = HashSetFactory.make();
		
		public static void dumpNames() {
			if (!doStatistics)
				return;
			Logger.println("#Different canonic names generated : " + allCanonicNames.size());
			
			if (ProgramProperties.getBooleanProperty("tvla.log.dumpCanonicNames", false)) {
				Logger.println("All canonic names:");
				Logger.println("------------------");
				for (Iterator<Canonic> iter = allCanonicNames.iterator(); iter.hasNext(); ) {
					Logger.println(iter.next());
				}
			}
		}
	}	
	
	/** A MutableBigInteger stores a stream of bits in an array of ints in
	 * big-endian format.
	 * The "size" field denotes the size of allocated storage, i.e.,
	 * the length of the array in ints.  The "position" field denotes the
	 * position of the next available bit or, alternatively, the length in bits
	 * of the currently stored number.  The WIDTH constant is the width of
	 * integer in bits (32 on PC).  In order to get the current position in the
	 * array, we compute (position >> SHIFT).  Similarly, the current position
	 * in the integer is calculated by: (WIDTH-1) - position & (WIDTH-1).
	 * The bits layout in the array is as follows:
	 * [0 1 ... 31] [32 32 ... 63] ...
	 * Each Kleene value is stored in two bits
	 * (<0 0>=false, <1 0>=true, <0 1>=unknown). For example, the values
	 * [0, 1/2, 1, 1/2] will be represented as: 
	 * [00011001 00000000 00000000 00000000] or: 0x19000000.
	 * The "position" field will equal 8, and the size will equal 1.
	 * 
	 * @author Igor Bogudlov
	 *
	 */
	final private class MutableBigInteger {
		static final int SHIFT = 5;
		static final int WIDTH = 1 << SHIFT; // 32
		static final int SHIFT_MASK = WIDTH - 1;
		static final long MASK = (1L << WIDTH) - 1;
		static final int UNKNOWN_MASK = (int)(MASK / 3);

		private int[] bits;
		private int size;
		private int position;
		
		public String toString() {
		    return size + "," + position + "," + bits;
		}
		
		private int savedHashCode;
		private boolean modified = true;
				
		MutableBigInteger() {
			this(1);
		}

        MutableBigInteger(int size) {
			this.size = size;
			bits = new int[size];
			position = 0;
		}

		MutableBigInteger(MutableBigInteger bi) {
			this(bi, bi.size);
		}
		
		MutableBigInteger(MutableBigInteger bi, int size) {
			assert(bi.size <= size);
			this.size = size;
			bits = new int[size];
			System.arraycopy(bi.bits, 0, bits, 0, bi.size);
			position = bi.position;
		}
		
		/** Returns the integer value that corresponds to the Kleene value
		 * stored at position <code>position</code>.
		 * 
		 * @param position The position from which to retrieve.
		 * @return An integer that corresponds to a Kleene value.
		 */
        final int getTwoBit(int position) {
            int i = position >> SHIFT;
            int j = SHIFT_MASK - 1 - (position & SHIFT_MASK);
            return (bits[i] >> j) & 3;
        }

        public void setTwoBit(int position, int value) {
            int i = position >> SHIFT;
            int j = SHIFT_MASK - 1 - (position & SHIFT_MASK);
            bits[i] = (bits[i] & ~(3 << j)) | (value << j);
        }
        
		final void addTwoBit(int value) {
			// Assume this will NOT be used interchangeably with add!
			int i = position >> SHIFT;
			if (i >= size) {
				MutableBigInteger newBI = new MutableBigInteger(this, 2 * size);
				bits = newBI.bits;
				size = newBI.size;
			}
			int j = SHIFT_MASK - 1 - (position & SHIFT_MASK);
			bits[i] |= (value << j);
			position += 2;
			modified = true;
		}
		
		void add(boolean bit) {
			int i = position >> SHIFT;
			if (i >= size) {
				MutableBigInteger newBI = new MutableBigInteger(this, 2 * size);
				bits = newBI.bits;
				size = newBI.size;				
			}
			if (bit) {
				int j = SHIFT_MASK - (position & SHIFT_MASK);
				bits[i] |= (1 << j);
			}
			++position;
			modified = true;
		}
		
		final public boolean equals(MutableBigInteger bi) {
			if (position != bi.position)
				return false;
			int[] bits = this.bits;
			int[] other_bits = bi.bits;
			for (int i = size; i != 0; ) {
				if (bits[--i] != other_bits[i])
					return false;
			}
			return true;
		}
		
		public int compareTo(MutableBigInteger bi) {
			if (position != bi.position) {
				return position - bi.position;
			}
			for (int i=0, _size=size; i < _size; ++i) {
				if (bits[i] != bi.bits[i])
					return bits[i] - bi.bits[i];
			}
			return 0;
		}
		
		/** @author Igor Bogudlov
		 */
	    final public boolean lessThanOrEqual(MutableBigInteger bi) {
	        if (position != bi.position)
	            return false;
	        int[] bits = this.bits;
	        int[] otherBits = bi.bits ;
	        
	        for (int i = size; i != 0; ) {
	            int mask = otherBits[--i] & UNKNOWN_MASK;
	            mask = ~(mask | (mask << 1)); // places of known bits
	            if ((bits[i] & mask) != (otherBits[i] & mask)) 
	                return false;
	        }
	        return true;
	    }
	    
	    /** 
	     * @param bi
	     * @return
	     * 
	     * @author Roman Manevich
	     */
	    final public boolean agreesWith(MutableBigInteger bi) {
	        if (position != bi.position)
	            return false;
	        for (int i = 0; i < position; i+=2) {
	        	Kleene kleene = Kleene.kleene( (byte) getTwoBit(i) );
	        	Kleene otherKleene = Kleene.kleene( (byte) bi.getTwoBit(i) );
	        	if (!Kleene.agree(kleene, otherKleene))
	        		return false;
	        }
	        return true;
	    }

		public long signature() {
			long h = position;
			int[] bits = this.bits;
			for (int i=0, _size=size; i < _size; ++i) {
				h = 47 * h + bits[i];
			}
			return h;
		}
	    
		public int hashCode() {
			if (!modified)
				return savedHashCode;
			int h = position;
			int[] bits = this.bits;
			for (int i=0, _size=size; i < _size; ++i) {
				h = 47 * h + bits[i];
			}
			savedHashCode = h;
			modified = false;
			return h;
		}
	}
}