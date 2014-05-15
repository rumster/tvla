/*
 * File: CFGEdgeIntra.java 
 * Created on: 20/10/2004
 */

package tvla.analysis.interproc.transitionsystem.method;

import tvla.analysis.interproc.semantics.ActionInstance;

/** 
 * @author maon
 */
public interface CFGEdgeIntra extends CFGEdge {
	public abstract ActionInstance getActionInstance();
}