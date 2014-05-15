package tvla.core.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import tvla.core.Constraints;
import tvla.core.Node;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.common.GetFormulaPredicates;
import tvla.core.common.ModifiedPredicates;
import tvla.formulae.AllQuantFormula;
import tvla.formulae.AndFormula;
import tvla.formulae.AtomicFormula;
import tvla.formulae.EqualityFormula;
import tvla.formulae.EquivalenceFormula;
import tvla.formulae.ExistQuantFormula;
import tvla.formulae.Formula;
import tvla.formulae.IfFormula;
import tvla.formulae.NotFormula;
import tvla.formulae.OrFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.TransitiveFormula;
import tvla.formulae.ValueFormula;
import tvla.formulae.Var;
import tvla.formulae.TransitiveFormula.TCCache;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Logger;
import tvla.util.SingleIterator;
import tvla.util.StringUtils;

/**
 * An optimized implementation of Coerce.
 * 
 * @author Tal Lev-Ami.
 */
public class CoerceTVLA2 extends GenericCoerce {
  private static final boolean smartEval = true;

  private static final boolean smartTC = true;

  private static final boolean USE_MODIFIED_PREDICATES = false;

  private Map<Predicate, Collection<TransitiveFormula>> allTC = HashMapFactory.make();

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

  public CoerceTVLA2(Set<Constraints.Constraint> constraints) {
    super(constraints);
    init();
  }
  
  public static void reset() {
	  predicateToConstraints = HashMapFactory.make();	  
  }

  public boolean coerce(TVS structure) {
//    boolean changed = false;
//    System.out.println(structure);

    SortedSet<AdvancedConstraint> workSet = null;
    if (USE_MODIFIED_PREDICATES) {
      if (ModifiedPredicates.getModified().contains(Vocabulary.sm) || ModifiedPredicates.getModified().contains(Vocabulary.active)) {
        workSet = new TreeSet<AdvancedConstraint>(advancedToGeneric.keySet());
      } else {
        workSet = getInitialWorkSetConstraints();
      }
    } else {
      workSet = new TreeSet<AdvancedConstraint>(advancedToGeneric.keySet());
    }

    // TODO: why aren't we doing this lazily?
    for (Iterator<TransitiveFormula> i = calculatorTC.values().iterator(); i.hasNext();) {
      TransitiveFormula calculator = i.next();
      calculator.calculateTC(structure, null);
    }

    while (!workSet.isEmpty()) {
      Iterator<AdvancedConstraint> first = workSet.iterator();
      AdvancedConstraint constraint = first.next();
      first.remove();
      if (!constraint.isActive(structure)) {
          continue;
      }
      
      // TODO: Can we minimize the number of constraints by computing  
      int action = coerce(structure, constraint);
      if (action == Invalid) {
//        System.out.println("Invalid");
        return false;
      }
      if (action == Modified) {
//        changed = true;
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
//    if (changed)
//        System.out.println("Modified");
    return true;
  }

  protected void addConstraint(Formula body, Formula head) {
    Constraint constraint = createConstraint(body, head);
    AdvancedConstraint advanced = new AdvancedConstraint(body, head);
    if (advancedToGeneric == null) {
      advancedToGeneric = HashMapFactory.make();
    }
    advancedToGeneric.put(advanced, constraint);

    // Update the map from predicates to the set of corresponding constraints.
    Set<Predicate> predicates = GetFormulaPredicates.get(body);
    predicates.addAll(GetFormulaPredicates.get(head));
    for (Iterator<Predicate> predIter = predicates.iterator(); predIter.hasNext();) {
      Predicate predicate = predIter.next();
      Collection<AdvancedConstraint> constraints = predicateToConstraints.get(predicate);
      if (constraints == null) {
        constraints = HashSetFactory.make();
        predicateToConstraints.put(predicate, constraints);
      }
      constraints.add(advanced);
    }
    this.constraints.add(createConstraint(body, head));
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

  protected void init() {
    // Initialize dependencies
    for (Iterator<AdvancedConstraint> it = advancedToGeneric.keySet().iterator(); it.hasNext();) {
      AdvancedConstraint constraint = it.next();
      constraint.calculateDependents(advancedToGeneric.keySet());
    }

    // Find a topological order with respect to dependencies.
    Set<AdvancedConstraint> workSet = HashSetFactory.make();
    Set<AdvancedConstraint> sourceSet = HashSetFactory.make();
    Set<AdvancedConstraint> sinkSet = HashSetFactory.make();
    for (Iterator<AdvancedConstraint> it = advancedToGeneric.keySet().iterator(); it.hasNext();) {
      AdvancedConstraint constraint = it.next();
      if (constraint.dependents.size() == 0) {
        sinkSet.add(constraint);
      } else if (constraint.dependsOn.size() == 0) {
        sourceSet.add(constraint);
      } else {
        workSet.add(constraint);
      }
    }
    int id = 1;
    // While there are constaints left with no id.
    while (!sourceSet.isEmpty() || !workSet.isEmpty()) {
      AdvancedConstraint constraint;
      if (sourceSet.isEmpty()) {
        // No sources left and still have graph.
        // This means we have a cycle.
        // Remove the constraint with maximum dependents.
        constraint = workSet.iterator().next();
        for (Iterator<AdvancedConstraint> it = workSet.iterator(); it.hasNext();) {
          AdvancedConstraint candidate = it.next();
          if (candidate.dependents.size() > constraint.dependents.size()) {
            constraint = candidate;
          }
        }

        if (debug) {
          Logger.println("Constraint cycle found, breaking cycle on constraint " + advancedToGeneric.get(constraint));
        }
      } else { // Just take another constraint from the current sources.
        Iterator<AdvancedConstraint> first = sourceSet.iterator();
        constraint = first.next();
        first.remove();
      }
      constraint.id = id++; // Give id.
      // Remove all dependsOn nodes from the dependent graph.
      for (Iterator<AdvancedConstraint> it = constraint.dependents.iterator(); it.hasNext();) {
        AdvancedConstraint dependent = it.next();
        if (dependent.id != 0)
          continue;
        dependent.dependsOn.remove(constraint);
        // If we created a new source, add it to the sourceSet.
        if (dependent.dependsOn.size() == 0)
          sourceSet.add(dependent);
      }
      workSet.remove(constraint);
    }

    // Add all the sinks last after all has settled down.
    for (Iterator<AdvancedConstraint> it = sinkSet.iterator(); it.hasNext();) {
      AdvancedConstraint constraint = it.next();
      constraint.id = id++;
    }

    if (debug) {
      // Print the dependency list.
      Logger.println(StringUtils.addUnderline("Constraints:"));
      for (Iterator it = new TreeMap<AdvancedConstraint, Constraint>(advancedToGeneric).entrySet().iterator(); it.hasNext();) {
        Map.Entry entry = (Map.Entry) it.next();
        AdvancedConstraint constraint = (AdvancedConstraint) entry.getKey();
        Constraint base = (Constraint) entry.getValue();
        Logger.print(constraint.id + ": " + base + "\n    dependents:");
        for (Iterator<AdvancedConstraint> depIt = constraint.dependents.iterator(); depIt.hasNext();) {
          AdvancedConstraint dependent = depIt.next();
          Logger.print(" " + dependent.id);
        }
        Logger.println();
      }
    }

    if (smartTC) {
      // Setup TC calculators.
      for (Iterator i = allTC.entrySet().iterator(); i.hasNext();) {
        Map.Entry entry = (Map.Entry) i.next();
        Predicate predicate = (Predicate) entry.getKey();
        Collection theTCs = (Collection) entry.getValue();

        Var[] binaryPredicateVars = { new Var("_v3"), new Var("_v4") };
        PredicateFormula bpFormula = new PredicateFormula(predicate, binaryPredicateVars);
        TransitiveFormula calculator = new TransitiveFormula(new Var("_v1"), new Var("_v2"), new Var("_v3"), new Var("_v4"),
            bpFormula);

        TCCache calculatedTC = new TCCache();
        calculator.setCalculatedTC(calculatedTC);
        calculatorTC.put(predicate, calculator);
        for (Iterator j = theTCs.iterator(); j.hasNext();) {
          TransitiveFormula theTC = (TransitiveFormula) j.next();
          theTC.setCalculatedTC(calculatedTC);
          theTC.explicitRecalc();
        }
      }
    }
  }

  protected int coerce(TVS structure, AdvancedConstraint constraint) {
    if (constraint.evalOrder == null) {
      return super.coerce(structure, advancedToGeneric.get(constraint));
    }

    int numberOfSteps = constraint.evalOrder.size();

    Iterator[] stepIt = new Iterator[numberOfSteps + 1];
    stepIt[0] = new SingleIterator(Assign.EMPTY);
    
    int currentStep = 0;
    Collection<Assign> problemAssigns = HashSetFactory.make();
    OUTER: while (currentStep >= 0) {
      if (stepIt[currentStep].hasNext()) {
        Assign currentAssign = (Assign) stepIt[currentStep].next();
        if (currentStep == numberOfSteps) {
          Assign problemAssign = new Assign(currentAssign);
          for (Iterator varIt = problemAssign.bound().iterator(); varIt.hasNext();) {

            Var var = (Var) varIt.next();
            Node node = problemAssign.get(var);
            if (structure.eval(Vocabulary.active, node).equals(Kleene.unknownKleene)) {
              continue OUTER;
            }
          }
          // TLA Removed for printing
          //problemAssign.project(constraint.head.atomic.freeVars());
          problemAssigns.add(problemAssign);
        } else {
          Formula formula = constraint.evalOrder.get(currentStep);
          currentStep++;
          // Notice that this is reference comparison - there is no need
          // to use the expensive equals comparison.
          if (formula == constraint.evalHead) {
            stepIt[currentStep] = structure.evalFormula(formula, currentAssign);
          } else {
            stepIt[currentStep] = structure.evalFormulaForValue(formula, currentAssign, Kleene.trueKleene);
          }

        }
      } else {
        // backtrack to previous assignment iterator
        currentStep--;
      }
    }

    int total = Unmodified;
    Constraint baseConstraint = advancedToGeneric.get(constraint);
    
    for (Iterator<Assign> it = problemAssigns.iterator(); it.hasNext();) {

      Assign currentAssign = it.next();
      int result = super.coerce(structure, baseConstraint, currentAssign);
      if (result == Invalid) {
        return Invalid;
      }
      if (result == Modified) {
        total = Modified;
      }
    }
    return total;
  }

  /**
   * @author Tal Lev-Ami
   */
  protected class AdvancedConstraint implements Comparable {
    ConstraintBodyTVLA2 body;

    Literal head;

    Set<AdvancedConstraint> dependents = HashSetFactory.make();

    // This is a temporary set. Used in creating the topological order.
    Set<AdvancedConstraint> dependsOn = HashSetFactory.make();

    // The position of the constraint in the topological order.
    int id = 0;

    // If the body is extended horn, this is the order in which the formula
    // should be evaluated for maximum efficiency.
    List<Formula> evalOrder = null;

    // If not null - this is the modified head formula. It needs be satisfied
    // and not necessarily true.
    Formula evalHead = null;

    public int compareTo(Object o) {
      AdvancedConstraint other = (AdvancedConstraint) o;
      return id - other.id;
    }

    void calculateOptimalOrder() {
      evalOrder = new ArrayList<Formula>();

      List<Formula> nullary = new ArrayList<Formula>();
      List<Formula> unary = new ArrayList<Formula>();
      List<Formula> negatedUnary = new ArrayList<Formula>();
      List<Formula> binary = new ArrayList<Formula>();
      List<Formula> negatedBinary = new ArrayList<Formula>();
      List<PredicateFormula> kary = new ArrayList<PredicateFormula>();
      List<NotFormula> negatedKary = new ArrayList<NotFormula>();
      List<Formula> equality = new ArrayList<Formula>();
      List<Formula> negatedEquality = new ArrayList<Formula>();

      for (Iterator<Literal> it = body.literals.iterator(); it.hasNext();) {
        Literal literal = it.next();
        if (literal.atomic instanceof EqualityFormula) {
          if (literal.negated)
            negatedEquality.add(new NotFormula(literal.atomic));
          else
            equality.add(literal.atomic);
        } else if (literal.atomic instanceof PredicateFormula) {
          PredicateFormula predicateFormula = (PredicateFormula) literal.atomic;
          switch (predicateFormula.predicate().arity()) {
          case 0:
            if (literal.negated)
              nullary.add(new NotFormula(predicateFormula));
            else
              nullary.add(predicateFormula);
            break;
          case 1:
            if (literal.negated)
              negatedUnary.add(new NotFormula(predicateFormula));
            else
              unary.add(predicateFormula);
            break;
          case 2:
            if (literal.negated)
              negatedBinary.add(new NotFormula(predicateFormula));
            else
              binary.add(predicateFormula);
            break;
          default:
            if (literal.negated)
              negatedKary.add(new NotFormula(predicateFormula));
            else
              kary.add(predicateFormula);
          }
        } else if (literal.atomic instanceof ValueFormula) {
          ValueFormula vformula = (ValueFormula) literal.atomic;
          Kleene value = vformula.value();
          if (literal.negated)
            value = Kleene.not(value);
          if (value == Kleene.trueKleene)
            continue;
          else {
            evalOrder.add(new ValueFormula(value));
            return;
          }
        }
      }
      // Add the negation of the head to the formula.
      if (head.atomic instanceof EqualityFormula) {
        if (head.negated) {
          evalHead = head.atomic;
          equality.add(evalHead);
        } else {
          evalHead = new NotFormula(head.atomic);
          negatedEquality.add(evalHead);
        }
      } else if (head.atomic instanceof PredicateFormula) {
        PredicateFormula predicateFormula = (PredicateFormula) head.atomic;
        switch (predicateFormula.predicate().arity()) {
        case 0:
          break;
        case 1:
          if (head.negated) {
            evalHead = predicateFormula;
            unary.add(evalHead);
          } else {
            evalHead = new NotFormula(predicateFormula);
            negatedUnary.add(evalHead);
          }
          break;
        case 2:
          if (head.negated) {
            evalHead = predicateFormula;
            binary.add(evalHead);
          } else {
            evalHead = new NotFormula(predicateFormula);
            negatedBinary.add(evalHead);
          }
          break;
        default:
        }
      }

      // Now take the all the formulas in optimal order:
      // nullary, unary, negatedUnary, binary, negatedBinary, equality,
      // negatedEquality
      evalOrder.addAll(nullary);
      evalOrder.addAll(unary);
      evalOrder.addAll(negatedUnary);
      evalOrder.addAll(binary);
      evalOrder.addAll(negatedBinary);
      evalOrder.addAll(kary);
      evalOrder.addAll(negatedKary);
      evalOrder.addAll(equality);
      evalOrder.addAll(negatedEquality);
    }

    AdvancedConstraint(Formula body, Formula head) {
      this.body = new ConstraintBodyTVLA2(body);
      for (Iterator<Map.Entry<Predicate, Collection<TransitiveFormula>>> i = this.body.allTC.entrySet().iterator(); i.hasNext();) {
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

      this.head = new Literal(head);

      if (this.body.horn && !this.body.negated && smartEval) {
        calculateOptimalOrder();
      }
    }

    public boolean safe(ConstraintBodyTVLA2 otherBody, Literal otherHead) {
      if (!body.negated || otherBody.negated)
        return false;
      if (!body.horn || !otherBody.horn)
        return false;

      Set<Literal> onlyThis = HashSetFactory.make(body.literals);
      onlyThis.removeAll(otherBody.literals);
      Set<Literal> onlyOther = HashSetFactory.make(otherBody.literals);
      onlyOther.removeAll(body.literals);
      if ((onlyThis.size() != 1) || (onlyOther.size() != 1))
        return false;
      Literal onlyThisLiteral = onlyThis.iterator().next();
      Literal onlyOtherLiteral = onlyOther.iterator().next();
      if (!onlyOtherLiteral.equals(head))
        return false;
      if (!onlyThisLiteral.atomic.equals(otherHead.atomic))
        return false;
      return onlyThisLiteral.negated != otherHead.negated;
    }

    void calculateDependents(Collection<AdvancedConstraint> constraints) {
      for (Iterator<AdvancedConstraint> i = constraints.iterator(); i.hasNext();) {
        AdvancedConstraint other = i.next();
        if (other.body.dependsOn(head) && !safe(other.body, other.head)) {
          this.dependents.add(other);
          other.dependsOn.add(this);
        }
      }
    }

    public String toString() {
      return body + "=>" + head;
    }
    
    public boolean isActive(TVS structure) {
        Set<Predicate> activePredicates = structure.getVocabulary().all();
        if (head.atomic instanceof PredicateFormula) {
            PredicateFormula predicateFormula = (PredicateFormula) head.atomic;
          if (!activePredicates.contains(predicateFormula.predicate())) {
                return false;
            }
        }
        for (Literal literal : body.literals) {
            if (literal.atomic instanceof PredicateFormula) {
                PredicateFormula predicateFormula = (PredicateFormula) literal.atomic;
              if (!activePredicates.contains(predicateFormula.predicate())) {
                    return false;
                }
            }
        }
        return true;
    }
  }
}

/**
 * @author Tal Lev-Ami
 */
class ConstraintBodyTVLA2 {
  boolean negated = false;

  Collection<Literal> literals = new ArrayList<Literal>();

  Map<Predicate, Collection<TransitiveFormula>> allTC = HashMapFactory.make();

  boolean horn = false;

  Set<Predicate> predicates = HashSetFactory.make();

  public boolean dependsOn(Literal head) {
    if (head.atomic instanceof ValueFormula)
      return false;
    for (Iterator<Literal> i = literals.iterator(); i.hasNext();) {
      Literal literal = i.next();
      boolean negated = literal.negated ^ this.negated;
      if ((literal.atomic instanceof EqualityFormula) && (head.atomic instanceof EqualityFormula)) {
        if (head.negated == negated)
          return true;
      } else if ((literal.atomic instanceof PredicateFormula) && (head.atomic instanceof PredicateFormula)) {
        if (((PredicateFormula) literal.atomic).predicate().equals(((PredicateFormula) head.atomic).predicate())
            && (head.negated == negated))
          return true;
      }
    }
    return false;
  }

  private boolean traverse(Formula formula, boolean negated) {
    if (formula instanceof ExistQuantFormula) {
      ExistQuantFormula eformula = (ExistQuantFormula) formula;
      return traverse(eformula.subFormula(), negated);
    } else if (formula instanceof AndFormula) {
      AndFormula aformula = (AndFormula) formula;
      boolean leftHorn = traverse(aformula.left(), negated);
      boolean rightHorn = traverse(aformula.right(), negated);
      return leftHorn && rightHorn;
    } else if (formula instanceof NotFormula) {
      NotFormula nformula = (NotFormula) formula;
      traverse(nformula.subFormula(), !negated);
      return (nformula.subFormula() instanceof AtomicFormula);
    } else if (formula instanceof AtomicFormula) {
      if (formula instanceof PredicateFormula) {
        PredicateFormula pformula = (PredicateFormula) formula;
        predicates.add(pformula.predicate());
      }
      literals.add(new Literal((AtomicFormula) formula, negated));
      return true;
    }

    if (formula instanceof AllQuantFormula) {
      AllQuantFormula aformula = (AllQuantFormula) formula;
      traverse(aformula.subFormula(), negated);
    } else if (formula instanceof OrFormula) {
      OrFormula oformula = (OrFormula) formula;
      traverse(oformula.left(), negated);
      traverse(oformula.right(), negated);
    } else if (formula instanceof EquivalenceFormula) {
      EquivalenceFormula eformula = (EquivalenceFormula) formula;
      traverse(eformula.left(), negated);
      traverse(eformula.left(), !negated);
      traverse(eformula.right(), negated);
      traverse(eformula.right(), !negated);
    } else if (formula instanceof IfFormula) {
      IfFormula iformula = (IfFormula) formula;
      traverse(iformula.condSubFormula(), negated);
      traverse(iformula.condSubFormula(), !negated);
      traverse(iformula.trueSubFormula(), negated);
      traverse(iformula.falseSubFormula(), negated);
    } else if (formula instanceof TransitiveFormula) {
      TransitiveFormula tformula = (TransitiveFormula) formula;
      if (tformula.subFormula() instanceof PredicateFormula && ((PredicateFormula) tformula.subFormula()).predicate().arity() == 2) {
        PredicateFormula bformula = (PredicateFormula) tformula.subFormula();

        if (bformula.getVariable(0).equals(tformula.subLeft()) && bformula.getVariable(1).equals(tformula.subRight())) {
          Collection<TransitiveFormula> theTCs = allTC.get(bformula.predicate());
          if (theTCs == null) {
            theTCs = new ArrayList<TransitiveFormula>();
            allTC.put(bformula.predicate(), theTCs);
          }
          theTCs.add(tformula);
        }
      }

      traverse(tformula.subFormula(), negated);
    }
    return false;
  }

  public ConstraintBodyTVLA2(Formula formula) {
    if (formula instanceof NotFormula) {
      formula = ((NotFormula) formula).subFormula();
      negated = true;
    }
    horn = traverse(formula, false);
  }

  public String toString() {
    return (negated ? "!" : "") + literals;
  }

}
