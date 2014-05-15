package tvla.predicates;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import tvla.exceptions.SemanticErrorException;
import tvla.exceptions.TVLAException;
import tvla.formulae.Formula;
import tvla.formulae.Var;
import tvla.transitionSystem.Location;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;

/** This class represents the predicates which are available for all structures.
 * @author Roman Manevich.
 * @since 29.5.2001 Initial creation.
 */
public class Vocabulary {
	private static SortedSet<Predicate> allPredicates			= new TreeSet<Predicate>();
	private static SortedSet<Predicate> nullaryPredicates		= new TreeSet<Predicate>();
	private static SortedSet<Predicate> nullaryRelPredicates	= new TreeSet<Predicate>();
	private static SortedSet<Predicate> nullaryNonRelPredicates	= new TreeSet<Predicate>();
	private static SortedSet<Predicate> unaryPredicates			= new TreeSet<Predicate>();
	private static SortedSet<Predicate> unaryRelPredicates		= new TreeSet<Predicate>();
	private static SortedSet<Predicate> unaryNonRelPredicates	= new TreeSet<Predicate>();
	private static SortedSet<Predicate> binaryPredicates		= new TreeSet<Predicate>();
	private static SortedSet<Predicate> positiveArityPredicates	= new TreeSet<Predicate>();
	private static SortedSet<Instrumentation> instrumentationPredicates  = new TreeSet<Instrumentation>();
	/**
	 * a separate set for predicates of arity 3 and above
	 */
	private static SortedSet<Predicate> karyPredicates			= new TreeSet<Predicate>();	
	/** A list of location predicates "at[l]".
	 * @author Eran Yahav.
	 */
	public static List<LocationPredicate> locationPredicates = new ArrayList<LocationPredicate>();
	
    /** The "is summary" predicate.
     * @author Tal Lev-Ami.
     */
    public static final Predicate sm = createPredicate("sm", 1, false);
    
	/** The "is active" predicate.
	 * @author Tal Lev-Ami.
	 */
    public static final Predicate active = createPredicate("ac", 1, false);
	
	/** The "instance" predicate.
	 * @author Tal Lev-Ami.
	 */
	public static final Predicate instance = createPredicate("instance", 2, false);

	/** The "is new" predicate.
	 * @author Tal Lev-Ami.
	 */
	public static final Predicate isNew = createPredicate("isNew", 1, false);

	/** The "ready" predicate.
	 * @author Eran Yahav.
	 */
	public static final Predicate ready = createPredicate("ready", 1, true);
	
	/** The "is thread" predicate.
	 * @author Eran Yahav.
	 */
	public static final Predicate isThread = createPredicate("isthread", 1, true);
	
	/** The "runnable" predicate.
	 * @author Eran Yahav.
	 */
	public static final Predicate runnable = createPredicate("runnable", 1, false, true, false, false);

    /**
     * Indicates which nodes are outside the current decomposition
     */
    public static final Predicate outside = createPredicate("outside", 1);    
    
	/** A map from predicate names to their corresponding objects.
	 */
	private static Map<String, Predicate> nameToPredicate;

	public static void reset() {
		allPredicates.clear();
		nullaryPredicates.clear();
		nullaryRelPredicates.clear();
		nullaryNonRelPredicates.clear();
		unaryPredicates.clear();
		unaryRelPredicates.clear();
		unaryNonRelPredicates.clear();
		binaryPredicates.clear();
		positiveArityPredicates.clear();
		instrumentationPredicates.clear();
		
		nameToPredicate.clear();
		addPredicate(sm);
		addPredicate(active);
		addPredicate(instance);
		addPredicate(isNew);
		addPredicate(ready);
		addPredicate(isThread);
		addPredicate(runnable);
		addPredicate(outside);
	}
	
	/** Creates a new predicate and adds it to the existing vocabulary.
	 */
	public static Predicate createPredicate(String name, int arity) {
		Predicate predicate = new Predicate(name, arity);
		addPredicate(predicate);
		return predicate;
	}
	
	/** Creates a new predicate and adds it to the existing vocabulary.
	 */
	public static Predicate createPredicate(String name, 
											int arity, 
											boolean abstraction) {
	    if (arity > 1 && abstraction) {
	        throw new TVLAException("Attempt to create an abstraction predicate " +
	                name + " of arity higher than 1 (" + arity + ")!");
	    }
	    
		Predicate predicate = new Predicate(name, arity, abstraction);
		addPredicate(predicate);
		return predicate;
	}

	/** Creates a new predicate and adds it to the existing vocabulary.
	 */
	public static Predicate createPredicate(String name,
											int arity, 
											boolean abstraction,
											boolean unique,
											boolean pointer) {
	    if (arity > 1 && abstraction) {
	        throw new TVLAException("Attempt to create an abstraction predicate " +
	                name + " of arity higher than 1 (" + arity + ")!");
	    }
	    if (arity != 1 && unique) {
	        throw new TVLAException("Attempt to create a predicate " +
	                name + " of arity different than 1 (" + arity + ") with " +
	                "the unique functional dependency!");
	    }

	    Predicate predicate = new Predicate(name, arity, abstraction, unique, pointer);
		addPredicate(predicate);
		return predicate;
	}

	/** Creates a new predicate and adds it to the existing vocabulary.
	 */
	public static Predicate createPredicate(String name, 
											int arity, 
											boolean abstraction, 
											boolean unique, 
											boolean pointer, 
											boolean visible) {
	    if (arity > 1 && abstraction) {
	        throw new TVLAException("Attempt to create an abstraction predicate " +
	                name + " of arity higher than 1 (" + arity + ")!");
	    }
	    if (arity != 1 && abstraction) {
	        throw new TVLAException("Attempt to create a predicate " +
	                name + " of arity different than 1 (" + arity + ") with " +
	                "the unique functional dependency!");
	    }

	    Predicate predicate = new Predicate(name, arity, abstraction, unique, pointer, visible);
		addPredicate(predicate);
		return predicate;
	}
	
	/** Creates a new binary predicate and adds it to the existing vocabulary.
	 * In case the predicate exists its properties are updated.
	 */
	public static Predicate createBinaryPredicate(String name, 
												  boolean func, 
												  boolean invfunc, 
												  boolean acyc, 
												  boolean visible) {
		if (nameToPredicate == null)
			initNameMap();
		Predicate predicate = nameToPredicate.get(name);
		if (predicate == null) {
			predicate = new Predicate(name, 2, false);
			addPredicate(predicate);
		}
		
		predicate.function(func);
		predicate.invfunction(invfunc);
		predicate.acyclic(acyc);
		predicate.showTrue = visible;
		predicate.showUnknown = visible;
	
		return predicate;
	}

	/** Creates a new instrumentation predicate and adds it to the 
	 * existing vocabulary.
	 */
	public static Instrumentation createInstrumentationPredicate(String name, 
																 int arity,
																 boolean abstraction,
																 Formula formula,
																 List<Var> vars) {
	    if (arity > 1 && abstraction) {
            throw new TVLAException(
                    "Attempt to create an abstraction predicate " + name
                            + " of arity higher than 1 (" + arity + ")!");
        }
        assert vars != null : "Attempt to create an instrumentation predicate "
                + "with a null argument list!";
        assert formula != null : "Attempt to create an instrumentation predicate "
                + "with a null defining formula!";
        assert vars.size() == arity : "Attempt to create an instrumentation predicate "
                + "with an argument list of size "
                + vars.size()
                + " and different arity: " + arity + "!";
        if (!vars.containsAll(formula.freeVars()))
        	throw new SemanticErrorException(
        			"The defining formula " + formula + " uses the free variables " +
        			formula.freeVars() + " some of which are not arguments of the " +
        			"instrumentation predicate: " + vars +"!");
        assert HashSetFactory.make(vars).size() == arity : "Argument variables of instrumentation predicates "
                + "should form a set!";

        
        // We need the if in case the program runs with Assertions disabled and the assert check
        // is ignored
        if (!((vars != null) &&
            (formula != null) &&
            (vars.size() == arity) &&
            (vars.containsAll(formula.freeVars())) &&
            (HashSetFactory.make(vars).size() == arity)))
          return null;
        
	    Instrumentation predicate = new Instrumentation(name, arity, abstraction, formula, vars);
		addPredicate(predicate);
		return predicate;
	}

	/** Creates a new location predicate and adds it to the 
	 * existing vocabulary.
	 * In case the predicate exists its properties are updated.
	 */
	public static LocationPredicate createLocationPredicate(String name,
															Location location) {
	    assert location != null;
		LocationPredicate predicate = new LocationPredicate(name, location);
		addPredicate(predicate);
		locationPredicates.add(predicate);
		return predicate;
	}

	/** Retrieves a predicate using its name.
	 * @return The predicate object or null if no predicate with such name exists.
	 */
	public static Predicate getPredicateByName(String predicateName) {
		if (predicateName.equals("inac")) // for backward compatibility with TVLA 0.91
			predicateName = "ac";
		Predicate p = nameToPredicate.get(predicateName);
		return p;
	}
	
	/** Find a location predicate by its label.
	 * @author Eran Yahav.
	 */
	public static LocationPredicate findLocationPredicate(String locationLebl) {
		for (LocationPredicate currLoc : locationPredicates) {
			if (currLoc.getLocation().label().equals(locationLebl)) {
				return currLoc;
			}
		}
		throw new RuntimeException("label " + locationLebl + " not found!");
	}

	/** Returns the number of predicates in the vocabulary.
	 */
	public static int size() {
		return allPredicates.size();
	}
	
    /** Returns the collection of all predicates.
     */
	
	public static SortedSet<Predicate> allPredicates() {
		if (allPredicates == null)
			allPredicates = new TreeSet<Predicate>();
		return allPredicates;
	}
	
	/** Returns the collection of all the nullary predicates.
	 */
	public static SortedSet<Predicate> allNullaryPredicates() {
		if (nullaryPredicates == null)
			nullaryPredicates = new TreeSet<Predicate>();
		return nullaryPredicates;
	}
	
	/** Returns the collection of all the nullary relational predicates.
	 */
	public static SortedSet<Predicate> allNullaryRelPredicates() {
		if (nullaryRelPredicates == null)
			nullaryRelPredicates = new TreeSet<Predicate>();
		return nullaryRelPredicates;
	}

	/** Returns the collection of all the nullary non-relational predicates.
	 */
	public static SortedSet<Predicate> allNullaryNonRelPredicates() {
		if (nullaryNonRelPredicates == null)
			nullaryNonRelPredicates = new TreeSet<Predicate>();
		return nullaryNonRelPredicates;
	}

	/** Returns the collection of all the unary predicates.
	 */
	public static SortedSet<Predicate> allUnaryPredicates() {
		if (unaryPredicates == null)
			unaryPredicates = new TreeSet<Predicate>();
		return unaryPredicates;
	}

	/** Returns the collection of all the unary relational predicates.
	 */
	public static SortedSet<Predicate> allUnaryRelPredicates() {
		if (unaryRelPredicates == null)
			unaryRelPredicates = new TreeSet<Predicate>();
		return unaryRelPredicates;
	}

	/** Returns the collection of all the unary non-relational predicates.
	 */
	public static SortedSet<Predicate> allUnaryNonRelPredicates() {
		if (unaryNonRelPredicates == null)
			unaryNonRelPredicates = new TreeSet<Predicate>();
		return unaryNonRelPredicates;
	}

	/** Returns the collection of all the binary predicates.
	 */
	public static SortedSet<Predicate> allBinaryPredicates() {
		if (binaryPredicates == null)
			binaryPredicates = new TreeSet<Predicate>();
		return binaryPredicates;
	}
	
	/** Returns the collection of all the predicates with arity > 2.
	 */
	public static SortedSet<Predicate> allKaryPredicates() {
		if (karyPredicates == null)
			karyPredicates = new TreeSet<Predicate>();
		return karyPredicates;
	}

	/** Returns the collection of all the predicates with 
	 * positive arity (all non-nullary predicates).
	 */
	public static SortedSet<Predicate> allPositiveArityPredicates() {
		if (positiveArityPredicates == null)
			positiveArityPredicates = new TreeSet<Predicate>();
		return positiveArityPredicates;
	}

	/** Returns the set of all instrumentation predicates.
	 */
	public static SortedSet<Instrumentation> allInstrumentationPredicates() {
		if (instrumentationPredicates == null)
			instrumentationPredicates = new TreeSet<Instrumentation>();
		return instrumentationPredicates;
	}
	
	/** Returns a human-readable representation of the vocabulary.
	 * @since tvla-2-alpha (May 12 2002)
	 */
	public static String dump() {
		StringBuffer result = new StringBuffer(128);		
		result.append("Vocabulary={");
		for (Iterator<Predicate> i = allPredicates().iterator(); i.hasNext(); ) {
			Predicate p = (Predicate) i.next();
			result.append(p);
			if (i.hasNext())
				result.append(", ");
		}
		result.append("}");
		return result.toString();
	}
	
	/** Removes the predicate from the existing vocabulary
	 * and also from the category sets to which it belongs.
	 */
	public static final void removePredicate(Predicate predicate) {
		switch (predicate.arity()) {
		case 0: nullaryPredicates.remove(predicate);
				if (predicate.abstraction())
					nullaryRelPredicates.remove(predicate);
				else
					nullaryNonRelPredicates.remove(predicate);
				break;
		case 1: unaryPredicates.remove(predicate);
				if (predicate.abstraction())
					unaryRelPredicates.remove(predicate);
				else
					unaryNonRelPredicates.remove(predicate);
				break;
		case 2: binaryPredicates.remove(predicate);
				break;
		default:
				karyPredicates.remove(predicate);
				break;
		}
		
		if (predicate instanceof Instrumentation)
			instrumentationPredicates.remove(predicate);
		if (predicate.arity() > 0)
			positiveArityPredicates.remove(predicate);		
		allPredicates.remove(predicate);
		nameToPredicate.remove(predicate.name());
	}

	/** Sets the abstraction property of the predicate.
	 *  Reflects the change in the appropriate category sets.
	 */
	public static final void setAbstractionProperty(Predicate predicate, boolean abs) {
		if (predicate.abstraction() ? abs : !abs)
			return;  // The new value is the same as the old one.

		if (abs) {
			if (predicate.arity() == 0) {
				nullaryNonRelPredicates.remove(predicate);
				nullaryRelPredicates.add(predicate);
			} else if (predicate.arity() == 1) {
				unaryNonRelPredicates.remove(predicate);
				unaryRelPredicates.add(predicate);
			}
		} else {
			if (predicate.arity() == 0) {
				nullaryRelPredicates.remove(predicate);
				nullaryNonRelPredicates.add(predicate);
			} else if (predicate.arity() == 1) {
				unaryRelPredicates.remove(predicate);
				unaryNonRelPredicates.add(predicate);
			}
		}
		predicate.setAbstraction(abs);
	}

	/** Adds the predicate to the existing vocabulary
	 * and also to the category sets to which it belongs.
	 */
	private static final void addPredicate(Predicate predicate) {
		if (nameToPredicate == null)
			initNameMap();
		// TODO: add a more thorough check of the predicate's name.
		assert !predicate.name().equals("") : "Attempting to create a predicate with an empty name!";
	    if (nameToPredicate.containsKey(predicate.name())) {
	        throw new SemanticErrorException("Attempting to recreate the predicate " + predicate.name());	        
	    }
        if (predicate.arity() < 0) {
            throw new TVLAException("Attempt to create a predicate ("
                + predicate.name() + ") with negative arity: "
                + predicate.arity + "!");
        }
	    nameToPredicate.put(predicate.name(), predicate);
	    
		switch (predicate.arity()) {
		case 0: nullaryPredicates.add(predicate);
				if (predicate.abstraction())
					nullaryRelPredicates.add(predicate);
				else
					nullaryNonRelPredicates.add(predicate);
				break;
		case 1: unaryPredicates.add(predicate);
				if (predicate.abstraction())
					unaryRelPredicates.add(predicate);
				else
					unaryNonRelPredicates.add(predicate);
				break;
		case 2: binaryPredicates.add(predicate);
				break;
		default:
				karyPredicates.add(predicate);
				break;
		}
		
		if (predicate instanceof Instrumentation)
			instrumentationPredicates.add((Instrumentation) predicate);
		if (predicate.arity() > 0)
			positiveArityPredicates.add(predicate);		
		allPredicates.add(predicate);
		predicate.id = allPredicates.size();
	}
	
	/** Singleton pattern.
	 */
	private Vocabulary() {
	}

	private static final void initNameMap() {
		nameToPredicate = HashMapFactory.make();
		for (Predicate predicate : allPredicates) {
			nameToPredicate.put(predicate.name(), predicate);
		}
	}
}
/*
class QuickTreeSet extends TreeSet {
	private LinkedList al; 
	
	QuickTreeSet() {
		super();
		al = new LinkedList();
	}
	
	private void modify() {
		al = new LinkedList((Collection)this);
	}
	
	public Iterator iterator() {
		return al.iterator();
	}
	
	public boolean add(Object o) {
		boolean r = super.add(o);
		modify();
		return r;
	}
	public boolean addAll(Collection o) {
		boolean r = super.addAll(o);
		modify();
		return r;
	}
	public boolean remove(Object o) {
		boolean r = super.remove(o);
		modify();
		return r;
	}
	public boolean removeAll(Collection o) {
		boolean r = super.removeAll(o);
		modify();
		return r;
	}
};
*/