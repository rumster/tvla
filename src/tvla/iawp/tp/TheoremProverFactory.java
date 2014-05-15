package tvla.iawp.tp;

import tvla.util.ProgramProperties;
import tvla.util.PropertiesEx;

/**
 * Theorem prover factory, allows the use of an arbitrary theorem 
 * prover and wraps it as a singleton.
 * The theorem prover to be used is taken from the property file, 
 * the property is: tvla.tp.prover
 * The name provided should be in lower case, e,g. :
 * simplify 
 * spass
 * otter
 * 
 * The name is then mapped to a qualified class name by the property file: 
 * tvla.iawp.tp.properties which contains mapping like:
 * spass = tvla.iawp.tp.spass.Spass
 * simplify = tvla.tp.prover = tvla.iawp.tp.simplify.Simplify
 *       
 * @author Eran Yahav (eyahav )
 * */
public class TheoremProverFactory {

	private static TheoremProver instance;

	public static TheoremProver getInstance() {
		if (instance != null)
			return instance;

		String implementation =
			ProgramProperties.getProperty("tvla.tp.prover", "spass");

		implementation = implementation.toLowerCase();

		String className =
			new PropertiesEx(
				"/tvla/iawp/tp/tvla.iawp.tp.properties").getProperty(
				implementation,
				implementation);

		try {
			Class tpClass = Class.forName(className);
			instance = (TheoremProver) tpClass.newInstance();
		} catch (ClassNotFoundException e) {
			String message =
				"Unable to find class : "
					+ className
					+ " at"
					+ System.getProperty("java.class.path");
			throw new RuntimeException(message);
		} catch (InstantiationException e) {
			String message =
				"Class "
					+ className
					+ " could not be instantiated : "
					+ e.getMessage();
			throw new RuntimeException(message);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e.getMessage());
		}

		return instance;
	}

	/** private constructor to avoid instantiation */
	private TheoremProverFactory() {

	}

}
