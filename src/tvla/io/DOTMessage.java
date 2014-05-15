package tvla.io;

import java.io.File;

import tvla.util.Pair;
import tvla.util.ProgramProperties;
import tvla.util.StringUtils;

/** Converts a string to a page containing a message in DOT format.
 * @author Roman manevich.
 */
public class DOTMessage extends StringConverter {
	public static DOTMessage defaultInstance = new DOTMessage();

	public DOTMessage() {
	}
	
	public String convert(Object o) {
		String title = "msg";
		String message;
		if (o instanceof Pair) {
			Pair p = (Pair) o;
			// Replace '.' and '/' (or '\') chars in title with '_' to appease dot.
			title = ((String) p.first).replace('.', '_').replace(File.separatorChar, '_');
			message = (String) p.second;
		}
		else message = (String) o;
		if (!ProgramProperties.getBooleanProperty("tvla.dot.meaningfulTitles", false))
			title = "msg";  // Reset to default
		message = StringUtils.replace(message, "\n", "\\n");
		return "digraph " + title + " {" + "size = \"7.5,10\";center=true;" + "\"" + message + "\" [shape=box, fontsize=80]; }\n";
	}
}