package tvla.analysis.decompose;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.Engine;
import tvla.analysis.IntraProcEngine;
import tvla.core.Coerce;
import tvla.core.Framer;
import tvla.core.HighLevelTVS;
import tvla.core.IsomorphismEquivalenceClassCreator;
import tvla.core.TVS;
import tvla.core.TVSSet;
import tvla.core.assignments.Assign;
import tvla.core.decompose.CartesianElement;
import tvla.core.decompose.Decomposer;
import tvla.core.decompose.DecompositionName;
import tvla.core.decompose.OverlapDecomposer;
import tvla.core.generic.GenericHashPartialJoinTVSSet;
import tvla.core.meet.Meet;
import tvla.exceptions.SemanticErrorException;
import tvla.io.IOFacade;
import tvla.predicates.DynamicVocabulary;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.AnalysisGraph;
import tvla.transitionSystem.Location;
import tvla.transitionSystem.Action.ReportMessage;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Logger;
import tvla.util.Pair;
import tvla.util.ProgramProperties;
import tvla.util.Timer;
/**
 * IntraProc engine which uses the decomposition domain
 * @author tla
 *
 */
public class DecompositionIntraProcEngine extends IntraProcEngine {
    /**
     * Is the fixed point computation incremental
     */
    protected boolean incremental;
    
    /**
     * Strategy used to decompose structures after running a transformer
     */
    protected DecompositionStrategy decompositionStrategy;


    private DecompositionName currentComponent;

    /** Constructs and initializes an intra-procedural engine.
     */
    public DecompositionIntraProcEngine() {
        super();
    }

    /**
     * The DecomposeLocation being processed.
     */
    public DecomposeLocation getProcessedLocation() {
        return (DecomposeLocation) currentLocation;
    }
    
    @Override
    public void prepare(Collection<HighLevelTVS> initial) {
        this.cfg = (DecomposeAnalysisGraph) AnalysisGraph.activeGraph;
        init();
        
        // Filter structures according to dname 
        CartesianElement element = new CartesianElement();
        for (HighLevelTVS tvs : initial) {
            element.join(tvs);
        }
        initial.clear();
        DecomposeLocation entryLocation = (DecomposeLocation) cfg.getEntryLocation();
        CartesianElement entryElement = entryLocation.getElement();
        boolean removed = false;
        ArrayList<TVS> inconsistent = new ArrayList<TVS>();
        for (DecompositionName name : element.names()) {
            TVSSet set = element.get(name);
            for (HighLevelTVS tvs : set) {
                HighLevelTVS prepared = Decomposer.getInstance().prepareForAction(tvs, name, DynamicVocabulary.empty());                

                if (prepared == null) {
                    currentLocation = entryLocation;
                    boolean savedCurLocPrint = currentLocation.setShouldPrint(true);
                    boolean savedCoerce = Coerce.debug;
                    boolean savedAnalysis = AnalysisStatus.debug;                    
                    Coerce.debug = true;
                    AnalysisStatus.debug = true;                    
                    Decomposer.getInstance().prepareForAction(tvs, name, DynamicVocabulary.empty());
                    Coerce.debug = savedCoerce;
                    AnalysisStatus.debug = savedAnalysis;                    
                    currentLocation.setShouldPrint(savedCurLocPrint);
                }
                
                if (prepared == null) {
                    removed = true;
                    inconsistent.add(tvs);
                } else {
                    entryElement.join(Decomposer.getInstance().decompose(prepared, name));
                }
            }
        }
        if (removed) {
            if (!entryElement.isEmpty()) {
                Logger.println();
                Logger.println("The following "
                                + inconsistent.size()
                                + " structures are inconsistent with the instrumentation constraints:");
                for (TVS structure : inconsistent) {
                    Logger.println(tvla.io.StructureToTVS.defaultInstance
                            .convert(structure));
                }
            } else {
                Logger.println();
                Logger.println("All input structures are inconsistent with the instrumentation constraints!");
            }
        }
        entryElement.permuteBack();
    }

    public void evaluate(Collection<HighLevelTVS> initial) {
        
        // Joining the input structures and putting them in the entry location.
        DecomposeLocation entryLocation = (DecomposeLocation) cfg.getEntryLocation();
        cfg.storeStructures(entryLocation, initial);
        status.numberOfStructures = entryLocation.size();
        
        SortedSet<Location> workSet = new TreeSet<Location>();
        workSet.add(cfg.getEntryLocation());
        OUTER: while (!workSet.isEmpty()) {
           ++numberOfIterations;
           maxWorkSetSize = maxWorkSetSize < workSet.size() ? workSet.size() : maxWorkSetSize;
           averageWorkSetSize += workSet.size();
           
           Iterator<Location> first = workSet.iterator();
           currentLocation = first.next();
           first.remove();
           
           getProcessedLocation().startTimer();
           if (!AnalysisStatus.terse)
               System.err.print("\r" + currentLocation.label() + "    ");

           // Get the old version and the delta of this location
           Pair<CartesianElement, CartesianElement> before = getProcessedLocation().retrieveDelta();
           CartesianElement beforeOld = before.first == null ? null : before.first.copy();
           CartesianElement beforeDelta = before.second == null ? null : before.second.copy() ;
           
           // Get the new version of this location
           CartesianElement beforeNew = getProcessedLocation().getElement().copy();
           Set<? extends DecompositionName> currentNames = HashSetFactory.make(beforeNew.names());

           sanityCheck(beforeNew, "Before");           
           int numActions = getProcessedLocation().getActions().size();
           for (int actionIt = 0; actionIt < numActions; actionIt++) {
               currentAction = getProcessedLocation().getAction(actionIt);
               //System.err.print(currentAction + "    ");
               String target = getProcessedLocation().getTarget(actionIt);
               DecomposeLocation nextLocation = (DecomposeLocation) cfg.getLocationByLabel(target);

               // Compose structures according to action
               CompositionStrategy strategy = CompositionStrategy.getStrategy(currentAction, currentNames, incremental, nextLocation);
               strategy.init(beforeOld, beforeDelta, beforeNew);
               for (Pair<DecompositionName, Iterable<HighLevelTVS>> component : strategy) {
            	   DecompositionName name = component.first;
            	   Iterable<HighLevelTVS> structures = component.second;
//                   int c = 0;
//                   for (HighLevelTVS structure : st) {
//                     c++;
//                   } 
//                   System.err.print(name + " " + c + "    ");
            	   currentComponent = name;
            	   Timer componentTimer = Timer.getTimer("Action", currentLocation + " " + currentAction + " " + currentComponent);   
            	   
                   componentTimer.start();
                   try {
                       for (HighLevelTVS structure : structures) {
    
                    	   Map<HighLevelTVS, Set<String>> messages = HashMapFactory.make(0);
                           Collection<HighLevelTVS> results = apply(currentAction, structure, currentLocation.label(), messages);
                           if (Engine.coerceAfterUpdateFailed || !messages.isEmpty()) {
                               boolean debug = AnalysisStatus.debug;
                               AnalysisStatus.debug = true;
                               apply(currentAction, structure,
                                       currentLocation.label(), messages);
                               AnalysisStatus.debug = debug;
                           }
                           status.numberOfMessages += getProcessedLocation().addMessages(messages);
                           
                           if (status.shouldFinishAnalysis()) {
                               getProcessedLocation().stopTimer();
                               break OUTER;
                           }
                           
                           strategy.after(name, results);
                       }
                   } finally {
                       componentTimer.stop();
                   }
               }
               currentComponent = null;
               
               try {
                   strategy.verify();
               } catch (DecompositionFailedException exception) {
                   replay(strategy, beforeOld, beforeDelta, beforeNew);
                   
                   SemanticErrorException e = new SemanticErrorException(exception);
                   e.append(" while processing location " + currentLocation + " and action " + currentAction);
                   getProcessedLocation().stopTimer();
                   throw e;
               }
               
               CartesianElement decomposed = strategy.getDecomposed();

               strategy.done();

               if (decomposed.isEmpty()) continue;

               decomposed.permuteBack();
               
               if (AnalysisStatus.debug) {
                   for (HighLevelTVS structure : decomposed) {
                       IOFacade.instance().printStructure(
                               structure,
                               "After decompose " + currentLocation.label() + " "
                                       + currentAction.toString());
                   }
               }
               
               boolean needJoin;
               status.startTimer(AnalysisStatus.JOIN_TIME);
               needJoin = nextLocation.join(decomposed);                          
               status.stopTimer(AnalysisStatus.JOIN_TIME);
               // Sanity check
               sanityCheck(nextLocation.getElement(), "After");
               if (needJoin) {
                   workSet.add(nextLocation);
                   status.numberOfStructures += decomposed.size();
                   updateStatus();
                   if (status.shouldFinishAnalysis())
                       break OUTER;
               }
           }
           getProcessedLocation().stopTimer();
       }
       status.stopTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);
       
       for (Location location : cfg.getLocations()) {
           for (Action action : location.getActions()) {
               if (!action.checkedAllMessages()) {
                   throw new SemanticErrorException("Not all messages of action " + action + 
                           " have been checked! Probably problem in component definition in %message");
               }
           }
       }

       printAnalysisInfo();
    }
    
    private void sanityCheck(CartesianElement element, String more) {
        // Check that no name contains another name
        for (DecompositionName name1 : element.names()) {
            for (DecompositionName name2 : element.names()) {
                if (name1 != name2) {
                    if (!name1.isAbstraction() & !name2.isAbstraction() && name1.contains(name2)) {
                        throw new SemanticErrorException("Error in location " + currentLocation +
                                " for action " + currentAction +  " " + more + ":\n " + name1 + "\n contains \n" + name2);
                    }
                }
            }
        }
    }

    private void replay(CompositionStrategy strategy, CartesianElement beforeOld, CartesianElement beforeDelta, CartesianElement beforeNew) {
        boolean debug = AnalysisStatus.debug;
        AnalysisStatus.debug = true;

        strategy.init(beforeOld, beforeDelta, beforeNew);
        for (Pair<DecompositionName, Iterable<HighLevelTVS>> component : strategy) {
     	   DecompositionName name = component.first;
     	   Iterable<HighLevelTVS> structures = component.second;
     	   currentComponent = name;
            for (HighLevelTVS structure : structures) {
                Map<HighLevelTVS, Set<String>> messages = HashMapFactory.make(0);
                apply(currentAction, structure, currentLocation.label(), messages);
            }
        }
        AnalysisStatus.debug = debug;
        strategy.done();
    }

    @Override
    protected boolean reportMessages(Action action, HighLevelTVS structure, Assign assign,
            Map<HighLevelTVS, Set<String>> messages) {
        Set<String> newMessages = reportMessages(action, structure, assign, action.getMessages());
        if (!newMessages.isEmpty()) {
            messages.put(structure, newMessages);
            return freezeStructuresWithMessages;
        }
        return false;
    }

    private Set<String> reportMessages(Action action, HighLevelTVS structure, Assign assign, Collection<ReportMessage> messages) {
        Set<String> newMessages = new HashSet<String>();
        for (Action.ReportMessage message : messages) {
//            action.candidateMessage(message);
            if (message.shouldCheck(currentComponent)) {
//                action.checkedMessage(message);
                message.reportMessage(structure, assign, newMessages);
            }
        }
        return newMessages;
    }

    @Override
    protected void reportPostMessages(Action action, HighLevelTVS structure,
            Map<HighLevelTVS, Set<String>> messages) {
        // TODO What should be done here with the frame???? Check post update, post blur messages
        HighLevelTVS blurred = structure.copy();
        if (blurAllowed) {
        	blurred.blur();
        }
        Set<String> postMsgs = reportMessages(action, blurred, Assign.EMPTY, action.getPostMessages());
        if (!postMsgs.isEmpty()) {
            messages.put(blurred, postMsgs);
        }
    }

    @Override
    public void printAnalysisInfo() {
        super.printAnalysisInfo();
        double ratio = (((double)Meet.successfullTvsMeets)/Meet.totalNumberOfTvsMeets);
        ratio = ((int)(ratio * 100.0))/100.0; 
        Logger.println("Meet ratio: " + Meet.successfullTvsMeets + "/" +
        		Meet.totalNumberOfTvsMeets + "=" + ratio);
        
        Logger.printf("PrepareForAction cache %d/%d=%f soft %f\n", OverlapDecomposer.hit, OverlapDecomposer.total,
                (((double)OverlapDecomposer.hit) / OverlapDecomposer.total),
                (((double)OverlapDecomposer.soft) / OverlapDecomposer.total)
                );
        if (BasicCompositionFilter.checkedStructures != 0) {
            Logger.println("Skip: " + BasicCompositionFilter.skippedStructures + "/" + BasicCompositionFilter.nochangeStructures + "/" + 
                    BasicCompositionFilter.checkedStructures + ". Time: " + BasicCompositionFilter.skipFilterTime);
        }
        IsomorphismEquivalenceClassCreator.printStatistics(System.err);
        GenericHashPartialJoinTVSSet.printStatistics(System.err);
        if (ProgramProperties.getBooleanProperty("tvla.decompose.fineGrainedTimers", false)) {
            System.err.println("Fine Grained Times:");
            Timer.printTimerGroup("Action", System.err);
        }
        if (ProgramProperties.getBooleanProperty("tvla.decompose.coerceTimers", false)) {
            System.err.println("Coerce Times:");
            Timer.printTimerGroup("Coerce", System.err);
        }
        if (ProgramProperties.getBooleanProperty("tvla.decompose.lowLevelTimers", false)) {
            System.err.println("Low Level Times:");
            Timer.printTimerGroup("Low", System.err);
        }
    }


    /** Gives the engine a chance to do some initializations.
     * @author Roman Manevich
     * @since 13.10.2001 Initial creation.
     */
    public void init() {
        super.init();
        
        // Framer can't support blur in the middle 
        if (Framer.enabled) {
        	blurAllowed = false;
        }
        
        if (Decomposer.getInstance().names().isEmpty()) {
        	Logger.println("No decomposition names have been specified.  Stopping analysis!");
        	throw new RuntimeException();
        }        

        this.freezeStructuresWithMessages = false;
        this.incremental = ProgramProperties.getBooleanProperty("tvla.decompose.incremental", true);
    }

    public void stopTimers() {
        super.stopTimers();
        status.stopTimer(AnalysisStatus.MEET_TIME);        
        status.stopTimer(AnalysisStatus.COMPOSE_TIME);        
        status.stopTimer(AnalysisStatus.DECOMPOSE_TIME);        
    }    
}
