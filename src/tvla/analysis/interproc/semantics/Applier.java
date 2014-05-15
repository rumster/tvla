/*
 * File: ActionApplier.java 
 * Created on: 16/10/2004
 */

package tvla.analysis.interproc.semantics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.IActionApplier;
import tvla.core.Coerce;
import tvla.core.Focus;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.TVS;
import tvla.core.TVSFactory;
import tvla.core.assignments.Assign;
import tvla.core.common.ModifiedPredicates;
import tvla.io.IOFacade;
import tvla.logic.Kleene;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.Location;
import tvla.transitionSystem.PrintableProgramLocation;
import tvla.util.Filter;
import tvla.util.Logger;
import tvla.util.ProgramProperties;
import tvla.util.StringUtils;

/** Applies an anction to a HighLevelTVS and returns 
 * a collection of the resulting HighLevelTVSs.
 * Currently implemented as a subtype of engine
 * This class is an adapter for the ActionEngine class
 * 
 * TODO Implemented by inheritance -ugly - to fix by moving code of 
 * apply here / cleaning Engine 
 * @author maon
 */
public final class Applier {
	final private ActionEngine applier;
	final private String marker;

	public Applier(
			AnalysisStatus totalStatus,
			boolean doFocus,
			boolean doCoerceAfterFocus,
			boolean doCoerceAfterUpdate,
			boolean doBlur,
			boolean freezeStructuresWithMessages,
			boolean breakIfCoerceAfterUpdateFailed,
			String marker) {
		super();
		applier = new ActionEngine (
				totalStatus,
				doFocus,
				doCoerceAfterFocus,
				doCoerceAfterUpdate,
				doBlur,
				freezeStructuresWithMessages,
				breakIfCoerceAfterUpdateFailed);	
		
		this.marker = marker;
	}
	
	public Collection apply(ActionInstance action, TVS tvs, PrintableProgramLocation processedLocation, Map msgs) {
		assert (tvs instanceof HighLevelTVS);
		return applier.apply(
				action.getAction(), 
				tvs, 
				processedLocation, 
				msgs);
	}
	
	public AnalysisStatus getAnalysisStatus() {
		return this.applier.getAnalysisStatus();
	}
	
	public AnalysisStatus getTotalAnalysisStatus() {
		return this.applier.getTotalAnalysisStatus();
	}
	
	public boolean setPrintStrucutreIfCoerceAfetFocusFailed(boolean shouldPrint) {
		return this.applier.setPrintStrucutreIfCoerceAfetFocusFailed(shouldPrint);
	}

	
	public class ActionEngine implements IActionApplier {
		protected boolean doBlur = true;
		protected boolean doFocus = true;
		protected boolean doCoerceAfterFocus = false;
		protected boolean doCoerceAfterUpdate = true;
		protected boolean freezeStructuresWithMessages = false;
		protected boolean breakIfCoerceAfterUpdateFailed = false;		
		protected boolean printStrucutreIfCoerceAfetFocusFailed = false;
		
		
		protected Action currentAction;
		protected final AnalysisStatus status;
		protected final AnalysisStatus totalStatus;
		//  TODO add monitoring of SpaceStatistics: protected SpaceStatistics statistics; 		

		
		public ActionEngine(
					AnalysisStatus totalStatus,
					boolean doFocus,
					boolean doCoerceAfterFocus,
					boolean doCoerceAfterUpdate,
					boolean doBlur,
					boolean freezeStructuresWithMessages,
					boolean breakIfCoerceAfterUpdateFailed) {

			assert(doBlur);	
			
			this.doFocus = doFocus;
			this.doCoerceAfterFocus = doCoerceAfterFocus;
			this.doCoerceAfterUpdate = doCoerceAfterUpdate;
			this.doBlur = doBlur;
			this.freezeStructuresWithMessages = freezeStructuresWithMessages;
			this.breakIfCoerceAfterUpdateFailed = breakIfCoerceAfterUpdateFailed;
			this.printStrucutreIfCoerceAfetFocusFailed = false;

			this.totalStatus = totalStatus;
			// FIXME this.status = new AnalysisStatus();
			this.status = totalStatus;
		}
			
		public void evaluate(Collection initial) {
			throw new InternalError(marker + " Internal appliers evaluate are used only for the apply");
		}
		
		public boolean setPrintStrucutreIfCoerceAfetFocusFailed(boolean shouldPrint) {
			boolean oldVal = this.printStrucutreIfCoerceAfetFocusFailed;
			this.printStrucutreIfCoerceAfetFocusFailed = shouldPrint;
			return oldVal;
		}

		/** Apply the action on the structure at program location label 
		 * returning all possible resulting structures.
		 * @param messages Map with messages generated for structures. 
		 * Must be initialized. 
		 * @since 8.2.2001 Added TVS output printing capabilities.
		 */
		public Collection apply(Action action, TVS tvs, PrintableProgramLocation currentLocation, Map messages) {
			String nodeLabel = currentLocation.label();
			HighLevelTVS structure = (HighLevelTVS) tvs; 
			Collection answer = new ArrayList();
			
			if (action.isSkipAction()) {
				answer.add(tvs.copy());
				return answer;
			}
			
			// Focus
			Collection focusResult = null;
			if (doFocus && action.getFocusFormulae().size() > 0) {
				status.startTimer(AnalysisStatus.FOCUS_TIME);
				focusResult = Focus.focus(structure, action.getFocusFormulae());
				status.stopTimer(AnalysisStatus.FOCUS_TIME);
			}
			else {
				focusResult = Collections.singleton(structure);
			}
			
			for (Iterator focusIt = focusResult.iterator(); focusIt.hasNext(); ) {
				HighLevelTVS focusedStructure = (HighLevelTVS) focusIt.next();

				if (AnalysisStatus.debug) {
					IOFacade.instance().printStructure(structure, marker + " Executing " + action.toString()); 
					if (doFocus)
						IOFacade.instance().printStructure(
								focusedStructure, 
								marker + " After Focus " + 
								nodeLabel + " " + action.toString());
				}
				
				// Coerce if coerce-after-focus is enabled
				if (doCoerceAfterFocus) {
					status.startTimer(AnalysisStatus.COERCE_TIME);
					boolean valid = focusedStructure.coerce();
					status.stopTimer(AnalysisStatus.COERCE_TIME);
					
					if (!valid) {
						status.numberOfConstraintBreaches++;
						
						if (printStrucutreIfCoerceAfetFocusFailed) {	
							if (!AnalysisStatus.debug) { // Print the constraint breach.
								boolean savedCurLocPrint = currentLocation.setShouldPrint(true);
								AnalysisStatus.debug = true;
								focusedStructure.coerce();
								AnalysisStatus.debug = false; // Restore the old value.
								currentLocation.setShouldPrint(savedCurLocPrint);
							}
						}
					
						continue;
					}
					
					// FOCUS returned a valid structure 
					if (AnalysisStatus.debug)
						IOFacade.instance().printStructure(
								focusedStructure, marker + " After Coerce in Focus" + 
								nodeLabel + " " + action.toString());
				}

				// Precondition evaluation
				status.startTimer(AnalysisStatus.PRECONDITION_TIME);
				Collection assigns = action.checkPrecondition(focusedStructure);
				status.stopTimer(AnalysisStatus.PRECONDITION_TIME);
				
				for (Iterator assignIt = assigns.iterator(); assignIt.hasNext(); ) {
					Assign assign = (Assign)assignIt.next();
//					if (action.checkHaltCondition(focusedStructure,assign))
//						throw new AnalysisHaltException(nodeLabel, action);

					Collection newMessages = action.reportMessages(focusedStructure, assign);
					if (!newMessages.isEmpty()) {
						messages.put(focusedStructure, newMessages);
						if (freezeStructuresWithMessages) {
							status.numberOfMessages++;
							assignIt.remove();
							continue;
						}
					}

					if (AnalysisStatus.debug && (!assign.isEmpty() || !doCoerceAfterFocus)) {
						IOFacade.instance().printStructure(
								focusedStructure, marker + " Precondition binding " + 
								nodeLabel + " " + action + " " +
								(assign.isEmpty() ? "{}" : "" + assign));
					}
					
					// Update formulae evaluation
					status.startTimer(AnalysisStatus.UPDATE_TIME);
					final HighLevelTVS result = action.evaluate(focusedStructure, assign);

					if (AnalysisStatus.debug)
						IOFacade.instance().printStructure(
								result, marker + " After Update " + 
								nodeLabel + " " + action + 
								(assign.isEmpty() ? "{}" : " " + assign));					
					result.filterNodes(new Filter<Node>() {
                        public boolean accepts(Node node) {
                            return result.eval(AuxiliaryPredicates.kill, node) != Kleene.trueKleene;
                        }
                    });
					status.stopTimer(AnalysisStatus.UPDATE_TIME);

					if (AnalysisStatus.debug)
						IOFacade.instance().printStructure(
								result, marker + " After Removing marked nodes " + 
								nodeLabel + " " + action );

					// Coerce (if coerce after update is enabled)
					if (doCoerceAfterUpdate) {
						status.startTimer(AnalysisStatus.COERCE_TIME);
						boolean valid = result.coerce();
						status.stopTimer(AnalysisStatus.COERCE_TIME);
						
						if (!valid) {
							status.numberOfConstraintBreachesAfterUpdtae++;
							
							// PRINT ORIGINAL STRUCTURE
							if (!AnalysisStatus.debug) {
								IOFacade.instance().printStructure(
										structure, marker + " Coerce (after update) failed for this (original) structure in " + 
										nodeLabel + " action: " + action + 
										(assign.isEmpty() ? "{}" : " " + assign));
							}
														
							// PRINT COERCED STRUCTURE
							if (!Coerce.debug) { // Print the constraint breach.
								boolean savedCurLocPrint = currentLocation.setShouldPrint(true);
								Coerce.debug = true;
								result.coerce();
								Coerce.debug = false; // Restore the old value.
								currentLocation.setShouldPrint(savedCurLocPrint);
							}
							
							if (breakIfCoerceAfterUpdateFailed) {
								Logger.println(StringUtils.newLine + marker +
										       " The analysis has stopped since a constraint was breached during the operation " +
											   "of Coerce, after Update was applied!" + StringUtils.newLine +
											   "Action = " + action.toString() + StringUtils.newLine +
											   "Program location = " + nodeLabel + StringUtils.newLine +
											   " input TVS" + structure + StringUtils.newLine +
											   " updated TVS" + result);
								
								status.finishAnalysis();									
								// FIXME, should return emptry-set, but we throw an exception
								throw new InternalError("Coerce after update failed! ");
								//return Collections.EMPTY_SET;
							}
							
							// continue with the other strucutres in the focused result
							continue;
						}
					}
					
					// Blur is applied when the structure is joined to 
					// the target location, but for the sake of debugging
					// it is also performed here.
					if (doBlur) { 
						status.startTimer(AnalysisStatus.BLUR_TIME);
						result.blur();
						status.stopTimer(AnalysisStatus.BLUR_TIME);

						if (AnalysisStatus.debug) {
							IOFacade.instance().printStructure(
									result, marker + " After Blur " + 
									nodeLabel + " " + action + 
									(assign.isEmpty() ? "" : " " + assign));
						}
					}
					
					answer.add(result);
				}
			}
			ModifiedPredicates.clear();
			return answer;
		}
		
	
		/** Gives the engine a chance to do some initializations, such as
		 * updating values from program properties.
		 * @author Roman Manevich
		 * @since 13.10.2001 Initial creation.
		 */
		protected void init() {
		}
		
		/** Use this method to update the analysis status periodically.
		 * @author Roman Manevich.
		 * @since 5.4.2002 initial creation.
		 */
		protected void updateStatus() {
		}

		/** Stops all status timers.  Used to obtain statistics at
		 *  the end of a refinement iteration, after analysis was
		 *  terminated due to imprecision.  Stopping an already stopped
		 *  timer is now okay because of the isStarted flag.
		 *  @author Alexey Loginov.
		 *  @since 4.1.2004 Initial creation.
		 */
		public void stopTimers() {
			status.stopTimer(AnalysisStatus.LOAD_TIME);
			status.stopTimer(AnalysisStatus.FOCUS_TIME);
			status.stopTimer(AnalysisStatus.PRECONDITION_TIME);
			status.stopTimer(AnalysisStatus.COERCE_TIME);
			status.stopTimer(AnalysisStatus.UPDATE_TIME);
			status.stopTimer(AnalysisStatus.BLUR_TIME);
			status.stopTimer(AnalysisStatus.JOIN_TIME);
			status.stopTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);
		}
		
		/** Prints all statistics and info for the current analysis run
		 *  (and current iteration of abstraction refinement).
		 * @author Alexey Loginov.
		 * @since 4.1.2004 Initial creation.
		 */
		public void printAnalysisInfo() {
		}

		/** Let's the factories do runtime statistics.
		 * @author Roman Manevich. 
		 * @since 2.1.2001 Initial creation.
		 */
		public class SpaceStatistics {
			public int statisticsEvery;
			private Collection locations;
			
			public SpaceStatistics(Collection locations) {
				statisticsEvery = ProgramProperties.getIntProperty("tvla.spaceStatistics.every", 10000);
				this.locations = locations;
			}
			
			public void doStatistics() {
				TVSFactory.getInstance().collectTVSSizeInfo(new TVSIterator());
			}
			
			public class TVSIterator implements Iterator {
				private Iterator locationIterator;
				private Iterator structureIter;
				private HighLevelTVS nextTVS;
				
				public TVSIterator() {
					locationIterator = locations.iterator();
					findNextStructure();
				}
				
				public boolean hasNext() {
					boolean answer = nextTVS != null;
					return answer;
				}
				
				public Object next() {
					Object result = nextTVS;
					findNextStructure();
					return result;
				}
				
				public void remove() {
					throw new UnsupportedOperationException();
				}
				
				private void findNextStructure() {
					if (structureIter != null && structureIter.hasNext()) {
						nextTVS = (HighLevelTVS) structureIter.next();
						return;
					}
					else {
						while (locationIterator.hasNext()) {
							Location location = (Location) locationIterator.next();
							if (!location.frozenStructures().hasNext())
								continue;
							structureIter = location.frozenStructures();
							nextTVS = (HighLevelTVS) structureIter.next();
							return;
						}
					}
					nextTVS = null;
				}
			}
		}
		
		public boolean doesFocus() {
			return doFocus;
		}
		
		public boolean doesCoerceAfterFocus(){
			return doCoerceAfterFocus;
		}
		
		public boolean doesCoerceAfterUpdate(){
			return doCoerceAfterUpdate;
		}
		
		public boolean doesBlur(){
			return doBlur;
		}
		
		public boolean freezesStructuresWithMessages() {
			return freezeStructuresWithMessages;
		}
		
		public boolean breaksIfCoerceAfterUpdateFailed(){
			return breakIfCoerceAfterUpdateFailed;
		}

		public AnalysisStatus getAnalysisStatus() {
			return status;
		}
		
		public AnalysisStatus getTotalAnalysisStatus() {
			return totalStatus;
		}

	}
}
