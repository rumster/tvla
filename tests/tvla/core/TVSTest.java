/*
 * Created on Mar 4, 2004
 *
 */
package tvla.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import junit.framework.TestCase;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;

/** JUnit tests for class TVS implementations.
 * TODO: finish writing tests.
 * 
 * @author Roman Manevich
 */
public class TVSTest extends TestCase {
	protected TVS empty;
	protected TVS tvs1;
	
	protected static Predicate absNullary1 = Vocabulary.createPredicate("absNullary1", 0, true);
	protected static Predicate absNullary2 = Vocabulary.createPredicate("absNullary2", 0, true);
	protected static Predicate absUnary1 = Vocabulary.createPredicate("absUnary1", 1, true);
	protected static Predicate absUnary2 = Vocabulary.createPredicate("absUnary2", 1, true);
	protected static Predicate binary1 = Vocabulary.createPredicate("binary1", 2, false);
	protected static Predicate binary2 = Vocabulary.createPredicate("binary2", 2, false);

	protected static Predicate x = Vocabulary.createPredicate("x", 1, true);
	protected static Predicate y = Vocabulary.createPredicate("y", 1, true);
	protected static Predicate t = Vocabulary.createPredicate("t", 1, true);
	protected static Predicate n = Vocabulary.createPredicate("n", 2, false);

	/** Constructor for TVSTest.
	 * @param name The name of the test.
	 */
	public TVSTest(String name) {
		super(name);
	}

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(TVSTest.class);
	}

	/** @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		TVSFactory.getInstance().init();
		empty = TVSFactory.getInstance().makeEmptyTVS();
		tvs1 = TVSFactory.getInstance().makeEmptyTVS();
	}
	
	protected TVS createSLL() {
		TVS sll;
		sll = TVSFactory.getInstance().makeEmptyTVS();
		Node head = sll.newNode();
		Node tail = sll.newNode();
		sll.update(Vocabulary.sm, tail, Kleene.unknownKleene);
		sll.update(x, head, Kleene.trueKleene);
		sll.update(n, head, tail, Kleene.unknownKleene);
		sll.update(n, tail, tail, Kleene.unknownKleene);
		return sll;
	}

	/** @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	/** Test for TVS copy().
	 */
	//public void testCopy() {
	//	TVS sll1 = createSLL();
	//	TVS sll2 = sll1.copy();
	//}

	/** Test for void update(Predicate, Kleene) and
	 * Kleene eval(Predicate).
	 */
	public void testEvalUpdatePredicate() {
		assertTrue(empty.eval(absNullary1) == Kleene.falseKleene);
		assertTrue(empty.eval(absNullary2) == Kleene.falseKleene);
		
		assertTrue(tvs1.eval(absNullary1) == Kleene.falseKleene);
		assertTrue(tvs1.eval(absNullary2) == Kleene.falseKleene);
		
		tvs1.update(absNullary1, Kleene.trueKleene);
		assertTrue(tvs1.eval(absNullary1) == Kleene.trueKleene);
		assertTrue(tvs1.eval(absNullary2) == Kleene.falseKleene);

		tvs1.update(absNullary2, Kleene.unknownKleene);
		assertTrue(tvs1.eval(absNullary1) == Kleene.trueKleene);
		assertTrue(tvs1.eval(absNullary2) == Kleene.unknownKleene);
		
		try {
			tvs1.update(absUnary1, Kleene.trueKleene);
			fail("TVS should not allow updating non-nullary predicates with update(Predicate,Kleene)!");
		}
		catch (AssertionError e) {
		}
		
		try {
			tvs1.eval(absUnary1);
			fail("TVS should not allow evaluating non-nullary predicates with eval(Predicate)!");
		}
		catch (AssertionError e) {
		}
	}

	/** Test for void update(Predicate, Node, Kleene) 
	 * and Kleene eval(Predicate, Node).
	 */
	public void testEvalPredicateNode() {
		Node n = tvs1.newNode();
		
		assertTrue(empty.eval(absUnary1, n) == Kleene.falseKleene);
		assertTrue(empty.eval(absUnary2, n) == Kleene.falseKleene);
		
		assertTrue(tvs1.eval(absUnary1, n) == Kleene.falseKleene);
		assertTrue(tvs1.eval(absUnary2, n) == Kleene.falseKleene);
		
		tvs1.update(absUnary1, n, Kleene.trueKleene);
		assertTrue(tvs1.eval(absUnary1, n) == Kleene.trueKleene);
		assertTrue(tvs1.eval(absUnary2, n) == Kleene.falseKleene);

		tvs1.update(absUnary2, n, Kleene.unknownKleene);
		assertTrue(tvs1.eval(absUnary1, n) == Kleene.trueKleene);
		assertTrue(tvs1.eval(absUnary2, n) == Kleene.unknownKleene);
		
		try {
			tvs1.update(absNullary1, n, Kleene.trueKleene);
			fail("TVS should not allow updating non-unary predicates with update(Predicate,Node,Kleene)!");
		}
		catch (AssertionError e) {
		}

		try {
			tvs1.eval(absNullary1, n);
			fail("TVS should not allow evaluating non-unary predicates with eval(Predicate,Node)!");
		}
		catch (AssertionError e) {
		}
	}

	/** Test for void update(Predicate, Node, Node, Kleene) and
	 * Kleene eval(Predicate, Node, Node).
	 */
	public void testEvalPredicateNodeNode() {
		Node n1 = tvs1.newNode();
		Node n2 = tvs1.newNode();
		
		assertTrue(empty.eval(binary1, n1, n2) == Kleene.falseKleene);
		assertTrue(empty.eval(binary2, n1, n2) == Kleene.falseKleene);
		
		assertTrue(tvs1.eval(binary1, n1, n2) == Kleene.falseKleene);
		assertTrue(tvs1.eval(binary2, n1, n2) == Kleene.falseKleene);
		
		tvs1.update(binary1, n1, n2, Kleene.trueKleene);
		assertTrue(tvs1.eval(binary1, n1, n2) == Kleene.trueKleene);
		assertTrue(tvs1.eval(binary2, n1, n2) == Kleene.falseKleene);

		tvs1.update(binary2, n1, n2, Kleene.unknownKleene);
		assertTrue(tvs1.eval(binary1, n1, n2) == Kleene.trueKleene);
		assertTrue(tvs1.eval(binary2, n1, n2) == Kleene.unknownKleene);
		
		try {
			tvs1.update(absNullary1, n1, n2, Kleene.trueKleene);
			fail("TVS should not allow updating non-binary predicates with update(Predicate,Node,Node,Kleene)!");
		}
		catch (AssertionError e) {
		}

		try {
			tvs1.eval(absNullary1, n1, n2);
			fail("TVS should not allow evaluating non-binary predicates with eval(Predicate,Node,Node)!");
		}
		catch (AssertionError e) {
		}
	}

	// Test for Kleene eval(Predicate, NodeTuple)
	//public void testEvalPredicateNodeTuple() {
	//}
	
	/** Test node creation and removal.
	 */
	public void testCreateRemoveNodes() {
		assertTrue("An empty TVS should return a non-null collection of nodes!", empty.nodes() != null);
		assertTrue("An empty TVS should return an empty collection of nodes!", empty.nodes().isEmpty());
		
		// Create a 100 nodes.
		Collection<Node> tmpNodes = new HashSet<Node>();
		for (int i = 0; i < 100; ++i) {
			tmpNodes.add( tvs1.newNode());
		}
		assertTrue("The universe of tvs1 should be equal to tmpNodes!", 
		tvs1.nodes().containsAll(tmpNodes) && tmpNodes.containsAll(tvs1.nodes()));

		// Remove 50% of the nodes and create 12.5% new ones on each
		// iteration until set is empty or contains just one node. 
		while (!tvs1.nodes().isEmpty() || tvs1.nodes().size() == 1) {
			Collection<Node> addedNodes = new HashSet<Node>();
			Collection<Node> removedNodes = new HashSet<Node>();
			Iterator<Node> nodeIter = tmpNodes.iterator();
			for (int i = 0; nodeIter.hasNext(); ++i) {
				if (i % 2 == 0) {
					Node n = (Node) nodeIter.next();
					removedNodes.add(n);
				}
			}
			
			for (int i = 0; nodeIter.hasNext(); ++i) {
				if (i % 8  == 0) {
					addedNodes.add(tvs1.newNode());
				}
			}
			
			for (Iterator<Node> removeIter = removedNodes.iterator();
			removeIter.hasNext(); ) {
				Node n = (Node) removeIter.next();
				tvs1.removeNode(n);
				tmpNodes.remove(n);
			}
			tmpNodes.addAll(addedNodes);
			
			assertTrue(
				"The universe of tvs1 should be equal to tmpNodes!",
				tvs1.nodes().containsAll(tmpNodes)
					&& tmpNodes.containsAll(tvs1.nodes()));
		}
	}

	//public void testDuplicateNode() {
	//}

	//public void testMergeNodes() {
	//}

	//public void testEvalFormula() {
	//}

	//public void testEvalFormulaForValue() {
	//}

	//public void testClearPredicate() {
	//}

	//public void testNumberSatisfy() {
	//}

	// Test for String toString()
	//public void testToString() {
	//}
}