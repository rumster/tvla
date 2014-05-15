package tvla.core.functional;

import java.util.Map;

import tvla.util.HashMapFactory;

/* HashCons: Enables hash-consing: i.e., the creation of unique representatives
 * of any structured value. This utilizes Java collection classes and relies
 * on the convention of Object::equals and Object::hashCode; thus, the objects
 * that are to be hash-consed must have appropriate implementations of these
 * methods, which will determine the equality of objects.
 *
 * Users can create distinct instances of HashCons as desired, or use the
 * static methods to utilize a single common global hash-cons table.
 */

public class HashCons {
	public Map map;

   public Object value(Object key, Object val) {
		Object result = map.get(key);
		if (result == null) {
		  	map.put(key, val);
			return val;
		} else
			return result;
	}

   public Object value(Object objToBeHashConsed) {
		return value (objToBeHashConsed, objToBeHashConsed);
		/*
		Object result = map.get(objToBeHashConsed);
		if (result == null) {
		  	map.put(objToBeHashConsed, objToBeHashConsed);
			return objToBeHashConsed;
		} else
			return result;
	   */
	}

	/*
	public Object value(int i, int j, Object val) {
		long num1 = i;
		long num2 = j;
		long pair = (num2 << 32) + num1;
		return value( new Long(pair), val );
	}
	*/

	public HashCons() {
		map = HashMapFactory.make(10001);
	}

	// Users can use a fixed global hash-cons table, if desired:
	static public HashCons globalTable = new HashCons();

	public static Object instance(Object obj) {
		return globalTable.value(obj);
	}

}
