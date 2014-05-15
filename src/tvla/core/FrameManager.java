package tvla.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tvla.core.decompose.DecompositionName;
import tvla.core.decompose.PClauseName;
import tvla.exceptions.SemanticErrorException;
import tvla.formulae.EquivalenceFormula;
import tvla.formulae.Formula;
import tvla.formulae.PredicateFormula;
import tvla.predicates.Predicate;
import tvla.transitionSystem.Action;
import tvla.util.HashMapFactory;

/**
 * Manage framers for decomposition names
 */
public class FrameManager {
	Map<Formula, Framer> framers = HashMapFactory.make();
	
	/**
	 * Build framer from specification formula of the form:
	 * \/{ component_spec_i /\ frame_spec_i }
	 * component_spec is a conjuction of predicates formulas matching a decomposition
	 * name for which all the given predicates are decomposition predicates.
	 * frame_spec - see Framer.
	 * @param updatedVocabulary The predicates that can be updated between frame and unframe.
	 */
	public FrameManager(Formula formula, Action action) {
		if (formula == null) return;
		
		List<Formula> frameForComponents = new ArrayList<Formula>();
		Formula.getOrs(formula, frameForComponents);
		for (Formula frameForComponent : frameForComponents) {
			assert frameForComponent instanceof EquivalenceFormula;
			EquivalenceFormula asEq = (EquivalenceFormula) frameForComponent;
			Formula componentNameFormula  = asEq.left();
			Formula frameFormula = asEq.right();
			Framer framer = new Framer(frameFormula, action);
			framers.put(componentNameFormula, framer);
		}
	}

	/**
	 * Get the framer which matches this composition name.
	 * An exception is thrown if more than one framer matches.
	 */
	public Framer getFramer(DecompositionName name) {
		PClauseName pname = (PClauseName) name;
		Framer result = null;
		Formula resultName = null;
		FRAMER: for (Map.Entry<Formula, Framer> entry : framers.entrySet()) {
			Formula componentNameFormula = entry.getKey();
			Framer framer = entry.getValue();

			// Go over all conjuncts and make sure they match the decomposition name.
			List<Formula> conjuncts = new ArrayList<Formula>();
			Formula.getAnds(componentNameFormula, conjuncts);
			for (Formula conjunct : conjuncts) {
				assert conjunct instanceof PredicateFormula;
				PredicateFormula pformula = (PredicateFormula) conjunct;
				Predicate predicate = pformula.predicate();
				if (!pname.getDisjuncts().contains(predicate)) {
					// No match - go to next framer
					continue FRAMER;
				}
			}
			if (result != null) {
				throw new SemanticErrorException("Multiple matches for framer - " + resultName + " and " + componentNameFormula);
			}
			// Record match (name is recorded for exception message)
			result = framer;
			resultName = componentNameFormula;
		}
		return result;
	}
}
