package tvla.core.common;

import tvla.formulae.AllQuantFormula;
import tvla.formulae.AndFormula;
import tvla.formulae.ExistQuantFormula;
import tvla.formulae.Formula;
import tvla.formulae.ImpliesFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.Var;
import tvla.iawp.symbolic.CopyVisitor;
import tvla.predicates.Predicate;

public class Relativizer {

    private static class Visitor extends CopyVisitor {
        private final Predicate predicate;

        public Visitor(Predicate predicate) {
            this.predicate = predicate;
        }
        
        public Formula relativizeUniversal(Var var, Formula formula) {
            return new ImpliesFormula(new PredicateFormula(predicate, var), formula);            
        }

        public Formula relativizeExistential(Var var, Formula formula) {
            return new AndFormula(new PredicateFormula(predicate, var), formula);            
        }

        @Override
        public Formula accept(AllQuantFormula f) {
            return new AllQuantFormula(
                    f.boundVariable(),
                    relativizeUniversal(f.boundVariable(), f.subFormula().visit(this)));
        }

        @Override
        public Formula accept(ExistQuantFormula f) {
            return new ExistQuantFormula(
                    f.boundVariable(),
                    relativizeExistential(f.boundVariable(), f.subFormula().visit(this)));
        }
        
    }
    
    public static Formula relativize(Formula formula, Predicate predicate, boolean exist) {
        Visitor visitor = new Visitor(predicate);
        Formula relative = formula.visit(visitor);
        for (Var var : formula.freeVars()) {
            if (exist)
                relative = visitor.relativizeExistential(var, relative);
            else
                relative = visitor.relativizeUniversal(var, relative);
        }
        return relative;
    }    
}
