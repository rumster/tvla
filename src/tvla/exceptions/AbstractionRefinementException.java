package tvla.exceptions;

import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.transitionSystem.Action;

/** An exception used to terminate the execution of the analysis when
 *  the precondition evaluates to 1/2 on some assignment and abstraction
 *  refinement is configured to kick in at that point.
 * @author Alexey Loginov
 */
public class AbstractionRefinementException extends AnalysisHaltException {
	protected TVS theStructure;
	protected Assign theAssign;

	public AbstractionRefinementException(String label, Action action,
	                                      TVS structure, Assign assign) {
		super(label, action);
		theStructure = structure;
		theAssign = assign;
	}

	public TVS getStructure() {
		return theStructure;
	}

	public Assign getAssign() {
		return theAssign;
	}

	public String getMessage() {
		return "\n\n\tAbstraction needs to be refined due to imprecision of"
			   + "\n\tevaluating precondition " + theAction.getPrecondition()
			   + "  under assignment: " + theAssign
			   + "\n\tat " + theLabel + ": " + theAction + "\n";
	}
}