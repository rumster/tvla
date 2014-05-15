package tvla.core.generic;

import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.decompose.DecomposeLocation;
import tvla.core.Canonic;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.StoresCanonicMaps;
import tvla.core.TVS;
import tvla.core.TVSSet;
import tvla.core.base.BaseBlur;
import tvla.core.base.PredicateEvaluator;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.transitionSystem.AnalysisGraph;
import tvla.transitionSystem.Location;
import tvla.util.HashMapFactory;
import tvla.util.Pair;
import tvla.util.Timer;

public final class GenericHashPartialJoinTVSSet extends GenericPartialJoinTVSSet {
	/** Maps sets of canonic names to structures.
	 */
	protected Map<Set<Canonic>,HighLevelTVS> universeToStructure = new HashMap<Set<Canonic>,HighLevelTVS>();

	protected static int foundInCache = 0;

	protected static int totalQueries = 0;

	protected static int nonIsomorphic = 0;

	/** Applies the Join confluence operator.
	 * @return The difference between the updated set
	 * and the old set or null if there is no difference.
	 */
	
	public static int countNewStructures = 0;
	public static int countMergedStructures = 0;

    public TVSSet copy() {
        GenericHashPartialJoinTVSSet copy = new GenericHashPartialJoinTVSSet();
        copy.cachingMode = this.cachingMode;
        for (Map.Entry<Set<Canonic>, HighLevelTVS> entry : universeToStructure.entrySet()) {
            HighLevelTVS structureCopy = entry.getValue();
            copy.universeToStructure.put(entry.getKey(), structureCopy);
            copy.structures.add(structureCopy);
        }
        return copy;
    }

    public boolean contains(HighLevelTVS structure) {
        AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.BLUR_TIME);
        structure.blur();
        AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.BLUR_TIME);

        cleanup();

        // ADDED
        Set<Canonic> canonicNames = getCanonicSetForBlurred(structure);
        HighLevelTVS singleStructure = universeToStructure.get(canonicNames);
        if (singleStructure == null) {
            return false;
        } else {
            return isomorphic(singleStructure,structure);
        }
    }
    
	public HighLevelTVS mergeWith(HighLevelTVS newStructure) {
	    Timer timer = Timer.getTimer("Low", "Join");
	    try {
	        timer.start();
    		AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.BLUR_TIME);
    		newStructure.blur();
    		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.BLUR_TIME);
    
    		cleanup();
    
    		// ADDED
    		Set<Canonic> canonicNames = getCanonicSetForBlurred(newStructure);
    		
            HighLevelTVS singleStructure = universeToStructure.get(canonicNames);
    		if (singleStructure == null) {
    			countNewStructures++;
    			addStructure(newStructure, canonicNames);
    			return (HighLevelTVS) newStructure;
    		}
    		else {
    			countMergedStructures++;
    			// Remove the old structure before merging to avoid breaking
    			// the invariants of 'structures' and 'universeToStructure'.
    			structures.remove(singleStructure);
    			universeToStructure.remove(canonicNames);
    
    			HighLevelTVS singleStructureOrig = singleStructure;
    			singleStructure = singleStructure.copy(); // Instead of copying in copy
    			
    			// Callng the following method causes the the canonic maps of both structure
    			// to be constructed, which has to be done beore calling 'mergeStructures'.
    			boolean conditionHolds = mergeCondition(singleStructure, newStructure);
    			assert conditionHolds;
    			boolean change = mergeStructures(singleStructure, newStructure);
    			if (change) {
    				AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.BLUR_TIME);
    				BaseBlur.getInstance().rebuildCanonicMaps(singleStructure);
    				AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.BLUR_TIME);
    			} else {
    				singleStructure = singleStructureOrig;
    			}
    			
    			addStructure(singleStructure, canonicNames);
    			return change ? singleStructure : null;
    		}
        } finally {
            timer.stop();
        }
	}

	protected void addStructure(HighLevelTVS structure, Set<Canonic> canonicNames) {
		if (cachingMode) {
			structure = addStructureToCache(structure, canonicNames);
		}
		
		structures.add(structure);
		universeToStructure.put(canonicNames, structure);
	}
		
	protected static Map<Long,Collection<WeakReference<HighLevelTVS>>> globalCache = HashMapFactory.make();
	
	protected static HighLevelTVS addStructureToCache(HighLevelTVS structure,
			Set<Canonic> canonicNames) {
		totalQueries++;
		
		// Calculate Signature
		long signature = calcSignature(structure, canonicNames);

		Collection<WeakReference<HighLevelTVS>> bucket = globalCache.get(signature);
		if (bucket == null) {
			// No bucket - create bucket and add structure to it.
			bucket = new LinkedList<WeakReference<HighLevelTVS>>();
			globalCache.put(signature, bucket);
			bucket.add(new WeakReference<HighLevelTVS>(structure));
		} else {
			boolean found = false; 
			for (Iterator<WeakReference<HighLevelTVS>> iterator = bucket.iterator(); iterator.hasNext();) {
				WeakReference<HighLevelTVS> cacheEntry = iterator.next();
				HighLevelTVS cacheStructure = cacheEntry.get();
				if (cacheStructure == null) {
					iterator.remove();
					continue;
				}
				// Found isomorphic structure!
				if (isomorphic(structure,cacheStructure)) {
					structure = cacheStructure;
					found = true;
					break;
				} else {
					nonIsomorphic++;
				}
			}
			if (found) {
				foundInCache++;
			} else {
				// Not found - add it bucket (i.e. to the cache)
				bucket.add(new WeakReference<HighLevelTVS>(structure));
			}
		}
		return structure;
	}

	protected static long calcSignature(HighLevelTVS structure, Set<Canonic> canonicNames) {
		TreeSet<Long> nodeSignatures = new TreeSet<Long>();		
		for (Canonic canonic : canonicNames) {
			nodeSignatures.add(canonic.signature());
		}
		long signature = 0;
		for (long nodeSignature : nodeSignatures) {
			signature = (signature * 31) + nodeSignature;
		}
		for (Predicate predicate : structure.getVocabulary().nullaryNonRel()) {
			signature = (signature * 31) + structure.eval(predicate).kleene();			
		}
		for (Predicate predicate : structure.getVocabulary().unaryNonRel()) {
			signature = (signature * 31) + structure.numberSatisfy(predicate);			
		}
		for (Predicate predicate : structure.getVocabulary().binary()) {
			signature = (signature * 31) + structure.numberSatisfy(predicate);			
		}
		return signature;
	}

	public boolean mergeWith(HighLevelTVS newStructure, Collection<Pair<HighLevelTVS,HighLevelTVS>> mergedWith) {
		HighLevelTVS result = mergeWith(newStructure);
		if (result == null) {
			return false;
		} else if (result == newStructure) {
			return true; // First time
		} else {
			// record merge
			mergedWith.add(new Pair<HighLevelTVS,HighLevelTVS>(newStructure, result));
			return true;
		}
	}

	protected Set<Canonic> makeCanonicMapForBlurred_Old(TVS structure) {
		Collection<Node> nodes = structure.nodes();
		Set<Canonic> canonicNames = new HashSet<Canonic>(nodes.size());
		
		Canonic nullaryCanonic = new Canonic(structure.getVocabulary().nullaryRel().size());
		for (Predicate predicate : structure.getVocabulary().nullaryRel()) {
			Kleene value = structure.eval(predicate);
			nullaryCanonic.add(value);
		}
		canonicNames.add(nullaryCanonic);
		
		for (Node node : nodes) {
	        Canonic canonic = new Canonic(structure.getVocabulary().unaryRel().size());
	        for (Predicate predicate : structure.getVocabulary().unaryRel()) {
    			Kleene value = structure.eval(predicate, node);
    			canonic.add(value);
    		}
    		canonicNames.add(canonic);
		}
		return canonicNames;
	}
	
	protected static Set<Canonic> getCanonicSetForBlurred(TVS structure) {
		if (!(structure instanceof StoresCanonicMaps)) {
			return makeCanonicSet(structure);
		}
		
		Map<Canonic,Node> invCanonicMap = ((StoresCanonicMaps)structure).getInvCanonic();
		if (invCanonicMap == null) {
			return makeCanonicSet(structure);
		}
		
		Set<Canonic> canonicNames = new HashSet<Canonic>(structure.nodes().size());
		
        Canonic nullaryCanonic = new Canonic(structure.getVocabulary().nullaryRel().size());
        for (Predicate predicate : structure.getVocabulary().nullaryRel()) {
			Kleene value = structure.eval(predicate);
			nullaryCanonic.add(value);
		}
		canonicNames.add(nullaryCanonic);
		canonicNames.addAll(invCanonicMap.keySet());
		return canonicNames;
	}
	
	protected static Set<Canonic> makeCanonicSet(TVS structure) {		
        Canonic nullaryCanonic = new Canonic(structure.getVocabulary().nullaryRel().size());
        for (Predicate predicate : structure.getVocabulary().nullaryRel()) {
			Kleene value = structure.eval(predicate);
			nullaryCanonic.add(value);
		}
        Set<Canonic> canonicNames = GenericBlur.defaultGenericBlur.makeCanonicSet(structure);
		canonicNames.add(nullaryCanonic);
		return canonicNames;
	}

    @Override
    public Boolean isomorphic(TVSSet o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GenericHashPartialJoinTVSSet)) {
            return super.isomorphic(o);
        }
        GenericHashPartialJoinTVSSet other = (GenericHashPartialJoinTVSSet) o;
        if (!universeToStructure.keySet().equals(other.universeToStructure.keySet())) {
            return false;
        }
        
        for (Set<Canonic> signature : universeToStructure.keySet()) {
            HighLevelTVS thisTVS = universeToStructure.get(signature);
            HighLevelTVS otherTVS = other.universeToStructure.get(signature);
            assert otherTVS != null; // Checked equality between keysets
            
            if (thisTVS.getVocabulary() != otherTVS.getVocabulary()) {
                return null;
            }
            
            if (!(thisTVS instanceof StoresCanonicMaps)) {
                return null;
            }
            
            if (!(otherTVS instanceof StoresCanonicMaps)) {
                return null;
            }
            return isomorphic(thisTVS, otherTVS);
        }
        return true;
    }

	protected static Boolean isomorphic(HighLevelTVS thisTVS, HighLevelTVS otherTVS) {
		if (thisTVS == otherTVS) {
			return true;
		}		
		if (thisTVS.getVocabulary() != otherTVS.getVocabulary()) {
			return false;
		}
		
		Map<Node, Canonic> thisMap = ((StoresCanonicMaps) thisTVS).getCanonic();
		Map<Canonic, Node> otherInvMap = ((StoresCanonicMaps) otherTVS).getInvCanonic();
		if (thisMap == null || otherInvMap == null) {
		    return null;
		}
		// Check non relational nullary
		for (Predicate predicate : thisTVS.getVocabulary().nullaryNonRel()) {
		    if (thisTVS.eval(predicate) != otherTVS.eval(predicate)) {
		        return false;
		    }
		}
		// Check non relational unary
		if (!checkIsomorphism(thisTVS, otherTVS, thisMap, otherInvMap, thisTVS.getVocabulary().unaryNonRel())) {
		    return false;
		}
		if (!checkIsomorphism(thisTVS, otherTVS, thisMap, otherInvMap, thisTVS.getVocabulary().binary())) {
		    return false;
		}
		if (!checkIsomorphism(thisTVS, otherTVS, thisMap, otherInvMap, thisTVS.getVocabulary().kary())) {
		    return false;
		}
		return true;
	}

    protected static boolean checkIsomorphism(HighLevelTVS thisTVS, HighLevelTVS otherTVS,
            Map<Node, Canonic> thisMap, Map<Canonic, Node> otherInvMap, Set<Predicate> predicates) {
        for (Predicate predicate : predicates) {
            if (thisTVS.numberSatisfy(predicate) != otherTVS.numberSatisfy(predicate)) {
                return false;
            }
            PredicateEvaluator otherEval = PredicateEvaluator.evaluator(predicate, otherTVS);
            Iterator<Entry<NodeTuple, Kleene>> thisIt = thisTVS.iterator(predicate);
            while (thisIt.hasNext()) {
                Entry<NodeTuple, Kleene> thisEntry = thisIt.next();
                NodeTuple thisTuple = thisEntry.getKey();
                NodeTuple otherTuple = mapNodeTuple(predicate, thisTuple, thisMap, otherInvMap);
                if (thisEntry.getValue() != otherEval.eval(otherTuple)) {
                    return false;
                }
            }
        }
        return true;
    }
	
    public static void printStatistics(PrintStream out) {
    	if (totalQueries > 0) {
    		long current = 0;
    		long weak = 0;
    		for (Collection<WeakReference<HighLevelTVS>> entries : globalCache.values()) {
    			for (WeakReference<HighLevelTVS> ref : entries) {
    				if (ref.get() == null) {
    					weak++;
    				} else {
    					current++;
    				}
    			}
    		}
    		out.println("TVS Cache: " + (foundInCache) + "/" + (totalQueries) + ". Current " + current + ". Weak " + weak + ". NonIso " + nonIsomorphic);
    	}
    	
    	tryAllInCache(out);
    	globalCache.clear();
    	tryAllInCache(out);    	
    }

	private static void tryAllInCache(PrintStream out) {
		long structures = 0;
    	long notInCache = 0;
    	Collection<HighLevelTVS> addedToCache = new ArrayList<HighLevelTVS>(); 
    	for (Location location : AnalysisGraph.activeGraph.getLocations()) {
    		DecomposeLocation dlocation = (DecomposeLocation) location;
    		
    		for (HighLevelTVS structure : dlocation.everyStructure()) {
    			structures++;
    			structure = structure.copy(); 
    			Set<Canonic> canonicNames = getCanonicSetForBlurred(structure);
    			if (addStructureToCache(structure, canonicNames) == structure) {
    				addedToCache.add(structure); // Save them to avoid weak reference to die in the middle of the counting...
    				notInCache++;
    			}
    		}
    	}
		out.println("TVS Cache Internal: " + (notInCache) + "/" + (structures));
	}
}