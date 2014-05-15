package tvla.analysis.multithreading;

import tvla.transitionSystem.Action;
import tvla.transitionSystem.Location;

/** Represents the state-space explored by the analysis.
 * @author Eran Yahav
 */
public class StateSpace extends Location implements Comparable<Location> {
   /**
    * create a new statespace
    * @param label statespace label  
    * @param shouldPrint should the results be printed?
    */
	public StateSpace(String label, boolean shouldPrint) {
		super(label, shouldPrint);
		doJoin = true;
	}

	/**
	 * adds an action to the statespace (location)
	 * @param action - action to be added
	 * @param target - target location of the action
	 */
	public void addAction(Action action, String target) {
		super.addAction(action, target);
		action.setLocation(this);
	}
}