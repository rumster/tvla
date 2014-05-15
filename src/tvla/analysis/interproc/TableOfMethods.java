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
 * Contains all the analyzed methods. 
 * Gives every methods a unique identifier, and maps every method name to its
 * own info
 * 
 * @author maon
 */
public class TableOfMethods {
	Map allMethods = null;
	Set allNames = null; 
	
	/**
	 * Constructs a table of classes without an initial size estimate.
	 */
	public TableOfMethods() {
		allMethods = HashMapFactory.make();
		allNames = allMethods.keySet();
	}

	/**
	 * Constructs a table of classes with an initial size estimate
	 * of the number of classes
	 */
	public TableOfMethods(int totalNumOfMethods) {
		allMethods = HashMapFactory.make((totalNumOfMethods* 5)  / 4);
		allNames = allMethods.keySet();
	}
	
	/**
	 * Adds a new entry to the class table.
	 * Throws an exception if the class already contains an entry 
	 * with the same name
	 * @param className Name of new class
	 */
	public void addMethod(String sig, Method mtd) {
		assert(!allMethods.containsKey(sig));

		allMethods.put(sig,mtd);
	}
	
	/**
	 * Returns the Class object from the table according to its name.
	 * Returns null if no such class exists.
	 * @param className Name of requested interface
	 */
	public Method getMethod(String sig) {
		assert(allMethods != null);		
		return  (Method) allMethods.get(sig);
	}

	
	public void dump(PrintStream out) {
		out.println();
		out.println("======================");
		out.println("= Methods Table Dump =");
		out.println("======================");

		out.println();
		Iterator mitr = allNames.iterator();
		while (mitr.hasNext()){
			String name = (String) mitr.next();
			out.println("METHOD NAME: " + name);
			out.print("METHOD INFO: ");
			
			Method mtd = (Method) allMethods.get(name);
			assert(mtd!=null);
			mtd.dump(out);
			out.println();
		}
}

		
	

}
