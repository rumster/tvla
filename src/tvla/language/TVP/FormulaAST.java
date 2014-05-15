package tvla.language.TVP;

import tvla.formulae.Formula;

/** The base class of all abstract syntax tree nodes used to represent formulae.
 * @author Tal Lev-Ami.
 */
public abstract class FormulaAST extends AST {
    protected String type;
    
	public abstract Formula getFormula();
    
    public abstract FormulaAST copy();
    
    public String toString() {
    	throw new RuntimeException("Please implement toString() for your subclass!");
    }
}