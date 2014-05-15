//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.javaanalysis.abstraction.allocationsitesNN;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaVocabulary;
import tvla.analysis.interproc.api.javaanalysis.abstraction.basic.AbstractBasicAbstraction;
import tvla.analysis.interproc.api.javaanalysis.abstraction.basic.BasicAssertionFactory;
import tvla.analysis.interproc.api.javaanalysis.abstraction.basic.BasicMemoryModeler;
import tvla.analysis.interproc.api.tvlaadapter.TVLAAPI;
import tvla.api.TVLAAnalysisOptions;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaAnalsyisEnvironmentServicesPovider;
import tvla.api.ITVLAJavaAnalyzer.ITVLAJavaAssertionFactory;
import tvla.api.ITVLAJavaAnalyzer.ITVLAJavaTVSBuilder;
import tvla.api.ITVLAJavaAnalyzer.ITVLAMemoryModeler;

public class NotNullAllocationSitesAbstraction extends AbstractBasicAbstraction {
  /**
   * Analysis specific components
   */
  protected final ITVLAMemoryModeler memoryModeler;
  protected final ITVLAJavaAssertionFactory assertionFactory;
  protected final NotNullVocabulary vocabulary;
  protected final NotNullTVSBuilder tvsBuilder;
    

  public NotNullAllocationSitesAbstraction(
      TVLAAPI tvlaapi, 
      ITVLAJavaAnalsyisEnvironmentServicesPovider environmentServicesProvider) {
    super(tvlaapi, environmentServicesProvider);
    this.vocabulary =  new NotNullVocabulary(this, this.program, this.client);
    this.assertionFactory = new BasicAssertionFactory(this, this.vocabulary);
    this.tvsBuilder = new NotNullTVSBuilder(this, this.vocabulary, this.program, tvlaapi.getKleene(), this.client);
    this.memoryModeler = new BasicMemoryModeler(this, tvlaapi, vocabulary, this.client, this.program);
  }
  
  public final ITVLAMemoryModeler getMemoryModeler() {
    return memoryModeler;
  }
    
  public final ITVLAJavaTVSBuilder getJavaTVSBuilder() {
    return this.tvsBuilder;
  }
  
  public final IJavaVocabulary getVocabulary() {
    return this.vocabulary;
  }
  
  public final ITVLAJavaAssertionFactory getAssertionFactory() {
    return this.assertionFactory;
  }
  
  // TODO copy this mehtods to alll analyses
  public int getAnalysisCode() {
    return TVLAAnalysisOptions.allocationSitesAbstractionNN;
  }

  public String getAnalysisStr() {
    return TVLAAnalysisOptions.getAnalysisStr(getAnalysisCode());
  }
  
  public String toString() {
    return getAnalysisStr();
  }
}