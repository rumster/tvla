/*
 * File: AbstractState.java 
 * Created on: 15/10/2004
 */

package tvla.analysis.interproc.transitionsystem;

import java.util.Iterator;
import java.util.Map;

import tvla.core.HighLevelTVS;
import tvla.util.SingleSet;

/** Abstract state - a collection of handles for TVSs.
 * Hides the "join" operation behind it.
 * @author maon
 */
public interface AbstractState {
	public boolean addTVS(HighLevelTVS tvs, SingleSet mergedTo);
	
	public void addMessages(
			AbstractState.Fact propagatedFact,
			Map focusedTVSToMessages);
	
	// Get map of focused structures of the given fact to messages.
	public Map getMessages(AbstractState.Fact propagatedFact);

	// Get messages from propagated facts to focused strucutres to messages.
	public Map getMessages();
	
	public Map allocateMessagesMap();
	
	public Fact getFactForExistingTVS(HighLevelTVS tvsInState);

	/**
	 * used to assert whenter a fact is in a state. 
	 * To be used in assertions only - if returns falsem we're in trouble.
	 * If the fact is not in the set - the set is changed !
	 * @param fact
	 * @return
	 */
	public boolean containsFact(Fact fact);
	
	public Iterator getTVSsItr();
	
	
	public interface Fact  {
		public HighLevelTVS getTVS();
		public AbstractState getContainingState();
	}
}
