/*
 * File: MethodDefStaticAST.java 
 * Created on: 23/09/2004
 */

package tvla.language.PTS;

/** AST for a definition of a static method
 * @author maon
 */
public class SymbExtendsInterfaceAST extends  SymbExtendsAST {	
	public SymbExtendsInterfaceAST (String className,
						        String superClassName){
		super(className,superClassName);
	}
}
