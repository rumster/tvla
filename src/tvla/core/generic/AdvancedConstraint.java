package tvla.core.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.core.TVS;
import tvla.core.generic.GenericCoerce.Constraint;
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
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;

/**
 * @author Tal Lev-Ami
 */
public class AdvancedConstraint extends GraphNode<AdvancedConstraint> {
    ConstraintBody body;

    Literal head;
    /*
     * Set dependents = HashSetFactory.make(); Collection nonStrongDependents;
     * //Set strongDependents = HashSetFactory.make(); Collection
     * strongDependents; Collection strongBackDependents;
     * 
     * // This is a temporary set. Used in creating the topological order. Set
     * dependsOn = HashSetFactory.make();
     * 
     * // The position of the constraint in the topological order. int id = 0;
     */
    // If the body is extended horn, this is the order in which the formula
    // should be evaluated for maximum efficiency.
    List<Formula> evalOrder = null;

    // If not null - this is the modified head formula. It needs be satisfied
    // and not necessarily true.
    Formula evalHead = null;

    /*
     * public int getId() { return id; }
     */
    public boolean isComplex() {
        return body.complex;
    }

    /*
     * public int compareTo(Object o) { AdvancedConstraint other =
     * (AdvancedConstraint) o; return id - other.id; }
     */
    class OrderCalculator {
        public List<Formula> evalOrder = new ArrayList<Formula>();
        public Formula evalHead = null;

        List<Formula> nullary = new ArrayList<Formula>();
        List<Formula> unary = new ArrayList<Formula>();
        List<Formula> negatedUnary = new ArrayList<Formula>();
        List<Formula> binary = new ArrayList<Formula>();
        List<Formula> negatedBinary = new ArrayList<Formula>();
        List<Formula> kary = new ArrayList<Formula>();
        List<Formula> negatedKary = new ArrayList<Formula>();
        List<Formula> equality = new ArrayList<Formula>();
        List<Formula> negatedEquality = new ArrayList<Formula>();
        List<Formula> otherFormula0 = new ArrayList<Formula>();
        List<Formula> otherFormula1 = new ArrayList<Formula>();
        List<Formula> otherFormula2 = new ArrayList<Formula>();
        List<Formula> otherFormula3 = new ArrayList<Formula>();
        List<Formula> negatedFormula1 = new ArrayList<Formula>();
        List<Formula> negatedFormula2 = new ArrayList<Formula>();
        List<Formula> negatedFormula3 = new ArrayList<Formula>();

        ConstraintBody body;
        Literal head;

        OrderCalculator(AdvancedConstraint constraint) {
            body = constraint.body;
            head = constraint.head;

            processAll();

            // Best with predicate negation
            evalOrder.addAll(nullary);
            evalOrder.addAll(otherFormula0);
            evalOrder.addAll(unary);
            evalOrder.addAll(binary);
            evalOrder.addAll(equality);
            evalOrder.addAll(negatedBinary);
            evalOrder.addAll(negatedUnary);
            evalOrder.addAll(otherFormula1);
            evalOrder.addAll(otherFormula2);
            evalOrder.addAll(otherFormula3);
            evalOrder.addAll(kary);
            evalOrder.addAll(negatedFormula3);
            evalOrder.addAll(negatedFormula2);
            evalOrder.addAll(negatedFormula1);
            evalOrder.addAll(negatedEquality);
            evalOrder.addAll(negatedKary);
        }

        void processEquality(Formula formula, boolean negated) {
            if (negated)
                negatedEquality.add(formula);
            else
                equality.add(formula);
        }

        void processPredicate(Formula formula, boolean negated, PredicateFormula predicateFormula) {
            switch (predicateFormula.predicate().arity()) {
            case 0:
                if (negated)
                    nullary.add(formula);
                else
                    nullary.add(formula);
                break;
            case 1:
                if (negated)
                    negatedUnary.add(formula);
                else
                    unary.add(formula);
                break;
            case 2:
                if (negated)
                    negatedBinary.add(formula);
                else
                    binary.add(formula);
                break;
            default:
                if (negated)
                    negatedKary.add(formula);
                else
                    kary.add(formula);
            }
        }

        void processAny(Formula f) {
            int nFree = f.freeVars().size();
            if (nFree == 0) {
                otherFormula0.add(f);
            } else if (nFree == 1) {
                if (f instanceof NotFormula) {
                    negatedFormula1.add(f);
                } else {
                    otherFormula1.add(f);
                }
            } else if (nFree == 2) {
                if (f instanceof NotFormula) {
                    negatedFormula2.add(f);
                } else {
                    otherFormula2.add(f);
                }
            } else {
                if (f instanceof NotFormula) {
                    negatedFormula3.add(f);
                } else {
                    otherFormula3.add(f);
                }
            }
        }

        void processHead() {
            // Add the negation of the head to the formula.
            if (head.negated) {
                evalHead = head.atomic;
            } else {
                evalHead = new NotFormula(head.atomic);
            }

            if (head.atomic instanceof EqualityFormula) {
                processEquality(evalHead, !head.negated);
            } else if (head.atomic instanceof PredicateFormula) {
                processPredicate(evalHead, !head.negated, (PredicateFormula) head.atomic);
            }
        }

        boolean process(Formula formula, boolean _negated) {
            boolean negated = _negated ^ body.negated;
            if (formula instanceof NotFormula) {
                negated = !negated;
                formula = ((NotFormula) formula).subFormula();
            }
            if (formula instanceof ValueFormula) {
                ValueFormula vformula = (ValueFormula) formula;
                Kleene value = vformula.value();
                if (negated)
                    value = Kleene.not(value);
                if (value == Kleene.trueKleene) {
                    return true;
                } else {
                    evalOrder.add(new ValueFormula(value));
                    return false;
                }
            }
            Formula nformula;
            if (negated)
                nformula = new NotFormula(formula);
            else
                nformula = formula;

            if (formula instanceof EqualityFormula) {
                processEquality(nformula, negated);
            } else if (formula instanceof PredicateFormula) {
                processPredicate(nformula, negated, (PredicateFormula) formula);
            } else {
                processAny(nformula);
            }
            return true;
        }

        void processAll() {
            if (!body.complex) {
                for (Iterator<Literal> it = body.literals.iterator(); it.hasNext();) {
                    Literal literal = (Literal) it.next();
                    if (!process(literal.atomic, literal.negated))
                        return;
                }
            } else {
                // FIXME: change back?
                // if (!body.negated && body.formula instanceof AndFormula) {
                if (!body.negated) {
                    List<Formula> cnf = new ArrayList<Formula>();
                    body.formula.toCNFArray(cnf);
                    for (Iterator<Formula> it = cnf.iterator(); it.hasNext();) {
                        Formula formula = it.next();
                        if (!process(formula, false))
                            return;
                    }
                } else {

                    processAny(body.formula);
                }
            }

            processHead();
        }
    };

    AdvancedConstraint(Formula body, Formula head) {
        this.body = new ConstraintBody(body);
        this.head = new Literal(head);

        // calculateOptimalOrder2();
        OrderCalculator calc = new OrderCalculator(this);
        evalOrder = calc.evalOrder;
        evalHead = calc.evalHead;
    }

    Constraint base = null;

    public Iterator<Map.Entry<Predicate, Collection<TransitiveFormula>>> allTCIterator() {
        return body.allTC.entrySet().iterator();
    }

    public void setBaseConstraint(Constraint base) {
        this.base = base;
    }

    public Constraint getBase() {
        return base;
    }

    public boolean safe(ConstraintBody otherBody, Literal otherHead) {
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
        Literal onlyThisLiteral = (Literal) onlyThis.iterator().next();
        Literal onlyOtherLiteral = (Literal) onlyOther.iterator().next();
        if (!onlyOtherLiteral.equals(head))
            return false;
        if (!onlyThisLiteral.atomic.equals(otherHead.atomic))
            return false;
        return onlyThisLiteral.negated != otherHead.negated;
    }

    void calculateDependents(Collection<AdvancedConstraint> constraints) {
        for (Iterator<AdvancedConstraint> i = constraints.iterator(); i.hasNext();) {
            AdvancedConstraint other = i.next();
            // TODO: make sure this is right. Add safety calculation only when
            // appropriate.
            if (other.body.dependsOn(head) && !safe(other.body, other.head)) {
                // if (other.body.dependsOn(head)) {
                this.dependents.add(other);
                other.dependsOn.add(this);
            }
        }
    }

    /*
     * public void setStrongDependents(Collection deps) { strongDependents = new
     * LinkedList(deps); strongBackDependents = new LinkedList(); for (Iterator
     * itDeps = strongDependents.iterator(); itDeps.hasNext();) {
     * AdvancedConstraint dependent = (AdvancedConstraint)itDeps.next(); if
     * (dependent.id < id) strongBackDependents.add(dependent); } Set
     * nonStrongDependents = HashSetFactory.make(dependents);
     * nonStrongDependents.removeAll(strongDependents); this.nonStrongDependents
     * = new LinkedList(nonStrongDependents); }
     */
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

/**
 * @author Tal Lev-Ami
 */
class ConstraintBody {
    boolean negated = false;

    Collection<Literal> literals = new ArrayList<Literal>();

    Formula formula;

    Map<Predicate, Collection<TransitiveFormula>> allTC = HashMapFactory.make();

    boolean horn = false;
    boolean complex = false;

    Set<Predicate> predicates = HashSetFactory.make();

    Set<Formula> badTC = HashSetFactory.make();

    public boolean dependsOn(Literal head) {
        if (head.atomic instanceof ValueFormula)
            return false;

        // The following assumes pushBackNegations was called
        // on the constraint and we are only interested in
        // dependencies that result in modification (not detection
        // of invalid constraint).

        for (Iterator<Literal> i = literals.iterator(); i.hasNext();) {
            Literal literal = i.next();
            boolean negated = literal.negated ^ this.negated;
            if ((literal.atomic instanceof EqualityFormula) && (head.atomic instanceof EqualityFormula)) {
                // CHANGE-1:
                // if (head.negated == negated)
                if (!head.negated && (!negated || literal.complex))
                    return true;
            } else if ((literal.atomic instanceof PredicateFormula) && (head.atomic instanceof PredicateFormula)) {
                if (((PredicateFormula) literal.atomic).predicate()
                        .equals(((PredicateFormula) head.atomic).predicate())
                        // && (head.negated == negated))
                        // FIXME: Do we really handle all dependency cases? What
                        // about complex
                        // constraints?
                        && ((head.negated == negated) || literal.complex))
                    // )
                    return true;
            }
        }

        return false;
    }

    private boolean traverse(Formula formula, boolean negated, boolean complex) {
        if (formula instanceof ExistQuantFormula) {
            ExistQuantFormula eformula = (ExistQuantFormula) formula;
            if (!complex)
                complex = negated;
            return traverse(eformula.subFormula(), negated, complex);
        } else if (formula instanceof AndFormula) {
            AndFormula aformula = (AndFormula) formula;
            if (!complex)
                complex = negated;
            boolean leftHorn = traverse(aformula.left(), negated, complex);
            boolean rightHorn = traverse(aformula.right(), negated, complex);
            return leftHorn && rightHorn;
        } else if (formula instanceof NotFormula) {
            NotFormula nformula = (NotFormula) formula;
            traverse(nformula.subFormula(), !negated, complex);
            return (nformula.subFormula() instanceof AtomicFormula);
        } else if (formula instanceof AtomicFormula) {
            if (formula instanceof PredicateFormula) {
                PredicateFormula pformula = (PredicateFormula) formula;
                predicates.add(pformula.predicate());
            }
            Literal literal = new Literal((AtomicFormula) formula, negated);
            literal.complex = complex;
            literals.add(literal);
            return true;
        }

        if (formula instanceof AllQuantFormula) {
            AllQuantFormula aformula = (AllQuantFormula) formula;
            traverse(aformula.subFormula(), negated, true);
        } else if (formula instanceof OrFormula) {
            OrFormula oformula = (OrFormula) formula;
            traverse(oformula.left(), negated, true);
            traverse(oformula.right(), negated, true);
        } else if (formula instanceof EquivalenceFormula) {
            EquivalenceFormula eformula = (EquivalenceFormula) formula;
            traverse(eformula.left(), negated, true);
            traverse(eformula.left(), !negated, true);
            traverse(eformula.right(), negated, true);
            traverse(eformula.right(), !negated, true);
        } else if (formula instanceof IfFormula) {
            IfFormula iformula = (IfFormula) formula;
            traverse(iformula.condSubFormula(), negated, true);
            traverse(iformula.condSubFormula(), !negated, true);
            traverse(iformula.trueSubFormula(), negated, true);
            traverse(iformula.falseSubFormula(), negated, true);
        } else if (formula instanceof TransitiveFormula) {
            TransitiveFormula tformula = (TransitiveFormula) formula;
            if (tformula.subFormula() instanceof PredicateFormula
                    && ((PredicateFormula) tformula.subFormula()).predicate().arity() == 2) {
                PredicateFormula bformula = (PredicateFormula) tformula.subFormula();

                if (bformula.getVariable(0).equals(tformula.subLeft())
                        && bformula.getVariable(1).equals(tformula.subRight())) {
                    Collection<TransitiveFormula> theTCs = allTC.get(bformula.predicate());
                    if (theTCs == null) {
                        theTCs = new ArrayList<TransitiveFormula>();
                        allTC.put(bformula.predicate(), theTCs);
                    }
                    theTCs.add(tformula);
                } else {
                    badTC.add(tformula);
                }
            } else {
                badTC.add(tformula);
            }

            traverse(tformula.subFormula(), negated, true);
        }
        return false;
    }

    public ConstraintBody(Formula formula) {
        this.formula = formula;
        if (formula instanceof NotFormula) {
            formula = ((NotFormula) formula).subFormula();
            negated = true;
        }
        horn = traverse(formula, false, false);
        complex = !horn || (negated && literals.size() > 1);
        // complex = !horn || (negated && !(formula instanceof AtomicFormula));
    }

    public String toString() {
        return (negated ? "!" : "") + literals;
    }

}
