package tvla.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import tvla.analysis.AnalysisStatus;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignKleene;
import tvla.core.base.PredicateEvaluator;
import tvla.core.base.PredicateUpdater;
import tvla.core.common.ModifiedPredicates;
import tvla.core.decompose.Decomposer;
import tvla.core.decompose.DecompositionName;
import tvla.exceptions.SemanticErrorException;
import tvla.formulae.AndFormula;
import tvla.formulae.Formula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.PredicateUpdateFormula;
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.predicates.DynamicVocabulary;
import tvla.predicates.Instrumentation;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.transitionSystem.Action;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.MapInverter;
import tvla.util.Pair;
import tvla.util.ProgramProperties;
import tvla.util.Timer;

/**
 * Class to handle removing and re-adding the frame of structures before and after an action.
 */
public class Framer {
    public static boolean enabled = ProgramProperties.getBooleanProperty("tvla.framer.enabled", false);
    public static boolean collapseIsomorphic = ProgramProperties.getBooleanProperty("tvla.framer.collapseIsomorphic", true);
    public static boolean frameWithCanonicalNames = ProgramProperties.getBooleanProperty("tvla.framer.withCanoincalNames", false);
    public static boolean checkSoundness = ProgramProperties.getBooleanProperty("tvla.framer.checkSoundness", true);
    
    protected DynamicVocabulary frameVocabulary;
    protected DynamicVocabulary contextFrameVocabulary;
    protected Collection<Pair<Formula, PredicateFormula>> contextualFrameConditions = new ArrayList<Pair<Formula,PredicateFormula>>();
    protected DynamicVocabulary updatedVocabulary;
    protected Map<Predicate, PredicateUpdateFormula> changeFormulas;
    protected Action action;
    protected DynamicVocabulary updatedFrameVoc;

    protected static Predicate[] markPredicates = new Predicate[10];
    protected static int maxMark = (1 << markPredicates.length) - 1;
    protected static Random random = new Random();
    public static DynamicVocabulary markPredicatesVoc = DynamicVocabulary.empty();
    static {
        if (frameWithCanonicalNames) {
            Set<Predicate> set = HashSetFactory.make();
            for (int i = 0; i < markPredicates.length; i++) {
                markPredicates[i] = Vocabulary.createPredicate("__frame_" + i, 1, true);
                markPredicates[i].setShowAttr(false, true, false);
                set.add(markPredicates[i]);
            }
            markPredicatesVoc = DynamicVocabulary.create(set);
        }
    }
    
    /**
     * Build a framer from the given specification formula.
     * The updatedVocabulary specifies the predicates that can change between frame and unframe and
     * thus can't be copied from the original.
     */
    public Framer(Formula frameFormula, Action action) {
        if (!enabled) return;
        AnalysisStatus.getActiveStatus().startTimer(
                AnalysisStatus.FRAME_TIME);
        this.updatedVocabulary = action.getUpdatedVocabulary();
        this.changeFormulas = Decomposer.getInstance().getChangeFormulas(action);
        this.action = action;
        
        List<Formula> disjuncts = new ArrayList<Formula>();
        Formula.getOrs(frameFormula, disjuncts);
        Set<Predicate> framePredicates = HashSetFactory.make();
        Set<Predicate> contextFramePredicates = HashSetFactory.make();
        
        for (Formula disjunct : disjuncts) {
            if (disjunct instanceof PredicateFormula) {
                Predicate predicate = ((PredicateFormula) disjunct).predicate();
                framePredicates.add(predicate);
            } else {
                assert disjunct instanceof AndFormula;
                AndFormula implies = (AndFormula) disjunct;
                Formula contextFormula = implies.left();
                Formula rightFormula = implies.right();
                assert rightFormula instanceof PredicateFormula;
                PredicateFormula predicateFormula = (PredicateFormula) rightFormula;
                Predicate predicate = ((PredicateFormula) predicateFormula).predicate();
                PredicateUpdateFormula changeFormula = changeFormulas.get(predicate);
                if (changeFormula != null && predicate.arity() > 0) {
                    // Rename variable according to changeFormula
                    Map<Var, Var> renaming = HashMapFactory.make();
                    for (int i = 0; i < predicate.arity(); i++) {
                        renaming.put(predicateFormula.getVariable(i), changeFormula.variables[i]);
                    }
                    contextFormula.substituteVars(renaming);
                    predicateFormula.substituteVars(renaming);
                }
                contextFramePredicates.add(predicate);
                contextualFrameConditions.add(Pair.create(contextFormula, predicateFormula));
            }
        }
        frameVocabulary = DynamicVocabulary.create(framePredicates);
        contextFrameVocabulary = DynamicVocabulary.create(contextFramePredicates);
        DynamicVocabulary both = frameVocabulary.intersection(contextFrameVocabulary);
        if (!both.all().isEmpty()) {
            throw new SemanticErrorException("Some predicates are both contextual and non-contextual frame: " + both);
        }
        updatedFrameVoc = updatedVocabulary.intersection(frameVocabulary.union(contextFrameVocabulary));
        AnalysisStatus.getActiveStatus().stopTimer(
                AnalysisStatus.FRAME_TIME);
    }
    
    /**
     * Copy constructor
     */
    public Framer(Framer framer) {
        this.frameVocabulary = framer.frameVocabulary;
        this.contextualFrameConditions = new ArrayList<Pair<Formula,PredicateFormula>>(framer.contextualFrameConditions);
        this.updatedFrameVoc = framer.updatedFrameVoc;
    }

    /**
     * Remove frame from given structures. Returning one representative from each isomorphism equivalence class.
     * The returned structures are annotated with the original structure and mark them as originating from the given component.
     */
    public Iterable<HighLevelTVS> frame(Iterable<HighLevelTVS> structures, DecompositionName component) {
        if (!enabled) return structures;
        AnalysisStatus.getActiveStatus().startTimer(
                AnalysisStatus.FRAME_TIME);
        
        Collection<HighLevelTVS> strippedStructures = new ArrayList<HighLevelTVS>();
        // Strip the structures and record the originals.
        int count = 0;
        for (HighLevelTVS structure : structures) {
            HighLevelTVS stripped = strip(structure);
            StructureGroup group = new StructureGroup(stripped);
            group.addMember(structure, StructureGroup.Member.buildIdentityMapping(structure), component);
            stripped.setStructureGroup(group);
            strippedStructures.add(stripped);
            count++;
        }
        
        if (!collapseIsomorphic) {
            AnalysisStatus.getActiveStatus().stopTimer(
                    AnalysisStatus.FRAME_TIME);
            return strippedStructures;
        }
        
        // Compute equivalence class according to isomorphism and return a representative structure from each
        // equivalence class.
        Collection<StructureGroup> eqClasses = IsomorphismEquivalenceClassCreator.compute(strippedStructures);
        TVSSet result = TVSFactory.getInstance().makeEmptySet(TVSFactory.JOIN_CONCRETE);
        for (StructureGroup eqClass : eqClasses) {
            eqClass.getRepresentative().setStructureGroup(eqClass);
            result.mergeWith(eqClass.getRepresentative());
        }
        // Mark structures with uninterpreted unary predicates to make sure they have
        // canoincal names (i.e. that blur(S) = S)
        for (StructureGroup eqClass : eqClasses) {
            mark(eqClass.getRepresentative());
        }
        
        
//        System.err.println("Frame " + count + " => " + result.size());
        AnalysisStatus.getActiveStatus().stopTimer(
                AnalysisStatus.FRAME_TIME);
        return result;
    }

    
    static int counter = 0;
    protected void mark(HighLevelTVS structure) {
        if (!frameWithCanonicalNames) return;
        DynamicVocabulary vocabulary = structure.getVocabulary();
        if (vocabulary.contains(markPredicates[0])) {
            // Already marked by previous framer. Remark
            for (Predicate predicate : markPredicatesVoc.all()) {
                structure.clearPredicate(predicate);
            }
        } else {
            vocabulary = vocabulary.union(markPredicatesVoc);
            structure.updateVocabulary(vocabulary, Kleene.falseKleene);
        }
        Set<Integer> used = HashSetFactory.make();
        for (Node node : structure.nodes()) {
            while(!used.add(counter)) {
                counter = random.nextInt(maxMark+1);
            }

            // Use binary encoding of counter to make sure
            // all nodes have distinct canonical names.
            for (int pos = 0; (1 << pos) <= counter; pos++) {
                if ((counter & (1<<pos)) != 0) {
                    Predicate predicate = markPredicates[pos];
                    structure.update(predicate, node, Kleene.trueKleene);
                }
            }
        }
        return;
    }

    protected void unmark(HighLevelTVS representative) {
        if (!frameWithCanonicalNames) return;
        // Nothing to do. Unframing the frame vocabulary will do the job. 
    }
    
    /**
     * Re-insert the frame for each structure according to the originals kept in its structure group that
     * match the given component name
     */
    public TVSSet unframe(HighLevelTVS structure, DecompositionName name) {
        if (!enabled) {
            TVSSet result = TVSFactory.getInstance().makeEmptySet(TVSFactory.JOIN_CONCRETE);
            result.mergeWith(structure);
            return result;
        }
        AnalysisStatus.getActiveStatus().startTimer(
                AnalysisStatus.FRAME_TIME);

        
        TVSSet result = TVSFactory.getInstance().makeEmptySet(TVSFactory.JOIN_CONCRETE);
        StructureGroup structureGroup = structure.getStructureGroup();
        for (StructureGroup.Member member : structureGroup.getMembers()) {
            HighLevelTVS merged = structure.copy();            
            if (name == null || member.getComponent() == null || name == member.getComponent()) {
                unmark(merged);
                merged.setStructureGroup(null);
                merged.setOriginalStructure(structure);
                Map<Node, Node> mapping = member.getMapping();
                HighLevelTVS origin = member.getStructure();
                if (!unframeFrameVocabulary(merged, origin, structure, mapping)) {
                    continue;
                }
                unframeContextual(merged, origin, mapping);
                // Reevaluated the instrumentation predicates that have changed                    
                // TODO should be done in an order which ensured dependencies are evaluated first.
                for (Predicate predicate : updatedFrameVoc.all()) {
                    if (shouldReevaluate(predicate)) {
                        reevaluate(merged, predicate);
                    }
                }
                if (Decomposer.getInstance().coercedBeforeAction) {
                    Timer timer = Timer.getTimer("Coerce", "unframe");
                    timer.start();
                    boolean valid = merged.coerce();
                    timer.stop();
                    if (!valid) {
                        AnalysisStatus.getActiveStatus().numberOfComposeConstraintBreaches++;
                        continue;
                    }                
                }
                result.mergeWith(merged);
            }
        }
        assert !result.isEmpty();
        AnalysisStatus.getActiveStatus().stopTimer(
                AnalysisStatus.FRAME_TIME);
        return result;
    }

    /**
     * Get the vocabulary expected after the framing.
     */
    public DynamicVocabulary getFramedVocabulary(DynamicVocabulary vocabulary) {
        return enabled ? vocabulary.subtract(frameVocabulary).union(markPredicatesVoc) : vocabulary;
    }

    /**
     * Compose two framer (usually so they can be unframed simultaneously.
     */
    public static Framer compose(Framer first, Framer second) {
        if (!enabled) return null;
        if (first == null) return second;
        if (second == null) return first;
        Framer result = new Framer(first);
        result.frameVocabulary = first.frameVocabulary.union(second.frameVocabulary);
        result.contextFrameVocabulary = first.contextFrameVocabulary.union(second.contextFrameVocabulary);
        result.contextualFrameConditions.addAll(second.contextualFrameConditions);
        result.updatedFrameVoc = result.updatedVocabulary.intersection(result.frameVocabulary.union(result.contextFrameVocabulary));
        return result;
    }    
    
    /**
     * Unframe contextual frame conditions, i.e., a frame formula specifies which 
     * predicate and a context formula specifies which part of the structure is part of frame.  
     * @param structure Structure to be updated
     * @param origin The original structure to take the frame from
     * @param mapping Mapping the nodes of the new structure to nodes of the original structure.
     */
    private void unframeContextual(HighLevelTVS structure, HighLevelTVS origin,
            Map<Node, Node> mapping) {
        for(Pair<Formula, PredicateFormula> contextualFrameCondition : contextualFrameConditions) {
            Formula contextFormula = contextualFrameCondition.first;
            PredicateFormula frameFormula = contextualFrameCondition.second;
            
            Predicate updatedPredicate = frameFormula.predicate();
            if (shouldReevaluate(updatedPredicate)) {
                throw new SemanticErrorException("Reevalution of contextual frames is unsupported " + contextualFrameCondition);
            }
            
            PredicateUpdater updater = PredicateUpdater.updater(updatedPredicate, structure);
            PredicateEvaluator evaluator = PredicateEvaluator.evaluator(updatedPredicate, origin);                        
            
            Set<Var> additionalVars = HashSetFactory.make(frameFormula.freeVars());
            additionalVars.removeAll(contextFormula.freeVars());

            // For each assignment matching the context formula
            Iterator<AssignKleene> contextIter = structure.evalFormula(contextFormula, Assign.EMPTY);
            while (contextIter.hasNext()) {
                AssignKleene contextAssign = contextIter.next();
                if (contextAssign.kleene == Kleene.unknownKleene) {
                    throw new SemanticErrorException("Can't use predicate with 1/2 for context in frame for structure " + structure);
                }
                // Go over all assignments to the frame formula that match the current context assignment
                Iterator<Assign> assignIter = Assign.getAllAssign(structure.nodes(), additionalVars, contextAssign);
                while (assignIter.hasNext()) {
                    Assign assign = assignIter.next();
                    // Copy the tuple matching this assignment from the matching tuple in the original structure.
                    NodeTuple tuple = frameFormula.makeTuple(assign);
                    NodeTuple originTuple = tuple.mapNodeTuple(mapping);                                
                    updater.update(tuple, evaluator.eval(originTuple));
                }
            }
        }
    }
    
    /**
     * Unframe an entire predicate simultaneously
     * @param structure Structure to be updated
     * @param origin The original structure to take the frame from
     * @param mapping Mapping the nodes of the new structure to nodes of the original structure.
     */
    private boolean unframeFrameVocabulary(HighLevelTVS structure,
            HighLevelTVS origin, HighLevelTVS representative, Map<Node, Node> mapping) {
        // Revive the original vocabulary
        DynamicVocabulary newVoc = structure.getVocabulary().subtract(markPredicatesVoc).union(frameVocabulary);

        // Consider predicate which were removed by frame and re-added by meet
        // For such predicates we want to meet the previous values with the new values.
        DynamicVocabulary readdedVoc = structure.getVocabulary().intersection(frameVocabulary);
        DynamicVocabulary deltaVoc = frameVocabulary.subtract(readdedVoc);
        
        structure.updateVocabulary(newVoc, Kleene.falseKleene);
        
        // Copy nullary predicates (except for reevaluation)
        for (Predicate predicate : frameVocabulary.nullary()) {
            if (shouldReevaluate(predicate)) {
                continue;
            }
            structure.update(predicate, origin.eval(predicate));
            ModifiedPredicates.modify(structure, predicate);
        }
        
        // Meet readded predicates. (Not sound...)
        if (!unframePositiveArityFramePredicates(structure, origin, mapping, readdedVoc)) {
            return false;
        }
        
        // Build inverse node mapping
        Map<Node,Set<Node>> inverse = HashMapFactory.make();
        MapInverter.invertMapNonInjective(mapping, inverse);
        
        // Copy all predicate values from the origin tuple 
        for (Predicate predicate : deltaVoc.positiveArity()) {
            if (shouldReevaluate(predicate)) {
                continue;
            }
            structure.clearPredicate(predicate);
            PredicateUpdater updater = PredicateUpdater.updater(predicate, structure);
            
            // For each 1 or 1/2 tuple in the origin structure
            for (Iterator<Entry<NodeTuple, Kleene>> iterator = origin.iterator(predicate); iterator.hasNext(); ) {
                Entry<NodeTuple, Kleene> entry = iterator.next();
                NodeTuple originTuple = entry.getKey(); // The origin tuple
                Kleene value = entry.getValue(); // The value for the origin tuple
                // Find all of the structure's tuples that matching the origin tuple
                for (Iterator<? extends NodeTuple> matchIter = originTuple.matchingTuples(inverse); matchIter.hasNext(); ) {
                    NodeTuple tuple = matchIter.next();
                    updater.update(tuple, value);
                }
            }
            ModifiedPredicates.modify(structure, predicate);
        }
        return true;
    }

    /**
     * unframe positive arity frame predicates by copying matching origin tuple's
     * value. Replaced by code which goes over satisfying origin tuples. 
     */
    private boolean unframePositiveArityFramePredicates(HighLevelTVS structure,
            HighLevelTVS origin, Map<Node, Node> mapping, DynamicVocabulary vocabulary) {
        /*
           for (Predicate predicate : vocabulary.positiveArity()) {
                if (shouldReevaluate(predicate)) {
                    continue;
                }
                PredicateUpdater updater = PredicateUpdater.updater(predicate, structure);
                PredicateEvaluator eval = PredicateEvaluator.evaluator(predicate, origin);
                // Find all 1/2 values of structure and copy values from original
                for (Iterator<Entry<NodeTuple, Kleene>> iterator = representative.iterator(predicate); iterator.hasNext(); ) {
                    Entry<NodeTuple, Kleene> entry = iterator.next();
                    if (entry.getValue() != Kleene.unknownKleene) continue;
                    NodeTuple tuple = entry.getKey(); // The origin tuple
                    NodeTuple originTuple = tuple.mapNodeTuple(mapping);
                    updater.update(tuple, eval.eval(originTuple));
                }
                ModifiedPredicates.modify(structure, predicate);
            }
        */
        // Copy unary predicates (except for reevaluation). Use mapping to retrieve the original node name
        for (Predicate predicate : vocabulary.unary()) {
            if (shouldReevaluate(predicate)) {
                continue;
            }
            PredicateUpdater updater = PredicateUpdater.updater(predicate, structure);
            PredicateEvaluator origEvaluator = PredicateEvaluator.evaluator(predicate, origin);
            PredicateEvaluator structureEvaluator = PredicateEvaluator.evaluator(predicate, structure);
            for (Node newNode : structure.nodes()) {
                Node originNode = mapping.get(newNode);
                Kleene result = Kleene.meet2(origEvaluator.eval(originNode),structureEvaluator.eval(newNode));
                if (result == null)
                    return false;
                updater.update(newNode, result);                            
            }
        }
        // Copy binary predicates (except for reevaluation). Use mapping to retrieve the original node name
        for (Predicate predicate : vocabulary.binary()) {
            if (shouldReevaluate(predicate)) {
                continue;
            }
            PredicateUpdater updater = PredicateUpdater.updater(predicate, structure);
            PredicateEvaluator origEvaluator = PredicateEvaluator.evaluator(predicate, origin);
            PredicateEvaluator structureEvaluator = PredicateEvaluator.evaluator(predicate, structure);
            for (Node newNodeLeft : structure.nodes()) {
                Node originNodeLeft = mapping.get(newNodeLeft);
                for (Node newNodeRight : structure.nodes()) {
                    Node originNodeRight = mapping.get(newNodeRight);
                    Kleene result = Kleene.meet2(origEvaluator.eval(originNodeLeft, originNodeRight),structureEvaluator.eval(newNodeLeft, newNodeRight));
                    if (result == null)
                        return false;
                    updater.update(newNodeLeft, newNodeRight, result);
                }
            }
        }
        return true;
    }


    /**
     * Reevaluate given instrumentation predicate.
     * TODO In case of contextual frames - Only reevaluate the relevant part.
     */
    private void reevaluate(HighLevelTVS structure, Predicate predicate) {
        assert predicate instanceof Instrumentation;
        Formula definition = ((Instrumentation) predicate).getFormula();
        List<Var> vars = ((Instrumentation) predicate).getVars();
        Iterator<AssignKleene> defIter = structure.evalFormula(definition, Assign.EMPTY);
        structure.clearPredicate(predicate);
        PredicateUpdater updater = PredicateUpdater.updater(predicate, structure);
        Node[] nodes = new Node[vars.size()];
        while (defIter.hasNext()) {
            AssignKleene assign = defIter.next();            
            if (predicate.arity() == 0) {
                structure.update(predicate, assign.kleene);
            } else {
                for(int i = 0; i < nodes.length; i++) {
                    nodes[i] = assign.get(vars.get(i));
                }
                updater.update(NodeTuple.createTuple(nodes), assign.kleene);
            }
        }
    }
    
    /**
     * Should a predicate be reevaluated?
     */
    private boolean shouldReevaluate(Predicate predicate) {
        if (!updatedVocabulary.all().contains(predicate)) {
            // Not updated - no need for reevaluation
            return false;
        }
        if (predicate instanceof Instrumentation) {
            // Updated instrumentation - reevaluate
            return true;
        }
        return false;
    }

    /**
     * Strip the structure from the frame - remove whole predicates from vocabulary 
     * and set to 1/2 predicates according to their context formulas.
     */
    private HighLevelTVS strip(HighLevelTVS structure) {
        if (!frameVocabulary.union(contextFrameVocabulary).subsetof(structure.getVocabulary())) {
            throw new SemanticErrorException("Frame should be subset of the structure vocabulary" +
                    ", but these are extra " + (frameVocabulary.union(contextFrameVocabulary).subtract(structure.getVocabulary())));
        }
        
        HighLevelTVS result = structure.copy();
        
        checkSoundness(structure);
        
        for(Pair<Formula, PredicateFormula> contextualFrameCondition : contextualFrameConditions) {
            Formula contextFormula = contextualFrameCondition.first;
            PredicateFormula frameFormula = contextualFrameCondition.second;
            PredicateUpdater updater = PredicateUpdater.updater(frameFormula.predicate(), result);
            Iterator<AssignKleene> contextIter = result.evalFormula(contextFormula, Assign.EMPTY);
            while (contextIter.hasNext()) {
                AssignKleene contextAssign = contextIter.next();
                if (contextAssign.kleene == Kleene.unknownKleene) {
                    throw new SemanticErrorException("Can't use predicate with 1/2 for context in frame for structure " + structure);
                }
                Set<Var> freeVars = HashSetFactory.make(frameFormula.freeVars());
                freeVars.removeAll(contextAssign.bound());
                Iterator<Assign> assignIter = Assign.getAllAssign(result.nodes(), freeVars, contextAssign);
                while (assignIter.hasNext()) {
                    Assign assign = assignIter.next();
                    NodeTuple tuple = frameFormula.makeTuple(assign);
                    updater.update(tuple, Kleene.unknownKleene);
                }
            }
        }
        
        DynamicVocabulary newVocabulary = structure.getVocabulary().subtract(frameVocabulary);
        result.updateVocabulary(newVocabulary);        
        return result;
    }

    protected void checkSoundness(HighLevelTVS structure) {
        if (!checkSoundness) return;
        for (Predicate predicate : frameVocabulary.all()) {
            PredicateUpdateFormula changeFormula = changeFormulas.get(predicate);
            if (changeFormula != null) {
                if (structure.evalFormula(changeFormula.getFormula(), Assign.EMPTY).hasNext()) {
                    throw new SemanticErrorException("Unsound frame for predicate " + predicate + " in action " + action);
                }
            }
        }
        for(Pair<Formula, PredicateFormula> contextualFrameCondition : contextualFrameConditions) {
            Predicate predicate = contextualFrameCondition.second.predicate();
            PredicateUpdateFormula changeFormula = changeFormulas.get(predicate);
            if (changeFormula != null) {
                if (structure.evalFormula(new AndFormula(contextualFrameCondition.first, changeFormula.getFormula()), Assign.EMPTY).hasNext()) {
                    throw new SemanticErrorException("Unsound frame for predicate " + predicate + " in action " + action);
                }
            }            
        }
    }

    public HighLevelTVS prepareUnframe(DecompositionName targetName, HighLevelTVS structure) {
        if (!enabled || !frameWithCanonicalNames) return structure;
        HighLevelTVS result = structure.copy();
        DynamicVocabulary newVoc = Decomposer.getInstance().getVocabulary(targetName);
        newVoc = newVoc.subtract(frameVocabulary);
        newVoc = newVoc.union(markPredicatesVoc);
        result.updateVocabulary(newVoc);
        return result;
    }
}
