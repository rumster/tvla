package tvla.core.assignments;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.common.NodePair;
import tvla.formulae.Var;
import tvla.logic.Kleene;


/**
 * This class represents a map from variables to nodes.
 * 
 * @see tvla.formulae.Var
 * @see tvla.core.Node
 * @author Tal Lev-Ami
 * @TODO: we may want to change this into a TreeMap as this implementation
 *        potentially allocates large arrays (needlessly)
 */
public class Assign implements Comparable<Assign> {
  /**
   * An empty assignement.
   */
  //static Node[] globalAssign = new Node[1000];

  public final static Assign EMPTY = new Assign();

  protected Collection<Var> vars;

  protected Node[] assign;
  //private Map assign;
  
  private boolean shared = false;
  

  /**
   * Creates an empty assignment.
   */
  public Assign() {
    //vars = HashSetFactory.make(0);
	vars = new LinkedHashSet<Var>(0);
	//vars = new LinkedList();
    assign = new Node[Var.maxId()];
	/*
	if (Var.maxId() > globalAssign.length) {
		int newSize = globalAssign.length * 2 >= Var.maxId() ? globalAssign.length * 2 : Var.maxId();
		assign = new Node[newSize];
		System.arraycopy(globalAssign, 0, assign, 0, globalAssign.length);
		globalAssign = assign;
	}
	else {
		assign = globalAssign;
		java.util.Arrays.fill(assign, null);
	}
	*/
  }
  
  final private void modify() {
	  if (shared) {
		  vars = new LinkedHashSet<Var>(this.vars);
		  shared = false;
	  }
  }

  /**
   * Creates a new assignment as a copy of another assignment.
   * 
   * @param from
   *          the assignment to copy.
   */
  public Assign(Assign from) {
    //this.vars = HashSetFactory.make(from.vars);
	//this.vars = new LinkedHashSet(from.vars);
	this.vars = from.vars;
	this.shared = from.shared = true;
	//this.vars = (Collection)((LinkedList)from.vars).clone();
    this.assign = new Node[Var.maxId()];
    //this.assign = new TreeMap(); 
    putNodes(from);
  }

  /**
   * Is the assignment empty?
   */
  public boolean isEmpty() {
    return vars.isEmpty();
  }

  /**
   * Returns the set of variables bound in this assignment.
   */
  public Collection<Var> bound() {
    return vars;
  }

  /**
   * Add the variable assignments of another assignment to this one, overriding
   * in case of collision.
   * 
   * @param newAssign
   *          The assignment from which to copy.
   */
  public void put(Assign newAssign) {
	modify();
    for (Iterator<Var> it = newAssign.vars.iterator(); it.hasNext();) {
      Var var = it.next();
      vars.add(var);
      //assign.put(var, newAssign.assign.get(var));
      assign[var.id()] = newAssign.assign[var.id()];
    }
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
  public void put(Var variable, Node node) {
	modify();
    vars.add(variable);
    assign[variable.id()] = node;
    //assign.put(variable, node);
  }

  final public void putNode(Var variable, Node node) {
    assign[variable.id()] = node;
    //assign.put(variable, node);
  }
  
  public void putNodes(Assign newAssign) {
    for (Iterator<Var> it = newAssign.vars.iterator(); it.hasNext();) {
      int id = it.next().id();
      assign[id] = newAssign.assign[id];
    }
  }

  public void addVars(Collection<Var> vars) {
	modify();
    this.vars.addAll(vars);
  }

  public void addVar(Var var) {
    modify();
	vars.add(var);
  }

  /**
   * Remove the variable from the current assignment.
   * 
   * @param variable
   *          the variable whose assignment should be removed
   */
  public void remove(Var variable) {
	modify();
    vars.remove(variable);
    //assign.remove(variable);
  }

  /**
   * Return the node assigned to the given variable.
   * 
   * @param variable
   *          the variable whose assignment we want.
   */
  final public Node get(Var variable) {
    return assign[variable.id()];
	//return (Node)assign.get(variable);
  }

  /**
   * Returns true iff the given variable is assigned in this assignment.
   * 
   * @param variable
   *          the variable to check
   */
  public boolean contains(Var variable) {
    return vars.contains(variable);
  }

  /**
   * Create a shallow copy of the current assignment.
   */
  public Assign copy() {
    return new Assign(this);
  }
  
  public NodeTuple makeTuple() {
	  return makeTuple(vars);
  }

  public NodeTuple makeTuple(Collection<Var> variables) {
		Node[] nodes = new Node[variables.size()];
		int i = 0;
		for (Iterator<Var> it = variables.iterator(); it.hasNext();) {
			Var var = it.next();
			nodes[i++] = assign[var.id()];
		}
		return NodeTuple.createTuple(nodes);
  }

  final public NodeTuple makeTuple(Var[] variables) {
	  switch(variables.length) {
	  case 0:
		  return NodeTuple.EMPTY_TUPLE;
	  case 1:
		  return assign[variables[0].id()];
	  case 2:
		  return new NodePair(assign[variables[0].id()], 
				  			  assign[variables[1].id()]);
	  default:
		Node[] nodes = new Node[variables.length];
		int i = 0;
		for (i = 0; i < variables.length; ++i) {
			nodes[i] = assign[variables[i].id()];
		}
		return NodeTuple.createTuple(nodes);
	  }
  }

  final public Node makeTuple(int firstId) {
	  return assign[firstId];
  }

  final public NodePair makeTuple(int firstId, int secondId) {
	  return new NodePair(assign[firstId], assign[secondId]);
  }

  public void putTuple(Collection<Var> variables, NodeTuple tuple) {
		int i = 0;
		for (Var var : variables) {
			assign[var.id()] = tuple.get(i++);
		}
  }

  
  /**
   * Projects the current assignment onto the given variable collection, i.e.
   * retains only variables found in the given collection, in place.
   * 
   * @param variables
   *          the variable collection to project onto.
   */
  public void project(Collection<Var> variables) {
	modify();
    vars.retainAll(variables);
  }
  
  public boolean containsAll(Collection<Var> variables) {
	  return vars.containsAll(variables);
  }

  public boolean equals(Object o) {
    if (!(o instanceof Assign)) {
      return false;
    }
    Assign other = (Assign) o;
    if (!this.vars.equals(other.vars))
      return false;
    for (Iterator<Var> it = vars.iterator(); it.hasNext();) {
      Var var = it.next();
      if (!this.assign[var.id()].equals(other.assign[var.id()]))
      //if (!this.assign.get(var).equals(other.assign.get(var)))
        return false;
    }
    return true;
  }

  public int hashCode() {
	  int hashCode = 0;
	  for (Iterator<Var> it = vars.iterator(); it.hasNext();) {
		  Var var = it.next();
		  //hashCode += (131 + var.hashCode())*(131 + assign.get(var).hashCode());
		  hashCode += (131 + var.hashCode())*(131 + assign[var.id()].hashCode());
	  }
	  //Logger.println(toString() + "" + hashCode);
	  return hashCode;
  }
  
  public final int compareTo(Assign other) {
		return this.hashCode() - other.hashCode();
  }

  /**
   * Print a human readable representation of the assignment.
   */
  public String toString() {
    StringBuffer result = new StringBuffer();

    result.append("{");
    for (Iterator<Var> i = vars.iterator(); i.hasNext();) {
      Var var = i.next();
      Node node = assign[var.id()];
      //Node node = (Node)assign.get(var);
      result.append(var.toString());
      result.append("=");
      result.append(node.name());
      if (i.hasNext())
        result.append(", ");
    }
    result.append("}");

    return result.toString();
  }
  
  public AssignKleene instanceForIterator(Collection<Var> freeVars, boolean isFull) {
	return new AssignKleene(this, Kleene.falseKleene);
  }


  /**
   * Return an iterator to all the possible assignment of the given node set to
   * the given var set (cartesian product).
   * 
   * @param nodeSet
   *          The range of nodes for each variable
   * @param varSet
   *          The variables participating in the resulting assignments.
   */
  final public static Iterator<Assign> getAllAssign(final Collection<Node> nodeSet, final Set<Var> varSet) {
	return getAllAssign(nodeSet, varSet, null);  
  }

  public static Iterator<Assign> getAllAssign(final Collection<Node> nodeSet, final Collection<Var> varSet, final Assign partial) {
    return new AssignIterator(partial) {
      Var[] vars = new Var[varSet.size()];

      Node[] nodes = new Node[nodeSet.size()];

      int[] assignIterators = new int[varSet.size()];
      
      int firstCounter;
      int firstVarId;
      Node firstNode;

      boolean first = true;

      public boolean hasNext() {
        if (result == null) {
          return false;
        }
        if (hasResult) {
          return true;
        }

        if (first) {
          first = false;

          // If varSet is empty, there is a single (empty)
          // assignment, whether or not nodeSet is empty.
          if (varSet.size() == 0) {
            // The single empty assignment.
            hasResult = true;
            return true;
          }
          if (nodeSet.size() == 0) {
            result = null;
            return false;
          }

          int i = 0;
          Node[] _nodes = nodes;
          for (Node node : nodeSet) {
            _nodes[i] = node;
            i++;
          }

          Var _vars[] = vars;
          int[] _assignIterators = assignIterators;
          Node _firstNode = _nodes[0];
          i = 0;
          for (Var var : varSet) {
            _vars[i] = var;
            _assignIterators[i] = 0;
            result.put(var, _firstNode);
            i++;
          }
          // Prepare for the next tuple.
          //_assignIterators[0] = 1;
          firstCounter = 1;
          firstVarId = vars[0].id();
          firstNode = _firstNode;
          hasResult = true;
          return true;
        } // end case of first call to hasNext()

        // The single empty assignment has been consumed.
        if (varSet.size() == 0) {
          result = null;
          return false;
        }

        // Must test for result == null at the top because
        // if hasNext() is called again after the loop below
        // explores the last assignment, this loop will try
        // to assign into result, which is null.

        while (true) {

          // Is the 0th counter not too high?
       /*
          if (_assignIterators[0] < _nodes.length) {
            result.assign[_vars[0].id()] = _nodes[_assignIterators[0]];
        	//result.assign.put(vars[0], nodes[assignIterators[0]]);
            _assignIterators[0]++;
            hasResult = true;
            return true;
          }
       */
          if (firstCounter < nodes.length) {
        	result.assign[firstVarId] = nodes[firstCounter];
        	++firstCounter;
            hasResult = true;
            return true;
          }
          else {
            firstCounter = 0;
            result.assign[firstVarId] = firstNode;

            // The 0th counter is too high.
            // Find the first counter that has room to grow.
            // Roll over those before it (including the 0th) to 0.
            int i, length = assignIterators.length;
            for (i = 1; i < length; i++) {
              // Counter maxed out, reset it to 0.
              int cur = assignIterators[i];
              if (cur + 1 >= nodes.length) {
                assignIterators[i] = 0;
                result.assign[vars[i].id()] = firstNode;
                //result.assign.put(vars[i], nodes[0]);
              } else {
                // Found a counter with room to grow!
                assignIterators[i]++;
                result.assign[vars[i].id()] = nodes[cur + 1];
                //result.assign.put(vars[i], nodes[assignIterators[i]]);
                break;
              }
            }
            // All counters reached max, saw all assignments.
            if (i == length)
              break;
          }
        }
        result = null;
        return false;
      }
    };
  }
}
