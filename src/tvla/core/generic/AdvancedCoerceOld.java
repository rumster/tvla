package tvla.core.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import tvla.analysis.AnalysisStatus;
import tvla.core.Coerce;
import tvla.core.Constraints;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.base.BaseTVS;
import tvla.core.common.ModifiedPredicates;
import tvla.core.common.NodeValue;
import tvla.formulae.EqualityFormula;
import tvla.formulae.Formula;
import tvla.formulae.FormulaIterator;
import tvla.formulae.NotFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.TransitiveFormula;
import tvla.formulae.Var;
import tvla.io.IOFacade;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Logger;
import tvla.util.SingleIterator;
import tvla.util.StringUtils;

public class AdvancedCoerceOld extends GenericCoerce {
    protected static boolean debug2 = false;
    private static final boolean USE_MODIFIED_PREDICATES = false;
    protected Map<Predicate, TransitiveFormula> calculatorTC = HashMapFactory.make();
    /**
     * Maps advanced (optimized form of) constraints to generic constraints.
     * 
     * @author Roman Manevich.
     * @since tvla-2-alpha November 18 2002, Initial creation.
     */
    protected Map<AdvancedConstraint, Constraint> advancedToGeneric;
    /**
     * Maps predicates to collections of constraints.
     */
    private static Map<Predicate, Collection<AdvancedConstraint>> predicateToConstraints = HashMapFactory.make();
    protected Collection<Collection<AdvancedConstraint>> connectedComponents = null;
    
    public static void reset() {
    	predicateToConstraints = HashMapFactory.make();
    }

    protected void addConstraint(Formula _body, Formula head) {
        Formula body = _body.optimizeForEvaluation();
        List<Formula> bodyDisjuncts = new ArrayList<Formula>();
        Formula.getOrs(body, bodyDisjuncts);
        for (Formula bodyDisjunct : bodyDisjuncts) {
            Constraint constraint = createConstraint(bodyDisjunct, head);
            AdvancedConstraint advanced = new AdvancedConstraint(bodyDisjunct, head);

            for (Iterator<Map.Entry<Predicate, Collection<TransitiveFormula>>> i = advanced.allTCIterator(); i
                    .hasNext();) {
                Map.Entry<Predicate, Collection<TransitiveFormula>> entry = i.next();
                Predicate predicate = entry.getKey();
                Collection<TransitiveFormula> newTCs = entry.getValue();
                if (allTC == null)
                    allTC = HashMapFactory.make();
                Collection<TransitiveFormula> theTCs = allTC.get(predicate);
                if (theTCs == null) {
                    theTCs = new ArrayList<TransitiveFormula>();
                    allTC.put(predicate, theTCs);
                }
                theTCs.addAll(newTCs);
            }

            if (advancedToGeneric == null) {
                advancedToGeneric = HashMapFactory.make();
            }
            advancedToGeneric.put(advanced, constraint);
            advanced.setBaseConstraint(constraint);
            // Update the map from predicates to the set of corresponding
            // constraints.
            // Set predicates = GetFormulaPredicates.get(bodyDisjunct);
            // predicates.addAll(GetFormulaPredicates.get(head));
            Set<Predicate> predicates = HashSetFactory.make(bodyDisjunct.getPredicates());
            predicates.addAll(head.getPredicates());

            for (Predicate predicate : predicates) {
                Collection<AdvancedConstraint> constraints = predicateToConstraints.get(predicate);
                if (constraints == null) {

                    // constraints = HashSetFactory.make();
                    constraints = new LinkedHashSet<AdvancedConstraint>();
                    predicateToConstraints.put(predicate, constraints);
                }
                constraints.add(advanced);
            }
            this.constraints.add(constraint);
        }
    }

    /**
     * Returns the set of constraints that involve the predicates that were
     * modified by the last action.
     */
    protected SortedSet<AdvancedConstraint> getInitialWorkSetConstraints() {
        Collection<Predicate> modifiedPredicates = ModifiedPredicates.getModified();
        SortedSet<AdvancedConstraint> result = new TreeSet<AdvancedConstraint>();
        for (Iterator<Predicate> predIter = modifiedPredicates.iterator(); predIter.hasNext();) {
            Predicate predicate = predIter.next();
            Collection<AdvancedConstraint> constraints = predicateToConstraints.get(predicate);
            if (constraints != null)
                result.addAll(constraints);
        }
        return result;
    }

    protected Collection<AdvancedConstraint> getModifiedConstraints() {
        return getModifiedConstraints(ModifiedPredicates.getModified());
    }

    protected Collection<AdvancedConstraint> getModifiedConstraints(Set<Predicate> modifiedPredicates) {
        Set<AdvancedConstraint> result = HashSetFactory.make((int) (advancedToGeneric.size() * 1.3));
        // Collection result = new FixedSortedSet(advancedToGeneric.size());
        for (Iterator<Predicate> predIter = modifiedPredicates.iterator(); predIter.hasNext();) {
            Predicate predicate = predIter.next();
            Collection<AdvancedConstraint> constraints = predicateToConstraints.get(predicate);
            if (constraints != null)
                result.addAll(constraints);
        }
        return result;
    }

    public static int stat_coerce = 0;
    public static int stat_goodConstraints = 0;
    public static int stat_badConstraints = 0;
    public static int stat_incrementalEvals = 0;
    public static int stat_totalCoerceCalls = 0;
    public static int stat_firstCoerceCalls = 0;
    public static long time_coerceInc = 0;
    public static long time_coerceBad = 0;
    public static long time_coerceBad2 = 0;
    public static long time_coerceGetIncrements = 0;
    public static long time_coerceTC = 0;
    public static long time_coerce = 0;
    boolean nodesAdded = false;
    public static float stat_coerceIncrementalIters = 0;

    public boolean coerce(TVS structure) {
        long time = System.currentTimeMillis(), time2;

        boolean checkModified = false;
        // Set modifiedPredicates2 = ModifiedPredicates.getModified();
        Set<Predicate> modifiedPredicates = structure.getModifiedPredicates();

        /*
         * Logger.print("preds1:"); for (Iterator it =
         * modifiedPredicates.iterator(); it.hasNext();) { Predicate p =
         * (Predicate)it.next(); Logger.print(" " + p); } Logger.println();
         * Logger.print("preds2:"); for (Iterator it =
         * modifiedPredicates2.iterator(); it.hasNext();) { Predicate p =
         * (Predicate)it.next(); Logger.print(" " + p); } Logger.println();
         */

        if (modifiedPredicates.isEmpty()) {
            time_coerce += System.currentTimeMillis() - time;
            return true;
        }
        time_coerceBad2 += System.currentTimeMillis() - time;

        stat_coerce++;

        NodeValueMap newChanges, oldChanges, initialMap = null;

        time2 = System.currentTimeMillis();
        initialMap = structure.getIncrementalUpdates();
        if (initialMap != null) {
            modifiedPredicates = initialMap.map.keySet();
            if (modifiedPredicates.isEmpty()) {
                time_coerceGetIncrements += System.currentTimeMillis() - time2;
                time_coerce += System.currentTimeMillis() - time;
                return true;
            }
        }
        time_coerceGetIncrements += System.currentTimeMillis() - time2;

        time2 = System.currentTimeMillis();
        Collection<AdvancedConstraint> modifiedSet = null;
        if (!modifiedPredicates.contains(Vocabulary.active)) {
            modifiedSet = getModifiedConstraints(modifiedPredicates);
            checkModified = true;
        }
        nodesAdded = !checkModified;
        time_coerceBad2 += System.currentTimeMillis() - time2;

        time2 = System.currentTimeMillis();
        for (Map.Entry<Predicate, TransitiveFormula> entry : calculatorTC.entrySet()) {
            // Predicate p = (Predicate)entry.getKey();
            // if (checkModified && !modifiedPredicates.contains(p))
            // continue;
            TransitiveFormula calculator = (TransitiveFormula) entry.getValue();
            // calculator.calculateTC(structure, null);
            calculator.invalidateTC();
        }
        time_coerceTC += System.currentTimeMillis() - time2;

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
        Collection<AdvancedConstraint> workSet;

        for (Iterator<Collection<AdvancedConstraint>> it = connectedComponents.iterator(); it.hasNext();) {
            boolean firstTime = true;
            Collection<AdvancedConstraint> component = it.next();
            newChanges = null;
            oldChanges = initialMap;

            while ((component != null) && (!component.isEmpty())) {
                workSet = null;
                if (!firstTime) {
                    oldChanges = newChanges;
                    newChanges = null;
                }

                for (Iterator<AdvancedConstraint> j = component.iterator(); j.hasNext();) {
                    AdvancedConstraint constraint = j.next();
                    if (firstTime && !constraint.isActive(structure)) {
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
                    time_coerceBad2 += System.currentTimeMillis() - time2;

                    if (oldChanges == null) {
                        time2 = System.currentTimeMillis();
                        action = coerce(structure, constraint, newChanges);
                        time_coerceBad += System.currentTimeMillis() - time2;
                    } else {
                        action = coerceIncremental(structure, constraint, oldChanges, newChanges);
                    }

                    if (action == Invalid) {

                        if (debug) {
                            Logger.println("Constraint invalid: "
                                    + (String) advancedToGeneric.get(constraint).toString());
                            if (debug2) {
                                Logger.println(structure.toString());
                                if (oldChanges != null)
                                    Logger.println(oldChanges.toString());
                                else
                                    Logger.println("Incremental map = null");
                                Logger.println("============================================");
                            }
                        }

                        time_coerce += System.currentTimeMillis() - time;
                        return false;
                    }

                    if (action == Modified) {
                        if (debug) {
                            Logger.println("Constraint modified: "
                                    + (String) advancedToGeneric.get(constraint).toString());
                            if (debug2) {
                                Logger.println(structure.toString());
                            }
                        }

                        time2 = System.currentTimeMillis();
                        if (!constraint.strongDependents.isEmpty()) {
                            if (workSet == null) {
                                // workSet = new TreeSet();
                                workSet = new FixedSortedSet<AdvancedConstraint>(constraints.size());
                            }
                            workSet.addAll(constraint.strongDependents);
                        }
                        time_coerceBad2 += System.currentTimeMillis() - time2;

                        if (constraint.head.atomic instanceof PredicateFormula
                                && ((PredicateFormula) constraint.head.atomic).predicate().arity() == 2) {
                            PredicateFormula pf = (PredicateFormula) constraint.head.atomic;
                            Predicate predicate = pf.predicate();
                            TransitiveFormula calculator = calculatorTC.get(predicate);
                            if (calculator != null) {
                                time2 = System.currentTimeMillis();
                                // calculator.calculateTC(structure, null);
                                calculator.invalidateTC();
                                time_coerceTC += System.currentTimeMillis() - time2;
                            }
                        }

                        time2 = System.currentTimeMillis();
                        if (checkModified) {
                            modifiedSet.addAll(constraint.nonStrongDependents);
                            // modifiedSet.addAll(constraint.dependents);
                        }
                        time_coerceBad2 += System.currentTimeMillis() - time2;
                    }
                }

                time2 = System.currentTimeMillis();
                if (initialMap != null && newChanges != null && !newChanges.isEmpty()) {
                    initialMap.addAll(newChanges);
                }
                time_coerceBad2 += System.currentTimeMillis() - time2;

                component = workSet;
                firstTime = false;
            }
        }

        if (debug2) {
            Logger.println("final structure:");
            Logger.println(structure.toString());
        }

        if (debug) {
            int all_constraints = stat_goodConstraints + stat_incrementalEvals;
            if (!checkModified)
                Logger.println("==> all constraints: " + all_constraints + ", good constrains: " + stat_goodConstraints
                        + ", incremental: " + stat_incrementalEvals);
            else
                Logger.println("==> all constraints: " + all_constraints + ", good constrains: " + stat_goodConstraints
                        + ", incremental: " + stat_incrementalEvals + ", total modified: " + modifiedSet.size());
        }

        time_coerce += System.currentTimeMillis() - time;
        return true;
    }

    public boolean coerceOld(TVS structure) {
        SortedSet<AdvancedConstraint> workSet = null;
        if (USE_MODIFIED_PREDICATES) {
            BaseTVS base = (BaseTVS) structure;
            if (ModifiedPredicates.getModified().contains(Vocabulary.sm)
                    || ModifiedPredicates.getModified().contains(Vocabulary.active)
                    || base.getModifiedPredicates().contains(Vocabulary.sm)
                    || base.getModifiedPredicates().contains(Vocabulary.active)) {
                workSet = new TreeSet<AdvancedConstraint>(advancedToGeneric.keySet());
            } else {
                workSet = getInitialWorkSetConstraints();
            }
        } else {
            workSet = new TreeSet<AdvancedConstraint>(advancedToGeneric.keySet());
        }

        for (Iterator<TransitiveFormula> i = calculatorTC.values().iterator(); i.hasNext();) {
            TransitiveFormula calculator = i.next();
            calculator.calculateTC(structure, null);
        }

        if (debug2) {
            Logger.println("current structure:");
            Logger.println(structure.toString());
        }

        while (!workSet.isEmpty()) {
            Iterator<AdvancedConstraint> first = workSet.iterator();
            AdvancedConstraint constraint = first.next();
            first.remove();

            int action = coerce(structure, constraint);
            if (action == Invalid) {
                if (debug) {
                    Logger.println("Constraint invalid: " + (String) advancedToGeneric.get(constraint).toString());
                    if (debug2) {
                        Logger.println(structure.toString());
                        Logger.println("============================================");
                    }
                }
                return false;
            }
            if (action == Modified) {
                if (debug) {
                    Logger.println("Constraint modified: " + (String) advancedToGeneric.get(constraint).toString());
                }
                workSet.addAll(constraint.dependents);
                if (constraint.head.atomic instanceof PredicateFormula
                        && ((PredicateFormula) constraint.head.atomic).predicate().arity() == 2) {
                    PredicateFormula pf = (PredicateFormula) constraint.head.atomic;
                    Predicate predicate = pf.predicate();
                    TransitiveFormula calculator = calculatorTC.get(predicate);
                    if (calculator != null)
                        calculator.calculateTC(structure, null);
                }
            }
        }
        return true;
    }

    protected int coerce(TVS structure, AdvancedConstraint constraint) {
        if (constraint.evalOrder == null) {
            stat_badConstraints++;
            return super.coerce(structure, advancedToGeneric.get(constraint));
        } else {
            stat_goodConstraints++;
        }

        int numberOfSteps = constraint.evalOrder.size();

        Iterator[] stepIt = new Iterator[numberOfSteps + 1];
        stepIt[0] = new SingleIterator<Assign>(Assign.EMPTY);

        int currentStep = 0;
        Collection<Assign> problemAssigns = HashSetFactory.make();
        OUTER: while (currentStep >= 0) {
            if (stepIt[currentStep].hasNext()) {
                Assign currentAssign = (Assign) stepIt[currentStep].next();
                if (currentStep == numberOfSteps) {
                    // We get here only if (goal_1 ^ ... ^ goal_n ^ !head) are
                    // all true,
                    // that means the constraint is violated.
                    Assign problemAssign = new Assign(currentAssign);
                    for (Iterator<Var> varIt = problemAssign.bound().iterator(); varIt.hasNext();) {

                        Var var = varIt.next();
                        Node node = problemAssign.get(var);
                        // FIXME:
                        // if (structure.eval(Vocabulary.active,
                        // node).equals(Kleene.unknownKleene)) {
                        if (!structure.eval(Vocabulary.active, node).equals(Kleene.trueKleene)) {
                            continue OUTER;
                        }
                    }
                    problemAssign.project(constraint.head.atomic.freeVars());
                    problemAssigns.add(problemAssign);
                } else {
                    Formula formula = (Formula) constraint.evalOrder.get(currentStep);
                    currentStep++;
                    // Notice that this is reference comparison - there is no
                    // need
                    // to use the expensive equals comparison.
                    if (formula == constraint.evalHead) {
                        stepIt[currentStep] = structure.evalFormula(formula, currentAssign);
                        // stepIt[currentStep] =
                        // structure.evalFormulaForValue(formula, currentAssign,
                        // null);
                    } else {
                        stepIt[currentStep] = structure.evalFormulaForValue(formula, currentAssign, Kleene.trueKleene);
                    }

                }
            } else {
                // backtrack to previous assignment iterator
                currentStep--;
            }
        }

        Constraint baseConstraint = advancedToGeneric.get(constraint);
        // Map repairedPredicates = HashMapFactory.make();
        NodeValueMap repairedPredicates = new NodeValueMap();

        int result = resolve(structure, baseConstraint, problemAssigns, repairedPredicates);
        return result;
    }

    public static float stat_coerceBasicIters = 0;

    public AdvancedCoerceOld(Set<Constraints.Constraint> constraints) {
        super(constraints);
        init();
    }

    private void init() {
        if (advancedToGeneric == null) {
            advancedToGeneric = HashMapFactory.make();
        }
        
        // Initialize dependencies
        for (Iterator<AdvancedConstraint> it = advancedToGeneric.keySet().iterator(); it.hasNext();) {
            AdvancedConstraint constraint = it.next();
            constraint.calculateDependents(advancedToGeneric.keySet());
        }
        Logger.println();
        Logger.println("#constraints:       " + advancedToGeneric.keySet().size());

        int num_bad = 0;
        for (Iterator<AdvancedConstraint> it = advancedToGeneric.keySet().iterator(); it.hasNext();) {
            AdvancedConstraint ac = it.next();
            if (ac.isComplex()) {
                num_bad++;
                if (Coerce.debug)
                	Logger.println("Complex constraint: " + ac);
            }
        }

        Logger.println("#Complex constraints (cannot compute incrementally): " + num_bad);
 
    }
    
    protected void printDependencyListToLog() {
		int scc = 0;
		Logger.println(StringUtils.addUnderline("Constraints:"));
		for (Iterator<Collection<AdvancedConstraint>> it2 = connectedComponents
				.iterator(); it2.hasNext();) {
			Collection<AdvancedConstraint> component = it2.next();
			Logger.println("-------------------------------------------");
			Logger.println("Component: " + scc);
			scc++;
			for (Iterator<AdvancedConstraint> it3 = component.iterator(); it3
					.hasNext();) {
				AdvancedConstraint constraint = it3.next();
				Constraint base = advancedToGeneric.get(constraint);
				Logger.print(constraint.id + ": " + base);
				if (constraint.isComplex()) {
					Logger.println("(bad)");
				} else
					Logger.println();
				Logger.print("    dependents:");
				for (Iterator<AdvancedConstraint> depIt = constraint.dependents
						.iterator(); depIt.hasNext();) {
					AdvancedConstraint dependent = depIt.next();
					Logger.print(" " + dependent.id);
				}
				Logger.print("; strong: ");
				for (Iterator<AdvancedConstraint> depIt = constraint.strongDependents
						.iterator(); depIt.hasNext();) {
					AdvancedConstraint dependent = depIt.next();
					Logger.print(" " + dependent.id);
				}
				Logger.println();
			}
		}
		// Logger.println("bad constraints: " + num_bad);
	}

    protected int coerce(TVS structure, AdvancedConstraint constraint, NodeValueMap newChanges) {
        assert (constraint.evalOrder != null);
        stat_goodConstraints++;

        float savedEvals = FormulaIterator.stat_TotalEvals;

        int total = Unmodified;
        // Constraint baseConstraint = (Constraint)
        // advancedToGeneric.get(constraint);
        Constraint baseConstraint = constraint.getBase();

        int numberOfSteps = constraint.evalOrder.size();

        Iterator[] stepIt = new Iterator[numberOfSteps + 1];
        stepIt[0] = SingleIterator.instance(Assign.EMPTY);

        int currentStep = 0;
        // Collection problemAssigns = HashSetFactory.make();
        List<PredicateAssign> problemAssigns = null;

        OUTER: while (currentStep >= 0) {
            if (stepIt[currentStep].hasNext()) {
                Assign currentAssign = (Assign) stepIt[currentStep].next();
                if (currentStep == numberOfSteps) {
                    // We get here only if (goal_1 ^ ... ^ goal_n ^ !head) are
                    // all true,
                    // that means the constraint is violated.
                    for (Iterator varIt = currentAssign.bound().iterator(); varIt.hasNext();) {

                        Var var = (Var) varIt.next();
                        Node node = currentAssign.get(var);
                        if (structure.eval(Vocabulary.active, node).equals(Kleene.unknownKleene)) {
                            continue OUTER;
                        }
                    }

                    // Seems to be no need for project, since resolve doesn't
                    // use other variables.
                    // problemAssign.project(constraint.head.atomic.freeVars());
                    PredicateAssign newAssign = new PredicateAssign();

                    int result = resolve(structure, baseConstraint, currentAssign, newAssign);
                    if (result == Invalid) {
                        stat_coerceBasicIters += FormulaIterator.stat_TotalEvals - savedEvals;
                        return Invalid;
                    } else if (result == Modified) {
                        // newAssign.apply();
                        if (problemAssigns == null)
                            problemAssigns = new LinkedList<PredicateAssign>();
                        problemAssigns.add(newAssign);
                        newChanges.put(newAssign);
                        total = Modified;
                    }
                } else {
                    Formula formula = (Formula) constraint.evalOrder.get(currentStep);
                    currentStep++;
                    // Notice that this is reference comparison - there is no
                    // need
                    // to use the expensive equals comparison.
                    if (formula == constraint.evalHead) {
                        stepIt[currentStep] = formula.assignments(structure, currentAssign);
                    } else {
                        stepIt[currentStep] = formula.assignments(structure, currentAssign, Kleene.trueKleene);
                    }
                }
            } else {
                // backtrack to previous assignment iterator
                currentStep--;
            }
        }

        // Applicaton of resolve's changes to the structure must be done after
        // the evaluation
        // to avoid concurrent iterator update.
        if (problemAssigns != null) {
            for (Iterator<PredicateAssign> it = problemAssigns.iterator(); it.hasNext();) {
                PredicateAssign pa = it.next();
                pa.apply();
            }
        }

        // int result = resolve(structure, baseConstraint, problemAssigns,
        // newChanges);
        // baseConstraint.return result;
        stat_coerceBasicIters += FormulaIterator.stat_TotalEvals - savedEvals;
        return total;
    }

    protected boolean isIncrementalCoercePossible(AdvancedConstraint constraint, NodeValueMap oldChanges) {
        if (!constraint.isComplex())
            return true;
        if (nodesAdded)
            return false;
        for (Formula pivot : constraint.evalOrder) {
            if (!(pivot instanceof PredicateFormula)
                    && !((pivot instanceof NotFormula) && (((NotFormula) pivot).subFormula() instanceof PredicateFormula))) {
                // If modified predicates are inside complex formula,
                // fall back to regular (not incremental) coerce
                Set<Predicate> predicates = pivot.getPredicates();
                for (Predicate predicate : predicates) {
                    // Equality is handled too, because getPredicates returns sm
                    // for equality in a formula.
                    if (oldChanges.containsKey(predicate)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    protected int coerceIncremental(TVS structure, AdvancedConstraint constraint, NodeValueMap oldChanges,
            NodeValueMap newChanges) {

        assert (constraint.evalOrder != null);
        long time = System.currentTimeMillis();
        if (!isIncrementalCoercePossible(constraint, oldChanges)) {
            stat_badConstraints++;
            int result = coerce(structure, constraint, newChanges);
            time_coerceBad += System.currentTimeMillis() - time;
            return result;
        }

        float savedEvals = FormulaIterator.stat_TotalEvals;

        stat_incrementalEvals++;
        // Constraint baseConstraint = (Constraint)
        // advancedToGeneric.get(constraint);
        Constraint baseConstraint = constraint.getBase();
        int total = Unmodified;

        int numberOfSteps = constraint.evalOrder.size();
        Iterator[] stepIt = new Iterator[numberOfSteps + 1];
        // Collection problemAssigns = HashSetFactory.make();
        List<PredicateAssign> problemAssigns = new LinkedList<PredicateAssign>();

        for (Formula pivot : constraint.evalOrder) {
            PredicateFormula predicateFormula = null;
            EqualityFormula equalityFormula = null;
            boolean negated;
            if ((pivot instanceof PredicateFormula)) {
                predicateFormula = (PredicateFormula) pivot;
                negated = false;
            } else if ((pivot instanceof NotFormula) && (((NotFormula) pivot).subFormula() instanceof PredicateFormula)) {
                predicateFormula = (PredicateFormula) (((NotFormula) pivot).subFormula());
                negated = true;
            } else if ((pivot instanceof EqualityFormula)) {
                equalityFormula = (EqualityFormula) pivot;
                negated = false;
            } else if ((pivot instanceof NotFormula) && (((NotFormula) pivot).subFormula() instanceof EqualityFormula)) {
                if (pivot != constraint.evalHead)
                    continue;

                equalityFormula = (EqualityFormula) (((NotFormula) pivot).subFormula());
                negated = true;
            }
            // TODO: this now could be a more complicated formula with a
            // predicate inside.
            // We should probably traverse it. The analysis is currently
            // correct,
            // because we fall back to non-incremental coerce in such cases.
            else {
                continue;
            }

            if (predicateFormula != null) {
                Predicate predicate = predicateFormula.predicate();
                Collection<NodeValue> nodeValues = oldChanges.get(predicate);
                if (nodeValues == null)
                    continue;

                stepIt[0] = new IncrementalPredicateIterator(predicateFormula, nodeValues, negated,
                        pivot == constraint.evalHead);
            } else if (equalityFormula != null) {
                Collection<NodeValue> nodeValues = oldChanges.get(Vocabulary.sm);
                if (nodeValues == null)
                    continue;

                stepIt[0] = new IncrementalEqualityIterator(equalityFormula, nodeValues, negated,
                        pivot == constraint.evalHead);
            }

            int currentStep = 0;

            OUTER: while (currentStep >= 0) {
                if (stepIt[currentStep].hasNext()) {
                    Assign currentAssign = (Assign) stepIt[currentStep].next();
                    if (currentStep == numberOfSteps) {
                        // We get here only if (goal_1 ^ ... ^ goal_n ^ !head)
                        // are all true,
                        // that means the constraint is violated.
                        // Assign problemAssign = new Assign(currentAssign);

                        for (Iterator<Var> varIt = currentAssign.bound().iterator(); varIt.hasNext();) {

                            Var var = varIt.next();
                            Node node = currentAssign.get(var);
                            // if (structure.eval(Vocabulary.active,
                            // node).equals(Kleene.unknownKleene)) {
                            if (!structure.eval(Vocabulary.active, node).equals(Kleene.trueKleene)) {
                                continue OUTER;
                            }
                        }
                        // Seems to be no need for project, since resolve
                        // doesn't use other variables.
                        //problemAssign.project(constraint.head.atomic.freeVars(
                        // ));
                        PredicateAssign newAssign = new PredicateAssign();

                        int result = resolve(structure, baseConstraint, currentAssign, newAssign);
                        if (result == Invalid) {
                            time_coerceInc += System.currentTimeMillis() - time;
                            stat_coerceIncrementalIters += FormulaIterator.stat_TotalEvals - savedEvals;
                            return Invalid;
                        } else if (result == Modified) {
                            // newAssign.apply();
                            problemAssigns.add(newAssign);
                            newChanges.put(newAssign);
                            total = Modified;
                        }

                    } else {
                        Formula formula = (Formula) constraint.evalOrder.get(currentStep);
                        currentStep++;
                        // Notice that these are reference comparisons - there
                        // is no need
                        // to use the expensive equals comparison.
                        if (formula == pivot) {
                            // Do nothing, because we've already iterated on the
                            // pivot
                            // assignments
                            stepIt[currentStep] = SingleIterator.instance(currentAssign);
                        } else if (formula == constraint.evalHead) {
                            stepIt[currentStep] = formula.assignments(structure, currentAssign);
                        } else {
                            stepIt[currentStep] = formula.assignments(structure, currentAssign, Kleene.trueKleene);
                        }
                    }
                } else {
                    // backtrack to previous assignment iterator
                    currentStep--;
                }
            }
        }

        // Applicaton of resolve's changes to the structure must be done after
        // the evaluation
        // to avoid concurrent iterator update.
        for (Iterator<PredicateAssign> it = problemAssigns.iterator(); it.hasNext();) {
            PredicateAssign pa = it.next();
            pa.apply();
        }

        time_coerceInc += System.currentTimeMillis() - time;
        stat_coerceIncrementalIters += FormulaIterator.stat_TotalEvals - savedEvals;
        return total;
    }

    protected final int resolve(TVS structure, Constraint constraint, Collection<Assign> problemAssigns,
            NodeValueMap repairedPredicates) {
        int total = Unmodified;

        for (Iterator<Assign> it = problemAssigns.iterator(); it.hasNext();) {
            PredicateAssign newAssign = new PredicateAssign();
            Assign currentAssign = it.next();
            int result = resolve(structure, constraint, currentAssign, newAssign);
            if (result == Invalid) {
                return Invalid;
            } else if (result == Modified) {
                newAssign.apply();
                repairedPredicates.put(newAssign);
                total = Modified;
            }
        }
        return total;
    }

    protected final int resolve(TVS structure, Constraint constraint, Assign trueAssign, PredicateAssign newAssign) {
        if (constraint.constant) {
            // Constraint breached. No way to repair.
            if (AnalysisStatus.debug)
                IOFacade.instance().printStructure(structure,
                        "Constraint Breached: " + constraint + " on assignment " + trueAssign);
            return Invalid;
        } else if (constraint.predicateFormula != null) {
            Predicate predicate = constraint.predicateFormula.predicate();
            NodeTuple tuple = NodeTuple.EMPTY_TUPLE;

            if (predicate.arity() > 0) { // building the tuple for the truth
                                         // assignment
                Var[] vars = constraint.predicateFormula.variables();
                Node[] nodesTmp = new Node[predicate.arity()];
                for (int index = 0; index < nodesTmp.length; ++index) {
                    nodesTmp[index] = trueAssign.get(vars[index]);
                }
                tuple = NodeTuple.createTuple(nodesTmp);
            }

            Kleene currentValue = structure.eval(predicate, tuple);
            if (currentValue == (constraint.negated ? Kleene.trueKleene : Kleene.falseKleene)) {
                // Constraint breached. No way to repair.
                if (AnalysisStatus.debug)
                    IOFacade.instance().printStructure(structure,
                            "Constraint Breached: " + constraint + " on assignment " + trueAssign);
                return Invalid;
            } else if (currentValue == Kleene.unknownKleene) {
                newAssign
                        .copy(structure, predicate, tuple, constraint.negated ? Kleene.falseKleene : Kleene.trueKleene);
                return Modified;
            }
        } else if (constraint.equality) {
            Node leftNode = trueAssign.get(constraint.left);
            Node rightNode = trueAssign.get(constraint.right);
            if (constraint.negated) {
                if (leftNode.equals(rightNode)) {
                    // Constraint breached. No way to repair.
                    if (debug)
                        IOFacade.instance().printStructure(structure,
                                "Constraint Breached:" + constraint + " on assignment " + trueAssign);
                    return Invalid;
                }
            } else {
                if (leftNode.equals(rightNode)) {
                    if (structure.eval(Vocabulary.sm, rightNode) == Kleene.unknownKleene) {
                        // Fix the problem. This is no longer a summary node.
                        newAssign.copy(structure, Vocabulary.sm, rightNode, Kleene.falseKleene);
                        // CHANGE-1:
                        // summaryNodeModified = true;
                        return Modified;
                    }
                } else { // Constaint breached. No way to repair.
                    if (debug)
                        IOFacade.instance().printStructure(structure,
                                "Constraint Breached:" + constraint + " on assignment " + trueAssign);
                    return Invalid;
                }
            }
        } else {
            throw new RuntimeException("addConstraint should have handled this case.");
        }
        return Unmodified;
    }
}
