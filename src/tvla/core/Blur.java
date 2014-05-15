package tvla.core;

import java.util.Collection;
import java.util.Iterator;

/** This class is an abstract base class for the algorithm Blur as defined in the
 * Shape Analysis article. 
 * @see tvla.core.TVS
 * @see tvla.core.Canonic
 * @see tvla.core.Node
 * @author Tal Lev-Ami
 */
public abstract class Blur {
	/** Blur the given structure in place. This is done by calculating the canonization of the
	 * structure nodes and merging nodes with the same canonic name. 
	 * @param structure The structure to blur.
	 */
	public abstract void blur(TVS structure);
	
	/** Blur all the structures in the given collection.
	 * @param structures The structures to blur.
	 */
	public void blurAll(Collection structures) {
		for (Iterator iter = structures.iterator(); iter.hasNext(); ) {
			TVS structure = (TVS) iter.next();
			blur(structure);
		}
	}
}
