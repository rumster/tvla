//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.core.generic;

import java.util.Iterator;
import java.util.Set;

import java.util.Map;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignIterator;
import tvla.core.assignments.AssignKleene;
import tvla.formulae.Formula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.util.HashSetFactory;
import tvla.util.EmptyIterator;

public class EvalPredicateFormulaForValue extends EvalFormulaForValue {

  private static final boolean DEBUG_COMPARE = false;
  private Var[] variables = null;
  private Node[] partialNodes = null;

  public EvalPredicateFormulaForValue(TVS structure, Formula formula, Assign partialAssignment, Kleene desiredValue) {
    super(structure, formula, partialAssignment, desiredValue);
  }

  final public boolean hasNext() {
    if (hasResult) {
      return true;
    }

    if (assignIterator == null) {
      result.put(partial);

      // Attempt to solve TC cache bug by calling prepare on eval
      formula.prepare(structure);
      
      if (partial.containsAll(formula.freeVars())) {
        assignIterator = EmptyIterator.instance();
        result.kleene = formula.eval(structure, result);
        hasResult = checkDesiredValue(result.kleene);
        return hasResult;
      } else {
        PredicateFormula pf = (PredicateFormula) formula;
        variables = pf.variables();
        partialNodes = new Node[variables.length];

        for (int i = 0; i < variables.length; i++) {
          partialNodes[i] = partial.get(variables[i]);
          if (partialNodes[i] == null)
        	  result.addVar(variables[i]);
        }
        
       	assignIterator = structure.predicateSatisfyingNodeTuples(pf.predicate(), partialNodes, desiredValue);
      }
    }
    
    OUTER: while (assignIterator.hasNext()) {
      Map.Entry entry = (Map.Entry)assignIterator.next();
      NodeTuple nt = (NodeTuple)entry.getKey();
      //AssignKleene ak = new AssignKleene(desiredValue);

      for (int i = 0; i < variables.length; i++) {
    	for (int j = 0; j < i; j++) {
    		if ((variables[i] == variables[j]) && (nt.get(i) != nt.get(j)))
    			continue OUTER;
    	}
        result.putNode(variables[i], nt.get(i));
      }
      result.kleene = (Kleene)entry.getValue();
      //result.put(partial);
      //if (desiredValue == null) {
      //  result.kleene = formula.eval(structure, result);
      //}
      
      //result = ak;
      if (DEBUG_COMPARE) {
        Kleene compValue = formula.eval(structure, result);
        if (!checkDesiredValue(compValue)) {

          compValue = formula.eval(structure, result);
          System.err.println("Formula: " + formula);
          System.err.println("Assignment " + result);
          System.err.println("Partial " + partial);
          Var[] variables = ((PredicateFormula) formula).variables();
          for (int i=0;i<variables.length;i++) {
            System.err.println("Var [" + i + "] = " + variables[i]);
          }

          throw new RuntimeException("Incompatible result " + result + " value = " + compValue);
        } else {
          // System.err.print("*** Match" + ak + " value = " + compValue);
        }
      }
     
      hasResult = true;
      return true;
    }
    result = null;
    return false;
  }

}
