//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.javaanalysis.abstraction.localheaps;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaVocabulary;
import tvla.analysis.interproc.api.javaanalysis.abstraction.basic.AbstractBasicAbstraction;
import tvla.analysis.interproc.api.javaanalysis.abstraction.basic.BasicAssertionFactory;
import tvla.analysis.interproc.api.javaanalysis.abstraction.basic.BasicMemoryModeler;
import tvla.analysis.interproc.api.tvlaadapter.TVLAAPI;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaAnalsyisEnvironmentServicesPovider;
import tvla.api.ITVLAJavaAnalyzer.ITVLAJavaAssertionFactory;
import tvla.api.ITVLAJavaAnalyzer.ITVLAJavaTVSBuilder;
import tvla.api.ITVLAJavaAnalyzer.ITVLAMemoryModeler;

public class LocalHeapAbstraction extends AbstractBasicAbstraction  {  
  /**
   * Analysis specific components
   */
  protected final ITVLAJavaAssertionFactory assertionFactory;
  protected final ITVLAMemoryModeler memoryModeler;

  /** Local heap unique predicats */
  protected final LocalHeapVocabulary  localHeapPredicates;
  /** Local heap unique TVS Factory */
  protected final LocalHeapTVSFactory localHeapTVSBuilder;
  
  
  public LocalHeapAbstraction(
      TVLAAPI tvlaapi, 
      ITVLAJavaAnalsyisEnvironmentServicesPovider environmentServicesProvider) {
    super(tvlaapi, environmentServicesProvider);
    this.localHeapPredicates =  new LocalHeapVocabulary(this, this.program, this.client);
    this.localHeapTVSBuilder = new LocalHeapTVSFactory(this, localHeapPredicates, this.program, tvlaapi.getKleene(), this.client);
    this.assertionFactory = new BasicAssertionFactory(this, localHeapPredicates);
    this.memoryModeler = new BasicMemoryModeler(this, tvlaapi, localHeapPredicates, this.client, this.program);
  }
  
  public final ITVLAMemoryModeler getMemoryModeler() {
    return memoryModeler;
  }
    
  public final ITVLAJavaTVSBuilder getJavaTVSBuilder() {
    return this.localHeapTVSBuilder;
  }
  
  public final IJavaVocabulary getVocabulary() {
    return this.localHeapPredicates;
  }
  
  public final ITVLAJavaAssertionFactory getAssertionFactory() {
    return this.assertionFactory;
  }

}