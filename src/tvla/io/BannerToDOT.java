/*
 * File: BannerToDOT.java 
 * Created on: 20/10/2004
 */

package tvla.io;


/** 
 * @author maon
 */
public class BannerToDOT extends StringConverter {
	public static ProgramToDOT defaultInstance = new ProgramToDOT();
	static int bannerCounter = 0;
	/** DOT attributes for the CFG printout.
	 */
	private static final String cfgAttributes = 
			"size = \"7.5,10\"\ncenter = true; fontsize=6; node [fontsize=20, style=filled]; " +
			"edge [fontsize=10]; nodesep=0.1; ranksep=0.1;\n";


	public String convert(Object o) {
		assert(o instanceof String);
		
		bannerCounter++;
		
		StringBuffer result  = new StringBuffer("digraph BANNER" + bannerCounter + "" + " {\n");  

		result.append(quote(o.toString()));
		result.append("[shape=box,color=lightblue,style=filled];\n");

		result.append("\n } \n");
		
		return result.toString();
	}
}


