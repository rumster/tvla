package tvla.io;

//import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;

import tvla.analysis.interproc.transitionsystem.method.MethodTS;
import tvla.analysis.interproc.transitionsystem.method.TSNode;
import tvla.util.ProgramProperties;
import tvla.util.SingleSet;

//import tvla.io.IOFacade.ImplementationsBundle;


/** A class for directing PASTA output.
 * @author maon (based on Roman Manevich.)
 */
public class InterProcIOFacade extends  IOFacade  {
	protected boolean multiFiles = ProgramProperties.getBooleanProperty("tvla.output.multipleOutputFiles", true);

	InterProcIOFacade() {
		super();
	}
	
	
	/** PrintStructure hook.
	 */
	public void printStructure(Object structure, String header) {
		super.printStructure(structure, header, "");
	}

	/** PrintProgram hook
	 */
	protected void printProgram(java.io.PrintStream bundleStream, StringConverter c, Object program) {
		Collection cfgs = (Collection) program;
		Iterator cfgItr = cfgs.iterator();
		
		while (cfgItr.hasNext()) {
			Object cfg = cfgItr.next();
			String convertedCFG = c.convert(cfg);
			bundleStream.println(convertedCFG);
		}
	}
	
	/*
	 * PrintAnalysisState 
	 */
	/** Prints the state of the analysis - the structures of a given method to
	 * all desired outputs.
	 * @param state A MethodTS
	 */
	public void printAnalysisState(Object state) {
		printResults((MethodTS) state);
	}

//	public void printResults(PrintStream  outputStream, PrintStream messagesStream, LocationConverter c, MethodTS mts) {		
	public void printResults(MethodTS mts) {
		String outStreamName = genValidStreamName(mts.getSig());
		String prefix = null;
		if (multiFiles) 
			redirectOutput(outStreamName);
		else 
			prefix = outStreamName + "@";
			
		printBanner("Analysis Results for method " + mts.getSig());
		//+ " " +  " to file " + outStreamName);
		printProgram(new SingleSet(true,mts));
		
		PrintNodes(mts, prefix);
	}
	
		
	public void PrintNodes(MethodTS mts, String prefix) {
		String fixpointBanner = "Fixpoint (using propagated strucutres)";
		printBanner(fixpointBanner);

		mts.getEntrySite().setLabelPrefix(prefix);
		mts.getExitSite().setLabelPrefix(prefix);
		
		if (mts.getEntrySite().getShouldPrint()) 
			printLocation(mts.getEntrySite());
		if (mts.getExitSite().getShouldPrint()) 
			printLocation(mts.getExitSite());
		
		mts.getEntrySite().setLabelPrefix(null);
		mts.getExitSite().setLabelPrefix(null);

		Iterator nodeItr = mts.getCFG().DFSIterator();
		assert(nodeItr != null);
		while (nodeItr.hasNext()) {
			TSNode node = (TSNode) nodeItr.next();
			if (node == mts.getEntrySite() || node == mts.getExitSite())
				continue;
			
			if (!node.getShouldPrint()) 
				continue;
			
			node.setLabelPrefix(prefix);
			printLocation(node);
			node.setLabelPrefix(null);
		}
	}
	

	



	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/*
	protected void printConstraintBreach(Object origStructure, 
             Object focusedStructure,
			 Object updateStrucutre,
			 Object ConstraintBreach,
			 String header, 
			 String breachedHeader) {
		for (Iterator i = implementations.values().iterator(); i.hasNext(); ) {
			ImplementationsBundle bundle = (ImplementationsBundle) i.next();
			StringConverter c = bundle.structureConverter;
			if (bundle.outputEnabled && c != null) {
				if (bundle.breacesStream != null && 
				header.indexOf("Constraint Breached") >=0) {
				String converted = c.convert(structure, breachedHeader);
				bundle.breacesStream.println(converted);
			}
			else {
				String converted = c.convert(structure, header);
				bundle.outputStream.println(converted);
			}
		}
	}
	*/

	
}