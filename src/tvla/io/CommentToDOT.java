package tvla.io;

import tvla.util.StringUtils;

/** Converts a string to a DOT comment.
 * @author Roman manevich.
 */
public class CommentToDOT extends StringConverter {
	public static CommentToDOT defaultInstance = new CommentToDOT();

	/** A counter that's added as a comment to every page.
	 */
	public static int pageCounter = 0;
	
	/** Converts a string to a TVS comment : // text
	 */
	public String convert(Object o) {
		String comment = (String) o;
		comment = StringUtils.replace(comment, "\n", "\n// ");
		return "// " + comment + "\n";
	}

	/** Returns a page number comment and increments the page counter.
	 */
	static String getPageComment() {
		++pageCounter;
		String pageNumber = new Integer(pageCounter).toString();
		return defaultInstance.convert("Page number " + pageNumber);
	}
}