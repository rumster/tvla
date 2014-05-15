package tvla.core.common;

import java.util.Set;

import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashSetFactory;
import tvla.core.TVS;
import tvla.core.base.BaseTVS;

/** A global place to store the predicates that were
 * modified by the last action.
 * @author Roman Manevich,
 * @since tvla-2-alpha Nomber 18 2002, Initial creation.
 */
public class ModifiedPredicates {
	/** A set of modified predicates.
	 */
	private static Set<Predicate> predicates = HashSetFactory.make();
	
	public static void reset() {
		predicates = HashSetFactory.make();
	}
	
	/** Adds the specified predicate to the list of modified predicates.
	 */
	public static void modify(Predicate predicate) {
		predicates.add(predicate);
	}

	public static void modify(TVS structure, Predicate predicate) {
		predicates.add(predicate);
		structure.modify(predicate);
	}

	/** Removes all predicates from the set.
	 */
	public static void clear() {
		predicates.clear();
	}
	
	/** Returns the set of predicates that were reported as modified.
	 */
	public static Set<Predicate> getModified() {
		return predicates;
	}
	
	public static void modify(TVS structure) {
        ModifiedPredicates.modify(structure, Vocabulary.active);
        if (structure instanceof BaseTVS) {
            ((BaseTVS)structure).setOriginalStructure(null);
        }
	}
}
