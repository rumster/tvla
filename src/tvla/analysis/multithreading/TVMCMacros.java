package tvla.analysis.multithreading;

import java.util.Iterator;

import tvla.core.Node;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.formulae.Formula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.predicates.LocationPredicate;
import tvla.predicates.Vocabulary;

/** A class that provides macros for handling structures for TVMC.
 * @author Roman Manevich,
 * @since 14.7.2001 Initial creation.
 */
public class TVMCMacros {
	/** Adds the given new node as a thread node.
	 * postcondition: structure.eval(Vocabulary.isThread, n) == true
	 * @author Eran Yahav
 	 * @since 14.7.2001 Moved this functionality here from the old structure interface.
	 */
	public static Node newThreadNode(TVS structure) {
		Node n = structure.newNode();
		structure.modify(Vocabulary.isThread);
		structure.update(Vocabulary.isThread, n, Kleene.trueKleene);
		return n;
	}
	
	/** Add the given new node as a thread node starting from the given entry label.
	 * postcondition: structure.eval(Vocabulary.isThread, n) == true
	 * @author Eran Yahav
	 * @since 14.7.2001 Moved this functionality here from the old structure interface.
	 */
	public static Node newThreadNode(TVS structure, String entryLabel) {
		Node n = structure.newNode();
		structure.modify(Vocabulary.isThread);
		structure.update(Vocabulary.isThread, n, Kleene.trueKleene);
		LocationPredicate locationPredicate = Vocabulary.findLocationPredicate(entryLabel);
		structure.modify(locationPredicate);
		structure.update(locationPredicate, n, Kleene.trueKleene);
		return n;
	}
	
	/** Returns an iterator over the nodes for which isThread is true.
	 */
	public static Iterator<Node> allThreadNodes(TVS structure) {
		return new ThreadNodesIterator(structure);
	}
	
	/** An iterator over the nodes representing threads.
	 * @author Roman Manevich.
	 */
	public static class ThreadNodesIterator implements Iterator<Node> {
		private Var t;
		private Iterator assignIter;
		
		public ThreadNodesIterator(TVS strcuture) {
			t = new Var("t");
			Formula threadSelector = new PredicateFormula(Vocabulary.isThread, t);
			assignIter = strcuture.evalFormulaForValue(threadSelector, Assign.EMPTY, Kleene.trueKleene);
		}
		
		public boolean hasNext() {
			return assignIter.hasNext();
		}
		
		public Node next() {
			Assign assign = (Assign) assignIter.next();
			Node threadNode = assign.get(t);
			return threadNode;
		}
		
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}