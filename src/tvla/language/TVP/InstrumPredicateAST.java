package tvla.language.TVP;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import tvla.core.Constraints;
import tvla.exceptions.SemanticErrorException;
import tvla.formulae.AllQuantFormula;
import tvla.formulae.AndFormula;
import tvla.formulae.EqualityFormula;
import tvla.formulae.ExistQuantFormula;
import tvla.formulae.Formula;
import tvla.formulae.NotFormula;
import tvla.formulae.OrFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashSetFactory;

/** An abstract snytax node for instrumentation predicates. 
 * @author Tal Lev-Ami
 */
public class InstrumPredicateAST extends PredicateDefAST {    
	List<Var> args;
	FormulaAST formula;

	public InstrumPredicateAST(String name, List<PredicateAST> params, List<Var> args, 
							   FormulaAST formula, PredicatePropertiesAST type, Set<Kleene> attr) {
		super(name, params, type, attr, args.size());
		this.args = args;
		this.formula = formula;
	}

	private InstrumPredicateAST(InstrumPredicateAST other) {
		super(other);
		this.args = other.args;
		this.formula = (FormulaAST) other.formula.copy();
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		formula.substitute(from, to);
		super.substitute(from, to);
	}
	
	public InstrumPredicateAST copy() {
		return new InstrumPredicateAST(this);
	}

	public void generate() {
		// Add to the structure.
		Formula formula;
		try {
			formula = this.formula.getFormula();
		}
		catch (SemanticErrorException e) {
			e.append("while generating the instrumentation predicate " + toString());
			throw e;
		}
		Predicate predicate;
		try {
		predicate = Vocabulary.createInstrumentationPredicate(generateName(), 
																		arity, 
																		properties.abstraction(),
																		formula,
																		args);
		}
		catch (SemanticErrorException e) {
			e.append("while generating the instrumentation predicate " + toString());
			throw e;
		}

		generatePredicate(predicate);
        
		checkParametric(predicate);

		PredicateFormula predicateFormula = new PredicateFormula(predicate,args);
		
		// Check that the free variables of the formula match the arguments to the predicate.
		Set<Var> argVars = HashSetFactory.make(args);
		Set<Var> formulaFreeVars = HashSetFactory.make(formula.freeVars());
		if (!argVars.equals(formulaFreeVars)) 
			throw new SemanticErrorException("Formula's (" + formula + 
				") free variables (" +
				formula.freeVars() + ") must match " + 
				" the predicates arguments (" + 
				args + ") in instrumentation " + 
				generateName());
		

		if (!Constraints.automaticConstraints)
			return;
		// Add the definition constraints
		Constraints.getInstance().addConstraint(formula, predicateFormula.copy());
		Constraints.getInstance().addConstraint(new NotFormula(formula), 
									new NotFormula(predicateFormula.copy()));

		// If the definition is expanded to extended horn, create the closure.

		// Get the prenex DNF normal form.
		Formula prenexDNF = Formula.toPrenexDNF(formula);
		boolean negated = false;

        // If this instrum pred is defined as pk(v1..vk) = A (v1..vl) pl(v1..vl),
        // (l > k, and vars can be in any order), then create constraint
        // pk(v1..vk) | pl(v1..vl) ==> pl(v1..vl).  (We really want
        // pk(v1..vk) ==> pl(v1..vl) but we OR on pl(v1..vl) in the body to
        // appease TVLA with the same set of free vars in the head and the body. 
        if (prenexDNF instanceof AllQuantFormula) {
            // First get to the top subformula that's not a forall.
            AllQuantFormula allFormula = (AllQuantFormula) prenexDNF;
            Formula subFormula = allFormula.subFormula();
            while (subFormula instanceof AllQuantFormula)
                subFormula = ((AllQuantFormula) subFormula).subFormula();

            if (subFormula instanceof PredicateFormula) {
                // Now create the constraint pk(v1..vk) | pl(v1..vl) ==> pl(v1..vl).
                PredicateFormula predSubFormula = (PredicateFormula) subFormula;
                Formula body = new OrFormula(predicateFormula.copy(), predSubFormula.copy());
                Formula head = predSubFormula.copy();
                Constraints.getInstance().addConstraint(body, head);
            }   
        }

		// Remove all existantial quantifiers.
		while (true) {
			if (prenexDNF instanceof ExistQuantFormula) {
				ExistQuantFormula eformula = (ExistQuantFormula) prenexDNF;
				prenexDNF = eformula.subFormula();
			}
			else {
				break;
			}
		}

		if ((prenexDNF instanceof AllQuantFormula) || (prenexDNF instanceof OrFormula)) {
			// Try the negated formula.
			negated = true;
			prenexDNF = Formula.toPrenexDNF(new NotFormula(formula));
			// Remove all existantial quantifiers.
			while (true) {
				if (prenexDNF instanceof ExistQuantFormula) {
					ExistQuantFormula eformula = (ExistQuantFormula) prenexDNF;
					prenexDNF = eformula.subFormula();
				}
				else {
					break;
				}
			}
		}

		if (prenexDNF instanceof AndFormula) {
			// OK. This is a good candidate for closure.
			List<Formula> terms = new ArrayList<Formula>();
			Formula.getAnds(prenexDNF, terms);
			for (Formula term : terms) {
				Formula origTerm = term;
				boolean negatedTerm = false;
				if (term instanceof NotFormula) {
					NotFormula nterm = (NotFormula) term;
					term = nterm.subFormula();
					negatedTerm = true;
				}

				if ((term instanceof PredicateFormula) || (negatedTerm && (term instanceof EqualityFormula))) {
					// The body formula is the terms without this term and 
					// with the negated instrumentation. All the variables not in the head are
					// existantialy quantified.
					Formula body = negated ? predicateFormula.copy() : new NotFormula(predicateFormula.copy());
					for (Formula otherTerm : terms) {
						if (otherTerm == origTerm)
							continue;
						body = new AndFormula(body, otherTerm.copy());
					}
					Formula head = negatedTerm ? term.copy() : new NotFormula(term.copy());
					Set<Var> freeVars = HashSetFactory.make(body.freeVars());
					freeVars.removeAll(head.freeVars());
					for (Var var : freeVars) {
						body = new ExistQuantFormula(var, body);
					}

					Constraints.getInstance().addConstraint(body, head);
				}
			}
		}
	}
	
	public String toString() {
		return name + "(" + args + ") = " + formula; 
	}
}
