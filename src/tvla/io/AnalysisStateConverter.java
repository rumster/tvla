package tvla.io;

/** A subclass of all converters that convert the analysis
 * graph and its structures to some format.
 * @author Roman Manevich
 * @since tvla-2-alpha May 30 2002 Initial creation.
 */
public abstract class AnalysisStateConverter extends StringConverter {
	/** This conversion filters the structures and leaves
	 * only the one with messages.
	 */
	public abstract String convertMessagesOnly(Object o);
	
	/** This conversion filters the structures and leaves
	 * only the one without messages.
	 */
	public abstract String convertWithoutMessages(Object o);
}