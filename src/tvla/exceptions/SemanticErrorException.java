package tvla.exceptions;

import tvla.analysis.Engine;

/** An exception representing a semantic error in the specification.
 * @author Roman Manevich
 * @since tvla-2-alpha (12 May 2002)
 */
@SuppressWarnings("serial")
public class SemanticErrorException extends TVLAException {
	public SemanticErrorException(final String message) {
		super(buildErrorMessage(message));
	}

    private static String buildErrorMessage(final String message) {
        StringBuilder builder = new StringBuilder();
        builder.append(message).append(".");
        if (Engine.getCurrentLocation() != null) {
            builder.append(" Location ");
            builder.append(Engine.getCurrentLocation());
        }
        if (Engine.getCurrentAction() != null) {
            builder.append(" Action ");
            builder.append(Engine.getCurrentAction());
        }
        return builder.toString();
    }

    public SemanticErrorException(Exception exception) {
        super(exception);
    }
}
