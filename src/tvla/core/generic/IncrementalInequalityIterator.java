package tvla.core.generic;

import java.util.Collection;
import java.util.Iterator;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.assignments.AssignKleene;
import tvla.core.common.NodeTupleIterator;
import tvla.formulae.EqualityFormula;
import tvla.formulae.Var;
import tvla.util.EmptyIterator;
import tvla.util.SimpleIterator;

public class IncrementalInequalityIterator extends SimpleIterator<AssignKleene> {
    Var right;
    Var left;
    Iterator<Collection<Node>> inequalitiesIt;
    Iterator<? extends NodeTuple> pairIt;
    AssignKleene assign; 
    
    public IncrementalInequalityIterator(EqualityFormula formula, Collection<Collection<Node>> inequalities,
            AssignKleene assign) {
        this.inequalitiesIt = inequalities.iterator();
        this.assign = assign;
        right = formula.right();
        left = formula.left();
        assign.addVar(right);
        assign.addVar(left);
        pairIt = EmptyIterator.instance();
    }

    @Override
    protected AssignKleene advance() {
        while (true) {
            while (!pairIt.hasNext()) {
                if (!inequalitiesIt.hasNext()) {
                    return null;
                }
                Collection<Node> nodes = inequalitiesIt.next();
                pairIt = NodeTupleIterator.createIterator(nodes, 2);
            }
            NodeTuple tuple = pairIt.next();
            Node leftNode = tuple.get(0);
            Node rightNode = tuple.get(1);
            if (leftNode==rightNode)
                continue;
            assign.putNode(left, leftNode);
            assign.putNode(right, rightNode);
            return assign;
        }
    }    
}
