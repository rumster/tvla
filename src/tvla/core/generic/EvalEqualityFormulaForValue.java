//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.core.generic;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignIterator;
import tvla.core.assignments.AssignKleene;
import tvla.formulae.Formula;
import tvla.formulae.EqualityFormula;
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.util.EmptyIterator;
import tvla.util.HashSetFactory;
import tvla.predicates.Vocabulary;

public class EvalEqualityFormulaForValue extends EvalFormulaForValue {
  private Var leftVar;
  private Var rightVar;

  public EvalEqualityFormulaForValue(TVS structure, Formula formula, Assign partialAssignment, Kleene desiredValue) {
    super(structure, formula, partialAssignment, desiredValue);
  }
  
  public boolean hasNext() {
    if (hasResult) {
      return true;
    }

    if (assignIterator == null) {
        result.put(partial);
        // TODO: prepare is an expensive way to erase TC cache.
        formula.prepare(structure);
        EqualityFormula ef = (EqualityFormula) formula;

        leftVar = ef.left();
        rightVar = ef.right();
        Node leftNode = partial.get(leftVar);
        Node rightNode = partial.get(rightVar);

        if ((leftNode != null) && (rightNode != null)) {
          assignIterator = EmptyIterator.instance();
          result.kleene = formula.eval(structure, result);
          hasResult = checkDesiredValue(result.kleene);
          return hasResult;
        } else if ((leftNode == null) && (rightNode == null)) {
          assignIterator = structure.nodes().iterator();
          result.put(leftVar, null);
          result.put(rightVar, null);
        } else {
          assignIterator = EmptyIterator.instance();
          if (leftNode == null) {	  
            result.put(leftVar, rightNode);
          }
          else if (rightNode == null) {
        	result.put(rightVar, leftNode);
          }
          result.kleene = formula.eval(structure, result);
          hasResult = checkDesiredValue(result.kleene);
          return hasResult;
        }
    }

    while (assignIterator.hasNext()) {
  	  	Node node = (Node)assignIterator.next();
  	  	result.putNode(leftVar, node);
  	  	result.putNode(rightVar, node);
        result.kleene = formula.eval(structure, result);
        if (checkDesiredValue(result.kleene)) {
          hasResult = true;
          return true;
        }
    }
    result = null;
    return false;
  }

}
