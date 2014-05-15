/*
 * File: TSNode.java 
 * Created on: 22/09/2004
 */

package tvla.analysis.interproc.transitionsystem.method;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import tvla.analysis.interproc.transitionsystem.AbstractState;
import tvla.util.HashMapFactory;




/** A MethodTS Node in a specific MethodTS
 *  
 * @author maon
 */
public class TSNode implements ProgramLocation {
	protected final String label;
	protected final AbstractState state;
	protected final CFG cfg;

	protected int type;  // INTRA type might change to CALL_SITE
	private boolean shouldPrint;
	
	// Node unique idnetification (within the scope of a MethodTS)
	private long id = 0;

	// Prefix of label used in printing of results t the same file to 
	// discriminate the labels of differernt methods
	// The prefix is used only in the label() method
	private String labelPrefix;
	
	
	/////////////////////////////////////////////////
	/////              Constructors             /////
    /////////////////////////////////////////////////

	private TSNode(
			CFG cfg,
			long nodeId,
			int type,
			String label,
			AbstractState state) {
		super();
		this.cfg = cfg;
		this.id = nodeId;
		this.type = type;
		this.label = label;
		this.state = state;
		this.shouldPrint = false;
	}
	
	public static TSNode newMethodTSNode(
			int nodeType, 
			CFG cfg, 
			long nodeId,
			String label, 
			AbstractState state) {
		switch(nodeType) {
			case ENTRY_SITE:
				return new TSNode(cfg, nodeId, ENTRY_SITE, label, state);
			case EXIT_SITE:
				return new TSNode(cfg, nodeId, EXIT_SITE, label, state);
			case INTRA:
				return new TSNode(cfg, nodeId, INTRA, label, state);
			case STATIC_CALL_SITE:
				return new TSNode(cfg, nodeId, STATIC_CALL_SITE, label, state);
			case VIRTUAL_CALL_SITE:
				return new TSNode(cfg, nodeId, VIRTUAL_CALL_SITE, label, state);
			case CONSTRUCTOR_CALL_SITE:
				return new TSNode(cfg, nodeId, CONSTRUCTOR_CALL_SITE, label, state);
			case RET_SITE:
				return new TSNode(cfg, nodeId, RET_SITE, label, state);
			default: 
				throw new InternalError("newMethodTSNode: Unknown node type " + nodeType);
		}
	}

	/////////////////////////////////////////////////
	/////                CFGNode                /////
    /////////////////////////////////////////////////

	
	public String getLabel() {
		return label;
	}
		
	public boolean isRetSite() {
		return type == RET_SITE;
	}

	public boolean isEntrySite() {
		return type == ENTRY_SITE;
	}

	public boolean isExitSite() {
		return type == EXIT_SITE;
	}
	
	public boolean isStaticCallSite() {
		return type == STATIC_CALL_SITE;
	}
	
	public boolean isVirtualCallSite() {
		return type == VIRTUAL_CALL_SITE;
	}

	public boolean isConstructorCallSite() {
		return type == CONSTRUCTOR_CALL_SITE;
	}

	public boolean isCallSite() {
		return isCallType(type);
	}
	
	public boolean isIntraStmtSite() {
		return type == INTRA;
	}
	
	public int getType() {
		return type;
	}
	
	public String getTypeString() {
		switch (type) {
		case ENTRY_SITE: return "Entry";  
		case EXIT_SITE: return "Exit";  
		case INTRA: return "Intra";  
		case STATIC_CALL_SITE: return "sCall";  
		case VIRTUAL_CALL_SITE: return "vCall";  
		case CONSTRUCTOR_CALL_SITE: return "cCall";  
		case RET_SITE: return "Return"; 
		default:
			throw new InternalError("getTypeString: unknown node type " + type);
		}
	}	
	
	/**
	 *  Sets a node which was previously taken as an intra node
	 * (the target of an intra statement edge) as a call site
	 * @param callSiteType shold be either STATIC_CALL_SITE,
	 * VIRTUAL_CALL_SITE, or CONSTRUCTOR_CALL_SITE.
	 */
	void setAsCallSite(int callSiteType) {
		assert(type == INTRA);
		assert(isCallType(callSiteType));
		type = callSiteType;
	}
	
	public static boolean isCallType(int type) {
		return  (type == STATIC_CALL_SITE      ||
				 type == VIRTUAL_CALL_SITE     ||
				 type == CONSTRUCTOR_CALL_SITE  );
	}

	public static boolean isIntraStmtType(int type) {
		return  (type == INTRA);
	}

	public CFG getCFG() {
		return cfg;
	}
	
	public long getId() {
		return id;
	}

	/////////////////////////////////////////////////
	/////           AbstractStateNode           /////
	/////////////////////////////////////////////////
	
	public AbstractState getAbstractState() {
		return state;
	}
	
	
	/////////////////////////////////////////////////
	/////       PrintableProgramLocation        /////
    /////////////////////////////////////////////////
	
	
	public String label() {
		String ret = getTypeString() + ": " + getLabel();
		if (labelPrefix != null) 
			ret = labelPrefix + ret;
		
		return ret;
	}
	
	public String setLabelPrefix(String prfLbl) {
		String old = labelPrefix;
		labelPrefix = prfLbl;
		return old;
	}
	
	public 	boolean setShouldPrint(boolean val) {
		boolean old = this.shouldPrint;
		this.shouldPrint = val;
		return old;
	}
	
	public boolean getShouldPrint() {
		return cfg.printAllNodes() || this.shouldPrint;
	}	

	public Iterator getStructuresIterator() {
		return state.getTVSsItr();
	}

	/**
	 * Returns a map from foucused strucutres to messages
	 * This makes the map compatible with the map of Intraprocedural Location
	 */
	
	public Map getMessages() {
		Map map = HashMapFactory.make();

		Iterator tvsItr = state.getMessages().values().iterator();
		while (tvsItr.hasNext()) {
			Map focusedTVSToMsgs = (Map) tvsItr.next();
			Iterator focusedTVSItr = focusedTVSToMsgs.keySet().iterator();
			while (focusedTVSItr.hasNext()) {
				Object tvs = focusedTVSItr.next();
				Collection focusedTVSMsgs = (Collection) focusedTVSToMsgs.get(tvs);
				Collection currentMsgs = (Collection) map.get(tvs);
				if (currentMsgs == null)
					map.put(tvs,focusedTVSMsgs);
				else
					currentMsgs.addAll(focusedTVSMsgs);
			}
		}
				
		return map;
	}
	
	/////////////////////////////////////////////////
	/////               Object                  /////
    /////////////////////////////////////////////////
	
	public boolean equals(Object other) {
		if (other == null)
			return false;
		
		return other == this;
	}
	
	public int hashCode() {
		return label.hashCode();
	}
	
	public String toString() {
		return " TSNode type: " + type + " (" + getTypeString() + ") " +
		       " label: " + label + 
			   " method: " + cfg.getSig();
	}
}
