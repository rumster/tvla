package tvla.formulae;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignIterator;
import tvla.core.assignments.AssignKleene;
import tvla.core.assignments.AssignPrecomputed;
import tvla.logic.Kleene;
import tvla.predicates.Vocabulary;
import tvla.util.HashMapFactory;

/**
 * A ForAll quanitifier sub formula
 * 
 * @author Tal Lev-Ami
 */
final public class AllQuantFormula extends QuantFormula {
    AssignPrecomputed assignFactory;

    /** Create a copy of the formula */
    public Formula copy() {
        return new AllQuantFormula(boundVariable, subFormula.copy());

    }

    /** Create a new ForAll quantified formula. */
    public AllQuantFormula(Var boundVariable, Formula subFormula) {
        super(boundVariable, subFormula);
        assignFactory = new AssignPrecomputed();
    }

    /** Evaluate the formula on the given structure and assignment. */
    public Kleene eval(TVS s, Assign assign) {
        Assign localAssign = assignFactory.instanceForIterator(assign, boundVars(), true);

        Kleene result = Kleene.trueKleene;
        localAssign.addVar(boundVariable);
        // Compute the three valued logical conjunction on the value of the sub
        // formula for all the possible assignments into the bound variable.
        Iterator<Entry<NodeTuple, Kleene>> activeIt = s.iterator(Vocabulary.active);
        while (activeIt.hasNext()) {
            Entry<NodeTuple, Kleene> entry = activeIt.next();
            Node node = (Node) entry.getKey();
            localAssign.putNode(boundVariable, node);
            result = Kleene.and(result, subFormula.eval(s, localAssign));
            if (result == Kleene.falseKleene)
                result = Kleene.not(entry.getValue());

            if (result == Kleene.falseKleene)
                return result;
        }

        return result;
    }
    
    /** Return a human readable representation of the formula. */
    public String toString() {
        return "((A " + boundVariable.toString() + ")." + subFormula.toString() + ")";
    }

    /** Equate the this formula with the given formula by structure. */
    public boolean equals(Object o) {
        if (!(o instanceof AllQuantFormula))
            return false;
        return super.equals(o);
    }

    /**
     * Calls the specific accept method, based on the type of this formula
     * (Visitor pattern).
     * 
     * @author Roman Manevich.
     * @since tvla-2-alpha November 18 2002, Initial creation.
     */
    @Override
    public <T> T visit(FormulaVisitor<T> visitor) {
        return visitor.accept(this);
    }

    /***
     * added constant 17 to differentiate AllQuant from ExistQuant
     * 
     * @since 9/10/02 Eran Yahav, hashCode fix for bug in TVLA-alpha-2.
     */
    public int hashCode() {
        int result = 17;
        if (!alphaRenamingEquals) {
            result += subFormula.hashCode() * 31;
            result += boundVariable.hashCode();
        } else {
            result = ignoreVarHashCode();
        }
        return result;
    }

    public int ignoreVarHashCode() {
        int result = 17;
        result += subFormula.ignoreVarHashCode() * 31;
        return result;
    }

    /**
     * @param formula
     * @return
     * @author Greta Yorsh
     */
    public static Formula close(Formula formula1) {
        Formula formula = formula1;
        for (Iterator iter = formula1.freeVars().iterator(); iter.hasNext();) {
            Var var = (Var) iter.next();
            formula = new AllQuantFormula(var, formula);
        }
        return formula;
    }

    public Formula pushBackNegations(boolean negated) {
        if (!negated) {
            subFormula = subFormula.pushBackNegations(false);
            return this;
        } else {
            return new ExistQuantFormula(boundVariable, subFormula.pushBackNegations(true));

        }
    }

    public Formula pushBackQuant(Var bound, boolean allQuant) {
        if (bound != null) {
            subFormula = subFormula.pushBackQuant(bound, allQuant);
            freeVars = boundVars = null;
            return this;
        } else {
            return subFormula.pushBackQuant(boundVariable, true);
        }
    }

    public FormulaIterator assignments(TVS structure, Assign partial, Kleene value) {
        return new FormulaIterator(structure, this, partial, value) {
            int trueNodes = 0;
            int totalNodes = 0;

            public boolean step() {
                if (assignIterator == null) {
                    stat_QuantAssigns++;
                    // result.put(partial);

                    // if (partial.containsAll(formula.freeVars())) {
                    if (buildFullAssignment()) {
                        assignIterator = new AssignIterator();
                        result.kleene = formula.eval(structure, result);
                        stat_Evals++;
                        return checkDesiredValue(result.kleene);
                    } else {
                        Map<NodeTuple, int[]> assignsMap = HashMapFactory.make();
                        // Map assignsMap = new LinkedHashMap();
                        Collection<Var> freeVars = formula.freeVars();
                        // FIXME?
                        // result.addVars(freeVars);

                        Iterator<AssignKleene> subAssigns = subFormula.assignments(structure, partial);
                        while (subAssigns.hasNext()) {
                            AssignKleene assign = subAssigns.next();
                            NodeTuple nt = assign.makeTuple(freeVars);
                            int[] val;
                            val = assignsMap.get(nt);
                            if (val == null) {
                                val = new int[2];
                                val[0] = 0;
                                val[1] = 0;
                            }
                            if (assign.kleene != Kleene.falseKleene)
                                val[0]++;
                            if (assign.kleene == Kleene.trueKleene)
                                val[1]++;
                            assignsMap.put(nt, val);
                        }

                        Kleene r;
                        // TODO: do it quicker by getting size of the active
                        // predicate list?
                        Iterator<Map.Entry<NodeTuple, Kleene>> iter = structure.iterator(
                                Vocabulary.active);
                        while (iter.hasNext()) {
                            Map.Entry<NodeTuple, Kleene> entry = iter.next();
                            r = entry.getValue();
                            if (r == Kleene.trueKleene) {
                                trueNodes++;
                                totalNodes++;
                            } else if (r == Kleene.unknownKleene) {
                                totalNodes++;
                                throw new RuntimeException("Doesn't really work!!!!");
                            }
                        }

                        assignIterator = assignsMap.entrySet().iterator();
                        if (assignIterator.hasNext()) {
                            result.addVars(freeVars);
                        }
                        stat_NonEvals++;
                    }
                }

                while (assignIterator.hasNext()) {
                    Map.Entry<NodeTuple, int[]> entry = (Map.Entry<NodeTuple, int[]>) assignIterator.next();
                    int[] val = entry.getValue();
                    if (val[1] >= totalNodes)
                        result.kleene = Kleene.trueKleene;
                    else if (val[0] >= trueNodes)
                        result.kleene = Kleene.unknownKleene;
                    else
                        result.kleene = Kleene.falseKleene;
                    if (checkDesiredValue(result.kleene)) {
                        NodeTuple tuple = entry.getKey();
                        result.putTuple(formula.freeVars(), tuple);
                        return true;
                    }
                }
                return false;
            }
        };
    }

}
