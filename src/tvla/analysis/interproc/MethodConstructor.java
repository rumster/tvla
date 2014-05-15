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
public final class MethodConstructor extends Method{
	public static final String INIT = "<init>" ; // soot's name for constructors
	public MethodConstructor(Class belongsToClass, String methodName, String sig,
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
		assert(isConstructor);
		assert(methodName.equals(INIT)); //(belongsToClass.getName()));
		
	}
	
	public void dump(PrintStream out) {
		out.println("** Constructor **");
		super.dump(out);
	}
}
