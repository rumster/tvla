package tvla.core.base;

import java.util.Map;

import tvla.core.Blur;
import tvla.core.StoresCanonicMaps;
import tvla.core.TVS;
import tvla.core.generic.GenericBlur;
import tvla.util.HashMapFactory;

/** An implementation of the Blur algorithm with a specialization
 * for structures that implement the StoresCanonicMaps interface.
 */
public class BaseBlur extends GenericBlur {
	/** A convenience instance.
	 */
	protected static BaseBlur defaultBaseBlur = new BaseBlur();
	
	public static void reset() {
		defaultBaseBlur = new BaseBlur();
	}
	
	public static BaseBlur getInstance() {
		return defaultBaseBlur;
	}
																  
	/** Blurs the specified structure in-place.
	 * @param structure A structure that implements the 
	 * StoresCanonicMaps interface.
	 */
	public void blur(TVS structure) {
		// Check if the structure is not already blurred.
		if (((StoresCanonicMaps) structure).getCanonic() != null)
			return;
		
		super.blur(structure);
		
		// Memoize the canonic maps.
		((StoresCanonicMaps) structure).setCanonic(canonicName, blurredInvCanonic);
	}
	
	/** Rebuilds the canonic and inverse-canonic maps and caches them
	 * in the structure.
	 */
	public void rebuildCanonicMaps(TVS structure) {
		Map canonicMap = HashMapFactory.make();
		Map inverseCanonicMap = HashMapFactory.make();
		GenericBlur genericBlur = (GenericBlur) GenericBlur.defaultGenericBlur;
		genericBlur.makeCanonicMapsForBlurred(structure, canonicMap, inverseCanonicMap);
		((StoresCanonicMaps) structure).setCanonic(canonicMap, inverseCanonicMap);
	}
}
