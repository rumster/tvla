/*
 * File: BannerToDOT.java 
 * Created on: 20/10/2004
 */

package tvla.io;


/** 
 * @author maon
 */
public class BannerToTVS extends StringConverter {
	public static BannerToTVS defaultInstance = new BannerToTVS();

	public String convert(Object o) {
		assert(o instanceof String);

		String result  = "/************************************************************\n" +
				 		 (String) o + "\n" +
				  	     "*************************************************************/\n" ;
		return result;
	}
}


