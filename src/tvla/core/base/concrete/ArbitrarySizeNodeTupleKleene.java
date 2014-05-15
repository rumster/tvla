package tvla.core.base.concrete;

import java.util.List;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.common.ArbitrarySizeNodeTuple;
import tvla.logic.Kleene;

public class ArbitrarySizeNodeTupleKleene extends ArbitrarySizeNodeTuple implements NodeTupleKleene {
    protected Kleene kleene;    
    protected NodeTupleKleene next;
    
    public ArbitrarySizeNodeTupleKleene(List<Node> nodeList, Kleene kleene) {
        super(nodeList);
        this.kleene = kleene;
    }

    public ArbitrarySizeNodeTupleKleene(Node[] nodes, Kleene kleene) {
        super(nodes);
        this.kleene = kleene;
    }

    public ArbitrarySizeNodeTupleKleene(ArbitrarySizeNodeTupleKleene tuple, Kleene kleene) {
        super(tuple.nodes);
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

    public ArbitrarySizeNodeTupleKleene copy() {
        return new ArbitrarySizeNodeTupleKleene(this, kleene);
    }
}
