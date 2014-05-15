package tvla.analysis.multithreading;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import tvla.language.TVM.ActionAST;
import tvla.predicates.LocationPredicate;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.Location;

/** @author Eran Yahav.
 */
public class ProgramThread extends ProgramMethodBody {
	static final boolean DEBUG = true;
	
	public ProgramThread(String threadName, List<ActionAST> threadActions, Map<String, Location> parentProgram) {
		super(threadName, threadActions, parentProgram);		
		}
	
	private void dumpProrgamThread() {
		if (DEBUG) {
			// Print the program.
			
			System.out.println("digraph program {");
			for (Iterator locationIt = actions.values().iterator(); locationIt.hasNext(); ) {
				String colorString = new String("");
				Location location = (Location) locationIt.next();
				System.out.println(location.label() 
							+ " [label=\"" + location.label() + "\"];");
				for (int i = 0; i < location.getActions().size(); i++) {
					Action action = (Action) location.getActions().get(i);
					String target = (String) location.getTarget(i);
					if (!action.performUnschedule())  {
						colorString = new String(",color = red");
					}
						
					System.out.println(location.label() 
								+ "->" + target 
								+ "[label=\"" + action + "\"" + colorString + "];");
				}
			}
			System.out.println("}");
		}
	}
	
	
	/**
	 * returns the Map of thread actions.
	 * @return a Map of (String,Location)
	 */
	public Map<String, Location> getThreadActions() {
		return actions;
	}
	
	public String getEntryLabel() {
		return entry;
	}
	
	public LocationPredicate getEntryLocationPredicate() {
		return (LocationPredicate) ProgramMethodBody.locationToPredicate.get(actions.get(entry));
	}
}