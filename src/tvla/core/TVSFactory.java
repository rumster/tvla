package tvla.core;

import java.util.Iterator;

import tvla.core.generic.GenericBaseTVS;
import tvla.exceptions.UserErrorException;
import tvla.predicates.DynamicVocabulary;
import tvla.util.Logger;
import tvla.util.ProgramProperties;
import tvla.util.PropertiesEx;

/** A Factory to generate core objects such as structures and sets of structures.
 * The objects returned by the factory depend on specific TVS representations
 * that can be specified by the tvla.tvsFactoryClass property.
 * @author Ganesan Ramalingam.
 * @author Roman Manevich.
 * @author Deepak Goyal.
 * @author John Field.
 * @author Mooly Sagiv.
 */
public class TVSFactory {
	public static final int JOIN_RELATIONAL				= 0;
	public static final int JOIN_INDEPENDENT_ATTRIBUTES = 1;
	public static final int JOIN_CANONIC				= 2;
	public static final int JOIN_CANONIC_EMBEDDING		= 3;
	public static final int JOIN_J3						= 4;
	public static final int JOIN_CONCRETE				= 5;
	public static int joinMethod = JOIN_RELATIONAL;
	
	/** Initialization method: gives the factory an opportunity to do any
	 * necessary preprocessing.
	 * @author Ganesan Ramalingam.
	 */
	public void init() {
	}
	
	public static void reset() {
		instance = null;
	}
	
	/** The one and only instance of this factory.
	 */
	private static TVSFactory instance;
	
	/** Returns the one and only instance of this factory.
	 */
	public static TVSFactory getInstance() {
		if (instance == null) {
			String implementation = ProgramProperties.getProperty("tvla.implementation", "generic");
			setTVSFactoryClass(implementation);
		}
		return instance;
	}
	
	/** Returns a new empty structure.
	 */
	public HighLevelTVS makeEmptyTVS() {
		return new GenericBaseTVS();
	}

	/** Returns an empty set of structures.
	 * @param freezeStructures Specifies whether the set should freeze structures
	 * when they are joined (make them immutable).
	 * @author Roman Manevich.
	 * @since 20.11.2001 Initial creation.
	 */
	public TVSSet makeEmptySet(boolean freezeStructures) {
		// Currently this method is used for the hybrid base-bdd implementation.
		// In the future it may be removed.
		return makeEmptySet();
	}
	
	/** Returns an empty set of structures.
	 */
	public TVSSet makeEmptySet() {
		TVSSet result = null;
		
		switch (joinMethod) {
		case JOIN_RELATIONAL: 
			result = new tvla.core.generic.GenericHashTVSSet();
			break;
		case JOIN_INDEPENDENT_ATTRIBUTES: 
			result = new tvla.core.generic.GenericSingleTVSSet();
			break;
		case JOIN_CANONIC:
			//result = new tvla.core.generic.GenericPartialJoinTVSSet(); // unoptimized implementation (quadratic behaviour)
			result = new tvla.core.generic.GenericHashPartialJoinTVSSet(); // optimized implementation
			break;
		case JOIN_CANONIC_EMBEDDING:
			result = new tvla.core.generic.GenericCanonicEmbeddingTVSSet();			
			break;
		case JOIN_J3:
			result = new tvla.core.generic.GenericEmbeddingTVSSet();
			break;
		case JOIN_CONCRETE:
			result = new tvla.core.generic.ConcreteTVSSet();
			break;
		default:
			throw new IllegalStateException("Unknown Join method!");
		}
		
		return result;
	}
	
	
	/** Returns an empty set of structures.
	 */
	public TVSSet makeEmptySet(int joinMode) {
		TVSSet result = null;
		
		switch (joinMode) {
		case JOIN_RELATIONAL: 
			result = new tvla.core.generic.GenericHashTVSSet();
			break;
		case JOIN_INDEPENDENT_ATTRIBUTES: 
			result = new tvla.core.generic.GenericSingleTVSSet();
			break;
		case JOIN_CANONIC:
			result = new tvla.core.generic.GenericPartialJoinTVSSet();
			break;
		case JOIN_CANONIC_EMBEDDING:
			result = new tvla.core.generic.GenericCanonicEmbeddingTVSSet();			
			break;
		case JOIN_J3:
			result = new tvla.core.generic.GenericEmbeddingTVSSet();
			break;
		case JOIN_CONCRETE:
			result = new tvla.core.generic.ConcreteTVSSet();
			break;
		default:
			throw new IllegalStateException("Unknown Join method!");
		}
		
		return result;
	}
	
	/** Prints factory specific statistics to the log stream.
	 */
	public static void printStatistics() {
		instance.dumpStatistics();
		Canonic.CanonicNamesStatistics.dumpNames(); // conditionally dumps statistics		
	}

	/** Let's the user instantiate a factory class from a string.
	 * @author Roman Manevich.
	 * @since 24.11.2001 Initial creation.
	 */
	public static void setTVSFactoryClass(String className) {
		instance = getTVSFactoryClass(className);
	}
	
	/** Override this method to collect factory specific information.
	 * @author Roman Manevich.
	 * @since 24.11.2001 Initial creation.
	 */
	public void collectStatisticsInfo() {
	}
	
	/** Override this method to collect runtime space statistics.
	 * @param structureIter An iterator over the set of all the structure
	 * that exist during one point of time in the analysis.
	 * @author Roman Manevich.
	 * @since 2.1.2002 Initial creation.
	 */
	public void collectTVSSizeInfo(Iterator structureIter) {
	}
	
	/** Constructs a factory and initializes variables from properties.
	 */
	protected TVSFactory() {
		String joinType = ProgramProperties.getProperty("tvla.joinType", "rel");
		if (joinType.equals("rel"))
			TVSFactory.joinMethod = TVSFactory.JOIN_RELATIONAL;
		else if (joinType.equals("ind")) {
			TVSFactory.joinMethod = TVSFactory.JOIN_INDEPENDENT_ATTRIBUTES;
			Focus.needToFocusOnActive = true;
		}
		else if (joinType.equals("part"))
			TVSFactory.joinMethod = TVSFactory.JOIN_CANONIC;
		else if (joinType.equals("part_embedding"))
			TVSFactory.joinMethod = TVSFactory.JOIN_CANONIC_EMBEDDING;
		else if (joinType.equals("j3"))
			TVSFactory.joinMethod = TVSFactory.JOIN_J3;
		else if (joinType.equals("conc"))
			TVSFactory.joinMethod = TVSFactory.JOIN_CONCRETE;
		else {
			throw new UserErrorException("Invalid property value specified for tvla.joinType : " + joinType);
		}
	}
	
	/** Instantiates a factory class from a string.
	 * @author Roman Manevich.
	 * @since 24.11.2001 Initial creation.
	 */
	protected static TVSFactory getTVSFactoryClass(String implementation) {
		// the default is set to implementation to allow users to pass factory class names
		String className = new PropertiesEx("/tvla/core/tvla.core.properties").getProperty(implementation, implementation);
		
		TVSFactory result = null;
		try {
			Class factoryClass = Class.forName(className);
			result = (TVSFactory) factoryClass.newInstance();
		}
		catch (ClassNotFoundException e) {
			String message = "Unable to find factory class : " + className + " at" +
							 System.getProperty("java.class.path");
			throw new RuntimeException(message);
		}
		catch (InstantiationException e) {
			String message = "Factory class " + className +
							 " could not be instantiated : " + e.getMessage();
			throw new RuntimeException(message);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e.getMessage());
		}
		return result;
	}

	/** Override this method to print factory specific information.
	 */
	protected void dumpStatistics() {
		Logger.println("Factory " + instance.getClass().toString());
	}

    public HighLevelTVS makeEmptyTVS(DynamicVocabulary newVoc) {
        return new GenericBaseTVS(newVoc);
    }
}