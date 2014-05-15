/*
 * File: CFGEdge.java 
 * Created on: 20/10/2004
 */

package tvla.analysis.interproc.transitionsystem.method;

/** 
 * @author maon
 */
public interface CFGEdge {
	// prints the id (false) or the title (true)
	public static boolean printTitle = false;
	
	public CFGNode getSource(); 

	public CFGNode getDestination();
	
	public String getLabel();
	
	public String title();
	
	public long getId();
}