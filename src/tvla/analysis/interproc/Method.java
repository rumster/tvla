/*
 * File: Method.java 
 * Created on: 22/09/2004
 */

package tvla.analysis.interproc;

import java.io.PrintStream;


/** An analyzed method.
 * Note that the Type parameter passed to the constructor is stored in the 
 * newly created object.
 *  @author maon
 */
public class Method {
	Class belongsToClass = null;
	String methodName = null;
	String methodSig = null;
	int numOfParams = 0;
	Type[] aformalArgsTypes = null;
	String[] aformalArgsNames = null;
	final boolean isStatic;
	final boolean isConstructor;
	boolean isMain;
	String entryLabel, exitLabel;
	
	public Method(Class belongsToClass, String methodName, String sig,
				  Type[] aformalArgsTypes, 
				  String[] aformalArgsNames,
				  String entryLabel, String exitLabel,
				  boolean isStatic,
				  boolean isConstructor) {
		assert((aformalArgsTypes == null && aformalArgsNames == null) || 
			   (aformalArgsTypes.length == aformalArgsNames.length));
		
		this.belongsToClass = belongsToClass;
		this.methodName = methodName;
		this.methodSig = sig;
		this.entryLabel = entryLabel;
		this.exitLabel = exitLabel;
		this.isStatic = isStatic;
		this.isConstructor = isConstructor;
		
		numOfParams = (aformalArgsTypes == null) ? 0 : aformalArgsTypes.length; 
		if (aformalArgsTypes != null) {
			this.aformalArgsTypes = new Type[numOfParams];
			this.aformalArgsNames = new String[numOfParams];
			
			for (int i=0; i<numOfParams; i++) {
				this.aformalArgsTypes[i] = aformalArgsTypes[i];
				this.aformalArgsNames[i] = aformalArgsNames[i];				
			}
		}
		isMain = false;
	}
		
	
	////////////////////////////////////////////
	///              MUTATORS               ////
	////////////////////////////////////////////

	public void setMain() {
		assert(isStatic);
		isMain = true;
	}
	
	////////////////////////////////////////////
	///             ACCESSORS               ////
	////////////////////////////////////////////

	public String getSig() {
		return methodSig;
	}

	public String getName() {
		return methodName;
	}
	
	public String getEntryLabel() {
		return entryLabel;
	}
	
	public String getExitLabel() {
		return exitLabel;
	}

	public Class belongsTo() {
		return belongsToClass;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public boolean isVirtual() {
		return !(isConstructor || isStatic);
	}

	public boolean isConstructor() {
		return isConstructor;
	}
	
	public boolean isMain() {
		return isMain;
	}

	public int hashCode() {
		return methodSig.hashCode();
	}

	public void dump(PrintStream out) {
		out.println(" CLASS: <" + belongsToClass.getName() + ">" +
					" METHOD: <" + methodName + ">" + 
					" SIGNATURE: <" + methodSig + ">");
		assert(numOfParams > 0 ^ (aformalArgsTypes == null && aformalArgsNames == null));    
			  
		for (int i = 0; i < numOfParams; i++) {
			out.println(" PARAM ["+i+"]: TYPE: " + aformalArgsTypes[i].getName() + 
									   " NAME: " + aformalArgsNames[i]);
			
		}		
	}
}
