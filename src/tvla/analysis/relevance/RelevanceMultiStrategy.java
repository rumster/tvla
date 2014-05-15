/*
 * Created on Sep 9, 2003
 */
package tvla.analysis.relevance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import tvla.relevance.RelevanceEnvironment;
import tvla.relevance.RelevanceQuantifiers;
import tvla.relevance.RelevanceTypeInformation;
import tvla.transitionSystem.AnalysisGraph;
import tvla.transitionSystem.RelevantAnalysisGraph;
import tvla.util.HashSetFactory;
import tvla.util.Logger;
import tvla.util.ProgramProperties;
import tvla.util.StringUtils;

/**
 * MultiStrategy chooses a single representative 
 * for every specified quantifier, thus creating a complete assignment 
 * for the free variables resulting from dropping of quantification.
 * Technically, this amounts to generating tuples of assignments, and 
 * analyzing each tuple separately.
 *  
 * @author Eran Yahav eyahav
 */
public class RelevanceMultiStrategy extends RelevanceEvaluationStrategy {

  	/**
  	 * Constructor
  	 * @param engine - owner engine that is using this strategy
  	 * @param runInParallel - should analysis be run in parallel? 
  	 */
	public RelevanceMultiStrategy(
		RelevanceEngine engine,
		boolean runInParallel) {
		this.engine = engine;
		this.runInParallel = runInParallel;
	}

	/**
	 * Evaluate assignment-tuples sequentially by analyzing them one tuple after another.
	 * This is done by looping through tuples and invoking a separate engine evaluation step
	 * for each tuple. 
	 * @param initial - an initial set of strcutres providing the input to the analysis 
	 */
	public void evaluateInSequence(Collection initial) {
		List quantifierOrder = getQuantifierOrder();

		System.out.println("Quantifier order = " + quantifierOrder);

		List productTuples = generateProductTuples(quantifierOrder);

		if (productTuples.isEmpty()) {
			Logger.fatalError(
				"No relevant locations found. Analysis terminated.");
		}

		for (Iterator it = productTuples.iterator(); it.hasNext();) {
			List currTuple = (List) it.next();
			Logger.println(
				"------------------------------------------------------");
			Logger.println("Analyzing with " + currTuple + " chosen.");
			Logger.println(
				"------------------------------------------------------");
			engine.evaluationStep(currTuple, initial);
		}
	}

	/**
	 * Evaluate assignment-tuples in parallel by creating a set of all tuples, and 
	 * running a single engine evaluation step for all tuples (this is referred to 
	 * as simultaneous verification in the paper).
	 * @param initial - an initial set of strcutres providing the input to the analysis
	 */
	public void evaluateInParallel(Collection initial) {
		List quantifierOrder = getQuantifierOrder();
		Set allTupleLabels = HashSetFactory.make();

		System.out.println("Quantifier order = " + quantifierOrder);

		List productTuples = generateProductTuples(quantifierOrder);

		if (productTuples.isEmpty()) {
			Logger.fatalError(
				"No relevant locations found. Analysis terminated.");
		}

		for (Iterator it = productTuples.iterator(); it.hasNext();) {
			List currTuple = (List) it.next();
			allTupleLabels.addAll(currTuple);
		}

		Logger.println(
			"------------------------------------------------------");
		Logger.println(
			"Analyzing in parallel with " + allTupleLabels + " chosen.");
		Logger.println(
			"------------------------------------------------------");

		engine.evaluationStep(allTupleLabels, initial);
	}

	/**
	 * run the strategy. uses the runInParallel field to determine if analysis should 
	 * be performed sequentially or simultaneously for the set of generated tuples.
	 * @param initial - an initial set of strcutres providing the input to the analysis
	 */
	public void evaluate(Collection initial) {
		if (runInParallel)
			evaluateInParallel(initial);
		else
			evaluateInSequence(initial);
	}

	/**
	* get quantifier order, either from the property 
	* - tvla.relevantAnalysis.qorder - 
	* or from the relevance.xml file.
	* 
	* @return a list of the quantifier, ordered from outermost
	* quantifier to innermost one.
	*/
	protected List getQuantifierOrder() {

		String quantifierOrderString =
			ProgramProperties.getProperty("tvla.relevantAnalysis.qorder", "");

		List result = StringUtils.breakString(quantifierOrderString, ";");

		// try to get it from relevance information
		if (result.isEmpty()) {
			RelevanceQuantifiers rq =
				RelevanceEnvironment.getInstance().getRelevanceQuantifiers();
			result = rq.getOrder();
		}

		// remove dead quantifiers (that have no active lables)
		for (Iterator it = result.iterator(); it.hasNext();) {
			String currQuant = (String) it.next();
			if (getQuantifierRelevantLabels(currQuant).isEmpty()) {
				Logger.println(
					"Quantifier "
						+ currQuant
						+ " had no labels and was removed");
				it.remove();
			}
		}

		Logger.println("QuantOrder : " + result);
		return result;
	}

	/**
	 * use the specified quantifier list to generate a set of tuples assigning possible 
	 * relevant locations. Each quantifier in the list is matched-up against 
	 * its possible relevant locations. Once each quantifier has a set of corresponding relevant 
	 * locations, the cross-product is computed, yielding tuples that assign all possible 
	 * combinations of quantifier selsection.
	 * (in principle, we can avoid some combinations when more than a single quantifier involves
	 * the same type due to symmetry, but we are currently not doing it).
	 * @param quantifierOrder - a list of quantifier to be assigned.
	 * @return a list of cross-product tuples.
	 */
	protected List generateProductTuples(List quantifierOrder) {

		RelevantAnalysisGraph activeGraph =
			(RelevantAnalysisGraph) AnalysisGraph.activeGraph;

		RelevanceTypeInformation rti =
			RelevanceEnvironment.getInstance().getRelevanceTypeInformation();

		Collection relevantNames = activeGraph.relevantNames();

		List assignmentTuples = new ArrayList();
		for (Iterator it = quantifierOrder.iterator(); it.hasNext();) {
			String qTypeName = (String) it.next();
			List currQuantLabels = new ArrayList();

			Collection relevantSubtypes = rti.getDerivedComponents(qTypeName);
			for (Iterator sit = relevantSubtypes.iterator(); sit.hasNext();) {
				String currName = (String) sit.next();

				if (!relevantNames.contains(currName)) {
					Logger.println(
						"Warning: unknown name "
							+ currName
							+ " in specified quantifier order.");
				}
				Collection relevantLabels = activeGraph.labelsForName(currName);
				if (relevantLabels != null) {
					currQuantLabels.addAll(relevantLabels);
					Logger.println(
						"Name " + currName + " got " + relevantLabels);
				}
			}
			if (currQuantLabels.isEmpty()) {
				Logger.println("Warning: got an empty list for a quantifier");
			} else {
				assignmentTuples.add(currQuantLabels);
			}
		}

		List productTuples = RelevanceTuple.cartesianProduct(assignmentTuples);

		Logger.println("productTuples = " + productTuples);

		return productTuples;
	}

	/**
	 * Retrieve the relevant locations for a given type name (quantifier name).
	 * @param qTypeName - the name of the quantifier (the quantified type).
	 * @return a collection of the relevant labels for the quantifier.
	 * @TODO: this should cache values to increase performance
	 */
	protected Collection getQuantifierRelevantLabels(String qTypeName) {
		RelevantAnalysisGraph activeGraph =
			(RelevantAnalysisGraph) AnalysisGraph.activeGraph;

		RelevanceTypeInformation rti =
			RelevanceEnvironment.getInstance().getRelevanceTypeInformation();
		Collection currQuantLabels = new ArrayList();
		Collection relevantNames = activeGraph.relevantNames();
		Collection relevantSubtypes = rti.getDerivedComponents(qTypeName);
		for (Iterator sit = relevantSubtypes.iterator(); sit.hasNext();) {
			String currName = (String) sit.next();
			Collection relevantLabels = activeGraph.labelsForName(currName);
			if (relevantLabels != null) {
				currQuantLabels.addAll(relevantLabels);
			}
		}
		return currQuantLabels;
	}

}
