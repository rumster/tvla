package tvla.core.base;

import tvla.core.TVSSet;

/** A factory that supplies implementations optimized for
 * very sparse structures..
 */
public class SparseTVSFactory extends BaseTVSFactory {
	public SparseTVSFactory() {
		super();
	}
	
	/** Returns an empty set.
	 */
	public TVSSet makeEmptySet () {
		TVSSet result = null;
		
		switch (joinMethod) {
		case JOIN_RELATIONAL: 
			result = new SparseHashTVSSet();
			break;
		}
		if (result == null)
			result = super.makeEmptySet();
		
		return result;
	}
}