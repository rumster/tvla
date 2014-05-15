package tvla.core.assignments;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.common.NodePair;
import tvla.formulae.Var;
import tvla.logic.Kleene;

// This is Assign that is reused multiple times in evaluation,
// without recreating or altering the variables table. The idea
// is that the variables set only depends on a place in Coerce
// pipeline i.e. the formulas and the computation order. For
// a fixed place in the pipeline only the node assignments
// change. 
// This is realized in a transparent way by overloading the
// instanceForIterator so that requested objects are stored
// in the requestor for later reuse.
// 

final public class AssignPrecomputed extends AssignKleene {
		  private boolean containsAllPrecomputed = false;
		  private boolean containsAll;
		  private int varIndex[];

		  /**
		   * Creates an empty assignment.
		   */
		  public AssignPrecomputed() {
			super(Kleene.falseKleene);
			buildVarIndex();
			//assign = new Node[0]; // Don't take up space unnecessarily.
			// This currently causes bugs, because .contains is not always
			// called before .get
		  }
		  
		  public AssignPrecomputed(Assign partial, Kleene value) {
			super(partial, value);
			buildVarIndex();
		  }

		  public AssignPrecomputed(AssignKleene partial) {
			this(partial, partial.kleene);
		  }
		  
		  private AssignPrecomputed nextAssign = null;
		  
		  public AssignKleene instanceForIterator(Collection freeVars, boolean isFull) {
			//if (isFull)
			//    return this;
			if (nextAssign == null) {
				nextAssign = new AssignPrecomputed(this, Kleene.falseKleene);
				nextAssign.addVarsInternal(freeVars);
			}
			else {
				//nextAssign.clean();
				nextAssign.rebuildIfNeeded();
				nextAssign.putNodes(this);
			}
			return nextAssign;
		  }
		  
		  public AssignKleene instanceForIterator(Assign other, Collection freeVars, boolean isFull) {
				//if (isFull)
				//    return this;
				if (nextAssign == null) {
					nextAssign = new AssignPrecomputed(other, Kleene.falseKleene);
					nextAssign.addVarsInternal(freeVars);
				}
				else {
					//nextAssign.clean();
					nextAssign.rebuildIfNeeded();
					nextAssign.putNodes(other);
				}
				return nextAssign;
		  }

		  final private void rebuildIfNeeded() {
			if (assign.length < Var.maxId()) {
				int newSize = 2 * assign.length < Var.maxId() ? Var.maxId() : 2 * assign.length;
				assign = new Node[newSize];
			}
		  }
		  
		  final public void clean() {
			assign = new Node[Var.maxId()];
		  }

		  /**
		   * Add the variable assignments of another assignment to this one, overriding
		   * in case of collision.
		   * 
		   * @param newAssign
		   *          The assignment from which to copy.
		   */
		  public void put(Assign newAssign) {
			  putNodes(newAssign);
		  }

		  /**
		   * Assign the given node to the given variable overriding any old assignment
		   * for that variable.
		   * 
		   * @param variable
		   *          the variable to be assigned.
		   * @param node
		   *          that node assigned to it.
		   */
		  final public void put(Var variable, Node node) {
			  putNode(variable, node);
		  }
		  
		  public void putInternal(Var variable, Node node) {
			  super.put(variable, node);
		  }

		  public void addVars(Collection vars) {
		  }
		  
		  private void addVarsInternal(Collection vars) {
			  super.addVars(vars);
			  buildVarIndex();
		  }
		  
		  private void buildVarIndex() {
			  varIndex = new int[vars.size()];
			  int i = 0;
			  for (Iterator it = vars.iterator(); it.hasNext(); i++) {
				  Var var = (Var)it.next();
				  varIndex[i] = var.id();
			  }
		  }

		  final public void addVar(Var var) {
		  }
		  
		  public void putNodes(AssignPrecomputed newAssign) {
			    int [] index = newAssign.varIndex;
			    for (int i = 0; i < index.length; ++i) {
			      int id = index[i];
			      assign[id] = newAssign.assign[id];
			    }
		  }

		  /**
		   * Remove the variable from the current assignment.
		   * 
		   * @param variable
		   *          the variable whose assignment should be removed
		   */
		  public void remove(Var variable) {
		  }

		  public void removeInternal(Var variable) {
			  super.remove(variable);
		  }

		  /**
		   * Create a shallow copy of the current assignment.
		   */
		  public Assign copy() {
			  // A copy not requested via the instanceForIterator method
			  // looses the persistence functionality.
			  return new AssignKleene(this, kleene);
			  //return new AssignPrecomputed(this, kleene);
		  }

		  /**
		   * Projects the current assignment onto the given variable collection, i.e.
		   * retains only variables found in the given collection, in place.
		   * 
		   * @param variables
		   *          the variable collection to project onto.
		   */
		  public void project(Collection variables) {
		  }
		  
		  public boolean containsAll(Collection variables) {
			  if (containsAllPrecomputed)
				  return containsAll;
			  else {
				  containsAllPrecomputed = true;
				  return containsAll = super.containsAll(variables);
			  }
		  }
}