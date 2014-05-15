package tvla.exceptions;

/** An exception denoting a (possibly) non-termination condition for Focus.
 */
public class FocusNonTerminationException extends TVLAException {
	public FocusNonTerminationException(String message) {
		super(message);
	}
}