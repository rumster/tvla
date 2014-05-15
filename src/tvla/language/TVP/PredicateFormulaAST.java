package tvla.language.TVP;

import java.util.List;
import java.util.ListIterator;

import tvla.exceptions.SemanticErrorException;
import tvla.formulae.Formula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.Var;

/** @author Tal Lev-Ami
 */
public class PredicateFormulaAST extends FormulaAST {
	PredicateAST predicate;
	Var[] parameters; 

	public PredicateFormulaAST(PredicateAST predicate) {
		// A nullary predicate
		type = "NullaryPredicateFormula";
		this.predicate = predicate;
		this.predicate.checkArity(0);
	}

	public PredicateFormulaAST(PredicateAST predicate, Var var) {
		// An unary predicate
		type = "UnaryPredicateFormula";
		this.predicate = predicate;
		this.predicate.checkArity(1);
		this.parameters = new Var[1];
		this.parameters[0] = var;
	}

	public PredicateFormulaAST(PredicateAST predicate, 
							   Var first, Var second) {
		// A binary predicate
		type = "BinaryPredicateFormula";
		this.predicate = predicate;
		this.predicate.checkArity(2);
		this.parameters = new Var[2];
		this.parameters[0] = first;
		this.parameters[1] = second;
	}

	public PredicateFormulaAST(PredicateAST predicate, List<Var> args) {	
		this.predicate = predicate;
		if (args.size() == 0)
			this.type = "NullaryPredicateFormula";
		else if (args.size() == 1)
			this.type = "UnaryPredicateFormula";
		else if (args.size() == 2)
			this.type = "BinaryPredicateFormula";
		else {
			this.type = "PredicateFormula";
		}
        this.predicate.checkArity(args.size());
		this.parameters = new Var[args.size()];
		
		ListIterator<Var> li = args.listIterator();
		int i = 0;
		while (li.hasNext()) {
			parameters[i] = (Var)li.next();
			i++;
		}
	}
	
	public PredicateFormulaAST(PredicateAST predicate, Var[] vars) {	
		this.predicate = predicate;
		if (vars.length == 0)
			this.type = "NullaryPredicateFormula";
		else if (vars.length == 1)
			this.type = "UnaryPredicateFormula";
		else if (vars.length == 2)
			this.type = "BinaryPredicateFormula";
		else {
			this.type = "PredicateFormula";
		}
        this.predicate.checkArity(vars.length);
		this.parameters = (Var[])vars.clone();
	}
	
	

	public PredicateFormulaAST copy() {
		return new PredicateFormulaAST((PredicateAST) predicate.copy(), parameters);
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		predicate.substitute(from, to);
	}

	public Formula getFormula() {
		try {
			return new PredicateFormula(predicate.getPredicate(),parameters);
		}
		catch (SemanticErrorException e) {
			e.append("while generating predicate formula " + toString());
			throw e;
		}
	}
    
    public String toString() {
      String ret = predicate.toString() + "(";
      for (int i=0; i< parameters.length; i++) {
        ret = ret + parameters[i];
        if (i < parameters.length -1)
          ret = ret + ",";
      }
      ret = ret + ")";
      
      return ret;
    }
}
