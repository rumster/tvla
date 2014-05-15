package tvla.io;

import tvla.util.StringUtils;

/** Converts a string to a TVS comment.
 * @author Roman manevich.
 */
public class CommentToTVS extends StringConverter {
	public static CommentToTVS defaultInstance = new CommentToTVS();

	/** Converts a string to a TVS comment : // text
	 */
	public String convert(Object o) {
		String comment = (String) o;
		comment = StringUtils.replace(comment, "\n", "\n// ");
		return "// " + comment + "\n";
	}
}