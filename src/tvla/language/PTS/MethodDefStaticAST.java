/*
 * File: MethodDefStaticAST.java 
 * Created on: 23/09/2004
 */

package tvla.language.PTS;

import java.util.List;

/** AST for a definition of a static method
 * @author maon
 */
public class MethodDefStaticAST extends MethodDefAST {

	/**
	 * @param methodName
	 * @param methodActions
	 */
	public MethodDefStaticAST(String sig, List methodActions) {
		super(sig, methodActions);
	}

}
