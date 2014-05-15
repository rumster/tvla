package tvla.core.generic;

import java.util.Collection;

import tvla.core.common.NodeValue;
import tvla.formulae.Var;
import tvla.core.Node;
import tvla.formulae.FormulaIterator;
import tvla.formulae.PredicateFormula;
import tvla.logic.Kleene;
import tvla.core.assignments.AssignKleene;

public class IncrementalPredicateIterator extends FormulaIterator {
	  boolean negated;
	  boolean allow_unknown;
	  boolean skipAdded;
	  Collection nodeValues;
	  int arity;
	  
	  public IncrementalPredicateIterator(PredicateFormula formula, 
			  Collection nodeValues, boolean negated, boolean allow_unknown) {
		  this(formula, nodeValues, negated, allow_unknown, false, new AssignKleene(Kleene.falseKleene));
	  }
	  
	  public IncrementalPredicateIterator(PredicateFormula formula, 
			  Collection nodeValues, boolean negated, boolean allow_unknown, boolean skipAdded) {
		  this(formula, nodeValues, negated, allow_unknown, skipAdded, new AssignKleene(Kleene.falseKleene));
		  PredicateFormula pf = (PredicateFormula)formula;
		  for (int i = 0, _arity = arity; i < _arity; i++) {
			  result.addVar(pf.getVariable(i));
		  }
	  }

	  public IncrementalPredicateIterator(PredicateFormula formula, 
			  Collection nodeValues, boolean negated, boolean allow_unknown, boolean skipAdded, AssignKleene assign) {
		  super(null, formula, null, null);
		  this.negated = negated;
		  this.allow_unknown = allow_unknown;
		  this.skipAdded = skipAdded;
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
			  if ((!negated && (nv.value == Kleene.trueKleene)) ||
				  (!negated && allow_unknown && (nv.value != Kleene.falseKleene)) ||
	    		   (negated && (nv.value == Kleene.falseKleene)) ||
	    		   (negated && allow_unknown && (nv.value != Kleene.trueKleene)))
			  {
				    if (skipAdded && nv.added)
					    continue;
				    
	    			for (int i = 0; i < arity; i++) {
	    				Var var = pf.getVariable(i);
	    				Node node = nv.tuple.get(i);
	    		    	for (int j = 0; j < i; j++) {
	    		    		if ((var == pf.getVariable(j)) &&
	    		    			(node != nv.tuple.get(j)))
	    		    			continue OUTER;
	    		    	}
	    				result.putNode(var, node);
	    			}
    				result.kleene = nv.value;
	    			return true;
			  }
		  }
		  return false;
	  }
};
