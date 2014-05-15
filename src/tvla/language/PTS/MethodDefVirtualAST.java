/*
 * File: MethodDefVirtualAST.java 
 * Created on: 23/09/2004
 */

package tvla.language.PTS;

import java.util.List;

/** AST for a definition of a virtual method
 * @author maon
 */
public class MethodDefVirtualAST extends MethodDefAST {

	/**
	 * @param methodName
	 * @param methodActions
	 */
	public MethodDefVirtualAST(String sig, List methodActions) {
		super(sig, methodActions);
	}

}
