package tvla.analysis.multithreading;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import tvla.differencing.Differencing;
import tvla.language.TVM.ActionAST;
import tvla.language.TVM.SetDefAST;
import tvla.predicates.LocationPredicate;
import tvla.predicates.Vocabulary;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.Location;
import tvla.util.HashMapFactory;

/** @author Eran Yahav.
 */
public class ProgramMethodBody {
	private static final boolean DEBUG = false;
	protected static Map<Location, LocationPredicate> locationToPredicate = HashMapFactory.make();

	protected String name;
	protected String entry;
	protected Map<String, Location> actions;
	protected Map<String, Location> parentProgram;
	//	protected List transitions;
	int currentPostOrderID;
	int currentPreOrderID;
	protected List<String> inOrder;
	/** list of threadActions */
	protected List<ActionAST> bodyActions;

	public ProgramMethodBody(
		String name,
		List<ActionAST> bodyActions,
		Map<String, Location> parentProgram) {
		Location targetLocation;

		this.name = name;
		/** a HashMap of (label,location) tuples  */
		actions = HashMapFactory.make();
		/** the list of thread labels */
		inOrder = new ArrayList<String>();
		/** list of transitions  */
		//		transitions = new ArrayList();
		/** set bodyActions  */
		this.bodyActions = bodyActions;
		this.parentProgram = parentProgram;

		/** first list item is the thread entry */
		entry = bodyActions.get(0).label();

		for (Iterator<ActionAST> i = bodyActions.iterator(); i.hasNext();) {
			ActionAST actionAST = i.next();
			String label = actionAST.label();
			String target = actionAST.next();

			Location location = actions.get(label);
			if (location == null) {
				Location parentLocation = parentProgram.get(label);
				if (parentLocation == null) {
					location = new Location(label, true);
				} else {
					location = parentLocation;
				}

				actions.put(label, location);
			}
			if (!inOrder.contains(label)) {
				inOrder.add(label);
			}

			if (actions.containsKey(target)) {
				targetLocation = actions.get(target);
			} else {
				Location parentTargetLocation =
					parentProgram.get(target);
				if (parentTargetLocation == null) {
					targetLocation = new Location(target, true);
				} else {
					targetLocation = parentTargetLocation;
				}
				//targetLocation = new Location(target, true);
				actions.put(target, targetLocation);
			}
		}

		prepareProgram(entry);
		createLocationPredicates();
	}

	public void compile() {

		for (Iterator<ActionAST> i = bodyActions.iterator(); i.hasNext();) {
			ActionAST actionAST = i.next();
			String label = actionAST.label();
			String target = actionAST.next();

			Action action = actionAST.def().getAction();
			action.performUnschedule(actionAST.performUnschedule);

			Location location = actions.get(label);
			Location targetLocation = actions.get(target);

			if (targetLocation == null) {
				throw new RuntimeException("Target is null");
			}

			location.addAction(action, target);
			Differencing.registerAction(action);

			//			Transition currTransition =
			//				new Transition(location, action, targetLocation);
			//			transitions.add(currTransition);
		}
		createLocationFormulae();
	}

	/** Creates location predicates according to thread locations.
	 */
	private void createLocationPredicates() {
		LocationPredicate currLocationPredicate;
		String currName;
		Location currLocation;
		for (Iterator<String> i = inOrder.iterator(); i.hasNext();) {
			String label = i.next();
			currName = "at[" + label + "]";
			currLocation = actions.get(label);
			currLocationPredicate =
				Vocabulary.createLocationPredicate(currName, currLocation);
			locationToPredicate.put(currLocation, currLocationPredicate);
		}
		// add to Labels set
		SetDefAST.addLabels(inOrder);
	}

	/** Create location predicate precondition and update formulae.
	 */
	private void createLocationFormulae() {
		Location currLocation;
		for (Iterator<String> i = inOrder.iterator(); i.hasNext();) {
			String label = i.next();

			currLocation = parentProgram.get(label);
			if (currLocation == null) {
				currLocation = actions.get(label);
			}

			int n = currLocation.getActions().size();

			for (int actionIt = 0; actionIt < n; actionIt++) {
				Action currAction = currLocation.getAction(actionIt);

				String targetLabel = currLocation.getTarget(actionIt);
				Location targetLocation =
					parentProgram.get(targetLabel);
				if (targetLocation == null) {
					targetLocation = actions.get(targetLabel);
				}

				if (DEBUG) {
					System.out.println(
						"*** source location"
							+ currLocation
							+ "-> target location"
							+ targetLocation);
				}

				LocationPredicate sourceLocationPredicate =
					locationToPredicate.get(currLocation);
				LocationPredicate targetLocationPredicate =
					locationToPredicate.get(targetLocation);

				currAction.createInternalFormulae(
					sourceLocationPredicate,
					targetLocationPredicate);
				currAction.createInternalPrecondition(sourceLocationPredicate);
			}
		}
	}

	private void dumpProrgamThread() {
		if (DEBUG) {
			// Print the program.

			System.out.println("digraph program {");
			for (Iterator<Location> locationIt = actions.values().iterator();
				locationIt.hasNext();
				) {
				String colorString = new String("");
				Location location = locationIt.next();
				System.out.println(
					location.label()
						+ " [label=\""
						+ location.label()
						+ "\"];");
				for (int i = 0; i < location.getActions().size(); i++) {
					Action action = (Action) location.getActions().get(i);
					String target = (String) location.getTarget(i);
					if (!action.performUnschedule()) {
						colorString = new String(",color = red");
					}

					System.out.println(
						location.label()
							+ "->"
							+ target
							+ "[label=\""
							+ action
							+ "\""
							+ colorString
							+ "];");
				}
			}
			System.out.println("}");
		}
	}

	private Location prepareProgram(final String entry) {
		for (Iterator<String> i = actions.keySet().iterator(); i.hasNext();) {
			String label = i.next();
			if (!inOrder.contains(label)) {
				inOrder.add(label);
			}
				
		}
		Location entryLocation = actions.get(entry);
		currentPostOrderID = 0;
		currentPreOrderID = 0;
		DFS(entryLocation);
		return entryLocation;
	}

	public void DFS(Location current) {
		current.postOrder = 0;
		current.preOrder = ++currentPreOrderID;
		for (int i = current.getTargets().size() - 1; i >= 0; i--) {
			Location nextLocation =
				actions.get(current.getTarget(i));
			if (nextLocation != null) {
				nextLocation.incoming++;
				if (nextLocation.postOrder == -1) {
					DFS(nextLocation);
					// This is a tree edge.
				} else if (current.preOrder < nextLocation.preOrder) {
					// This is a forward edge.
				} else if (nextLocation.postOrder == 0) {
					// This is a back edge
					nextLocation.hasBackEdge = true;
				} else {
					/// This is a cross edge.
				}
			}
		}
		current.postOrder = ++currentPostOrderID;
	}

	/**
	 * returns the Map of body actions.
	 * @return a Map of (String,Location)
	 */
	public Map<String, Location> getActions() {
		return actions;
	}

	public String getEntryLabel() {
		return entry;
	}

	public LocationPredicate getEntryLocationPredicate() {
		return locationToPredicate.get(actions.get(entry));
	}
}
