package tvla.core.common;

import java.util.Comparator;
import java.util.Map;

import tvla.core.Canonic;

/** A comparator for nodes that uses their canonic names as the
 * criterion for comparison.
 * @author Roman Manevich.
 */
public class NodeComparatorByCanonicName implements Comparator {
	private Map nodeToCanonic;
	
	/** Constructs a comparator from a canonic map.
	 * @param nodeToCanonic A map from nodes to their canonic names.
	 */
	public NodeComparatorByCanonicName(Map nodeToCanonic) {
		this.nodeToCanonic = nodeToCanonic;
	}
	
	/** Compares its two arguments for order. 
	 */
	public int compare(Object o1, Object o2) {
		Canonic firstCanonic = (Canonic) nodeToCanonic.get(o1);
		Canonic secondCanonic = (Canonic) nodeToCanonic.get(o2);
		return firstCanonic.compareTo(secondCanonic);
	}
	
	/** Indicates whether some other object is "equal to" this Comparator. 
	 */
	public boolean equals(Object obj) {
		throw new UnsupportedOperationException();
	}
}