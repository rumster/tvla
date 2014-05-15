package tvla.predicates;

import java.util.Map;

import tvla.exceptions.SemanticErrorException;
import tvla.util.HashMapFactory;

/** A predicate used in the analysis. 
 * @author Tal Lev-Ami
 */
public class Predicate implements Comparable<Predicate> {
	/** The arity of the predicate.
	 */
	protected int arity;
	
	/** The name of the predicate.
	 */
	protected String name;
	
	/** Determines whether the predicate participates in Blur.
	 */
	protected boolean abstraction = true;

	/** For unary: It can be true for at most one node.
	 */
	protected boolean unique;
	
	/** For binary: It can be true for at most one right node for 
	 * each left node.
	 */	
	protected boolean function;
	
	/** For binary: It can be true for at most one left node for 
	 * each right node.
	 */	
	protected boolean invfunction;

	/** For binary: every node has to have a self-loop.
	 */	
	protected boolean reflexive;

	/** For binary: It is true for predicates whose paths form no cycles.
	 * @author Alexey Loginov.
	 */	
	protected boolean acyclic; 

	/** For unary: If non-null, this predicate can be true for at most one node
	    in a connected component of the predicate referenced by this field.
	 * @author Alexey Loginov.
	 */	
	protected Predicate uniquePerCCofPred; 

	////////////////////////////////////////
	// Graphical properties of predicates //
	////////////////////////////////////////
	protected boolean showTrue		= true;	 // Show the true values in graphical representation.
	protected boolean showUnknown	= true;  // Show the unknown values in graphical representation.
	protected boolean showFalse		= false; // Show the false values in graphical representation.
	protected boolean pointer		= false; // Draw as a pointer in the graphical representation.

	/** An integer unique to each predicate.
	 * This value is initialized by the Vocabulary class.
	 * @since 15.7.2001
	 */
	protected int id;
	
	/** This field may be used for various TVS representation-dependent
	 * optimizations to classify the predicate.
	 */
	public int category;

	/** This field may be used for various TVS representation-dependent
	 * optimizations.
	 */
	public int num;
	
	/**
	 * Used to speed up predicate hash computation.
	 * @author: igor
	 */
	static Map<Integer, String> globalHashCodes = HashMapFactory.make();
	static int HashMultiplier = (int)(System.currentTimeMillis() | 1);
	private int savedHashCode = -1;
	
	public Object cachedStructure = null;
	public Object cachedReference = null;
	
	public float rank = 0;
	
	public static void reset() {
		globalHashCodes = HashMapFactory.make();
	}
	
	/** Create a new predicate with the given name and arity.
	 */
	Predicate(String name, int arity) {
		this.name = name;
		this.arity = arity;

		savedHashCode = (this.name.hashCode() * 3 + this.arity) * HashMultiplier;
		if (globalHashCodes.containsKey(savedHashCode) &&
			!name.equals(globalHashCodes.get(savedHashCode))) {
			throw new RuntimeException("Predicate name hash collision detected!\nPlease run the program again.");
		}
		else {
			globalHashCodes.put(savedHashCode, name);
		}
	}

	/** Create a new predicate.
	 * @param name the name of the predicate
	 * @param arity the arity of the predicate (0..2).
	 * @param abstraction should the predicate participate in the blur. 
	 */
	Predicate(String name, int arity, boolean abstraction) {
		this(name, arity);
		this.abstraction = abstraction;
	}

	/** Create a new predicate.
	 * @param name the name of the predicate
	 * @param arity the arity of the predicate (0..2).
	 * @param abstraction should the predicate participate in the blur. 
	 * @param unique - true/false
	 * @param pointer - true/false
	 */
	Predicate(String name, 
			  int arity, 
			  boolean abstraction, 
			  boolean unique, 
			  boolean pointer) {
		this(name, arity, abstraction);
		this.unique = unique;
		this.pointer = pointer;
	}
	
	/** Create a new predicate.
	 * @param name the name of the predicate
	 * @param arity the arity of the predicate (0..2).
	 * @param abstraction should the predicate participate in the blur. 
	 * @param unique - true/false
	 * @param pointer - true/false
	 * @param visible - true/false
	 */
	Predicate(String name, int arity, boolean abstraction, boolean unique, boolean pointer, boolean visible) {
		this(name, arity, abstraction, unique, pointer);
		this.showFalse = false;
		this.showTrue = visible;
		this.showUnknown = visible;
	}
	
	/** Set the abstraction attribute.
	 */
	public void setAbstraction(boolean abstraction) {
		this.abstraction = abstraction;
	}

	/** Should the predicate participate in the blur.
	 */
	public boolean abstraction() {
		return abstraction;
	}

	/** Set the graphical representation attribute of the predicate.
	 * @param showTrue Show the true values in graphical representation.
	 * @param showUnknown Show the unknown values in graphical representation.
	 * @param showFalse Show the false values in graphical representation.
	 */
	public void setShowAttr(boolean showTrue, boolean showUnknown, boolean showFalse) {
		this.showTrue = showTrue;
		this.showUnknown = showUnknown;
		this.showFalse = showFalse;
	}

	/** Set the graphical representation attribute of the predicate.
	 * @param pointer Show as a pointer in the graphical representation.
	 */
	public void setShowAttr(boolean pointer) {
		this.pointer = pointer;
	}

	/** Show the true values in graphical representation?
	 */
	public boolean showTrue() {
		return showTrue;
	}

	/** Show the unknown values in graphical representation?
	 */
	public boolean showUnknown() {
		return showUnknown;
	}

	/** Show the false values in graphical representation?
	 */
	public boolean showFalse() {
		return showFalse;
	}
	
	/** Show as a pointer in the graphical representation?
	 */
	public boolean pointer() {
		return this.pointer;
	}
	
	/** Returns the name of the predicate.
	 */
	public String name() {
		return this.name;
	}

	/** Returns the arity of the predicate.
	 */
	public int arity() {
		return this.arity;
	}

	/** For Unary: Can the predicate be true for at most one node?
	 */
	public boolean unique() {
		if (arity != 1)
			return false;
			//throw new SemanticErrorException("Only a unary predicate can have the unique property. "
			//	+ name + " is " + arity + "-ry.");
		return unique;
	}

	/** For binary: Can the predicate be true for at most one right node for each left node.
	 */
	public boolean function() {
		if (arity != 2) 
			throw new SemanticErrorException("Only a binary predicate can have the function property. "
				+ name + " is " + arity + "-ry.");
		return function;
	}

	/** For binary: Can the predicate be true for at most one left node for each right node.
	 */
	public boolean invfunction() {
		if (arity != 2) 
			throw new SemanticErrorException("Only a binary predicate can have the inverse-function property. "
				+ name + " is " + arity + "-ry.");
		return invfunction;
	}

	/** For binary: Should every node have an edge to itself?
	 */
	public boolean reflexive() {
		if (arity != 2) 
			throw new SemanticErrorException("Only a binary predicate can have the reflexivity property. "
				+ name + " is " + arity + "-ry.");
		return reflexive;
	}

	/** For binary: Are the paths defined by the predicate acyclic?
	 * @author Alexey Loginov.
	 */
	public boolean acyclic() {
		if (arity != 2) 
			throw new RuntimeException("Only a binary predicate can have the acyclic property. "
				+ name + " is " + arity + "-ry.");
		return acyclic;
	}

	/** For Unary: Return the predicate for whose connected components this one is unique.
	 * @author Alexey Loginov.
	 */
	public Predicate uniquePerCCofPred() {
		if (arity != 1) 
			throw new SemanticErrorException("Only a unary predicate can have the uniquePerCC property. "
				+ name + " is " + arity + "-ry.");
		return uniquePerCCofPred;
	}

	/** For Unary: Set the unique property.
	 */
	public void unique(boolean unique) {
		if (arity != 1 && unique) 
			throw new SemanticErrorException("Only a unary predicate can have the unique property. "
				+ name + " is " + arity + "-ry.");
		this.unique = unique;
	}

	/** For binary: Can the predicate be true for at most one right node for each left node.
	 */
	public void function(boolean function) {
		if (arity != 2) 
			throw new SemanticErrorException("Only a binary predicate can have the function property. "
				+ name + " is " + arity + "-ry.");
		this.function = function;
	}
	
	/** For binary: Can the predicate be true for at most one left node for each right node.
	 */
	public void invfunction(boolean invfunction) {
		if (arity != 2) 
			throw new SemanticErrorException("Only a binary predicate can have the inverse-function property. "
				+ name + " is " + arity + "-ry.");
		this.invfunction = invfunction;
	}
	
	/** For binary: Should every node have an edge to itself?
	 */
	public void reflexive(boolean reflexive) {
		if (arity != 2) 
			throw new SemanticErrorException("Only a binary predicate can have the reflexivity property. "
				+ name + " is " + arity + "-ry.");
		this.reflexive = reflexive;
	}
	
	/** For binary: Are the paths defined by the predicate acyclic?
	 * @author Alexey Loginov.
	 */
	public void acyclic(boolean acyclic) {
		if (arity != 2) 
			throw new RuntimeException("Only a binary predicate can have the acyclic property. "
				+ name + " is " + arity + "-ry.");
		this.acyclic = acyclic;
	}

	/** For Unary: Set the uniquePerCC property.
	 * @author Alexey Loginov.
	 */
	public void uniquePerCCofPred(Predicate ccPredicate) {
		if (arity != 1) 
			throw new SemanticErrorException("Only a unary predicate can have the uniquePerCC property. "
				+ name + " is " + arity + "-ry.");
		this.uniquePerCCofPred = ccPredicate;
	}

	/** Returns the predicate's identifying integer.
	 */
	public final int id() {
		return id;
	}
	
	/** Return a human-readable representation of the predicate's name.
	 */
	public String toString() {
		return name;
	}

	/** Equate this predicate with the given predicate according to name and arity.
	 */
	public boolean equals(Object o) {
		if (!(o instanceof Predicate)) {
			return false;
		}

		Predicate other = (Predicate) o;
		//return (this.arity == other.arity) && other.name.equals(this.name);
		
		// We consider predicates equal if their hash codes, rather
		// than names, are equal. This is done for performance considerations.
		// To prevent an improbable case of hash collision for two different
		// names, we keep global dictionary of all the allocated names and
		// detect collisions on predicate creation.
		return (this.arity == other.arity) && (other.hashCode() == this.hashCode());
	}

	/** Compare the predicate with the given predicate according to lexicographical
	 * order of the name and arity.
	 * @author Tal Lev-Ami.
	 */
	public int compareTo(Predicate other) {
		if (this.arity == other.arity)
			return this.name.compareTo(other.name);
		else
			return this.arity - other.arity;
	}

	/** Returns the predicate's hash code.
	 * @author Tal Lev-Ami.
	 */
	
	public int hashCode() {
		return savedHashCode;
		//return this.name.hashCode() * 3 + this.arity;
	}
	
	
	/** Return a human-readable representation of the predicate's name.
	 */
	public String description() {
		return name + " arity = " + arity + 
			((this instanceof Instrumentation) ? " instrumentation" : " core") + 
		    (abstraction ? " abstraction" : "") +
			(unique ? " unique" : "") +
			(function ? " function" : "") +
			(invfunction? " invfunction ": "") +
			(reflexive? " reflexive": "") +
			(acyclic? " acyclic": "");
	}

}
