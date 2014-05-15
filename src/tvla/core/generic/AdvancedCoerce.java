package tvla.core.generic;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tvla.analysis.AnalysisStatus;
import tvla.core.Constraints;
import tvla.core.TVS;
import tvla.core.base.BaseTVS;
import tvla.formulae.PredicateFormula;
import tvla.formulae.TransitiveFormula;
import tvla.formulae.Var;
import tvla.formulae.TransitiveFormula.TCCache;
import tvla.io.IOFacade;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.Logger;
import tvla.util.StringUtils;

/**
 * An optimized implementation of Coerce.
 * 
 * @author Tal Lev-Ami.
 * @author Igor Bogudlov
 */
public class AdvancedCoerce extends AdvancedCoerceOld {
    private static final boolean smartTC = true;

    public AdvancedCoerce(Set<Constraints.Constraint> constraints) {
        super(constraints);
        init();
    }

    Collection<Collection<MultiConstraint>> multiComponents = null;

    private void init() {
        // debug = true;

        // connectedComponents =
        // getConnectedComponents(advancedToGeneric.keySet());
        connectedComponents = GraphNode.getConnectedComponents(advancedToGeneric.keySet());
        MultiConstraint.putConstraints(advancedToGeneric.keySet());
        multiComponents = MultiConstraint.getConnectedComponents();
        
        if (debug) {
        	printDependencyListToLog();
        }

        Logger.println("#multi-constraints: " + MultiConstraint.totalSize());

        if (debug) {
            int scc = 0;
            Logger.println(StringUtils.addUnderline("MultiConstraints:"));
            for (Iterator<Collection<MultiConstraint>> it2 = multiComponents.iterator(); it2.hasNext();) {
                Collection<MultiConstraint> component = it2.next();
                Logger.println("-------------------------------------------");
                Logger.println("Component: " + scc);
                scc++;
                for (Iterator<MultiConstraint> it3 = component.iterator(); it3.hasNext();) {
                    MultiConstraint constraint = it3.next();
                    Logger.print(constraint.id + ": " + constraint);
                    Logger.print("    dependents:");
                    for (Iterator<MultiConstraint> depIt = constraint.dependents.iterator(); depIt.hasNext();) {
                        MultiConstraint dependent = depIt.next();
                        Logger.print(" " + dependent.id);
                    }
                    Logger.print("; strong: ");
                    for (Iterator<MultiConstraint> depIt = constraint.strongDependents.iterator(); depIt.hasNext();) {
                        MultiConstraint dependent = depIt.next();
                        Logger.print(" " + dependent.id);
                    }
                    Logger.print("; constraints: ");
                    for (Iterator<AdvancedConstraint> depIt = constraint.advancedConstraints.iterator(); depIt
                            .hasNext();) {
                        AdvancedConstraint ac = depIt.next();
                        Logger.print(" " + ac.id);
                    }
                    Logger.println();
                }
            }
        }

        if (smartTC) {
            // Setup TC calculators.
            for (Map.Entry<Predicate, Collection<TransitiveFormula>> entry : allTC.entrySet()) {
                Predicate predicate = entry.getKey();
                Collection<TransitiveFormula> theTCs = entry.getValue();

                Var[] binaryPredicateVars = { new Var("_v3"), new Var("_v4") };
                PredicateFormula bpFormula = new PredicateFormula(predicate, binaryPredicateVars);
                TransitiveFormula calculator = new TransitiveFormula(new Var("_v1"), new Var("_v2"), new Var("_v3"),
                        new Var("_v4"), bpFormula);

                TCCache calculatedTC = new TCCache();
                calculator.setCalculatedTC(calculatedTC);
                calculatorTC.put(predicate, calculator);
                for (Iterator<TransitiveFormula> j = theTCs.iterator(); j.hasNext();) {
                    TransitiveFormula theTC = j.next();
                    theTC.setCalculatedTC(calculatedTC);
                    theTC.explicitRecalc();
                }
            }
        }
    }

    public static long time_coerceLoop1 = 0;
    public static long time_coerceLoop2 = 0;
    public static long time_coerceLoop3 = 0;
    boolean summaryNodeModified = false;
    static private boolean VerifyCorrectness = false;

    private class CorrectnessVerifier {
        TVS oldS;
        TVS newS;
        TVS newS_copy;

        CorrectnessVerifier(TVS structure) {
            newS = structure;
            if (VerifyCorrectness) {
                newS_copy = structure.copy();
                oldS = structure.copy();
            }
        }

        private void verifyCorrectness(boolean newResult) {
            if (!VerifyCorrectness)
                return;
            boolean oldResult = coerceOld(oldS);
            String msg = null;
            if (oldResult == true && newResult == false) {
                msg = "Invalid constraint detected in the new structure, but not the old!";
            } else if (oldResult == false && newResult == true) {
                msg = "Invalid constraint detected in the old structure, but not the new!";
            } else if (newResult && !oldS.toString().equals(newS.toString())) {
                msg = "Copies don't match!";
            }
            if (msg != null) {
                TVS orig_copy = newS_copy.copy();
                TVS oldS_copy = newS_copy.copy();
                VerifyCorrectness = false;
                debug = true;
                debug2 = true;
                coerce(newS_copy);
                Logger.println("--------- Old coerce: ----------");
                coerceOld(oldS_copy);
                Logger.println("orig structure: " + orig_copy.toString());
                Logger.println("new structure: " + newS_copy.toString());
                Logger.println("old structure: " + oldS_copy.toString());
                throw new RuntimeException(msg);
            }
        }
    }

    public boolean coerceAdvanced(TVS structure) {
        return super.coerce(structure);
    }

    // Current coerce version, using MultiConstraints
    public boolean coerce(TVS structure) {
        long time = System.currentTimeMillis(), time2;

        CorrectnessVerifier verifier = new CorrectnessVerifier(structure);

        Set<Predicate> modifiedPredicates = structure.getModifiedPredicates();
        if (modifiedPredicates.isEmpty()) {
            time_coerce += System.currentTimeMillis() - time;
            verifier.verifyCorrectness(true);
            return true;
        }

        NodeValueMap newChanges, oldChanges, initialMap;

        time2 = System.currentTimeMillis();
        initialMap = structure.getIncrementalUpdates();
        time_coerceGetIncrements += System.currentTimeMillis() - time2;

        modifiedPredicates = structure.getModifiedPredicates();
        if (modifiedPredicates.isEmpty()) {
            time_coerce += System.currentTimeMillis() - time;
            verifier.verifyCorrectness(true);
            return true;
        }

        stat_coerce++;

        boolean checkModified = false;
        BitSet modifiedSet = null;

        time2 = System.currentTimeMillis();
        if (!modifiedPredicates.contains(Vocabulary.active) && ((BaseTVS) structure).getOriginalStructure() != null) {
            modifiedSet = MultiConstraint.predicateToConstraints.getConstraints(modifiedPredicates);
            checkModified = true;
        }
        time_coerceLoop1 += System.currentTimeMillis() - time2;

        for (Map.Entry<Predicate, TransitiveFormula> entry : calculatorTC.entrySet()) {
            TransitiveFormula calculator = (TransitiveFormula) entry.getValue();
            calculator.invalidateTC();
        }

        if (debug2) {
            Logger.println("current structure:");
            Logger.println(structure.toString());
            Logger.println("original structure:");
            if (((BaseTVS) structure).getOriginalStructure() != null)
                Logger.println(((BaseTVS) structure).getOriginalStructure().toString());
            else
                Logger.println(" -- null");
            Logger.println("incremental map:");
            if (initialMap != null)
                Logger.println(initialMap.toString());
            else
                Logger.println(" -- null");
        }

        // SortedSet workSet;
        Collection<MultiConstraint> workSet;

        for (Collection<MultiConstraint> component : multiComponents) {
            boolean firstTime = true;
            newChanges = null;
            oldChanges = initialMap;

            while ((component != null) && (!component.isEmpty())) {
                workSet = null;
                if (!firstTime) {
                    oldChanges = newChanges;
                    newChanges = null;
                }

                for (Iterator<MultiConstraint> j = component.iterator(); j.hasNext();) {
                    MultiConstraint constraint = j.next();

                    if (!constraint.isActive(structure)) {
                        continue;
                    }

                    if (checkModified && (firstTime == true) && !modifiedSet.contains(constraint))
                        continue;

                    int action;

                    stat_totalCoerceCalls++;
                    if (firstTime) {
                        stat_firstCoerceCalls++;
                    }

                    time2 = System.currentTimeMillis();
                    if (newChanges == null)
                        newChanges = new NodeValueMap();
                    time_coerceLoop3 += System.currentTimeMillis() - time2;

                    action = constraint.coerce(structure, oldChanges, newChanges, !checkModified, firstTime);

                    switch (action) {
                    case Unmodified:
                        break;

                    case Invalid:
                        for (Iterator<EvalLiteral> itHeads = constraint.heads.iterator(); itHeads.hasNext();) {
                            EvalLiteral head = itHeads.next();
                            head.testAndReset();
                        }

                        if (debug) {
                            Logger.println("MultiConstraint invalid: " + constraint.toString());

                            if (debug2) {
                                Logger.println(structure.toString());
                                if (oldChanges != null)
                                    Logger.println(oldChanges.toString());
                                else
                                    Logger.println("Incremental map = null");
                                Logger.println("============================================");
                            }
                        }
                        if (AnalysisStatus.debug) {
                            IOFacade.instance()
                                    .printStructure(
                                            structure,
                                            "Constraint Breached: " + constraint + " on assignment "
                                                    + constraint.currentAssign);
                        }

                        time_coerce += System.currentTimeMillis() - time;
                        verifier.verifyCorrectness(false);
                        return false;

                    case Modified:
                        time2 = System.currentTimeMillis();
                        for (Iterator<EvalLiteral> itHeads = constraint.heads.iterator(); itHeads.hasNext();) {
                            EvalLiteral head = itHeads.next();
                            if (head.testAndReset()) {
                                if (!head.strongDependents.isEmpty()) {
                                    if (workSet == null) {
                                        workSet = new FixedSortedSet<MultiConstraint>(MultiConstraint.totalSize());
                                    }
                                    workSet.addAll(head.strongDependents);
                                }
                                if (checkModified) {
                                    modifiedSet.addAll(head.nonStrongDependentsBits);
                                }

                                if (head.isPredicate() && head.predicate.arity() == 2) {
                                    TransitiveFormula calculator = calculatorTC.get(head.predicate);
                                    if (calculator != null) {
                                        calculator.invalidateTC();
                                    }
                                }
                            }
                        } // for heads
                        time_coerceLoop2 += System.currentTimeMillis() - time2;

                        if (debug) {
                            Logger.println("MultiConstraint modified: " + constraint.toString());
                            if (debug2)
                                Logger.println(structure.toString());
                        }
                    } // switch
                } // for constraints

                time2 = System.currentTimeMillis();
                if (initialMap != null && newChanges != null && !newChanges.isEmpty()) {
                    initialMap.addAll(newChanges);
                }
                time_coerceLoop3 += System.currentTimeMillis() - time2;

                component = workSet;
                firstTime = false;
            } // while component
        } // for components

        if (debug) {
            if (debug2) {
                Logger.println("final structure:");
                Logger.println(structure.toString());
            }

            int all_constraints = stat_goodConstraints + stat_incrementalEvals;
            if (!checkModified)
                Logger.println("==> all constraints: " + all_constraints + ", good constrains: " + stat_goodConstraints
                        + ", incremental: " + stat_incrementalEvals);
            else
                Logger.println("==> all constraints: " + all_constraints + ", good constrains: " + stat_goodConstraints
                        + ", incremental: " + stat_incrementalEvals + ", total modified: " + modifiedSet.size());
        }

        time_coerce += System.currentTimeMillis() - time;
        verifier.verifyCorrectness(true);
        return true;
    }
}

class FixedSortedSet<T> implements Collection<T> {
    Object table[];
    int size;
    int firstNonEmpty;

    public FixedSortedSet(int size) {
        table = new Object[size];
        this.size = 0;
        firstNonEmpty = size;
    }

    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public T[] toArray(Object[] o) {
        throw new UnsupportedOperationException();
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        for (int i = 0; i < table.length; ++i)
            table[i] = null;
        size = 0;
        firstNonEmpty = table.length;
    }

    public boolean add(Object o) {
        int hashCode = ((Identifiable) o).getId();
        if (table[hashCode] != null) {
            size++;
            if (hashCode < firstNonEmpty)
                firstNonEmpty = hashCode;
        }
        table[hashCode] = o;
        return true;
    }

    public boolean contains(Object o) {
        if (table[((Identifiable) o).getId()] == null)
            return false;
        else
            return true;
    }

    public boolean remove(Object o) {
        int hashCode = ((Identifiable) o).getId();
        if (table[hashCode] != null) {
            table[hashCode] = null;
            size--;
            if (hashCode == firstNonEmpty) {
                for (firstNonEmpty = hashCode; firstNonEmpty < table.length; firstNonEmpty++)
                    if (table[firstNonEmpty] != null)
                        break;
            }
        }

        return true;
    }

    public boolean addAll(Collection<? extends T> col) {
        for (Iterator<? extends T> it = col.iterator(); it.hasNext();) {
            Identifiable o = (Identifiable) it.next();
            int hashCode = o.getId();
            if (table[hashCode] == null) {
                size++;
                if (hashCode < firstNonEmpty)
                    firstNonEmpty = hashCode;
            }
            table[hashCode] = o;
        }
        return true;
    }

    public boolean removeAll(Collection<?> col) {
        for (Iterator<?> it = col.iterator(); it.hasNext();) {
            Identifiable o = (Identifiable) it.next();
            int hashCode = o.getId();
            if (table[hashCode] != null) {
                table[hashCode] = null;
                size--;
                if (hashCode == firstNonEmpty) {
                    for (firstNonEmpty = hashCode; firstNonEmpty < table.length; firstNonEmpty++)
                        if (table[firstNonEmpty] != null)
                            break;
                }
            }
        }
        return true;
    }

    public boolean containsAll(Collection<?> col) {
        for (Iterator<?> it = col.iterator(); it.hasNext();) {
            Identifiable o = (Identifiable) it.next();
            if (table[o.getId()] == null)
                return false;
        }
        return true;
    }

    public boolean retainAll(Collection<?> col) {
        boolean flag = false;
        for (int i = 0; i < table.length; i++) {
            if (table[i] != null && !col.contains(table[i])) {
                table[i] = null;
                size--;
            }
            if (!flag && table[i] != null) {
                flag = true;
                firstNonEmpty = i;
            }
        }
        return true;

    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int index = firstNonEmpty;

            public boolean hasNext() {
                while (index < table.length && table[index] == null)
                    ++index;
                return index < table.length;
            }

            @SuppressWarnings("unchecked")
            public T next() {
                if (hasNext()) {
                    return (T) table[index++];
                } else {
                    return null;
                }
            }

            public void remove() {
            }
        };
    }
}
