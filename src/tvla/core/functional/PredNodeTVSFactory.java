package tvla.core.functional;

import tvla.core.HighLevelTVS;
import tvla.core.TVSFactory;
import tvla.core.TVSSet;
import tvla.predicates.DynamicVocabulary;

@Deprecated
public class PredNodeTVSFactory extends TVSFactory {
	public PredNodeTVSFactory() {
		super();
	}

    /** Returns a new empty structure.
     */
    public HighLevelTVS makeEmptyTVS (DynamicVocabulary vocabulary) {
        throw new RuntimeException("PredNodeTVSFactory implementation no longer available.");
    }

    public HighLevelTVS makeEmptyTVS () {
		throw new RuntimeException("PredNodeTVSFactory implementation no longer available.");
	}

	public TVSSet makeEmptySet () {
		throw new RuntimeException("PredNodeTVSFactory implementation no longer available.");
	}

	public void init() {
	}

	/** A trick used to add a dependency between this class and TVSFactory,
	 * so that when TVSFactory gets compiled, this class gets compiled as well.
	 * @author Roman Manevich.
	 * @since 23.11.2001 Initial creation.
	 */
	public static void compileMe() {
	}
}
