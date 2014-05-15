package tvla.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import tvla.core.common.ModifiedPredicates;
import tvla.predicates.Vocabulary;
import tvla.util.Logger;
import tvla.util.ProgramProperties;

/** Abstract base class for the coerce algorithm as described in the Shape Analysis article.
 * A constraint is breached if there is an assignment in which the body is 
 * evaluated to true and the head is not.
 * Each structure given is modified so that no constraint is breached, or discarded,
 * in case there is no way to do that. 
 * @see tvla.core.TVS
 * @see tvla.formulae.Formula
 * @author Tal Lev-Ami.
 */
public abstract class Coerce {
	/** When this variable is set to true, Coerce reports to the user about the
	 * structures for which there were constraints breaches that could not be repaired.
	 */
	public static boolean debug = ProgramProperties.getBooleanProperty("tvla.coerce.debug", false);

	/** Coerce the given structure using the presupplied set of constraints.
	 * Return true iff successful (i.e. all breaches could be repaired).
	 * @param structure The structure to be coerced (done in place). 
	 */
	public abstract boolean coerce(TVS structure);

	/** Coerce the given collection of structures retaining only the ones where
	 * the operation was successful.
	 * @param structures the set of structures to coerce.
	 */
	public void coerceAll(Collection<? extends TVS> structures) {
		for (Iterator<? extends TVS> iter = structures.iterator(); iter.hasNext(); ) {
			TVS structure = (TVS) iter.next();
			if (!coerce(structure)) {
				iter.remove();
			}
		}
	}
	
	public void coerceInitial(Collection<? extends TVS> structures) {
		boolean removed = false;
		ArrayList<TVS> inconsistent = new ArrayList<TVS>();
		for (Iterator<? extends TVS> iter = structures.iterator(); iter
				.hasNext();) {
			TVS structure = iter.next();
			// Hack to force checking all constraints
			ModifiedPredicates.modify(structure, Vocabulary.active);
			if (!coerce(structure)) {
				iter.remove();
				removed = true;
				inconsistent.add(structure);
			}
		}
		if (removed) {
			if (!structures.isEmpty()) {
				Logger.println();
				Logger
						.println("The following "
								+ inconsistent.size()
								+ " structures are inconsistent with the instrumentation constraints:");
				for (TVS structure : inconsistent) {
					Logger.println(tvla.io.StructureToTVS.defaultInstance
							.convert(structure));
				}
			} else {
				Logger.println();
				Logger
						.println("All input structures are inconsistent with the instrumentation constraints!");
			}
		}
	}
}