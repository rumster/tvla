package tvla.language.TVP;

import tvla.core.decompose.Decomposer;
import tvla.exceptions.SemanticErrorException;
import tvla.formulae.Formula;

public class DecompositionNameAST extends AST {
    FormulaAST formula;
    MessageAST name;
    
    public DecompositionNameAST(FormulaAST formula) {
        this.formula = formula;
    }
    public DecompositionNameAST(FormulaAST formula, MessageAST name) {
        this.formula = formula;
        this.name = name;
    }

    
    @Override
    public AST copy() {
        return new DecompositionNameAST(formula.copy(), name == null ? null : (MessageAST) name.copy());
    }

    @Override
    public void substitute(PredicateAST from, PredicateAST to) {
        formula.substitute(from, to);
        if (name != null) {
            name.substitute(from, to);
        }
    }

    @Override
    public void generate() {
    	try {
        Formula formula = this.formula.getFormula();
        String name = null;
        if (this.name != null) {
            name = this.name.getMessage();
        }
        Decomposer.getInstance().addDecompositionFormula(formula, name);
    	}
    	catch (SemanticErrorException e) {
    		e.append("while generating the decomposition name " + toString());
    		throw e;
    	}
    }
    
    @Override
    public String toString() {
    	return formula.toString();
    }
}
