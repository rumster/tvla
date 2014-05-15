/*
 * Created on Sep 9, 2003
 */
package tvla.analysis.relevance;

import java.util.Collection;

/**
 * Determines the manner in which separation takes place. 
 * @author Eran Yahav eyahav
 */
public abstract class RelevanceEvaluationStrategy {

  	/**
  	 * should the analysis be performed in parallel (simultaneously) or sequentially (separately)?
  	 */
	protected boolean runInParallel = false;
	/**
	 * owner engine using this strategy
	 */
	protected RelevanceEngine engine;

	/**
	 * run the strategy. uses the runInParallel field to determine if analysis should 
	 * be performed sequentially or simultaneously for the set of generated tuples.
	 * @param initial - an initial set of strcutres providing the input to the analysis
	 */
	public abstract void evaluate(Collection initial);

}
