package tvla.core.base.concrete;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.common.NodePair;
import tvla.logic.Kleene;

public class NodePairKleene extends NodePair implements NodeTupleKleene {
    protected Kleene kleene;    
    protected NodeTupleKleene next;
    
    public NodePairKleene(Node first, Node second, Kleene kleene) {
        super(first, second);
        this.kleene = kleene;
    }

    public NodeTupleKleene getNext() {
        return next;
    }

    public void setNext(NodeTupleKleene next) {
        this.next = next;
    }

    public NodeTuple getKey() {
        return this;
    }

    public Kleene getValue() {
        return kleene;
    }

    public Kleene setValue(Kleene value) {
        Kleene prev = kleene;
        kleene = value;
        return prev;
    }

    public NodePairKleene copy() {
        return new NodePairKleene(first, second, kleene);
    }
}
