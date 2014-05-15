package tvla.analysis.interproc.api.javaanalysis.abstraction.localheaps;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaAbstraction;
import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaVocabulary;
import tvla.analysis.interproc.api.javaanalysis.abstraction.basic.BasicTVSBuilder;
import tvla.api.ITVLAAPIDebuggingServices;
import tvla.api.ITVLAKleene;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModelerServices;
import tvla.api.ITVLAKleene.ITVLAKleeneValue;

/********************************************************
 * Local Heap TVS Builder
 * TODO finish this to handle reachabiltiy
 ********************************************************/ 

public class LocalHeapTVSFactory extends BasicTVSBuilder {
  protected final LocalHeapVocabulary  localHeapPredicates;
  public LocalHeapTVSFactory(
      IJavaAbstraction abstraction,
      LocalHeapVocabulary voc, 
      ITVLAJavaProgramModelerServices program,
      ITVLAKleene kleene,
      ITVLAAPIDebuggingServices client) {
    super(abstraction, voc, program, kleene, client);
    // we cache the referecne with "right" type here so there will be no 
    // need to downcast all the time ...
    // TODO is there a better way which avoid double stroring of downcasted pointers? 
    localHeapPredicates = voc;
  }
  
  
  public boolean setIsObj(int node, ITVLAKleeneValue val) {
    String isObj = localHeapPredicates.getIsObj().getPredId();
    return setUnaryPredicate(isObj, node, val);
  }
  
  public boolean setIsOLabel(int node, ITVLAKleeneValue val) {
    String olb = localHeapPredicates.getIsOLabel().getPredId();
    return setUnaryPredicate(olb, node, val);			
  }
  
  public boolean setIsCPLabel(int node, ITVLAKleeneValue val) {
    String cplb = localHeapPredicates.getIsCPLabel().getPredId();
    return setUnaryPredicate(cplb, node, val);			
  }	
    
  public boolean setLbl(int from, int to, ITVLAKleeneValue val) { 
    String lbl = localHeapPredicates.getLbl().getPredId();
    return setBinaryPredicate(lbl, from, to, val);			
  }


  public IJavaVocabulary getVocabulary() {
    return localHeapPredicates;
  }  
}