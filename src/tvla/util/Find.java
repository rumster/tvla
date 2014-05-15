package tvla.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

/** A generic lookup algorithm for collections.
 * This implementation does a linear search, so it is not very efficient.
 * @author Roman Manevich
 * @since 16/12/2000
 */
public class Find {
	/** Finds an object in a collection using the reference equality test.
	 */
	public static Object findObject(Collection c, Object o) {
		Iterator iter = c.iterator();
		while(iter.hasNext()) {
			Object tmpObj = iter.next();
			if (tmpObj == o)
				return tmpObj;
		}
		return null;
	}
	
	/** Finds an object in a collection using the Object.equals() test.
	 */
	public static Object findEqualObject(Collection c, Object o) {
		Iterator iter = c.iterator();
		while(iter.hasNext()) {
			Object tmpObj = iter.next();
			if (tmpObj.equals(o))
				return tmpObj;
		}
		return null;
	}
	
	/** Finds an object in a collection using the specified comparator.
	 */
	public static Object findEqual(Collection c, Object o, Comparator cmp) {
		Iterator iter = c.iterator();
		while(iter.hasNext()) {
			Object tmpObj = iter.next();
			if (cmp.compare(tmpObj, o) == 0)
				return tmpObj;
		}
		return null;
	}
}
