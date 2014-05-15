/*
 * Created on Aug 28, 2003
 */
package tvla.transitionSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Logger;

/**
 * @author Eran Yahav eyahav
 */
public class RelevantAnalysisGraph extends AnalysisGraph {

	/**
	 * map from relevant-string to a Set of source-location labels
	 * for the relevant-string
	 */
	protected Map relevantLocations = HashMapFactory.make();

	/**
	 * a set of the _current_ relevant location labels
	 * this set is determined by the current specialization applied
	 */
	protected Set currentRelevantLocationLabels = HashSetFactory.make();

	/**
	 * map from sourceLabel to a list of relevant actions at that label
	 */
	protected Map relevantActions = HashMapFactory.make();

	/**
	 * map from sourceLabel to list of original actions at that label
	 */
	protected Map originalActions = HashMapFactory.make();


	/**
	 * a set of new targets added by specialization only. 
	 * these should be removed when the graph is reverted 
	 * at least to avoid warning on unreachable locations.
	 */
	protected Set newTargets = HashSetFactory.make();

	/**
	 * Add an action to a RelevantAnalysisGraph 
	 * Adds the basic action (without relevance id) to super.
	 */
	public void addAction(
		String source,
		Action action,
		String target,
		String relevant) {
		if (relevant.equals("")) {
			super.addAction(source, action, target);
			addOriginalAction(new ActionData(source, action, target));
		} else {
			RelevantActionData curr =
				new RelevantActionData(source, action, target, relevant);
			addRelevantLocationLabel(relevant, source);
			addRelevantAction(curr);
		}
	}

	private void addRelevantLocationLabel(String relevant, String source) {
		if (!relevantLocations.containsKey(relevant))
			relevantLocations.put(relevant, HashSetFactory.make());

		((Collection) relevantLocations.get(relevant)).add(source);

	}

	private void addOriginalAction(ActionData actionData) {
		if (!originalActions.containsKey(actionData.source))
			originalActions.put(actionData.source, new ArrayList());

		((Collection) originalActions.get(actionData.source)).add(actionData);
	}

	private void addRelevantAction(RelevantActionData relevantAction) {
		if (!relevantActions.containsKey(relevantAction.source))
			relevantActions.put(relevantAction.source, new ArrayList());

		((Collection) relevantActions.get(relevantAction.source)).add(
			relevantAction);
	}

	/**
	 * returns a collection of labels for the given relevant name 
	 * (usually the name corresponds to a class name) 
	 * @return collection of labels for the given relevant name
	 */
	public Collection labelsForName(String relevantName) {
		return (Collection) relevantLocations.get(relevantName);
	}

	/**
	 * returns a collection of all the relevant names specified.
	 * usually relevant-names correspond to class names
	 * @return
	 */
	public Collection relevantNames() {
		return (Collection) relevantLocations.keySet();
	}

	/**
	 * specialized the graph to one in which the specified relevant locations 
	 * have their relevant-actions at the location.
	 * @param relevantLocations
	 * @return
	 */
	public void specializeGraph(Collection relevantLocations) {
		
		currentRelevantLocationLabels.addAll(relevantLocations);
		
		for (Iterator it = relevantLocations.iterator(); it.hasNext();) {
			String currLocationLabel = (String) it.next();

			Collection currActions =
				(Collection) relevantActions.get(currLocationLabel);

			boolean firstTime = true;
			for (Iterator actionIt = currActions.iterator();
				actionIt.hasNext();
				) {

				RelevantActionData rad = (RelevantActionData) actionIt.next();

				if (rad == null)
					continue;
				Location sourceLocation = (Location) program.get(rad.source);
				if (sourceLocation != null && firstTime) {
					sourceLocation.clearLocation();
					sourceLocation.initLocationOrder();
					firstTime = false;
				}
				
				if (!program.containsKey(rad.target)) {
					newTargets.add(rad.target);
					Logger.println("new target added " + rad.target + " for " + rad.source);
				}
				
				super.addAction(rad.source, rad.action, rad.target);
				
				Location progLocation = (Location) program.get(rad.source);
				
			}
		}
	}

	/**
	 * is the given label relevant?
	 * @param locationLabel - label of the location in question
	 * @return true when the label corresponds to a relevant location
	 */
	public boolean isRelevant(String locationLabel) {
		return currentRelevantLocationLabels.contains(locationLabel);
	}

	/**
	 * reverts the graph with original actions at the specified 
	 * locations
	 * 
	 * @param relevantLocations
	 * @return
	 */
	public void revertGraph(Collection relevantLocations) {
		
		currentRelevantLocationLabels.removeAll(relevantLocations);
		
		for (Iterator it = relevantLocations.iterator(); it.hasNext();) {
			String currLocationLabel = (String) it.next();

			Collection currActions =
				(Collection) originalActions.get(currLocationLabel);

			boolean firstTime = true;
			for (Iterator actionIt = currActions.iterator();
				actionIt.hasNext();
				) {

				ActionData ad = (ActionData) actionIt.next();

				if (ad == null)
					continue;
				Location sourceLocation = (Location) program.get(ad.source);
				if (sourceLocation != null && firstTime) {
					sourceLocation.clearLocation();
					sourceLocation.initLocationOrder();
					firstTime = false;
				}
				super.addAction(ad.source, ad.action, ad.target);

				Location progLocation = (Location) program.get(ad.source);
				
				
				if (!newTargets.isEmpty()) {
					for(Iterator targetsIt=newTargets.iterator();targetsIt.hasNext();) {
						String currTarget = (String)targetsIt.next();
						Logger.println("removing new target " + currTarget);
						program.remove(currTarget);
						inOrder.remove(currTarget);
					}
					newTargets.clear();
				}
				
			}
		}
	}

		public class ActionData {
			public String source;
			public Action action;
			public String target;

			public ActionData(String source, Action action, String target) {
				this.source = source;
				this.action = action;
				this.target = target;
			}

		}

		public class RelevantActionData {
			public String source;
			public Action action;
			public String target;
			public String relevant;

			public RelevantActionData(
				String source,
				Action action,
				String target,
				String relevant) {
				this.source = source;
				this.action = action;
				this.target = target;
				this.relevant = relevant;
			}

			/**
			 * is this relevant action equals to another one?
			 * @TODO: check whether we need a finer notion of action equality
			 */
			public boolean equals(Object other) {
				if (!(other instanceof RelevantActionData))
					return false;
				RelevantActionData otherRelevantAction =
					(RelevantActionData) other;
				return (
					source.equals(otherRelevantAction.source)
						&& (action == otherRelevantAction.action)
						&& target.equals(otherRelevantAction.target)
						&& relevant.equals(otherRelevantAction.relevant));
			}

			public int hashCode() {
				return source.hashCode()
					+ action.hashCode()
					+ 31 * target.hashCode()
					+ relevant.hashCode();
			}
		}
	}
