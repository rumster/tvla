package tvla.core.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import tvla.analysis.AnalysisStatus;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.SparseTVS;
import tvla.core.TVS;
import tvla.core.Update;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignKleene;
import tvla.core.base.PredicateUpdater;
import tvla.core.common.ModifiedPredicates;
import tvla.core.common.NodePair;
import tvla.core.common.NodeTupleIterator;
import tvla.formulae.CloneUpdateFormula;
import tvla.formulae.Formula;
import tvla.formulae.NewUpdateFormula;
import tvla.formulae.NotFormula;
import tvla.formulae.PredicateUpdateFormula;
import tvla.formulae.RetainUpdateFormula;
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashMapFactory;

/** A generic update algorithm.
 * @author Roman Manevich.
 * @since tvla-2-alpha (September 6 2001) Initial creation.
 */
public class GenericUpdate extends Update {
	/** A convenience object.
	 */
	protected static Update defaultGenericUpdate = new GenericUpdate();
	
	public static void reset() {
		defaultGenericUpdate = new GenericUpdate();
	}
	
	/** Updates the structure's predicate interpretations according to the specified
	 * update formulae and a partial assignment to their variables.
	 * @param structure The structure to update.
	 * @param updateFormulae A collection of PredicateUpdateFormula objects.
	 * @param assignment A partial assignment to the variables of the right-hand side
	 * of the formulae.
	 * @see tvla.formulae.PredicateUpdateFormula
	 * @author Tal Lev-Ami
	 * @author Roman Manevich
	 * @todo Optimize extraction of node tuples from assignments.
	 * @since tvla-2-alpha Generalized for arbitrary arity predicates (16 May 2002 Roman). 
	 * @since 6/9/2001 Moved here from Action.
	 */
    
	public void updatePredicates(TVS structure,
								 Collection<PredicateUpdateFormula> updateFormulae, 
								 Assign assignment) {
      
      //DHACK
      //TVLAAPITrace.tracePrintln("GeenricUpdate.updatePredicates " + ++updPredCounter  + " assignment = " + assignment);
      
		TVS oldVersion = structure.copy();
		for (PredicateUpdateFormula updateFormula : updateFormulae) {
			Formula formula = updateFormula.getFormula(); // the formula's right-hand side
			Predicate predicate = updateFormula.getPredicate();
			if (!structure.getVocabulary().contains(predicate)) {
			    continue;
			}
			
			ModifiedPredicates.modify(structure, predicate);
			//ModifiedPredicates.modify(predicate);
			if (predicate.arity() == 0) {
			    // Attempt to solve TC cache bug by calling prepare on eval
                formula.prepare(oldVersion);
				Kleene newValue = formula.eval(oldVersion, assignment);
				structure.update(predicate, newValue);
			}
			else {
				structure.clearPredicate(predicate); // remove old values
				formula.prepare(oldVersion);
				PredicateUpdater updater = PredicateUpdater.updater(predicate, structure);
				for (Iterator<AssignKleene> satisfyIt = formula.assignments(oldVersion, assignment);
					 satisfyIt.hasNext();) {
					AssignKleene result = satisfyIt.next();
					updateFormula.update(updater, result, result.kleene);
				}
			}
		}
	}

	/** Applies the specified <tt> new </tt> formula to add new nodes to the structure.
	 * @param structure The structure to update.
	 * @param formula A NewUpdateFormula object.
	 * @param assignment A partial assignment to the variables of the formula.
	 * @return A collection containing the new nodes.
	 * @author Tal Lev-Ami
	 * @author Roman Manevich
	 * @since 6/9/2001 Moved here from Action.
	 */
	public Collection<Node> applyNewUpdateFormula(TVS structure,
									  NewUpdateFormula formula, 
									  Assign assignment) {
		Collection<Node> answer = null;
		Node newNode = null;
		Var newVar = formula.newVar;
		if (newVar == null) { // nullary new formula
			newNode = structure.newNode();
			structure.update(Vocabulary.isNew, newNode, Kleene.trueKleene);
			answer = Collections.singleton(newNode);
		}
		else { // unary new formula
			// answer = new ArrayList(1);
			answer = new LinkedList<Node>();
			List<NodePair> nodes = new LinkedList<NodePair>();
			TVS oldVersion = structure.copy();
			
			formula.prepare(oldVersion);
			for (Iterator<AssignKleene> i = formula.assignments(oldVersion, assignment); 
			 	i.hasNext(); ) {
				AssignKleene anotherNew = i.next();
				Node oldNode = (Node)anotherNew.get(newVar);
				newNode = structure.newNode();
				structure.update(Vocabulary.active, newNode, oldVersion.eval(Vocabulary.active, oldNode));
				answer.add(newNode);
				if (anotherNew.kleene == Kleene.unknownKleene)
					structure.update(Vocabulary.active, newNode, Kleene.unknownKleene);
				nodes.add(new NodePair(oldNode, newNode));
			}
			
			if (answer.isEmpty())
				return answer;
			
			for (Iterator<Node> i = answer.iterator(); i.hasNext();) {
				structure.update(Vocabulary.isNew, i.next(), Kleene.trueKleene);
			}
			
			for (Iterator<NodePair> j = nodes.iterator();  j.hasNext();) {
				NodePair pair = j.next();
				structure.update(Vocabulary.instance, pair.second(), pair.first(), Kleene.trueKleene);
			}
			
			for (Iterator<NodePair> j = nodes.iterator();  j.hasNext();) {
				NodePair pair = j.next();
				structure.update(Vocabulary.sm, pair.second(), oldVersion.eval(Vocabulary.sm, pair.first()));
			}
			/*
			for (Iterator i = oldVersion.evalFormula(formula.getFormula(), assignment); 
				 i.hasNext(); ) {
				AssignKleene anotherNew = (AssignKleene) i.next();
				Node oldNode = (Node) anotherNew.get(newVar);
				newNode = structure.newNode();
				structure.update(Vocabulary.instance, newNode, oldNode, Kleene.trueKleene);
				structure.update(Vocabulary.isNew, newNode, Kleene.trueKleene);
				structure.update(Vocabulary.sm, newNode, oldVersion.eval(Vocabulary.sm, oldNode));
				structure.update(Vocabulary.active, newNode, oldVersion.eval(Vocabulary.active, oldNode));
				answer.add(newNode);
				if (anotherNew.kleene == Kleene.unknownKleene)
					structure.update(Vocabulary.active, newNode, Kleene.unknownKleene);
				modified = true;
			}
			*/
		}
		
		ModifiedPredicates.modify(structure, Vocabulary.active);

		if (AnalysisStatus.debug)
			tvla.io.IOFacade.instance().printStructure(structure, "After New");
		return answer;	
	}
	
	/** Applies the specified <tt> clone </tt> formula to clone a sub-structure.
	 * @param formula A formula with one free variable that's used to mark
	 * the part of the universe that should be cloned. The nodes in the cloned
	 * part have isNew = true.
	 * @param assignment A partial assignment to the variables of the formula.
	 * @return A collection containing the new nodes.
	 * @see tvla.formulae.NewUpdateFormula
	 */
	public Collection<Node> applyCloneUpdateFormula(TVS structure,
										CloneUpdateFormula formula, 
										Assign assignment) {
		Collection<Node> answer = new ArrayList<Node>(1);
		Var cloneVar = formula.var;
		TVS oldVersion = structure.copy();
		Map<Node, Node> originalToCloned = HashMapFactory.make(16);
		// Clone the nodes specified by the formulae.
		for (Iterator<AssignKleene> i = oldVersion.evalFormula(formula.getFormula(), assignment); i.hasNext(); ) {
			AssignKleene anotherNew = i.next();
			Node originalNode = (Node) anotherNew.get(cloneVar);
			Node clonedNode = structure.newNode();
			originalToCloned.put(originalNode, clonedNode);
			answer.add(clonedNode);
		}
		
		if (originalToCloned.isEmpty())
			return answer;

		// Update predicate values for the cloned nodes according to the 
		// predicate values for the original nodes.
		Iterator<Predicate> predIter = null;
		if (structure instanceof SparseTVS) {
			predIter = ((SparseTVS) structure).nonZeroPredicates();
		}
		else {
			predIter = structure.getVocabulary().positiveArity().iterator();
		}
		while (predIter.hasNext()) {
			Predicate predicate = (Predicate) predIter.next();
			//ModifiedPredicates.modify(predicate);
			//ModifiedPredicates.modify(structure, predicate);
			switch (predicate.arity()) {
			case 0: // nullary predicates
				break;
				
			case 1: // unary predicates
				ModifiedPredicates.modify(structure, predicate);
				for (Iterator<Map.Entry<Node, Node>> entryIter = originalToCloned.entrySet().iterator();
					 entryIter.hasNext(); ) {
					Map.Entry<Node, Node> entry = entryIter.next();
					structure.update(predicate, entry.getValue(),
									 structure.eval(predicate, entry.getKey()));
				}
				break;
				
			default: // predicate of arity > 1
				ModifiedPredicates.modify(structure, predicate);
				Node [] nodesTmp = new Node[predicate.arity()];
				Iterator<? extends NodeTuple> tupleIter = NodeTupleIterator.createIterator(originalToCloned.keySet(),
																	  predicate.arity());
				while (tupleIter.hasNext()) {
					NodeTuple originalTuple = tupleIter.next();
					for (int index = 0; index < predicate.arity(); ++index)
						nodesTmp[index] = originalToCloned.get(originalTuple.get(index));
					NodeTuple clonedTuple = NodeTuple.createTuple(nodesTmp);
					structure.update(predicate, clonedTuple,
									 structure.eval(predicate, originalTuple));
				}
			}
		}
		
		// Update the values of isNew and instance for the new nodes.
		for (Iterator<Map.Entry<Node, Node>> entryIter = originalToCloned.entrySet().iterator(); 
			 entryIter.hasNext(); ) {
			Map.Entry<Node, Node> entry = entryIter.next();
			Node originalNode = entry.getKey();
			Node clonedNode = entry.getValue();
			structure.update(Vocabulary.isNew, clonedNode, Kleene.trueKleene);
			structure.update(Vocabulary.instance, clonedNode, originalNode, Kleene.trueKleene);
		}
		ModifiedPredicates.modify(structure, Vocabulary.active);
		
		if (AnalysisStatus.debug)
			tvla.io.IOFacade.instance().printStructure(structure, "After Clone");
		return answer;
	}

	/** Applies the specified <tt> retain </tt> formula to determine which nodes
	 * to reatin structure.
	 * @param structure The structure to update.
	 * @param formula A NewUpdateFormula object.
	 * @param assignment A partial assignment to the variables of the formula.
	 * @param refStructure The structure on which the formula is evaluated
	 */
	public void applyRetainUpdateFormula(TVS structure,
										 RetainUpdateFormula formula, 
										 Assign assignment,
										 TVS refStructure) {
		Var retainVar = formula.retainVar;
		Collection<Node> toDelete = new ArrayList<Node>();
		Collection<Node> maybeActive = new ArrayList<Node>();
		for (Iterator<AssignKleene> i = refStructure.evalFormula(new NotFormula(formula.getFormula()), 
												assignment); 
			 i.hasNext(); ) {
			AssignKleene anotherDelete = i.next();
			Node nodeToDelete = (Node) anotherDelete.get(retainVar);		
			if (anotherDelete.kleene == Kleene.unknownKleene)
				maybeActive.add(nodeToDelete);
			else
				toDelete.add(nodeToDelete);
		}

		for (Iterator<Node> i = toDelete.iterator(); i.hasNext(); ) {
			Node node = i.next();
			structure.removeNode(node);		
		}
		for (Iterator<Node> i = maybeActive.iterator(); i.hasNext(); ) {
			Node node = i.next();
			structure.update(Vocabulary.active, node, Kleene.unknownKleene);
		}
		
		if (!maybeActive.isEmpty() || !toDelete.isEmpty())
			ModifiedPredicates.modify(structure, Vocabulary.active);

		if (AnalysisStatus.debug)
			tvla.io.IOFacade.instance().printStructure(structure, "After Retain");
	}
}
