/*
 * File: Method.java 
 * Created on: 22/09/2004
 */

package tvla.analysis.interproc;

import java.io.PrintStream;


/** An analyzed method.
 * Note that the Type parameters passed to the constructor are stored in the 
 * newly created object.
 *  @author maon
 */
public final class MethodVirtual extends Method{
	private Type retType;
	
	public MethodVirtual(Class belongsToClass, String methodName, String sig,
				  Type retType,	
				  Type[] aformalArgsTypes, 
				  String[] aformalArgsNames,
				  String entryLabel, String exitLabel,
				  boolean isStatic,
				  boolean isConstructor) {
		super(belongsToClass,methodName,sig,
			  aformalArgsTypes, aformalArgsNames, 
			  entryLabel, exitLabel,
			  isStatic,isConstructor);
		assert(!isStatic);
		assert(!isConstructor);
		assert(retType != null);

		this.retType = retType; 
	}
	
	public void dump(PrintStream out) {
		out.println("** VIRTUAL METHOD **");
		super.dump(out);
		out.println(" RET TYPE: " + retType);
	}
}
