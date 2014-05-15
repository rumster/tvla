package tvla.analysis.decompose;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import tvla.core.HighLevelTVS;
import tvla.core.TVSFactory;
import tvla.core.TVSSet;
import tvla.core.assignments.Assign;
import tvla.core.common.GetFormulaPredicates;
import tvla.core.decompose.Decomposer;
import tvla.core.decompose.DecompositionName;
import tvla.formulae.Formula;
import tvla.formulae.PredicateUpdateFormula;
import tvla.predicates.DynamicVocabulary;
import tvla.predicates.Predicate;
import tvla.transitionSystem.Action;
import tvla.util.ProgramProperties;
import tvla.util.Timer;

public class BasicCompositionFilter implements CompositionFilter {
    public static boolean enableSkipFilter = ProgramProperties.getBooleanProperty("tvla.skipFilter.enable", true);
    
    private Formula filterFormula;
    private Collection<DecompositionName> targetNames;
    private Map<Predicate, PredicateUpdateFormula> changeFormulas;
    private final DecomposeLocation nextLocation;
    private boolean badAction = false;
    public static int nochangeStructures = 0;
    public static int checkedStructures = 0;
    public static int skippedStructures = 0;
    public static Timer totalTime = new Timer();
    public static Timer skipFilterTime = new Timer();

    public BasicCompositionFilter(Formula filterFormula, Action action, DecomposeLocation nextLocation) {
        this.filterFormula = filterFormula;
        this.nextLocation = nextLocation;
        if (enableSkipFilter) {
            if (action.isUniverseChanging()) {
                badAction = true;
            } else {
                changeFormulas = Decomposer.getInstance().getChangeFormulas(action);
            }
        }        
    }

    public TVSSet filter(DecompositionName name, TVSSet beforeSet, boolean singleUse) {
        totalTime.start();
        skipFilterTime.start();
        boolean checkSkip = 
            enableSkipFilter && // Configuration enabled 
            singleUse && // Part of composition, but not used more than once
            targetNames.contains(name) && targetNames.size() == 1 && // Will decompose from it, and nobody else will 
            !badAction; // Not a universe changing action.
        TVSSet target = null;
        Map<Predicate, Predicate> permutation = null;
        // Prepare target and permutation in case skip is even remotely possible
        if (checkSkip) {
            DecompositionName rep = Decomposer.getInstance().getParametricRepresentative(name);            
            target = nextLocation.getElement().get(rep);
            if (target == null) {
                checkSkip = false;
            } else {
                if (rep != name) {
                    Iterator<Map<Predicate, Predicate>> permutationIt = Decomposer.getInstance().getPermutation(rep, name).iterator();
                    assert permutationIt.hasNext();
                    permutation = permutationIt.next();
                    assert !permutationIt.hasNext();
                }
            }
        }                
        skipFilterTime.stop();
        
        DynamicVocabulary filterVoc = DynamicVocabulary.create(GetFormulaPredicates.get(filterFormula));

        // Filter structures not satisfying the filter formula
        // Do the full join in case updated vocabulary or decomp predicates will
        // cause structures to merge
        TVSSet result = TVSFactory.getInstance().makeEmptySet();
        for (HighLevelTVS ostructure : beforeSet) {
            HighLevelTVS structure = Decomposer.getInstance().prepareForAction(ostructure, name, filterVoc);
	    if (structure == null) continue;
            if (structure.getStructureGroup() != null) {
                throw new RuntimeException("Structure group must be null before frame_pre");
            }
            if (!structure.evalFormula(filterFormula, Assign.EMPTY).hasNext()) {
                continue;
            }
            if (checkSkip) {
                skipFilterTime.start();
                checkedStructures++;
                // Check if the action can change this structure
                boolean change = false;
                for (Entry<Predicate, PredicateUpdateFormula> entry : changeFormulas.entrySet()) {
                    Predicate predicate = entry.getKey();
                    if (!ostructure.getVocabulary().contains(predicate)) continue;
                    Formula changeFormula = entry.getValue().getFormula();
                    // Has visible change?
                    if (structure.evalFormula(changeFormula, Assign.EMPTY).hasNext()) {
                        change = true;
                        break;
                    }
                }
                if (!change) {
                    nochangeStructures++;
                    // Must check original unprepared structure
                    HighLevelTVS targetStructure = permutation == null ? ostructure : ostructure.permute(permutation);
                    // This is a filter action if the structure is already in the target structure - skip it!
                    if (target.contains(targetStructure)) {
                        skippedStructures++;
                        skipFilterTime.stop();
                        continue;
                    }
                }
                skipFilterTime.stop();
            }
            structure = structure.copy(); // Don't hurt internal cache
            structure.updateVocabulary(Decomposer.getInstance().getVocabulary(name));                        
            
            result.mergeWith(structure);            
        }
        totalTime.stop();
        return result;
    }

    public void setTargetNames(Collection<DecompositionName> targetNames) {
        this.targetNames = targetNames;
    }
}
