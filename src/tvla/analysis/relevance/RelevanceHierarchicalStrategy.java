/*
 * Created on Sep 9, 2003
 */
package tvla.analysis.relevance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import tvla.core.TVS;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.Logger;

/**
 * @author Eran Yahav eyahav
 */
public class RelevanceHierarchicalStrategy extends RelevanceMultiStrategy {

  	/**
  	 * Name prefix of ignore-choice-predicate recording which quantifiers need-not be 
  	 * assigned by the current analysis phase. 
  	 */
	protected static final String IGNORE_CHOICE_PREDICATE = "ich";

	/**
  	 * Constructor
  	 * @param engine - owner engine that is using this strategy
  	 * @param runInParallel - should analysis be run in parallel? 
  	 */
	public RelevanceHierarchicalStrategy(
		RelevanceEngine engine,
		boolean runInParallel) {
		super(engine, runInParallel);
	}

	/**
	* perform hieraricahl evaluation, 
	* the order of quantifiers is determined by the property 
	* tvla.relevantAnalysis.hierarchical.quantorder 
	* 
	* @TODO: replace the way quantifier-order is being 
	* communicated into TVLA. In particular, we need to allow
	* the front-end to pass it via some persistent form (TVP file?)
	* and not as a command-line parameter.
	* 
	* @param initial
	*/
	public void evaluate(Collection initial) {

		List quantifierOrder = getQuantifierOrder();

		List productTuples = generateProductTuples(quantifierOrder);

		int quantifierNumber = quantifierOrder.size();
		int currNumber = 1;

		if (productTuples.isEmpty()) {
			Logger.fatalError(
				"No relevant locations found. Analysis terminated.");
		}

		while (!productTuples.isEmpty() && currNumber <= quantifierNumber) {

			List currQuantifiers =
				quantifierOrder.subList(
					quantifierNumber - currNumber,
					quantifierNumber);

			List ignoredQuantifiers =
				quantifierOrder.subList(0, quantifierNumber - currNumber);

			Logger.println("Current quantifiers = " + currQuantifiers);
			Logger.println("Ignored quantifiers = " + ignoredQuantifiers);
			Logger.println("Product tuples size = " + productTuples.size());

			Collection currInitialStructures =
				initQuantifierPredicates(ignoredQuantifiers, initial);

			Map currTuples =
				RelevanceTuple.project(
					productTuples,
					quantifierNumber - currNumber,
					quantifierNumber);

			evaluateStepInSequence(
				currInitialStructures,
				currTuples,
				productTuples);

			currNumber++;
		}
	}

	/**
	 * note that this removes items from productTuples
	 * @param currInitialStructures
	 * @param currTuples
	 * @param productTuples
	 */
	protected void evaluateStepInSequence(
		Collection currInitialStructures,
		Map currTuples,
		Collection productTuples) {
		for (Iterator it = currTuples.keySet().iterator(); it.hasNext();) {
			List currTuple = (List) it.next();
			Logger.println(
				"------------------------------------------------------");
			Logger.println("Analyzing with " + currTuple + " chosen.");
			Logger.println(
				"------------------------------------------------------");
			boolean currResult =
				engine.evaluationStep(currTuple, currInitialStructures);
			if (currResult) {
				Logger.println(
					"------------------------------------------------------");
				Logger.println("Tuple " + currTuple + " succesfully verified.");
				Logger.println(
					"------------------------------------------------------");

				productTuples.removeAll((List) currTuples.get(currTuple));
			}
		}
	}

	/**
	 * returns a collection of structure that were initialized according  
	 * to which quantifiers should be ignored 
	 * 
	 * @param ignoredQuantifiers
	 * @param initial
	 * @return a collection of structures with initialized values for 
	 * the quantifier-predicates.
	 */
	private Collection initQuantifierPredicates(
		List ignoredQuantifiers,
		Collection initial) {
		Collection result = new ArrayList();
		for (Iterator it = initial.iterator(); it.hasNext();) {
			TVS structure = (TVS) it.next();
			TVS newStructure = structure.copy();
			for (Iterator pit = ignoredQuantifiers.iterator();
				pit.hasNext();
				) {
				String quantName = (String) pit.next();
				String qPredName =
					IGNORE_CHOICE_PREDICATE + "[" + quantName + "]";
				Predicate qPredicate = Vocabulary.getPredicateByName(qPredName);
				if (qPredicate == null)
					throw new RuntimeException(
						"Predicate " + qPredName + " used but not defined");

				System.out.println(
					"Predicate " + qPredicate + " udpate to true");

				newStructure.update(qPredicate, Kleene.trueKleene);
			}
			System.out.println("-- added structure --");
			result.add(newStructure);
		}
		return result;
	}

}
