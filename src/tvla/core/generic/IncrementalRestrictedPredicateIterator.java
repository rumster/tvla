package tvla.core.generic;

import java.util.Collection;

import tvla.core.assignments.AssignKleene;
import tvla.core.common.NodeValue;
import tvla.formulae.FormulaIterator;
import tvla.formulae.PredicateFormula;
import tvla.logic.Kleene;

public class IncrementalRestrictedPredicateIterator extends FormulaIterator {
	  boolean negated;
	  boolean allow_unknown;
	  Collection nodeValues;
	  int arity;
	  
	  public IncrementalRestrictedPredicateIterator(PredicateFormula formula, 
			  Collection nodeValues, Kleene desiredValue, boolean skipAdded) {
		  this(formula, nodeValues, desiredValue, new AssignKleene(Kleene.falseKleene));
		  PredicateFormula pf = (PredicateFormula)formula;
		  for (int i = 0, _arity = arity; i < _arity; i++) {
			  result.addVar(pf.getVariable(i));
		  }
	  }

	  public IncrementalRestrictedPredicateIterator(PredicateFormula formula, 
			  Collection nodeValues, Kleene desiredValue, AssignKleene assign) {
		  super(null, formula, null, desiredValue);
		  this.nodeValues = nodeValues;
		  this.arity = formula.predicate().arity();
		  this.result = assign;
	  }

	  public boolean step() {
		  PredicateFormula pf = (PredicateFormula)formula;
		  if (assignIterator == null) {
			  assignIterator = nodeValues.iterator();
		  }
		  OUTER:
		  while (assignIterator.hasNext()) {
			  NodeValue nv = (NodeValue)assignIterator.next();
			  if ((nv.value == desiredValue))
			  {
	    			for (int i = 0; i < arity; i++) {
	    		    	for (int j = 0; j < i; j++) {
	    		    		if ((pf.getVariable(i) == pf.getVariable(j)) &&
	    		    			(nv.tuple.get(i) != nv.tuple.get(j)))
	    		    			continue OUTER;
	    		    	}
	    				result.putNode(pf.getVariable(i), nv.tuple.get(i));
	    			}
	    			result.kleene = nv.value;
	    			return true;
			  }
		  }
		  return false;
	  }
};
