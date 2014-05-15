/*
 * Created on 22/09/2004
 */
package tvla.analysis.interproc;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tvla.util.HashMapFactory;

/** Class: TableOfClasses 
 * Contains all the interfaces used in the analyzed program. 
 * Interfaces are needed because they contain static (constant) data. 
 * Gives every interface a unique identifier, and maps every interface to its
 * own info.
 * 
 * @author maon
 */
public class TableOfInterfaces {
	Map table = null;
	Set allNames = null;
	/**
	 * Constructs a table of classes without an initial size estimate.
	 */
	public TableOfInterfaces() {
		table = HashMapFactory.make();
		allNames = table.keySet();
	}

	/**
	 * Constructs a table of classes with an initial size estimate
	 * of the number of classes
	 */
	public TableOfInterfaces(int size) {
		table = HashMapFactory.make((size * 5)  / 4);
		allNames = table.keySet();
	}
	
	/**
	 * Adds a new entry to the class table.
	 * Requires that interfaceName is not already in the table.
	 * @param interfaceName Name of new interface
	 */
	public void addInterface(String interfaceName, Interface intrf) {
		assert(table != null);
		assert(!table.containsKey(interfaceName));
		
		table.put(interfaceName,intrf);
	}
	
	/**
	 * Returns the interface object from the table according to its name.
	 * Returns null if no such interface exists.
	 * @param interfaceName Name of requested interface
	 */
	public Interface getInterface(String interfaceName) {
		assert(table != null);		
		return  (Interface) table.get(interfaceName);
	}
	

	public void dump(PrintStream out) {
		assert(table != null);
		assert(allNames != null);

		out.println();
		out.println("========================");
		out.println("= Interface Table Dump =");
		out.println("========================");

    	out.println();
	    Iterator iitr = allNames.iterator();
    	while (iitr.hasNext()){
    		String name = (String) iitr.next();
    		out.println("INTERFACE NAME: " + name);
    		out.println("INTERFACE INFO:");
		
    		Interface intrf = (Interface) table.get(name);
    		assert (intrf != null);
				intrf.dump(out);
    	}
	}
}
