package tvla.io;

import java.io.PrintStream;

/** A class for directing information produced by the analysis to output streams.
 * This class is used to separate logical outputs from physical streams.
 * @author Roman Manevich.
 * @since tvla-2-alpha.
 */
public class APIIOFacade extends  IOFacade  {
	APIIOFacade() {
		super();
	}
	
	/** PrintStructure hook.
	 */
	public void printStructure(Object structure, String header) {
		String breachedHeader = null;

        /* TODO see if we can pass the courrent transformer
        if (header.indexOf("Constraint Breached") >=0) {
			breachedHeader = "Location \t : " + Engine.getCurrentLocation().label() + "\n" +
					 "Action \t : " + Engine.getCurrentAction().toString() + "\n" +
					 header;
		}
		*/
        
		super.printStructure(structure, header, breachedHeader);
	}

  protected void printProgram(PrintStream bundleStream, StringConverter c, Object program) {
    throw new RuntimeException("Unreachable code");
  }

  public void printAnalysisState(Object state) {
    throw new RuntimeException("Unreachable code");
  }	

}