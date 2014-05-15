/*
 * Created on 22/09/2004
 *
 */
package tvla.analysis.interproc;

/**
 * An analyzed class in the analyzed program. 
 * @author maon
 */
public class Type {
	protected String name;
	/**
	 * 
	 */
	public Type(String name) {
		this.name = name;
	}
	
	/**
	 * Returns the class fully qualified name (including package)
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns just the class name (without the package)
	 * @return
	 */
	public String getClassName() {
		int lastDot = name.lastIndexOf('.');
		if (lastDot == -1)
			return name;
		
		assert(lastDot+1 < name.length());
//		String clsName = name.substring(lastDot+1);
		return name;
	}

	/**
	 * Returns the package (fully qulaified) name 
	 * @return
	 */
	public String getPackageName() {
		int lastDot = name.lastIndexOf('.');
		if (lastDot == -1)
			return "";
		
		assert(0 < lastDot);
//		String clsName = name.substring(0,lastDot);
		return name;
	}
	
	public int hashCode() {
		return name.hashCode();
	}
	
	public boolean equals(Object obj) {
		return obj == this;
	}
	
	public String toString() {
		return name;
	}
	
	public void dump(java.io.PrintStream out) {
		out.println("Name: " + name);
	}
}
