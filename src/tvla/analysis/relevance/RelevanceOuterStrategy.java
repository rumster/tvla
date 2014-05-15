/*
 * Created on Sep 9, 2003
 */
package tvla.analysis.relevance;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import tvla.relevance.RelevanceEnvironment;
import tvla.relevance.RelevanceTypeInformation;
import tvla.transitionSystem.AnalysisGraph;
import tvla.transitionSystem.RelevantAnalysisGraph;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Logger;
import tvla.util.ProgramProperties;

/**
 * @author Eran Yahav eyahav
 */
public class RelevanceOuterStrategy extends RelevanceEvaluationStrategy {

  	/**
  	 * create a new relevance outer strategy 
  	 * @param engine - owner engine who is using this strategy
  	 * @param runInParallel - should analysis be run in parallel (simultanesou)?
  	 */
	public RelevanceOuterStrategy(
		RelevanceEngine engine,
		boolean runInParallel) {
		this.engine = engine;
		this.runInParallel = runInParallel;
	}

	/**
	* perform iterative evaluation of outer mode
	* 
	* @param initial
	*/
	public void evaluate(Collection initial) {
		if (runInParallel)
			evaluateInParallel(initial);
		else
			evaluateInSequence(initial);
	}

	/**
	 * each assignment is executed separately
	 * @param initial
	 */
	public void evaluateInSequence(Collection initial) {

		Map nameToLabels = getNameToLabelsFromProgram();

		if (nameToLabels.isEmpty()) {
			Logger.fatalError("No relevant locations found. Analysis terminated.");
		}
	
		for (Iterator ntl = nameToLabels.entrySet().iterator();
			ntl.hasNext();
			) {
			Map.Entry currEntry = (Map.Entry) ntl.next();
			Collection relevantLabels = (Collection) currEntry.getValue();
			for (Iterator labelIt = relevantLabels.iterator();
				labelIt.hasNext();
				) {
				String currLabel = (String) labelIt.next();

				Logger.println(
					"------------------------------------------------------");
				Logger.println("Analyzing with " + currLabel + " chosen.");
				Logger.println(
					"------------------------------------------------------");

				Collection relevanceSet = Collections.singleton(currLabel);
				engine.evaluationStep(relevanceSet, initial);
			}
		}
	}

	/**
	 * all assignments running together in the same analysis
	 * @param initial
	 */
	public void evaluateInParallel(Collection initial) {

		Map nameToLabels = getNameToLabelsFromProgram();
		Collection relevanceSet = HashSetFactory.make();

		if (nameToLabels.isEmpty()) {
			Logger.fatalError("No relevant locations found. Analysis terminated.");
		}
	

		for (Iterator ntl = nameToLabels.entrySet().iterator();
			ntl.hasNext();
			) {
			Map.Entry currEntry = (Map.Entry) ntl.next();
			Collection relevantLabels = (Collection) currEntry.getValue();
			for (Iterator labelIt = relevantLabels.iterator();
				labelIt.hasNext();
				) {
				String currLabel = (String) labelIt.next();
				relevanceSet.add(currLabel);
			}
		}

		assert !relevanceSet.isEmpty() : "No relevant locations found. Analysis terminated." ;

		Logger.println(
			"------------------------------------------------------");
		Logger.println(
			"Analyzing in parallel with " + relevanceSet + " chosen.");
		Logger.println(
			"------------------------------------------------------");

		engine.evaluationStep(relevanceSet, initial);

	}

	/**
	 * returns a map of type String->Collection containing the mapping
	 * of each name to its relevant labels as obtained from the CFG
	 * @return
	 */
	protected Map getNameToLabelsFromProgram() {
		RelevantAnalysisGraph activeGraph =
			(RelevantAnalysisGraph) AnalysisGraph.activeGraph;
		Collection relevantNames = getRelevantNames(activeGraph);
		return getLabelsForName(activeGraph, relevantNames);
	}

	/**
	 * returns a Map of type String->Collection containing the mapping
	 * of each name to its relevant labels
	 * @param activeGraph
	 * @param relevantNames
	 * @return Map of type String->Collection
	 */
	protected Map getLabelsForName(
		RelevantAnalysisGraph activeGraph,
		Collection relevantNames) {
		// String->Collection
		Map result = HashMapFactory.make();
		
		
		if (relevantNames.isEmpty()) {
			System.out.println("Warning: no relevant names found.");
		}
		
		for (Iterator nameIt = relevantNames.iterator(); nameIt.hasNext();) {
			String currName = (String) nameIt.next();

			Collection currRelevantLabels = activeGraph.labelsForName(currName);
			Logger.println(
				"------------------------------------------------------");
			Logger.println(
				"found "
					+ currRelevantLabels.size()
					+ " relevant labels for "
					+ currName
					+ ".");
			Logger.println(
				"------------------------------------------------------");
			result.put(currName, currRelevantLabels);
		}
		return result;
	}

	/**
	 * Get a set of relevant type names from the specified outer class (if specified)
	 * or from the actual relevant locations in the program.
	 * When an outer class name is specified, all of its subtypes are also
	 * considered to be relevant names.
	 * For example, specifying java.util.Collection as the outer name will 
	 * cause locations in which (transitive) subtypes such as java.util.ArrayList
	 * to be considered relevant as well.
	 * 
	 * Inclusion of the subtypes could be controlled by using the boolean
	 * property - tvla.relevantAnalysis.outer.includesubtypes, which 
	 * is true by default.
	 * 
	 * When an outer class name is not specified, the outer quantifier is taken
	 * to be the set of ALL relevant locations in the program. Thus, this
	 * option should only be used when there is a single quantifier. 
	 * 
	 * 
	 * @param activeGraph representation of program's call graph
	 * @return Collection of relevant class names
	 */
	protected Collection getRelevantNames(RelevantAnalysisGraph activeGraph) {

		Collection result = HashSetFactory.make();

		String outerClass =
			ProgramProperties.getProperty(
				"tvla.relevantAnalysis.outerclass",
				null);

		boolean includeSubtypes =
			ProgramProperties.getBooleanProperty(
				"tvla.relevantAnalysis.outer.includesubtypes",
				true);

		Collection relevantNames = activeGraph.relevantNames();

		if (outerClass == null) {
			result = relevantNames;
			if (relevantNames.size() > 1)
				Logger.println(
					"Warning: outer mode with more than one relevant name.");
		} else {

			Collection specifiedNames;

			if (includeSubtypes) {
				RelevanceTypeInformation rti =
					RelevanceEnvironment.getInstance().getRelevanceTypeInformation();
				specifiedNames = rti.getDerivedComponents(outerClass);
			} else {
				specifiedNames = Collections.singleton(outerClass);
			}

			Logger.println("Specified Names: " + specifiedNames);

			for (Iterator it = specifiedNames.iterator(); it.hasNext();) {
				String currName = (String) it.next();
				if (relevantNames.contains(currName))
					result.add(currName);
			}

			if (result.isEmpty()) {
				Logger.println(
					"Warning: outer component "
						+ outerClass
						+ " and its derived components are not relevant names.");
			} else {
				Logger.println(
					"--------------------------------------------------------------");
				Logger.println("Relevant types:" + result.toString());
				Logger.println(
					"--------------------------------------------------------------");

			}
		}
		return result;
	}
}
