/*
 * File: InterProcTS.java 
 * Created on: 12/10/2004
 */

package tvla.analysis.interproc.transitionsystem.program;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import tvla.analysis.interproc.semantics.ActionInstance;
import tvla.analysis.interproc.transitionsystem.AbstractState.Fact;
import tvla.analysis.interproc.transitionsystem.method.MethodTS;
import tvla.analysis.interproc.transitionsystem.method.TSNode;
import tvla.util.HashMapFactory;


/** 
 * @author maon
 */
public class InterProcTS {
	static final boolean xdebug = false;
	static final PrintStream out = System.out;
	
	final Map staticCallSites; // maps a static call to a CallSiteStatic 
	final Map ctorCallSites;  // maps a constructor call to a CallSiteCtor
	final Map virtualCallSites; // maps a virtual call-site to a map of calleeTS->CallSiteVirtual
	final TableOfCallingContexts callingCTXs;
	
	boolean analysisStarted;
	/**
	 * 
	 */
	
	public InterProcTS(int numOfMethods,
					   int numOfStaticCallSites,
					   int numOfVirtualCallSites,
					   int numOfCtorCallSites) {
		super();
		staticCallSites = new LinkedHashMap((numOfStaticCallSites * 4) / 3 + 1, 0.75f);
		virtualCallSites = new LinkedHashMap((numOfVirtualCallSites * 4) / 3 + 1, 0.75f);
		ctorCallSites = new LinkedHashMap((numOfCtorCallSites * 4) / 3 + 1, 0.75f);

		callingCTXs = new TableOfCallingContexts(numOfMethods);
		analysisStarted = false;
	}

	public void addStaticMethod(MethodTS mtd) {
		if (xdebug) 
			out.println("InterProcTS.addStaticMethod(" + mtd.getMethod().getSig()+")");

		assert(mtd.getMethod().isStatic());
		assert(!analysisStarted);

		callingCTXs.addTarget(mtd);
	}

	public void addVirtualMethod(MethodTS mtd) {
		if (xdebug) 
			out.println("InterProcTS.addVirtualMethod(" + mtd.getMethod().getSig()+")");

		assert(mtd.getMethod().isVirtual());
		assert(!analysisStarted);
		
		callingCTXs.addTarget(mtd);
	}

	public void addConstructor(MethodTS mtd) {
		if (xdebug) 
			out.println("InterProcTS.addConstructor(" + mtd.getMethod().getSig()+")");

		assert(mtd.getMethod().isConstructor());
		assert(!analysisStarted);
		
		callingCTXs.addTarget(mtd);
	}
	

	public void addStaticInvocation(
			MethodTS caller,
			TSNode callSite,
			MethodTS callee,
			ActionInstance iActionCall, 
			ActionInstance iActionRet) {
		if (xdebug)
			out.println("InterProcTS.addStaticInvocation: " + 
						caller.getMethod().getSig() +
					    "->" + 
					    callee.getMethod().getSig());

		assert(!analysisStarted);
		assert(callee.getMethod().isStatic());

		if (staticCallSites.get(callSite) != null ||
			virtualCallSites.get(callSite) != null ||
			ctorCallSites.get(callSite) != null)
			throw new Error(
					"Duplicate call to " + callee.getMethod().getSig() +
					" at " + callSite.getLabel() + 
					" in " + caller.getMethod().getSig());
		

		CallSiteStatic callNode = new CallSiteStatic(
				caller,callSite,callee,iActionCall,iActionRet);
		
		assert(callNode != null);
		assert(callNode.getCaller() == caller);
		assert(callNode.getCallee() == callee);
		assert(callNode.getCallSite() == callSite);		
		assert(callingCTXs.containsTarget(callee));

		staticCallSites.put(callSite, callNode);
	}

	public void addVirtualInvocation(
			MethodTS caller,
			TSNode callSite,
			MethodTS callee,
			ActionInstance iActionCall, 
			ActionInstance iActionRet,
			ActionInstance iActionGuard) {
		if (xdebug)
			out.println("addVirtualInvocation: " + 
						caller.getMethod().getSig() +
					    "->" + 
					    callee.getMethod().getSig());

		assert(!analysisStarted);
		assert(callee.getMethod().isVirtual());

		if (staticCallSites.get(callSite) != null ||
			ctorCallSites.get(callSite) != null)
			throw new Error(
				"Duplicate call to " + callee.getMethod().getSig() +
				" at " + callSite.getLabel() + 
				" in " + caller.getMethod().getSig());

		
		Map virtualTargets = (Map) virtualCallSites.get(callSite);
		if (virtualTargets == null)  {
			virtualTargets = HashMapFactory.make();
			virtualCallSites.put(callSite, virtualTargets);
		}
		else {
			if (virtualTargets.containsKey(callee)) 
				throw new Error(
						"Duplicate call to " + callee.getMethod().getSig() +
						" at " + callSite.getLabel() + 
						" in " + caller.getMethod().getSig());
		}
		
		CallSiteVirtual callNode = new CallSiteVirtual(
				caller,callSite,callee,iActionCall,iActionRet,iActionGuard);
		
		assert(callNode != null);
		assert(callNode.getCaller() == caller);
		assert(callNode.getCallee() == callee);
		assert(callNode.getCallee() == callee);
		assert(callNode.getCallSite() == callSite);		
		assert(callingCTXs.containsTarget(callee));
		
		virtualTargets.put(callee, callNode);
	}
	
	
	public void addConstructorInvocation(
			MethodTS caller,
			TSNode callSite,
			MethodTS callee,
			ActionInstance iActionCall, 
			ActionInstance iActionRet) {
		if (xdebug)
			out.println("addConstructorInvocation: " + 
						caller.getMethod().getSig() +
					    "->" + 
					    callee.getMethod().getSig());

		assert(!analysisStarted);
		assert(callee.getMethod().isConstructor());

		if (staticCallSites.get(callSite) != null ||
			virtualCallSites.get(callSite) != null ||
			ctorCallSites.get(callSite) != null)
			throw new Error(
					"Duplicate call to " + callee.getMethod().getSig() +
					" at " + callSite.getLabel() + 
					" in " + caller.getMethod().getSig());
		
		CallSiteConstructor callNode = new CallSiteConstructor(
				caller,callSite,callee,iActionCall,iActionRet);
		
		assert(callNode != null);
		assert(callNode.getCaller() == caller);
		assert(callNode.getCallee() == callee);
		assert(callNode.getCallee() == callee);
		assert(callNode.getCallSite() == callSite);
		assert(callingCTXs.containsTarget(callee));

		ctorCallSites.put(callSite, callNode);
	}	
	
	public void completeDefinitions() {
		analysisStarted = true;
	}
	

	
	////////////////////////////////////////////
	////           Analaysis Phase         /////
	////////////////////////////////////////////	
	
	/*
	 * Returns the target method of a call-statement.
	 */
	
	public MethodTS getStaticTarget(MethodTS mtdTS, TSNode callSite) {
		CallSiteStatic ipCallSite = getIPStaticCallSite(mtdTS,callSite);
		assert(ipCallSite != null);
		return ipCallSite.getCallee();
	}
	
	public MethodTS getConstructorTarget(MethodTS mtdTS, TSNode callSite) {
		CallSiteConstructor ipCallSite = getIPConstructorCallSite(mtdTS,callSite);
		assert(ipCallSite != null);
		return ipCallSite.getCallee();
	}

	public Collection getVirtualTargets(MethodTS mtdTS, TSNode callSite) {
		Map virtualTargets = getIPVirtualCallSites(mtdTS,callSite);
		assert(virtualTargets != null);
		return Collections.unmodifiableCollection(virtualTargets.keySet());
	}

	/*
	 * Returns the various interporcedural actions 
	 */
	
	public ActionInstance getCallAction(
			MethodTS caller, TSNode callNode, MethodTS callee) {
		CallSite callSite = getIPCallSite(caller, callNode, callee);
		assert(callSite != null);
		return callSite.getCallAction();
	}
	
	public ActionInstance getRetAction(
			MethodTS caller, TSNode callNode, MethodTS callee) {
		CallSite callSite = getIPCallSite(caller, callNode, callee);
		assert(callSite != null);
		return callSite.getRetAction();
	}

	public ActionInstance getGuardAction(
			MethodTS caller, TSNode callNode, MethodTS callee) {
		CallSiteVirtual callSite = (CallSiteVirtual) getIPCallSite(caller, callNode, callee);
		assert(callSite != null);
		return callSite.getGuardAction();
	}
	
	/**
	 *  Handling calling contexts
	 */
	
	public void updateCallingCTXs(
			MethodTS callee, 
			Collection factsAtEntry, 
			MethodTS caller, 
			TSNode callSite,
			Fact ctx, 
			Collection refinedCtxs) {
		assert(callee!=null && factsAtEntry != null);
		assert(!factsAtEntry.isEmpty());
		assert(caller != null && callSite != null && ctx != null && refinedCtxs != null);
		assert(!refinedCtxs.isEmpty());
		assert(callSite.isCallSite());
		assert(caller.containsSite(callSite));
		assert(isCallSiteOf(caller, callSite, callee)); 
		
		callingCTXs.addToCallingContext(
				callee, 
				factsAtEntry, 
				caller, 
				callSite,
				ctx, 
				refinedCtxs);
	}

	
	public CallingContext getCallingContext(MethodTS mtd, Fact factAtEntry) {
		CallingContext ctx = callingCTXs.getCallingContext(mtd,factAtEntry);
		assert(ctx != null);
		return  ctx;
	}	
	/**
	 * For debugging
	 */	

	public boolean isStaticCallSiteOf(
			MethodTS caller, TSNode callSite, MethodTS callee) {
		CallSiteStatic callNode = getIPStaticCallSite(caller, callSite);
		if (callNode == null)
			return false;
		return (callNode.getCallee() == callee);
	}
	
	public boolean isVirtualCallSiteOf(
			MethodTS caller, TSNode callSite, MethodTS callee) {
		Map virtualTargets = getIPVirtualCallSites(caller, callSite);
		if (virtualTargets == null)
			return false;
		
		return virtualTargets.containsKey(callee);
	}

	
	public boolean isConstructorCallSiteOf(
			MethodTS caller, TSNode callSite, MethodTS callee) {
		CallSiteConstructor callNode = getIPConstructorCallSite(caller, callSite);
		if (callNode == null)
			return false;
		return (callNode.getCallee() == callee);
	}

	public boolean isCallSiteOf(
			MethodTS caller,  TSNode callSite, MethodTS callee) 
	{
		assert(callSite.isCallSite());
		
		if (callSite.isStaticCallSite())
			return  isStaticCallSiteOf(caller, callSite, callee);
		
		if (callSite.isVirtualCallSite())
			return	isVirtualCallSiteOf(caller, callSite, callee);
		
		if (callSite.isConstructorCallSite())
			return isConstructorCallSiteOf(caller, callSite, callee);
		
		throw new InternalError("Node " + callSite.getLabel() + " is not a call site!");
	}


	/////////////////////////////////////////////////////////
	///                  Logging                          ///
	/////////////////////////////////////////////////////////	
	
	
	public void printStatistics() {
		callingCTXs.printStatistics();
	}
	
	/////////////////////////////////////////////////////////
	///                  Internal Stuff                   ///
	/////////////////////////////////////////////////////////

	private CallSiteStatic getIPStaticCallSite(
			MethodTS caller, TSNode callSite) {
		assert(caller != null && callSite != null);
		assert(callSite.isStaticCallSite());
		assert(caller.containsSite(callSite));
		
		CallSiteStatic callNode = (CallSiteStatic) staticCallSites.get(callSite);
	
		if (callNode == null)
			return null;
		
		assert(callNode.caller == caller); 
	
		return callNode;
	}
	
	private Map getIPVirtualCallSites(
			MethodTS caller, TSNode callSite) {
		assert(caller != null && callSite != null);
		assert(callSite.isVirtualCallSite());
		assert(caller.containsSite(callSite));
		
		Map virtualTargets = (Map) virtualCallSites.get(callSite);

		return virtualTargets;
	}

	
	private CallSiteConstructor getIPConstructorCallSite(
			MethodTS caller, TSNode callSite) {
		assert(caller != null && callSite != null);
		assert(callSite.isConstructorCallSite());
		assert(caller.containsSite(callSite));

		CallSiteConstructor callNode = (CallSiteConstructor) ctorCallSites.get(callSite);

		if (callNode == null)
			return null;
	
		assert(callNode.caller == caller); 
		
		return callNode;
	}

	public CallSite getIPCallSite(
			MethodTS caller, TSNode callSite, MethodTS callee) {
		assert(caller != null);
		assert(callee != null);
		assert(callSite != null);
		
		CallSite ret = null;
		switch(callSite.getType()) {
		case TSNode.STATIC_CALL_SITE :
			ret = getIPStaticCallSite(caller,callSite);
			break;

		case TSNode.VIRTUAL_CALL_SITE :
			Map target = getIPVirtualCallSites(caller,callSite);
			ret = (CallSite) target.get(callee);
			break;

		case TSNode.CONSTRUCTOR_CALL_SITE :
			ret = getIPConstructorCallSite(caller,callSite);
			break;

		default:
			throw new InternalError ("Unknon call-site type " + callSite.getType());
		}
		
		assert(ret != null);
		return ret;
	}
}

