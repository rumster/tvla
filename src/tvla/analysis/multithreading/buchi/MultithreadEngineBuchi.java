package tvla.analysis.multithreading.buchi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.multithreading.MultithreadEngine;
import tvla.analysis.multithreading.StateSpace;
import tvla.analysis.multithreading.TVMCMacros;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.TVS;
import tvla.core.TVSFactory;
import tvla.core.TVSSet;
import tvla.core.assignments.Assign;
import tvla.core.generic.GenericFocus;
import tvla.exceptions.AnalysisHaltException;
import tvla.io.IOFacade;
import tvla.logic.Kleene;
import tvla.predicates.LocationPredicate;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.transitionSystem.Action;
import tvla.util.HashMapFactory;
import tvla.util.Logger;

/** An engine used for analyzing multi-threaded programs using 
 * a Buchi automaton.
 * @author Eran Yahav
 */
public class MultithreadEngineBuchi extends MultithreadEngine {
	/** extreme detailed debug mode 	 */
	private static final boolean XDEBUG = false;

	/**
	 * automaton representing the property to be verified
	 */
	protected BuchiAutomaton property;
	/**
	 * stack used for cycle detection
	 */
	protected Stack cycleStack;
	/**
	 * stack used in state-space explocation
	 */
	protected Stack searchStack;

	/**
	 * A colored structure records the exploration status of a structure.
	 * It is a wrapper of a regular structure that allows us to track which 
	 * structures have been visited, and which structures have the buchi automaton 
	 * in an accepting states.
	 * @author yahave
	 */
	public class ColoredStructure {
		public HighLevelTVS structure;
		public boolean marked;
		public boolean accepting;

		public ColoredStructure(
			HighLevelTVS s,
			boolean marked,
			boolean accepting) {
			this.structure = s;
			this.marked = marked;
			this.accepting = accepting;
		}
	}

	/**
	 * instantiate a new engine
	 */
	public MultithreadEngineBuchi() {
		super();
	}

	/**
	 * get a list of actions that are enabled in the given structure
	 * @param structure - structure to check enabled actions in
	 * @return a list of actions enabled in the given structure 
	 * (actions for which the precondition is potentially satisfied).
	 */
	public List getEnabledActions(TVS structure) {
		List result = new ArrayList();
		for (Iterator i = TVMCMacros.allThreadNodes(structure); i.hasNext();) {
			Node n = (Node) i.next();
			if (structure.eval(Vocabulary.ready, n) == Kleene.trueKleene) {
				// n is a ready thread, add its actions
				// iterate over location predicates	   
				for (Iterator li = Vocabulary.locationPredicates.iterator();
					li.hasNext();
					) {
					LocationPredicate lp = (LocationPredicate) li.next();
					if (structure.eval(lp, n) == Kleene.trueKleene) {
						result.addAll(lp.getLocation().getActions());
					} // endif
				} // end for
			} // end if ready
		} // end for all threads
		return result;
	}

	/**
	 * Get the enabled actions that could be taken by the proeprty in a given structure
	 * @param structure - the given structure
	 * @return a list of property actions that are enabled in the given structure
	 */
	public List getEnabledBuchiActions(TVS structure) {
		List result = new ArrayList();
		// find set of (possibly) enabled Buchi automaton transitions
		for (Iterator np = property.allStatePredicates(); np.hasNext();) {
			Predicate currNullary = (Predicate) np.next();
			///System.err.println("Nullary state predicate " + np);
			if (!structure.eval(currNullary).equals(Kleene.falseKleene)) {
				result.addAll(property.transitions(currNullary));
			}
		}
		return result;
	}

	/** sets the buchi auatomaton specifying the property 
	 * @param ba - property automaton 
	 * */
	public void setProperty(BuchiAutomaton ba) {
		property = ba;
	}

	/** is the given abstract state (TVS) accepting? 
	 * @param s - a given state
	 * @return true if the given state is accepting  
	 */
	public boolean isAccepting(TVS s) {
		for (Iterator np = property.allAcceptingStatePredicates();
			np.hasNext();
			) {
			Predicate currNullary = (Predicate) np.next();
			if (!s.eval(currNullary).equals(Kleene.falseKleene)) {
				return true;
			}
		}
		return false;
	}

	/** does the given structure (s) already exist on the search stack? 
	 * @param stack - serach stack 
	 * @param s - given structure
	 * @return true if given structure already exists on the stack, false otherwise
	 * */
	public boolean structureContained(Stack stack, HighLevelTVS s) {
		TVSSet set = TVSFactory.getInstance().makeEmptySet();
		set.mergeWith(s);

		for (Iterator i = stack.iterator(); i.hasNext();) {
			ColoredStructure cs = (ColoredStructure) i.next();
			HighLevelTVS delta = set.mergeWith(cs.structure);
			if (delta == s) {
				return true;
			}

		}
		return false;
	}

	/** debug: output the stack 
	 * @param stack - stack to be printed 
	 */
	public void dumpColoredStack(Stack stack) {
		for (Iterator i = stack.iterator(); i.hasNext();) {
			ColoredStructure cs = (ColoredStructure) i.next();
			IOFacade.instance().printStructure(cs.structure, "");
		}
	}
	
	/** debug: output the stack 
	 * @param stack - stack to be printed 
	 */
	public void dumpStack(Stack stack) {
		for (Iterator i = stack.iterator(); i.hasNext();) {
			TVS s = (TVS) i.next();
			IOFacade.instance().printStructure(s, "");
		}
	}

	/***
	 * Apply with update of Buchi automaton state.
	 * Apply the action on the structure at program location label,
	 * returnning all possible resulting structures.
	 * @param messages Map with messages generated for structures.  Must be initialized.
	 * @return a collection of all possible structures resulting from action evaluation. 
	 */

	public Collection apply(
		Action action,
		Action buchiAction,
		HighLevelTVS structure,
		String label,
		Map messages) {
		Collection answer = new ArrayList();

		/**
		* Steps of applying an action with Buchi action:
		* 1. focus
		* 2. coerce (if coerce-after-focus is enabled)
		* 3. evaluate action precondition
		* 4. perform action update 
		* 5. coerce
		* (5a. focus buchi-action precondition, currently not performed)
		* (5b. coerce after buchi-action focus, currently not performed)
		* 6. evaluate buchi-action precondition
		* 7. perform buchi-action update
		* 8. coerce
		* 9. blur
		*/

		// focus
		Collection focusResult;
		if (doFocus) {
			status.startTimer(AnalysisStatus.FOCUS_TIME);
			focusResult =
				GenericFocus.focus(structure, action.getFocusFormulae());
			status.stopTimer(AnalysisStatus.FOCUS_TIME);
		} else {
			focusResult = Collections.singleton(structure);
		}

		for (Iterator focusIt = focusResult.iterator(); focusIt.hasNext();) {
			HighLevelTVS focusedStructure = (HighLevelTVS) focusIt.next();
			// coerce if coerce-after-focus is enabled
			if (doCoerceAfterFocus) {
				if (AnalysisStatus.debug) {
					IOFacade.instance().printStructure(
						structure,
						"Executing " + label + " " + action);
					if (doFocus) {
						IOFacade.instance().printStructure(
							focusedStructure,
							"After Focus " + label + " " + action);
					}

				}

				status.startTimer(AnalysisStatus.COERCE_TIME);
				boolean valid = focusedStructure.coerce();
				status.stopTimer(AnalysisStatus.COERCE_TIME);
				if (!valid) {
					continue;
				}

			}
			// evaluate precondition
			status.startTimer(AnalysisStatus.PRECONDITION_TIME);
			Collection assigns = action.checkPrecondition(focusedStructure);
			status.stopTimer(AnalysisStatus.PRECONDITION_TIME);

			for (Iterator assignIt = assigns.iterator(); assignIt.hasNext();) {
				Assign assign = (Assign) assignIt.next();

				if (action.checkHaltCondition(focusedStructure, assign)) {
					throw new AnalysisHaltException(label, action);
				}

				Collection newMessages =
					action.reportMessages(focusedStructure, assign);
				if (!newMessages.isEmpty()) {
					messages.put(focusedStructure, newMessages);
				}

				if (AnalysisStatus.debug
					&& (!assign.isEmpty() || !doCoerceAfterFocus)) {
					IOFacade.instance().printStructure(
						structure,
						"Executing "
							+ label
							+ " "
							+ action
							+ (assign.isEmpty() ? "" : " " + assign));
					if (doFocus) {
						IOFacade.instance().printStructure(
							focusedStructure,
							"After Focus "
								+ label
								+ " "
								+ action
								+ (assign.isEmpty() ? "" : " " + assign));
					}
				}
				// evaluate update formulae
				status.startTimer(AnalysisStatus.UPDATE_TIME);
				HighLevelTVS result = action.evaluate(focusedStructure, assign);
				status.stopTimer(AnalysisStatus.UPDATE_TIME);

				if (AnalysisStatus.debug) {
					IOFacade.instance().printStructure(
						result,
						"After Update\\n"
							+ label
							+ " "
							+ action
							+ (assign.isEmpty() ? "" : " " + assign));
				}
				// coerce (if coerce after update is enabled)
				if (doCoerceAfterUpdate) {
					status.startTimer(AnalysisStatus.COERCE_TIME);
					boolean valid = result.coerce();
					status.stopTimer(AnalysisStatus.COERCE_TIME);
					if (!valid) {
						continue;
					}

					if (AnalysisStatus.debug) {
						IOFacade.instance().printStructure(
							result,
							"After Coerce\\n"
								+ label
								+ " "
								+ action
								+ (assign.isEmpty() ? "" : " " + assign));
					}
				}

				// evaluate Buchi-action precondition (on result structure)
				Collection buchiAssigns = buchiAction.checkPrecondition(result);
				for (Iterator bai = buchiAssigns.iterator(); bai.hasNext();) {
					Assign buchiAssign = (Assign) bai.next();
					HighLevelTVS resultWithBuchi =
						buchiAction.evaluate(result, buchiAssign);
					if (doCoerceAfterUpdate) {
						boolean valid = resultWithBuchi.coerce();
						if (!valid) {
							continue;
						}

						if (AnalysisStatus.debug) {
							IOFacade.instance().printStructure(
								resultWithBuchi,
								"After Coerce\\n"
									+ label
									+ " "
									+ action
									+ (buchiAssign.isEmpty()
										? ""
										: " " + buchiAssign));
						}
					}

					// blur 
					if (AnalysisStatus.debug) {
						status.startTimer(AnalysisStatus.BLUR_TIME);
						resultWithBuchi.blur();
						status.stopTimer(AnalysisStatus.BLUR_TIME);
						IOFacade.instance().printStructure(
							resultWithBuchi,
							"After Blur\\n"
								+ label
								+ " "
								+ action
								+ (buchiAssign.isEmpty()
									? ""
									: " " + buchiAssign));
					} // end if debug

					answer.add(resultWithBuchi);
				} // end for buchiAssigns
			}
		}
		return answer;
	}

	/**
	 * evaluate
	 * State space exploration
	 */
	public void evaluate(Collection<HighLevelTVS> initial) {
		long cycleDepth = 0;
		long cycleLength = 0;
		boolean cycleDetected = false;
		/** the search stack is a stack of configurations (colored-structures) */
		searchStack = new Stack();
		StateSpace stateSpace = new StateSpace("State_Space", true);
		init();

		if (XDEBUG) {
			System.err.println("evaluate " + initial + ", " + property);
		}

		/**
		* initialize
		* for each s in Initial: push(s)
		*/
		for (Iterator i = initial.iterator(); i.hasNext();) {
			HighLevelTVS s = (HighLevelTVS) i.next();
			searchStack.push(new ColoredStructure(s, false, isAccepting(s)));
		}

		maxStackDepth = searchStack.size();

		/**
		* explore
		*/
		try {
			OUTER : while (!searchStack.isEmpty()) {
				// structure = pop()
				ColoredStructure cs = (ColoredStructure) searchStack.peek();
				HighLevelTVS structure = cs.structure;
				if (cs.marked) {
					if (cs.accepting) {
						if (detectCycle(searchStack, structure)) {
							cycleDetected = true;
							break OUTER;
						}
					}
					searchStack.pop();
				}
				if (XDEBUG) {
					Logger.println("Structure popped" + searchStack.size());
				}

				// if structure is not a member of the statespace
				status.startTimer(AnalysisStatus.JOIN_TIME);
				structure = stateSpace.join(structure);
				boolean delta = (structure != null);
				status.stopTimer(AnalysisStatus.JOIN_TIME);
				if (XDEBUG) {
					Logger.println("Delta exists =" + delta);
				}

				if (delta) {
					// add structure to the statespace
					status.numberOfStructures++;
					updateStatus();
					if (status.shouldFinishAnalysis()) {
						break OUTER;
					}

					// find set of enabled actions
					// global actions are always enabled, so they are initially
					// insereted into "enabledActions"
					List enabledActions = getEnabledActions(structure);
					List enabledBuchiActions =
						getEnabledBuchiActions(structure);
					// for each enabled action
					for (int actionIt = 0;
						actionIt < enabledActions.size();
						actionIt++) {
						Action action = (Action) enabledActions.get(actionIt);
						String label = action.location().label();

						Map messages = HashMapFactory.make(0);
						// for each possibly enabled Buchi action
						for (Iterator eba = enabledBuchiActions.iterator();
							eba.hasNext();
							) {
							BuchiTransition bt = (BuchiTransition) eba.next();
							Action buchiAction = bt.action();

							if (XDEBUG) {
								Logger.println(
									"Action precondition "
										+ action.getPrecondition());

							}

							Collection results =
								apply(
									action,
									buchiAction,
									structure,
									label,
									messages);
							if (XDEBUG) {
								Logger.println("Action Applied");
							}

							status.numberOfMessages
								+= stateSpace.addMessages(messages);
							if (status.shouldFinishAnalysis()) {
								break OUTER;
							}
								
							for (Iterator resultIt = results.iterator();
								resultIt.hasNext();
								) {
								HighLevelTVS result =
									(HighLevelTVS) resultIt.next();
								searchStack.push(
									new ColoredStructure(
										result,
										false,
										isAccepting(result)));
								if (XDEBUG) {
									Logger.println(
										"Structure pushed"
											+ searchStack.size());
								}
								stackDepth = searchStack.size();
								if (stackDepth > maxStackDepth) {
									maxStackDepth = stackDepth;
								}
							} // end for results
						} // end for each buci action
					} // end for actions
				} // end if delta exists
				cs.marked = true;
			} // end while
			if (!AnalysisStatus.terse && cycleDetected) {
				System.err.println("Cycle was found");
				cycleDepth = searchStack.size();
				cycleLength = cycleStack.size();
			}
		} catch (AnalysisHaltException ahe) {
			System.err.println(ahe.getMessage());
		}
		status.stopTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);

		/** print statespace */
		if (!stateSpace.messages.isEmpty()) {
			IOFacade.instance().printLocation(stateSpace);
		}
			

		printStateSpaceStatistics();

		System.out.println(
			"digraph stateSpace {\n \"" + stateSpace.label() + "\";\n}\n");
		for (Iterator res = stateSpace.structures.iterator(); res.hasNext();) {
			TVS structure = (TVS) res.next();
			IOFacade.instance().printStructure(structure, "");
		}
		if (cycleDetected) {
			System.out.println(
				"digraph cycleTrace {\n \"" + "cycle trace" + "\";\n}\n");
			dumpColoredStack(searchStack);
			dumpStack(cycleStack);
			Logger.println("A cycle was detected");
			Logger.println("Cycle depth " + cycleDepth);
			Logger.println("Cycle length " + cycleLength);
		}

		printStatistics();
		statistics.doStatistics();
	}
	/***
	 * detectCycle
	 * @param aSearchStak - a given search stack
	 * @param origin - structure from which cycle-detection begins
	 * @return true if a cycle is found, false otherwise
	 */
	public boolean detectCycle(Stack aSearchStack, TVS origin) {
		cycleStack = new Stack();
		StateSpace cycleSpace = new StateSpace("cycleSpace", false);

		// init - push origin
		cycleStack.push(origin);
		try {
			while (!cycleStack.isEmpty()) {
				HighLevelTVS structure = (HighLevelTVS) cycleStack.pop();
				structure = cycleSpace.join(structure);
				boolean delta = (structure != null);
				if (delta) {
					// find set of enabled actions
					List enabledActions = getEnabledActions(structure);
					List enabledBuchiActions =
						getEnabledBuchiActions(structure);

					// for each enabled action
					for (int actionIt = 0;
						actionIt < enabledActions.size();
						actionIt++) {
						Action action = (Action) enabledActions.get(actionIt);
						String label = action.location().label();
						Map messages = HashMapFactory.make(0);
						// for each possibly enabled Buchi action
						for (Iterator eba = enabledBuchiActions.iterator();
							eba.hasNext();
							) {
							BuchiTransition bt = (BuchiTransition) eba.next();
							Action buchiAction = bt.action();
							Collection results =
								apply(
									action,
									buchiAction,
									structure,
									label,
									messages);
							cycleSpace.addMessages(messages);
							for (Iterator resultIt = results.iterator();
								resultIt.hasNext();
								) {
								HighLevelTVS result =
									(HighLevelTVS) resultIt.next();
								// check if the search-stack already contains it
								if (structureContained(aSearchStack, result)) {
									return true;
								} else {
									cycleStack.push(result);
								}
							} // end for results
						} // end for each buci action
					} // end for actions
				} // end if delta exists
			} // end while
		} catch (AnalysisHaltException ahe) {
			System.err.println(ahe.getMessage());
		}
		return false;
	} // end method detectCycle

	/** Prints statistical information about the state space structures.
	 * @author Eran Yahav.
	 * @since 21.7.2001 Moved here from outside the evaluate method.
	 */
	protected void printStateSpaceStatistics() {
		Logger.println();
		if (!stateSpace.structures.isEmpty()) {
			Logger.print(
				stateSpace.label()
					+ ": "
					+ stateSpace.structures.size()
					+ " structures");
			int maxSatisfy = 0;
			for (Iterator res = stateSpace.structures.iterator();
				res.hasNext();
				) {
				TVS structure = (TVS) res.next();
				int satisfy = 0;
				for (Iterator predIt =
					Vocabulary.allUnaryPredicates().iterator();
					predIt.hasNext();
					) {
					Predicate pred = (Predicate) predIt.next();
					satisfy += structure.numberSatisfy(pred);
				}
				for (Iterator predIt =
					Vocabulary.allBinaryPredicates().iterator();
					predIt.hasNext();
					) {
					Predicate pred = (Predicate) predIt.next();
					satisfy += structure.numberSatisfy(pred);
				}
				if (satisfy > maxSatisfy) {
					maxSatisfy = satisfy;
				}
			}
			Logger.println(" max graph=" + maxSatisfy + "");
		}
	}
}
