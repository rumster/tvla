/*
 * File: MethodCallingContexts.java 
 * Created on: 22/09/2004
 */

package tvla.analysis.interproc.transitionsystem.program;

/** Contains all the callers for a method.
 * This is basically a mapping of 
 * strucutre at the entry (input-structure) 
 * to 
 * the set of calling contexts. For every caller we keep the the 
 * cfg in which the call occured + the "calling" structure.
 * 
 * @note Once a calling context "registered its interest" in an 
 * input strucutre it cannot "unregister".
 * 
 * @author maon
 */
public class MethodCallingContexts {

	/**
	 * 
	 */
	public MethodCallingContexts() {
		super();
		// TODO Auto-generated constructor stub
	}

}
