package tvla.core;

import java.util.Collections;
import java.util.Set;

import tvla.formulae.Formula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.ValueFormula;
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.predicates.Vocabulary;
import tvla.util.HashSetFactory;
import tvla.util.Pair;
import tvla.util.ProgramProperties;
import tvla.util.StringUtils;

/** A set of constraints to be used by the Coerce algorithm.
 * Currently the set of constraints is global.
 */
public class Constraints {
	public static boolean automaticConstraints = true; // used by AST classes
	
	/** The one and only instance of this class.
	 */
	private static Constraints instance = new Constraints();

	/** A set, containing all of the constraints used for the analysis.
	 */
	private static Set<Constraint> allConstraints = HashSetFactory.make();
	
	/** Returns the one and only instance of this class.
	 */
	public static Constraints getInstance() {
		return instance;
	}
	
	public static void reset() {
		instance = new Constraints();
		allConstraints = HashSetFactory.make();
	}
	
	/** Add a new constraint to the set.
	 * @param body The body of the constraint.
	 * @param head The head of the constraint.
	 */
	public void addConstraint(Formula body, Formula head) {
		if (allConstraints == null)
			allConstraints = HashSetFactory.make();
		allConstraints.add( new Constraint(body, head) );
	}
	
	/** Returns a set of Constraint objects.
	 */
	public Set<Constraint> constraints() {
		return Collections.unmodifiableSet(allConstraints);
	}
	
	/** Returns a human-readable representation of the
	 * set of constraints.
	 */
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(StringUtils.addUnderline("Constraints:") + "\n");
		int index = 0;
		for (Constraint c : allConstraints) {
			result.append("" + index + ".\t" + c + "\n");
            ++index;
		}
		return result.toString();
	}

	/** Singleton pattern.
	 */
	private Constraints() {
		automaticConstraints = ProgramProperties.getBooleanProperty("tvla.generateAutomaticConstraints", true);
		
		// Add the constraint sm(_) ==> 0, which says that the sm
		// predicate should never take the value of 1.
		addConstraint(new PredicateFormula(Vocabulary.sm, new Var("_")), 
					  new ValueFormula(Kleene.falseKleene));
	}
	
	/** This class represents a constraint. A constraint is made of
	 * two formulae - the body and the head, connected by logical
	 * implication.
	 */
	public static class Constraint extends Pair<Formula, Formula> {
		/** Constructs a constraint from the specified formulae for
		 * the body and for the head.
		 */
		public Constraint(Formula body, Formula head) {
			super(body, head);
		}
		
		
		/** Returns the body of the constraint.
		 */
		public Formula getBody() {
			return (Formula) first;
		}

		/** Returns the head of the constraint.
		 */
		public Formula getHead() {
			return (Formula) second;
		}
		
		/** Returns a human-readable representation of the constraint.
		 */
		public String toString() {
			return first + " ==> " + second;
		}
	}
}
