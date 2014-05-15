/*
 * File: CallingContext.java 
 * Created on: 22/09/2004
 */

package tvla.analysis.interproc.transitionsystem.program;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import tvla.analysis.interproc.transitionsystem.AbstractState.Fact;
import tvla.analysis.interproc.transitionsystem.method.TSNode;
import tvla.util.HashSetFactory;

/** A calling context for a < method , structure at entry >.
 * This is basically a map form basic contexts (<callNode, Strucutre at Call site>)
 * ot collection of (refined) calling contexts.
 * Implemnted a map:
 *  => from <call Node, Structure at call-site> to a set of refined calling contexts.
 * 
 * FIXME improve implementation for the cases in which the refined set contais only 
 * the strucutre at the calling context.
 * 
 * @author maon
 */

public final class CallingContext {
	//private static final boolean xdebug = true;
	//private static final boolean xxdebug = true;
	//private static final java.io.PrintStream out = System.out;
	
	final private Map bcToRefinedFacts;

	
	////////////////////////////////////////////////////////
	///                  Analysis Phase                  ///
	////////////////////////////////////////////////////////

	///////////////////////////////////////////
	///              Mutators               ///
    ///////////////////////////////////////////	
	
	public CallingContext() 
	{
		bcToRefinedFacts = new LinkedHashMap();
	}
	
 	
	public boolean update(
			TSNode callSite,
			Fact callFact,
			Collection refinedFacts) {
		assert(callSite != null && callFact != null && callSite != null);
		assert(callSite.isCallSite());
		assert(callFact.getContainingState() == callSite.getAbstractState());
		
		BasicCTX bc = new BasicCTX(callSite,callFact);
		Collection storedRefinedFacts = (Collection)  bcToRefinedFacts.get(bc);
		
		if (storedRefinedFacts == null) {
			bcToRefinedFacts.put(bc,refinedFacts);
			return true;
		}
		
		assert(refinedFacts.containsAll(storedRefinedFacts));
		assert(storedRefinedFacts.containsAll(refinedFacts));
		
		return false;
	}

	
    ///////////////////////////////////////////
	///              Accessors              ///
    ///////////////////////////////////////////	

	public Collection getAllBasicCTXs() {
		return bcToRefinedFacts.keySet();
	}
	
	public Collection getAllCallSites() {
		Set ret = HashSetFactory.make();
		Iterator bcItr = getAllBasicCTXs().iterator();
		
		while (bcItr.hasNext()) {
			BasicCTX bc = (BasicCTX) bcItr.next();
			TSNode callSite = bc.getCallSite();
			assert(callSite.isCallSite());
			ret.add(callSite);
		}
		
		return ret;
	}

	public Collection getRefinedFacts(BasicCTX bc) {
		return (Collection)  bcToRefinedFacts.get(bc);
	}
	
	public int getNumOfCTXs() {
		return getAllBasicCTXs().size();
	}
	
	public static class BasicCTX {
		private final TSNode callSite;
		private final Fact callFact;
		
		BasicCTX(TSNode callSite, Fact callFact) {
			this.callSite=callSite;
			this.callFact=callFact;
		}
		
		public TSNode getCallSite() {
			return callSite;
		}
		
		public Fact getCallingFact() {
			return callFact;
		}

		public boolean equals(Object other) {
			if (other==null)
				return false;
			if (!(other instanceof BasicCTX))
				return false;
			
			BasicCTX otherBC = (BasicCTX) other;
			return (callSite.equals(otherBC.getCallSite()) &&
					callFact.equals(otherBC.getCallingFact()));
		}

		public int hashCode() {
			return callSite.hashCode() * 7 + callFact.hashCode() * 3 ;
		}
		
		public String toString() {
			return "BasicContext at " + callSite.getLabel() + 
			       " in method " + callSite.getCFG().getSig();
		}
	}
}
