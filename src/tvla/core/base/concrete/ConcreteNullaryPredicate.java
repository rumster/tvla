package tvla.core.base.concrete;

import java.util.Collections;
import java.util.Iterator;

import tvla.core.NodeTuple;
import tvla.logic.Kleene;
import tvla.util.EmptyIterator;

/**
 * An implementation of a nullary predicate interpretation.
 * 
 * @author Tal Lev-Ami.
 * @author Roman Manevich.
 */
public final class ConcreteNullaryPredicate extends ConcretePredicate {
    /**
     * The Kleene value of the predicate.
     */
    private final Kleene value;

    /**
     * A concrete nullary predicate that always evaluates to true.
     */
    private static final ConcreteNullaryPredicate concreteTrue = new ConcreteNullaryPredicate(
            Kleene.trueKleene);

    /**
     * A concrete nullary predicate that always evaluates to false.
     */
    private static final ConcreteNullaryPredicate concreteFalse = new ConcreteNullaryPredicate(
            Kleene.falseKleene);

    /**
     * A concrete nullary predicate that always evaluates to unknown.
     */
    private static final ConcreteNullaryPredicate concreteUnknown = new ConcreteNullaryPredicate(
            Kleene.unknownKleene);

    /**
     * Returns the object of a clean nullary predicate with the specified Kleene
     * value.
     */
    public static ConcreteNullaryPredicate getInstance(Kleene aValue) {
        if (aValue == Kleene.trueKleene)
            return concreteTrue;
        else if (aValue == Kleene.unknownKleene)
            return concreteUnknown;
        else
            return concreteFalse;
    }

    @Override
    public ConcreteNullaryPredicate copy() {
        return this;
    }

    /**
     * Returns the Kleene value associated with this prediacte.
     */
    @Override
    public Kleene get(NodeTuple tuple) {
        return value;
    }

    /**
     * Constructs a concrete nullary predicate from a specified truth value.
     */
    private ConcreteNullaryPredicate(Kleene value) {
        this.value = value;
    }

    public Iterator<NodeTuple> satisfyingTupleIterator(final Kleene desiredValue) {
        if (value == desiredValue) {
            return Collections.singleton(NodeTuple.EMPTY_TUPLE).iterator();
        } else {
            return EmptyIterator.instance();
        }
    }

    @Override
    public int numberSatisfy() {
        return value == Kleene.falseKleene ? 0 : 1;
    }
}