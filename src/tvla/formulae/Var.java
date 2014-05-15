package tvla.formulae;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tvla.util.HashMapFactory;

/** A variable to be used in formulae.
 * @author Tal Lev-Ami
 */
public final class Var implements Comparable {
	private String name;
	private int id;
	
	static int maxId = 0;
	static Map ids = HashMapFactory.make();
	public static Var tr = new Var("tr");
	public static int maxId() {
		return maxId;
	}

	/** Create a variable with the given name. */
	public Var(String name) {
		this.name = name;
		Integer id = (Integer) ids.get(name);
		if (id == null) {
			id = new Integer(maxId++);
			ids.put(name, id);
		}
		this.id = id.intValue();
	}
	
	/** Return the unique id of this variable */
	public int id() {
		return id;
	}

	/** Return the name of this variable */
	public String name() {
		return name;
	}

	/** Equate this variable with the given variable by name. */
	public boolean equals(Object other) {
		if (!(other instanceof Var)) {
			return false;
		}
		return this.id == ((Var) other).id;
	}

	public final int hashCode() {
		return id;
	}
	
	public final int compareTo(Object other) {
		return this.id - ((Var)other).id;
	}

	/** Return a human readable representation of the variable */
	public String toString() {
		return name;
	}

	private static int lastAllocatedID = 0;

	/** Allocate a never before used variable */
	public static Var allocateVar() {
		return new Var("___v" + lastAllocatedID++);
	}
	
	/** Allocate a never before used variable 
	 * @author Greta Yorsh
	 */
	public static Var allocateVarPrefix(String prefix) {
		return new Var(prefix + (lastAllocatedID++) + "_");
	}
	/** Creates List of variables, named prefix+i, where i=1..n
	 * @author Greta Yorsh
	 */
	public static List CreateVars(String prefix, int n) {
		List vars = new ArrayList();		
		for (int i = 0; i < n; i++){
			vars.add(Var.allocateVarPrefix(prefix));
		}						
		return vars;
	}
}
