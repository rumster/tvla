package tvla.exceptions;

/** An exception that denotes an illegal usage of a TVLA feature.
 * @author Roman Manevich.
 */
public class UserErrorException extends TVLAException {
	public UserErrorException(String message) {
		super(message);
	}
}