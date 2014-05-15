package tvla.core.generic;

import java.util.Collection; 
import java.util.Iterator;

public final class BitSet {
	private static final int SHIFT = 5;
	private static final int WIDTH = 1 << SHIFT;
	private static final int SHIFT_MASK = WIDTH - 1;
	
	int[] bits;
	
	public BitSet(int size) {
		bits = new int[(size >> SHIFT) + 1];
	}
	
	BitSet(int size, Collection<Identifiable> col) {
		this(size);
		for (Iterator<Identifiable> it = col.iterator(); it.hasNext();) {
			set(it.next());
		}
	}
	
	final void set(Identifiable o) {
		set(o.getId());
	}
	
	public final void set(int i) {
		bits[i >> SHIFT] |= 1 << (i & SHIFT_MASK);
	}
	
	final void addAll(BitSet bs) {
		for (int i = 0; i < bits.length; ++i) {
			bits[i] |= bs.bits[i];
		}
	}
	
	final boolean contains(Identifiable o) {
		return contains(o.getId());
	}
	
	public final boolean contains(int i) {
		int j = i & SHIFT_MASK;
		int m = (1 << j);
		return (bits[i >> SHIFT] & m) != 0;
	}
	
	public final int wordAtPosition(int i) {
		int idx = i >> SHIFT;
		int j = i & SHIFT_MASK;
		return (bits[idx] >>> j) | (bits[idx+1] << (WIDTH - j)); 
	}
	
	public void clear() {
		java.util.Arrays.fill(bits, 0);
	}
	
	public void setAll(BitSet other) {
		assert(bits.length == other.bits.length);
		System.arraycopy(other.bits, 0, bits, 0, bits.length);
	}
	
	int size() {
		int n = 0;
		for (int i = 0; i < bits.length; ++i) {
			int b = bits[i];
			for (int j = 0; j < WIDTH; ++j) {
				n += b & 1;
				b = (b >> 1);
			}
		}
		return n;
	}
};
