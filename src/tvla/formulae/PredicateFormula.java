package tvla.formulae;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.common.NodePair;
import tvla.exceptions.SemanticErrorException;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.EmptyIterator;
import tvla.util.NoDuplicateLinkedList;

/** Abstract base class for predicate atomic formula.
 * @author Eran Yahav
 * @since tvla-2-alpha (14 May 2002) initial creation.
 */
final public class PredicateFormula extends AtomicFormula {
	private Predicate predicate;
	
	/** Predicate arity is widely used and does not change, so cache it here.
	 */
	private int	cachedArity;
	
	/** Array of variables used in the predicate formula.
	 */
	private Var[] variables;
	
	/** Creates a predicate formula from the given predicate.
	 */
	public PredicateFormula(Predicate predicate) {
		super();
		init(predicate, null);
	}
	
	/** Creates a predicate formula from the given predicate and an array 
	 * of variables.
	 */
	public PredicateFormula(Predicate predicate, Var[] vars) {
		super();
		init(predicate, vars);
	}
	
	/** Creates a new predicate formula from the given predicate and variable.
	 */
	public PredicateFormula(Predicate predicate, Var variable) {
		super();
		Var[] vars = {variable};
		init(predicate, vars);	
	}
	
	/** Creates a new predicate formula from the given predicate and variable pair.
	 */
	public PredicateFormula(Predicate predicate, Var first, Var second) {
		super();
		Var[] vars = {first, second};
		init(predicate, vars);	
	}
	
	public PredicateFormula(Predicate predicate, List<Var> args) {
		super();
		Var[] vars = new Var[args.size()];
		ListIterator<Var> li = args.listIterator();
		int i = 0;
		while (li.hasNext()) {
			vars[i] = li.next();
			i++;
		}
		init(predicate, vars);
	}
	
	
	private int firstVarId;
	private int secondVarId;
	
	/** Initializes private data.
	 */
	protected void init(Predicate predicate, Var[] vars) {
	    if (vars == null) vars = new Var[0];
		this.predicate = predicate;
		this.cachedArity = predicate.arity();
		this.variables = new Var[vars.length];
		this.nodes = new Node[cachedArity];
		System.arraycopy(vars, 0, this.variables, 0, vars.length);
		if (vars != null && cachedArity != vars.length)
			throw new SemanticErrorException("Predicate of arity " + predicate.arity() +
							 " expected but got " + predicate +
							 " of arity " + vars.length);
		switch (cachedArity) {
		case 2:
			secondVarId = variables[1].id();
		case 1:
			firstVarId = variables[0].id();
			break;
		}
	}
	
	/** Returns the formula's predicate.
	 */
	public Predicate predicate() {
		return predicate;
	}
	
	/** Returns the formula's hash code.
	 */
	public int hashCode() {
		int result = predicate.hashCode();
		for (int i = 0; i < cachedArity; ++i) {
			result += 31 * (i + 1) * variables[i].hashCode();
		}
		return result;
	}

	/** 
	 * Returns the formula's hash code.
	 */
	public int ignoreVarHashCode() {
		int result = predicate.hashCode();
		result += cachedArity * 31;
		return result;
	}

	
	/** Creates a copy of the formula.
	 */
	public Formula copy() {
		return new PredicateFormula(predicate, variables);
	}

	/** Returns the variables.
	 */
	public Var[] variables() {
		return variables;
	}

	public Var getVariable(int i) {
		return variables[i];
	}

	/** Calculates and return the free variables for this formula.
	 */
	public List<Var> calcFreeVars() {
		List<Var> result = new NoDuplicateLinkedList<Var>();
		for (int i = 0; i < cachedArity; ++i)
			result.add(variables[i]);
		return result;
	}

	/** Return a human readable representation of the formula.
	 */
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(predicate.toString());
		result.append("(");
		
		if (cachedArity > 0) {
			result.append(variables[0]);
		}
		for (int i=1; i < cachedArity; i++) {
			result.append(",");
			result.append(variables[i].toString());
		}
		result.append(")");
		
		return result.toString();
	}

	/** Equate the this formula with the given fomula by structure.
	 */
	public boolean equals(Object o) {
		if (!(o instanceof PredicateFormula))
			return false;
		PredicateFormula other = (PredicateFormula) o;
		if (!predicate().equals(other.predicate()))
			return false;
		
		boolean result = true;
		int arity = predicate().arity();
		
		for (int i = 0; i < arity; ++i) {
			result = result && getVariable(i).equals(other.getVariable(i));
		}
		return result;
	}

	public boolean equalsByStructure(Object o) {
		if (!(o instanceof PredicateFormula))
			return false;
		PredicateFormula other = (PredicateFormula) o;
		if (!predicate().equals(other.predicate()))
			return false;
		
		if (cachedArity != other.cachedArity || cachedArity > 2)
			return false;
		
		if (cachedArity == 2) {
			Var v1 = variables[0];
			Var v2 = variables[1];
			Var u1 = other.variables[0];
			Var u2 = other.variables[1];
			if (v1.equals(v2)) {
				return u1.equals(u2);
			}
			if (u1.equals(u2))
				return false;
			if (u1.equals(v2) || u2.equals(v1))
				return false;
		}
		
		return true;
	}
	
	/** Substitute the given variable name to a new name. 
	 */
	public void substituteVar(Var from, Var to) {
		int arity = predicate().arity();
		for(int i = 0; i < arity; ++i) {
			if (variables[i].equals(from)) {
				variables[i] = to;
				switch (i) {
				case 0:
					firstVarId = to.id();
					break;
				case 1:
					secondVarId = to.id();
					break;
				}
			}
		}
		freeVars = null;
	}

	/** Substitute variables in parallel according to the sub map. */
	public void substituteVars(Map<Var,Var> sub) {
		int arity = predicate().arity();
		for(int i = 0; i < arity; ++i) {
			Var var = variables[i];
			if (sub.containsKey(var)) {
				variables[i] = var = sub.get(var);
				switch (i) {
				case 0:
					firstVarId = var.id();
					break;
				case 1:
					secondVarId = var.id();
					break;
				}

			}
		}
		freeVars = null;
	}

	/** Evaluate the formula on the given structure and assignment.
	 */
	Node[] nodes;
	
	public Kleene eval(TVS s, Assign assign) {
	  NodeTuple tuple;
	  switch(cachedArity) {
		  case 0:
			  tuple = NodeTuple.EMPTY_TUPLE;
			  break;
		  case 1:
			  tuple = assign.makeTuple(firstVarId);
			  break;
		  case 2:
			  tuple = assign.makeTuple(firstVarId, secondVarId);
			  break;
		  default:
			  tuple = assign.makeTuple(variables);
	  }
	  return s.eval(predicate, tuple);
	}

	
	public NodeTuple makeTuple(Assign assign) {
		for (int i = 0; i < cachedArity; ++i) {
			nodes[i] = assign.get(variables[i]);
			if (nodes[i] == null) {
				throw new SemanticErrorException("Variable " + variables[i].name() + 
					" missing from assignment " + assign +
								 " in formula " + toString());	
			}
		}
		return NodeTuple.createTuple(nodes);
	}
	
	
	/** Calls the specific accept method, based on the type of this formula
	 * (Visitor pattern).
	 * @author Roman Manevich.
	 * @since tvla-2-alpha November 18 2002, Initial creation.
	 */
    @Override
    public <T> T visit(FormulaVisitor<T> visitor) {
		return visitor.accept(this);
	}
	
	public Set<Predicate> getPredicates() {
		if (predicates != null) {
			return predicates;
		}
		predicates = new LinkedHashSet<Predicate>(2);
		predicates.add(predicate());
		return predicates;
	}

	//public FormulaIterator assignments(TVS structure, Assign partial, Kleene value) {
	//	return new PredicateFormulaIterator(structure, this, partial, value, variables);
	//}
	
	public FormulaIterator assignments(TVS structure, Assign partial, Kleene value) {
		switch (cachedArity) {
		case 0:
			return new FormulaIterator(structure, this, partial, value) {
				public boolean step() {
					if (assignIterator == null) {
						stat_PredicateAssigns++;
						stat_Evals++;
						stat_TotalEvals++;
						assignIterator = EmptyIterator.instance();
						result = partial.instanceForIterator(java.util.Collections.EMPTY_LIST, true);
						result.kleene = eval(structure, result);
						return checkDesiredValue(result.kleene);
					}
					else return false;
				}
			};
		case 1:
			return new FormulaIterator(structure, this, partial, value) {
				private Node partialNode = null;
				private Var var;
				
				public boolean step() {
					if (assignIterator == null) {
						stat_PredicateAssigns++;
						
						if (buildFullAssignment()) {
							assignIterator = EmptyIterator.instance();
							result.kleene = eval(structure, result);
							stat_Evals++;
							stat_TotalEvals++;
							return checkDesiredValue(result.kleene);
						} else {
							Var var = variables[0];
							partialNode = partial.contains(var) ? partial.get(var) : null;
							result.addVar(var);		
							this.var = var;
							assignIterator = structure.iterator(predicate());
							stat_NonEvals++;
						}
					}
					
					OUTER: while (assignIterator.hasNext()) {
						stat_TotalEvals += .17;
						Map.Entry<NodeTuple,Kleene> entry = (Map.Entry)assignIterator.next();
						Kleene tupleValue = entry.getValue();
						
						if (!checkDesiredValue(tupleValue))
							continue;
						
						NodeTuple nt = entry.getKey();
						Node node = nt.get(0);
						if (partialNode != null && !partialNode.equals(node))
							continue OUTER;
						
						result.putNode(var, node);
						result.kleene = tupleValue;
						return true;
					}
					return false;
				}
			};
		case 2:
			return new FormulaIterator(structure, this, partial, value) {

				private Node partialNode_0;
				private Node partialNode_1;
				private Var var_0;
				private Var var_1;
	
				public boolean step() {
			      if (assignIterator == null) {
			    	stat_PredicateAssigns++;
			        
			    	if (buildFullAssignment()) {
			    		assignIterator = EmptyIterator.instance();
			    		result.kleene = eval(structure, result);
			    		stat_Evals++;
			    		stat_TotalEvals++;
			    		return checkDesiredValue(result.kleene);
			        } else {
			        	var_0 = variables[0];
			        	partialNode_0 = partial.contains(var_0) ? partial.get(var_0) : null;
			        	var_1 = variables[1];
			        	partialNode_1 = partial.contains(var_1) ? partial.get(var_1) : null;
			        	result.addVar(var_0);
			        	result.addVar(var_1);
			        	
			        	assignIterator = structure.iterator(predicate());
			        	stat_NonEvals++;
			        }
			      }
			      
			      while (assignIterator.hasNext()) {
			    	stat_TotalEvals += .17;
			    	Map.Entry<NodeTuple, Kleene> entry = (Map.Entry)assignIterator.next();
			        Kleene tupleValue = entry.getValue();
	
			        if (!checkDesiredValue(tupleValue))
			        	continue;
	
			        NodePair nt = (NodePair)entry.getKey();
			        
		            //Node node_0, node_1;
	            	//node_0 = nt.get(0);
	            	if (partialNode_0 != null && !partialNode_0.equals(nt.first()))
	            		continue;
	            	//node_1 = nt.get(1);
	            	if (partialNode_1 != null && !partialNode_1.equals(nt.second()))
	            		continue;
	            	
	            	if ((var_0 == var_1) && (nt.first() != nt.second()))
		      			continue;
	            	
	            	result.putNode(var_0, nt.first());
	            	result.putNode(var_1, nt.second());
			        result.kleene = tupleValue;
			        return true;
			      }
			      return false;
			}
		};
		default:
			return new FormulaIterator(structure, this, partial, value) {
				private Node[] partialNodes;
	
				public boolean step() {
			      if (assignIterator == null) {
			    	stat_PredicateAssigns++;
			        
			    	if (buildFullAssignment()) {
			          assignIterator = EmptyIterator.instance();
			          result.kleene = eval(structure, result);
			          stat_Evals++;
			          stat_TotalEvals++;
			          return checkDesiredValue(result.kleene);
			        } else {
			          partialNodes = new Node[variables.length];
	
			          for (int i = 0; i < variables.length; i++) {
			        	Var var = variables[i];
			        	if (partial.contains(var))
			        		partialNodes[i] = partial.get(var);
			        	else
			        		result.addVar(var);
			          }
			          
			          assignIterator = structure.iterator(predicate());
			          stat_NonEvals++;
			        }
			      }
	
			      OUTER: while (assignIterator.hasNext()) {
			    	stat_TotalEvals += .17;
			    	Map.Entry<NodeTuple, Kleene> entry = (Map.Entry)assignIterator.next();
			        Kleene tupleValue = entry.getValue();
	
			        if (!checkDesiredValue(tupleValue))
			        	continue;
	
			        NodeTuple nt = entry.getKey();
			        
		            Node partialNode;
		            // make sure tuple matches the partial assignment, otherwise its invalid
		            for (int i=0; i < partialNodes.length; i++) {
		              partialNode = partialNodes[i];
		              if (partialNode != null) {
		                if (!partialNode.equals(nt.get(i)))
		             	  continue OUTER;
		              }
		            }
	
			        for (int i = 0; i < variables.length; i++) {
			        	Var var_i = variables[i];
			        	Node node_i = nt.get(i);
				      	for (int j = 0; j < i; j++) {
				      		if ((var_i == variables[j]) && (node_i != nt.get(j)))
				      			continue OUTER;
				      	}
				      	result.putNode(var_i, node_i);
			        }
			        result.kleene = tupleValue;
			        return true;
			      }
			      return false;
				}
			};
		}
	}
	
	public FormulaIterator assignments2(TVS structure, Assign partial, Kleene value) {
		return new FormulaIterator(structure, this, partial, value) {
			private Node[] partialNodes;

			public boolean step() {
		      if (assignIterator == null) {
		    	stat_PredicateAssigns++;
		        //result.put(partial);
		        
		        // Maybe needed for TC calculation if used from other 
		        // places beside AdvancedCoerce.
		        //formula.prepare(structure);
		        
		    	if (buildFullAssignment()) {
		        //if (partial.containsAll(formula.freeVars())) {
		          assignIterator = EmptyIterator.instance();
		          result.kleene = eval(structure, result);
		          stat_Evals++;
		          stat_TotalEvals++;
		          return checkDesiredValue(result.kleene);
		        } else {
		          partialNodes = new Node[variables.length];

		          for (int i = 0; i < variables.length; i++) {
		        	Var var = variables[i];
		        	if (partial.contains(var))
		        		partialNodes[i] = partial.get(var);
		        	else
		        		result.addVar(var);
		          }
		          
		          assignIterator = structure.predicateSatisfyingNodeTuples(predicate(), null, null);
		          stat_NonEvals++;
		        }
		      }
			  //java.util.Iterator<Map.Entry> _assignIterator = assignIterator;
		      
		      Var[] variables = PredicateFormula.this.variables;
		      Node[] partialNodes = this.partialNodes;
		      
		      OUTER: while (assignIterator.hasNext()) {
		    	stat_TotalEvals += .17;
		    	Map.Entry<NodeTuple, Kleene> entry = (Map.Entry)assignIterator.next();
		        Kleene tupleValue = entry.getValue();

		        if (!checkDesiredValue(tupleValue))
		        	continue;

		        NodeTuple nt = entry.getKey();
		        
	            Node node_0, partialNode;
	            switch (cachedArity) {
	            case 0:
	            	break;
	            case 1:
	            	partialNode = partialNodes[0];
	            	node_0 = nt.get(0);
	            	if (partialNode != null && !partialNode.equals(node_0))
	            		continue OUTER;
	            	result.putNode(variables[0], node_0);
	            	break;
	            	
	            case 2:
	            	partialNode = partialNodes[0];
	            	node_0 = nt.get(0);
	            	if (partialNode != null && !partialNode.equals(node_0))
	            		continue OUTER;
	            	Node node_1 = nt.get(1);
	            	partialNode = partialNodes[1];
	            	if (partialNode != null && !partialNode.equals(node_1))
	            		continue OUTER;
	            	Var var_0 = variables[0];
	            	Var var_1 = variables[1];
	            	if ((variables[0] == variables[1]) && (node_0 != node_1))
		      			continue OUTER;
	            	result.putNode(var_0, node_0);
	            	result.putNode(var_1, node_1);
	            	break;
	            	
	            default:
		            // make sure tuple matches the partial assignment, otherwise its invalid
		            for (int i=0; i < partialNodes.length; i++) {
		              partialNode = partialNodes[i];
		              if (partialNode != null) {
		                if (!partialNode.equals(nt.get(i)))
		             	  continue OUTER;
		              }
		            }

			        for (int i = 0; i < variables.length; i++) {
			        	Var var_i = variables[i];
			        	Node node_i = nt.get(i);
				      	for (int j = 0; j < i; j++) {
				      		if ((var_i == variables[j]) && (node_i != nt.get(j)))
				      			continue OUTER;
				      	}
				      	result.putNode(var_i, node_i);
			        }
	            }
		        result.kleene = tupleValue;
		        return true;
		      }
		      return false;
			}
		};
	}
}
