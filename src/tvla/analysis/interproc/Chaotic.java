/*
 * File: Chaotic.java 
 * Created on: 15/10/2004
 */

package tvla.analysis.interproc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.interproc.semantics.AbstractInterpreter;
import tvla.analysis.interproc.semantics.ActionInstance;
import tvla.analysis.interproc.transitionsystem.AbstractState;
import tvla.analysis.interproc.transitionsystem.ProgramTS;
import tvla.analysis.interproc.transitionsystem.AbstractState.Fact;
import tvla.analysis.interproc.transitionsystem.method.CFG;
import tvla.analysis.interproc.transitionsystem.method.CFGNode;
import tvla.analysis.interproc.transitionsystem.method.MethodTS;
import tvla.analysis.interproc.transitionsystem.method.TSEdgeIntra;
import tvla.analysis.interproc.transitionsystem.method.TSNode;
import tvla.analysis.interproc.transitionsystem.program.CallingContext;
import tvla.analysis.interproc.worklist.Event;
import tvla.analysis.interproc.worklist.EventConstructorCall;
import tvla.analysis.interproc.worklist.EventFactory;
import tvla.analysis.interproc.worklist.EventIntra;
import tvla.analysis.interproc.worklist.EventRet;
import tvla.analysis.interproc.worklist.EventStaticCall;
import tvla.analysis.interproc.worklist.EventTransition;
import tvla.analysis.interproc.worklist.EventVirtualCall;
import tvla.analysis.interproc.worklist.StackWorklist;
import tvla.analysis.interproc.worklist.Worklist;
import tvla.core.HighLevelTVS;
import tvla.transitionSystem.PrintableProgramLocation;
import tvla.util.HashSetFactory;
import tvla.util.Logger;
import tvla.util.SingleSet;
import tvla.util.graph.Graph;

/** The chaotic iteration algorithm
 * Heuristic: The calculation of the transitive closure is delayed until the last possible moment.
 * 
 * @author maon
 */
public class Chaotic { //implements AnalysisMonitor  {
	private static final boolean xdebug = false; 
    private static  boolean xxdebug = false; 
	private static final java.io.PrintStream out = System.out; 
	
	private final ProgramTS progTS;
	private final MethodTS mainTS;
	private final AbstractInterpreter ai;
	private final Worklist worklist;
//	private final InterProcTS ipTS;
	private final ChaoticPriorityPolicy priorityPolicy;

	/// method whose transition system has changed which requires
	/// a recalculation of th method summary and the propgation of 
	/// exit->entry->callSite
	private Set modifiedMethods;
	
	// Monitoring / Statistics
	TSNode currentLocation;
	long iterationNum;
	long numOfIntraEvents;
	long numOfStaticCallEvents;
	long numOfVirtualCallEvents;
	long numOfConstructorCallEvents;
	long numOfRetEvents;
	long numOfGuardEvents;
	long numOfTransitionEvents;
	long numOfSummaryUpdates;
	
		
	////////////////////////////////////////////////////
	////                CONSTRUCTORS               ////
	////////////////////////////////////////////////////
	
	public Chaotic(ProgramTS progTS, AbstractInterpreter ai) {
		assert(progTS != null);
		
		this.progTS = progTS;
		this.ai = ai;
		
//		this.ipTS = progTS.getInterProcTS();

		this.iterationNum = 0;
		this.numOfIntraEvents = 0;
		this.numOfStaticCallEvents = 0;
		this.numOfVirtualCallEvents = 0;
		this.numOfConstructorCallEvents = 0;
		this.numOfRetEvents = 0;
		this.numOfGuardEvents = 0;
		this.numOfTransitionEvents = 0;
		
		this.numOfSummaryUpdates = 0;
		
		this.currentLocation = null;

		mainTS = progTS.getMain();
		assert(mainTS != null);
		
		this.worklist = new StackWorklist();
		this.priorityPolicy = new ChaoticPriorityPolicy(progTS); 
		
		this.modifiedMethods = HashSetFactory.make();
	}
	
	public void initAnalysis(MethodTS mainTS, Collection initial) {
		assert(mainTS != null);
		assert(initial != null && !initial.isEmpty());
		assert(progTS != null);
		assert(iterationNum == 0);
				
		TSNode mainEntry = mainTS.getEntrySite();
		assert(mainEntry != null);
		assert(mainEntry.isEntrySite());
		
		for (Iterator initItr = initial.iterator(); initItr.hasNext(); ) {
			AbstractState.Fact  fact = (AbstractState.Fact)  initItr.next();
			assert(fact != null);
			addIntraEvent(mainTS, mainEntry, fact);
		}
	}

	
	/**
	 * Runs the chaotic iteration algorithm 
	 * maxNumOfIteration iterations
	 * @param maxNumOfIteration maximum iterations to execute.
	 * Required to be positive.
	 * @return whether a fixpoint was reached.
	 */
	public boolean iterate(int maxNumOfIterations) {
		return doIterate(maxNumOfIterations);
	}

	/**
	 * Runs the chaotic iteration algorithm until  
	 * a fixpoint was reached.
	 * @return whether a fixpoint was reached.
	 */
	public boolean iterate() {
		return doIterate(-1);
	}

	
	private boolean doIterate(int iterationBound) {
		assert(0 < iterationBound || iterationBound == -1);
		
		while (iterationBound != 0) {
			if (0 < iterationBound)
				--iterationBound;
						
			iterationNum++;

            
			if (xdebug) 
				out.println(" Iteration Number:" + iterationNum);
			
			if (worklist.hasEvent())  {	
				Event event = (Event) worklist.extractEvent();
				handleEvent(event);
			}
			else if (!modifiedMethods.isEmpty()) {
				updateMethodSummaries();
				if (xdebug)
					out.println("Updating summary");
			}
			else 
				return true;
		}
		
		return false;		
	}

	/**
	 * Runs the chaotic iteration algorithm 
	 * for a maxNumOfIteration iterations if 0 < maxNumOfIteration
	 * or until a fixpoint is reached if 
     */	
	private void handleEvent(Event event) {
		currentLocation = event.getSite();
				
		switch (event.getType()) {
		case Event.INTRA:
			assert (event instanceof EventIntra);
			numOfIntraEvents++;
			EventIntra intraEvent = (EventIntra) event;
			handleIntraEvent(
					intraEvent.getMethod(),
					intraEvent.getSite(),
					intraEvent.getUnprocessedFact());
			break;
						
		case Event.STATIC_CALL:
			assert (event instanceof EventStaticCall);
			numOfStaticCallEvents++;
			EventStaticCall scallEvent = (EventStaticCall) event;
			handleStaticCallEvent(
				scallEvent.getMethod(),
				(TSNode) scallEvent.getCallSite(),
				scallEvent.getCTX(),
				scallEvent.getCallee());
			break;
				
		case Event.VIRTUAL_CALL:		
			assert (event instanceof EventVirtualCall);
			numOfVirtualCallEvents++;
			EventVirtualCall vcallEvent = (EventVirtualCall) event;
			handleVirtualCallEvent(
				vcallEvent.getMethod(),
				(TSNode) vcallEvent.getCallSite(),
				vcallEvent.getCTX(),
				vcallEvent.getRefinedCTXs(),
				vcallEvent.getCallee());
			break;
			
		case Event.CONSTRUCTOR_CALL:
			assert (event instanceof EventConstructorCall);
			numOfConstructorCallEvents++;
			EventConstructorCall ccallEvent = (EventConstructorCall) event;
			handleConstructorCallEvent(
				ccallEvent.getMethod(),
				(TSNode)  ccallEvent.getCallSite(),
				ccallEvent.getCTX(),
				ccallEvent.getCallee());
				break;
				
		case Event.RET:
			assert (event instanceof EventRet);
			numOfRetEvents++;
			EventRet retEvent = (EventRet) event;
			handleRetEvent(
				retEvent.getMethod(),
				retEvent.getEntryS(),
				retEvent.getExitS());
			break;

		case Event.TRANSITION:
			assert (event instanceof EventTransition);
			numOfTransitionEvents++;
			EventTransition transitionEvent = (EventTransition) event;
			handleTransitionEvent(
				transitionEvent.getMethod(),
				transitionEvent.getSite(),
				transitionEvent.getFromFact(),
				transitionEvent.getToNode(),
				transitionEvent.getToFact());
			break;
			
		default:
			throw new InternalError("Unknown event type " + event.getType());
		}
	}	
	
	
	//////////////////////////////////////////////////
	///                Event Handlers              ///
	//////////////////////////////////////////////////
	
	public void handleIntraEvent(
			MethodTS mtdTS,
			TSNode site, 
			Fact newFact) {
		assert(mtdTS != null && site != null && newFact != null);
		
		if (xdebug) {
			out.println("Chaotic.handleIntraEvent: " +
					    "in method " + mtdTS.getSig() + " " + 
						"at site " + site.getLabel()); 
		}

		assert(site.getAbstractState().containsFact(newFact));
		
		Iterator edgeItr = progTS.followingIntraEdgesIterator(mtdTS, site);
		switch (site.getType()) {
		case CFGNode.ENTRY_SITE:
		case CFGNode.INTRA:
		case CFGNode.RET_SITE:
			assert(!site.isCallSite());
			while (edgeItr.hasNext()) {
				TSEdgeIntra intraEdge = (TSEdgeIntra) edgeItr.next();
				assert(intraEdge.getSource() == site);
				TSNode follow = (TSNode) intraEdge.getDestination();
				Collection generatedTVSs = applyIntra(intraEdge,newFact);
				
				Iterator genItr = generatedTVSs.iterator();
				addFactTransitions(mtdTS,site,newFact,follow,genItr);
			}			
			break;

		case CFGNode.EXIT_SITE:
			modifiedMethods.add(site.getCFG().getMethodTS());
			break;
			
		case CFGNode.STATIC_CALL_SITE:
			assert(site.isStaticCallSite());
			MethodTS staticCallee = progTS.getStaticCallee(mtdTS,site);
			addStaticCallEvent(mtdTS,site,newFact,staticCallee);
			break;
		
		case CFGNode.CONSTRUCTOR_CALL_SITE:
			assert(site.isConstructorCallSite());
			MethodTS ctorCallee = progTS.getConstructorCallee(mtdTS,site);
			addConstructorCallEvent(mtdTS,site,newFact,ctorCallee);
			break;
		
		case CFGNode.VIRTUAL_CALL_SITE:
			assert(site.isVirtualCallSite());
			Iterator targetsItr = progTS.getPossibleVirtualCallees(mtdTS,site);
			while (targetsItr.hasNext()) {
				MethodTS possibleTarget = (MethodTS) targetsItr.next();
				Collection refinedFacts = applyVirtualGuard(mtdTS,site,newFact,possibleTarget);
				if (!refinedFacts.isEmpty()) {
					addVirtualCallEvent(mtdTS,site,newFact,refinedFacts,possibleTarget);
				}
			}
			break;
		
		default:
			throw new InternalError("Unknown node type " + site.getType());
		}
	}

	public void handleStaticCallEvent(
			MethodTS caller,
			TSNode callNode, 
			Fact callFact, 
			MethodTS callee) {
		assert(caller != null && callNode != null && callFact != null && callee != null);
		assert(progTS.isStaticCallSiteOf(caller,callNode,callee));

//		ActionInstance call = progTS.getCallAction(caller,callNode,callee); 
		if (xdebug) {
			out.println("Chaotic.handleStaticCallEvent: " +
					    "caller " + caller.getSig() + " " + 
						"call-site " + callNode.getLabel() + " " + 
						"callee " + callee.getSig()); 
		}
		
		// Only virtual calls can refine the calling structure
		Collection refinedCallFact= new SingleSet(true,callFact);
		
		handleCallEvent(caller,callNode,callFact,refinedCallFact,callee);
	}
	
	
	public void handleVirtualCallEvent(
			MethodTS caller,
			TSNode callNode, 
			Fact callFact, 
			Collection refinedCallFacts,
			MethodTS callee) {
//		ActionInstance call = progTS.getCallAction(caller,callNode,callee); 
		assert(progTS.isVirtualCallSiteOf(caller,callNode,callee));
		if (xdebug) {
			out.println("Chaotic.handleVirtualCallEvent: " +
				    "caller " + caller.getSig() + " " + 
					"call-site " + callNode.getLabel() + " " + 
					"callee " + callee.getSig());
		}

		// FIXME Currently, no optimzation is made based on the 
		//       possible focused resuld of the guard.
		//       Possible optimization/generalization in a later phase.
		Collection refinedCallFact = new SingleSet(true,callFact);
		handleCallEvent(caller,callNode,callFact,refinedCallFact,callee);		
	}
	
	
	public void handleConstructorCallEvent(
			MethodTS caller,
			TSNode callNode, 
			Fact callFact, 
			MethodTS callee) {
		assert(caller != null && callNode != null && callFact != null && callee != null);
		assert(progTS.isConstructorCallSiteOf(caller,callNode,callee));

//		ActionInstance call = progTS.getCallAction(caller,callNode,callee); 
		if (xdebug) {
			out.println("Chaotic.handleConstructorCallEvent: " +
				    "caller " + caller.getSig() + " " + 
					"call-site " + callNode.getLabel() + " " + 
					"callee " + callee.getSig()); 
		}
			
		// Only virtual calls can refine the calling structure
		Collection refinedCallFact = new SingleSet(true,callFact);
			
		handleCallEvent(caller,callNode,callFact,refinedCallFact,callee);
	}
	
	
	public void handleCallEvent(
			MethodTS caller,
			TSNode callNode, 
			Fact callFact,
			Collection refinedCallFacts,
			MethodTS callee) {
		assert(caller != null && callNode != null && callFact != null && callee != null);

		ActionInstance call = progTS.getCallAction(caller,callNode,callee); 		
		
		Collection generatratedTVSs = 
			applyCall(
				caller,
				callNode,
				callFact,
				refinedCallFacts,
				call,
				callee);
		
		if (generatratedTVSs.isEmpty()) {
			if (xdebug) 
				out.println("No strucutres survived the call action, continuing");
			return;
		}
		
		Collection factsAtEntry = propagateToEntry(
				caller,
				callNode,
				callFact,
				refinedCallFacts,
				callee,
				generatratedTVSs);
		
		progTS.updateCallingCTXs(
				callee, factsAtEntry, 
				caller, callNode, callFact, refinedCallFacts);

	}
	
	
	public void handleRetEvent(
			MethodTS returnsFrom,
			Fact entryS,
			Fact exitS) {
		assert(returnsFrom != null && entryS != null && exitS != null);
		assert(returnsFrom.getEntrySite().getAbstractState().containsFact(entryS));
		assert(returnsFrom.getExitSite().getAbstractState().containsFact(exitS));
		
		if (xdebug) {
			out.println("Chaotic.handleRetEvent: " +
						"returnsFrom " + returnsFrom.getSig()); 
		}

		CallingContext ctx = progTS.getCallingContext(returnsFrom,entryS);
		assert(ctx != null);
		
		Collection BCs=  ctx.getAllBasicCTXs();
		assert(!BCs.isEmpty());

		Iterator bcItr = BCs.iterator();
		while (bcItr.hasNext()) {
			CallingContext.BasicCTX bc = (CallingContext.BasicCTX) bcItr.next();
			TSNode callSite = bc.getCallSite();
			MethodTS caller = callSite.getCFG().getMethodTS();
			Fact callFact  = bc.getCallingFact();
			Collection refinedCallFacts = ctx.getRefinedFacts(bc);
			assert(refinedCallFacts!=null);
			assert(!refinedCallFacts.isEmpty());
			
			addToRetNode(
				caller,callSite,callFact,refinedCallFacts,
				returnsFrom, entryS, exitS);
			}
	}
	
	public void handleTransitionEvent(
			MethodTS mtd,
			TSNode fromNode,
			Fact fromFact,
			TSNode toNode,
			Fact toFact) {
		if (xdebug) {
			out.println("Chaotic.handleTransitionEvent " +
					    "method " + mtd.getSig() + " " + 
						"from " + fromNode.getLabel() + " " + 
						"to " + toNode.getLabel()); 
		}
		
		modifiedMethods.add(mtd);
	}
	
	
	private void updateMethodSummaries() {
		Iterator itr = null;
		MethodTS mtd = null;
		
		if (xdebug) {
			out.println("Chaotic.UpdateSummaries for " + modifiedMethods.size() + " methods");
			itr = modifiedMethods.iterator();
			while (itr.hasNext()) {
				mtd = (MethodTS) itr.next();
				String sig = mtd.getSig();
				out.println(" method: " + sig);
			}
		}	
		
		itr = modifiedMethods.iterator();
		while (itr.hasNext()) {
			mtd = (MethodTS) itr.next();
			itr.remove();
			
			numOfSummaryUpdates++;
			updateSummary(mtd);
		}
	}
	
	private void updateSummary(MethodTS mtd) {
		Collection delta = progTS.updateSummary(mtd);
		if (delta == null)
			return;
		
		Iterator summaryEdgeItr = delta.iterator();
		while (summaryEdgeItr.hasNext()) {
			Object edgeObj = summaryEdgeItr.next();
			Graph.Edge edge = (Graph.Edge) edgeObj;
			HighLevelTVS entryTVS = (HighLevelTVS) edge.getSource(); 
			HighLevelTVS exitTVS = (HighLevelTVS) edge.getDestination(); 
			if (xdebug) {
				out.println("Summary updated for method " + mtd.getSig());
			}
			Fact entryFact = progTS.getFactForTVS(mtd,mtd.getEntrySite(),entryTVS);
			Fact exitFact = progTS.getFactForTVS(mtd,mtd.getExitSite(),exitTVS);
					
			if (mtd != mainTS)
				addRetEvent(mtd,entryFact,exitFact);
		}
	}
	
	//////////////////////////////////////////////////
	///                Intra Stuff                 ///
	//////////////////////////////////////////////////		

	/*
	 * Aplies the intraprocedural effect of the statement
	 * annotating edge intraEdge emenatig from site to 
	 * 
	 */
	Collection applyIntra(
			TSEdgeIntra intraEdge,
			Fact newFact) {
		TSNode site = (TSNode) intraEdge.getSource();
		CFG cfg = site.getCFG();
		assert(cfg.containsSite(site));
		assert(cfg.containsEdge(intraEdge));

		MethodTS mtdTS = cfg.getMethodTS();
		
		Map msgs = site.getAbstractState().allocateMessagesMap();
		Collection generatedTVSs = 
		ai.applyIntra(
			mtdTS.getMethod(),
			site,
			intraEdge.getActionInstance(),
			newFact.getTVS(),
			msgs);
		
		if (!msgs.isEmpty()) {
			site.getAbstractState().addMessages(newFact,msgs);
			msgs = null;
		}
	
		return generatedTVSs ;
	}
	
	/**
	 * Selects the possible targets of the virtual invocation of the
	 * at callSite site in method mtdTS. 
	 * The abstractFact at the callSite is newFact.
	 * 
	 * @param caller TS of the caller
	 * @param site the callSite
	 * @param newFact the abstract fact at the callSite
	 * @param possibleTarget a candidate for invocation.
	 * @return a collection of refined facts at the callSites 
	 * which are refined according to the virtual guard.
	 * If the collection id empty, then possibleTarget is not 
	 * a possible callee (this is possible due to overapproximation in the
	 * initial callGraph analysis). 
	 */
	private Collection applyVirtualGuard(
			MethodTS caller,
			TSNode site,
			Fact newFact,
			MethodTS possibleTarget) {
	
		assert(caller != null);
		assert(site!= null);
		assert(possibleTarget!=null);
		assert(site.isVirtualCallSite());
		assert(progTS.isVirtualCallSiteOf(caller,site,possibleTarget));
		
		ActionInstance guard = progTS.getGuardAction(caller,site,possibleTarget);
		
		Map msgs = site.getAbstractState().allocateMessagesMap();
		Collection generatedTVSs = 
			ai.applyGuard(		
					caller.getMethod(),
					site,
					guard,
					newFact.getTVS(),
					msgs);
		
		if (!msgs.isEmpty()) {
			site.getAbstractState().addMessages(newFact,msgs);
			msgs = null;
		}

		return generatedTVSs;
		
		/* currently there is no use for refined stuctures.
		 * Uses only to indicate whether to prpagate the structure or not.
		Collection facts = new ArrayList();
		Iterator tvsItr = generatedTVSs.iterator();
		while (tvsItr.hasNext()) {
			HighLevelTVS tvs = (HighLevelTVS) tvsItr.next();
			Fact fact = site.getAbstractState().getFactForExistingTVS(tvs);
			facts.add(fact);
		}
		
		return facts ;
		*/
	}

	/**
	 * Applies the call semantics associated with a specific call statment.
	 * @param caller
	 * @param callNode
	 * @param ctx
	 * @param call
	 * @param callee
	 */
	private Collection applyCall(
			MethodTS caller,
			TSNode callNode,
			Fact callFact,
			Collection refinedCallFacts,
			ActionInstance call,
			MethodTS callee) {
		
		assert(caller != null);
		assert(callNode!= null);
		assert(callFact!=null);
		assert(refinedCallFacts!=null);
		assert(!refinedCallFacts.isEmpty());
		assert(progTS.isCallSiteOf(caller,callNode,callee));
		
		Collection allGeneratedTVSs = ai.emptyTVSCollection();
		Iterator refinedFactsItr = refinedCallFacts.iterator();
		while (refinedFactsItr.hasNext()) {
			Fact refinedFact = (Fact) refinedFactsItr.next();
			Map msgs = callNode.getAbstractState().allocateMessagesMap();
			Collection generatedTVSs = 
				ai.applyCall(		
						caller.getMethod(),
						callNode,
						call,
						refinedFact.getTVS(),
						msgs);
		
			if (!msgs.isEmpty()) 
				callNode.getAbstractState().addMessages(callFact,msgs);
			
			allGeneratedTVSs.addAll(generatedTVSs);
		}

		return allGeneratedTVSs;	
	}


	private Collection propagateToEntry(
			MethodTS caller,
			TSNode callNode,
			Fact ctx,
			Collection refinedTVSs,
			MethodTS callee,
			Collection propagatedTVSs) {
		assert(propagatedTVSs != null);
		assert(ctx != null);
		assert(refinedTVSs != null);
		assert(!refinedTVSs.isEmpty());
		assert(progTS.isCallSiteOf(caller,callNode,callee));
		
		// assert ctx.getTVS() is less precise than any s in refinedCTX
		TSNode entryNode = callee.getEntrySite(); 
		TSNode exitNode = callee.getExitSite(); 
		Collection factsAtEntry = new ArrayList();
		
		Iterator tvsItr = propagatedTVSs.iterator();
		while (tvsItr.hasNext()){
			HighLevelTVS tvs = (HighLevelTVS) tvsItr.next();
			SingleSet mergedTo = new SingleSet(true);
			boolean tvsIsNewAtEntry = progTS.addTVS(callee,entryNode,tvs,mergedTo);
			Fact factAtEntry = (Fact) mergedTo.get();
			
			factsAtEntry.add(factAtEntry);
			
			if (tvsIsNewAtEntry) {
				// The resulting TVS is new in the entry to the method
				addIntraEvent(callee,entryNode,factAtEntry);
				// The method summary is no longer up-to-date
				modifiedMethods.add(callee);
			}

		
			
			//Collection knownEffect = progTS.getKnownEffect(callee,factAtEntry);
			Collection knownEffect = progTS.getKnownEffect(callee,factAtEntry);
			assert(knownEffect != null); 
			if (!knownEffect.isEmpty()) {
				Iterator knownEffectItr = knownEffect.iterator();
				while (knownEffectItr.hasNext()) {
					HighLevelTVS tvsAtExit = (HighLevelTVS) knownEffectItr.next(); 
					Fact factAtExit = progTS.getFactForTVS(callee,exitNode,tvsAtExit);
					addToRetNode(
							caller, callNode, ctx, refinedTVSs,
							callee, factAtEntry, factAtExit);
				}
			}

		}
		
		
		
		return factsAtEntry;
	}

	
	private void addToRetNode(
			MethodTS caller, TSNode callNode, Fact ctx, Collection refinedTVSs,
			MethodTS callee, Fact factAtEntry, Fact  factAtExit) {
		Collection generatedTVSs = 
			applyRet(caller,callNode,ctx,refinedTVSs,
					callee,factAtExit);

		Iterator tvsItr = generatedTVSs.iterator();
		TSNode retNode = progTS.getMatchingReturnNode(caller,callNode);
		addFactTransitions(caller,callNode,ctx,retNode,tvsItr);
	}
	
	
	private Collection applyRet(
			MethodTS caller, TSNode callNode, Fact ctx, Collection refinedTVSs,
			MethodTS callee,  Fact factAtExit) {
		
		assert(caller != null);
		assert(callNode!= null);
		assert(ctx!=null && factAtExit != null);
		assert(progTS.isCallSiteOf(caller,callNode,callee));
		assert(refinedTVSs != null);
		assert(!refinedTVSs.isEmpty());

		ActionInstance ret = progTS.getRetAction(caller,callNode,callee);
		assert(ret != null);
		
		Map msgs = callNode.getAbstractState().allocateMessagesMap();
		Iterator refinedItr = refinedTVSs.iterator();
		Collection result = ai.emptyTVSCollection();
		while (refinedItr.hasNext()) {
			Fact refinedCallFact = (Fact) refinedItr.next();
			Collection generatedTVSs =  
				ai.applyBinary(		
					caller.getMethod(),
					callNode,
					ret,
					refinedCallFact.getTVS(),
					factAtExit.getTVS(),
					msgs);
	
			if (!msgs.isEmpty()) {
				callNode.getAbstractState().addMessages(ctx,msgs);
				msgs = null;
			}
			
			result.addAll(generatedTVSs);
		}		
		return result;
	}
	
	
	/**
	 * Adds a new pair of fromFact->toFact in 
	 * the cfg edge fromNode->toNode of method mtd.
	 * @param mtd
	 * @param fromNode
	 * @param fromFact
	 * @param toNode
	 * @param toTVS
	 */
	private void addFactTransitions(
			MethodTS mtd,
			TSNode fromNode,
			Fact fromFact,
			TSNode toNode,
			Iterator tvsItr) {
		
		while (tvsItr.hasNext()) {
			HighLevelTVS toTVS = (HighLevelTVS) tvsItr.next(); 		
			SingleSet mergedTo = new SingleSet(true);
			boolean toStateChanged = progTS.addTVS(mtd,toNode,toTVS,mergedTo);

            if (xdebug) {
                out.println("addFactTransitions: after addTVS - staete chaged ? " + toStateChanged);
            }
            
			Fact toFact = (Fact) mergedTo.get();
			if (toStateChanged)
				addIntraEvent(mtd,toNode,toFact);
		
			boolean transitionChanged = progTS.addTransition(mtd,fromNode,fromFact,toNode,toFact);
			if (transitionChanged)
				addTransitionEvent(mtd,fromNode,fromFact,toNode,toFact);
		}
	}

	/////////////////////////////////////////////////////////////
	////                WorkList  Manipulation              /////  
	/////////////////////////////////////////////////////////////

	public void addIntraEvent(
			MethodTS mtdTS, 
			TSNode site, 
			Fact newFact) {
		assert(mtdTS != null);
		assert(site != null);
		assert(newFact != null);
		assert(site instanceof TSNode);
		assert(((TSNode) site).getCFG() == mtdTS.getCFG());
		assert(newFact.getContainingState() == site.getAbstractState());
		if (xdebug) {
			out.println("Chaotic.addIntraEvent: " + mtdTS.getSig() + " " +
					    "site " + site.getLabel());
            if (xxdebug) {
              out.println("    TVS  " + newFact.getTVS().toString());
            }
		}

		EventIntra event = EventFactory.intraEvent(mtdTS, site, newFact);
		worklist.addEvent(event);
	}
	

	public void addStaticCallEvent(
			MethodTS caller, 
			TSNode callSite,
			Fact ctx, 
			MethodTS callee) {
		assert(caller != null);
		assert(callSite != null);
		assert(ctx != null);
		assert(callee != null);
		assert(callSite.getCFG() == caller.getCFG());
		assert(ctx.getContainingState() == callSite.getAbstractState());
		assert(callee.getMethod().isStatic());
		assert(progTS.isStaticCallSiteOf(caller, callSite,callee));
		
		if (xdebug) {
			out.println("Chaotic.addStaticCallEvent: caller " + caller.getSig() + " " +
					    "call site " + callSite.getLabel() + " " + 
						"callee " + callee.getSig());
		}

		EventStaticCall event = EventFactory.staticCallEvent(caller, callSite, ctx, callee);
		worklist.addEvent(event);
	}

	public void addVirtualCallEvent(
			MethodTS caller, 
			TSNode callSite, 
			Fact  ctx, 
			Collection refinedFacts, // refining ctx according to the given callee
			MethodTS callee) {
		assert(callSite != null);
		assert(ctx != null);
		assert(callee != null);
		assert(callSite.getCFG() == caller.getCFG());
		assert(ctx.getContainingState() == callSite.getAbstractState());
		assert(callee.getMethod().isVirtual());
		assert(progTS.isVirtualCallSiteOf(caller, callSite,callee));
		
		if (xdebug) {
			out.println("Chaotic.addVirtualCallEvent: caller " + caller.getSig() + " " +
					    "call site " + callSite.getLabel() + " " + 
						"callee " + callee.getSig()	+ " " + 
						"num of refined facts " + refinedFacts.size()); 

		}

		EventVirtualCall event = EventFactory.virtualCallEvent(caller, callSite, ctx, refinedFacts, callee);
		Worklist.Priority priority = priorityPolicy.calcPriority(event);
		worklist.addEvent(event,priority);		
	}

	public void addConstructorCallEvent(
			MethodTS caller, 
			TSNode callSite, 
			Fact ctx, 
			MethodTS callee) {
		assert(callSite != null);
		assert(ctx != null);
		assert(callee != null);
		assert(callSite.getCFG() == caller.getCFG());
		assert(ctx.getContainingState() == callSite.getAbstractState());
		assert(callee.getMethod().isConstructor());
		assert(progTS.isConstructorCallSiteOf(caller, callSite,callee));
	
		if (xdebug) {
			out.println("Chaotic.addConstructorCallEvent: caller " + caller.getSig() + " " +
					"call site " + callSite.getLabel() + " " + 
					"callee " + callee.getSig());
		}

		EventConstructorCall event = EventFactory.constructorCallEvent(caller, callSite, ctx, callee);
		Worklist.Priority priority = priorityPolicy.calcPriority(event);
		worklist.addEvent(event,priority);		
	}

	public void addRetEvent(
			MethodTS callee,
			Fact entryS, 
			Fact exitS) {
		assert(callee != null);
		assert(entryS != null);
		assert(exitS != null);
		assert(callee.getEntrySite().getAbstractState() == entryS.getContainingState());
		assert(callee.getExitSite().getAbstractState() == exitS.getContainingState());
		
	
		if (xdebug) {
			out.println("Chaotic.addRetEvent: callee " + callee.getSig());
		}

		EventRet event = EventFactory.retEvent(callee, entryS, exitS);
		Worklist.Priority priority = priorityPolicy.calcPriority(event);
		worklist.addEvent(event,priority);			
	}


	public void addTransitionEvent(
			MethodTS mtd,
			TSNode fromNode,
			Fact fromFact, 
			TSNode toNode,
			Fact toFact) {
		assert(mtd != null);
		assert(fromNode != null);
		assert(fromFact != null);
		assert(toNode != null);
		assert(toFact != null);
			
		if (xdebug) {
			out.println("Chaotic.addTransitionEvent: " + mtd.getSig() + " " + 
					     "from " + fromNode.getLabel() + " " + 
						 "to " +  toNode.getLabel());
		}

		EventTransition event = EventFactory.transitionEvent(
				mtd,fromNode,fromFact,toNode,toFact);
		Worklist.Priority priority = priorityPolicy.calcPriority(event);
		worklist.addEvent(event,priority);			
	}

	/////////////////////////////////////////////////////
	/////             AnalysisMonitor               /////
	/////////////////////////////////////////////////////
	
	public PrintableProgramLocation  getProcessedLocation() {
		assert(currentLocation != null);
		return this.currentLocation;
	}
	
	public long getNumOfIterations() {
		return this.iterationNum;
	}

	public void updateStatus() {
		this.ai.getTotalStatus().updateStatus();
		//this.ai.getTotalStatus().printMemoryStatistics();
	}
	
	public AnalysisStatus getAnalysisStatus() {
		return this.ai.getTotalStatus();
	}

	public void printStatistics() {				
		long totalNumOfEvents = 
			this.iterationNum +
			this.numOfIntraEvents +
			this.numOfStaticCallEvents +
			this.numOfVirtualCallEvents +
			this.numOfConstructorCallEvents +
			this.numOfRetEvents +
			this.numOfGuardEvents +
			this.numOfTransitionEvents;
		
		Logger.println("Chaotic Iterations Statistics");
		Logger.println("Total number of Intra Events " + numOfIntraEvents);
		Logger.println("Total number of Static Call Events " + numOfStaticCallEvents);
		Logger.println("Total number of Virtual Call Events " + numOfVirtualCallEvents);
		Logger.println("Total number of Constructor Call Events " + numOfConstructorCallEvents);
		Logger.println("Total number of Ret Events " + numOfRetEvents);
		Logger.println("Total number of Guard Events " + numOfGuardEvents);
		Logger.println("Total number of Transition Events " + numOfTransitionEvents);
		Logger.println("Total number of events " + totalNumOfEvents);
		Logger.println("------------------------------");
		Logger.println();
		Logger.println("Total number of Summary Updates " + numOfSummaryUpdates);
		Logger.println();		
		Logger.println("Total number of iterations " + iterationNum);
	}	
}
