package tvla.core.decompose;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.Engine;
import tvla.core.HighLevelTVS;
import tvla.core.TVSFactory;
import tvla.core.TVSSet;
import tvla.core.generic.ConcreteTVSSet;
import tvla.core.meet.Meet;
import tvla.exceptions.SemanticErrorException;
import tvla.predicates.DynamicVocabulary;
import tvla.util.EmptyIterator;
import tvla.util.HashMapFactory;

/**
 * Represent a Cartesian element in the decomposed abstract domain. Realized as
 * a map between decomposition name and the structures with that name.
 */
public class CartesianElement implements Iterable<HighLevelTVS> {
    protected Map<DecompositionName, TVSSet> structures;

    /**
     * Construct a new element with the given structures
     * 
     * @param structures
     *            The structures for the new element. Ownership is transferred.
     */
    public CartesianElement(Map<DecompositionName, TVSSet> structures) {
        this.structures = structures;
    }

    /**
     * Construct a new empty element
     */
    public CartesianElement() {
        this.structures = HashMapFactory.make();
    }

    public boolean isEmpty() {
        return structures.isEmpty();
    }

    /**
     * The number of structures stored in this element
     */
    public int size() {
        int result = 0;
        for (TVSSet set : structures.values()) {
            result += set.size();
        }
        return result;
    }

    /**
     * Empty iterator of the right type.
     */
    static private Iterator<HighLevelTVS> empty = EmptyIterator.instance();
	private boolean cachingMode;

    // public static long prepareTime;

    /**
     * Iterate over all structures in the element regardless of their
     * decomposition name.
     */
    public Iterator<HighLevelTVS> iterator() {
        return new Iterator<HighLevelTVS>() {
            Iterator<TVSSet> setIterator = structures.values().iterator();

            Iterator<HighLevelTVS> iterator = setIterator.hasNext() ? setIterator.next().iterator()
                    : empty;

            HighLevelTVS next = null;

            public void remove() {
                throw new UnsupportedOperationException("Can't remove from this collection");
            }

            public void advance() {
                while (next == null && (iterator.hasNext() || setIterator.hasNext())) {
                    if (iterator.hasNext()) {
                        next = iterator.next();
                    } else {
                        iterator = setIterator.next().iterator();
                    }
                }
            }

            public HighLevelTVS next() {
                advance();
                HighLevelTVS result = next;
                next = null;
                return result;
            }

            public boolean hasNext() {
                advance();
                return next != null;
            }

        };
    }

    /**
     * Join this element with a new element, updating this element.
     * 
     * @return true iff this element has changed by this join
     */
    public CartesianElement join(CartesianElement other) {
        CartesianElement delta = new CartesianElement();
        delta.cachingMode = cachingMode;
        boolean change = false;
        for (DecompositionName name : other.structures.keySet()) {
            change = join(other, name, delta) || change;
        }
        return change ? delta : null;
    }

    public boolean join(CartesianElement other, DecompositionName name, CartesianElement delta) {
        boolean change = false;
        TVSSet current = structures.get(name);
        TVSSet newStructs = other.structures.get(name);
        if (newStructs == null) {
            return false;
        }
        if (current == null) {
            share(name, other);
            if (delta != null) {
                delta.share(name, other);
            }
            change = true;
        } else {
            current = getForWrite(name);
            for (HighLevelTVS newStruct : newStructs) {
                HighLevelTVS deltaTVS = current.mergeWith(newStruct);
                if (deltaTVS != null) {
                    if (delta != null) {
                        delta.join(name, deltaTVS);
                    }
                    change = true;
                }
            }
        }
        return change;
    }

    /**
     * Meet this element with the given element.
     */
    public void meet(CartesianElement other) {
        for (DecompositionName name : other.structures.keySet()) {
            Iterable<HighLevelTVS> current = structures.get(name);
            if (current == null) {
                share(name, other);
            } else {
                TVSSet newStructs = other.structures.get(name);
                Engine.activeEngine.getAnalysisStatus().startTimer(AnalysisStatus.MEET_TIME);
                current = Meet.meet(current, newStructs);
                Engine.activeEngine.getAnalysisStatus().stopTimer(AnalysisStatus.MEET_TIME);
                put(name, current);
            }
        }
    }

    /**
     * Decompose the given structure and join it with this element, updating
     * this element.
     * 
     * @return true iff this element has changed by this join
     */
    public CartesianElement join(HighLevelTVS structure) {
        return join(Decomposer.getInstance().decompose(structure));
    }

    /**
     * Join a new substructure with the given decomposition name with this
     * element, updating this element.
     * 
     * @return true iff this element has changed by this join
     */
    public boolean join(DecompositionName name, HighLevelTVS substruct) {
        TVSSet bucket = getForWrite(name);
        HighLevelTVS delta = bucket.mergeWith(substruct);
        return delta != null;
    }

    /**
     * The current set of decomposition names
     */
    public Set<? extends DecompositionName> names() {
        return Collections.unmodifiableSet(structures.keySet());
    }

    /**
     * The structures associated with the given decomposition name. Should NOT
     * be changed.
     */
    public TVSSet get(DecompositionName name) {
        return structures.get(name);
    }

    /**
     * Copy on write
     */
    public CartesianElement copy() {
        CartesianElement copy = new CartesianElement();
        for (DecompositionName name : names()) {
            copy.share(name, this);
        }
        copy.setCachingMode(cachingMode);
        return copy;
    }

    /**
     * Compose the given decomposition names into a new name. The original
     * decomposition names and their structures are untouched.
     * 
     * @return The decomposition name of the tuple.
     */
    public DecompositionName compose(Collection<DecompositionName> tuple) {
        Iterator<DecompositionName> nameIt = tuple.iterator();
        assert nameIt.hasNext();
        DecompositionName composedName = nameIt.next();
        Iterable<HighLevelTVS> composed = get(composedName);
        assert nameIt.hasNext();
        while (nameIt.hasNext()) {
            DecompositionName newName = nameIt.next();
            composedName = composedName.compose(newName);
            composed = compose(composed, get(newName), composedName, null);
        }
        put(composedName, composed);
        return composedName;
    }

    /**
     * Compose the given decomposition names into a new name. The original
     * decomposition names and their structures are untouched.
     * 
     * @return The decomposition name of the tuple.
     */
    public DecompositionName compose(DecompositionName oneName, DecompositionName twoName, DynamicVocabulary composedVoc) {
        DecompositionName composedName = oneName.compose(twoName);
        Iterable<HighLevelTVS> composed = compose(get(oneName), get(twoName), composedName, composedVoc);
        put(composedName, composed);
        return composedName;
    }

    /**
     * Compose two sets of structures which will have the given composedName.
     * 
     * @return The result of the composition
     */
    public static Iterable<HighLevelTVS> compose(Iterable<HighLevelTVS> oneSet, Iterable<HighLevelTVS> twoSet, 
    		DecompositionName composedName, DynamicVocabulary composedVoc) {
        // Prepare both structures for composition
        Iterable<HighLevelTVS> thisCopy = Decomposer.getInstance().prepareForComposition(oneSet, composedName, composedVoc);
        Iterable<HighLevelTVS> otherCopy = Decomposer.getInstance().prepareForComposition(twoSet, composedName, composedVoc);
        // Meet
        Iterable<HighLevelTVS> meet = Meet.meet(thisCopy, otherCopy);
                
        // Cleanup after composition
        Iterable<HighLevelTVS> after = Decomposer.getInstance().afterComposition(meet);
        return after;
    }

    /**
     * Remove the given decomposition name.
     */
    public void remove(DecompositionName name) {
        TVSSet oldSet = structures.remove(name);
        if (oldSet != null) {
            oldSet.unshare();
        }
    }

    /**
     * Set the decomposition name to have the given set.
     */
    public void put(DecompositionName name, Iterable<HighLevelTVS> set) {
        TVSSet currentSet = structures.get(name);
        boolean empty = !set.iterator().hasNext();
        if (currentSet != null) {
            currentSet.unshare();
            if (empty) {
                structures.remove(name);
            } else {
                structures.put(name, toTVSSet(set));
            }       
        } else if (!empty) {
            structures.put(name, toTVSSet(set));
        }
    }

    protected TVSSet toTVSSet(Iterable<HighLevelTVS> set) {
        if (set instanceof TVSSet) {
            return (TVSSet) set;
        } else {
            TVSSet result = TVSFactory.getInstance().makeEmptySet();
            result.setCachingMode(cachingMode);
            for (HighLevelTVS structure : set) {
                result.mergeWith(structure);
            }
            return result;
        }
    }

    public String toString() {
        return structures.toString();
    }

    public void share(DecompositionName name, CartesianElement origin) {
        TVSSet set = origin.structures.get(name);
        if (set == null) {
            remove(name);
        } else {
            if (set instanceof ConcreteTVSSet || (cachingMode && !origin.cachingMode)) {
                TVSSet copy = TVSFactory.getInstance().makeEmptySet();
                copy.setCachingMode(cachingMode);
                for (HighLevelTVS structure : set) {
                    copy.mergeWith(structure);
                }
                set = copy;
            } else {
                set.share();
            }
        	assert cachingMode == set.getCachingMode();
            put(name, set);
        }
    }

    protected TVSSet getForWrite(DecompositionName name) {
        TVSSet bucket = structures.get(name);
        if (bucket == null) {
            bucket = TVSFactory.getInstance().makeEmptySet();
            bucket.setCachingMode(cachingMode);
            structures.put(name, bucket);
        } else {
            TVSSet set = bucket.modify();
            if (set != bucket) {
                bucket = set;
                put(name, bucket);
            }
        }
        return bucket;
    }

    public boolean isomorphic(CartesianElement orig) {
        if (!orig.names().equals(names())) {
            return false;
        }
        for (DecompositionName name : names()) {
            TVSSet set = get(name);
            TVSSet origSet = orig.get(name);
            if (!Boolean.TRUE.equals(origSet.isomorphic(set))) {
                return false;
            }
        }
        return true;
    }

    public void permuteBack() {
        AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.PERMUTE_TIME);
        if (ParametricSet.isParamteric()) {
            CartesianElement result = new CartesianElement();
            result.cachingMode = this.cachingMode;
            for (Map.Entry<DecompositionName, TVSSet> entry : structures.entrySet()) {
                DecompositionName name = entry.getKey();
                TVSSet set = entry.getValue();
                // What are the right target names?
                if (Decomposer.getInstance().isComposed(name)) {
                    throw new SemanticErrorException("Can't permute a composed domain! " + name);
                }

                // Pick one in a deterministic way
                DecompositionName rep = Decomposer.getInstance().getParametricRepresentative(name);
                
                for (TVSSet permutedSet : Decomposer.getInstance().permute(rep, name, set)) {
                    CartesianElement temp = new CartesianElement();
                    temp.put(rep, permutedSet);
                    result.join(temp);
                }
            }
            this.structures = result.structures;
        }
        AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.PERMUTE_TIME);
    }

	public void setCachingMode(boolean cachingMode) {
		this.cachingMode = cachingMode;
	}

	public boolean getCachingMode() {
		return this.cachingMode;
	}
}
