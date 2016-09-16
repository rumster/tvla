package tvla.analysis;

import java.util.*;

import tvla.core.*;
import tvla.core.assignments.Assign;
import tvla.core.base.BaseHighLevelTVS;
import tvla.core.common.ModifiedPredicates;
import tvla.exceptions.AnalysisHaltException;
import tvla.exceptions.FocusNonTerminationException;
import tvla.exceptions.SemanticErrorException;
import tvla.exceptions.UserErrorException;
import tvla.io.IOFacade;
import tvla.io.TVLAIO;
import tvla.termination.TerminationAnalysisInput;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.Location;
import tvla.transitionSystem.PrintableProgramLocation;
import tvla.util.Logger;
import tvla.util.Pair;
import tvla.util.ProgramProperties;
import tvla.util.StringUtils;

/**
 * Core TVLA Engine.
 * 
 * @see tvla.transitionSystem.Action
 * @see tvla.transitionSystem.Location
 * @author Tal Lev-Ami
 */
public abstract class Engine {
    /**
     * This variable gives convenience access to the active engine.
     * 
     * @author Roman Manevich.
     * @since 18.11.2001 Initial creation.
     */
    public static Engine activeEngine;

    /**
     * A reference to the location currently being processed by the engine.
     * 
     * @author Roman Manevich.
     * @since tvla-2-alpha 15 July 2002
     */
    protected PrintableProgramLocation currentLocation;

    /**
     * A reference to the action currently being processed by the engine.
     * 
     * @author Roman Manevich.
     * @since tvla-2-alpha 15 July 2002
     */
    protected Action currentAction;

    /**
     * @since 5.4.2002 Blur is always performed as the last step of an action.
     */
    public static boolean coerceAfterUpdateFailed = false;

    protected boolean doBlur = true;

    protected boolean doFocus = true;

    public boolean doCoerceAfterFocus = true;

    protected boolean doCoerceAfterUpdate = true;

    protected boolean breakIfCoerceAfterUpdateFailed = false;

    protected boolean freezeStructuresWithMessages = true;

    protected AnalysisStatus status;

    /**
     * Holds the transition relation of the active analysis
     */
    protected TransitionRelation transitionRelation = null;

    /**
     * Holds the termination analysis data of the active analysis
     */
    protected TerminationAnalysisInput terminationAnalysisInput = null;

    // /////////////////////////////////////////////////
    // Some of the statistics is still stored here,
    // but should be moved to the AnalysisStatus class.

    protected SpaceStatistics statistics;

	protected boolean blurAllowed = true;

    /**
     * Constructs and initializes a new engine.
     */
    protected Engine(boolean doFocus, boolean doCoerceAfterFocus, boolean doCoerceAfterUpdate,
            boolean doBlur, boolean freezeStructuresWithMessages,
            boolean breakIfCoerceAfterUpdateFailed) {
        this.doFocus = doFocus;
        this.doCoerceAfterFocus = doCoerceAfterFocus;
        this.doCoerceAfterUpdate = doCoerceAfterUpdate;
        this.doBlur = doBlur;
        this.freezeStructuresWithMessages = freezeStructuresWithMessages;
        this.breakIfCoerceAfterUpdateFailed = breakIfCoerceAfterUpdateFailed;
    }

    protected Engine() {
        freezeStructuresWithMessages = ProgramProperties.getBooleanProperty(
                "tvla.engine.freezeStructuresWithMessages", false);
        breakIfCoerceAfterUpdateFailed = ProgramProperties.getBooleanProperty(
                "tvla.engine.breakIfCoerceAfterUpdateFailed", false);

        // Processing of action order property value.
        {
            String action = ProgramProperties.getProperty("tvla.engine.actionOrder", "fpucb");
            int pos = 0;
            if (pos < action.length() && action.charAt(pos) == 'f') {
                doFocus = true;
                pos++;
            } else {
                doFocus = false;
            }
            if (pos < action.length() && action.charAt(pos) == 'c') {
                doCoerceAfterFocus = true;
                pos++;
            } else {
                doCoerceAfterFocus = false;
            }
            if (pos < action.length() && action.charAt(pos) == 'p') {
                pos++;
            } else {
                throw new UserErrorException("Illegal action order specified : " + action);
            }
            if (pos < action.length() && action.charAt(pos) == 'u') {
                pos++;
            } else {
                throw new UserErrorException("Illegal action order specified : " + action);
            }
            if (pos < action.length() && action.charAt(pos) == 'c') {
                doCoerceAfterUpdate = true;
                pos++;
            } else {
                doCoerceAfterUpdate = false;
            }
            if (pos < action.length() && action.charAt(pos) == 'b') {
                pos++;
            }
        }
    }

    public static float stat_FocusCalls = 0;

    public static float stat_FocusDelta = 0;

    public static float stat_FocusSqrDelta = 0;

    public static int stat_FocusDeltaN = 0;

    /**
     * Apply the action on the structure at program location label returning all
     * possible resulting structures.
     * 
     * @param messages
     *            Map with messages generated for structures. Must be
     *            initialized.
     * @since 8.2.2001 Added TVS output printing capabilities.
     */
    public Collection<HighLevelTVS> apply(Action action,
                                          HighLevelTVS structure,
                                          String label,
                                          Map<HighLevelTVS, Set<String>> messages,
                                          Map<HighLevelTVS, Map<Node, Node>> nodesTransition) {
        try {

            HighLevelTVS parent = structure;

            Collection<HighLevelTVS> answer = new ArrayList<HighLevelTVS>();
            // Focus
            Collection<HighLevelTVS> focusResult = null;

            if (Coerce.debug)
                Logger.println("--------------------------- " + label
                        + " --------------------------");

            float focusOnEntry, focusDiscardPercent;

            if (doFocus && action.getFocusFormulae().size() > 0) {
                stat_FocusCalls++;
                try {
                    status.startTimer(AnalysisStatus.FOCUS_TIME);
                    focusResult = Focus.focus(structure, action.getFocusFormulae(), action.getPrecondition());
                    status.stopTimer(AnalysisStatus.FOCUS_TIME);
                } catch (FocusNonTerminationException e) {
                    String message = "While focusing on " + action.getFocusFormulae()
                            + StringUtils.newLine;
                    message += "During the evaluation of " + action;
                    e.append(message);
                    throw e;
                }
            } else {
                focusResult = Collections.singleton(structure);
            }
            focusOnEntry = (float) focusResult.size();

            for (Iterator<HighLevelTVS> focusIt = focusResult.iterator(); focusIt.hasNext();) {
                HighLevelTVS focusedStructure = focusIt.next();

                if (AnalysisStatus.debug) {
                    IOFacade.instance().printStructure(structure,
                            "Executing " + label + " " + action.toString());
                    if (doFocus)
                        IOFacade.instance().printStructure(focusedStructure,
                                "After Focus " + label + " " + action.toString());
                }

                // Coerce if coerce-after-focus is enabled
                if (doCoerceAfterFocus) {

                    // if (Coerce.debug)
                    // Logger.println("FOCUS");

                    status.startTimer(AnalysisStatus.COERCE_TIME);
                    boolean valid = focusedStructure.coerce();
                    status.stopTimer(AnalysisStatus.COERCE_TIME);
                    if (!valid) {
                        status.numberOfConstraintBreaches++;
                        continue;
                    }
                    if (AnalysisStatus.debug)
                        IOFacade.instance().printStructure(focusedStructure,
                                "After Coerce " + label + " " + action.toString());
                }

                // Precondition evaluation
                status.startTimer(AnalysisStatus.PRECONDITION_TIME);
                Collection<Assign> assigns = action.checkPrecondition(focusedStructure);
                status.stopTimer(AnalysisStatus.PRECONDITION_TIME);

                for (Iterator<Assign> assignIt = assigns.iterator(); assignIt.hasNext();) {
                    Assign assign = (Assign) assignIt.next();
                    if (action.checkHaltCondition(focusedStructure, assign))
                        throw new AnalysisHaltException(label, action);

                    boolean freeze = reportMessages(action, focusedStructure, assign, messages);
                    if (freeze) {
                        assignIt.remove();
                        continue;
                    }

                    if (AnalysisStatus.debug && (!assign.isEmpty() || !doCoerceAfterFocus)) {
                        IOFacade.instance().printStructure(
                                focusedStructure,
                                "Precondition binding " + label + " " + action + " "
                                        + (assign.isEmpty() ? "{}" : "" + assign));
                    }

                    // Update formulae evaluation
                    status.startTimer(AnalysisStatus.UPDATE_TIME);
                    HighLevelTVS result = action.evaluate(focusedStructure, assign);
                    status.stopTimer(AnalysisStatus.UPDATE_TIME);

                    if (AnalysisStatus.debug)
                        IOFacade.instance().printStructure(
                                result,
                                "After Update " + label + " " + action
                                        + (assign.isEmpty() ? "{}" : " " + assign));

                    // Coerce (if coerce after update is enabled)
                    if (doCoerceAfterUpdate) {
                        // if (Coerce.debug)
                        // Logger.println("UPDATE");

                        status.startTimer(AnalysisStatus.COERCE_TIME);
                        boolean valid = result.coerce();
                        status.stopTimer(AnalysisStatus.COERCE_TIME);
                        if (!valid) {
                            status.numberOfConstraintBreaches++;
                            if (breakIfCoerceAfterUpdateFailed) {
                                Logger
                                        .println(StringUtils.newLine
                                                + "The analysis has stopped since a constraint was breached during the operation "
                                                + "of Coerce, after Update was applied!"
                                                + StringUtils.newLine + "Action = "
                                                + action.toString() + StringUtils.newLine
                                                + "Program location = " + label);
                                if (!Coerce.debug) { // Print the constraint
                                                        // breach.
                                    boolean savedCurLocPrint = currentLocation.setShouldPrint(true);
                                    Coerce.debug = true;
                                    result.coerce();
                                    Coerce.debug = false; // Restore the old
                                                            // value.
                                    currentLocation.setShouldPrint(savedCurLocPrint);
                                }
                                coerceAfterUpdateFailed = true;
                                status.finishAnalysis();
                                return Collections.emptySet();
                            }
                            continue;
                        }
                        if (AnalysisStatus.debug)
                            IOFacade.instance().printStructure(
                                    result,
                                    "After Coerce " + label + " " + action
                                            + (assign.isEmpty() ? "" : " " + assign));
                    }

                    reportPostMessages(action, result, messages);
	
                    // Blur is applied when the structure is joined to
                    // the target location, but for the sake of debugging
                    // it is also performed here.
                    if (AnalysisStatus.debug && blurAllowed) {
                        status.startTimer(AnalysisStatus.BLUR_TIME);
                        result.blur();
                        status.stopTimer(AnalysisStatus.BLUR_TIME);
                        IOFacade.instance().printStructure(
                                result,
                                "After Blur " + label + " " + action
                                        + (assign.isEmpty() ? "" : " " + assign));
                    }

                    answer.add(result);

                    BaseHighLevelTVS resultH = (BaseHighLevelTVS)focusedStructure;
                    if (nodesTransition != null && resultH.LastIncrements != null && resultH.LastIncrements.nodesMap != null) {
                        nodesTransition.put(result, resultH.LastIncrements.nodesMap);
                    }
                }
            }

            ModifiedPredicates.clear();
            focusDiscardPercent = (focusOnEntry - answer.size()) / focusOnEntry;
            // if ((focusOnEntry - focusOnExit)/(float)focusOnEntry < .5 &&
            // (focusOnEntry - focusOnExit) > 1)
            // stat_FocusDelta++;
            stat_FocusDelta = (stat_FocusDelta * stat_FocusDeltaN + focusDiscardPercent)
                    / (stat_FocusDeltaN + 1);
            stat_FocusSqrDelta = (stat_FocusSqrDelta * stat_FocusDeltaN + focusDiscardPercent
                    * focusDiscardPercent)
                    / (stat_FocusDeltaN + 1);
            stat_FocusDeltaN++;
            return answer;
        } catch (SemanticErrorException e) {
            e.append("while evaluating the action " + action);
            throw e;
        }
    }

    protected void reportPostMessages(Action action, HighLevelTVS structure,
            Map<HighLevelTVS, Set<String>> messages) {
        // Check post update, post blur messages
        HighLevelTVS blurred = structure.copy();
        if (blurAllowed) blurred.blur();
        Set<String> postMsgs = action.reportPostMessages(blurred, Assign.EMPTY);
        if (!postMsgs.isEmpty()) {
            messages.put(blurred, postMsgs);
        }
    }

    protected boolean reportMessages(Action action, HighLevelTVS structure, Assign assign,
            Map<HighLevelTVS, Set<String>> messages) {
        Set<String> newMessages = action.reportMessages(structure, assign);
        if (!newMessages.isEmpty()) {
            messages.put(structure, newMessages);
            return freezeStructuresWithMessages;
        }
        return false;
    }

    /**
     * overridden by subclasses.
     * 
     * @author Eran Yahav.
     */
    public abstract void evaluate(Collection<HighLevelTVS> initial);

    /**
     * Can be overriden by subclasses.
     * 
     * @author maon
     */
    public void printResults(TVLAIO io) {
    }

    /**
     * Returns a reference to the location currently being processed by the
     * active engine.
     * 
     * @author Roman Manevich.
     * @since tvla-2-alpha 15 July 2002
     */
    public static PrintableProgramLocation getCurrentLocation() {
        return activeEngine.getProcessedLocation();
    }

    /**
     * Returns a reference to the action currently being processed by the active
     * engine.
     * 
     * @author Roman Manevich.
     * @since tvla-2-alpha 15 July 2002
     */
    public static Action getCurrentAction() {
        return activeEngine.currentAction;
    }

    /**
     * Gives the engine a chance to do some initializations, such as updating
     * values from program properties.
     * 
     * @author Roman Manevich
     * @since 13.10.2001 Initial creation.
     */
    public void init() {
        status = new AnalysisStatus();
    }

    /**
     * Use this method to update the analysis status periodically.
     * 
     * @author Roman Manevich.
     * @since 5.4.2002 initial creation.
     */
    protected void updateStatus() {
    }

    /**
     * Stops all status timers. Used to obtain statistics at the end of a
     * refinement iteration, after analysis was terminated due to imprecision.
     * Stopping an already stopped timer is now okay because of the isStarted
     * flag.
     * 
     * @author Alexey Loginov.
     * @since 4.1.2004 Initial creation.
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

    /**
     * Prints all statistics and info for the current analysis run (and current
     * iteration of abstraction refinement).
     * 
     * @author Alexey Loginov.
     * @since 4.1.2004 Initial creation.
     */
    public void printAnalysisInfo() {
    }

    /**
     * Returns the transition relation of the current engine
     * 
     * @author noam rinetzky
     */
    public TransitionRelation getTransitionRelation() {
        return transitionRelation;
    }

    /**
     * Let's the factories do runtime statistics.
     * 
     * @author Roman Manevich.
     * @since 2.1.2001 Initial creation.
     */
    public class SpaceStatistics {
        public int statisticsEvery;

        private Collection<? extends Location> locations;

        public SpaceStatistics(Collection<? extends Location> locations) {
            statisticsEvery = ProgramProperties.getIntProperty("tvla.spaceStatistics.every", 10000);
            this.locations = locations;
        }

        public void doStatistics() {
            TVSFactory.getInstance().collectTVSSizeInfo(new TVSIterator());
        }

        public class TVSIterator implements Iterator<HighLevelTVS> {
            private Iterator<? extends Location> locationIterator;

            private Iterator<HighLevelTVS> structureIter;

            private HighLevelTVS nextTVS;

            public TVSIterator() {
                locationIterator = locations.iterator();
                findNextStructure();
            }

            public boolean hasNext() {
                boolean answer = nextTVS != null;
                return answer;
            }

            public HighLevelTVS next() {
                HighLevelTVS result = nextTVS;
                findNextStructure();
                return result;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            private void findNextStructure() {
                if (structureIter != null && structureIter.hasNext()) {
                    nextTVS = structureIter.next();
                    return;
                } else {
                    while (locationIterator.hasNext()) {
                        Location location = locationIterator.next();
                        if (!location.frozenStructures().hasNext())
                            continue;
                        structureIter = location.frozenStructures();
                        nextTVS = structureIter.next();
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

    public boolean doesCoerceAfterFocus() {
        return doCoerceAfterFocus;
    }

    public boolean doesCoerceAfterUpdate() {
        return doCoerceAfterUpdate;
    }

    public boolean doesBlur() {
        return doBlur;
    }

    public boolean freezesStructuresWithMessages() {
        return freezeStructuresWithMessages;
    }

    public boolean breaksIfCoerceAfterUpdateFailed() {
        return breakIfCoerceAfterUpdateFailed;
    }

    // ///////////////////////////////////////////////////
    // /// AnalysisMonitor /////
    // ///////////////////////////////////////////////////

    public PrintableProgramLocation getProcessedLocation() {
        assert (currentLocation != null);
        return this.currentLocation;
    }

    public long getNumOfIterations() {
        throw new UnsupportedOperationException();
    }

    public AnalysisStatus getAnalysisStatus() {
        return status;
    }

    public void prepare(Collection<HighLevelTVS> initial) {
        HighLevelTVS.advancedCoerce.coerceInitial(initial);
    }
}