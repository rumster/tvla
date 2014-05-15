package tvla.core.common;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import tvla.core.HighLevelTVS;
import tvla.core.ImmutableTVS;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.formulae.CloneUpdateFormula;
import tvla.formulae.Formula;
import tvla.formulae.NewUpdateFormula;
import tvla.formulae.RetainUpdateFormula;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.EmptyIterator;

/**
 * This class represents a constant immutable structure with an empty universe.
 * 
 * @author Roman Manevich.
 * @since tvla-2-alpha November 23 2002, Initial creation.
 */
public final class EmptyTVS extends HighLevelTVS implements ImmutableTVS {
  /**
   * The one and only instance of this class (Singleton pattern).
   */
  public static final EmptyTVS instance = new EmptyTVS();

  /**
   * The error message emitted on attempts to modify an empty structure.
   */
  private static String errorMessage = "Internal error! An attempt to modify an immutable (EmptyTVS) structure!";

  /**
   * Bounds the structure in-place.
   */
  public void blur() {
  }

  /**
   * Applies a constraint-solver to the structure.
   * 
   * @return true is the structure is feasible and false otherwise.
   */
  public boolean coerce() {
    return true;
  }

  /**
   * Applies the focus algorithm with the specified formula.
   * 
   * @param focusFormula
   *          A Formula object.
   * @return The collection of focused structures.
   */
  public Collection focus(Formula focusFormula) {
    return Collections.singleton(this);
  }

  /**
   * Updates the structure's predicate interpretations according to the
   * specified update formulae and a partial assignment to their variables.
   * 
   * @param updateFormulae
   *          A collection of PredicateUpdateFormula objects.
   * @param assignment
   *          A partial assignment to the variables of the right-hand side of
   *          the formulae.
   * @see tvla.formulae.PredicateUpdateFormula
   */
  public void updatePredicates(Collection updateFormulae, Assign assignment) {
    throw new RuntimeException(errorMessage);
  }

  /**
   * Applies the specified <tt> new </tt> formula to add new nodes to the
   * structure.
   * 
   * @param formula
   *          A NewUpdateFormula object.
   * @param assignment
   *          A partial assignment to the variables of the formula.
   * @see tvla.formulae.NewUpdateFormula
   */
  public Collection applyNewUpdateFormula(NewUpdateFormula formula, Assign assignment) {
    throw new RuntimeException(errorMessage);
  }

  /**
   * Applies the specified <tt> clone </tt> formula to clone a sub-structure.
   * 
   * @param formula
   *          A formula with one free variable that's used to mark the part of
   *          the universe that should be cloned. The nodes in the cloned part
   *          have isNew = true.
   * @param assignment
   *          A partial assignment to the variables of the formula.
   * @see tvla.formulae.NewUpdateFormula
   */
  public Collection applyCloneUpdateFormula(CloneUpdateFormula formula, Assign assignment) {
    throw new RuntimeException(errorMessage);
  }

  /**
   * Applies the specified <tt> retain </tt> formula to remove nodes from the
   * structure's universe.
   * 
   * @param formula
   *          A RetainUpdateFormula object.
   * @param assignment
   *          A partial assignment to the variables of the formula.
   * @see tvla.formulae.RetainUpdateFormula
   */
  public void applyRetainUpdateFormula(RetainUpdateFormula formula, Assign assignment, TVS refStructure) {
    throw new RuntimeException(errorMessage);
  }

  /**
   * Creates a copy of this structure.
   */
  public EmptyTVS copy() {
    return this;
  }

  /**
   * Returns the predicate's interpretation in this structure.
   */
  public Kleene eval(Predicate nullaryPredicate) {
    return Kleene.falseKleene;
  }

  /**
   * Returns the predicate's interpretation on the specified node for this
   * structure.
   */
  public Kleene eval(Predicate unaryPredicate, Node node) {
    return Kleene.falseKleene;
  }

  /**
   * Returns the predicate's interpretation on the specified node pair for this
   * structure.
   */
  public Kleene eval(Predicate binaryPredicate, Node from, Node to) {
    return Kleene.falseKleene;
  }

  /**
   * Returns the predicate's interpretation on the specified node tuple for this
   * structure. precondition: predicate.arity() == tuple.size()
   * 
   * @since tvla-2-alpha (May 12 2002).
   */
  public Kleene eval(Predicate predicate, NodeTuple tuple) {
    assert (predicate.arity() == tuple.size());
    return Kleene.falseKleene;
  }

  /**
   * Assigns a new interpreation for the specified predicate in this structure.
   */
  public void update(Predicate nullaryPredicate, Kleene newValue) {
    throw new RuntimeException(errorMessage);
  }

  /**
   * Assigns a new interpretation for the specified predicate and node pair for
   * this structure.
   */
  public void update(Predicate binaryPredicate, Node from, Node to, Kleene newValue) {
    throw new RuntimeException(errorMessage);
  }

  /**
   * Assigns a new interpretation for the specified predicate and node tuple for
   * this structure.
   * 
   * @since tvla-2-alpha (May 12 2002).
   */
  public void update(Predicate predicate, NodeTuple tuple, Kleene newValue) {
    throw new RuntimeException(errorMessage);
  }

  public Iterator predicateSatisfyingNodeTuples(Predicate predicate, Node[] partialNodes, Kleene desiredValue) {
    assert (desiredValue != Kleene.falseKleene);
    return EmptyIterator.instance();
  }

  /**
   * Returns the universe of this structure.
   */
  public Collection<Node> nodes() {
    return Collections.EMPTY_SET;
  }

  /**
   * Adds a new node to the structure's universe.
   */
  public Node newNode() {
    throw new RuntimeException(errorMessage);
  }

  /**
   * Removes a node from the structure's universe.
   */
  public void removeNode(Node node) {
    throw new RuntimeException(errorMessage);
  }

  // /////////////////////////////////////////////////////////////////////////
  // The following are operations are optional. //
  // They may be implemented to take advantage of specific TVS //
  // implementations to increase efficiency. //
  // /////////////////////////////////////////////////////////////////////////

  /**
   * Duplicates the specified node by adding another node to the structure and
   * copying its predicate values. An optional operation.
   * 
   * @see tvla.core.generic.DuplicateNode
   */
  public Node duplicateNode(Node node) {
    throw new RuntimeException(errorMessage);
  }

  /**
   * Merges the specified collection of nodes by joining their predicate values.
   * The specified nodes are removed and the resulting node is returned. Note
   * that the result may be a node in the specified collection, in which case
   * it's not removed from the structure's universe. The operation does not
   * affect the node collection. An optional operation.
   * 
   * @see tvla.core.generic.MergeNodes
   */
  public Node mergeNodes(Collection nodesToMerge) {
    throw new RuntimeException(errorMessage);
  }

  /**
   * @return An iterator to a set of assignments that satisfy the formula. An
   *         optional operation.
   * @see tvla.core.generic.EvalFormula
   */
  public Iterator evalFormula(Formula formula, Assign partialAssignment) {
    return Collections.EMPTY_SET.iterator();
  }

  /**
   * @return An iterator to a set of assignments that evaluate to the specified
   *         value. An optional operation.
   * @see tvla.core.generic.EvalFormulaForValue
   */
  public Iterator evalFormulaForValue(Formula formula, Assign partialAssignment, Kleene desiredValue) {
    return Collections.EMPTY_SET.iterator();
  }

  /**
   * Resets the specified predicate. An optional operation.
   * 
   * @see tvla.core.generic.ClearPredicate
   */
  public void clearPredicate(Predicate predicate) {
    throw new RuntimeException(errorMessage);
  }

  /**
   * Returns the number of satisfying assignments for the specified predicate in
   * this structure. An optional operation. postcondition: (predicate ==
   * Vocabulary.active) implies return == nodes().size()
   */
  public int numberSatisfy(Predicate predicate) {
    return 0;
  }

  /**
   * Singleton pattern.
   */
  private EmptyTVS() {
  }
}