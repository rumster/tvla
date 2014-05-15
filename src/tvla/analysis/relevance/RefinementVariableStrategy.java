/*
 * Created on Oct 1, 2003
 */
package tvla.analysis.relevance;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.transitionSystem.AnalysisGraph;
import tvla.transitionSystem.RelevantAnalysisGraph;
import tvla.util.HashSetFactory;
import tvla.util.Logger;

/**
 * RefinementVariableStrategy refines the abstraction by making additional
 * variable-predicates into abstraction predicates. The effect is currently
 * global - meaning that when a variable-predicate is refined to be an
 * abstraction predicate, it remains an abstraction predicate throughout the
 * analysis. The could (and should) be made into a local refinement rather than
 * a global one.
 * 
 * @author Eran Yahav eyahav
 */
public class RefinementVariableStrategy extends RefinementStrategy {

  /**
   * assume reference-variable predicates have a certain form - starting with
   * this suffix this is an assumption that could be avoided and is made here
   * for simplicity of implementation.
   */
  protected static final String PREDICATE_PREFIX = "ref";

  /**
   * A collection of already-refined variables.
   */
  protected Collection refinedVars = HashSetFactory.make();

  /**
   * contructor
   * 
   * @param engine -
   *          owner engine that is using the strategy
   * @param runInParallel -
   *          should analysis be performed in parallel (simultaneously)?
   */
  public RefinementVariableStrategy(RelevanceEngine engine, boolean runInParallel) {
    super(engine, runInParallel);
  }

  /*
   * (non-Javadoc)
   * 
   * @see tvla.analysis.relevance.RefinementStrategy#refine()
   */
  public boolean refine() {
    boolean result = false;
    boolean foundRefinementMessage = false;
    RefinementMessage refinementMessage = null;
    Collection messages = engine.getStepMessages();

    System.out.println("--- Refine Starting...");

    for (Iterator mit = messages.iterator(); mit.hasNext() && !foundRefinementMessage;) {
      StringBuffer messageBuffer = (StringBuffer) mit.next();
      String message = messageBuffer.toString();

      System.out.println("MSG: " + message);

      if (RefinementMessage.isRefinementMessage(message)) {
        System.out.println("Found refinement message.");
        foundRefinementMessage = true;
        refinementMessage = RefinementMessage.getRefinementMessage(message);
      }
    }

    if (foundRefinementMessage) {
      result = refineVariable(refinementMessage);
    }
    return result;
  }

  /**
   * refine with respect to a specific variable.
   * 
   * @param refinementMessage
   * @return true if refinement applied to this variable. false if it was
   *         already previously refined.
   */
  protected boolean refineVariable(RefinementMessage refinementMessage) {
    boolean result = false;
    String varName = refinementMessage.var();
    String predicateName = PREDICATE_PREFIX + "[" + varName + "]";
    //@TODO: for now we assume the var's predicate is ref[var]
    Predicate predicate = Vocabulary.getPredicateByName(predicateName);
    if (predicate == null)
      throw new RuntimeException("Could not find predicate " + predicateName);

    if (!predicate.abstraction() && !refinedVars.contains(varName)) {
      System.out.println("Enabling abstraction for predicate " + predicate);
      Vocabulary.setAbstractionProperty(predicate, true);
      refinedVars.add(varName);
      result = true;
    }
    return result;
  }

  /**
   * run the strategy. uses the runInParallel field to determine if analysis
   * should be performed sequentially or simultaneously for the set of generated
   * tuples.
   * @param initial - an initial set of strcutres providing the input to the analysis
   */
  public void evaluate(Collection initial) {
    if (runInParallel)
      evaluateInParallel(initial);
    else
      evaluateInSequence(initial);
  }

  /**
   * each assignment is executed separately
   * 
   * @param initial
   */
  public void evaluateInSequence(Collection initial) {

    System.out.println("RefinementVariableStrategy starting...");

    RelevantAnalysisGraph activeGraph = (RelevantAnalysisGraph) AnalysisGraph.activeGraph;

    Collection relevantNames = getRelevantNames(activeGraph);

    boolean restart;

    do {
      restart = false;
      for (Iterator nameIt = relevantNames.iterator(); nameIt.hasNext() && !restart;) {
        String currName = (String) nameIt.next();

        Collection relevantLabels = activeGraph.labelsForName(currName);
        Logger.println("------------------------------------------------------");
        Logger.println("found " + relevantLabels.size() + " relevant labels for " + currName + ".");
        Logger.println("------------------------------------------------------");

        for (Iterator labelIt = relevantLabels.iterator(); labelIt.hasNext() && !restart;) {
          String currLabel = (String) labelIt.next();

          Logger.println("------------------------------------------------------");
          Logger.println("Analyzing with " + currLabel + " chosen.");
          Logger.println("------------------------------------------------------");

          Collection relevanceSet = Collections.singleton(currLabel);
          boolean noMessages = engine.evaluationStep(relevanceSet, initial);
          if (!noMessages)
            restart = refine();

          if (restart)
            System.out.println("Restarting...");
        }
      }
    } while (restart);
  }

}
