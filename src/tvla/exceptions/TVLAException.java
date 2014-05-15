package tvla.exceptions;

import java.util.Stack;

/** A base class for TVLA specific exceptions.
 * @author Roman Manevich.
 * @since tvla-2-alpha (April 12 2002) Initial creation.
 */
@SuppressWarnings("serial")
public class TVLAException extends RuntimeException {
	private Stack<String> messages = new Stack<String>();

	public TVLAException(String message) {
		super(message);
	}

	public TVLAException(Exception exception) {
        super(exception);
    }

    public void append(final String message) {
		messages.push(message);
	}
	
	@Override
	public String getMessage() {
		StringBuilder message = new StringBuilder(super.getMessage());
		for (String msg : messages) {
			message.append(tvla.util.StringUtils.newLine + msg);
		}
		return message.toString();
	}
}