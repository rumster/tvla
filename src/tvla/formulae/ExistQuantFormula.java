package tvla.formulae;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignKleene;
import tvla.core.assignments.AssignPrecomputed;
import tvla.logic.Kleene;
import tvla.predicates.Vocabulary;
import tvla.util.EmptyIterator;
import tvla.util.HashMapFactory;

/** An Exists quanitified sub formula.
 * @author Tal Lev-Ami 
 */
final public class ExistQuantFormula extends QuantFormula {
	/** Create a new exists quanitified formula. */
	AssignPrecomputed assignFactory;
	
	public ExistQuantFormula(Var boundVariable, Formula subFormula) {
		super(boundVariable, subFormula);
		assignFactory = new AssignPrecomputed();
	}

	/** Create a copy of the formula */
	public Formula copy() {
		return new ExistQuantFormula(boundVariable, subFormula.copy());
	}

	/** Evaluate the formula on the given structure and assignment. */
	public Kleene eval(TVS s, Assign assign) {
/*
		Kleene result = Kleene.falseKleene;
		FormulaIterator it = subFormula.assignments(s, assign);
		while (it.hasNext()) {
			AssignKleene current = (AssignKleene)it.next();
			result = current.kleene;
			if (result == Kleene.trueKleene) {
				result = s.eval(Vocabulary.active, current.get(boundVariable));
				if (result == Kleene.trueKleene)
					return result;
			}
		}
		return result;
*/		
/*
		FormulaIterator it = subFormula.assignments(s, assign, Kleene.trueKleene);
		if (it.hasNext())
			return Kleene.trueKleene;
		it = subFormula.assignments(s, assign, Kleene.unknownKleene);
		if (it.hasNext())
			return Kleene.unknownKleene;
		return Kleene.falseKleene;
*/

		//Assign localAssign = assign.copy();
		Assign localAssign = assignFactory.instanceForIterator(assign, boundVars(), true);
		localAssign.addVar(boundVariable);
		Kleene result = Kleene.falseKleene;
		// Compute the three valued logical disjunction on the value of the sub formula
		// for all the possible assignments into the bound variable.
		for (Node node : s.nodes()) {
			localAssign.putNode(boundVariable, node);
			result = Kleene.or(result, subFormula.eval(s, localAssign));
			if (result == Kleene.trueKleene)
				result = s.eval(Vocabulary.active, node);
			if (result == Kleene.trueKleene)
				return result;
		}
		return result;

	}
	
	/** Equate the this formula with the given fomula by structure. */
	public boolean equals(Object o) {
		if (!(o instanceof ExistQuantFormula))
			return false;
		return super.equals(o);
	}

	public String toString() {
		return "((E " + boundVariable.toString() + ")." + subFormula.toString() + ")";
	}

	/** Calls the specific accept method, based on the type of this formula
	 * (Visitor pattern).
	 * @author Roman Manevich.
	 * @since tvla-2-alpha November 18 2002, Initial creation.
	 */
    @Override
    public <T> T visit(FormulaVisitor<T> visitor) {
		return visitor.accept(this);
	}
	
	/***
	 * hashCode for ExistQuantFormula
	 * note the fix for alphaRenamingEquals
	 * @since 9/10/02 Eran Yahav, hashCode fix for bug in TVLA-alpha-2.
	 */
	public int hashCode() {
		int result;
		if (!alphaRenamingEquals) {
			result = subFormula.hashCode() * 31;
			result += boundVariable.hashCode();
		} else {
			result = ignoreVarHashCode();
		}	
		return result;
	}
	
	public int ignoreVarHashCode() {
		return subFormula.ignoreVarHashCode() * 31;
	}
	
	public Formula pushBackNegations(boolean negated) {
		if (!negated) {
			subFormula = subFormula.pushBackNegations(false);
			return this;
		}
		else {
			return new AllQuantFormula(boundVariable,
				    subFormula.pushBackNegations(true));
			
		}
	}
	
	public void toCNFArray(Collection<Formula> partial) {
		subFormula.toCNFArray(partial);
	}

	public Formula pushBackQuant(Var bound, boolean allQuant) {
		if (bound != null) {
			subFormula = subFormula.pushBackQuant(bound, allQuant);
			freeVars = boundVars = null;
			return this;
		}
		else {
			return subFormula.pushBackQuant(boundVariable, false);
		}
	}
	
/*	
	public Formula pushBackExistQuant(Var bound) {
		if (bound != null) {
			subFormula = subFormula.pushBackExistQuant(bound);
			freeVars = boundVars = null;
			return this;
		}
		else {
			return subFormula.pushBackExistQuant(boundVariable);
		}
	}

	public Formula pushBackAllQuant(Var bound) {
		subFormula = subFormula.pushBackAllQuant(bound);
		freeVars = boundVars = null;
		return this;
	}
*/
	
	public FormulaIterator assignments(TVS structure, Assign partial, Kleene value) {
		return new FormulaIterator(structure, this, partial, value) {
			
			public boolean step() {
				  if (assignIterator == null) {
					  stat_QuantAssigns++;
					  //result.put(partial);

					  //if (partial.containsAll(formula.freeVars())) {
					  if (buildFullAssignment()) {
						  // TODO: prepare is an expensive way to erase TC cache.
						  //formula.prepare(structure);
						  assignIterator = EmptyIterator.instance();
						  result.kleene = formula.eval(structure, result);
						  stat_Evals++;
						  return checkDesiredValue(result.kleene);
					  }
					  else {
						  // prepare is done inside subformula assignments call.
				          Map<NodeTuple, Kleene> assignsMap = HashMapFactory.make();
						  //Map assignsMap = new LinkedHashMap();
				          Collection<Var> freeVars = formula.freeVars();
				          // FIXME?
				          // result.addVars(freeVars)

				          Iterator<AssignKleene> subAssigns = subFormula.assignments(structure, partial);
				          while (subAssigns.hasNext()) {
				        	  AssignKleene assign = subAssigns.next();
				        	  NodeTuple nt = assign.makeTuple(freeVars);
				        	  Kleene newVal, oldVal;
				        	  oldVal = assignsMap.get(nt);
				        	  if (oldVal != null) {
				        		  newVal = Kleene.or(oldVal, assign.kleene);
				        	  }
				        	  else {
				        		  newVal = assign.kleene;
				        	  }
			        		  if ((newVal == Kleene.trueKleene) && (oldVal != Kleene.trueKleene))
			        			  newVal = structure.eval(Vocabulary.active, assign.get(boundVariable));
			        		  
			        		  assignsMap.put(nt, newVal);
				          }
				          assignIterator = assignsMap.entrySet().iterator();
		                  if (assignIterator.hasNext()) {
		                      result.addVars(freeVars());
		                  }
				          stat_NonEvals++;
					  }
				  }

				  while (assignIterator.hasNext()) {
					  Map.Entry<NodeTuple, Kleene> entry = (Map.Entry<NodeTuple, Kleene>)assignIterator.next();
					  result.kleene = entry.getValue();
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

    public static Formula close(Formula formula1) {
        Formula formula = formula1;
        for (Iterator<Var> iter = formula1.freeVars().iterator(); iter.hasNext();){
            Var var = (Var) iter.next();
            formula = new ExistQuantFormula(var, formula);
        }
        return formula;
    }	
}
