/*
 * Created on Mar 4, 2004
 *
 */
package tvla.core.functional;

import junit.framework.TestCase;
import tvla.core.TVSFactory;
import tvla.core.TVSTest;

/** JUnit tests for the functional TVS implementation.
 * 
 * @author Roman Manevich
 */
public class NodePredTVSTest extends TVSTest {

	/** Constructor for NodePredTVSTest.
	 * @param name The name of the test.
	 */
	public NodePredTVSTest(String name) {
		super(name);
	}

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(NodePredTVSTest.class);
	}

	/** @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		TVSFactory.setTVSFactoryClass("functional");
		super.setUp();
	}
	
	/** Tests setUp to make sure it successfuly set the TVSFactory to be
	 * NodePredTVSFactory.
	 */	
	public void testThatNodePredFactoryWasSet() {
		assertTrue("setUp Should have set the TVSFactory to be NodePredTVSFactory!", TVSFactory.getInstance() instanceof NodePredTVSFactory);
	}
}