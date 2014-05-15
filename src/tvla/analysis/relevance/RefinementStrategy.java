/*
 * Created on Oct 1, 2003
 */
package tvla.analysis.relevance;

/**
 * A refinement strategy iteratively refines the abstraction.
 * 
 * @author Eran Yahav eyahav
 */
public abstract class RefinementStrategy extends RelevanceOuterStrategy {

  /**
   * constructor
   * @param engine - owner engine that is using the strategy
   * @param runInParallel - should analysis be performed in parallel (simultaneously)?
   */
  public RefinementStrategy(RelevanceEngine engine, boolean runInParallel) {
    super(engine, runInParallel);
  }

  /**
   * applies a refinement step.
   * 
   * @return true when abstraction was refined, false otherwise.
   */
  public abstract boolean refine();

}