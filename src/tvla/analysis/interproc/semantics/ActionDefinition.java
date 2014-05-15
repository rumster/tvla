/*
 * File: ActionDefinition.java 
 * Created on: 11/10/2004
 */

package tvla.analysis.interproc.semantics;

import tvla.language.PTS.ActionMacroAST;


/** 
 * Contains the inofrmation regarding a macro definition. 
 * @author maon
 */
public class ActionDefinition {
	ActionMacroAST macro;
	
	/**
	 * 
	 */
	public ActionDefinition(ActionMacroAST macro) {
		this.macro = macro;
	}
	
	public String getName() {
		return macro.getName();
	}
	
	public tvla.language.PTS.ActionMacroAST getMacro() {
		return macro;
	}
    
    public String toString() {
      return macro.toString();
    }
}
