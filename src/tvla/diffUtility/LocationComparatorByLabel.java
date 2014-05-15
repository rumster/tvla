package tvla.diffUtility;

import tvla.transitionSystem.Location;

/** Compares two locations using their labels.
 * Note: this comparator imposes orderings that are inconsistent with equals.
 * @author Roman Manevich
 * @since 19.12.2000 first created
 */
public class LocationComparatorByLabel implements java.util.Comparator {

	/** Compares its two arguments for order. 
	 * @param o1 the first location to be compared.
	 * @param o2 the second location to be compared.
	 * a negative integer, zero, or a positive integer as the first
	 * location's label is less than, equal to, or greater than the second
	 * location's label.
	 * @exception if the arguments' types are not Location.
	 */
	public int compare(Object o1,
					   Object o2) {
		Location l1 = (Location)o1;
		Location l2 = (Location)o2;
		return l1.label().compareTo(l2.label());
	}

	/** Indicates whether some other object is "equal to" this Comparator. 
	 */
	public boolean equals(Object obj) {
		return this.getClass() == obj.getClass();
	}		
}