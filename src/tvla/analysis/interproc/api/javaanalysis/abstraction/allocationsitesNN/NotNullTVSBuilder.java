//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.javaanalysis.abstraction.allocationsitesNN;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaAbstraction;
import tvla.analysis.interproc.api.javaanalysis.abstraction.basic.BasicTVSBuilder;
import tvla.api.ITVLAAPIDebuggingServices;
import tvla.api.ITVLAKleene;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModelerServices;
import tvla.api.ITVLAKleene.ITVLAKleeneValue;

public class NotNullTVSBuilder extends BasicTVSBuilder { 
  public NotNullTVSBuilder(
      IJavaAbstraction abstraction,
      NotNullVocabulary voc, 
      ITVLAJavaProgramModelerServices program, 
      ITVLAKleene kleene, 
      ITVLAAPIDebuggingServices client) {
    super(abstraction, voc, program, kleene, client);
  }
  
  public void newTVS(Object method, int numOfNodes) {
    super.newTVS(numOfNodes);
   }
  
  
  public int addNode() {
    int id = super.addNode();
    if (id < 0)
      return id;
    
    boolean success = setPossibleVarCutPoint(id, kleene.unknownVal());
    if (success)
      return id;
    
    return -1;  
  }
  
  public boolean setInstanceReferenceField(Object field, int from, int to, ITVLAKleeneValue val) {
    boolean success = super.setInstanceReferenceField(field, from, to, val);
    if (success) 
      success = setNotNullField(field, from, to, val);
    
    return success;
  }
  
  
  public boolean setInstanceArrayField(Object arrayField, int from, int to, ITVLAKleeneValue val) {
    boolean success = super.setInstanceArrayField(arrayField, from, to, val);
    if (success) 
      success = setNotNullField(arrayField, from, to, val);
    
    return success;
  }
  
  
  protected boolean setNotNullField(Object field, int from, int to, ITVLAKleeneValue val) {
    String nonNullFldPred = ((NotNullVocabulary) this.vocabualry).notNullInstanceFieldToPred(field);
    if (nonNullFldPred == null)
      return false;
    
    boolean  success = setUnaryPredicate(nonNullFldPred, from, val);
    
    return success;
  }
  
  public boolean setRefLocal(Object method, int indx, int node, ITVLAKleeneValue val) {
    boolean success = super.setRefLocal(method, indx, node, val); 
    if (success) 
      success = setNotNullLocal(method, indx, node, val);
    
    return success;                      
  }
  
  protected boolean setNotNullLocal(Object method, int indx, int node, ITVLAKleeneValue val) {
    String nonNullLocalPred = ((NotNullVocabulary) this.vocabualry).notNullLocalToPred(method, indx);
    if (nonNullLocalPred == null)
      return false;
    
    boolean  success = setUnaryPredicate(nonNullLocalPred, node, val);
    
    return success;
  }
  
  
  public boolean setPossibleVarCutPoint(int node, ITVLAKleeneValue val) {
    boolean  success = true;
    /*
     * No ned to set the "possible" cutpoints
     * This information is computed during the analysis
     * 
    if (method != null && ! program.methodIsJVMMain(method)) {
      String allocPred = ((NotNullVocabulary) this.vocabualry).ptpV.getPredId();
      
      success = setUnaryPredicate(allocPred, node, val);
    }
    */
    return success;    
    
  }
}
