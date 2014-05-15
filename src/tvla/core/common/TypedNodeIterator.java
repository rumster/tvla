package tvla.core.common;

import java.util.Iterator;

import tvla.core.Node;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.formulae.Formula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;

/** A filtering iterator for iterating over nodes of a specified type.
 * The type is specified by a unary predicate.
 * @author Roman Manevich.
 */
public class TypedNodeIterator  implements Iterator {
	/** A reusable variable.
	 */
	private Var v;
	
	/** An internal iterator over assignments.
	 */
	private Iterator assignIter;
	
	/** Constructs an iterator over nodes in a specified structure
	 * with a specified type.
	 * @param structure The structure containing the nodes.
	 * @param type A predicate serving as a type qualifier.
	 * precondition: type.arity() == 1
	 */
	public TypedNodeIterator(TVS strcuture, Predicate type) {
		v = new Var("v");
		Formula nodeSelector = new PredicateFormula(type, v);
		assignIter = strcuture.evalFormulaForValue(nodeSelector, Assign.EMPTY, Kleene.trueKleene);
	}
	
	/** Checks whether there are more nodes of the specified type.
	 */
	public boolean hasNext() {
		return assignIter.hasNext();
	}
	
	/** Returns the next node.
	 */
	public Object next() {
		Assign assign = (Assign) assignIter.next();
		Node threadNode = assign.get(v);
		return threadNode;
	}
	
	/** Unsupported.
	 */
	public void remove() {
		throw new UnsupportedOperationException();
	}
}