package tvla.predicates;

import tvla.transitionSystem.Location;

/** A predicate designating a CFG node.
 * @author Eran Yahav
 * @since tvla-0.91
 */
public class LocationPredicate extends Predicate {
	private Location location;
	
	/** Create a new location-predicate.
	 * @param name The name of the predicate
	 * @param location The corresponding location
	 * note that location predicates are always unary predicates, and
	 * used as abstraction predicates.
     */
    LocationPredicate(String name, Location loc) {
		super(name,1,true); // a unary abstraction predicate
		location = loc;
    }
	
	/** Retruns the CFG node associated with this predicate.
	 */
	public Location getLocation() {
		return location;
	}
	
	public void setLocation(Location location) {
		this.location = location;
	}
}
