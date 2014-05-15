package tvla.core.generic;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignKleene;
import tvla.core.assignments.AssignPrecomputed;
import tvla.core.base.BaseTVS;
import tvla.core.common.NodeValue;
import tvla.formulae.EqualityFormula;
import tvla.formulae.Formula;
import tvla.formulae.FormulaIterator;
import tvla.formulae.FormulaVisitor;
import tvla.formulae.NotFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.TransitiveFormula;
import tvla.formulae.ValueFormula;
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.predicates.DynamicVocabulary;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.ConcatIterator;
import tvla.util.HashMapFactory;
import tvla.util.SingleIterator;

import com.ibm.dk.dps.util.BooleanContainer;

public final class MultiConstraint extends GraphNode<MultiConstraint> {

    boolean MultiConstraintsEnabled = true;
    
    List<EvalLiteral> literals = null;
    Collection<EvalLiteral> heads = null;
    Collection<AdvancedConstraint> advancedConstraints;
    Collection<Predicate> predicates;
    Collection<Predicate> complexPredicates;
    
    static Set<MultiConstraint> multiConstraints = new LinkedHashSet<MultiConstraint>(); 
    private static Map<AdvancedConstraint, MultiConstraint> advancedToMulti = HashMapFactory.make();
    public static PredicateConstraintsTranslator predicateToConstraints;

    static Collection<Collection<MultiConstraint>> connectedComponents = java.util.Collections.emptySet();
    
    public static void reset() {
    	multiConstraints = new LinkedHashSet<MultiConstraint>();
    	advancedToMulti = HashMapFactory.make();
    	predicateToConstraints = null;
    	connectedComponents = java.util.Collections.emptySet();
    }
    
    public static void putConstraints(Collection<AdvancedConstraint> col) {
        multiConstraints = new LinkedHashSet<MultiConstraint>(); 
        advancedToMulti = HashMapFactory.make();
        
        for (AdvancedConstraint ac : col) {
            addConstraint(ac);
        }

        for (AdvancedConstraint ac : col) {
            MultiConstraint mc = advancedToMulti.get(ac);
            for (AdvancedConstraint ac2 : ac.dependents) {
                MultiConstraint mc2 = advancedToMulti.get(ac2);
                mc.dependents.add(mc2);
            }
            for (AdvancedConstraint ac2 : ac.dependsOn) {
                MultiConstraint mc2 = advancedToMulti.get(ac2);
                mc.dependsOn.add(mc2);
            }
        }
                
        setAllLiteralDependencies();
        connectedComponents = getConnectedComponents(multiConstraints);
        predicateToConstraints = new PredicateConstraintsTranslator(multiConstraints);
    }
    
    public static Collection<Collection<MultiConstraint>> getConnectedComponents() {
        return connectedComponents;
    }
    
    public static int totalSize() {
        return multiConstraints.size();
    }
    
    private static void addConstraint(AdvancedConstraint ac) {
        MultiConstraint mc = new MultiConstraint(ac);

        for (MultiConstraint mc2 : multiConstraints) {
            if (mc2.equalLiterals(mc)) {
                mc2.merge(ac);
                advancedToMulti.put(ac, mc2);
                return;
            }
        }
        multiConstraints.add(mc);
        advancedToMulti.put(ac, mc);
    }
    
    public static void printStatistics() {
        System.err.println("Multiconstraints statistics:");
        for (MultiConstraint mc : multiConstraints) {
            System.err.println(
                    mc.id + "\t" + 
                    mc + "\t" + 
                    mc.countTotal + "\t" + 
                    mc.countModified + "\t" + 
                    mc.countInvalid + "\t" + 
                    (mc.time_coerceInc/1000.0) + "\t" + 
                    (mc.time_coerceBad/1000.0) + "\t" + 
                    ((mc.time_coerceInc+mc.time_coerceBad)/1000.0));
        }
    }
    
    static int stat_oneNegated = 0;
    static int stat_oneNegatedFixed = 0;
    
    private Formula body;

    private boolean complexTC = false;

    private DynamicVocabulary vocabulary;
    
    public MultiConstraint(AdvancedConstraint ac) {
        super();
        body = ac.body.formula;
        
        literals = new ArrayList<EvalLiteral>();
        Set<EvalLiteral> heads = new LinkedHashSet<EvalLiteral>();
        Set<Predicate> predicates = new LinkedHashSet<Predicate>();
        Set<Predicate> complexPredicates = new LinkedHashSet<Predicate>();
        
        int numNegated = 0;
        EvalLiteral singleNegated = null;
        
        for (Formula formula : ac.evalOrder) {
            EvalLiteral el = new EvalLiteral(formula);
            if (el.formula == ac.evalHead) {
                el.setHead(ac);
                heads.add(el);
            }
            literals.add(el);
            if (el.complex) {
                complex = true;
                complexPredicates.addAll(el.formula.getPredicates());
                complexTC = complexTC || hasComplexTC(el.formula, ac.body);
            }
            
            if (el.ispredicate && el.negated) {
                numNegated++;
                singleNegated = el;
            }
            
            predicates.addAll(el.formula.getPredicates());
        }
        
        if (numNegated == 1 && !complex) {
            stat_oneNegated++;
            Set<Var> freeVarsWithout = new LinkedHashSet<Var>();
            for (EvalLiteral el : literals) {
                if (el == singleNegated)
                    continue;
                freeVarsWithout.addAll(el.formula.freeVars());
            }
            if (freeVarsWithout.containsAll(singleNegated.formula.freeVars())) {
                singleNegated.skipAdded = true;
                stat_oneNegatedFixed++;
            }
        }
        
        this.heads = new ArrayList<EvalLiteral>(heads);
        this.predicates = new ArrayList<Predicate>(predicates);
        this.complexPredicates = new ArrayList<Predicate>(complexPredicates);
        
        advancedConstraints = new LinkedHashSet<AdvancedConstraint>();
        advancedConstraints.add(ac);
        
        stepIt = new Iterator[literals.size() + 1];
        literalsArray = new EvalLiteral[literals.size()];
        int i = 0;
        for (EvalLiteral el : literals) {
            literalsArray[i++] = el;
        }
        
        this.precomputedAssign = new AssignPrecomputed();
        
        this.vocabulary = DynamicVocabulary.create(predicates);        
    }
    
    private boolean hasComplexTC(Formula formula, final ConstraintBody constraintBody) {
        final BooleanContainer result = new BooleanContainer(false);
        new FormulaVisitor<Boolean>() {        
            @Override
            public Boolean accept(TransitiveFormula formula) {
                result.setValue(result.getValue() || constraintBody.badTC.contains(formula));
                return null;
            }        
        }.traverse(formula);
        return result.getValue();
    }

    // All heads should be initialized when calling this function
    public void setLiteralDependencies() {
        Collection<EvalLiteral> visitedLiterals = new LinkedList<EvalLiteral>();

        for (EvalLiteral el : literals) {
            if (!el.ispredicate && !el.equality)
                continue;
            
            for (EvalLiteral other : visitedLiterals) {
                if (!(el.ispredicate && other.ispredicate && (el.negated == other.negated) && 
                   ((PredicateFormula)el.subFormula).equalsByStructure((PredicateFormula)other.subFormula)) && 
                   !(el.equality && other.equality && (el.negated == other.negated))) 
                    continue;
                el.relatedLiteral = other;
                if (el.head == other.head)
                    el.samePredicate = true;
                else if (el.head)
                    el.pivotUnknownOnly = true;
                else 
                    el.nonPivotUnknownOnly = true;
            }
            
            visitedLiterals.add(el);
        }
    }
    
    public static void setAllLiteralDependencies() {
        for (MultiConstraint mc : multiConstraints) {
            mc.setLiteralDependencies();
        }
    }
    
    public void setStrongDependents(Collection<MultiConstraint> deps) {
        super.setStrongDependents(deps);
        
        for (EvalLiteral el : heads) {
            Collection<MultiConstraint> strongSet = new LinkedHashSet<MultiConstraint>();
            Collection<Identifiable> nonStrongSet = new LinkedHashSet<Identifiable>();
            for (AdvancedConstraint ac : el.constraint.dependents) {
                MultiConstraint mc = advancedToMulti.get(ac);
                if (strongDependents.contains(mc)) {
                    strongSet.add(mc);
                }
                else if (nonStrongDependents.contains(mc)) {
                    nonStrongSet.add(mc);
                }
                else {
                    throw new RuntimeException("Shouldn't happen!");
                }
            }
            el.strongDependents = new ArrayList<MultiConstraint>(strongSet);
            el.nonStrongDependents = new ArrayList<Identifiable>(nonStrongSet);
            el.nonStrongDependentsBits = new BitSet(totalSize(), nonStrongSet);
        }
    }
    
    private boolean complex = false;
    
    public boolean isComplex() {
        return complex;
    }
    
    public String toString() {
        return literals.toString();
    }
    
    private boolean equalLiterals(MultiConstraint mc) {
        int size = literals.size();
        if (mc.literals.size() != size || !MultiConstraintsEnabled)
            return false;
        if (size > 31)
            throw new RuntimeException("More than 31 literals in constraint");
        int mask = 0;

        OUTER:
        for (int i = 0; i < size; ++i) {
            EvalLiteral el1 = literals.get(i);
            for (int j = 0; j < size; ++j) {
                EvalLiteral el2 = mc.literals.get(j);
                if (el1.equals(el2)) {
                    mask |= (1 << j);
                    continue OUTER;
                }
            }
            return false;
        }
        return mask == (1 << size) - 1;
    }
    
    private void merge(AdvancedConstraint ac) {
        for (EvalLiteral el : literals) {
            if (el.formula.equals(ac.evalHead)) {
                el.setHead(ac);
                heads.add(el);
            }
        }
        advancedConstraints.add(ac);
    }
    
    final private Iterator<AssignKleene>[] stepIt;
    final private EvalLiteral[] literalsArray; 
    
    public int countInvalid = 0;
    public int countModified = 0;
    public int countTotal = 0;

    private long time_coerceBad;

    private long time_coerceInc;
    
    public int coerce(TVS structure, NodeValueMap oldChanges, NodeValueMap newChanges, boolean nodesAdded, boolean firstTime) {
        int result = AdvancedCoerce.Unmodified;
        long time = System.currentTimeMillis();
        
        if (isIncrementalCoercePossible(structure, oldChanges, nodesAdded, firstTime)) {
            AdvancedCoerce.stat_incrementalEvals++;
            result = coerceIncremental(structure, oldChanges, newChanges, firstTime);
            long delta = System.currentTimeMillis() - time;
            time_coerceInc += delta;
            AdvancedCoerce.time_coerceInc += delta;
        }
        else {
            AdvancedCoerce.stat_goodConstraints++;
            result = coerceFull(structure, newChanges);
            long delta = System.currentTimeMillis() - time;
            time_coerceBad += delta;
            AdvancedCoerce.time_coerceBad += delta;
        }
        
        
        countTotal++;
        switch (result) {
        case AdvancedCoerce.Unmodified:
            break;
        case AdvancedCoerce.Modified:
            countModified++;
            break;
        default:
            countInvalid++;
            break;
        }

        return result;
    }
    
    
    final private boolean isIncrementalCoercePossible(TVS structure, NodeValueMap oldChanges, boolean nodesAdded, boolean firstTime) {
        if (oldChanges == null || !BaseTVS.EnableIncrements)
            return false;
        
        if (oldChanges.getDeltaPredicates().intersection(vocabulary) != DynamicVocabulary.empty()) {
            return false;
        }
        
        if (!complex) {
            return true;
        }
        
        if (complexTC || nodesAdded)
            return false;

        if (firstTime && complexPredicates.contains(Vocabulary.sm) && oldChanges.getInequalities() != null) {
            return false;
        }
        
        for (Predicate predicate : complexPredicates) {
            if (oldChanges.containsKey(predicate)) {
                return false;
            }
        }
        return true;
    }

    private AssignPrecomputed precomputedAssign;

    protected AssignKleene currentAssign;
    
    private final AssignKleene getInitialAssign() {
        return precomputedAssign;
    }
        
    private int coerceFull(TVS structure, NodeValueMap newChanges) {
        float savedEvals = FormulaIterator.stat_TotalEvals;

        int total = AdvancedCoerce.Unmodified;

        // Invalidate complex TCs. It is quite slow - we should come up
        // with a better idea...
        if (complexTC) {
            body.prepare(structure);
        }
        
        int numberOfSteps = literals.size();
        
        Iterator<?>[] stepIt = this.stepIt;
        stepIt[0] = SingleIterator.instance(getInitialAssign());
        
        int currentStep = 0;
        int unknownEncounteredStep = -1;
        EvalLiteral currentHead = null;
        List<PredicateAssign> problemAssigns = null;
        
        OUTER: while (currentStep >= 0) {
          if (stepIt[currentStep].hasNext()) {
            currentAssign = (AssignKleene) stepIt[currentStep].next();
            if (currentAssign.kleene == Kleene.unknownKleene) {
                unknownEncounteredStep = currentStep;
                currentHead = literals.get(currentStep - 1);
            }
            if (currentStep == numberOfSteps) {
              // We get here only if (goal_1 ^ ... ^ goal_n ^ !head) are all true,
              // that means the constraint is violated.
              for (Var var : currentAssign.bound()) {
                Node node = currentAssign.get(var);
                if (!structure.eval(Vocabulary.active, node).equals(Kleene.trueKleene)) {
                  continue OUTER;
                }
              }
              
              PredicateAssign newAssign = null;
              int result = (unknownEncounteredStep >= 0)?
                            currentHead.resolve(structure, 
                                    currentAssign, newAssign = new PredicateAssign()) :
                            AdvancedCoerce.Invalid;

              switch (result) {
              case AdvancedCoerce.Invalid:
                    AdvancedCoerce.stat_coerceBasicIters += FormulaIterator.stat_TotalEvals - savedEvals;
                      return AdvancedCoerce.Invalid;
              case AdvancedCoerce.Modified:
                        if (!newChanges.putAndCheck(newAssign)) {
                            if (problemAssigns == null)
                                problemAssigns = new LinkedList<PredicateAssign>();
                            problemAssigns.add(newAssign);
                        }
                        total = AdvancedCoerce.Modified;
              }

            } else {
              EvalLiteral el = literals.get(currentStep);
              Formula formula = el.formula;
              currentStep++;
              
              if (el.head && unknownEncounteredStep == -1) {
                stepIt[currentStep] = formula.assignments(structure, currentAssign);
              } else {
                stepIt[currentStep] = formula.assignments(structure, currentAssign, Kleene.trueKleene);
              }
            }
          } else {
            // backtrack to previous assignment iterator
            currentStep--;
            if (currentStep <= unknownEncounteredStep)
                unknownEncounteredStep = -1;
          }
        }
        
        // Application of resolve's changes to the structure must be done after the evaluation
        // to avoid concurrent iterator update.
        if (problemAssigns != null) {
            for (PredicateAssign pa : problemAssigns) {
                pa.apply();
            }
        }

        AdvancedCoerce.stat_coerceBasicIters += FormulaIterator.stat_TotalEvals - savedEvals;
        return total;
      }
    
    private int coerceIncremental(TVS structure, NodeValueMap oldChanges, NodeValueMap newChanges, boolean firstTime) {
        float savedEvals = FormulaIterator.stat_TotalEvals;
        
        int total = AdvancedCoerce.Unmodified;
        
        List<EvalLiteral> literals = this.literals;
        int numberOfSteps = literals.size();
        
        Iterator<AssignKleene>[] stepIt = this.stepIt;
        List<PredicateAssign> problemAssigns = null;
        
        for (EvalLiteral pivot : literals) {
          if (pivot.ispredicate) {
              if (pivot.samePredicate)
                  continue;
              Predicate predicate = pivot.predicate;
              Collection<NodeValue> nodeValues = oldChanges.get(predicate);
              if (nodeValues == null)
                  continue;

              AssignKleene initial = pivot.getInitialAssign();
              
              if (pivot.pivotUnknownOnly) {
                  stepIt[0] = new IncrementalRestrictedPredicateIterator((PredicateFormula)pivot.subFormula, 
                            nodeValues, Kleene.unknownKleene, initial);
              }
              else {
                  stepIt[0] = new IncrementalPredicateIterator((PredicateFormula)pivot.subFormula, 
                            nodeValues, pivot.negated, pivot.head, pivot.skipAdded, initial);
              }
          }
          else if (pivot.equality) {
              if (pivot.samePredicate)
                  continue;
              
              AssignKleene initial = pivot.getInitialAssign();

              Iterator<AssignKleene> iteratorNeq = null;
              if (firstTime && pivot.negated) {
                  Collection<Collection<Node>> inequalities = oldChanges.getInequalities();
                  if (inequalities != null) {
                      iteratorNeq = new IncrementalInequalityIterator((EqualityFormula)pivot.subFormula, inequalities, initial);
                  }
              }              
              Iterator<AssignKleene> iteratorEq = null;
              if (!pivot.negated || pivot.head) {
                  Collection<NodeValue> nodeValues = oldChanges.get(Vocabulary.sm);
                  if (nodeValues != null) {
                      iteratorEq = new IncrementalEqualityIterator((EqualityFormula)pivot.subFormula,
                              nodeValues, pivot.negated, pivot.head, initial);                      
                  }
              }
              if (iteratorEq == null) {
                  if (iteratorNeq == null)
                      continue;
                  else 
                      stepIt[0] = iteratorNeq;                      
              } else {
                  if (iteratorNeq == null) 
                      stepIt[0] = iteratorEq;                      
                  else 
                      stepIt[0] = new ConcatIterator<AssignKleene>(iteratorEq, iteratorNeq);
              }
          }
          else continue;
        
          int currentStep = 0;
          int unknownEncounteredStep = -1;
          EvalLiteral currentHead = null;

          OUTER: while (currentStep >= 0) {
            Iterator<AssignKleene> currentIterator = stepIt[currentStep];
            if (currentIterator.hasNext()) {
                currentAssign = currentIterator.next();
                if (currentAssign.kleene == Kleene.unknownKleene) {
                    unknownEncounteredStep = currentStep;
                    if (currentStep == 0)
                        currentHead = pivot;
                    else
                        currentHead = literals.get(currentStep - 1);
                }
                if (currentStep == numberOfSteps) {
                  // We get here only if (goal_1 ^ ... ^ goal_n ^ !head) are all true,
                  // that means the constraint is violated.

                  for (Var var : currentAssign.bound()) {
                    Node node = currentAssign.get(var);
                    if (!structure.eval(Vocabulary.active, node).equals(Kleene.trueKleene)) {
                      continue OUTER;
                    }
                  }
                  if (unknownEncounteredStep >= 0 && !currentHead.head) throw new RuntimeException("Try to change non head predicate!");

                  PredicateAssign newAssign = null;
                  int result = (unknownEncounteredStep >= 0)?
                                currentHead.resolve(structure, 
                                        currentAssign, newAssign = new PredicateAssign()) :
                                AdvancedCoerce.Invalid;

                  switch (result) {
                  case AdvancedCoerce.Invalid:
                        AdvancedCoerce.stat_coerceIncrementalIters += FormulaIterator.stat_TotalEvals - savedEvals;
                          return AdvancedCoerce.Invalid;
                  case AdvancedCoerce.Modified:
                        if (!newChanges.putAndCheck(newAssign)) {
                            if (problemAssigns == null)
                                problemAssigns = new LinkedList<PredicateAssign>();
                            problemAssigns.add(newAssign);
                        }
                            total = AdvancedCoerce.Modified;
                  }
        
                } else {
                  EvalLiteral el = literals.get(currentStep);
                  Formula formula = el.formula;

                  currentStep++;

                  // Notice that these are reference comparisons - there is no need
                  // to use the expensive equals comparison.
                  if (el == pivot) {
                      // Do nothing, because we've already iterated on the pivot
                      // assignments
                      currentAssign.kleene = Kleene.trueKleene;
                      stepIt[currentStep] = SingleIterator.instance(currentAssign);
                  }
                  else if (el.head && unknownEncounteredStep == -1) {
                      if (pivot.nonPivotUnknownOnly && el == pivot.relatedLiteral)
                          stepIt[currentStep] = formula.assignments(structure, currentAssign, Kleene.unknownKleene);
                      else
                          stepIt[currentStep] = formula.assignments(structure, currentAssign);
                    
                  } else {
                      if (pivot.nonPivotUnknownOnly && el == pivot.relatedLiteral) {
                          currentStep--;
                          continue;
                      }
                      stepIt[currentStep] = formula.assignments(structure, currentAssign, Kleene.trueKleene);
                    
                  }
                }
            } else {
                // backtrack to previous assignment iterator
                currentStep--;
                if (currentStep <= unknownEncounteredStep)
                    unknownEncounteredStep = -1;
            }
          }
        }

        // Application of resolve's changes to the structure must be done after the evaluation
        // to avoid concurrent iterator update.
        if (problemAssigns != null) {
            for (PredicateAssign pa : problemAssigns) {
                pa.apply();
            }
        }
        
        AdvancedCoerce.stat_coerceIncrementalIters += FormulaIterator.stat_TotalEvals - savedEvals;
        return total;
    }

    public boolean isActive(TVS structure) {
        DynamicVocabulary active = structure.getVocabulary();
        return vocabulary.subsetof(active);
    }
    
    @Override
    public int compareTo(MultiConstraint other) {
        if (this.complex && !other.complex) {
            return 1;
        }
        if (!this.complex && other.complex) {
            return -1;
        }
        // Minimal number of variables
        int thisSize = this.body.freeVars().size() + this.body.boundVars().size();
        int otherSize = other.body.freeVars().size() + other.body.boundVars().size();
        int delta = thisSize - otherSize;
        if (delta != 0) {
            return delta;
        }
        return id - other.id;
    }    
}

class PredicateConstraintsTranslator {

    Map<Predicate, BitSet> map;
    int size;
    
    public PredicateConstraintsTranslator(int size) {
        map = HashMapFactory.make();
        this.size = size;
    }
    
    public PredicateConstraintsTranslator(Set<MultiConstraint> constraints) {
        size = constraints.size();
        map = HashMapFactory.make((int)(size / .75));
        for (MultiConstraint mc : constraints) {
            assert(mc.getId() < size);
            addConstraint(mc);
        }
    }
    
    public void addConstraint(MultiConstraint mc) {
        for (Predicate predicate : mc.predicates) {
            BitSet bs = map.get(predicate);
            if (bs == null) {
                bs = new BitSet(size);
                map.put(predicate, bs);
            }
            bs.set(mc.getId());
        }
    }
    
    public BitSet getConstraints(Predicate p) {
        return map.get(p);
    }
    
    public BitSet getConstraints(Collection<Predicate> set) {
        BitSet result = new BitSet(size);
        for (Predicate predicate : set) {
            BitSet bs = map.get(predicate);
            if (bs != null)
                result.addAll(bs);
        }
        return result;
    }
    
}

/**
 * Literal representing a high-level formula in a conjunction. 
 * Used for fast multi-constraint evaluation
 * @author igor
 */
final class EvalLiteral {

    boolean negated = false;
    boolean ispredicate = false;
    boolean equality = false;
    boolean complex = false;
    boolean constant = false;
    boolean head = false;
    
    Formula formula;
    Formula subFormula;
    
    Predicate predicate;
    Var left;
    Var right;
    
    Collection<MultiConstraint> strongDependents;
    Collection<Identifiable> nonStrongDependents;
    BitSet nonStrongDependentsBits;
    
    public EvalLiteral(Formula f) {
        formula = f;
        if (f instanceof NotFormula) {
            negated = true;
            subFormula = ((NotFormula)f).subFormula();
        }
        else {
            subFormula = f;
        }
        if (subFormula instanceof PredicateFormula) {
            ispredicate = true;
            predicate = ((PredicateFormula)subFormula).predicate();
            predicate.rank++;
        }
        else if (subFormula instanceof EqualityFormula) {
            equality = true;
            left = ((EqualityFormula)subFormula).left();
            right = ((EqualityFormula)subFormula).right();
        }
        else if (subFormula instanceof ValueFormula) {
            constant = true;
        }
        else {
            complex = true;
        }
        
        AssignPrecomputed tempAssign = new AssignPrecomputed();
        Collection<Var> freeVars = formula.freeVars();
        precomputedAssign = tempAssign.instanceForIterator(freeVars, freeVars.isEmpty());
    }
    
    final public boolean equals(EvalLiteral el) {
        return formula.equals(el.formula);
    }
    
    public String toString() {
        return (head ? "<" : "") + formula + (head ? ">" : "");
    }

    /**
     * Resolve unknown value in head literal
     * @param structure
     * @param trueAssign
     * @param newAssign
     * @return
     */
    public int resolve(TVS structure, Assign trueAssign, PredicateAssign newAssign) {
        if (constant) {
            // Constraint breached. No way to repair.
            return AdvancedCoerce.Invalid;
        }
        else if (ispredicate) {
            NodeTuple tuple = NodeTuple.EMPTY_TUPLE;
            PredicateFormula predicateFormula = (PredicateFormula)subFormula;
            
            if (predicate.arity() > 0) { // building the tuple for the truth assignment
                tuple = predicateFormula.makeTuple(trueAssign);
            }
            
            newAssign.copy(structure, predicate, tuple, 
                           !negated ? Kleene.falseKleene : Kleene.trueKleene);
            
            return modify();
        }
        else if (equality) {
            if (!negated) {
                // Constraint breached. No way to repair.
                return AdvancedCoerce.Invalid;
            }
            else {
                // Fix the problem. This is no longer a summary node.
                newAssign.copy( structure, 
                        Vocabulary.sm, trueAssign.get(left), Kleene.falseKleene);
                
                return modify();
            }
        }
        else {
            throw new RuntimeException("addConstraint should have handled this case.");
        }
    }
    
    public AdvancedConstraint constraint = null;
    BaseTVS structure;
    
    // Various precomputed optimization hints 
    boolean samePredicate = false;
    EvalLiteral relatedLiteral = null;
    boolean pivotUnknownOnly = false;
    boolean nonPivotUnknownOnly = false;
    boolean skipAdded = false;
    
    final public void setHead(AdvancedConstraint ac) {
        head = true;
        this.constraint = ac;
    }

    private boolean modified = false;
    
    private AssignKleene precomputedAssign;
    
    final AssignKleene getInitialAssign() {
        // return new AssignKleene(Kleene.falseKleene);
        return precomputedAssign;
    }
    
    final public boolean isModified() {
        return modified;
    }
    
    final public boolean testAndReset() {
        boolean result = modified;
        modified = false;
        return result;
    }
    
    final private int modify() {
        modified = true;
        return AdvancedCoerce.Modified;
    }
    
    final public boolean isPredicate() {
        return ispredicate;
    }

    final public boolean isEquality() {
        return equality;
    }

    final public boolean isComplex() {
        return complex;
    }    
}
