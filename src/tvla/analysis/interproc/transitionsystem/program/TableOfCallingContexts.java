/*
 * File: MethidCallingContexts.java 
 * Created on: 22/09/2004
 */

package tvla.analysis.interproc.transitionsystem.program;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import tvla.analysis.interproc.transitionsystem.AbstractState.Fact;
import tvla.analysis.interproc.transitionsystem.method.CFG;
import tvla.analysis.interproc.transitionsystem.method.CFGNode;
import tvla.analysis.interproc.transitionsystem.method.MethodTS;
import tvla.analysis.interproc.transitionsystem.method.TSNode;
import tvla.util.HashSetFactory;
import tvla.util.Logger;

/** Contains all the callers for a method.
 * This is basically a map method-> Structure at entry -> CallingContext 

 *  * @note Once a calling context "registered its interest" in an input strucutre,
 * it cannot "unregister".
 * @author maon
 */
public final class TableOfCallingContexts {
	//private static final boolean xdebug = true;
	//private static final boolean xxdebug = true;
	//private static final java.io.PrintStream out = System.out;

	
	Map mtdToCTXs;
	
    ///////////////////////////////////////////
	///          Construction Phase         ///
    ///////////////////////////////////////////
	public TableOfCallingContexts(int numOfMethods) {
		super();
		mtdToCTXs = new LinkedHashMap((numOfMethods * 4) / 3 + 1, 0.75f);
	}

	/**
	 * Adds a target (method) for a method call.
	 * Each method is added only once. 
	 * Every method shoould be added.
	 * Must be performed before the analysis starts.
	 * @param mtd the addded method.
	 */
	public void addTarget(MethodTS mtd) {
		Map mtdCTXs = (Map) mtdToCTXs.get(mtd);
		if (mtdCTXs == null) {
			mtdCTXs = new LinkedHashMap();
			mtdToCTXs.put(mtd, mtdCTXs);
		}
		assert(mtdCTXs.isEmpty());
	}

	public boolean containsTarget(MethodTS mtd) {
		assert(mtd != null);
		return  mtdToCTXs.containsKey(mtd);
	}

	
	////////////////////////////////////////////////////////
	///                  Analysis Phase                  ///
	////////////////////////////////////////////////////////

	
    ///////////////////////////////////////////
	///              Mutators               ///
    ///////////////////////////////////////////	

	public void addToCallingContext(
			MethodTS callee, 
			Collection factsAtEntry, 
			MethodTS caller, 
			TSNode callSite,
			Fact callFact, 
			Collection refinedCallFacts) {
		assert(callee!=null && factsAtEntry != null);
		assert(!factsAtEntry.isEmpty());
		assert(caller != null && callSite != null && callFact != null && refinedCallFacts != null);
		assert(!refinedCallFacts.isEmpty());
		assert(callSite.isCallSite());
		assert(caller.containsSite(callSite));

		Map mtdCTXs = (Map) mtdToCTXs.get(callee);
		assert(mtdCTXs != null);
		
		Iterator factItr = factsAtEntry.iterator();
		while (factItr.hasNext()) {
			Object factObj = factItr.next();
			Fact factAtEntry = (Fact) factObj;
			CallingContext ctx = (CallingContext) mtdCTXs.get(factAtEntry);

			if (ctx == null) {
				ctx = new CallingContext();
				mtdCTXs.put(factAtEntry,ctx);
			}
			
			ctx.update(callSite,callFact,refinedCallFacts);
		}
	}
	
	
    ///////////////////////////////////////////
	///              Accessors              ///
    ///////////////////////////////////////////	

	/**
	 * Retruns all the caling contexts of a given method
	 * @param mtd
	 * @return
	 * 
	 */	 
	public Collection getCallingContext(MethodTS mtd) {
		assert(containsTarget(mtd));
		
		Map mtdCTXs = (Map) mtdToCTXs.get(mtd);
		Collection allCallingCTXs = mtdCTXs.values();
		
		return allCallingCTXs;
	}

	/**
	 * Retruns all the caling contexts of a given method
	 * and a given fact at the entry to the method.
	 * @param mtd the method
	 * @param fact  the fact at the entry to the method
	 * @return
	 * 
	 */	 
	public CallingContext getCallingContext(MethodTS mtd, Fact factAtEntry) {
		assert(mtd != null && factAtEntry != null);
		assert(containsTarget(mtd));
		
		Map mtdCTXs = (Map) mtdToCTXs.get(mtd);
		assert(mtdCTXs != null);
		CallingContext callingContext = (CallingContext) mtdCTXs.get(factAtEntry);
		assert(callingContext != null);
		
		return callingContext;
	}
	
	/**
	 * Retruns all the callers of a given method
	 * @param mtd
	 * @return
	 * 
	 */	 
	public Collection getCallers(MethodTS mtd) {
		Collection allCallingCTXs = getCallingContext(mtd);
		
		Set callSites = HashSetFactory.make();
		Iterator mtdCTXItr = allCallingCTXs.iterator();
		while (mtdCTXItr.hasNext()) {
			CallingContext ctx = (CallingContext) mtdCTXItr.next();
			callSites.addAll(ctx.getAllCallSites());
		}
	
		Set callers = HashSetFactory.make();
		Iterator itr = callers.iterator();
		while (itr.hasNext()) {
			CFGNode callSite = (CFGNode) itr.next(); 
			CFG cfg = callSite.getCFG();
			MethodTS caller = cfg.getMethodTS();
			callers.add(caller);
		}
		
		return callers;
	}
	
	public void printStatistics() {
		Iterator mtdItr = mtdToCTXs.keySet().iterator();
		
		while (mtdItr.hasNext()) {
			MethodTS mtdTS = (MethodTS) mtdItr.next();
			Map mtdCTXs = (Map) mtdToCTXs.get(mtdTS);
		
			Iterator factItr = mtdCTXs.keySet().iterator();
			int numOfCallingCTXs = 0;
			while (factItr.hasNext()) {
				Object factObj = factItr.next();
				Fact factAtEntry = (Fact) factObj;
				CallingContext ctx = (CallingContext) mtdCTXs.get(factAtEntry);
				int numOfCTXForFact = ctx.getNumOfCTXs();
				numOfCallingCTXs += numOfCTXForFact;			
			}
			
			Logger.println("Method " + mtdTS.getSig() 
					       + " has entry TVSs / Calling Contexts \t" 
					       + mtdCTXs.keySet().size() 
					       + " / " + numOfCallingCTXs);

		}
	}

}
