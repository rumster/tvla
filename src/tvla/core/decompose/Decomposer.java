package tvla.core.decompose;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.Engine;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVSSet;
import tvla.core.base.PredicateUpdater;
import tvla.core.common.ModifiedPredicates;
import tvla.core.common.NodeTupleIterator;
import tvla.exceptions.SemanticErrorException;
import tvla.formulae.EquivalenceFormula;
import tvla.formulae.Formula;
import tvla.formulae.NotFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.PredicateUpdateFormula;
import tvla.formulae.Var;
import tvla.io.IOFacade;
import tvla.logic.Kleene;
import tvla.predicates.DynamicVocabulary;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.Location;
import tvla.util.Apply;
import tvla.util.ApplyIterable;
import tvla.util.HashSetFactory;
import tvla.util.Pair;
import tvla.util.ProgramProperties;
import tvla.util.Timer;

/**
 * Decompose TVS into substructures according to difference decomposition
 * methods and criteria
 * 
 * @author tla
 */
public abstract class Decomposer {

    protected static final Node[] EMPTY_NODE_ARRAY = new Node[0];

    protected static final boolean mustOutside = ProgramProperties.getBooleanProperty("tvla.decompose.mustOutside",
            false);

    public static Set<Predicate> ignorePredicates = HashSetFactory.make();

    public static Decomposer instance;

    static {
        ignorePredicates.add(Vocabulary.active);
        ignorePredicates.add(Vocabulary.instance);
        ignorePredicates.add(Vocabulary.isNew);
        // if (!(Engine.activeEngine instanceof MultithreadEngine)) {
        // ignorePredicates.add(Vocabulary.ready);
        // ignorePredicates.add(Vocabulary.isThread);
        // ignorePredicates.add(Vocabulary.runnable);
        // }
    }

    /**
     * Should a context node be kept for each substructure
     */
    protected final boolean explicitOutside;

    /**
     * Should the context be built by merging the current context or by assuming
     * any possible context
     */
    protected final boolean merge;

    protected final boolean keepBinary;

    protected final boolean printComposeConstraintBreach;

    public final boolean coercedBeforeAction;

    protected Decomposer() {
        this.explicitOutside = ProgramProperties.getBooleanProperty("tvla.decompose.outside.explicit", true);
        this.merge = ProgramProperties.getBooleanProperty("tvla.decompose.outside.merge", false);
        this.keepBinary = ProgramProperties.getBooleanProperty("tvla.decompose.outside.keepBinary", false);
        this.coercedBeforeAction = ProgramProperties.getBooleanProperty("tvla.decompose.coercedBeforeAction", false);
        this.printComposeConstraintBreach = ProgramProperties.getBooleanProperty(
                "tvla.decompose.printComposeConstraintBreach", false);
    }

    /**
     * Perform the decomposition
     * 
     * @param structure
     *            The structure to decompose
     * @return For each decomposition name, the resulting TVS
     */
    public abstract CartesianElement decompose(HighLevelTVS structure);

    /**
     * Perform the decomposition for a given decomposition name
     * 
     * @param structure
     *            The structure to decompose
     * @param name
     *            The decomposition name
     * @return The resulting element
     */
    public abstract CartesianElement decompose(HighLevelTVS structure, DecompositionName name);

    /**
     * Perform the decomposition on a set of structures
     * 
     * @param structures
     *            The structures to decompose
     * @return For each decomposition name, the resulting TVSSet
     */
    public CartesianElement decompose(TVSSet structures) {
        CartesianElement result = new CartesianElement();
        for (HighLevelTVS structure : structures) {
            result.join(decompose(structure));
        }
        return result;
    }

    /**
     * Perform the decomposition on a set of structures for a given
     * decomposition name
     * 
     * @param structures
     *            The structures to decompose
     * @param name
     *            The decomposition name
     * @return The resulting element
     */
    public CartesianElement decompose(TVSSet structures, DecompositionName name) {
        CartesianElement result = new CartesianElement();
        for (HighLevelTVS structure : structures) {
            result.join(decompose(structure, name));
        }
        return result;
    }

    /**
     * Return the set of decomposition names for which the given predicate holds
     * (or doesn't hold if negated is true)
     */
    public abstract Set<? extends DecompositionName> match(Predicate predicate, boolean negated,
            DynamicVocabulary actionVoc, DynamicVocabulary actionKillVoc);

    /**
     * Return the decomposition names of this decomposer
     */
    public abstract Set<? extends DecompositionName> names();

    /**
     * Add a new decomposition name described by the given formula
     * 
     * @param name
     */
    public abstract void addDecompositionFormula(Formula formula, String name);

    /**
     * Helper method for projecting the TVS on the given universe
     */
    protected void project(HighLevelTVS tvs, Set<Node> universe) {
        Collection<Node> nodes = new ArrayList<Node>(tvs.nodes());
        for (Node node : nodes) {
            if (!universe.contains(node)) {
                tvs.removeNode(node);
            }
        }
    }

    /**
     * Project onto given universe and add a summary node for which all
     * predicates are 1/2 representing the context of the substructure
     */
    protected Node addOutsideByProjection(HighLevelTVS substruct, Set<Node> universe) {
        boolean full = substruct.nodes().size() == universe.size();
        project(substruct, universe);
        substruct.clearPredicate(Vocabulary.outside);
        Node outside = null;
        if (!full) {
            outside = substruct.newNode();
            substruct.update(Vocabulary.outside, outside, Kleene.trueKleene);
        }
        return outside;
    }

    protected void setOutsideInformation(HighLevelTVS structure, Node outside, Set<Predicate> predicates, Kleene value) {
        for (Predicate predicate : predicates) {
            if (ignorePredicates.contains(predicate))
                continue;
            if (predicate == Vocabulary.outside)
                continue;
            ModifiedPredicates.modify(structure, predicate);

            if (predicate.arity() == 1) {
                structure.update(predicate, outside, value);
            } else {
                // Update all tuples in which at least one node is the outside
                // node with
                // value
                PredicateUpdater updater = PredicateUpdater.updater(predicate, structure);
                // Each round one position is outside and the rest is null
                Node[] template = new Node[predicate.arity()];
                for (int i = 0; i < template.length; i++) {
                    template[i] = outside;
                    Iterator<? extends NodeTuple> iterator = NodeTupleIterator.createIterator(structure.nodes(),
                            template);
                    while (iterator.hasNext()) {
                        NodeTuple tuple = iterator.next();
                        updater.update(tuple, value);
                    }
                    template[i] = null;
                }
            }
        }
    }

    /**
     * Merge all nodes not in the universe into a single abstract node or null
     * if not such nodes
     * 
     * @param keepBinary
     */
    protected Node addOutsideByMerge(HighLevelTVS substruct, Set<Node> universe) {
        substruct.clearPredicate(Vocabulary.outside);
        Node outside = null;
        if (substruct.nodes().size() != universe.size()) {
            Collection<Node> nodesToMerge = new ArrayList<Node>();
            for (Node node : substruct.nodes()) {
                if (!universe.contains(node)) {
                    nodesToMerge.add(node);
                }
            }

            outside = substruct.mergeNodes(nodesToMerge);
            substruct.update(Vocabulary.outside, outside, Kleene.trueKleene);

            Set<Predicate> predicatesToKill = keepBinary ? substruct.getVocabulary().unary() : substruct
                    .getVocabulary().positiveArity();
            setOutsideInformation(substruct, outside, predicatesToKill, Kleene.falseKleene);
        }
        return outside;
    }

    /**
     * In every structure, change all the nodes for which outside is
     * previousValue to newValue
     */
    protected Iterable<HighLevelTVS> changeOutside(final Iterable<HighLevelTVS> structures, final Kleene previousValue,
            final Kleene newValue, final boolean keepOriginal) {
        return new ApplyIterable<HighLevelTVS>(structures, new Apply<HighLevelTVS>() {
            public HighLevelTVS apply(HighLevelTVS structure) {
                HighLevelTVS orig = structure;
                structure = structure.copy();
                if (keepOriginal) {
                    structure.setOriginalStructure(orig);
                }
                Set<Node> outsides = HashSetFactory.make(0);
                Iterator<Entry<NodeTuple, Kleene>> iterator = structure.predicateSatisfyingNodeTuples(
                        Vocabulary.outside, EMPTY_NODE_ARRAY, previousValue);
                while (iterator.hasNext()) {
                    Node outside = (Node) iterator.next().getKey();
                    outsides.add(outside);
                }
                for (Node outside : outsides) {
                    structure.update(Vocabulary.outside, outside, newValue);
                }                
                return structure;
            }
        });
    }

    /**
     * Prepare the given TVSSet to be composed s.t. it's new name will be
     * newName
     * 
     * @return The prepared set
     */
    public Iterable<HighLevelTVS> prepareForComposition(Iterable<HighLevelTVS> set, final DecompositionName newName,
            DynamicVocabulary newVoc) {
        // Set outside predicate to be compatible
        Iterable<HighLevelTVS> result = changeOutside(set, Kleene.trueKleene, Kleene.unknownKleene, false);
        return new ApplyIterable<HighLevelTVS>(result, new Apply<HighLevelTVS>() {
            public HighLevelTVS apply(HighLevelTVS structure) {
                mark(structure, newName);
                return structure;
            }
        });
    }

    public HighLevelTVS prepareForAction(HighLevelTVS structure, DecompositionName name, DynamicVocabulary extraVoc) {
        HighLevelTVS orig = structure;
        structure = structure.copy();

        if (coercedBeforeAction) {
            structure.setOriginalStructure(orig);
        }

        Node outside = getOutsideNode(structure);
        if (outside != null) {
            Set<Predicate> toUnknown;
            if (merge) {
                if (keepBinary) {
                    toUnknown = adjustVocabularyToKill(structure.getVocabulary(), name).unary();
                } else {
                    toUnknown = Collections.emptySet();
                }
            } else {
                toUnknown = adjustVocabularyToKill(structure.getVocabulary(), name).positiveArity();
            }
            setOutsideInformation(structure, outside, toUnknown, Kleene.unknownKleene);
        }
        return structure;
    }

    /**
     * Remove from the vocabulary that should be killed on the outside node any
     * information we still retain for this decomposition name.
     */
    protected DynamicVocabulary adjustVocabularyToKill(DynamicVocabulary vocabulary, DecompositionName name) {
        return vocabulary;
    }

    public boolean isOutsideBinaryPrecise() {
        return merge;
    }

    public boolean isOutsideUnaryPrecise() {
        return merge & !keepBinary;
    }    
    
    public Node getOutsideNode(HighLevelTVS structure) {
        Iterator<Entry<NodeTuple, Kleene>> iterator = structure.predicateSatisfyingNodeTuples(Vocabulary.outside,
                EMPTY_NODE_ARRAY, Kleene.trueKleene);
        Node outside = iterator.hasNext() ? (Node) iterator.next().getKey() : null;
        return outside;
    }

    /**
     * Prepare the given TVSSet to be decomposed
     * 
     * @return The prepared set
     */
    public Iterable<HighLevelTVS> prepareForDecomposition(TVSSet set) {
        // Set outside predicate to be compatible
        return changeOutside(set, Kleene.trueKleene, Kleene.falseKleene, false);
    }

    /**
     * Cleanup a composed set from any intermediate changes
     * 
     * @return The cleaned set
     */
    public Iterable<HighLevelTVS> afterComposition(Iterable<HighLevelTVS> composed) {
        // Return to the normal outside mark
        Iterable<HighLevelTVS> result = changeOutside(composed, Kleene.unknownKleene, Kleene.trueKleene, false);
        return new ApplyIterable<HighLevelTVS>(result, new Apply<HighLevelTVS>() {        
            public HighLevelTVS apply(HighLevelTVS structure) {
                if (mustOutside) {
                    if (structure.numberSatisfy(Vocabulary.outside) == 0) {
                        return null;
                    }
                }

                HighLevelTVS backup = printComposeConstraintBreach ? structure.copy() : null;
                ModifiedPredicates.modify(structure);
                Timer timer = Timer.getTimer("Coerce", "afterComposition");
                timer.start();
                boolean valid = structure.coerce();
                timer.stop();
                if (!valid) {
                    AnalysisStatus.getActiveStatus().numberOfComposeConstraintBreaches++;
                    if (printComposeConstraintBreach) {
                        IOFacade.instance().printStructure(backup, "After Composition");
                        ModifiedPredicates.modify(backup);
                        if (!AnalysisStatus.debug) { // Print the constraint
                            // breach.
                            Location currentLocation = (Location) Engine.getCurrentLocation();
                            boolean savedCurLocPrint = currentLocation.setShouldPrint(true);
                            AnalysisStatus.debug = true;
                            backup.coerce();
                            AnalysisStatus.debug = false; // Restore the old value.
                            currentLocation.setShouldPrint(savedCurLocPrint);
                        }
                    }
                    return null;
                }
                return structure;
            }
        });        
    }

    /**
     * Get the mark for the given name
     */
    public abstract Predicate getMark(DecompositionName name);

    /**
     * Mark the given structure using the given name
     * 
     * @param structure
     * @param decompName
     */
    protected abstract void mark(HighLevelTVS structure, DecompositionName decompName);

    /**
     * Get the instance of the decomposer
     */
    public static Decomposer getInstance() {
        if (instance == null) {
            // TODO Consider adding configuration for controlling this
            instance = new OverlapDecomposer();
        }
        return instance;
    }

    public abstract boolean isBase(DecompositionName key);

    public abstract boolean isAbstraction(DecompositionName key);

    public abstract boolean isComposed(DecompositionName key);

    protected Node addOutsideNode(Set<Node> universe, HighLevelTVS substruct) {
        Node outside;
        if (merge) {
            outside = addOutsideByMerge(substruct, universe);
        } else {
            outside = addOutsideByProjection(substruct, universe);
        }
        return outside;
    }

    protected void setAll(HighLevelTVS structure, Set<Predicate> predicates, Kleene value) {
        for (Predicate predicate : predicates) {
            if (ignorePredicates.contains(predicate))
                continue;
            if (predicate == Vocabulary.outside)
                continue;
            if (value == Kleene.falseKleene) {
                structure.clearPredicate(predicate);
            } else {
                for (Node node : structure.nodes()) {
                    structure.update(predicate, node, value);
                }
            }
        }
    }

    public abstract Set<DecompositionName> permute(DecompositionName decompName);

    public abstract Collection<Pair<DecompositionName, TVSSet>> permute(Collection<? extends DecompositionName> name,
            TVSSet structures);

    public abstract Iterable<TVSSet> permute(DecompositionName target, DecompositionName source, TVSSet structures);

    public static Set<DecompositionName> toComposedDecompositionNames(Set<Set<DecompositionName>> names) {
        Set<DecompositionName> composedNames = HashSetFactory.make();
        for (Set<DecompositionName> composedNameComponents : names) {
            DecompositionName composedName = compose(composedNameComponents);
            composedNames.add(composedName);
        }
        return composedNames;
    }

    public static DecompositionName compose(Collection<? extends DecompositionName> components) {
        DecompositionName composedName = null;
        for (DecompositionName name : components) {
            if (composedName == null) {
                composedName = name;
            } else {
                composedName = composedName.compose(name);
            }
        }
        return composedName;
    }

    public static Set<Set<DecompositionName>> toDecompositionNames(Formula formula) {
        List<Formula> conjuncts = new ArrayList<Formula>();
        Formula.getAnds(formula, conjuncts);
        Set<Set<DecompositionName>> toComposeDisjunct = HashSetFactory.make();
        for (Var var : formula.freeVars()) {
            Set<? extends DecompositionName> names = null;
            // Find all decomposition names matching this variable in the
            // formula
            for (Formula conjunct : conjuncts) {
                boolean negated = false;
                if (conjunct instanceof NotFormula) {
                    conjunct = ((NotFormula) conjunct).subFormula();
                    negated = true;
                }
                DynamicVocabulary actionVoc = null;
                DynamicVocabulary actionKillVoc = null;
                if (conjunct instanceof EquivalenceFormula) {
                    EquivalenceFormula equiv = (EquivalenceFormula) conjunct;
                    assert !negated;
                    conjunct = equiv.left();
                    Formula actionFormula = equiv.right();

                    Pair<DynamicVocabulary, DynamicVocabulary> actionVocs = OverlapDecomposer
                            .getDecompositionAndKillPredicates(actionFormula);
                    actionVoc = actionVocs.first;
                    actionKillVoc = actionVocs.second;
                }
                assert conjunct instanceof PredicateFormula;
                PredicateFormula pformula = (PredicateFormula) conjunct;
                Predicate predicate = pformula.predicate();
                assert predicate.arity() == 1;
                if (var == pformula.variables()[0]) {
                    // Found a match, find all decomposition names with
                    // predicate
                    Set<? extends DecompositionName> match = getInstance().match(predicate, negated, actionVoc,
                            actionKillVoc);
                    if (names == null) {
                        names = match;
                    } else {
                        names.retainAll(match);
                    }
                }
            }
            if (names.size() > 1) {
                throw new SemanticErrorException("Decomposition Name ambigious matched " + names.size() + " names. In "
                        + formula + " for " + var + ". Matched " + names);
            }
            if (toComposeDisjunct.isEmpty()) {
                for (DecompositionName name : names) {
                    Set<DecompositionName> tuple = HashSetFactory.make();
                    tuple.add(name);
                    toComposeDisjunct.add(tuple);
                }
            } else {
                Set<Set<DecompositionName>> toComposeDisjunctNew = HashSetFactory.make();
                for (Set<DecompositionName> tuple : toComposeDisjunct) {
                    for (DecompositionName name : names) {
                        Set<DecompositionName> newTuple = HashSetFactory.make(tuple);
                        newTuple.add(name);
                        toComposeDisjunctNew.add(newTuple);
                    }
                }
                toComposeDisjunct = toComposeDisjunctNew;
            }
        }
        return toComposeDisjunct;
    }

    public static Set<? extends DecompositionName> getMatchingNames(Formula componentName) {
        List<Formula> conjuncts = new ArrayList<Formula>();
        Formula.getAnds(componentName, conjuncts);
        Set<? extends DecompositionName> names = null;
        // Find all decomposition names matching this variable in the
        // formula
        for (Formula conjunct : conjuncts) {
            assert conjunct instanceof PredicateFormula;
            PredicateFormula pformula = (PredicateFormula) conjunct;
            Predicate predicate = pformula.predicate();
            Set<? extends DecompositionName> match = getInstance().match(predicate, false, null, null);
            if (names == null) {
                names = match;
            } else {
                names.retainAll(match);
            }
        }
        return names;
    }

    public abstract DynamicVocabulary getVocabulary(DecompositionName name);

    public DecompositionName getParametricRepresentative(DecompositionName name) {
        // TODO Hack
        Set<DecompositionName> permute = Decomposer.getInstance().permute(name);
        TreeMap<String, DecompositionName> sorted = new TreeMap<String, DecompositionName>();
        for (DecompositionName candidate : permute) {
            sorted.put(candidate.toString(), candidate);
        }
        return sorted.values().iterator().next();
    }

    public abstract Map<Predicate, PredicateUpdateFormula> getChangeFormulas(Action action);

    public abstract Iterable<Map<Predicate, Predicate>> getPermutation(DecompositionName target,
            DecompositionName source);

}
