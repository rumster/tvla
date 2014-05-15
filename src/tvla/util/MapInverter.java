package tvla.util;

import java.util.Map;
import java.util.Set;

/** Creates an inverse map from a specified one-to-one map.
 * @author Roman Manevich.
 */
public class MapInverter {
	/** Inverts the specified map and stores the result in the
	 * second argument.
	 * @param m A one-to-one map.
	 * @param i The resulting one-to-one map is stored in this argument.
	 */
	public static <K,V> void invertMap(Map<K,V> m, Map<V,K> i) {
		assert (m != null && i != null); // precondition
		for (Map.Entry<K,V> entry : m.entrySet()) {
			i.put(entry.getValue(), entry.getKey());
		}
	}

	public static <K, V> void invertMapNonInjective(Map<K, V> mapping, Map<V, Set<K>> inverse) {
		assert (mapping != null && inverse != null); // precondition
		for (Map.Entry<K,V> entry : mapping.entrySet()) {
			Set<K> bucket = inverse.get(entry.getValue());
			if (bucket == null) {
				bucket = HashSetFactory.make();
				inverse.put(entry.getValue(), bucket);
			}
			bucket.add(entry.getKey());
		}		
	}
}
