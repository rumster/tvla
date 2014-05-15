package tvla.util;

public final class IntObjectPair {
	int first;
	Object second;
	
	public IntObjectPair(int i, Object o) {
		first = i;
		second = o;
	}
	
	public final int first() {
		return first;
	}
	
	public final Object second() {
		return second;
	}
}
