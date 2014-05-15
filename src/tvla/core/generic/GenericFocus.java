package tvla.core.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import tvla.analysis.AnalysisStatus;
import tvla.core.Focus;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignKleene;
import tvla.core.base.PredicateUpdater;
import tvla.core.common.GetFormulaPredicates;
import tvla.core.common.ModifiedPredicates;
import tvla.exceptions.FocusNonTerminationException;
import tvla.exceptions.SemanticErrorException;
import tvla.formulae.AllQuantFormula;
import tvla.formulae.AndFormula;
import tvla.formulae.EqualityFormula;
import tvla.formulae.ExistQuantFormula;
import tvla.formulae.Formula;
import tvla.formulae.NotFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.TransitiveFormula;
import tvla.formulae.ValueFormula;
import tvla.formulae.Var;
import tvla.io.StructureToTVS;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Logger;
import tvla.util.SingleIterator;

/**
 * A generic implementation of the Focus algorithm.
 * 
 * @since tvla-0.91 Renamed from AdvancedFocus (September 14 2001, Roman).
 * @author Tal Lev-Ami.
 */
public class GenericFocus extends Focus {
	/**
	 * A convenience object.
	 */
	protected static Focus defaultGenericFocus = new GenericFocus();

	/**
	 * Maps a Formula object to a FocusFormula object, which is its optimized
	 * form.
	 */
	private Map<Formula, FocusFormula> registeredFormulae = HashMapFactory
			.make();

	public static void reset() {
		defaultGenericFocus = new GenericFocus();
	}

	/**
	 * Returns a collection of structures, such that the specified formula
	 * evaluates to either true or false for each assignment of the free
	 * variables, and every concrete structure embedded in the given structure
	 * is embedded in one of the returned structures.
	 * 
	 * @param structure
	 *            A structure to focus.
	 * @param formula
	 *            A formula that should be brought into "focus".
	 * @return A collection of structures.
	 */
	public Collection<TVS> focus(TVS structure, Formula origFormula) {
		// Retrieve the optimized form of the formula, and if not found optimize
		// it.
		FocusFormula formula = registeredFormulae.get(origFormula);
		if (formula == null) {
			formula = new FocusFormula(origFormula);
			registeredFormulae.put(origFormula, formula);
		}

		if (!formula.isInVocabulary(structure)) {
			Set<TVS> totalAnswer = HashSetFactory.make();
			totalAnswer.add(structure);
			return totalAnswer;
		} else {
			return focusDisjunction(structure, formula);
		}
	}

	/**
	 * ROMAN: may return structures that are only focused for one of the
	 * disjuncts (but not the others).
	 */
	public Collection<TVS> buggyFocusDisjunction(TVS structure,
			FocusFormula formula) {
		Set<TVS> totalAnswer = HashSetFactory.make();

		// For each disjunct (a conjunction)
		for (int i = 0; i < formula.focusFormulae.length; i++) {
			List conjunction = (List) formula.focusFormulae[i];

			int numberOfSteps = conjunction.size();
			StructureAssign[] stepAssign = new StructureAssign[numberOfSteps + 1];
			Iterator[] stepIt = new Iterator[numberOfSteps + 1];
			stepIt[0] = new SingleIterator<StructureAssign>(
					new StructureAssign(structure, Assign.EMPTY));

			int currentStep = 0;
			while (currentStep >= 0) {
				if (stepIt[currentStep].hasNext()) {
					StructureAssign currentAssign = (StructureAssign) stepIt[currentStep]
							.next();
					stepAssign[currentStep] = currentAssign;
					if (currentStep == numberOfSteps) {
						totalAnswer.add(currentAssign.structure);
					} else {
						FocusStep step = (FocusStep) conjunction
								.get(currentStep);
						currentStep++;
						stepIt[currentStep] = step.focus(
								new StructureAssign(currentAssign)).iterator();
					}
				} else {
					currentStep--;
				}
			}
		}

		return totalAnswer;
	}

	public Collection<TVS> focusDisjunction(TVS structure, FocusFormula formula) {
		List<TVS> pendingStructures = new ArrayList<>();
		List<TVS> nextStructures = new ArrayList<>();
		pendingStructures.add(structure);

		// For each disjunct (a conjunction)
		for (int i = 0; i < formula.focusFormulae.length; i++) {
			for (TVS pendingStrucure : pendingStructures) {
				List conjunction = (List) formula.focusFormulae[i];

				int numberOfSteps = conjunction.size();
				StructureAssign[] stepAssign = new StructureAssign[numberOfSteps + 1];
				Iterator[] stepIt = new Iterator[numberOfSteps + 1];
				stepIt[0] = new SingleIterator<StructureAssign>(
						new StructureAssign(pendingStrucure, Assign.EMPTY));

				int currentStep = 0;
				while (currentStep >= 0) {
					if (stepIt[currentStep].hasNext()) {
						StructureAssign currentAssign = (StructureAssign) stepIt[currentStep]
								.next();
						stepAssign[currentStep] = currentAssign;
						if (currentStep == numberOfSteps) {
							nextStructures.add(currentAssign.structure);
						} else {
							FocusStep step = (FocusStep) conjunction
									.get(currentStep);
							currentStep++;
							stepIt[currentStep] = step.focus(
									new StructureAssign(currentAssign))
									.iterator();
						}
					} else {
						currentStep--;
					}
				}
			}
			pendingStructures = nextStructures;
			nextStructures = new ArrayList<>();
		}

		return pendingStructures;
	}
}

/**
 * This class describes an association of a structure and assignments to it.
 * 
 * @author Tal Lev-Ami
 */
class StructureAssign {
	public TVS structure;
	public List<Assign> assigns;

	/**
	 * Constructs a StructureAssign object by creating a deep copy of the
	 * specified object.
	 * 
	 * @param other
	 *            The StructureAssign to copy into this object.
	 */
	public StructureAssign(StructureAssign other) {
		this.structure = other.structure;
		this.assigns = new ArrayList<Assign>();
		for (Iterator<Assign> i = other.assigns.iterator(); i.hasNext();) {
			Assign assign = i.next();
			this.assigns.add(new Assign(assign));
		}
	}

	/**
	 * Constructs a StructureAssign object from the specified specified
	 * structure and a list of assignments.
	 * 
	 * @param structure
	 *            A structure.
	 * @param assigns
	 *            A list of Assign objects.
	 */
	public StructureAssign(TVS structure, List<Assign> assigns) {
		this.assigns = assigns;
		this.structure = structure;
	}

	/**
	 * Constructs a StructureAssign object from the specified structure and a
	 * single assignment.
	 * 
	 * @param structure
	 *            A structure.
	 * @param newAssign
	 *            An assignment.
	 */
	public StructureAssign(TVS structure, Assign newAssign) {
		this.structure = structure;
		this.assigns = new ArrayList<Assign>();
		if (newAssign != null)
			this.assigns.add(new Assign(newAssign));
	}

	/**
	 * Constructs a StructureAssign object from the specified structure and an
	 * iterator to a list of assignments. This constructor does a deep-copy of
	 * the specified list.
	 * 
	 * @param structure
	 *            A structure.
	 * @param newAssign
	 *            An assignment.
	 */
	public StructureAssign(TVS structure, Iterator iterator) {
		this.structure = structure;
		this.assigns = new ArrayList<Assign>();
		while (iterator.hasNext()) {
			Assign assign = (Assign) iterator.next();
			this.assigns.add(new Assign(assign));
		}
	}

	/**
	 * Constructs a StructureAssign object from the specified structure ans
	 * assignments. Both the list of assignments and the additional assignment
	 * are duplicated.
	 * 
	 * @param structure
	 *            A structure.
	 * @param oldAssigns
	 *            A list of assignments.
	 * @param newAssign
	 *            An optional additional assignment. This argument may be null.
	 */
	public StructureAssign(TVS structure, List oldAssigns, Assign newAssign) {
		this.structure = structure;
		this.assigns = new ArrayList<Assign>();
		for (Iterator i = oldAssigns.iterator(); i.hasNext();) {
			Assign assign = (Assign) i.next();
			this.assigns.add(new Assign(assign));
		}
		if (newAssign != null)
			this.assigns.add(new Assign(newAssign));
	}

	/**
	 * Projects the assignments associated with the structure to the set of
	 * relevant variables.
	 * 
	 * @param variables
	 *            The variables used to project each assignment.
	 */
	public void project(Set<Var> variables) {
		Set<Assign> newAssigns = HashSetFactory.make(assigns.size());

		for (Iterator<Assign> i = assigns.iterator(); i.hasNext();) {
			Assign assign = i.next();
			assign.project(variables);
			newAssigns.add(assign);
		}
		assigns = new ArrayList<Assign>(newAssigns);
	}

	/**
	 * Returns a human-readable form of the structure-assignments.
	 */
	public String toString() {
		return StructureToTVS.defaultInstance.convert(structure,
				assignsToString());
	}

	/**
	 * Returns a human-readable form of the assignments for debugging purposes.
	 */
	public String assignsToString() {
		StringBuffer result = new StringBuffer();
		for (Iterator<Assign> it = assigns.iterator(); it.hasNext();) {
			Assign assign = it.next();
			result.append(assign.toString() + "\n");
		}
		return result.toString();
	}
}

/**
 * A class that solves computes Focus for a single literal.
 * 
 * @author Tal Lev-Ami
 */
class FocusStep {
	/**
	 * Variables needed in future steps.
	 */
	public Set<Var> futureVariables = HashSetFactory.make(0);

	/**
	 * Variables relevant in this step.
	 */
	public Set<Var> nowAndFutureVariables = HashSetFactory.make(0);

	/**
	 * Variables introduced in this step.
	 */
	public Set<Var> newVariables = HashSetFactory.make(0);

	/**
	 * Variables used in this step and were bound before.
	 */
	public Set<Var> boundVariables = HashSetFactory.make(0);

	/**
	 * The literal to focus on.
	 */
	public Literal literal;

	/**
	 * Indicates whether a warning message has been emitted for a cases which
	 * can cause an infinite numebr of structures to arise.
	 */
	protected static boolean focusWarningEmitted = false;

	/**
	 * Constructs and initializes a focus step.
	 * 
	 * @param literal
	 *            The literal that the step concentrates on focusing.
	 */
	public FocusStep(Literal literal) {
		this.literal = literal;
	}

	public String toString() {
		return "literal=" + literal + ", future=" + futureVariables
				+ ", now and future=" + nowAndFutureVariables;
	}

	/**
	 * Performs the specific type of focus according to the arity and functional
	 * properties of the formula.
	 * 
	 * @author Tal Lev-Ami
	 */
	public Collection<TVS> focus(TVS structure, Assign assign) {
		PredicateFormula predicateFormula = (PredicateFormula) literal.atomic;
		switch (predicateFormula.predicate().arity()) {
		case 0:
			return focusNullary(structure, predicateFormula);

		case 1:
			if (predicateFormula.predicate().unique())
				return focusUnaryUnique(structure, predicateFormula, assign);
			else
				return focusUnaryNonUnique(structure, predicateFormula, assign);

		case 2:
			if (assign.isEmpty()) // a special case for unbound binary predicate
									// formulae
				return focusBinaryUnbound(structure, predicateFormula);
			else if (predicateFormula.predicate().function()
					&& assign.contains(predicateFormula.getVariable(0)))
				return focusBinaryFunction(structure, predicateFormula, assign);
			else if (predicateFormula.predicate().invfunction()
					&& assign.contains(predicateFormula.getVariable(1)))
				return focusBinaryInvFunction(structure, predicateFormula,
						assign);
			else
				return focusBinaryNonFunction(structure, predicateFormula,
						assign);

		default:
			return focusKaryNonFunction(structure, predicateFormula, assign);
		}
	}

	public Collection<StructureAssign> generateAssign(Collection structures,
			Assign relevant, List<Assign> previousAssigns) {
		Collection<StructureAssign> answer = new ArrayList<StructureAssign>();

		// The variables whose binding was added at this stage and
		// are needed in future steps.
		Collection<Var> newAndNeeded = HashSetFactory.make(newVariables);
		newAndNeeded.retainAll(futureVariables);

		if (newAndNeeded.isEmpty()) {
			for (Iterator structureIt = structures.iterator(); structureIt
					.hasNext();) {
				TVS structure = (TVS) structureIt.next();
				// No new information. Just project the old assignments
				// to the variables needed in the future.
				// Also check that the current literal evaluates to true...
				Iterator values = structure.evalFormulaForValue(literal.atomic,
						relevant, literal.negated ? Kleene.falseKleene
								: Kleene.trueKleene);
				if (!values.hasNext()) {
					// Structure doesn't satisfy the assignment.
					previousAssigns = new ArrayList<Assign>();
				}

				StructureAssign structureAssign = new StructureAssign(
						structure, previousAssigns);
				structureAssign.project(futureVariables);
				answer.add(structureAssign);
			}
			return answer;
		}

		// Now generate the new full assignments.
		for (Iterator structureIt = structures.iterator(); structureIt
				.hasNext();) {
			TVS structure = (TVS) structureIt.next();

			Iterator afterStep = structure.evalFormulaForValue(literal.atomic,
					relevant, literal.negated ? Kleene.falseKleene
							: Kleene.trueKleene);

			List<Assign> newAssigns = new ArrayList<Assign>();
			while (afterStep.hasNext()) {
				Assign newAssign = (Assign) afterStep.next();
				for (Iterator<Assign> prevIt = previousAssigns.iterator(); prevIt
						.hasNext();) {
					Assign prevAssign = prevIt.next();
					Assign combined = new Assign(newAssign);
					combined.put(prevAssign);
					newAssigns.add(combined);
				}
			}
			answer.add(new StructureAssign(structure, newAssigns));
		}
		return answer;
	}

	public Collection<StructureAssign> focus(StructureAssign current) {
		// Project all assignments into the variables needed for the
		// current and future steps.
		current.project(nowAndFutureVariables);

		List<StructureAssign> answer = new ArrayList<StructureAssign>();

		// If no assignments satisfy the preceding formula, we can stop now
		// and just propagate the structure.
		if (current.assigns.isEmpty()) {
			answer.add(new StructureAssign(current.structure,
					new ArrayList<Assign>()));
			return answer;
		}

		Map<Assign, Collection<TVS>> relevantAssignToStructures = HashMapFactory
				.make();
		Map<Assign, List<Assign>> relevantAssignToFullAssign = HashMapFactory
				.make();

		for (Iterator<Assign> assignIt = current.assigns.iterator(); assignIt
				.hasNext();) {
			Assign assign = assignIt.next();
			Assign relevant = new Assign(assign);

			relevant.project(boundVariables);
			List<Assign> fullAssigns = relevantAssignToFullAssign.get(relevant);
			if (fullAssigns == null) { // Unknown relevant assignment!
				// Focus and retain structures.

				// First focus on active for new variables that might satisfy
				// the
				// formula.
				Collection<TVS> activeFocused = focusActive(current.structure,
						relevant);
				// Now focus on the formula itself.
				Collection<TVS> stepFocused = new ArrayList<TVS>();
				for (TVS focused : activeFocused) {
					stepFocused.addAll(focus(focused, relevant));
				}
				relevantAssignToStructures.put(relevant, stepFocused);

				// Initialize the containing assignments list.
				fullAssigns = new ArrayList<Assign>();
				relevantAssignToFullAssign.put(relevant, fullAssigns);
			}
			// Add the current assignment to the appropriate partial assignment.
			fullAssigns.add(assign);
		}

		// Now everything was focused and we only need to
		// rebuild the StructureAssigns for the next round.
		for (Map.Entry<Assign, List<Assign>> entry : relevantAssignToFullAssign
				.entrySet()) {
			Assign relevant = entry.getKey();
			List<Assign> fullAssigns = entry.getValue();
			Collection structures = relevantAssignToStructures.get(relevant);
			answer.addAll(generateAssign(structures, relevant, fullAssigns));
		}
		return answer;
	}

	/**
	 * Creates all the structures that have the specified nodes as either active
	 * or non-active (all the binary combinations of active/non-active for these
	 * nodes). There are 2^(|toFocus|) such structures.
	 * 
	 * @param toFocus
	 *            A collection of maybe-active nodes. postcondition: foreach s
	 *            in return foreach n in toFocus s.eval(Vocabulary.active, n) !=
	 *            Kleene.unknownKleene
	 * @author Tal Lev-Ami
	 */
	private Collection<TVS> focusActive(TVS structure, Collection<Node> toFocus) {
		if (!Focus.needToFocusOnActive) {
			return Collections.singleton(structure);
		}

		List<TVS> answer = new ArrayList<TVS>();
		answer.add(structure); // start with a work set containing only this
								// structure
		for (Iterator<Node> nodeIt = toFocus.iterator(); nodeIt.hasNext();) {
			Node problemNode = nodeIt.next();

			List<TVS> workSet = new ArrayList<TVS>();
			for (TVS currentStructure : answer) {
				if (inScope(currentStructure, problemNode)
						&& currentStructure
								.eval(Vocabulary.active, problemNode) == Kleene.unknownKleene) {
					// Add new structure where the node does not exist.
					TVS without = currentStructure.copy();
					without.removeNode(problemNode);
					ModifiedPredicates.modify(without, Vocabulary.active);
					workSet.add(without);

					// Add new structure where the node is active.
					TVS with = currentStructure.copy();
					with.update(Vocabulary.active, problemNode,
							Kleene.trueKleene);
					ModifiedPredicates.modify(with, Vocabulary.active);
					workSet.add(with);
				} else {
					workSet.add(currentStructure);
				}
			}
			answer = workSet;
		}
		ModifiedPredicates.modify(Vocabulary.active);

		return answer;
	}

	/**
	 * @param toFocus
	 *            A collection of maybe-active nodes.
	 * @author Tal Lev-Ami
	 */
	private Collection<TVS> focusActiveUnique(TVS structure,
			Collection<Node> toFocus) {
		List<TVS> answer = new ArrayList<TVS>();

		// A new structure where eventually none of these nodes exist.
		TVS without = structure.copy();

		for (Iterator<Node> nodeIt = toFocus.iterator(); nodeIt.hasNext();) {
			Node problemNode = nodeIt.next();
			if (!inScope(structure, problemNode))
				continue;

			// Add new structure where this node is the only active node.
			TVS with = structure.copy();
			with.update(Vocabulary.active, problemNode, Kleene.trueKleene);
			for (Iterator<Node> otherIt = toFocus.iterator(); otherIt.hasNext();) {
				Node otherNode = otherIt.next();
				if (otherNode.equals(problemNode))
					continue;
				with.removeNode(otherNode);
			}
			ModifiedPredicates.modify(with, Vocabulary.active);
			answer.add(with);
			without.removeNode(problemNode);
		}
		answer.add(without);
		ModifiedPredicates.modify(without, Vocabulary.active);

		return answer;
	}

	private Collection<TVS> focusActive(TVS structure, Assign assign) {
		if (!Focus.needToFocusOnActive) {
			return Collections.singleton(structure);
		}

		Formula formula = literal.atomic;
		if (literal.negated)
			formula = new NotFormula(formula);

		Collection<Node> toFocus = HashSetFactory.make();
		Iterator problemIt = structure.evalFormula(formula, assign);
		while (problemIt.hasNext()) {
			Assign problemAssign = (Assign) problemIt.next();
			for (Iterator<Var> varIt = newVariables.iterator(); varIt.hasNext();) {
				Var newVar = varIt.next();
				Node problemNode = problemAssign.get(newVar);
				if (!inScope(structure, problemNode))
					continue;
				if (structure.eval(Vocabulary.active, problemNode).equals(
						Kleene.unknownKleene))
					toFocus.add(problemNode);
			}
		}

		PredicateFormula predicateFormula = (PredicateFormula) literal.atomic;
		if (predicateFormula.predicate().arity() == 1
				&& predicateFormula.predicate().unique()) {
			return focusActiveUnique(structure, toFocus);
		}
		return focusActive(structure, toFocus);
	}

	private Collection<TVS> focusNullary(TVS structure, PredicateFormula formula) {
		List<TVS> answer = new ArrayList<TVS>();
		// Attempt to solve TC cache bug by calling prepare on eval
		formula.prepare(structure);
		if (formula.eval(structure, Assign.EMPTY) == Kleene.unknownKleene) {
			TVS trueStructure = structure.copy();
			trueStructure.update(formula.predicate(), Kleene.trueKleene);
			TVS falseStructure = structure.copy();
			falseStructure.update(formula.predicate(), Kleene.falseKleene);
			answer.add(trueStructure);
			answer.add(falseStructure);

			ModifiedPredicates.modify(trueStructure, formula.predicate());
			ModifiedPredicates.modify(falseStructure, formula.predicate());
		} else {
			answer.add(structure);
		}
		return answer;
	}

	private Collection<TVS> focusUnaryUnique(TVS structure,
			PredicateFormula formula, Assign assign) {
		List<TVS> answer = new ArrayList<TVS>();

		Iterator<AssignKleene> problemIt = structure.evalFormulaForValue(
				formula, assign, Kleene.unknownKleene);

		if (!problemIt.hasNext()) {
			answer.add(structure);
			return answer;
		}

		Iterator<AssignKleene> hasTrue = structure.evalFormulaForValue(formula,
				assign, Kleene.trueKleene);
		if (hasTrue.hasNext()) {
			TVS without = structure.copy();
			while (problemIt.hasNext()) {
				Node problemNode = problemIt.next().get(formula.getVariable(0));
				without.update(formula.predicate(), problemNode,
						Kleene.falseKleene);
			}
			answer.add(without);
			return answer;
		}

		// Add structure where x doesn't point to anything.
		TVS without = structure.copy();
		ModifiedPredicates.modify(without, formula.predicate());
		without.clearPredicate(formula.predicate());
		TVS withoutSafe = structure.copy();
		ModifiedPredicates.modify(withoutSafe, formula.predicate());
		safeClearPredicate(withoutSafe, formula.predicate());
		answer.add(withoutSafe);
		while (problemIt.hasNext()) {
			Assign problemAssign = (Assign) problemIt.next();
			Node problemNode = problemAssign.get(formula.getVariable(0));
			if (!inScope(structure, problemNode))
				continue;

			// Add new structure where x points only to problemNode.
			TVS withEdge = without.copy();
			withEdge.update(formula.predicate(), problemNode, Kleene.trueKleene);
			// This is a unique predicate so the true node
			// is not a summary node.
			withEdge.update(Vocabulary.sm, problemNode, Kleene.falseKleene);
			ModifiedPredicates.modify(withEdge, formula.predicate());
			ModifiedPredicates.modify(withEdge, Vocabulary.sm);
			answer.add(withEdge);

			if (without.eval(Vocabulary.sm, problemNode).equals(
					Kleene.unknownKleene)) {
				// Add new structure where problemNode is bifurcated into two
				// nodes.
				TVS structureWithDuplicatedNode = without.copy();

				Node newNodeTrue = structureWithDuplicatedNode
						.duplicateNode(problemNode);
				structureWithDuplicatedNode.update(formula.predicate(),
						newNodeTrue, Kleene.trueKleene);
				structureWithDuplicatedNode.update(formula.predicate(),
						problemNode, Kleene.falseKleene);

				// This is a unique predicate so the true node is not a summary
				// node.
				structureWithDuplicatedNode.update(Vocabulary.sm, newNodeTrue,
						Kleene.falseKleene);
				ModifiedPredicates.modify(structureWithDuplicatedNode,
						Vocabulary.sm);
				ModifiedPredicates.modify(structureWithDuplicatedNode,
						formula.predicate());

				answer.add(structureWithDuplicatedNode);
			}
		}
		// ModifiedPredicates.modify(formula.predicate());
		// ModifiedPredicates.modify(Vocabulary.sm);
		return answer;
	}

	/**
	 * Performs focus for a unary predicate which is no unique.
	 */
	private Collection<TVS> focusUnaryNonUnique(TVS structure,
			PredicateFormula formula, Assign assign) {
		List<TVS> workSet = new ArrayList<TVS>();
		List<TVS> answer = new ArrayList<TVS>();
		workSet.add(structure);

		while (!workSet.isEmpty()) {
			TVS currentStructure = workSet.remove(0);

			Iterator problemIt = currentStructure.evalFormulaForValue(
					ensureInScope(formula), assign, Kleene.unknownKleene);
			if (!problemIt.hasNext()) {
				answer.add(currentStructure);
				continue;
			}
			Assign problemAssign = (Assign) problemIt.next();
			Node problemNode = problemAssign.get(formula.getVariable(0));

			// Case 1: p evaluates to false on all of the concrete nodes
			// represented by problemNode.
			// Add a new structure where p evaluates to false on problemNode.
			TVS withoutEdge = currentStructure.copy();
			withoutEdge.update(formula.predicate(), problemNode,
					Kleene.falseKleene);
			ModifiedPredicates.modify(withoutEdge, formula.predicate());
			workSet.add(withoutEdge);

			// Case 2: p evaluates to true on all of the concrete nodes
			// represented by problemNode.
			// Add a new structure where p evaluates to true on problemNode.
			TVS withEdge = currentStructure.copy();
			withEdge.update(formula.predicate(), problemNode, Kleene.trueKleene);
			ModifiedPredicates.modify(withEdge, formula.predicate());

			workSet.add(withEdge);

			// Case 3: p evaluates to true on some of the concrete nodes
			// represented by problemNode
			// and to false on the others.
			// If the node is a summary node, bifurcate it into two summary
			// nodes
			// such that p evalutes to true on one node and false on the other.
			if (currentStructure.eval(Vocabulary.sm, problemNode) == Kleene.unknownKleene) {
				// Add new structure where problemNode is bifurcated into two
				// nodes.
				TVS structureWithDuplicatedNode = currentStructure.copy();
				// Duplicate the problem node - p will evaluate to true on the
				// cloned node.
				Node newNodeTrue = structureWithDuplicatedNode
						.duplicateNode(problemNode);
				structureWithDuplicatedNode.update(formula.predicate(),
						newNodeTrue, Kleene.trueKleene);
				structureWithDuplicatedNode.update(formula.predicate(),
						problemNode, Kleene.falseKleene);
				ModifiedPredicates.modify(structureWithDuplicatedNode,
						formula.predicate());

				workSet.add(structureWithDuplicatedNode);
			}
		}
		ModifiedPredicates.modify(formula.predicate());
		return answer;
	}

	/**
	 * Adds the set of structures formed by focusing the specified structure on
	 * the binary predicate and two nodes to the answer set.
	 * 
	 * @author Tal Lev-Ami
	 */
	private void focusBinary(Collection<TVS> answer, TVS structure,
			Predicate predicate, Node leftProblem, Node rightProblem) {
		boolean leftSummary = structure.eval(Vocabulary.sm, leftProblem)
				.equals(Kleene.unknownKleene);
		boolean rightSummary = structure.eval(Vocabulary.sm, rightProblem)
				.equals(Kleene.unknownKleene);

		// Dectect infinite Focus conditions.
		if (leftSummary && rightSummary) {
			// This is a real problem - focus will cause an infinite number of
			// structure!
			// Don't give-up on the whole analysis, just return a less precise
			// anwer.
			if (focusWarningEmitted)
				return;
			String message = "\nError! Focusing on predicate " + predicate
					+ " will cause an infinite number of structures "
					+ "on structure:\n"
					+ StructureToTVS.defaultInstance.convert(structure, "")
					+ "\non the edge from " + leftProblem.name() + " to "
					+ rightProblem.name();
			if (AnalysisStatus.emitWarnings)
				throw new FocusNonTerminationException(message);
			else
				message = message + "\nFocus ignored this edge!";
			Logger.println(message);
			focusWarningEmitted = true;
			return;
		}

		// Add new structure where n(leftProblem, rightProblem) = 1
		TVS withEdge = structure.copy();
		withEdge.update(predicate, leftProblem, rightProblem, Kleene.trueKleene);
		ModifiedPredicates.modify(withEdge, predicate);

		// If the predicate is function the right node must be non summary.
		// (since the left node can't be a summary node).
		if (predicate.function() && rightSummary) {
			withEdge.update(Vocabulary.sm, rightProblem, Kleene.falseKleene);
			ModifiedPredicates.modify(withEdge, Vocabulary.sm);
		}
		// If the predicate is invfunction the left node must be non summary.
		// (since the right node can't be a summary node).
		if (predicate.invfunction() && leftSummary) {
			withEdge.update(Vocabulary.sm, leftProblem, Kleene.falseKleene);
			ModifiedPredicates.modify(withEdge, Vocabulary.sm);
		}
		answer.add(withEdge);

		if (rightSummary) {
			// Add new structure where rightProblem is bifurcated into two
			// nodes.
			TVS structureWithDuplicatedNode = structure.copy();
			Node rightProblemTrue = structureWithDuplicatedNode
					.duplicateNode(rightProblem);
			structureWithDuplicatedNode.update(predicate, leftProblem,
					rightProblemTrue, Kleene.trueKleene);
			structureWithDuplicatedNode.update(predicate, leftProblem,
					rightProblem, Kleene.falseKleene);
			ModifiedPredicates.modify(structureWithDuplicatedNode, predicate);
			// If the predicate is function the right node must be non summary.
			// (since the left node is a summary node).
			if (predicate.function()) {
				structureWithDuplicatedNode.update(Vocabulary.sm,
						rightProblemTrue, Kleene.falseKleene);
				ModifiedPredicates.modify(structureWithDuplicatedNode,
						Vocabulary.sm);
			}
			answer.add(structureWithDuplicatedNode);
		}
		if (leftSummary) {
			// Add new structure where leftProblem is bifurcated into two nodes.
			TVS structureWithDuplicatedNode = structure.copy();
			Node leftProblemTrue = structureWithDuplicatedNode
					.duplicateNode(leftProblem);
			structureWithDuplicatedNode.update(predicate, leftProblemTrue,
					rightProblem, Kleene.trueKleene);
			structureWithDuplicatedNode.update(predicate, leftProblem,
					rightProblem, Kleene.falseKleene);
			ModifiedPredicates.modify(structureWithDuplicatedNode, predicate);

			// If the predicate is invfunction the leftt node must be non
			// summary.
			// (since the right node is a summary node).
			if (predicate.invfunction()) {
				structureWithDuplicatedNode.update(Vocabulary.sm,
						leftProblemTrue, Kleene.falseKleene);
				ModifiedPredicates.modify(Vocabulary.sm);
			}
			answer.add(structureWithDuplicatedNode);
		}
		ModifiedPredicates.modify(predicate);
	}

	/**
	 * Adds the set of structures formed by focusing the specified structure on
	 * the binary predicate and two nodes to the answer set.
	 * 
	 * @author Roman Manevich
	 */
	private void focusKary(Collection<TVS> answer, TVS structure,
			Predicate predicate, NodeTuple problems) {
		int numberOfSummaries = 0;
		for (int index = 0; index < problems.size(); ++index) {
			Node node = problems.get(index);
			if (structure.eval(Vocabulary.sm, node) == Kleene.unknownKleene)
				++numberOfSummaries;
		}
		if (numberOfSummaries > 1) { // Dectect infinite Focus conditions.
			// This is a real problem - focus will cause an infinite number of
			// structure!
			// Don't give-up on the whole analysis, just return a less precise
			// anwer.
			if (focusWarningEmitted)
				return;
			String message = "\nError! Focusing on predicate " + predicate
					+ " will cause an infinite number of structures "
					+ "on structure:\n"
					+ StructureToTVS.defaultInstance.convert(structure, "")
					+ "\non the tuple " + problems;
			if (AnalysisStatus.emitWarnings)
				throw new FocusNonTerminationException(message);
			else
				message = message + "\nFocus ignored this tuple!";
			Logger.println(message);
			focusWarningEmitted = true;
			return;
		}

		// Add new structure where p(tuple) = 1
		TVS withTuple = structure.copy();
		withTuple.update(predicate, problems, Kleene.trueKleene);
		ModifiedPredicates.modify(withTuple, predicate);
		// ModifiedPredicates.modify(predicate);

		// If the predicate is function the true node must be non summary.
		// (since the left node can't be a summary node).
		// if (predicate.function())
		// withTuple.update(Vocabulary.sm, rightProblem, Kleene.falseKleene);
		answer.add(withTuple);

		// handle summary nodes
		for (int index = 0; index < problems.size(); ++index) {
			Node node = problems.get(index);
			if (structure.eval(Vocabulary.sm, node) != Kleene.unknownKleene)
				continue;
			// Add new structure where rightProblem is bifurcated into two
			// nodes.
			TVS structureWithDuplicatedTuple = structure.copy();
			Node problemTrue = structureWithDuplicatedTuple.duplicateNode(node);
			structureWithDuplicatedTuple.update(predicate,
					problems.substitute(node, problemTrue), Kleene.trueKleene);
			structureWithDuplicatedTuple.update(predicate, problems,
					Kleene.falseKleene);
			ModifiedPredicates.modify(structureWithDuplicatedTuple, predicate);

			// If the predicate is function the true node must be non summary.
			// (since the left node can't be a summary node).
			// if (predicate.function())
			// structureWithDuplicatedTuple.update(Vocabulary.sm, node,
			// Kleene.falseKleene);
			answer.add(structureWithDuplicatedTuple);
		}
	}

	/**
	 * Returns a set of structures formed by focusing the specified structure on
	 * the binary predicate with all indefinite edges. If the structure contains
	 * a summary node with an unknown edge (of the specified predicate) then at
	 * most a single node bifurcation may occur to insure termination.
	 */
	private Collection<TVS> focusBinaryUnbound(TVS structure,
			PredicateFormula bformula) {
		// Roman, November 17,2001: this method was added to solve a bug that
		// caused Focus not to terminate in this case.

		boolean function = bformula.predicate().function();
		boolean invfunction = bformula.predicate().invfunction();
		Iterator problemIt = structure.evalFormulaForValue(
				ensureInScope(bformula), Assign.EMPTY, Kleene.unknownKleene);
		if (!problemIt.hasNext())
			return Collections.singleton(structure);

		Set<TVS> answer = HashSetFactory.make();
		while (problemIt.hasNext()) {
			Assign problemAssign = (Assign) problemIt.next();
			if (function) {
				answer.addAll(focusBinaryFunction(structure, bformula,
						problemAssign));
			} else if (invfunction) {
				answer.addAll(focusBinaryInvFunction(structure, bformula,
						problemAssign));
			} else {
				answer.addAll(focusBinaryNonFunction(structure, bformula,
						problemAssign));
			}
		}
		return answer;
	}

	/**
	 * Returns a set of structures formed by focusing the specified structure on
	 * the binary function predicate with a partial assignment.
	 * 
	 * @author Tal Lev-Ami
	 * @param assign
	 *            A partial assignment.
	 */
	private Collection<TVS> focusBinaryFunction(TVS structure,
			PredicateFormula formula, Assign assign) {

		// Roman, May 9, 2001 : Added special treatment for definite edges
		// pointing to
		// maybe-active nodes to solve a bug from TVLA version 0.9.
		//
		// Roman, January 9, 2004: Fixed a bug from TVLA version 0.9, which
		// resulted
		// in removal of too many edges in cases where the indefinite edge is a
		// self-loop
		// (only the self-loop edge was refined and the rest were removed).

		Formula scopedFormula = ensureInScope(formula);
		List<TVS> answer = new ArrayList<TVS>();
		Iterator problemIt = structure.evalFormulaForValue(scopedFormula,
				assign, Kleene.unknownKleene);
		if (!problemIt.hasNext()) {
			answer.add(structure);
			return answer;
		}

		Predicate predicate = formula.predicate();

		// This is supposed to be bound.
		Node leftProblem = assign.get(formula.getVariable(0));
		if (leftProblem == null) {
			throw new RuntimeException(
					"Left node is supposed to be bound when "
							+ "focusing on predicate " + formula
							+ " on structure " + structure);
		}
		{
			// Special case in which both sides are bound
			Node rightProblem = assign.get(formula.getVariable(1));
			if (rightProblem != null) {
				assert structure.eval(Vocabulary.sm, leftProblem) == Kleene.falseKleene;
				// For without - simply remove the edge and return - we know
				// nothing of the other edges
				TVS without = structure.copy();
				without.update(predicate, leftProblem, rightProblem,
						Kleene.falseKleene);
				ModifiedPredicates.modify(without, predicate);
				answer.add(without);
				// For with and bifurcation - remove all other edges
				TVS other = structure.copy();
				Node[] partial = new Node[] { leftProblem, null };
				Iterator<Entry<NodeTuple, Kleene>> iterator = structure
						.predicateSatisfyingNodeTuples(predicate, partial,
								Kleene.unknownKleene);
				PredicateUpdater updater = PredicateUpdater.updater(predicate,
						other);
				while (iterator.hasNext()) {
					NodeTuple tuple = iterator.next().getKey();
					updater.update(tuple, Kleene.falseKleene);
				}

				focusBinary(answer, other, predicate, leftProblem, rightProblem);
				return answer;
			}
		}

		boolean hasDefiniteEdge = false;
		// Add a structure with no outgoing 1/2 edges.
		TVS withoutSafe = structure.copy();
		TVS without = structure.copy();
		for (Node node : structure.nodes()) {
			// Remove the outgoing 1/2 edge in the without graph.
			Kleene edgeVal = without.eval(predicate, leftProblem, node);
			if (edgeVal == Kleene.unknownKleene) {
				without.update(predicate, leftProblem, node, Kleene.falseKleene);
				ModifiedPredicates.modify(without, formula.predicate());
				if (inScope(structure, node)) {
					withoutSafe.update(predicate, leftProblem, node,
							Kleene.falseKleene);
					ModifiedPredicates.modify(withoutSafe, formula.predicate());
				}
			} else if (edgeVal == Kleene.trueKleene) {
				// A special case where the individual on the other end of the
				// predicate is a maybe-active individual.
				// In such a case we focus by removing all other edges.
				assert without.eval(Vocabulary.active, node) == Kleene.unknownKleene : "Attempt to focus on definite edge where individual on other end "
						+ "of edge is definitely active (expected to be maybe-active)!";
				hasDefiniteEdge = true;
			}
		}
		answer.add(withoutSafe);

		if (!hasDefiniteEdge) {
			// Refine all the previously removed 1/2 edges.
			for (Node rightProblem : structure.nodes()) {
				if (structure.eval(predicate, leftProblem, rightProblem) == Kleene.unknownKleene
						&& inScope(structure, rightProblem)) {
					focusBinary(answer, without, predicate, leftProblem,
							rightProblem);
				}
			}
		}
		return answer;
	}

	/**
	 * Returns a set of structures formed by focusing the specified structure on
	 * the binary inverse-function predicate with a partial assignment.
	 * 
	 * @param assign
	 *            A partial assignment.
	 * @author Roman Manevich
	 * @since tvla-2-alpha (June 28 2002) added support for invfunction.
	 */
	private Collection<TVS> focusBinaryInvFunction(TVS structure,
			PredicateFormula formula, Assign assign) {
		Formula scopedFormula = ensureInScope(formula);
		Collection<TVS> answer = HashSetFactory.make();

		Iterator problemIt = structure.evalFormulaForValue(scopedFormula,
				assign, Kleene.unknownKleene);

		if (!problemIt.hasNext()) {
			answer.add(structure);
			return answer;
		}

		// This is supposed to be bound.
		Node rightProblem = assign.get(formula.getVariable(1));
		if (rightProblem == null) {
			throw new RuntimeException(
					"Right node is supposed to be bound when "
							+ "focusing on predicate " + formula
							+ " on structure " + structure);
		}

		boolean hasDefiniteEdge = false;
		// Add a structure with no incoming edges.
		TVS without = structure.copy();
		for (Iterator nodeIt = structure.nodes().iterator(); nodeIt.hasNext();) {
			Node node = (Node) nodeIt.next();
			// Remove the incoming edge of the without graph.
			Kleene edgeVal = without.eval(formula.predicate(), node,
					rightProblem);
			if (edgeVal == Kleene.unknownKleene) {
				without.update(formula.predicate(), node, rightProblem,
						Kleene.falseKleene);
				ModifiedPredicates.modify(without, formula.predicate());
				// ModifiedPredicates.modify(formula.predicate());
			} else if (edgeVal == Kleene.trueKleene) {
				hasDefiniteEdge = true;
			}
		}
		answer.add(without);

		if (!hasDefiniteEdge) {
			while (problemIt.hasNext()) {
				Assign problemAssign = (Assign) problemIt.next();
				Node leftProblem = problemAssign.get(formula.getVariable(0));
				focusBinary(answer, without, formula.predicate(), leftProblem,
						rightProblem);
			}
		}
		return answer;
	}

	private Collection<TVS> focusBinaryNonFunction(TVS structure,
			PredicateFormula formula, Assign assign) {
		Formula scopedFormula = ensureInScope(formula);
		List<TVS> workSet = new LinkedList<TVS>();
		List<TVS> answer = new ArrayList<TVS>();
		workSet.add(structure);
		while (!workSet.isEmpty()) {
			TVS currentStructure = workSet.remove(0);

			Iterator problemIt = currentStructure.evalFormulaForValue(
					scopedFormula, assign, Kleene.unknownKleene);
			if (!problemIt.hasNext()) {
				answer.add(currentStructure);
				continue;
			}

			Assign problemAssign = (Assign) problemIt.next();
			Node leftProblem = problemAssign.get(formula.getVariable(0));
			Node rightProblem = problemAssign.get(formula.getVariable(1));

			if (!(formula.predicate().reflexive() && leftProblem
					.equals(rightProblem))) {
				// Add new structure where n(leftProblem, rightProblem) = 0
				TVS withoutEdge = currentStructure.copy();
				withoutEdge.update(formula.predicate(), leftProblem,
						rightProblem, Kleene.falseKleene);
				ModifiedPredicates.modify(withoutEdge, formula.predicate());

				workSet.add(withoutEdge);
			}
			focusBinary(workSet, currentStructure, formula.predicate(),
					leftProblem, rightProblem);
		}
		ModifiedPredicates.modify(formula.predicate());
		return answer;
	}

	private Collection<TVS> focusKaryNonFunction(TVS structure,
			PredicateFormula formula, Assign assign) {
		Formula scopedFormula = ensureInScope(formula);
		List<TVS> workSet = new LinkedList<TVS>();
		List<TVS> answer = new ArrayList<TVS>();
		workSet.add(structure);
		while (!workSet.isEmpty()) {
			TVS currentStructure = workSet.remove(0);

			Iterator problemIt = currentStructure.evalFormulaForValue(
					scopedFormula, assign, Kleene.unknownKleene);
			if (!problemIt.hasNext()) {
				answer.add(currentStructure);
				continue;
			}

			Assign problemAssign = (Assign) problemIt.next();
			Node[] nodesTmp = new Node[formula.variables().length];
			for (int index = 0; index < nodesTmp.length; ++index) {
				nodesTmp[index] = problemAssign.get(formula.getVariable(index));
			}
			NodeTuple tuple = NodeTuple.createTuple(nodesTmp);

			// Add new structure where n(leftProblem, rightProblem) = 0
			TVS withoutTuple = currentStructure.copy();
			withoutTuple.update(formula.predicate(), tuple, Kleene.falseKleene);
			ModifiedPredicates.modify(withoutTuple, formula.predicate());
			workSet.add(withoutTuple);
			focusKary(workSet, currentStructure, formula.predicate(), tuple);
		}
		ModifiedPredicates.modify(formula.predicate());
		return answer;
	}

	private Formula ensureInScope(Formula formula) {
		for (Var var : formula.freeVars()) {
			formula = new AndFormula(new NotFormula(new PredicateFormula(
					Vocabulary.outside, var)), formula);
		}
		return formula;
	}

	protected boolean inScope(TVS structure, Node node) {
		return structure.eval(Vocabulary.outside, node) == Kleene.falseKleene;
	}

	/**
	 * Safely clear the predicate without touching outside nodes.
	 */
	protected void safeClearPredicate(TVS structure, Predicate predicate) {
		Iterator<Entry<NodeTuple, Kleene>> outsideNodes = structure
				.predicateSatisfyingNodeTuples(Vocabulary.outside, new Node[0],
						Kleene.trueKleene);
		if (outsideNodes.hasNext()) {
			Var v = new Var("v");
			Iterator<AssignKleene> insideNodes = structure
					.evalFormulaForValue(new NotFormula(new PredicateFormula(
							Vocabulary.outside, v)), Assign.EMPTY,
							Kleene.trueKleene);
			while (insideNodes.hasNext()) {
				structure.update(predicate, insideNodes.next().get(v),
						Kleene.falseKleene);
			}
		} else {
			structure.clearPredicate(predicate);
		}
	}

	// /** @author Roman Manevich
	// * @since tvla-2-alpha (17 May 2002) Initial creation.
	// */
	// private Collection<TVS> focusKAryPredicate(TVS structure,
	// PredicateFormula formula,
	// Assign assign) {
	// throw new
	// UnsupportedOperationException("Focus does not yet support arbitrary arity predicates!");
	// }
}

/**
 * This class represents an optimized form of a formulae, especially suitable
 * for use in the Focus algorithm.
 * 
 * @author Tal Lev-Ami
 */
class FocusFormula {
	/**
	 * An array of the disjunctions of the formula.
	 */
	public List<FocusStep>[] focusFormulae;

	/**
	 * The set of quantifying variables in the unoptimized form of the formula.
	 */
	private Set<Var> quantVariables = HashSetFactory.make();

	private final Collection<Predicate> formulaPredicates;

	/**
	 * A list of unbound predicates found in a conjunction while computing the
	 * optimal order of literals. The list is used for emitting an informative
	 * error message.
	 */
	private static List<FocusStep> unboundBinaryPredicates = null;

	/**
	 * Constructs a new formula by optimizing the specified formula.
	 * 
	 * @param formula
	 *            The formula to optimize.
	 */
	public FocusFormula(Formula formula) throws FocusNonTerminationException {
		formulaPredicates = GetFormulaPredicates.get(formula);

		// Get the prenex DNF normal form.
		Formula prenexDNF = Formula.toPrenexDNF(formula);

		// Remove all quantifiers.
		while (true) {
			if (prenexDNF instanceof ExistQuantFormula) {
				ExistQuantFormula eformula = (ExistQuantFormula) prenexDNF;
				quantVariables.add(eformula.boundVariable());
				prenexDNF = eformula.subFormula();
			} else if (prenexDNF instanceof AllQuantFormula) {
				AllQuantFormula aformula = (AllQuantFormula) prenexDNF;
				quantVariables.add(aformula.boundVariable());
				prenexDNF = aformula.subFormula();
			} else {
				break;
			}
		}

		// For each disjunction put its literals in a list and insert it to the
		// focusFormulae list.
		List<Formula> orTerms = new ArrayList<Formula>();
		Formula.getOrs(prenexDNF, orTerms);
		focusFormulae = new List[orTerms.size()];
		boolean encounteredConjunctionWithUnbounded = false;
		for (int i = 0; i < orTerms.size(); i++) {
			Formula orTerm = orTerms.get(i);

			List<Formula> andTerms = new ArrayList<Formula>();
			Formula.getAnds(orTerm, andTerms);
			focusFormulae[i] = new ArrayList<FocusStep>();
			List<FocusStep> unbound = calculateOptimalOrder(andTerms,
					focusFormulae[i], formula);
			if (!unbound.isEmpty()) {
				encounteredConjunctionWithUnbounded = true;
				unboundBinaryPredicates = unbound;
			}
		}
		if (encounteredConjunctionWithUnbounded) {
			String message = "Warning! Simplifying the Focus formula "
					+ formula + "\n" + "resulted in the formula " + this + ", "
					+ "which has the following unbound binary predicate(s) :\n"
					+ unboundBinaryPredicates + "\n"
					+ "This may lead to an infinite numebr of structures!";
			if (AnalysisStatus.emitWarnings)
				throw new FocusNonTerminationException(message);
			else
				Logger.println(message);
		}
	}

	public boolean isInVocabulary(TVS structure) {
		for (Predicate predicate : formulaPredicates) {
			if (!structure.getVocabulary().all().contains(predicate)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the list of unbound (binary) predicates.
	 */
	private List<FocusStep> calculateOptimalOrder(List<Formula> terms,
			List<FocusStep> bound, Formula origFormula) {
		List<FocusStep> nullary = new ArrayList<FocusStep>();
		List<FocusStep> unary = new ArrayList<FocusStep>();
		List<FocusStep> unaryUnique = new ArrayList<FocusStep>();
		List<FocusStep> binary = new ArrayList<FocusStep>();
		Set<Var> boundVariables = HashSetFactory.make();
		List<FocusStep> unbound = new ArrayList<FocusStep>();

		for (Iterator<Formula> it = terms.iterator(); it.hasNext();) {
			Formula formula = it.next();
			Formula forTCformula = formula;
			if (formula instanceof NotFormula) {
				NotFormula nformula = (NotFormula) formula;
				forTCformula = nformula.subFormula();
			}
			if (forTCformula instanceof TransitiveFormula) {
				TransitiveFormula tformula = (TransitiveFormula) forTCformula;
				formula = tformula.subFormula();
				formula.substituteVar(tformula.subLeft(), Var.allocateVar());
				formula.substituteVar(tformula.subRight(), Var.allocateVar());
			}
			Literal literal = new Literal(formula);
			FocusStep step = new FocusStep(literal);
			if (literal.atomic instanceof PredicateFormula) {
				PredicateFormula predicateFormula = (PredicateFormula) literal.atomic;
				Predicate predicate = predicateFormula.predicate();
				if (predicate.arity() == 0)
					nullary.add(step);
				else if (predicate.arity() == 1) {
					if (predicate.unique()) {
						unaryUnique.add(step);
					} else {
						unary.add(step);
					}
					if (predicate.unique() && !literal.negated)
						boundVariables.add(predicateFormula.getVariable(0));
				} else
					// binary and on
					unbound.add(step);
			} else if (literal.atomic instanceof EqualityFormula) {
				EqualityFormula qformula = (EqualityFormula) literal.atomic;
				if (qformula.left().equals(qformula.right())) {
					// This is always 1. No need to focus on it.
					continue;
				} else {
					if (!AnalysisStatus.terse)
						throw new FocusNonTerminationException(
								"Trying to focus on non trivial equality will"
										+ " lead to"
										+ " infinite structures if"
										+ " summary nodes exist in formula "
										+ origFormula);
				}
			} else if (literal.atomic instanceof ValueFormula) {
				ValueFormula vformula = (ValueFormula) literal.atomic;
				Kleene value = vformula.value();
				if (value == Kleene.unknownKleene) {
					throw new SemanticErrorException(
							"Trying to focus on constant unknown.");
				} else {
					// This is always 1/0. No need to focus on it.
					continue;
				}
			} else {
				unbound.add(step);
			}
		}

		boolean change = true;
		while (change) {
			change = false;
			for (Iterator<FocusStep> moreUnbound = unbound.iterator(); moreUnbound
					.hasNext();) {
				FocusStep step = moreUnbound.next();
				Literal literal = step.literal;
				if (literal.atomic instanceof PredicateFormula
						&& ((PredicateFormula) literal.atomic).predicate()
								.arity() == 2) {
					PredicateFormula bformula = (PredicateFormula) literal.atomic;
					if (boundVariables.contains(bformula.getVariable(0))) {
						binary.add(step);
						change = true;
						moreUnbound.remove();
						if (bformula.predicate().function() && !literal.negated)
							boundVariables.add(bformula.getVariable(1));
					} else if (boundVariables.contains(bformula.getVariable(1))) {
						binary.add(step);
						change = true;
						moreUnbound.remove();
						if (bformula.predicate().invfunction()
								&& !literal.negated)
							boundVariables.add(bformula.getVariable(0));
					}
				} else { // internal error
					throw new RuntimeException("Unknown atomic formula "
							+ literal.atomic);
				}
			}
		}

		for (Iterator<FocusStep> moreUnbound = unbound.iterator(); moreUnbound
				.hasNext();) {
			FocusStep step = moreUnbound.next();
			Literal literal = step.literal;
			PredicateFormula bformula = (PredicateFormula) literal.atomic;
			if ((bformula.predicate().function() || bformula.predicate()
					.invfunction()) && !literal.negated) {
				binary.add(step);
				moreUnbound.remove();
			}
		}

		// Now take the all the formulas in optimal order:
		bound.addAll(nullary);
		bound.addAll(unaryUnique);
		bound.addAll(unary);
		bound.addAll(binary);
		bound.addAll(unbound);

		// Now calculate in each step which variables are interesting and
		// which are needed for future steps.
		Collection<Var> total = HashSetFactory.make();
		for (int i = 0; i < bound.size(); i++) {
			FocusStep step = bound.get(i);
			Collection<Var> vars = step.literal.atomic.freeVars();

			// Variables bound by this step.
			step.newVariables.addAll(vars);
			step.newVariables.removeAll(total);
			// Variables used in this step that were bounded before.
			step.boundVariables.addAll(vars);
			step.boundVariables.removeAll(step.newVariables);

			// Add current variables to the total.
			total.addAll(vars);
			if (i < (bound.size() - 1)) {
				FocusStep succ = bound.get(i + 1);
				step.futureVariables = succ.nowAndFutureVariables;
			}
			for (int j = 0; j <= i; j++) {
				FocusStep prevStep = bound.get(j);
				prevStep.nowAndFutureVariables.addAll(vars);
			}
		}
		return unbound;
	}

	/**
	 * @author Roman Manevich
	 * @since 04.5.2001 Initial creation.
	 */
	public String toString() {
		StringBuffer out = new StringBuffer(focusFormulae.length * 5);
		for (int i = 0; i < focusFormulae.length; ++i) {
			List conjunction = focusFormulae[i];
			out.append("(");
			for (Iterator conjuctionIterator = conjunction.iterator(); conjuctionIterator
					.hasNext();) {
				FocusStep focusStep = (FocusStep) conjuctionIterator.next();
				Literal literal = focusStep.literal;
				out.append(literal);
				if (conjuctionIterator.hasNext())
					out.append(" & ");
			}
			out.append(")");
			if (i < focusFormulae.length - 1)
				out.append(" | ");
		}
		return out.toString();
	}
}
