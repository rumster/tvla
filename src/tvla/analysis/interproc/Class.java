/*
 * Created on 22/09/2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package tvla.analysis.interproc;


/**
 * An analyzed class in the analyzed program. 
 * @author maon
 */
public final class Class extends Type {
	/**
	 * 
	 */
	public Class(String name) {
		super(name);
	}
		
	public void dump(java.io.PrintStream out) {
		out.println("CLASS: " + name);
	}
}
