/*
 * File: Cache.java 
 * Created on: 12/10/2004
 */

package tvla.util;

import java.util.Map;

/** Interface for a cache.
 * a non-positive maxCapacity means no limits.
 *
 *  * @author maon
 */
public interface Cache extends Map {
	public int getMaxCapacity();
}
