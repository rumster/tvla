package tvla.core;

import java.util.Map;
import java.util.Set;

/** An interfaces that adds extra support to a TVS for optimizing blur.
 * The implementing class should store two Map instances.
  */
public interface StoresCanonicMaps {
    /** Set the canonic and inverse canonic mapping for this structure. 
     * Will be cleared in case of a change in the structure. 
     * @author Tal Lev-Ami
     */
    public abstract void setCanonic(Map<Node,Canonic> canonic, Map<Canonic, Node> invCanonic);
    
	/** Get the canonic mapping for the structure. Returns null if not available.
	 * @author Tal Lev-Ami
	 */
    public abstract Map<Node,Canonic> getCanonic();
    
	/** Get the inverse canonic mapping for the structure.
	 *  Returns null if not available.
     * @author Tal Lev-Ami
	 */
    public abstract Map<Canonic, Node> getInvCanonic();
    
	/** Clear the saved canonic and inverse canonic mappings for this structure.
	 * @author Tal Lev-Ami
	 */
    public abstract void clearCanonic();
}
