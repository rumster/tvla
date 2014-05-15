/*
 * File: TVSRepository.java 
 * Created on: 13/10/2004
 */

package tvla.analysis.interproc.transitionsystem;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import tvla.core.HighLevelTVS;
import tvla.core.TVSFactory;
import tvla.core.TVSSet;
import tvla.util.Pair;
import tvla.util.SingleSet;


/** 
 * A repository is a TVS "universe" which can back many sets (BackedTVSSet).
 * Each set contains a subset of the TVSs in the repository.
 * A TVS is identified by a TVSInstance. Several instances can share the 
 * ame underlying TVS. The TVSInstances identify a TVS even if it changes.
 * 
 * Assumptions:
 * + When merging a tvla.core.HighLevelTVS S into a tvla.core.TVSSet which contains
 *   an isomprphic strucutre / less precise structure S' using
 *   mergeWith(HighLevelTVS, Collection mergedWith) then S' is returned.
 * + A Structure which is inserted into a TVSSet backing a BackedTVSSet is nver removed.
 * + The methods hashCode and equals retrun consistent results even if a strucutre changes.
 *
 *  * @author maon
 */
public final class TVSRepository {
	private static boolean xdebug = false;
	private static PrintStream out = System.out;
	/**
	 * 
	 */
	public static TVSRepository newTVSRepository() {
		return new TVSRepository();
	}
	
	private TVSRepository() {
		super();
	}
	
	public BackedTVSSet allocateAbstractState() {
		return new BackedTVSSet();
	}
	
	
	///////////////////////////////////////////////
	///                Set of facts             ///
	///////////////////////////////////////////////

	public final class BackedTVSSet implements AbstractState{
		protected static final boolean xxdebug = true;
		protected TVSSet collected;
		protected Map messages;
		protected long factIds = 0;
		
		private BackedTVSSet() {
			collected = TVSFactory.getInstance().makeEmptySet();
			if (xdebug) { 
				String joinMethod;
				switch (TVSFactory.joinMethod) {
					case TVSFactory.JOIN_CANONIC: joinMethod = "canonic"; break;
					case TVSFactory.JOIN_CANONIC_EMBEDDING: joinMethod = "canonic embedding"; break;
					case TVSFactory.JOIN_RELATIONAL: joinMethod = "relational"; break;
					case TVSFactory.JOIN_J3: joinMethod = "part"; break;
					case TVSFactory.JOIN_INDEPENDENT_ATTRIBUTES: joinMethod = "independent attribute"; break;
					default:
						joinMethod = "unKnwon join metod";
				}
				out.println("Woking with join " + joinMethod);
			}
		 
			if (TVSFactory.joinMethod != TVSFactory.JOIN_RELATIONAL &&
				TVSFactory.joinMethod != TVSFactory.JOIN_CANONIC) 
				throw new Error("Analysis works only with relational join or canoninc (partial) join");

			messages = new LinkedHashMap();
		}
		
		public boolean addTVS(HighLevelTVS tvs, SingleSet mergedTo) {
			assert(tvs != null && mergedTo != null);
			assert(mergedTo.isEmpty());
			assert(mergedTo.isBounded());
			
			boolean changed = collected.mergeWith(tvs, mergedTo);
			assert(mergedTo.size() == 0 || mergedTo.size() == 1);
			HighLevelTVS mergedTVS = null;
			/// After PASTA reinsertion
			if ( mergedTo.size() == 1) {
				Pair pair = (Pair) mergedTo.extract();
				assert((HighLevelTVS) pair.first == tvs);
				mergedTVS = (HighLevelTVS) pair.second;
			}
			else
				mergedTVS = tvs;
			/// After Pasta reinsertion
			
			Fact wrapper = new TVSInstance(mergedTVS);
			assert(mergedTo.isEmpty());
			mergedTo.add(wrapper);
			
			return changed;
		}
		
		public boolean containsFact(Fact fact) {
			assert(fact != null);
			assert(fact instanceof TVSInstance);
//			TVSInstance handle = (TVSInstance) fact;
			
			SingleSet mergedTo = new SingleSet(true);			
			boolean changed = collected.mergeWith(fact.getTVS(), mergedTo);
		
			if (changed)
				return false;

			return true;
		}
		
		
		public TVSRepository getTVSRepository() {
			return TVSRepository.this;
		}
		
		public Fact getFactForExistingTVS(HighLevelTVS tvsInState) {
			Fact wrapper = new TVSInstance(tvsInState);
			boolean in = containsFact(wrapper);
			assert(in);
			return wrapper;
		}
		
		public Iterator getTVSsItr() {
			return collected.iterator();
		}

		
		///////////////////////////////////////////////
		///          TVS wrapper == a fact          ///
		///////////////////////////////////////////////
		
		public Map allocateMessagesMap() {
			return new LinkedHashMap();
		}
		
		public void addMessages(
				Fact propagatedTVS,
				Map focusedTVSToMessages) {
			Map existingMsgMap = (Map) messages.get(propagatedTVS);
			if (existingMsgMap == null) {
				messages.put(propagatedTVS, focusedTVSToMessages);
			}
			else {
				Iterator msgItr = focusedTVSToMessages.keySet().iterator();
				while (msgItr.hasNext()) {
					HighLevelTVS focusedTVS = (HighLevelTVS) msgItr.next();
					Collection newMsgs = (Collection) focusedTVSToMessages.get(focusedTVS);
					assert(newMsgs!= null);
					assert(!newMsgs.isEmpty());
					Collection existingMsgs = (Collection) existingMsgMap.get(focusedTVS);
					if (existingMsgs == null) 
						existingMsgMap.put(focusedTVS, newMsgs);
					else
						existingMsgs.addAll(newMsgs);
				}
			}
		}
		
		public Map getMessages(Fact propagatedTVS) {
			return (Map) messages.get(propagatedTVS);
		}
		
		public Map getMessages() {
			return messages;
		}
		
		///////////////////////////////////////////////
		///          TVS wrapper == a fact          ///
		///////////////////////////////////////////////
		
		public final class TVSInstance implements Fact {
			private HighLevelTVS tvs;
		
			public TVSInstance(HighLevelTVS tvs) {
				this.tvs = tvs;
			}
			
			public HighLevelTVS getTVS() {
				return tvs;
			}
			
			public AbstractState getContainingState() {
				return getBackedTVSSet();
			}
			
			public BackedTVSSet getBackedTVSSet() {
				return BackedTVSSet.this;
			}

			public TVSRepository getTVSRepository() {
				return BackedTVSSet.this.getTVSRepository();
			}
			
			public boolean equals(Object obj) {
				if (obj == null)
					return false;					
				if (!(obj instanceof TVSInstance))
					return false;
				TVSInstance other = (TVSInstance) obj;
				if (other.tvs != this.tvs ||
					BackedTVSSet.this != other.getBackedTVSSet() ||
					BackedTVSSet.this.getTVSRepository() != other.getTVSRepository())
					return false;

				return true;
			}
			
			public int hashCode() {
				return tvs.hashCode();
			}
		}
	}

}


/***************************************************/
/***                   END                       ***/
/***************************************************/

///////////////////////////////////////////////
///          fact -> fact transition        ///
///////////////////////////////////////////////
/*
public AbstractTransition allocateAbstractTransition(
		ExplodedFlowGraph factsGraph,
		AbstractState src,
		AbstractState dst) {
	assert (src instanceof TVSRepository.BackedTVSSet);
	assert (dst instanceof TVSRepository.BackedTVSSet);
	assert (((TVSRepository.BackedTVSSet) src).getTVSRepository() == 
		    ((TVSRepository.BackedTVSSet) dst).getTVSRepository());
	
	return new Transition(src,dst);
}
*/
/*	public class Transition implements AbstractTransition {
	private final AbstractState src;
	private final AbstractState dst;
	
	private Transition(AbstractState src, AbstractState dst) {
		this.src = src;
		this.dst = dst;
	}
}*/
/*

public boolean addTransition(AbstractState.TVS src, AbstractState.TVS dst){
	return false;
}


*//** 
 *  Adds a transition from src to to.
 *  
 * @param src source strucutre
 * @param to target strucutre
 * @return whether the transition is new 
 *//*
public boolean addTransition(TVSRepository.BackedTVSSet.TVSInstance src,
					        TVSRepository.BackedTVSSet.TVSInstance to) {
	if (transition.edgeExists(src, to))
		return false;
	
	transition.addEdge(src, to);
	return true;
}

public boolean existsTransition(TVSRepository.BackedTVSSet.TVSInstance src,
        					    TVSRepository.BackedTVSSet.TVSInstance to) {
	return transition.edgeExists(src, to);
}

public Collection getDestinations(TVSRepository.BackedTVSSet.TVSInstance src) {
	return transition.getOutgoingNodes(src);
}

public Collection getOrigins(TVSRepository.BackedTVSSet.TVSInstance dst) {
	return transition.getIncomingNodes(dst);
}


*/