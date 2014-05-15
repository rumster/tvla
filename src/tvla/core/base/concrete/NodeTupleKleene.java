package tvla.core.base.concrete;

import java.util.List;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.logic.Kleene;
import tvla.util.IsvEntry;

public interface NodeTupleKleene extends IsvEntry<NodeTuple, Kleene, NodeTupleKleene> {
    class Factory {
        /** Creates and returns a tuple with a single node.
         */
        public static NodeKleene createSingle(Node n, Kleene kleene) {
            assert n != null && kleene != null;
            return new NodeKleene(n, kleene);
        }

        /** Creates and returns a tuple with a pair of nodes.
         */
        public static NodePairKleene createPair(Node first, Node second, Kleene kleene) {
            assert first != null && second != null && kleene != null;
            return new NodePairKleene(first, second, kleene);
        }

        /** Creates and returns a tuple of nodes.
         */
        public static NodeTuple createTuple(List<Node> nodeList, Kleene kleene) {
            switch (nodeList.size()) {
                case 0 :
                    throw new UnsupportedOperationException("Only support tuples of size > 0");
                case 1 :
                    return new NodeKleene(nodeList.get(0), kleene);
                case 2 :
                    return new NodePairKleene(
                        nodeList.get(0),
                        nodeList.get(1), kleene);
                default :
                    return new ArbitrarySizeNodeTupleKleene(nodeList, kleene);
            }
        }

        /** Creates and returns a tuple of nodes.
         */
        public static NodeTuple createTuple(Node[] nodes, Kleene kleene) {
            switch (nodes.length) {
                case 0 :
                    throw new UnsupportedOperationException("Only support tuples of size > 0");
                case 1 :
                    return new NodeKleene(nodes[0], kleene);
                case 2 :
                    return new NodePairKleene(
                            nodes[0], nodes[1], kleene);
                default :
                    return new ArbitrarySizeNodeTupleKleene(nodes, kleene);
            }
        }

        public static NodeTupleKleene createTuple(NodeTuple tuple, Kleene kleene) {
//            return new SimpleNodeTupleKleene(tuple, kleene);
            switch (tuple.size()) {
            case 0 :
                throw new UnsupportedOperationException("Only support tuples of size > 0");
            case 1 :
                return new NodeKleene(tuple.get(0), kleene);
            case 2 :
                return new NodePairKleene(
                        tuple.get(0), tuple.get(1), kleene);
            default :
                return new ArbitrarySizeNodeTupleKleene((ArbitrarySizeNodeTupleKleene) tuple, kleene);
            }
        }
    }
}
