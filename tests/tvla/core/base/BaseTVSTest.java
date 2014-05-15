/*
 * Created on Mar 4, 2004
 *
 */
package tvla.core.base;

import junit.framework.TestCase;
import tvla.core.TVSFactory;
import tvla.core.TVSTest;

/** JUnit tests for the functional TVS implementation.
 * 
 * @author Roman Manevich
 */
public class BaseTVSTest extends TVSTest {

	/** Constructor for BaseTVSTest.
	 * @param name The name of the test.
	 */
	public BaseTVSTest(String name) {
		super(name);
	}

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(BaseTVSTest.class);
	}

	/** @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		TVSFactory.setTVSFactoryClass("base");
		super.setUp();
	}
	
	/** Tests setUp to make sure it successfuly set the TVSFactory to be
	 * NodePredTVSFactory.
	 */	
	public void testThatBaseFactoryWasSet() {
		assertTrue("setUp Should have set the TVSFactory to be NodePredTVSFactory!", TVSFactory.getInstance() instanceof BaseTVSFactory);
	}
}