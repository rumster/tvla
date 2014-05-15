package tvla.io;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;

import tvla.analysis.Engine;
import tvla.util.Logger;

/** A class for directing information produced by the analysis to output streams.
 * This class is used to separate logical outputs from physical streams.
 * @author Roman Manevich.
 * @since tvla-2-alpha.
 */
public class IntraIOFacade extends  IOFacade  {
	IntraIOFacade() {
		super();
	}
	
	/** PrintStructure hook.
	 */
	public void printStructure(Object structure, String header) {
		if (Engine.getCurrentLocation() == null)
			return;
		
		if (!Engine.getCurrentLocation().getShouldPrint())
			return;

		String breachedHeader = null;
		if (header.indexOf("Constraint Breached") >=0) {
			breachedHeader = "Location \t : " + Engine.getCurrentLocation().label() + "\n" +
			         ( Engine.getCurrentAction() == null ? "No Action\n" : 
					 "Action \t : " + Engine.getCurrentAction().toString() + "\n") +
					 header;
		}

		super.printStructure(structure, header, breachedHeader);
	}

	
//	/** PrintLocation hook
//	 */
//	protected String convertNode(StringConverter c, Object node) {
//		return c.convert(node);
//	}
		
	
	/** PrintProgram hook
	 */
	protected void printProgram(java.io.PrintStream outStream, StringConverter c, Object program) {
		String converted = c.convert(program);
		outStream.println(converted);
	}
		
	/** Prints the state of the analysis - transitions and their structures to
	 * all desired outputs.
	 * @param state A collection of transitions.
	 */
	public void printAnalysisState(Object state) {
		for (Iterator i = implementations.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry entry = (Map.Entry) i.next();
			String implementation = (String) entry.getKey();
			
			ImplementationsBundle bundle = (ImplementationsBundle) entry.getValue();
			StringConverter c = bundle.analysisStateConverter;
			if (bundle.outputEnabled && c != null) {
				Logger.print("Producing " + implementation + " output ... ");
				if (bundle.messagesStream == null) { //print everything
					printState(bundle.outputStream, c, state);
				}
				else {
					AnalysisStateConverter stateConverter = (AnalysisStateConverter) c;
					
					// Print the structures without messages to the usual output stream
					printStateNoMessages(bundle.outputStream,stateConverter,state);
					
					// print the structures with messages to the messages stream
					printStateMessagesOnly(bundle.messagesStream,stateConverter, state);
				}
				Logger.println("done"); // done producing the output
			}
		}
	}
	
	
	protected void printState(PrintStream stream, StringConverter c, Object state) {
		c.print(stream, state, "");
		//String str = c.convert(state);
		//stream.println(str);
	}
	
	protected void printStateNoMessages(PrintStream stream, AnalysisStateConverter sc, Object state) {
		sc.print(stream, state, "");
		//String str = sc.convert(state);
		//stream.println(str);
	}
	
	protected void printStateMessagesOnly(PrintStream stream, AnalysisStateConverter sc, Object state) {
		String str = sc.convertMessagesOnly(state);
		stream.println(str);
	}	

}