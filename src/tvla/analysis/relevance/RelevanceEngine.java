/*
 * Created on Aug 28, 2003
 */
package tvla.analysis.relevance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.IntraProcEngine;
import tvla.core.HighLevelTVS;
import tvla.io.IOFacade;
import tvla.relevance.RelevanceEnvironment;
import tvla.transitionSystem.AnalysisGraph;
import tvla.transitionSystem.Location;
import tvla.transitionSystem.RelevantAnalysisGraph;
import tvla.util.ProgramProperties;

/**
 * @author Eran Yahav (eyahav)
 */
public class RelevanceEngine extends IntraProcEngine {

  	/**
  	 * strategy used to perform the actual separation.
  	 */
	protected RelevanceEvaluationStrategy strategy;
	/**
	 * separation settings and information provided by the frontend 
	 * on the type-structure of relevant types.
	 */
	protected RelevanceEnvironment relevanceEnvironment;
	/**
	 * accumulated statistics accumulating results of separate analysis phases
	 */
	protected AnalysisStatus accumulatedStats;
	/**
	 * error messages reported by the last analysis step. 
	 */
	protected Collection stepMessages = new ArrayList();


	/** Constructs and initializes an intra-procedural engine.
	 */
	public RelevanceEngine() {
		super();
		String modeString =
			ProgramProperties.getProperty(
				"tvla.relevantAnalysis.mode",
				"outer");

		boolean runInParallel =
			ProgramProperties.getBooleanProperty(
						"tvla.relevantAnalysis.parallel",
						false);

		if (modeString.equals("outer"))
			strategy = new RelevanceOuterStrategy(this,runInParallel);
		else if (modeString.equals("multi"))
			strategy = new RelevanceMultiStrategy(this,runInParallel);
		else if (modeString.equals("hierarchy"))
			strategy = new RelevanceHierarchicalStrategy(this,runInParallel);
		else if (modeString.equals("refinevar"))
			strategy = new RefinementVariableStrategy(this,runInParallel);
			
		relevanceEnvironment = RelevanceEnvironment.getInstance();
	}

	/**
	 * Perform the actual analysis. 
	 * invoke evaluation implemented by the strategy.
	 * @param initial - an initial set of strcutres providing the input to the analysis   
	 */
	public void evaluate(Collection<HighLevelTVS> initial) {
		//@TODO: revive accumulated statistics
		//accumulatedStats = new AnalysisStatus();
		//Timer loadTimer = AnalysisStatus.loadTimer;
		//AnalysisStatus.loadTimer = new Timer();
		//accumulatedStats.startTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);
		
		strategy.evaluate(initial);
		
		//AnalysisStatus.loadTimer = loadTimer;
		
		//accumulatedStats.isCumulative(true);
		//accumulatedStats.stopTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);
		//accumulatedStats.printStatistics();
	}

	/**
	 * perform the actual analysis step for a given collection of relevant locations
	 * and a given collection of initial configurations (structures).
	 * @param relevanceSet - collection of relevant locations
	 * @param initial - collection of initial structures
	 * @return evaluation result - true if no messages were reported (corresponding to succesful verification)
	 * false when at least one message was reported.
	 */
	protected boolean evaluationStep(
		Collection relevanceSet,
		Collection initial) {
		boolean result = true;

		RelevantAnalysisGraph activeGraph =
			(RelevantAnalysisGraph) AnalysisGraph.activeGraph;
		//		specialize graph, initialize it, and dump program CFG to output
		activeGraph.specializeGraph(relevanceSet);
		AnalysisGraph.activeGraph.init();
		IOFacade.instance().printProgram(AnalysisGraph.activeGraph);

		// do the actual analysis 
		super.evaluate(initial);
		
		stepMessages.clear();
		for (Iterator lit = AnalysisGraph.activeGraph.getLocations().iterator();
			lit.hasNext();
			) {
			Location currLocation = (Location) lit.next();
			result = result && !currLocation.hasMessages();
			stepMessages.addAll(currLocation.messages.values());
		}

		// write results
		AnalysisGraph.activeGraph.dump();
		// rever to old graph
		activeGraph.revertGraph(relevanceSet);

		return result;
	}

	/**
	 * Get last step's error messages
	 * @return a collection of error messages produced by the last analysis step
	 */
	public Collection getStepMessages() {
		return stepMessages;
	}
}
