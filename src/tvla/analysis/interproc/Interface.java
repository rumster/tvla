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
public final class Interface {
	private String name;
	/**
	 * 
	 */
	public Interface(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int hashCode() {
		return name.hashCode();
	}
	
	public boolean equals(Object obj) {
		return obj == this;
	}
	
	public void dump(java.io.PrintStream out) {
		out.println("Name: " + name);
	}

}
