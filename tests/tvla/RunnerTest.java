/* Created on 22/06/2004
 */
package tvla;

import junit.framework.TestCase;
import tvla.util.ProgramProperties;

/** Unit tests for tvla.Runner.
 * @author Eran Yahav yahave
 */
public class RunnerTest extends TestCase {

	/** Constructor for RunnerTest.
	 * @param name The name of the test.
	 */
	public RunnerTest(String name) {
		super(name);
	}

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(RunnerTest.class);
	}

	/** @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();

	}

	/**
	 * test parseArgs
	 * This test is required to make sure that changes to Runner do not break
	 * existing tvla options
	 */
	public void testParseAgrs() {

		/**
		 * argument string arrays to be tested
		 * to add new argument strings to this test, add strings 
		 * at _the end_ of the array.
		 */
		//		String testArgs[][] = {
		//				"tvpName", "-ms", "42" }, {
		//				"tvpName", "-mm", "42" }
		//		};

		try {
			String argStringArray[] = { "tvpName" };
			Runner.parseArgs(argStringArray);
			String programName =
				ProgramProperties.getProperty("tvla.programName", "");
			String tvsName = Runner.inputFile;
			assertTrue(
				"wrong parsing of TVP name",
				programName.equals("tvpName"));
			assertTrue("wrong parsing of TVS name", tvsName.equals("tvpName"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		try {
			String argStringArray[] = { "tvpName", "tvsName" };
			Runner.parseArgs(argStringArray);
			String programName =
				ProgramProperties.getProperty("tvla.programName", "");
			String tvsName = Runner.inputFile;
			assertTrue("wrong parsing of TVS name", tvsName.equals("tvsName"));
			assertTrue(
				"wrong parsing of TVP name",
				programName.equals("tvpName"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		testJoinType("rel");
		testJoinType("part");
		testJoinType("part_embedding");
		testJoinType("j3");
		testJoinType("ind");

		try {
			String argStringArray[] = { "tvpName", "-backward" };
			Runner.parseArgs(argStringArray);
			String programName =
				ProgramProperties.getProperty("tvla.programName", "");
			String tvsName = Runner.inputFile;
			assertTrue("wrong parsing of TVS name", tvsName.equals("tvpName"));
			assertTrue(
				"wrong parsing of TVP name",
				programName.equals("tvpName"));

			boolean backward =
				ProgramProperties.getBooleanProperty(
					"tvla.cfg.backwardAnalysis",
					false);
			assertTrue("error parsing -backward option", backward);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		testSave("back");
		testSave("ext");
		testSave("all");

	}

	private void testJoinType(String joinType) {
		try {
			String argStringArray[] = { "tvpName", "-join", joinType };
			Runner.parseArgs(argStringArray);
			String programName =
				ProgramProperties.getProperty("tvla.programName", "");
			String tvsName = Runner.inputFile;
			String theJoinType =
				ProgramProperties.getProperty("tvla.joinType", "");

			assertTrue("wrong parsing of TVS name", tvsName.equals("tvpName"));
			assertTrue(
				"wrong parsing of TVP name",
				programName.equals("tvpName"));
			assertTrue(
				"wrong parsing of join type",
				theJoinType.equals(joinType));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void testSave(String saveKind) {
		try {
			String argStringArray[] = { "tvpName", "-save", saveKind };
			Runner.parseArgs(argStringArray);
			String programName =
				ProgramProperties.getProperty("tvla.programName", "");
			String tvsName = Runner.inputFile;
			String theSaveKind =
				ProgramProperties.getProperty("tvla.cfg.saveLocations", "");

			assertTrue("wrong parsing of TVS name", tvsName.equals("tvpName"));
			assertTrue(
				"wrong parsing of TVP name",
				programName.equals("tvpName"));
			assertTrue(
				"wrong parsing of join type",
				theSaveKind.equals(saveKind));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
