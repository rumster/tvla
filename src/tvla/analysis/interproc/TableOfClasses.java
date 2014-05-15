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
 * Contains all the analyzed classes. 
 * Gives every class a unique identifier, and maps every class to its
 * own info
 * 
 * @author maon
 */
public class TableOfClasses {
	Map classesTable = null;
	Set classesNames = null; 
	
	/**
	 * Constructs a table of classes without an initial size estimate.
	 */
	public TableOfClasses() {
		classesTable = HashMapFactory.make();
		classesNames = classesTable.keySet();
	}

	/**
	 * Constructs a table of classes with an initial size estimate
	 * of the number of classes
	 */
	public TableOfClasses(int numOfClasses) {
		classesTable = HashMapFactory.make((numOfClasses * 5)  / 4);
		classesNames = classesTable.keySet();
	}
	
	/**
	 * Adds a new entry to the class table.
	 * Throws an exception if the class already contains an entry 
	 * with the same name
	 * @param className Name of new class
	 */
	public void addClass(String className, Class cls) {
		assert(!classesTable.containsKey(className));

		classesTable.put(className,cls);
	}
	
	/**
	 * Returns the Class object from the table according to its name.
	 * Returns null if no such class exists.
	 * @param className Name of requested interface
	 */
	public Class getClass(String className) {
		assert(classesTable != null);		
		return  (Class) classesTable.get(className);
	}

	
	public void dump(PrintStream out) {
		out.println();
		out.println("====================");
		out.println("= Class Table Dump =");
		out.println("====================");

		out.println();
		Iterator citr = classesNames.iterator();
		while (citr.hasNext()){
			String name = (String) citr.next();
			out.println("CLASS NAME: " + name);
			out.println("CLASS INFO:");
			
			Class cls = (Class) classesTable.get(name);
			if (cls == null)
				throw new Error("Class table does not match classes names set");
			cls.dump(out);
		}
}

		
	

}
