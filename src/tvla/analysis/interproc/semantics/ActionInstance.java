/*
 * File: ActionInstance.java 
 * Created on: 22/09/2004
 */

package tvla.analysis.interproc.semantics;

import java.util.List;

import tvla.language.PTS.ActionMacroAST;
import tvla.language.TVP.ActionDefAST;
import tvla.transitionSystem.Action;
import tvla.util.Cache;
import tvla.util.LRUCache;

/**  
 * An instance of a specific action.
 * This is a wrapper for an action. 
 * Action instances are uniquely identified by getId 
 * The instantiated actions are cached. 
 * * @author maon
 */
public class ActionInstance {
	static private final int MAX_ACTION_INSTANTIATIONS = 500; 
	static private 	Cache cachedActions = new LRUCache(MAX_ACTION_INSTANTIATIONS);
	
	private String id;
	private ActionDefinition definition;
    
	private List actualArgs;
	private String title;

 	/**
	 * 
	 */
	protected ActionInstance(ActionDefinition def, List args) {
		assert(def != null);
		assert(args != null);
		
		this.definition = def;
		this.actualArgs = args;	
		this.id = getId(def,args);
		this.title = null;		
  	}	

	public static ActionInstance getActionInstance(ActionDefinition def, List args){
		return new ActionInstance(def, args);
	}
	
	public static String getId(ActionDefinition def, List args){
		assert(def != null);
		assert(args != null);

		StringBuffer id = new StringBuffer(def.getName());
		id.append('(');
		for (int i=0; i<args.size(); i++) {
			id.append((String) args.get(i));
			if (i < args.size()-1)
				id.append(',');
		}
		id.append(')');
		
		return id.toString();
	}
	
	
	/*
	 * We use this metod to discriminate between methods that are already in the cache
	 * (and thus had been applied differencing) and methods that are generated from their definition
	 * (and may need to have differencing apply to them)
	 */
	public boolean cachedAction(){
		Action action = (Action) cachedActions.get(id);
		return action != null;
	}
	
	public Action getAction(){
		Action action;
		
		action = (Action) cachedActions.get(id);
		if (action != null) {
			return action;
		}
		
		ActionMacroAST macro = definition.getMacro();
		ActionDefAST instantiation = macro.expand(actualArgs);
		action = instantiation.getAction();
		title = action.toString();
		
		cachedActions.put(id,action);
	
		
		return action;
	}
    
	public String getMacroName(boolean withArgs) {
		if (!withArgs)
			return definition.getName();
		
		return getId(definition,actualArgs);
	}
	
	public String getTitle() {
		Action action;
		
		if (title != null)
			return title;
		
		action = getAction();

		title = action.toString();
		
		return title;
	}
	
	public String toString() {
		return getMacroName(true);
	}
}
