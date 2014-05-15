//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.javaanalysis.transformers;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaVocabulary;
import tvla.analysis.interproc.api.utils.PredicateIndependantNullaryCombiners;
import tvla.api.AbstractTVLAAPI;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaAnalsyisEnvironmentServicesPovider;
import tvla.core.Combine.INullaryCombiner;

/**
 * Temporary use this class to centralize th combiners
 * @author maon
 *
 */
public abstract class AbstratInterTransformersAbstractFactory extends AbstractIntraTransformersAbstractFactory {
  
  // TODO take local values of booleans from the caller
  protected static INullaryCombiner 
  callToEntryStagedCombiner =  trackingPrimitives  ? PredicateIndependantNullaryCombiners.getTop() :  PredicateIndependantNullaryCombiners.getZero();
 
  /**
   * combines the caller call site (inUc) with the caller pointsTo (inUx)
   * TODO take local values of booleans from the caller
   */
  protected static INullaryCombiner 
  callAndExitStagedCombiner =  trackingPrimitives  ? PredicateIndependantNullaryCombiners.getTop() :  PredicateIndependantNullaryCombiners.getProejctFirstNullaries();
  

  /**
   * combines the caller call site (inUc) with the caller pointsTo (inUx)
   * TODO take local values of booleans from the caller
   * TODO Need to forget due to static boolean fields.
   * TODO select the values of local boleans from the caller
   */
  protected static INullaryCombiner 
  callAndExitCombiner = trackingPrimitives  ? PredicateIndependantNullaryCombiners.getTop() : PredicateIndependantNullaryCombiners.getProejctFirstNullaries();
  
  
  public AbstratInterTransformersAbstractFactory(
      AbstractTVLAAPI tvlaAPI, 
      ITVLAJavaAnalsyisEnvironmentServicesPovider environmentServicesProvider,
      IJavaVocabulary vocabulary) {
    super(tvlaAPI, environmentServicesProvider, vocabulary);
  }
      
              
}
