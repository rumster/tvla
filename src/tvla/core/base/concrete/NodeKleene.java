package tvla.core.base.concrete;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.logic.Kleene;

public class NodeKleene extends Node implements NodeTupleKleene {
    protected Kleene kleene;    
    protected NodeTupleKleene next;
    
    public NodeKleene(Node node, Kleene kleene) {
        super(node.id());
        this.kleene = kleene;
    }

    public NodeTupleKleene getNext() {
        return next;
    }

    public void setNext(NodeTupleKleene next) {
        this.next = next;
    }

    public NodeTuple getKey() {
        return nodeForID(id);
    }

    public Kleene getValue() {
        return kleene;
    }

    public Kleene setValue(Kleene value) {
        Kleene prev = kleene;
        kleene = value;
        return prev;
    }
    
    public NodeKleene copy() {
        return new NodeKleene(this, kleene);
    }
}
