//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.javaanalysis.abstraction.allocationsitesNN;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaAbstraction;
import tvla.analysis.interproc.api.javaanalysis.abstraction.basic.BasicVocabulary;
import tvla.analysis.interproc.api.tvlaadapter.TVLAPredicate;
import tvla.analysis.interproc.api.utils.TVLAAPIAssert;
import tvla.api.ITVLAAPIDebuggingServices;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModelerServices;

public class NotNullVocabulary extends BasicVocabulary {

  public NotNullVocabulary(
      IJavaAbstraction abstraction,
      ITVLAJavaProgramModelerServices program, 
      ITVLAAPIDebuggingServices client) {
    super(abstraction, program, client);
  }

  
  
  /***********************************************
   * NotNullness of Instance Fields 
   ***********************************************/
  
  public String notNullInstanceFieldToPred(Object theInstanceField) {
    client.debugAssert(! program.fieldIsStatic(theInstanceField));

    if (TVLAAPIAssert.ASSERT) {
      // TODO FIXME so we can get the type
      // TVLAAPIAssert.debugAssert(program.fieldIsArray(theInstanceField) || program.fieldIsReference(theInstanceField));
    }
    
    String fldPred = instanceFieldToPred(theInstanceField);
    if (fldPred == null)
      return null;   
    
    return ("nnF[" + fldPred + "]");
  }  

  public String notNullLocalToPred(Object method, int indx) {
     if (TVLAAPIAssert.ASSERT) {
      // TODO FIXME so we can get the type
      // TVLAAPIAssert.debugAssert(program.methodLocalTypeIsReference(method, indx));
       
       // HACK FIXME with type system in the domo side
       String arrayPred = arrayLocalToPred(method, indx);
       String refPred = refLocalToPred(method, indx);
       TVLAAPIAssert.debugAssert(refPred.equals(arrayPred));
    }
    
    String varPred = refLocalToPred(method, indx);
    
    if (varPred == null)
      return null;
    
    return ("nnV[" + varPred + "]");
  }  
  
  /***************************************************
   * Dictionary
   ***************************************************/
  
  public final TVLAPredicate ptpV = new TVLAPredicate(NotNullConstants.pointedToByPendingLocal, 1);              
  public String allocated() {
    return ptpV.getPredId();
  }         

  
  public IPredicate getPredicate(String id) {
    if (id == null)
      return null;
    
    if (id.equals(ptpV.getPredId()))
      return ptpV;
    
    return super.getPredicate(id);
  }       

}
