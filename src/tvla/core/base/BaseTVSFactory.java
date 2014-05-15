package tvla.core.base;

import java.util.Iterator;

import tvla.core.HighLevelTVS;
import tvla.core.TVSFactory;
import tvla.core.TVSSet;
import tvla.predicates.DynamicVocabulary;
import tvla.util.Logger;

/** A factory for this package implementations.
 */
public class BaseTVSFactory extends TVSFactory {
	private long sumDenseBindings;
	private long sumSparseBindings;
	private long sumStructures;
	private long sumNormalizedBytes;

	/** Constructs and initializes a BaseTVSFactory object.
	 */
	public BaseTVSFactory() {
		super();
	}
	
	/** Returns a new empty structure.
	 */
	public HighLevelTVS makeEmptyTVS () {
		return new BaseHighLevelTVS();
	}
	
    /** Returns a new empty structure.
     */
    public HighLevelTVS makeEmptyTVS (DynamicVocabulary vocabulary) {
        return new BaseHighLevelTVS(vocabulary);
    }
	
	
	/** Returns an empty set.
	 */
	public TVSSet makeEmptySet () {
		TVSSet result = null;
		
		switch (joinMethod) {
		case JOIN_RELATIONAL: 
			result = new BaseHashTVSSet();
			break;
		case JOIN_INDEPENDENT_ATTRIBUTES: 
			result = new BaseSingleTVSSet();
			//result = new tvla.core.generic.GenericSingleTVSSet();
			break;
		}
		
		if (result == null)
			result = super.makeEmptySet();
		
		return result;
	}
	
	/** Override this method to collect space statistics.
	 * @author Roman Manevich.
	 * @since 2.1.2002 Initial creation.
	 * @todo update statistics for all predicate arities
	 */
	public void collectTVSSizeInfo(Iterator structureIter) {
		// The following code block should be modified to supply
		// correct metrics for the new bastTVS reoresentation
		// (which now supports predicaets of arbitrary arity).
		/*
		sumNormalizedBytes = 0;
		HashSet uniqueObjects = HashSetFactory.make();
		sumDenseBindings = 0;
		sumSparseBindings = 0;
		sumStructures = 0;
		
		while (structureIter.hasNext()) {
		BaseTVS base = (BaseTVS) structureIter.next();
		++sumStructures;
		{   // compute statistics for (the theoretical) dense representation
		// and sparse representation
		int n = base.nodes().size();
		int denseBindings = Vocabulary.allNullaryPredicates().size() +
		Vocabulary.allUnaryPredicates().size() * n +
		Vocabulary.allBinaryPredicates().size() * n * n;
		int denseBindingsWithCollections = denseBindings + Vocabulary.allPredicates().size();
		sumDenseBindings += denseBindings/4 + n*4;
		}
		
		int baseBindings = 0;
		for (Iterator iterpretations = base.predicates.values().iterator();
		iterpretations.hasNext(); ) {
		Object o = iterpretations.next();
		if (o instanceof ConcreteNullaryPredicate) {
		sumSparseBindings += 4;
		}
		else {if (o instanceof ConcreteUnaryPredicate) {
		ConcreteUnaryPredicate cup = (ConcreteUnaryPredicate) o;
		Map values = cup.values;
		sumSparseBindings += 4*values.size();
		ObjectWrapper wrapper = new ObjectWrapper(values);
		boolean changed = uniqueObjects.add(wrapper);
		if (changed)
		sumNormalizedBytes += (5 * values.size() + 5);
		else
		sumNormalizedBytes += 5;
		}
		else if (o instanceof ConcreteBinaryPredicate) {
		ConcreteBinaryPredicate cbp = (ConcreteBinaryPredicate) o;
		Map values = cbp.values;
		sumSparseBindings += 4*values.size();
		ObjectWrapper wrapper = new ObjectWrapper(values);
		boolean changed = uniqueObjects.add(wrapper);
		if (changed)
		sumNormalizedBytes += (9 * values.size() + 5);
		else
		sumNormalizedBytes += 5;
		}
		else
		throw new RuntimeException();
		}
		}
		sumNormalizedBytes += base.predicates.size() * 8;
		boolean changed = uniqueObjects.add(new ObjectWrapper(base.nodeSet));
		if (changed)
		sumNormalizedBytes += (5 + base.nodes().size()*4);
		else
		sumNormalizedBytes += 5;
		}
		uniqueObjects = null;
		System.gc();
		*/
	}
	
	/** Prints factory specific information.
	 */
	protected void dumpStatistics() {
		super.dumpStatistics();
		Logger.println("Number of access attempts to the join hash table : "
					   + BaseHashTVSSet.hashAccessAttempts);
		Logger.println("Number of collisions : " + BaseHashTVSSet.hashColisions);
		Logger.println("Number of redundant collisions : " + BaseHashTVSSet.redundantHashColisions);
		
		Logger.println("Normalized dense size in bytes    = " + sumDenseBindings);
		Logger.println("Normalized sparse size in bytes   = " + sumSparseBindings);
		Logger.println("Normalized base size in bytes     = " + sumNormalizedBytes);
		Logger.println("Total number of unique structures = " + sumStructures);
	}
}
