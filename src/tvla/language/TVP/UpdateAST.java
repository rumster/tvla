package tvla.language.TVP;

import java.util.*;

import tvla.exceptions.SemanticErrorException;
import tvla.formulae.Var;
import tvla.predicates.Predicate;
import tvla.transitionSystem.Action;

public class UpdateAST extends AST {
	protected PredicateAST predicate;
	protected FormulaAST updateFormula;
	protected List<Var> args;
	protected boolean auto;

	public UpdateAST(
		PredicateAST predicate,
		FormulaAST updateFormula,
		List<Var> args) {
		this(predicate, updateFormula, args, false);
	}

	public UpdateAST(
		PredicateAST predicate,
		FormulaAST updateFormula,
		List<Var> args,
		boolean auto) {
		this.predicate = predicate;
		this.updateFormula = updateFormula;
		this.args = args;
		this.auto = auto;
		predicate.checkArity(args.size());
	}

	public AST copy() {
		return new UpdateAST(
			(PredicateAST) predicate.copy(),
			(FormulaAST) updateFormula.copy(),
			new ArrayList<Var>(args),
			auto);
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		predicate.substitute(from, to);
		updateFormula.substitute(from, to);
	}

	public void addUpdate(Action action) {
		Predicate pred;
		try {
			pred = predicate.getPredicate();
		}
		catch (SemanticErrorException e) {
			e.append("while genearating the update formula " + toString());
			throw e;
		}
		
		if (pred.arity() != args.size())
			throw new SemanticErrorException(
				"Attempt to create a predicate update formula"
					+ " for predicate "
					+ pred
					+ " with arity "
					+ pred.arity()
					+ " and "
					+ args.size()
					+ " argument(s)!");
		if (args.size() == 0) {
			action.setPredicateUpdateFormula(
					pred,
				updateFormula.getFormula(),
				auto);
		} else if (args.size() == 1) {
			action.setPredicateUpdateFormula(
					pred,
				updateFormula.getFormula(),
				(Var) args.get(0),
				auto);
		} else if (args.size() == 2) {
			action.setPredicateUpdateFormula(
					pred,
				updateFormula.getFormula(),
				(Var) args.get(0),
				(Var) args.get(1),
				auto);
		} else {
			action.setPredicateUpdateFormula(
					pred,
				updateFormula.getFormula(),
				args,
				auto);
		}
	}

    public void addLet(Action action) {
		Predicate pred;
		try {
			pred = predicate.getPredicate();
		}
		catch (SemanticErrorException e) {
			e.append("while adding a let to the update formula " + toString());
			throw e;
		}
    	
        if (pred.arity() != args.size())
            throw new SemanticErrorException(
                "Attempt to create a predicate let formula"
                    + " for predicate "
                    + pred
                    + " with arity "
                    + pred.arity()
                    + " and "
                    + args.size()
                    + " argument(s)!");
        if (args.size() == 0) {
            action.setPredicateLetFormula(
            		pred,
                updateFormula.getFormula(),
                auto);
        } else if (args.size() == 1) {
            action.setPredicateLetFormula(
            		pred,
                updateFormula.getFormula(),
                (Var) args.get(0),
                auto);
        } else if (args.size() == 2) {
            action.setPredicateLetFormula(
            		pred,
                updateFormula.getFormula(),
                (Var) args.get(0),
                (Var) args.get(1),
                auto);
        } else {
            action.setPredicateLetFormula(
            		pred,
                updateFormula.getFormula(),
                args,
                auto);
        }
    }
    
	//	predicate:p LP optional_id_list:args RP ASSIGN formula:f optional_auto:a
	// {: RESULT = new UpdateAST(p, f, VarAST.asVariables(args), a.booleanValue()); :}
	public String toString() {
		StringBuffer result = new StringBuffer();
		String separator = "";
		result.append(predicate.toString());
		result.append("(");
		//@TODO: should we take only the predicate name and params?
		for (Var var : args) {
			result.append(separator);
			result.append(var.toString());
			separator = ",";
		}
		result.append(") = ");
		result.append(updateFormula.toString());
		if (auto) {
			result.append(" auto");
		}

		return result.toString();
	}
}
