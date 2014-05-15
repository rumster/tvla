package tvla.language.TVP;

import tvla.exceptions.SemanticErrorException;
import tvla.formulae.Formula;

public class ReportMessageAST extends AST {
	FormulaAST formula;
	MessageAST message;
    FormulaAST composeFormula;

	/** Indicates whether this message is evaluated after the update.
	 */
	public boolean postMessage;

	public ReportMessageAST(FormulaAST formula, MessageAST message) {
		this.formula = formula;
		this.message = message;
	}

    public ReportMessageAST(FormulaAST formula, MessageAST message, FormulaAST composeFormula) {
        this.formula = formula;
        this.message = message;
        this.composeFormula = composeFormula;
    }
	
	public AST copy() {
		return new ReportMessageAST(
			(FormulaAST) formula.copy(),
			(MessageAST) message.copy(),
            composeFormula == null ? null : (FormulaAST) composeFormula.copy()
			);
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		formula.substitute(from, to);
		message.substitute(from, to);
		if (composeFormula != null) {
		    composeFormula.substitute(from, to);
		}
	}

	public Formula getFormula() {
		try {
			return formula.getFormula();
		}
		catch (SemanticErrorException e) {
			e.append("while generating the message " + toString());
			throw e;
		}
	}

    public Formula getComposeFormula() {
        try {
            return composeFormula == null ? null : composeFormula.getFormula();
        }
        catch (SemanticErrorException e) {
            e.append("while generating the message " + toString());
            throw e;
        }
    }
	
	public String getMessage() {
		return message.getMessage();
	}

	public String toString() {
		StringBuffer result = new StringBuffer();

		result.append("%message ");
		if (composeFormula != null) {
		    result.append("[");
	        result.append(composeFormula.toString());
            result.append("] ");		    
		}
		result.append("(");
		result.append(formula.toString());
		result.append(") ->");
		result.append(message.toString());
		return result.toString();
	}
}