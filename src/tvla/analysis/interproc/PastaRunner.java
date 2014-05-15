/*
 * File: PastaRunner.java 
 * Created on: 04/11/2004
 */

package tvla.analysis.interproc;

import java.util.Calendar;
import java.util.Collection;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.Engine;
import tvla.analysis.interproc.semantics.AuxiliaryPredicates;
import tvla.core.HighLevelTVS;
import tvla.core.TVSFactory;
import tvla.io.IOFacade;
import tvla.language.PTS.PTSAST;
import tvla.language.PTS.PTSParser;
import tvla.language.TVS.TVSParser;
import tvla.util.Logger;
import tvla.util.ProgramProperties;
import tvla.util.StringUtils;


/** 
 * @author maon
 */
public class PastaRunner {
	static private final int MAX_EVENTS = -1; // if -1, works until a fixpoint is reached.
	/**
	 * 
	 */
	public PastaRunner() {
	}

	public static void  go(String programName, String searchPath, String inputFile) {
		try {
			setupOutput();
			AuxiliaryPredicates.init();
			
			InterProcEngine eng = (InterProcEngine) Engine.activeEngine;
			Calendar cal = new java.util.GregorianCalendar();
			cal.setTimeInMillis(System.currentTimeMillis());
			if (!AnalysisStatus.terse) 
				System.out.println("PASTA analysis starts at " + cal.getTime().toString());
			cal = null;
			
			AnalysisStatus.getActiveStatus().updateStatus();
			if (!AnalysisStatus.terse) 
				System.out.println(StringUtils.newLine + "PASTA (PreEng) Loads analysis specification " + programName);

//			PTSAST ptsFile = null;
//			try {
//				ptsFile = PTSParser.configure(programName, searchPath);
//				ptsFile.compileAll();
//			} catch (Exception e) {
//				System.err.println("Failed to compile program ");
//				System.err.println(e);
//				System.exit(1);
//			}
	
			readProgram(programName, searchPath);
			
			IOFacade.instance().printProgram(eng.getPrintableProgram());
			AnalysisStatus.getActiveStatus().updateStatus();
//			ptsFile = null; // opportunity for GC
			
			if (!AnalysisStatus.terse)
				Logger.print("Reading TVS files ... ");

			TVSFactory.getInstance().init();
			Collection initial = TVSParser.readStructures(inputFile);

			Logger.println();
			Logger.println("Load Memory Statistics");
			AnalysisStatus.getActiveStatus().printMemoryStatistics();
			AnalysisStatus.getActiveStatus().resetMemoryStatistics();
			System.gc();
			AnalysisStatus.loadTimer.stop();					
			
			if (!AnalysisStatus.terse) 
				System.out.println(StringUtils.newLine + "Starting analysis ...");
					
			/////////////////////////////
			/// Running The Analysis  ///
			/////////////////////////////
			int maxIterations = ProgramProperties.getIntProperty("tvla.pasta.maxEvents", MAX_EVENTS);
			
			// Added
			HighLevelTVS.advancedCoerce.coerceInitial(initial);
			
			eng.evaluate(initial, maxIterations);
						
			if (!AnalysisStatus.terse) {
				System.out.println(StringUtils.newLine + "Analysis done");
			}
			
			eng.printResults(IOFacade.instance());	
			
			//eng.saveResults(IOFacade.instance(), XML.instance());
			
			
			if (!AnalysisStatus.terse) {
				System.out.println(StringUtils.newLine + "All tasks completed");
			}
		}
		catch(Throwable t) {
			t.printStackTrace(System.err);
			System.err.println(t.getClass().toString());
			System.err.println(
				"Execption occured [type = " + t.getClass().getName() + "] ");
			
			System.err.println(
				" [msg = " + (t.getMessage() != null ?  t.getMessage() : "no message") +"]");
		
			Calendar cal = new java.util.GregorianCalendar();
			cal.setTimeInMillis(System.currentTimeMillis());
			System.err.println(" [crashed at  = " + cal.getTime().toString() +" ]");
		}
		
		
	}
	
	
	public static void  readProgram(String programName, String searchPath) {
		PTSAST ptsFile = null;
		try {
			ptsFile = PTSParser.configure(programName, searchPath);
			ptsFile.compileAll();
		} catch (Exception e) {
			System.err.println("Failed to compile program ");
			System.err.println(e);
			System.exit(1);
		}
		ptsFile = null; // opportunity for GC
	}
	
	private static void setupOutput() {
		// shutdown tvs & tr outputs  
//		if (ProgramProperties.getBooleanProperty("tvla.tvs.enabled", true)) {
//			System.err.println("PASTA Does not support tvs output - setting option off");
//			ProgramProperties.setBooleanProperty("tvla.tvs.enabled", false);
//		}
		
		if (ProgramProperties.getBooleanProperty("tvla.tr.enabled", true))	{
			System.err.println("PASTA Does not support tr output - setting option off");
			ProgramProperties.setBooleanProperty("tvla.tr.enabled", false);
		}
		
//		if (!ProgramProperties.getBooleanProperty("tvla.output.multipleOutputFiles", true)) {
//			System.err.println("PASTA requires multiple output files - setting option on");
//			ProgramProperties.setBooleanProperty("tvla.output.multipleOutputFiles", true);
//		}
	}
//	private static void setupOutput(String progName) {
//		String progDir = "";
//		int lastSlash = progName.lastIndexOf('\\');
//		int lastBackslash = progName.lastIndexOf('/');
//		int last = (lastSlash > lastBackslash) ? lastSlash : lastBackslash;
//		if ( 0 < last)  {
//			assert (last < progName.length());
//			progDir = progName.substring(0,last+1);			
//			progName = progName.substring(last+1);
//		}		
//		ProgramProperties.setProperty("tvla.output.progName", progName);
//		
//		String subDirectory = ProgramProperties.getProperty("tvla.output.subDirectory", "null");
//		if (subDirectory.equals("null")) 
//			subDirectory = "";
//		
//		String absoluteDirectory = ProgramProperties.getProperty("tvla.output.absoluteDirectory", "null");
//		if (absoluteDirectory == null || absoluteDirectory.equals("null"))
//			absoluteDirectory = progDir;
//		
//		String outputDirectory = absoluteDirectory + subDirectory;
//		ProgramProperties.setProperty("tvla.output.outputDirectory", outputDirectory);
//		
//		assert(progName != null);
//		String root = ProgramProperties.getProperty("tvla.output.root", "null");
//		if (root.equals("null"))
//			ProgramProperties.setProperty("tvla.output.root", progName);
//		
//		
//		// After reinsertion - temporary - 
//		ProgramProperties.setProperty("tvla.dot.outputFile", "null");
//	}

}
