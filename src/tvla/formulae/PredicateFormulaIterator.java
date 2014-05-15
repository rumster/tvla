package tvla.formulae;

import java.util.Map;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.predicates.Predicate;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignKleene;
import tvla.logic.Kleene;
import tvla.util.EmptyIterator;


final class PredicateFormulaIterator extends FormulaIterator {
	PredicateFormulaIterator(tvla.core.TVS structure, PredicateFormula formula, Assign partial, Kleene value, Var[] variables) {
		super(structure, formula, partial, value);
		this.variables = variables;
		predicate = formula.predicate();
	}
	
	final private Predicate predicate;
	final private Var[] variables;
	private Node[] partialNodes;

	public boolean step() {
      if (assignIterator == null) {
    	stat_PredicateAssigns++;
        
    	if (buildFullAssignment()) {
          assignIterator = EmptyIterator.instance();
          result.kleene = formula.eval(structure, result);
          stat_Evals++;
          stat_TotalEvals++;
          return checkDesiredValue(result.kleene);
        } else {
          partialNodes = new Node[variables.length];

          for (int i = 0; i < variables.length; i++) {
        	if (partial.contains(variables[i]))
        		partialNodes[i] = partial.get(variables[i]);
          }
          
          assignIterator = structure.predicateSatisfyingNodeTuples(predicate, null, null);
          stat_NonEvals++;
        }
      }
	  //java.util.Iterator<Map.Entry> _assignIterator = assignIterator;
      
      Var[] variables = this.variables;
      Node[] partialNodes = this.partialNodes;
      
      OUTER: while (assignIterator.hasNext()) {
    	stat_TotalEvals += .17;
    	Map.Entry entry = (Map.Entry)assignIterator.next();
        Kleene tupleValue = (Kleene)entry.getValue();

        if (!checkDesiredValue(tupleValue))
        	continue;

        NodeTuple nt = (NodeTuple)entry.getKey();
        
        Node node_0, partialNode;
        switch (variables.length) {
        case 0:
        	break;
        case 1:
        	partialNode = partialNodes[0];
        	node_0 = nt.get(0);
        	if (partialNode != null && !partialNode.equals(node_0))
        		continue OUTER;
        	result.put(variables[0], node_0);
        	break;
        	
        case 2:
        	partialNode = partialNodes[0];
        	node_0 = nt.get(0);
        	if (partialNode != null && !partialNode.equals(node_0))
        		continue OUTER;
        	Node node_1 = nt.get(1);
        	partialNode = partialNodes[1];
        	if (partialNode != null && !partialNode.equals(node_1))
        		continue OUTER;
        	Var var_0 = variables[0];
        	Var var_1 = variables[1];
        	if ((variables[0] == variables[1]) && (node_0 != node_1))
      			continue OUTER;
        	result.put(var_0, node_0);
        	result.put(var_1, node_1);
        	break;
        	
        default:
            // make sure tuple matches the partial assignment, otherwise its invalid
            for (int i=0; i < partialNodes.length; i++) {
              partialNode = partialNodes[i];
              if (partialNode != null) {
                if (!partialNode.equals(nt.get(i)))
             	  continue OUTER;
              }
            }

	        for (int i = 0; i < variables.length; i++) {
	        	Var var_i = variables[i];
	        	Node node_i = nt.get(i);
		      	for (int j = 0; j < i; j++) {
		      		if ((var_i == variables[j]) && (node_i != nt.get(j)))
		      			continue OUTER;
		      	}
		      	result.put(var_i, node_i);
	        }
        }
        result.kleene = tupleValue;
        return true;
      }
      return false;
	}
};
