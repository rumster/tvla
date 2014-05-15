//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.javaanalysis.abstraction;

import tvla.analysis.interproc.api.javaanalysis.abstraction.allocationsites.AllocationSitesAbstraction;
import tvla.analysis.interproc.api.javaanalysis.abstraction.allocationsitesNN.NotNullAllocationSitesAbstraction;
import tvla.analysis.interproc.api.javaanalysis.abstraction.localheaps.LocalHeapAbstraction;
import tvla.analysis.interproc.api.javaanalysis.abstraction.typepiles.TypePilesAbstraction;
import tvla.analysis.interproc.api.tvlaadapter.TVLAAPI;
import tvla.analysis.interproc.api.utils.TVLAAPIDebugControl;
import tvla.analysis.interproc.api.utils.TVLAAPITrace;
import tvla.api.TVLAAnalysisOptions;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaAnalsyisEnvironmentServicesPovider;


/**
 * Generates the abstarction governing the analysis
 * Allows for abstraction specific code to:
 *  - Use additional predicates
 *  - Construct TVSs
 *  - Create assertions
 * @author maon
 *
 */

public abstract  class AbstractionFactory {
  public final static int DEBUG_LEVEL = TVLAAPIDebugControl.getDebugLevel(4);
  
  private static IJavaAbstraction theAbstraction;
  
  public static IJavaAbstraction genAbstraction(
      TVLAAPI tvlaapi,
      ITVLAJavaAnalsyisEnvironmentServicesPovider environmentServicesProvider,
      int abstractionCode) {
    
    if (theAbstraction != null)
      return theAbstraction;
    
    
    switch(abstractionCode) {
    case TVLAAnalysisOptions.allocationSitesAbstractionNN:
      if (0 < DEBUG_LEVEL)
        TVLAAPITrace.tracePrintln("AbstractionFactory: allocationSitesAbstractionNN code");
      theAbstraction = new NotNullAllocationSitesAbstraction(tvlaapi, environmentServicesProvider);
      break;
      
    case TVLAAnalysisOptions.allocationSitesAbstraction:  
      if (0 < DEBUG_LEVEL)
        TVLAAPITrace.tracePrintln("AbstractionFactory: allocationSitesAbstraction code");
      theAbstraction = new AllocationSitesAbstraction(tvlaapi, environmentServicesProvider);
      break;

    case TVLAAnalysisOptions.typePilesAbstraction:
      if (0 < DEBUG_LEVEL)
        TVLAAPITrace.tracePrintln("AbstractionFactory: typePilesAbstraction code");
      theAbstraction = new TypePilesAbstraction(tvlaapi, environmentServicesProvider);
      break;

    case TVLAAnalysisOptions.localHeapsAbstraction:
      if (0 < DEBUG_LEVEL)
        TVLAAPITrace.tracePrintln("AbstractionFactory: localHeapsAbstraction code");
      theAbstraction = new LocalHeapAbstraction(tvlaapi, environmentServicesProvider);
      break;

    default:
      environmentServicesProvider.getJavaDebuggingServices().tracePrintln("AbstractionFactory: unknown abstraction code");
      return null;
    }
    
    tvlaapi.setVocabulary(theAbstraction.getVocabulary());
    
    return theAbstraction;
  }
  
  
  public static String consistent(int abstractionCode) {
    if (abstractionCode  != TVLAAnalysisOptions.allocationSitesAbstraction && 
        abstractionCode != TVLAAnalysisOptions.typePilesAbstraction  &&
        abstractionCode != TVLAAnalysisOptions.localHeapsAbstraction) 
      return "unknown abstraction code";
   
    return null;
  }
}
