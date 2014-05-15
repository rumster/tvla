package tvla.core.generic;

import gnu.trove.*;
import java.util.*;
import tvla.core.*;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.Pair;
import tvla.util.ProgramProperties;

/** An implementation of TVSSet that does not bound
 * structures, using hashing and isomorphism checks to
 * make it more efficient.
 * 
 * @author Roman Manevich
 */
@SuppressWarnings("unchecked")
public class ConcreteTVSSet extends TVSSet {
	public static int merges;
	
	private static Map<Predicate, Integer> bounds = new HashMap<Predicate, Integer>();
	private static final boolean bmc;
	static {
		List<String> boundsStrs = ProgramProperties.getStringListProperty("tvla.bmc", Collections.EMPTY_LIST);
		bmc = !boundsStrs.isEmpty();
		for (String boundStr : boundsStrs) {
			int colonIndex = boundStr.indexOf(':');
			if (colonIndex < 0) {
				throw new Error("Syntax error in property tvla.bmc [" + boundStr + "]!");
			}
			else {
				String predStr = boundStr.substring(0, colonIndex);
				String boundInt = boundStr.substring(colonIndex + 1, boundStr.length());
				Predicate pred = Vocabulary.getPredicateByName(predStr);
				if (pred == null)
					throw new Error("Unable to find predicate " + pred + " specified in property tvla.bmc!");
				if (pred.arity() != 1)
					throw new Error("Predicate " + pred + " specified in property tvla.bmc is not unary!");
				int bound = Integer.parseInt(boundInt);
				bounds.put(pred, bound);
			}
		}
	}
	
	public static void reset() {
		bounds = new HashMap<Predicate, Integer>();
	}
	
	public THashSet<HighLevelTVS> structures;

	public ConcreteTVSSet() {
		structures = new THashSet<HighLevelTVS>(TVSHashFunc.generalTVSHashFunc);
	}
	
	public ConcreteTVSSet(Collection<HighLevelTVS> set) {
		structures = new THashSet<HighLevelTVS>(set, TVSHashFunc.generalTVSHashFunc);
    }

    public ConcreteTVSSet(Iterable<HighLevelTVS> istruct) {
		structures = new THashSet<HighLevelTVS>(TVSHashFunc.generalTVSHashFunc);
	    for (HighLevelTVS structure : istruct) {
	    	structures.add(structure);
	    }
	}

    public TVSSet copy() {
        return new ConcreteTVSSet(structures);
    }
    
	public Collection<HighLevelTVS> getStructures() {
		return structures;
	}
	
	/** Applies the Join confluence operator.
	 * @return The difference between the updated set
	 * and the old set or null if there is no difference.
	 */
	public HighLevelTVS mergeWith(HighLevelTVS structure) {
		if (!isInBounds(structure))
			return null;
		++merges;
		boolean change = structures.add(structure);
		if (change)
			return structure;
		else
			return null;
	}
	
	protected boolean isInBounds(HighLevelTVS structure) {
		for (Map.Entry<Predicate, Integer> entry : bounds.entrySet()) {
			int size = structure.numberSatisfy(entry.getKey());
			if (size > entry.getValue())
				return false;
		}
		return true;
	}
	

	public boolean mergeWith(HighLevelTVS structure, Collection<Pair<HighLevelTVS, HighLevelTVS>> mergureMap) {
		structures.add(structure);
		return true;
    }	
	
	/** The current number of states in this set.
	 */
	public int size() {
		return structures.size();
	}
	
	/** Return an iterator to the states this set 
	 * represents - TVS objects.
	 */
	public Iterator<HighLevelTVS> iterator() {
		return structures.iterator();
	}
	
	public String toString() {
	    return structures.toString();
	}
}