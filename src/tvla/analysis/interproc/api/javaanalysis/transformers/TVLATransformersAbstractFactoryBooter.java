//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.javaanalysis.transformers;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaVocabulary;
import tvla.api.AbstractTVLAAPI;
import tvla.api.TVLAAnalysisOptions;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaAnalsyisEnvironmentServicesPovider;


public class TVLATransformersAbstractFactoryBooter {
  
  public static ITransformersAbstractFactory genTransformerFactory(
      AbstractTVLAAPI tvlaAPI, 
      ITVLAJavaAnalsyisEnvironmentServicesPovider environmentServicesProvider,
      IJavaVocabulary vocabulary, 
      int transformerCode) {
    
    if (transformerCode != TVLAAnalysisOptions.localHeapsTransformer && 
        transformerCode != TVLAAnalysisOptions.interNoneTransformer && 
        transformerCode != TVLAAnalysisOptions.interPreserveLocalTransformer &&
        transformerCode != TVLAAnalysisOptions.interPartialHeapsTransformer)
      return null;
    
    switch(transformerCode) {
    case TVLAAnalysisOptions.localHeapsTransformer :
      return new InterLocalHeapsTransformersAbstractFactory(
          tvlaAPI, environmentServicesProvider, vocabulary) ;

    case TVLAAnalysisOptions.interNoneTransformer :
      return new InterTopTransformersAbstractFactory(
          tvlaAPI, environmentServicesProvider, vocabulary) ;
    
    case TVLAAnalysisOptions.interPreserveLocalTransformer :
      return new InterPreserveLocalInfoTransformersAbstractFactory(
          tvlaAPI, environmentServicesProvider, vocabulary) ;
      
    case TVLAAnalysisOptions.interPartialHeapsTransformer :
      return new InterPartialHeapsTransformersAbstractFactory(
          tvlaAPI, environmentServicesProvider, vocabulary) ;

    default:
      environmentServicesProvider.getJavaDebuggingServices().tracePrint("TVLATransformersAbstractFactory: unknonw transformer code");
      return null;
    }
  }
}
