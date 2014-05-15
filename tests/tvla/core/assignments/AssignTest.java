/*
 * Created on May 25, 2004
 *
 */
package tvla.core.assignments;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;
import tvla.core.Node;
import tvla.core.TVS;
import tvla.core.TVSFactory;
import tvla.formulae.Formula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;

/** JUnit tests for class Assign.
 * TODO: finish writing tests.
 * 
 * @author Alexey Loginov
 */
public class AssignTest extends TestCase {
	protected TVS empty;
	protected TVS tvs1;
	protected TVS tvs2;
	protected TVS tvs3;
	protected TVS tvs4;
	protected TVS tvs5;

	protected Node n21;
	protected Node n22;
	
	protected static Predicate absUnary = Vocabulary.createPredicate("absUnary", 1, true);
	protected Formula absUnaryFormula;

	protected Set<Var> emptyVarSet = new HashSet<Var>();
	protected Set<Var> oneVarSet   = new HashSet<Var>();
	protected Set<Var> twoVarSet   = new HashSet<Var>();
	protected Set<Var> threeVarSet = new HashSet<Var>();
	protected Set<Var> fourVarSet  = new HashSet<Var>();

	/** Constructor for AssignTest.
	 * @param name The name of the test.
	 */
	public AssignTest(String name) {
		super(name);
	}

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(AssignTest.class);
	}

	/** @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		TVSFactory.getInstance().init();
		
		empty = TVSFactory.getInstance().makeEmptyTVS();
		tvs2 = TVSFactory.getInstance().makeEmptyTVS();
		tvs3 = TVSFactory.getInstance().makeEmptyTVS();
		tvs4 = TVSFactory.getInstance().makeEmptyTVS();
		tvs5 = TVSFactory.getInstance().makeEmptyTVS();

		n21 = tvs2.newNode(); n22 = tvs2.newNode();
		tvs3.newNode(); tvs3.newNode(); tvs3.newNode();
		tvs4.newNode(); tvs4.newNode(); tvs4.newNode(); tvs4.newNode();
		tvs5.newNode(); tvs5.newNode(); tvs5.newNode(); tvs5.newNode(); tvs5.newNode();

		Var v1 = new Var("v1");
		Var v2 = new Var("v2");
		Var v3 = new Var("v3");
		Var v4 = new Var("v4");

		oneVarSet.add(v1);
		twoVarSet.add(v1);   twoVarSet.add(v2);
		threeVarSet.add(v1); threeVarSet.add(v2); threeVarSet.add(v3);
		fourVarSet.add(v1);  fourVarSet.add(v2);  fourVarSet.add(v3);  fourVarSet.add(v4);

		absUnaryFormula = new PredicateFormula(absUnary, v1);
	}

	/** @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	/** Test for Assign's getAllAssign(Collection nodeSet, Set varSet).
	 *  Testing assignments to one variable in empty structure.
	 *  Expect returned iterator's hasNext() to return false.
	 *  Second call to hasNext() used to return true prior to rev 1.2 or Assign.java.
	 */
	public void testGetAllAssign01() {
		assertTrue(empty.nodes().size() == 0);
		assertTrue(oneVarSet.size() == 1);
		Iterator<Assign> assignIterator = Assign.getAllAssign(empty.nodes(), oneVarSet);
		assertFalse(assignIterator.hasNext());
		if (assignIterator.hasNext())
			fail("Assign.getAllAssign's iterator's hasNext returned true for an empty assignment set!");
	}

	/** Test for Assign's getAllAssign(Collection nodeSet, Set varSet).
	 *  Testing assingments to two vars in structure with five nodes.
	 *  Expect 25 assignments in returned iterator.
	 *  Used to get 30 prior to rev 1.2 of Assign.java.
	 */
	public void testGetAllAssign52() {
		assertTrue(tvs5.nodes().size() == 5);
		assertTrue(twoVarSet.size() == 2);
		Iterator<Assign> assignIterator = Assign.getAllAssign(tvs5.nodes(), twoVarSet);
		int count = 0;
		for (; assignIterator.hasNext(); count++, assignIterator.next());
		if (count != 25)
			fail("Assign.getAllAssign's iterator has " + count + " assignments instead of 25 for 5 nodes and 2 vars!");
	}

	/** Test for Assign's getAllAssign(Collection nodeSet, Set varSet).
	 *  Testing assingments to three vars in structure with four nodes.
	 *  Expect 64 assignments in returned iterator.
	 *  Used to get 84 prior to rev 1.2 of Assign.java.
	 */
	public void testGetAllAssign43() {
		assertTrue(tvs4.nodes().size() == 4);
		assertTrue(threeVarSet.size() == 3);
		Iterator<Assign> assignIterator = Assign.getAllAssign(tvs4.nodes(), threeVarSet);
		int count = 0;
		for (; assignIterator.hasNext(); count++, assignIterator.next());
		if (count != 64)
			fail("Assign.getAllAssign's iterator has " + count + " assignments instead of 64 for 4 nodes and 3 vars!");
	}

	/** Test for Assign's getAllAssign(Collection nodeSet, Set varSet).
	 *  Testing assingments to four vars in structure with three nodes.
	 *  Expect 81 assignments in returned iterator.
	 *  Used to get 120 prior to rev 1.2 of Assign.java.
	 */
	public void testGetAllAssign34() {
		assertTrue(tvs3.nodes().size() == 3);
		assertTrue(fourVarSet.size() == 4);
		Iterator<Assign> assignIterator = Assign.getAllAssign(tvs3.nodes(), fourVarSet);
		int count = 0;
		for (; assignIterator.hasNext(); count++, assignIterator.next());
		if (count != 81)
			fail("Assign.getAllAssign's iterator has " + count + " assignments instead of 81 for 3 nodes and 4 vars!");
	}

	/** Test for Assign's getAllAssign(Collection nodeSet, Set varSet).
	 *  Testing assingments to 0 vars in empty structure.
	 *  Expect 1 (empty) assignment in returned iterator.
	 *  Used to get 0 prior to rev 1.2 of Assign.java.
	 */
	public void testGetAllAssign00() {
		assertTrue(empty.nodes().size() == 0);
		assertTrue(emptyVarSet.size() == 0);
		Iterator<Assign> assignIterator = Assign.getAllAssign(empty.nodes(), emptyVarSet);
		if (!assignIterator.hasNext())
			fail("Assign.getAllAssign's iterator should always yield the empty assignment when the var set is empty!");
	}

	/** Test for TVS's evalFormula(Formula formula, Assign partialAssignment).
	 *  Testing assignments possibly satisfying absUnaryFormula in empty structure.
	 *  Expect 0 assignments in returned iterator.
	 *  Second call to hasNext() used to return true prior to rev 1.2 or EvalFormula.java.
	 *  Additionally, second call to hasNext() results in a NullPointerException if
	 *  file Assign.java is older than rev 1.2.
	 */
	public void testEvalFormula01() {
		assertTrue(empty.nodes().size() == 0);
		Iterator<AssignKleene> assignIterator = empty.evalFormula(absUnaryFormula, new Assign());
		assertFalse(assignIterator.hasNext());
		if (assignIterator.hasNext())
			fail("TVS.evalFormula's iterator's hasNext returned true for an empty assignment set!");
	}

	/** Test for TVS.java's evalFormulaForValue(Formula formula, Assign partialAssignment, Kleene desiredValue).
	 *  Testing assignments making absUnaryFormula 1/2 in structure tvs2.
	 *  Expect 0 assignments in returned iterator.
	 *  Second call to hasNext() used to return true prior to rev 1.2 or EvalFormula.java.
	 *  Additionally, second call to hasNext() results in a NullPointerException if
	 *  file Assign.java is older than rev 1.2.
	 */
	public void testEvalFormulaForValue21() {
		tvs2.update(absUnary, n21, Kleene.trueKleene);
		tvs2.update(absUnary, n22, Kleene.trueKleene);
		assertTrue(tvs2.eval(absUnary, n21) == Kleene.trueKleene);
		assertTrue(tvs2.eval(absUnary, n22) == Kleene.trueKleene);
		assertTrue(tvs2.nodes().size() == 2);

		Iterator<AssignKleene> assignIterator = tvs2.evalFormulaForValue(absUnaryFormula, new Assign(), Kleene.unknownKleene);
		assertFalse(assignIterator.hasNext());
		if (assignIterator.hasNext())
			fail("TVS.evalFormulaForValue's iterator's hasNext returned true for an empty assignment set!");
	}
}