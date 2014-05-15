package tvla.analysis.decompose;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import tvla.core.Framer;
import tvla.core.HighLevelTVS;
import tvla.core.TVSFactory;
import tvla.core.TVSSet;
import tvla.core.decompose.CartesianElement;
import tvla.core.decompose.Decomposer;
import tvla.core.decompose.DecompositionName;
import tvla.core.generic.GenericSingleTVSSet;
import tvla.exceptions.SemanticErrorException;
import tvla.formulae.EquivalenceFormula;
import tvla.formulae.Formula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.ValueFormula;
import tvla.logic.Kleene;
import tvla.predicates.Instrumentation;
import tvla.predicates.Predicate;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.ProgramProperties;

public class MeetDecompositionStrategy implements DecompositionStrategy {
    protected static final boolean abstractBeforeUnframe = 
        ProgramProperties.getBooleanProperty("tvla.decompose.abstractBeforeUnframe", false) &&
        Framer.frameWithCanonicalNames &&
        TVSFactory.getInstance().makeEmptySet() instanceof GenericSingleTVSSet; // Make sure join supports StructureGroup

    public static boolean focusDecomposeNames = ProgramProperties.getBooleanProperty("tvla.decompose.focusnames", true);

    protected final boolean incremental;

    protected Map<DecompositionName, HighLevelTVS> mustDecompose = null;

    private Formula decomposeFormula;
    
    public MeetDecompositionStrategy(Formula decomposeFormula, boolean incremental) {
        this.incremental = incremental;
        this.decomposeFormula = decomposeFormula;
    }

    private HighLevelTVS checkMinimalPrecision(DecompositionName name, TVSSet set) {        
        for (HighLevelTVS structure : set) {
            if (!name.canDecompose(structure, false)) {
                return structure;
            }
        }
        return null;
    }
    
    public void decompose(DecompositionName sourceName, DecompositionName targetName, Iterable<HighLevelTVS> composed,
            CartesianElement decomposed, Framer framer) {
        boolean self = sourceName.contains(targetName);
        if (!self) return;
        if (framer == null) {
            decomposeOrig(sourceName, targetName, composed, decomposed, framer);
            return;
        }
        if (abstractBeforeUnframe) {
            for (HighLevelTVS structure : composed) {
                decomposed.join(sourceName, framer.prepareUnframe(targetName, structure));
            }            
        } else {
            decomposeFrame(sourceName, targetName, composed, decomposed, framer);
        }
    }

    protected void decomposeFrame(DecompositionName sourceName, DecompositionName targetName,
            Iterable<HighLevelTVS> composed, CartesianElement decomposed, Framer framer) {
        boolean self = sourceName.contains(targetName);
        if (!self) return;
        for (HighLevelTVS fstructure : composed) {
            // We used frame - we need to unframe before we decompose.
            TVSSet unframed = framer.unframe(fstructure, targetName);
            HighLevelTVS problematic = checkMinimalPrecision(targetName, unframed);
            if (problematic == null) {
                decomposed.join(Decomposer.getInstance().decompose(unframed, targetName));
            } else if (!decomposed.names().contains(targetName)){
                if (self || !incremental) {
                    // Remember that we must eventually decompose this name
                    mustDecompose.put(targetName, problematic);
                    return;
                }
            }
        }
    }

    public void decomposeOrig(DecompositionName sourceName, DecompositionName targetName, Iterable<HighLevelTVS> inputComposed,
            CartesianElement decomposed, Framer framer) {
        boolean self = sourceName.contains(targetName);
        if (!self) return;
        
        TVSSet composed = TVSFactory.getInstance().makeEmptySet();
        // If we used frame - we need to unframe before we decompose.
        if (framer != null) {
            for (HighLevelTVS fstructure : inputComposed) {
                for (HighLevelTVS structure : framer.unframe(fstructure, targetName)) {
                    composed.mergeWith(structure);
                }
            }
        } else {
            // The original tvsset is can be concrete, abstract before decomposition
            // to save time.
            for (HighLevelTVS structure : inputComposed) {
                composed.mergeWith(structure);
            }           
        }
        HighLevelTVS problematic = checkMinimalPrecision(targetName, composed);
        if (focusDecomposeNames && self && problematic != null) {
            // Focus on name 
            TVSSet focused = TVSFactory.getInstance().makeEmptySet(TVSFactory.JOIN_CONCRETE);
            for (HighLevelTVS structure : Decomposer.getInstance().prepareForDecomposition(composed)) {
                for (HighLevelTVS focusedStructure : structure.focus(targetName.getFormula())) {
                    focused.mergeWith(focusedStructure);
                }
            }
            composed = focused;
            problematic = null;
        }
        if (problematic == null) {
            CartesianElement addition = Decomposer.getInstance().decompose(composed, targetName);            
            decomposed.join(addition);
        } else if (!decomposed.names().contains(targetName)){
            if (self || !incremental) {
                // Remember that we must eventually decompose this name
                mustDecompose.put(targetName, problematic);
            }
        }
    }
    
    private Map<DecompositionName, Set<DecompositionName>> automaticDecomposition(
            Collection<DecompositionName> sourceNames) {
        Map<DecompositionName, Set<DecompositionName>> decomposition = HashMapFactory.make();
        for (DecompositionName targetName : Decomposer.getInstance().names()) {
            Set<DecompositionName> from = HashSetFactory.make();
            for (DecompositionName composedName : sourceNames) {
                boolean self = composedName.contains(targetName);
                if ((!incremental || self) && targetName.canDecomposeFrom(composedName)) {
                    from.add(composedName);
                }
            }
            decomposition.put(targetName, from);
        }
        return decomposition;
    }

    private Map<DecompositionName, Set<DecompositionName>> nullDecomposition(
            Collection<DecompositionName> sourceNames) {
        Map<DecompositionName, Set<DecompositionName>> decomposition = HashMapFactory.make();
        for (DecompositionName name : sourceNames) {
            decomposition.put(name, Collections.singleton(name));
        }
        return decomposition;
    }

    public Map<DecompositionName, Set<DecompositionName>> getDecomposition(Collection<DecompositionName> sourceNames) {
        // Reset mustDecompose
        mustDecompose = HashMapFactory.make();
        
        // Decide on decomposition
        if (decomposeFormula == null) {
            return nullDecomposition(sourceNames);
        }
        if (decomposeFormula instanceof ValueFormula) {
            ValueFormula valueFormula = (ValueFormula) decomposeFormula;
            if (valueFormula.value() == Kleene.falseKleene) {
                return nullDecomposition(sourceNames);
            } else {
                return automaticDecomposition(sourceNames);
            }
        }
        if (decomposeFormula instanceof PredicateFormula) {
            PredicateFormula predicateFormula = (PredicateFormula) decomposeFormula;
            Predicate predicate = predicateFormula.predicate();
            if (predicate instanceof Instrumentation) {
                Instrumentation inst = (Instrumentation) predicate;
                if (inst.getFormula() instanceof ValueFormula) {
                    ValueFormula valueFormula = (ValueFormula) inst.getFormula();
                    if (valueFormula.value() == Kleene.falseKleene) {
                        return nullDecomposition(sourceNames);
                    } else {
                        return automaticDecomposition(sourceNames);                        
                    }
                }
            }
        }
        // Manual decomposition
        // Format |/{ to <-> (src1 | src2 | src3) }
        Map<DecompositionName, Set<DecompositionName>> decomposition = HashMapFactory.make();
        Collection<Formula> componentFormulas = new ArrayList<Formula>();
        Formula.getOrs(decomposeFormula, componentFormulas);
        for (Formula componentFormula : componentFormulas) {
            if (!(componentFormula instanceof EquivalenceFormula)) {
                throw new SemanticErrorException("Decompsition formula - expected iff and got " + componentFormula);
            }
            EquivalenceFormula eqFormula = (EquivalenceFormula) componentFormula;
            Formula targetFormula = eqFormula.left();
            Formula sourceFormula = eqFormula.right();
            Set<? extends DecompositionName> targets = Decomposer.getMatchingNames(targetFormula);
            if (targets.size() != 1) {
                throw new SemanticErrorException("Decomposition formula - Target name should be a singleton, but got " + targets);
            }
            DecompositionName target = targets.iterator().next();
            Collection<Formula> sourceComponentFormulas = new ArrayList<Formula>();
            Formula.getOrs(sourceFormula, sourceComponentFormulas);
            Set<DecompositionName> sources = HashSetFactory.make();
            for (Formula sourceComponentFormula : sourceComponentFormulas) {
                Set<? extends DecompositionName> sourceComponents = Decomposer.getMatchingNames(sourceComponentFormula);
                if (sourceComponents.size() != 1) {
                    throw new SemanticErrorException("Decomposition formula - Each source name should be a singleton, but got " + sourceComponents);
                }
                DecompositionName sourceComponent = sourceComponents.iterator().next();
                for (DecompositionName composedName : sourceNames) {
                    boolean self = composedName.contains(sourceComponent);
                    if ((!incremental || self) && sourceComponent.canDecomposeFrom(composedName)) {
                        sources.add(composedName);
                    }
                }
            }
            decomposition.put(target, sources);
        }
        return decomposition;        
    }

    public void verifyDecomposition() throws DecompositionFailedException {
        if (!mustDecompose.isEmpty()) {
            throw new DecompositionFailedException("Can't decompose for " + mustDecompose.keySet()
                + " undefined predicates in all options. Example problems: " + mustDecompose);
        }
    }

    public void done(DecompositionName targetName, CartesianElement stepDecomposed, CartesianElement decomposed, Framer framer) {        
        if (abstractBeforeUnframe && framer != null) {
            CartesianElement temp = stepDecomposed;
            stepDecomposed = new CartesianElement();
            for (DecompositionName sourceName : temp.names()) {
                decomposeFrame(sourceName, targetName, temp.get(sourceName), stepDecomposed, framer);
            }
        }
        // TODO Find solution for multiple decomposition options
//        if (stepDecomposed.names().contains(targetName)) {
//            mustDecompose.remove(targetName);
//        }
        assert stepDecomposed.names().size() <= 1;
        decomposed.meet(stepDecomposed);
    }
}
