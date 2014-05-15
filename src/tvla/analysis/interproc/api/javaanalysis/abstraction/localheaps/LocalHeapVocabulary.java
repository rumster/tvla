package tvla.analysis.interproc.api.javaanalysis.abstraction.localheaps;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaAbstraction;
import tvla.analysis.interproc.api.javaanalysis.abstraction.basic.BasicVocabulary;
import tvla.analysis.interproc.api.tvlaadapter.TVLAPredicate;
import tvla.api.ITVLAAPIDebuggingServices;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModelerServices;

/********************************************************
 * Local Heap Predicates
 ********************************************************/ 

class LocalHeapVocabulary extends BasicVocabulary  {    
  public final TVLAPredicate isObj;
  public final TVLAPredicate isOLabel;
  public final TVLAPredicate isCPLabel;
  public final TVLAPredicate lbl;
  public final TVLAPredicate[] lslPreds;
    
  public LocalHeapVocabulary(
      IJavaAbstraction abstraction,
      ITVLAJavaProgramModelerServices program, 
      ITVLAAPIDebuggingServices client) {
    super(abstraction, program, client);
    isObj = new TVLAPredicate("isObj", 1);
    isOLabel = new TVLAPredicate("isOLabel", 1);
    isCPLabel = new TVLAPredicate("isCPLabel", 1);
    lbl = new TVLAPredicate("lbl", 2);
    lslPreds = new TVLAPredicate[] {isObj, isOLabel, isCPLabel, lbl};
  }
  
  public TVLAPredicate[] getInternalPredicates() {
     return lslPreds;
  }
  
  
  public TVLAPredicate getIsCPLabel() {
    return isCPLabel;
  }
  
  public TVLAPredicate getIsObj() {
    return isObj;
  }
  
  public TVLAPredicate getIsOLabel() {
    return isOLabel;
  }
  
  
  public TVLAPredicate getLbl() {
    return lbl;
  }
  
  public IPredicate getPredicate(String id) {
    if (id == null)
      return null;
    
    // TODO imporve stupiut imp
    for (int i=0; i<preds.length; i++)
      if (id.equals(preds[i].getPredId()))
        return preds[i];
    
    return super.getPredicate(id);
  }
}