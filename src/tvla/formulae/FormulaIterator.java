package tvla.formulae;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignKleene;
import tvla.logic.Kleene;
import tvla.util.EmptyIterator;
import tvla.util.HashSetFactory;

public class FormulaIterator implements Iterator<AssignKleene> {
	protected boolean hasResult = false;
	//protected AssignKleene result = new AssignKleene(Kleene.falseKleene);
	protected AssignKleene result;
	
	protected final Assign partial;
	protected final Formula formula;
	protected final TVS structure;
	protected final Kleene desiredValue;
	protected Iterator assignIterator = null;
	
	public static float stat_TotalEvals = 0;
	public static int stat_Evals = 0;
	public static int stat_NonEvals = 0;
	public static int stat_DefaultAssigns = 0;
	public static int stat_PredicateAssigns = 0;
	public static int stat_PredicateNegationAssigns = 0;
	public static int stat_EqualityAssigns = 0;
	public static int stat_QuantAssigns = 0;
	
	final protected boolean buildFullAssignment() {
		Collection<Var> freeVars = formula.freeVars();
		boolean isFull = partial.containsAll(freeVars);
		result = partial.instanceForIterator(freeVars, isFull);
		return isFull;
	}
	  
	public boolean step() {
	  if (assignIterator == null) {
		  stat_DefaultAssigns++;
		  //result.put(partial);

		  if (buildFullAssignment()) {
			  assignIterator = EmptyIterator.instance();
			  result.kleene = formula.eval(structure, result);
			  stat_Evals++;
			  stat_TotalEvals++;
			  return checkDesiredValue(result.kleene);
		  } else {
			  
			  stat_NonEvals++;
			  Set<Var> freeVars = HashSetFactory.make(formula.freeVars());
			  freeVars.removeAll(partial.bound());
	          assignIterator = Assign.getAllAssign(structure.nodes(), freeVars);
	          if (assignIterator.hasNext()) {
	        	  result.addVars(freeVars);
	          }
		  }
	  }

	  while (assignIterator.hasNext()) {
		  result.putNodes((Assign) assignIterator.next());
		  result.kleene = formula.eval(structure, result);
		  stat_TotalEvals++;
		  if (checkDesiredValue(result.kleene)) {
			  return true;
		  }
	  }
	  return false;
	}

	final public boolean hasNext() {
		if (hasResult) {
	      return true;
	    }
	    
	    if (step()) {
	    	hasResult = true;
	    	return true;
	    }
	    else {
	    	result = null;
	    	return false;
	    }
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}	
		
	final public AssignKleene next() {
		if (!hasResult) {
			if (!hasNext()) {
				return null;
			}
		}
		hasResult = false;
		return result;
	}		    

	public void reset() {
		assignIterator = null;
		result = new AssignKleene(Kleene.falseKleene);
	}
	  
	final protected boolean checkDesiredValue(Kleene k) {
	    if (desiredValue == null) {
	    	return k != Kleene.falseKleene;
	    } else {
	      	return k == desiredValue;
	    }
	}

	public FormulaIterator(TVS structure, Formula formula, Assign partialAssignment, Kleene desiredValue) {
	    this.structure = structure;
	    this.formula = formula;
	    this.partial = partialAssignment;
	    this.desiredValue = desiredValue;
	}
}
