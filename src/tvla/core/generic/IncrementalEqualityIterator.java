package tvla.core.generic;

import java.util.Collection;

import tvla.core.Node;
import tvla.core.assignments.AssignKleene;
import tvla.core.common.NodeValue;
import tvla.formulae.EqualityFormula;
import tvla.formulae.FormulaIterator;
import tvla.formulae.PredicateFormula;
import tvla.formulae.Var;
import tvla.logic.Kleene;

public class IncrementalEqualityIterator extends FormulaIterator {
	  boolean negated;
	  boolean allow_unknown;
	  Collection nodeValues;
	  Var right;
	  Var left;
	  
	  public IncrementalEqualityIterator(EqualityFormula formula, 
			  Collection nodeValues, boolean negated, boolean allow_unknown) {
		  this(formula, nodeValues, negated, allow_unknown, new AssignKleene(Kleene.falseKleene));
	  }

	  public IncrementalEqualityIterator(EqualityFormula formula, 
			  Collection nodeValues, boolean negated, boolean allow_unknown, AssignKleene assign) {
		  super(null, formula, null, null);
		  this.negated = negated;
		  this.allow_unknown = allow_unknown;
		  this.nodeValues = nodeValues;
		  this.result = assign;
	  }
	  public boolean step() {
		  if (assignIterator == null) {
			  right = ((EqualityFormula)formula).right();
			  left = ((EqualityFormula)formula).left();
			  result.addVar(right);
			  result.addVar(left);
			  assignIterator = nodeValues.iterator();
		  }
		  OUTER:
		  while (assignIterator.hasNext()) {
			  NodeValue nv = (NodeValue)assignIterator.next();
			  if (nv.value == Kleene.falseKleene) {
				  if (negated)
					  continue;
				  result.kleene = Kleene.trueKleene;
			  }
			  else {
				  if (!allow_unknown)
					  continue;
				  result.kleene = Kleene.unknownKleene;
			  }
			  Node node = (Node)nv.tuple;
		  	  result.putNode(left, node);
		  	  result.putNode(right, node);
	      	  return true;
		  }
		  return false;
	  }
}
