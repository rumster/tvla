package tvla.language.TVP;

import tvla.core.decompose.CloseCycle;
import tvla.exceptions.SemanticErrorException;
import tvla.formulae.Formula;

public class CloseCycleAST extends AST {
    PredicateAST predicate;
    FormulaAST dname;
    
    public CloseCycleAST(PredicateAST predicate, FormulaAST dname) {
        this.dname = dname;
        this.predicate = predicate;
    }

    
    @Override
    public CloseCycleAST copy() {
        return new CloseCycleAST(predicate.copy(), dname.copy());
    }

    @Override
    public void substitute(PredicateAST from, PredicateAST to) {
        dname.substitute(from, to);
        predicate.substitute(from, to);
    }

    @Override
    public void generate() {
        try {
            Formula formula = this.dname.getFormula();
            CloseCycle.addCloseCycle(predicate.getPredicate(), formula);
        }
        catch (SemanticErrorException e) {
            e.append("while generating close_cycle" + toString());
            throw e;
        }
    }
    
    @Override
    public String toString() {
        return "close_cycle " + predicate.toString() + "->" + dname.toString();
    }
}
