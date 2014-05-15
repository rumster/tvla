/*
 * Created on Feb 28, 2005
 *
 */
package tvla.analysis;

import java.util.Collection;
import java.util.Map;

import tvla.core.TVS;
import tvla.util.graph.Graph;

/**
 * @author Roman Manevich
 *
 */
public interface TransitionRelation {
    /** Returns the locations in the CFG.
     * 
     * @return A set of <code>Locations</code>. 
     */
    public Collection getLocations();
    
    /** A graph (V,E) where V is a set of abstract states (structure/location)
     * pairs and E is a set of labeled edges describing how one state
     * evolved into another state.
     * 
     * @return
     */
    public Graph getConfigurationGraph();
    
    /**
     * @return Returns (an unmodifiable) map of locations to 
     * a collection of abstract states. 
     * 
     * This view of the transition relation is not maintained 
     * when the transition relation is updated.
     */    
    public Map getAbstractStatesAtLocations();
    
    /**
     * Assigns a unique Id to every abstract state in 
     * the transition relation. 
     * The uniqueness is not ensured if the transition relation is later updated
     * Modification to the transition relation invalidates the ids.
     * @author maon
     */
    public void assignIds();
    
    /**
     * Returns the id of a given abstract state.
     * A call to this method must be preceded by a call to assignIds that
     * is not invalidated.
     * */
    public long getId(AbstractState as);
    
    /**
     * Returns a partial map from abstract states to messages.
     */
    public Map getMessages();
    
    /**
     * Dumps the transition relation into a file 
     * @author maon
     */
    public void dump();
	
    public static interface AbstractState {
        public TVS getStructure();
        public Object getLocation();
    }
    
    public static interface AbstractTransition {
    }
}
