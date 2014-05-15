package tvla.formulae;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.base.PredicateUpdater;
import tvla.exceptions.SemanticErrorException;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.HashSetFactory;
import tvla.util.StringUtils;

/**
 * A formula used to update a structure's predicate.
 * 
 * @author Roman Manevich.
 * @since 4.9.2001 Initial creation.
 */
final public class PredicateUpdateFormula extends UpdateFormula {
    protected Predicate predicate;
    public Var[] variables;
    protected boolean auto;
    private int cachedArity, firstVarId, secondVarId;
    
    /** Returns a copy of the list of arguments of the
     * predicate updated by the formula.
     * 
     * @return An array of variables.
     */
    public Var[] getCopyOfArguments() {
    	return variables == null ? new Var[0] : variables.clone();
    }

    private void initPredicate(Predicate predicate) {
        this.predicate = predicate;
        cachedArity = predicate.arity();
    }

    public PredicateUpdateFormula(Formula updateFormula, Predicate predicateToUpdate, boolean auto) {
        super(updateFormula);
        initPredicate(predicateToUpdate);
        this.auto = auto;
        if (predicate.arity() != 0)
            throw new RuntimeException("Attempt to create a nullary update formula : " + updateFormula
                    + " with the predicate " + predicateToUpdate + " of arity " + predicateToUpdate.arity());
    }

    public PredicateUpdateFormula(Formula updateFormula, Predicate predicateToUpdate) {
        this(updateFormula, predicateToUpdate, false);
    }

    /**
     * @param v
     *            The variable on the left hand side of the formula.
     * @author Tal Lev-Ami
     * @since 19.4.2001 Added free variables from the left-hand side of the
     *        update formula to the formula's free variables set to solve a bug.
     *        The formula is copied so the new formula can have a different free
     *        variables set.
     */
    public PredicateUpdateFormula(Formula formula, Predicate unaryPredicate, Var v, boolean auto) {
        super(formula);
        initPredicate(unaryPredicate);
        this.auto = auto;
        if (predicate.arity() != 1)
            throw new RuntimeException("Attempt to create a unary update formula : " + formula + " with the predicate "
                    + predicate + " of arity " + predicate.arity());
        // this.formula = formula.copy();
        this.variables = new Var[1];
        this.variables[0] = v;
        if (this.variables[0] != null) {
            Set<Var> freeVars = Collections.singleton(variables[0]);
            try {
                this.formula.addAdditionalFreeVars(freeVars);
            } catch (RuntimeException e) {
                String message = e.getMessage() + StringUtils.newLine + "while creating the predicate update formula "
                        + toString();
                throw new SemanticErrorException(message);
            }
        }
        firstVarId = v.id();
    }

    public PredicateUpdateFormula(Formula formula, Predicate unaryPredicate, Var v) {
        this(formula, unaryPredicate, v, false);
    }

    /**
     * @param left
     *            The first variable on the left hand side of the formula.
     * @param left
     *            The second variable on the left hand side of the formula.
     * @author Tal Lev-Ami
     * @since 19.4.2001 Added free variables from the left-hand side of the
     *        update formula to the formula's free variables set to solve a bug.
     *        The formula is copied so the new formula can have a different free
     *        variables set (Roman).
     */
    public PredicateUpdateFormula(Formula formula, Predicate binaryPredicate, Var left, Var right, boolean auto) {
        super(formula);
        initPredicate(binaryPredicate);
        this.auto = auto;
        if (predicate.arity() != 2)
            throw new RuntimeException("Attempt to create a binary update formula : " + formula
                    + " with the predicate " + predicate + " of arity " + predicate.arity());
        // this.formula = formula.copy();
        this.variables = new Var[2];
        this.variables[0] = left;
        this.variables[1] = right;
        firstVarId = left.id();
        secondVarId = right.id();

        Set<Var> freeVars = HashSetFactory.make(2);
        if (this.variables[0] != null)
            freeVars.add(this.variables[0]);
        if (this.variables[1] != null)
            freeVars.add(this.variables[1]);
        if (freeVars.isEmpty() == false)
            this.formula.addAdditionalFreeVars(freeVars);
    }

    public PredicateUpdateFormula(Formula formula, Predicate binaryPredicate, Var left, Var right) {
        this(formula, binaryPredicate, left, right, false);
    }

    public PredicateUpdateFormula(Formula formula, Predicate predicate, Var[] vars, boolean auto) {
        super(formula);
        initPredicate(predicate);
        this.auto = auto;
        if (predicate.arity() != vars.length)
            throw new RuntimeException("Attempt to create a " + vars.length + "-ary update formula : " + formula
                    + " with the predicate " + predicate + " of arity " + predicate.arity());
        // this.formula = formula.copy();
        this.variables = vars;

        Set<Var> freeVars = HashSetFactory.make(this.variables.length);
        for (int i = 0; i < this.variables.length; i++) {
            if (this.variables[i] != null)
                freeVars.add(this.variables[i]);
        }

        if (cachedArity == 1) {
            firstVarId = variables[0].id();
        } else if (cachedArity == 2) {
            firstVarId = variables[0].id();
            secondVarId = variables[1].id();
        }

        if (freeVars.isEmpty() == false)
            this.formula.addAdditionalFreeVars(freeVars);
    }

    public PredicateUpdateFormula(Formula formula, Predicate predicate, Var[] vars) {
        this(formula, predicate, vars, false);
    }

    public PredicateUpdateFormula(Formula formula, Predicate predicate, List<Var> vars, boolean auto) {
        super(formula);
        initPredicate(predicate);
        this.auto = auto;
        if (predicate.arity() != vars.size())
            throw new RuntimeException("Attempt to create a " + vars.size() + "-ary update formula : " + formula
                    + " with the predicate " + predicate + " of arity " + predicate.arity());

        // this.formula = formula.copy();

        this.variables = new Var[vars.size()];
        ListIterator<Var> li = vars.listIterator();
        int index = 0;
        while (li.hasNext()) {
            this.variables[index] = (Var) li.next();
            index++;
        }

        if (cachedArity == 1) {
            firstVarId = variables[0].id();
        } else if (cachedArity == 2) {
            firstVarId = variables[0].id();
            secondVarId = variables[1].id();
        }

        Set<Var> freeVars = HashSetFactory.make(this.variables.length);
        for (int i = 0; i < this.variables.length; i++) {
            if (this.variables[i] != null)
                freeVars.add(this.variables[i]);
        }
        if (freeVars.isEmpty() == false)
            this.formula.addAdditionalFreeVars(freeVars);
    }

    public PredicateUpdateFormula(Formula formula, Predicate predicate, List<Var> vars) {
        this(formula, predicate, vars, false);
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public Var getVariable(int i) {
        return variables[i];
    }

    public int predicateArity() {
        return predicate.arity();
    }

    public boolean getAuto() {
        return auto;
    }

    public void setAuto(boolean auto) {
        this.auto = auto;
    }

    public void update(PredicateUpdater updater, Assign assign, Kleene value) {
        updater.update(makeTuple(assign), value);
    }

    public void update(TVS s, Assign assign, Kleene value) {
        s.update(predicate, makeTuple(assign), value);
    }

    final protected NodeTuple makeTuple(Assign assign) {
        NodeTuple tuple;
        switch (cachedArity) {
        case 0:
            tuple = NodeTuple.EMPTY_TUPLE;
            break;
        case 1:
            tuple = assign.makeTuple(firstVarId);
            break;
        case 2:
            tuple = assign.makeTuple(firstVarId, secondVarId);
            break;
        default:
            tuple = assign.makeTuple(variables);
        }
        return tuple;
    }

    /** Return a human readable representation of the formula. */
    public String toString() {
        int arity = predicate.arity();
        StringBuffer result = new StringBuffer();
        result.append(predicate.toString());
        result.append("(");

        if (arity > 0) {
            result.append(variables[0]);
        }
        for (int i = 1; i < arity; i++) {
            result.append(",");
            result.append(variables[i].toString());
        }
        result.append(") :=");
        result.append(formula.toString());

        return result.toString();
    }
}
