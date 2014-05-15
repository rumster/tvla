package tvla.iawp.tp;

import tvla.formulae.Formula;

/**
 * A listener to be notified on changes in the assumption pool
 * @author Eran Yahav (eyahav)
 */
public interface AssumptionsChangeListener {

	public void assumptionAddedEvent(Formula f);
	public void assumptionRemovedEvent(Formula f);
}

