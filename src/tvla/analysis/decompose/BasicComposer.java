package tvla.analysis.decompose;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.analysis.AnalysisStatus;
import tvla.core.FrameManager;
import tvla.core.Framer;
import tvla.core.HighLevelTVS;
import tvla.core.TVSFactory;
import tvla.core.TVSSet;
import tvla.core.decompose.CartesianElement;
import tvla.core.decompose.Decomposer;
import tvla.core.decompose.DecompositionName;
import tvla.core.decompose.ParametricSet;
import tvla.core.generic.ConcreteTVSSet;
import tvla.exceptions.SemanticErrorException;
import tvla.formulae.Formula;
import tvla.predicates.DynamicVocabulary;
import tvla.util.EmptyIterator;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.SimpleIterator;
import tvla.util.ProgramProperties;

/**
 * Basic Composer. The formula is a disjunction of conjunctions. All predicate
 * must be parts of decomposition names. Each free variable represents a column
 * to compose.
 * 
 * @author tla
 * 
 */
public class BasicComposer implements Composer {
  
        public static boolean compositionAbstraction = ProgramProperties.getBooleanProperty("tvla.decompose.compositionAbstraction", true);

	protected Map<DecompositionName, Collection<DecompositionName>> toCompose;
	protected Map<DecompositionName, Collection<DecompositionName>> baseCompose;
	protected Set<DecompositionName> redundant = HashSetFactory.make();
    protected Set<DecompositionName> singleUse = HashSetFactory.make();
	private final boolean incremental;
	private Map<DecompositionName, Iterable<HighLevelTVS>> readyOld;
	private Map<DecompositionName, Iterable<HighLevelTVS>> readyNew;
	private Map<DecompositionName, Iterable<HighLevelTVS>> readyDelta;

	private CartesianElement unframedOld;
	private CartesianElement unframedNew;
	private CartesianElement unframedDelta;
	
	private CartesianElement beforeOld;
	private CartesianElement beforeDelta;
	private CartesianElement beforeNew;
	private final FrameManager framerPre;
    private final CompositionFilter filter;
    
	public BasicComposer(Formula composeFormula, boolean incremental, FrameManager framerPre,
	        CompositionFilter filter) {
		this.incremental = incremental;
		this.framerPre = framerPre;
        this.filter = filter;
		if (composeFormula == null) {
			return;
		}
		baseCompose = HashMapFactory.make();

		// composeFormula = Formula.toPrenexDNF(composeFormula);
		List<Formula> disjuncts = new ArrayList<Formula>();
		Formula.getOrs(composeFormula, disjuncts);
		Collection<Collection<DecompositionName>> composition = HashSetFactory
				.make();
		for (Formula disjunct : disjuncts) {
			Set<Set<DecompositionName>> toComposeDisjunct = Decomposer
					.toDecompositionNames(disjunct);
			composition.addAll(toComposeDisjunct);
		}
		optimizeOrder(composition);

		for (Collection<DecompositionName> components : composition) {
			DecompositionName result = Decomposer.compose(components);
			baseCompose.put(result, components);
			redundant.addAll(components);
		}
	}

	/**
	 * Optimize the composition order to try to get as much memoization as
	 * possible.
	 * 
	 * @param composition
	 */
	protected void optimizeOrder(
			Collection<Collection<DecompositionName>> composition) {
		Set<DecompositionName> sharedNames = null;
		for (Collection<DecompositionName> tuple : composition) {
			if (sharedNames == null) {
				sharedNames = HashSetFactory.make(tuple);
			} else {
				sharedNames.retainAll(tuple);
			}
		}
		if (sharedNames != null && !sharedNames.isEmpty()) {
			Collection<Collection<DecompositionName>> newToCompose = new ArrayList<Collection<DecompositionName>>();
			for (Collection<DecompositionName> tuple : composition) {
				Collection<DecompositionName> newTuple = new ArrayList<DecompositionName>(
						sharedNames);
				// Decide on a fixed order for the rest of the predicates
				Set<DecompositionName> sortedTuple = HashSetFactory.make();
				sortedTuple.addAll(tuple);
				sortedTuple.removeAll(sharedNames);
				newTuple.addAll(sortedTuple);
				newToCompose.add(newTuple);
			}
			composition.clear();
			composition.addAll(newToCompose);
		}
	}

	protected boolean prepare(Map<DecompositionName, Iterable<HighLevelTVS>> ready, CartesianElement unframed, 
			CartesianElement before, DecompositionName name) {
		if (ready.containsKey(name)) {
			return true;
		}
		// System.err.println("prepare " + name + " before names " +
		// before.names());
		TVSSet beforeSet = before.get(name);
		if (beforeSet == null && (ParametricSet.isParamteric() || name.isAbstraction())) {
			AnalysisStatus.getActiveStatus().startTimer(
					AnalysisStatus.PERMUTE_TIME);
			// Try to restore the name from an existing name
			DecompositionName orig = null;
			for (DecompositionName candidate : Decomposer.getInstance()
					.permute(name)) {
				if (Decomposer.getInstance().isComposed(candidate))
					continue;
				candidate = candidate.getBase().iterator().next();
				if (before.names().contains(candidate)) {
					if (orig == null) {
						orig = candidate;
					} else if (name.isAbstraction()) {
					    // For abstraction we can have multiple matches.
					    // peek the one we origin
						orig = name.getBase().contains(orig) ? orig : candidate;
					} else {
						throw new SemanticErrorException(
								"Have multiple restore options for " + name
										+ " e.g. " + orig + " and " + candidate);
					}
				}
			}
			if (orig != null) {
				beforeSet = TVSFactory.getInstance().makeEmptySet(TVSFactory.JOIN_CONCRETE);
				TVSSet origBeforeSet = before.get(orig);
				for (TVSSet structures : Decomposer.getInstance().permute(name,
						orig, origBeforeSet)) {
					for (HighLevelTVS structure : structures) {
						beforeSet.mergeWith(structure);
					}
				}
				if (!ParametricSet.isMulti()) {
					before.put(name, beforeSet);
				}
			}
			AnalysisStatus.getActiveStatus().stopTimer(
					AnalysisStatus.PERMUTE_TIME);
		}
		if (beforeSet == null) {
			return false;
		}
        if (name.isAbstraction() && compositionAbstraction) {
            CartesianElement decomposed = Decomposer.getInstance().decompose(beforeSet, name);
            beforeSet = decomposed.get(name);
        }		
		
		TVSSet result = filter.filter(name, beforeSet, singleUse.contains(name));
		
		if (!result.isEmpty()) {
    		unframed.put(name, result);
    		ready.put(name, frame(result, name));
		}
		
		return ready.containsKey(name);
	}

    private TVSSet frame(TVSSet structures, DecompositionName name) {
		// Apply frame before composition
		Framer cFramerPre = framerPre.getFramer(name);
		if (cFramerPre != null) {
			structures = new ConcreteTVSSet(cFramerPre.frame(structures, name));
		}
		return structures;
	}

	/**
	 * Incrementally compose old, delta, and new.
	 * 
	 * @return The composed delta (i.e., all the composed structures not all
	 *         coming from old)
	 */
	protected void incrementalCompose(DecompositionName target) {
		if (!prepare(readyDelta, unframedDelta, beforeDelta, target)) {
			Set<DecompositionName> newNames = HashSetFactory.make();
			Collection<DecompositionName> tuple = toCompose.get(target);

			if (tuple.size() == 1) {
				return; // Already handled in above prepare
			}
			Iterator<DecompositionName> tupleIt = tuple.iterator();
			assert tupleIt.hasNext();
			DecompositionName currentName = tupleIt.next();
			if (!prepareAll(currentName)) {
				return;
			}
			DecompositionName prevName = null;
			DecompositionName prevPrevName = null;
			while (tupleIt.hasNext()) {
				DecompositionName addedName = tupleIt.next();
				if (!prepareAll(addedName)) {
					return;
				}
				DecompositionName combinedName = currentName.compose(addedName);
				if (!readyDelta.containsKey(combinedName)) {
					newNames.add(combinedName);
					if (!readyNew.containsKey(currentName)) {
						assert prevName != null && prevPrevName != null;
						TVSSet oldSet = 
						    toTVSSet(compose(readyOld, prevName, readyOld, prevPrevName, currentName));
						TVSSet newSet = oldSet.copy();
						readyOld.put(currentName, oldSet);
						if (readyDelta.containsKey(currentName)) {
							join(newSet, readyDelta.get(currentName));
						}
						readyNew.put(currentName, newSet);
					}
					Iterable<HighLevelTVS> combined = computeIncrement(currentName, addedName, combinedName);					
					readyDelta.put(combinedName, combined);
				}
				prevPrevName = currentName;
				prevName = addedName;
				currentName = combinedName;
			}
		}
	}

    private Iterable<HighLevelTVS> computeIncrement(
            final DecompositionName currentName, final DecompositionName addedName,
            final DecompositionName combinedName) {
        Iterable<HighLevelTVS> combined = new Iterable<HighLevelTVS>() {
            public Iterator<HighLevelTVS> iterator() {
                return new SimpleIterator<HighLevelTVS>() {                            
                    private Iterator<HighLevelTVS> structuresIt = EmptyIterator.instance(); 
                    private int stage = 0;
                    @Override
                    protected HighLevelTVS advance() {
                        boolean started = AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.COMPOSE_TIME);
                        while (!structuresIt.hasNext()) {
                            switch(stage) {
                            case 0:
                                structuresIt = compose(readyDelta, currentName,
                                        readyNew, addedName, combinedName).iterator();
                                break;
                            case 1:
                                structuresIt = compose(readyOld, currentName,
                                        readyDelta, addedName, combinedName).iterator();
                                break;
                            default:
                                if (started)
                                    AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.COMPOSE_TIME);
                                return null;
                            }
                            stage++;
                        }
                        HighLevelTVS result = structuresIt.next();
                        if (started)
                            AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.COMPOSE_TIME);
                        return result;
                    }
                };
            }
            
        };
        return combined;
    }

	private TVSSet toTVSSet(Iterable<HighLevelTVS> set) {
        if (set instanceof TVSSet) {
            return (TVSSet) set;
        } else {
            TVSSet result = TVSFactory.getInstance().makeEmptySet(TVSFactory.JOIN_CONCRETE);
            for (HighLevelTVS structure : set) {
                result.mergeWith(structure);
            }
            return result;
        }
    }

    private boolean prepareAll(DecompositionName name) {
		boolean haveDelta = prepare(readyDelta, unframedDelta, beforeDelta, name);
		boolean haveOld = prepare(readyOld, unframedOld, beforeOld, name);

		if (!haveDelta && !haveOld) {
			return false;
		}
		if (!haveDelta) {
			readyNew.put(name, new ConcreteTVSSet(readyOld.get(name)));
			return true;
		}
		if (!haveOld) {
			readyNew.put(name, new ConcreteTVSSet(readyDelta.get(name)));
			return true;
		}

		// Have both - compute new  
		// if there was an extra abstraction - recompute delta
		TVSSet both = unframedOld.get(name).copy();
		TVSSet realDelta = TVSFactory.getInstance().makeEmptySet(TVSFactory.JOIN_CONCRETE);
		for (HighLevelTVS structure : unframedDelta.get(name)) {
		    HighLevelTVS moreDelta = both.mergeWith(structure);
		    if (name.isAbstraction() && moreDelta != null) {
		        realDelta.mergeWith(moreDelta);
		    }
		}
		readyNew.put(name, frame(both, name));
		if (name.isAbstraction()) {
		    readyDelta.put(name, frame(realDelta, name));
		}
		return true;
	}

	/**
	 * Non incremental composition of the element
	 */
	protected void nonIncrementalCompose(DecompositionName target) {
		if (!prepare(readyNew, unframedNew, beforeNew, target)) {
			Set<DecompositionName> newNames = HashSetFactory.make();
			Collection<DecompositionName> tuple = toCompose.get(target);

			if (tuple.size() == 1) {
				return; // Already handled in above prepare
			}

			Iterator<DecompositionName> tupleIt = tuple.iterator();
			assert tupleIt.hasNext();
			DecompositionName currentName = tupleIt.next();
			if (!prepare(readyNew, unframedNew, beforeNew, currentName)) {
				return;
			}
			while (tupleIt.hasNext()) {
				DecompositionName addedName = tupleIt.next();
				if (!prepare(readyNew, unframedNew, beforeNew, addedName)) {
					return;
				}
				DecompositionName combinedName = currentName.compose(addedName);
				if (!readyNew.containsKey(combinedName)) {
					newNames.add(combinedName);
					Iterable<HighLevelTVS> composed = compose(readyNew, currentName, readyNew, addedName, combinedName);
					readyNew.put(combinedName, composed);
				}
				currentName = combinedName;
			}
		}
	}

	/**
	 * Utility method for composing a name from one element with a name in
	 * another element
	 * 
	 * @param combinedName
	 *            The expected combined name
	 * @return The composed set
	 */
	protected Iterable<HighLevelTVS> compose(Map<DecompositionName, Iterable<HighLevelTVS>> one,
			DecompositionName oneName, Map<DecompositionName, Iterable<HighLevelTVS>> two,
			DecompositionName twoName, DecompositionName combinedName) {
		Iterable<HighLevelTVS> oneSet = one.get(oneName);
		Iterable<HighLevelTVS> twoSet = two.get(twoName);
		if (oneSet == null || twoSet == null) {
			return TVSFactory.getInstance().makeEmptySet(
					TVSFactory.JOIN_CONCRETE);
		}

		// Build the combined vocabulary of the composed name using the
		// vocabulary of the original names and the frame information.
		DynamicVocabulary oneVoc = getVocabulary(oneName);
		DynamicVocabulary twoVoc = getVocabulary(twoName);
		DynamicVocabulary combinedVoc = oneVoc.union(twoVoc);
		
		return CartesianElement.compose(oneSet, twoSet, combinedName, combinedVoc);
	}

    protected DynamicVocabulary getVocabulary(DecompositionName name) {
        DynamicVocabulary oneVoc = Decomposer.getInstance().getVocabulary(name);
		Framer oneFramer = framerPre.getFramer(name);		
		if (oneFramer != null) {
			oneVoc = oneFramer.getFramedVocabulary(oneVoc);
		}
        return oneVoc;
    }

	/**
	 * Utility method for joining toAdd into current, returning the delta.
	 */
	protected TVSSet join(TVSSet current, Iterable<HighLevelTVS> structures) {
		// long start = System.currentTimeMillis();
		TVSSet delta = TVSFactory.getInstance().makeEmptySet(
				TVSFactory.JOIN_CONCRETE);
		for (HighLevelTVS structure : structures) {
			HighLevelTVS deltaTVS = current.mergeWith(structure);
			if (deltaTVS != null)
				delta.mergeWith(deltaTVS);
		}
		// joinTime += (System.currentTimeMillis() - start);
		return delta;
	}

	public boolean compose(DecompositionName target) {
		if (computeIncreamentally()) {
			incrementalCompose(target);
			return readyDelta.containsKey(target);
		} else {
			nonIncrementalCompose(target);
			return readyNew.containsKey(target);
		}
	}

	private boolean computeIncreamentally() {
		return incremental && beforeDelta != null && !beforeOld.isEmpty();
	}

	public Collection<DecompositionName> getComposedNames() {
		return toCompose.keySet();
	}

	public Set<DecompositionName> getIntermediates(DecompositionName target) {
		Collection<DecompositionName> tuple = toCompose.get(target);
		Set<DecompositionName> result = HashSetFactory.make();
		Iterator<DecompositionName> tupleIt = tuple.iterator();
		assert tupleIt.hasNext();
		DecompositionName currentName = tupleIt.next();
		result.add(currentName);
		while (tupleIt.hasNext()) {
			DecompositionName addedName = tupleIt.next();
			currentName = currentName.compose(addedName);
			result.add(addedName);
			result.add(currentName);
		}
		return result;
	}

	public void init(Set<? extends DecompositionName> currentNames) {
		toCompose = HashMapFactory.make();
		if (baseCompose == null) {
			for (DecompositionName name : currentNames) {
				toCompose.put(name, Collections.singleton(name));
			}
		} else {
			toCompose.putAll(baseCompose);
			for (DecompositionName name : currentNames) {
				if (!toCompose.containsKey(name) && !redundant.contains(name)) {
					toCompose.put(name, Collections.singleton(name));
				}
			}
		}
		// SingleUse should contain all components that are not used to build more than one component
		singleUse = HashSetFactory.make();
        Set<DecompositionName> multiUse = HashSetFactory.make();
        for (DecompositionName name : toCompose.keySet()) {
            for (DecompositionName component : toCompose.get(name)) {
                if (name != component && !singleUse.add(component)) {
                    multiUse.add(component);
                }
            }
        }
        singleUse.removeAll(multiUse);
        return; // For BP
	}

	public Map<DecompositionName, Iterable<HighLevelTVS>> init(CartesianElement beforeOld, CartesianElement beforeDelta,
			CartesianElement beforeNew) {
		this.readyOld = HashMapFactory.make();
		this.readyDelta = HashMapFactory.make();
		this.readyNew = HashMapFactory.make();
		this.beforeOld = beforeOld;
		this.beforeDelta = beforeDelta;
		this.beforeNew = beforeNew;
		this.unframedOld = new CartesianElement();
		this.unframedDelta = new CartesianElement();
		this.unframedNew = new CartesianElement();
		return computeIncreamentally() ? readyDelta : readyNew;
	}

	public Collection<DecompositionName> getSources(DecompositionName name) {
		return this.toCompose.get(name);
	}

	public void done() {
		// Go over before sets and remove decomposition name which have abstraction as they are local to an action
		cleanAbstrationComponents(this.beforeOld);
		cleanAbstrationComponents(this.beforeNew);
		cleanAbstrationComponents(this.beforeDelta);
		
		this.readyOld = null;
		this.readyDelta = null;
		this.readyNew = null;
		this.beforeOld = null;
		this.beforeDelta = null;
		this.beforeNew = null;
		this.unframedOld = null;
		this.unframedDelta = null;
		this.unframedNew = null;
	}

	private void cleanAbstrationComponents(CartesianElement element) {
		if (element == null) {
			return;
		}
		Collection<DecompositionName> names = new ArrayList<DecompositionName>(element.names());
		for (DecompositionName name : names) {
			if (name.isAbstraction()) {
				element.remove(name);
			}
		}
	}
	
	public void removeDelta(DecompositionName name) {
		if (this.readyDelta != null) {
			this.readyDelta.remove(name);
			this.unframedDelta.remove(name);
		}
	}

	public void removeNew(DecompositionName name) {
		this.readyNew.remove(name);
		this.unframedNew.remove(name);
	}

	public void removeOld(DecompositionName name) {
		this.readyOld.remove(name);
		this.unframedOld.remove(name);
	}
}
