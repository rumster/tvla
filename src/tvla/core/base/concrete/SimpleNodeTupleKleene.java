package tvla.core.base.concrete;

import tvla.core.NodeTuple;
import tvla.logic.Kleene;

public class SimpleNodeTupleKleene implements NodeTupleKleene {

    private final NodeTuple tuple;
    private Kleene kleene;
    private NodeTupleKleene next;

    public SimpleNodeTupleKleene(NodeTuple tuple, Kleene kleene) {
        this.tuple = tuple;
        this.kleene = kleene;
    }

    public NodeTupleKleene copy() {
        return new SimpleNodeTupleKleene(tuple, kleene);
    }

    public NodeTupleKleene getNext() {
        return next;
    }

    public void setNext(NodeTupleKleene next) {
        this.next = next;
    }

    public NodeTuple getKey() {
        return tuple;
    }

    public Kleene getValue() {
        return kleene;
    }

    public Kleene setValue(Kleene value) {
        Kleene prev = kleene;
        kleene = value;
        return prev;
    }

    public int hashCode() {
        return tuple.hashCode();
    }
    
    public boolean equals(Object other) {
        return tuple.equals(((SimpleNodeTupleKleene) other).tuple);
    }
}
