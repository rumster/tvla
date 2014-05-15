/*
 * File: CFGNode.java 
 * Created on: 20/10/2004
 */

package tvla.analysis.interproc.transitionsystem.method;

import tvla.transitionSystem.PrintableProgramLocation;

/** An interface for a node in th CFG. 
 * Proides access only to the "program properties" of the program 
 * location, e.g., label, actions, etc.
 * @author maon
 */
public interface CFGNode extends PrintableProgramLocation {
	public static final int ENTRY_SITE = 0;

	public static final int EXIT_SITE = 1;

	public static final int INTRA = 2;

	public static final int STATIC_CALL_SITE = 3;

	public static final int VIRTUAL_CALL_SITE = 4;

	public static final int CONSTRUCTOR_CALL_SITE = 5;

	public static final int RET_SITE = 6;

	public abstract String getLabel();

	public abstract boolean isRetSite();

	public abstract boolean isEntrySite();

	public abstract boolean isExitSite();

	public abstract boolean isStaticCallSite();

	public abstract boolean isVirtualCallSite();

	public abstract boolean isConstructorCallSite();

	public abstract boolean isCallSite();

	public abstract boolean isIntraStmtSite();

	public abstract int getType();

	public abstract String getTypeString();
	
	/*
	 * Returns the CFG the node is part of.
	 */
	public abstract CFG getCFG();
	
	public long getId();

}