/*
 * File: ProgramTS.java 
 * Created on: 12/10/2004
 */

package tvla.analysis.interproc.transitionsystem;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import tvla.analysis.interproc.Method;
import tvla.analysis.interproc.semantics.ActionInstance;
import tvla.analysis.interproc.transitionsystem.AbstractState.Fact;
import tvla.analysis.interproc.transitionsystem.method.MethodTS;
import tvla.analysis.interproc.transitionsystem.method.TSNode;
import tvla.analysis.interproc.transitionsystem.program.CallingContext;
import tvla.analysis.interproc.transitionsystem.program.InterProcTS;
import tvla.core.HighLevelTVS;
import tvla.io.TVLAIO;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.SingleSet;

//import tvla.language.XML.*;

/** 
 * The transition system of the analysis.
 * Contains the transition system of every method + a transition system
 * for the entire program.
 * @author maon
 */
public class ProgramTS  {
	static final boolean xdebug = false;
	static final PrintStream out = System.out;
	
	int numOfClasses;
	int numOfMethods;
	
	MethodTS mainTS;

	Map staticMethodsTS;
	Map virtualMethodsTS;
	Map constructorsTS;
	Map allMethodsTS;

	int numOfStaticCallSites;
	int numOfVirtualCallSites;
	int numOfCtorCallSites;
	int numOfIntra;
	
	InterProcTS ipTS;

	long methodTSId;
	
	private EventConsumer eventConsumer;

	boolean analysisStarted;
	private boolean printAllMethods;
	
	////////////////////////////////////////////////
	////   TransitionSystem CFG Construction   /////
	////////////////////////////////////////////////
		
	public ProgramTS(int numOfClasses, int numOfMethods,
			  		 int numOfIntra,
					 int numOfStaticCallSites,
					 int numOfVirtualCallSites,
					 int numOfCtorCallSites) {
		super();
		this.numOfClasses = numOfClasses;
		this.numOfMethods = numOfMethods;
		staticMethodsTS = HashMapFactory.make();
		virtualMethodsTS = HashMapFactory.make(numOfMethods);
		constructorsTS = HashMapFactory.make(numOfClasses);
		allMethodsTS = HashMapFactory.make(numOfMethods);
		this.numOfIntra = numOfIntra;
		this.numOfStaticCallSites = numOfStaticCallSites;
		this.numOfVirtualCallSites = numOfVirtualCallSites;
		this.numOfCtorCallSites = numOfCtorCallSites;
		
		ipTS = new InterProcTS(
				 numOfMethods,
				 numOfStaticCallSites,
				 numOfVirtualCallSites,
				 numOfCtorCallSites);

		eventConsumer = null;
		analysisStarted = false;
		printAllMethods = false;
		
		methodTSId = 0;
	}


	/////////////////////////////////////////////////
	////   Generating the TS for every method   /////
    ////     according to their declaration     /////
	/////////////////////////////////////////////////	
	
	public MethodTS addStaticMethod(Method mtd, int numOfStmts,
									String entryLabel, String exitLabel) {
		
		if (xdebug) 
			out.println("addStaticMethod(" + mtd.getSig()+")");
		
		assert(!analysisStarted);
		if (getTS(mtd) != null) 
			throw new Error("Duplicate definition of method " + mtd.getSig());
			
		MethodTS newts = MethodTS.newMethodTS(mtd, ++methodTSId, numOfStmts, entryLabel, exitLabel);
		staticMethodsTS.put(mtd, newts);
		allMethodsTS.put(mtd, newts);
		ipTS.addStaticMethod(newts);
		return newts;		
	}
	
	public MethodTS addVirtualMethod(Method mtd, int numOfStmts,
			String entryLabel, String exitLabel) {
		if (xdebug) 
			out.println("addVirtualMethod(" + mtd.getSig()+")");
		
		assert(!analysisStarted);
		if (getTS(mtd) != null) 
			throw new Error("Duplicate definition of method " + mtd.getSig());

		MethodTS newts = MethodTS.newMethodTS(mtd, ++methodTSId, numOfStmts, entryLabel, exitLabel);
		virtualMethodsTS.put(mtd, newts);
		allMethodsTS.put(mtd, newts);
		ipTS.addVirtualMethod(newts);
		
		return newts;		
	}
	
	public MethodTS addConstructor(Method mtd, int numOfStmts,
								   String entryLabel, String exitLabel) {
		if (xdebug) 
			out.println("addConstructorMethod(" + mtd.getSig()+")");
				
		assert(!analysisStarted);
		if (getTS(mtd) != null) 
			throw new Error("Duplicate definition of method " + mtd.getSig());

		MethodTS newts = MethodTS.newMethodTS(mtd, ++methodTSId, numOfStmts, entryLabel, exitLabel);
		constructorsTS.put(mtd, newts);
		allMethodsTS.put(mtd, newts);
		ipTS.addConstructor(newts);

		return newts;		
	}

	public MethodTS setMain(Method mtd) {
		assert(mtd != null);
		
		if (mainTS != null)
			throw new Error("Attempt to reset main method " + mtd.getSig());
		
		if (!mtd.isStatic())
			throw new Error("Attempt to set non-static method as main " + mtd.getSig());

		this.mainTS = (MethodTS) staticMethodsTS.get(mtd);
		
		if (this.mainTS == null)
			throw new Error("Attempt to set a non defined method as main "  + mtd.getSig());

		return this.mainTS;
	}

	public void setEventConsumer(EventConsumer ec) {
		assert(this.eventConsumer == null);
		this.eventConsumer = ec;
	}
	
	/////////////////////////////////////////////////////////////
	////   Adding the actions to the TS of every method     /////
    ////       according to the method definition           /////
	/////////////////////////////////////////////////////////////	
	
	public void addIntraStmt(
			Method mtd,
			String from, String to,
			ActionInstance iAction) {
		if (xdebug)
			out.println("addIntraStmt: " + from + "->" + to + ": " + iAction.getMacroName(true));
		
		assert(!analysisStarted);

		MethodTS ts = getTS(mtd);
		if (ts == null)
			throw new InternalError("Adding Statement to anundefined method " + mtd.getSig());

		ts.addIntraStmt(from,to,iAction);
		numOfIntra++;
	}
	
	public void addConstructorInvocation(
			Method mtd, List args,
			String from, String to,
			Method invokedMtd,
			ActionInstance iActionCall, ActionInstance iActionRet) {
		if (xdebug)
			out.println("addConstructorInvocation: " + from + "->" + to + ": " + mtd.getSig());

		assert(!analysisStarted);
		
		if (!invokedMtd.isConstructor())
			throw new Error("Invoking a non-constructor " + invokedMtd.getSig() + 
					        " as a constructor at " + from + 
							" in method " + mtd.getSig());		
		
		MethodTS ts = getTS(mtd);
		if (ts == null)
			throw new InternalError("Adding Statement to anundefined method " + mtd.getSig());

		MethodTS ctor = (MethodTS) constructorsTS.get(invokedMtd);
		if (ctor == null)
			throw new Error("Invoking an undefined constructor " + invokedMtd.getSig() + 
			        " as a constructor at " + from + 
					" in method " + mtd.getSig());		
	
		ts.addConstructorInvocation(from,to,ctor,args);
		TSNode callSite = ts.getNode(from);
		assert(callSite.isConstructorCallSite());
		
		ipTS.addConstructorInvocation(
				ts, 
				callSite,
				ctor,
				iActionCall,  
				iActionRet);
		
		numOfCtorCallSites++;
	}

	public void addStaticInvocation(
			Method mtd, List args,
			String from, String to,
			Method invokedMtd,
			ActionInstance iActionCall, ActionInstance iActionRet) {
		if (xdebug)
			out.println("addStaticInvocation: " + from + "->" + to + ": " + mtd.getSig());	

		assert(!analysisStarted);
		
		if (!invokedMtd.isStatic())
			throw new Error("Invoking a non-static " + invokedMtd.getSig() + 
					        " as a static method at " + from + 
							" in method " + mtd.getSig());		

		MethodTS ts = getTS(mtd);
		if (ts == null)
			throw new InternalError("Adding Statement to anundefined method " + mtd.getSig());

		MethodTS smtd = (MethodTS) staticMethodsTS.get(invokedMtd);
		if (smtd == null)
			throw new Error("Invoking an undefined constructor " + invokedMtd.getSig() + 
			        " as a constructor at " + from + 
					" in method " + mtd.getSig());			
		
		ts.addStaticInvocation(from,to,smtd,args);
		TSNode callSite = ts.getNode(from);
		assert(callSite.isStaticCallSite());
		
		ipTS.addStaticInvocation(
				ts, 
				callSite,
				smtd,
				iActionCall,  
				iActionRet);

		numOfStaticCallSites++;
	}

	public void addVirtualInvocation(
			Method mtd, List args,
			String from, String to,
			Method invokedMtd,
			ActionInstance iActionCall, 
			ActionInstance iActionRet,
			ActionInstance iActionGuard) {
		if (xdebug)
			out.println("addVirtualInvocation: " + from + "->" + to + ": " + mtd.getSig());

		assert(!analysisStarted);
		
		if (!invokedMtd.isVirtual())
			throw new Error("Invoking a non-virtual method" + invokedMtd.getSig() + 
					        " as a virtual method at " + from + 
							" in method " + mtd.getSig());		

		MethodTS ts = getTS(mtd);
		if (ts == null)
			throw new InternalError("Adding Statement to anundefined method " + mtd.getSig());

		MethodTS vmtd = (MethodTS) virtualMethodsTS.get(invokedMtd);
		if (vmtd == null)
			throw new Error("Invoking an undefined constructor " + invokedMtd.getSig() + 
			        " as a constructor at " + from + 
					" in method " + mtd.getSig());			

		ts.addVirtualInvocation(from,to,vmtd,args);
		TSNode callSite = ts.getNode(from);
		assert(callSite.isVirtualCallSite());

		ipTS.addVirtualInvocation(
				ts, 
				callSite,
				vmtd,
				iActionCall,  
				iActionRet,
				iActionGuard);

		numOfVirtualCallSites++;
	}


	
	////////////////////////////////////////////////////
	///                     Output                   ///
	////////////////////////////////////////////////////

	public void setPrintAllNodes() {
		printAllMethods = true;
		
		Iterator tsItr = allMethodsTS.values().iterator();
		while (tsItr.hasNext()) {
			MethodTS ts = (MethodTS) tsItr.next();
			ts.setPrintAllNodes();
		}
	 }
	
	public void setPrintInterProcNodes() {
		printAllMethods = false;
		
		Iterator tsItr = allMethodsTS.values().iterator();
		while (tsItr.hasNext()) {
			MethodTS ts = (MethodTS) tsItr.next();
			ts.setPrintInterProcNodes();
		}
	}
	
	public void setPrintAllNodesOfMethod(Method mtd) {
		assert(mtd != null);
		
		MethodTS mtdTS = getTS(mtd);
		if (mtdTS == null)
			throw new Error("Attempt to print a node of a non defined method " + mtd.getSig());
		
		mtdTS.setPrintAllNodes();
	}

	public void setPrintNode(Method mtd, String nodeLabel) {
		assert(mtd != null);
		
		MethodTS mtdTS = getTS(mtd);
		if (mtdTS == null)
			throw new Error("Attempt to print a node of a non defined method " + mtd.getSig());
		
		mtdTS.setPrintNode(nodeLabel);		
	}
		
	public boolean printAllMethods() {
		return printAllMethods;
	}
	
	public Collection  getPrintableProgram() {
		return Collections.unmodifiableCollection(this.allMethodsTS.values());
	}
	
	
	public void printResults(TVLAIO io) {
		Iterator mtdItr = allMethodsTS.values().iterator();
		while (mtdItr.hasNext()) {
			MethodTS ts = (MethodTS) mtdItr.next();
			//			ts.printResults(io);
			io.printAnalysisState(ts);
		}
	}
/*
	public void saveResults(TVLAIO io, XML xml) {
		Iterator mtdItr = allMethodsTS.values().iterator();
		while (mtdItr.hasNext()) {
			MethodTS ts = (MethodTS) mtdItr.next();
			ts.saveResults(io,xml);
		}
	}
*/
	////////////////////////////////////////////
	////           Analaysis Phase         /////
	////////////////////////////////////////////	
	
	/**
	 * To be invoked after the TS of all the method were constructed, but 
	 * before the analysis starts.
	 */
	public void completeDefinitions() {
		analysisStarted = true;
		ipTS.completeDefinitions();
	}
	
	/**
	 * Action Manipulation
	 */
	
	public boolean contains(Method mtd) {
		return getTS(mtd) != null;
	}
	
	public boolean analysisStarted() {
		return analysisStarted;
	}

	public MethodTS getMain() {
		assert(analysisStarted);
		assert(mainTS != null);
		return mainTS;
	}
	
	
	public InterProcTS getInterProcTS() {
		assert(analysisStarted);
		assert(ipTS != null);
		return ipTS;
	}	

	public Iterator followingIntraEdgesIterator(MethodTS mtdTS, TSNode site) {
		assert(mtdTS != null);
		assert(site != null);
		assert(getTS(mtdTS.getMethod()) != null);		
		return mtdTS.followingEdges(site);
	}
	
	public TSNode getMatchingReturnNode(MethodTS mtd, TSNode node) {
		assert(mtd.containsSite(node));
		assert(node.isCallSite());
		return mtd.getMatchingReturnNode(node);
	}
	
	public MethodTS getStaticCallee(MethodTS mtdTS, TSNode callSite) {
		assert(mtdTS != null);
		assert(callSite != null);
		assert(callSite.isStaticCallSite());
		assert(getTS(mtdTS.getMethod()) != null);		
		
		return ipTS.getStaticTarget(mtdTS,callSite);
	}

	public MethodTS getConstructorCallee(MethodTS mtdTS, TSNode callSite) {
		assert(mtdTS != null);
		assert(callSite != null);
		assert(callSite.isConstructorCallSite());
		assert(getTS(mtdTS.getMethod()) != null);		
		
		return ipTS.getConstructorTarget(mtdTS,callSite);
	}	
		
	public Iterator getPossibleVirtualCallees(
			MethodTS mtdTS, TSNode callSite) {
		assert(mtdTS != null);
		assert(callSite != null);
		assert(callSite.isVirtualCallSite());
		assert(getTS(mtdTS.getMethod()) != null);		
		
		Collection virtualTargets = ipTS.getVirtualTargets(mtdTS,callSite);
		return virtualTargets.iterator();
	}	

	
	
	
	public ActionInstance getCallAction(
			MethodTS caller, TSNode callNode, MethodTS callee) {
		return ipTS.getCallAction(caller,callNode,callee);
	}
	
	public ActionInstance getRetAction(
			MethodTS caller, TSNode callNode, MethodTS callee) {
		return ipTS.getRetAction(caller,callNode,callee);
	}

	public ActionInstance getGuardAction(
			MethodTS caller, TSNode callNode, MethodTS callee) {
		return ipTS.getGuardAction(caller,callNode,callee);
	}
	
	
	/**
	 * 	 State Manipulation 
	 */
	
	public Collection initAnalysis(Collection initial) {
		assert(analysisStarted);
		
		Collection initialASTVSs = HashSetFactory.make();
		TSNode mainEntry = mainTS.getEntrySite();
		assert(mainEntry != null);
		
		Iterator initItr = initial.iterator(); 
		while (initItr.hasNext()) {
			HighLevelTVS tvs = (HighLevelTVS) initItr.next();
			SingleSet ret = new SingleSet(true);
			boolean isNew = mainTS.addTVS(mainEntry,tvs,ret);
			if (isNew) {
				Object asTVS = ret.get();
				assert(asTVS != null);
				assert(asTVS instanceof Fact);
				initialASTVSs.add(asTVS);
			}
		}
		
		return initialASTVSs;
	}
	
	public boolean addTVS(
			MethodTS mtd,
			TSNode toNode, 
			HighLevelTVS tvs, 
			SingleSet mergedTo) {
		assert(mtd != null);
		return mtd.addTVS(toNode, tvs, mergedTo);
	}

	public boolean addTransition(
			MethodTS mtd,
			TSNode fromNode,
			Fact fromFact,
			TSNode toNode,
			Fact toFact) {
		return mtd.addTransition(fromNode,fromFact,toNode,toFact);		
	}
	
	public Collection getKnownEffect(
			MethodTS mtd,
			Fact entryFact) {
		return mtd.getKnownEffect(entryFact);
	}
	
	public Fact getFactForTVS(
			MethodTS mtd, 
			TSNode node,
			HighLevelTVS tvsAtNode) {
		return mtd.getFactForTVS(node,tvsAtNode);
	}
	
	
	public void updateCallingCTXs(
			MethodTS callee, 
			Collection factsAtEntry, 
			MethodTS caller, 
			TSNode callSite,
			Fact ctx, 
			Collection refinedCTX) {
		ipTS.updateCallingCTXs(
				callee,factsAtEntry, 
				caller,callSite,ctx,refinedCTX);
	}
	
	public CallingContext getCallingContext(MethodTS mtd, Fact factAtEntry) {
		CallingContext ctx = ipTS.getCallingContext(mtd,factAtEntry);
		assert(ctx != null);
		return  ctx;
	}
	
	
	public Collection updateSummary(MethodTS mtd) {
		assert(mtd != null);
		return mtd.updateSummary();
	}
	
	/**
	 * For debugging
	 */	

	public boolean isStaticCallSiteOf(
			MethodTS caller,  TSNode callSite, MethodTS callee) {
		return ipTS.isStaticCallSiteOf(caller, callSite, callee); 
	}
	
	public boolean isVirtualCallSiteOf(
			MethodTS caller,  TSNode callSite, MethodTS callee) {
		return ipTS.isVirtualCallSiteOf(caller, callSite, callee); 
	}

	public boolean isConstructorCallSiteOf(
			MethodTS caller,  TSNode callSite, MethodTS callee) {
		return ipTS.isConstructorCallSiteOf(caller, callSite, callee); 
	}

	public boolean isCallSiteOf(
			MethodTS caller,  TSNode callSite, MethodTS callee) {
		return ipTS.isCallSiteOf(caller, callSite, callee);   
	}

	public void printStatistics() {
		ipTS.printStatistics();
	}
	
	//////////////////////////////////////////////////////
	/////                     MISC                   /////
	//////////////////////////////////////////////////////

	private MethodTS getTS(Method mtd) {
		MethodTS ret = null;
		
		assert(mtd != null);
		
		if (mtd.isVirtual()) {
			assert(staticMethodsTS.get(mtd) == null);
			assert(constructorsTS.get(mtd) == null);
			ret = (MethodTS) virtualMethodsTS.get(mtd);
			
		}
		else if (mtd.isStatic()) {
			assert(virtualMethodsTS.get(mtd) == null);
			assert(constructorsTS.get(mtd) == null);
			ret = (MethodTS) staticMethodsTS.get(mtd);
		}
		else {
			assert(mtd.isConstructor());
			assert(staticMethodsTS.get(mtd) == null);
			assert(virtualMethodsTS.get(mtd) == null);
			ret = (MethodTS) constructorsTS.get(mtd);
		}
		
		assert(ret == allMethodsTS.get(mtd));
		return ret;
	}


}
