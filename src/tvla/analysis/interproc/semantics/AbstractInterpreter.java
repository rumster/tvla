/*
 * File: AbstractInterpreter.java 
 * Created on: 16/10/2004
 */

package tvla.analysis.interproc.semantics;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.interproc.Method;
import tvla.core.Node;
import tvla.core.TVS;
import tvla.core.Combine.INullaryCombiner;
import tvla.core.base.BaseTVS;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.transitionSystem.PrintableProgramLocation;
import tvla.util.Filter;
import tvla.util.Logger;
import tvla.util.Timer;
//import tvla.language.XML.*;

// FIXME uses engine for the implmentation of apply - ugly but quick.
// TODO move the code of apply into this class. 

/** An interface class with TVLA
 * + Applies actions to high level TVS. 
 * + combines the structures at a return node
 * 
 * @author maon
 */

public class AbstractInterpreter {
	private final AnalysisStatus totalStatus;
	private final Applier intraApplier;
	private final Applier guardApplier;
	private final Applier callApplier;
	private final Applier retApplier;
	private final Timer totalTimer;
	private final Timer actionTimer;
    private final INullaryCombiner orNullaries;

    
    
	/**
	 * 
	 */
	public AbstractInterpreter(
			AnalysisStatus totalStatus,
			Applier intraApplier,
			Applier guardApplier,
			Applier callApplier,
			Applier retApplier) {
		super();
		assert(totalStatus != null);
		assert(intraApplier != null);
		assert(guardApplier != null);
		assert(callApplier != null);
		assert(retApplier != null);

		this.intraApplier = intraApplier;
		this.guardApplier = guardApplier;
		this.callApplier = callApplier;
		this.retApplier = retApplier;	
		this.totalStatus = totalStatus;
		this.totalTimer = new Timer();
		this.actionTimer = new Timer();
		
		assert(intraApplier.getTotalAnalysisStatus() == totalStatus);				
		assert(guardApplier.getTotalAnalysisStatus() == totalStatus);
		assert(callApplier.getTotalAnalysisStatus() == totalStatus);
		assert(retApplier.getTotalAnalysisStatus() == totalStatus);
        
        orNullaries = 
            new INullaryCombiner () {
            public Kleene combineNumarryPredicate(Predicate pred, Kleene firstVal, Kleene secondVal) {
              Kleene val = Kleene.or(firstVal,secondVal);
              return val;
            }
          };
	}
	
	
	public Collection applyIntra(
			Method mtd,
			PrintableProgramLocation processedLocation,
			ActionInstance action,
			TVS tvs,
			Map msgs){

		actionTimer.start();
		intraApplier.getAnalysisStatus().startTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);
		//if (processedLocation.label().compareTo("L_l547_g161") == 0) {
		//	System.out.print("Got there!");
		//}
		
		Collection res =  intraApplier.apply(
				action, 
				tvs, 
				processedLocation, 
				msgs);
		intraApplier.getAnalysisStatus().stopTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);		
		actionTimer.stop();
		
		return res;
	}
	
	public Collection applyCall(
			Method mtd,
			PrintableProgramLocation processedLocation,
			ActionInstance action,
			TVS callTVS,
			Map msgs){
		
		actionTimer.start();
		callApplier.getAnalysisStatus().startTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);				
		Collection res = callApplier.apply(
				action, 
				callTVS, 
				processedLocation,
				msgs);
		callApplier.getAnalysisStatus().stopTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);				
		actionTimer.stop();

		
		return res;
	}

	public Collection applyBinary(
			Method mtd,
			PrintableProgramLocation processedLocation,
			ActionInstance action,
			TVS tvsCall,
			TVS tvsExit,
			Map msgs){

		actionTimer.start();
		retApplier.getAnalysisStatus().startTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);	
		
		TVS copiedTVSCall = tvsCall.copy();
		TVS copiedTVSExit = tvsExit.copy();
		
		copiedTVSCall.setAll(AuxiliaryPredicates.inUc, Kleene.trueKleene);
		copiedTVSExit.setAll(AuxiliaryPredicates.inUx, Kleene.trueKleene);		
		
		
		TVS combinedTVS = TVS.combine(orNullaries, copiedTVSCall,copiedTVSExit);
			
		Collection res = retApplier.apply(
				action, 
				combinedTVS, 
				processedLocation,
				msgs);
		
		assert(res != null);
		//tvsCall.setAll(Vocabulary.inUc, Kleene.falseKleene);
		//tvsExit.setAll(Vocabulary.inUx, Kleene.falseKleene);
		
		Iterator resItr = res.iterator();
		while (resItr.hasNext()) {
		    final TVS tvs = (TVS) resItr.next();

			tvs.setAll(AuxiliaryPredicates.inUc, Kleene.falseKleene);
			tvs.setAll(AuxiliaryPredicates.inUx, Kleene.falseKleene);
            tvs.filterNodes(new Filter<Node>() {
                public boolean accepts(Node node) {
                    return tvs.eval(AuxiliaryPredicates.kill, node) != Kleene.trueKleene;
                }
            });
 		}
		
		retApplier.getAnalysisStatus().stopTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);				
		actionTimer.stop();

		return res;
	}

	public Collection applyGuard(
			Method mtd,
			PrintableProgramLocation processedLocation,
			ActionInstance action,
			TVS tvs,
			Map msgs){

		actionTimer.start();
		guardApplier.getAnalysisStatus().startTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);
		Collection res = guardApplier.apply(
				action, 
				tvs, 
				processedLocation, 
				msgs);
		guardApplier.getAnalysisStatus().stopTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);
		actionTimer.stop();

		
		return res;
	}

	public Collection emptyTVSCollection() {
		return new ArrayList();
	}
	

	//////////////////////////////////////////////
	///                Status                  ///  
	//////////////////////////////////////////////

	public void startAnalysis() {
		totalStatus.updateStatus();
		totalTimer.start();
		totalStatus.startTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);
	}
	
	public void stopAnalysis() {
		totalStatus.stopTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);		
		totalTimer.stop();
		totalStatus.updateStatus();
	}

	public AnalysisStatus getTotalStatus() {
		return this.totalStatus;
	}

	public void printStatistics() {
/*		Logger.println();
		Logger.println("Intra Statistics");
		intraApplier.getAnalysisStatus().printStatistics();
		Logger.println();
		Logger.println("Guard Statistics");
		guardApplier.getAnalysisStatus().printStatistics();
		Logger.println();
		Logger.println("Call Statistics");
		callApplier.getAnalysisStatus().printStatistics();
		Logger.println();
		Logger.println("Return Statistics");
		retApplier.getAnalysisStatus().printStatistics();
		Logger.println();
*/		
		assert(intraApplier.getAnalysisStatus() == callApplier.getAnalysisStatus());
		assert(intraApplier.getAnalysisStatus() == retApplier.getAnalysisStatus());
		assert(intraApplier.getAnalysisStatus() == guardApplier.getAnalysisStatus());
		
		Logger.println();
		Logger.println("Detailed Statistics");
		intraApplier.getAnalysisStatus().printStatistics();
		
		Logger.println();
		Logger.println("Action Statistics");
		Logger.println("Action Time " + actionTimer.toString());

		Logger.println();
		Logger.println("Total Statistics");
		Logger.println("Total Time " + totalTimer.toString());
		
	}


	/////////////////////////////////////////////////////
	///                  Print Anlaysis               ///
	/////////////////////////////////////////////////////
/*
	public void saveAnalysis(TVLAIO io, XML xml) {
		String outStream = null;
		java.io.Writer writeTo = null;
		String fileName = null;
		
		fileName = io.genValidStreamName("vocabulary");
		writeTo = io.getFileWriter("xml", fileName, "xml"); 	
		xml.saveVocabulary(writeTo);
		try {
			writeTo.close();
		}
		catch (Exception e) {
			throw new Error(e.getMessage());
		}

		
		
		fileName = io.genValidStreamName("constraints");
		writeTo = io.getFileWriter("xml", fileName, "xml"); 	
		xml.saveConstraints(writeTo);
		try {
			writeTo.close();
		}
		catch (Exception e) {
			throw new Error(e.getMessage());
		}

	}
*/	
	
	/////////////////////////////////////////////////////
	///                  Internal Stuff               ///
	/////////////////////////////////////////////////////

	/**
	 * Creates a new strucutre which is a combinaion 
	 * of the two strucutres. callS and exitS are not modified.
	 * In the combined structure, all the nodes that "came from"
	 * the callS strucutre should a value 1 for the inUc predicate and 
	 * a value 0 for the in Ux predicates. The reverse for the nodes
	 *  that "came from" exitS. 
	 * 
	 * @pre the active predicate is 1 for all the nodes in callS and exitS.
	 * @param tvsCall the strucutre at the callSite. 
	 * @param tvsExit the strucutre at the exitSite
	 * @return the combined structure.
	 */
//	private TVS combine(TVS tvsCall, TVS tvsExit){
//		TVS combinedTVS = tvsCall;
//		return combinedTVS;
//	}
}
