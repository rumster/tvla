//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.javaanalysis.abstraction.basic;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaAbstraction;
import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaTVSBuilder;
import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaVocabulary;
import tvla.analysis.interproc.api.tvlaadapter.abstraction.TVSBuilder;
import tvla.analysis.interproc.api.utils.TVLAAPIAssert;
import tvla.api.ITVLAAPIDebuggingServices;
import tvla.api.ITVLAKleene;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModelerServices;
import tvla.api.ITVLAKleene.ITVLAKleeneValue;

public  class BasicTVSBuilder extends TVSBuilder implements IJavaTVSBuilder {
  protected final IJavaVocabulary vocabualry;
  protected final ITVLAJavaProgramModelerServices program;
  protected final ITVLAKleene kleene;
  protected final ITVLAAPIDebuggingServices      client;
 
  /**
   * The govering abstraction
   */  
  protected final IJavaAbstraction abstraction; 


  
  /**
   * @param abstraction1
   */
  public BasicTVSBuilder(
      IJavaAbstraction abstraction,
      IJavaVocabulary voc, 
      ITVLAJavaProgramModelerServices program,
      ITVLAKleene kleene,
      ITVLAAPIDebuggingServices client) {
    super(voc);
    this.vocabualry = voc;
    this.program = program;
    this.client = client;
    this.kleene = kleene;
    this.abstraction = abstraction;
  }
  
  public IJavaAbstraction getAbstraction() {
    return abstraction;
  }
  
  
  public int addNode() {
    int id = super.addNode();
    if (id < 0)
      return id;
    
    boolean success = setSM(id, kleene.unknownVal());
    if (success)
      return id;
    
    return -1;
  }
    
  public boolean setClass(Object klass, int node, ITVLAKleeneValue val) {
    client.debugAssert(program.representsClass(klass));
    
    String classPred = vocabualry.classToPred(klass);
    return setUnaryPredicate(classPred, node, val);
  }
  
  public boolean setArrayClass(Object arrayClass, int node, ITVLAKleeneValue val) {
    client.debugAssert(program.representsArrayClass(arrayClass));
    
    String arrayClassPred = vocabualry.arrayClassToPred(arrayClass);
    String isArray = vocabualry.isArray();
    boolean successSetClass =  setUnaryPredicate(arrayClassPred , node, val);
    if (successSetClass) {
      boolean successSetArray =  setUnaryPredicate(isArray , node, kleene.trueVal());
      return successSetArray;
    }
    return false;
  }
  
  public boolean setArrayContains(int arrayNode, int elemNode, ITVLAKleeneValue val) {
    String arrayElementPred = vocabualry.arrayContains();
    return setBinaryPredicate(arrayElementPred, arrayNode, elemNode, val);
  }
  
  public boolean setStaticReferenceField(Object field, int node, ITVLAKleeneValue val) {
    client.debugAssert(program.representsField(field));
    client.debugAssert(program.fieldIsReference(field));
    client.debugAssert(program.fieldIsStatic(field));
    
    String fieldPred = vocabualry.staticFieldToPred(field);
    return setUnaryPredicate(fieldPred, node, val);			
  }
  
  public boolean setStaticArrayField(Object field, int node, ITVLAKleeneValue val) {
    client.debugAssert(program.representsField(field));
    client.debugAssert(program.fieldIsArray(field));
    client.debugAssert(program.fieldIsStatic(field));
    
    String fieldPred = vocabualry.arrayStaticFieldToPred(field);
    return setUnaryPredicate(fieldPred, node, val);         
  }
  
  public boolean setInstanceReferenceField(Object field, int from, int to, ITVLAKleeneValue val) {
    client.debugAssert(program.representsField(field));
    if (!program.fieldIsReference(field)) {
      System.err.println("Problem " + field);
    }
    
    client.debugAssert(program.fieldIsReference(field));
    client.debugAssert(! program.fieldIsStatic(field));
    
    String fieldPred = vocabualry.instanceFieldToPred(field);
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(fieldPred != null, " got null for field predicate " + field);
    
    boolean ret = setBinaryPredicate(fieldPred, from, to, val);			
    if (TVLAAPIAssert.ASSERT) {
      if (!ret)
        System.err.println("  failed to set field predicate " + field + " from " + from + " to " + to);
      
      TVLAAPIAssert.debugAssert(ret, " faield to set field predicate " + field + " from " + from + " to " + to);
    }
    
    return ret;
  }
  
  public boolean setInstanceArrayField(Object arrayField, int from, int to, ITVLAKleeneValue val) {
    client.debugAssert(program.representsField(arrayField));
    if (!program.fieldIsArray(arrayField)) {
      System.err.println("Problem " + arrayField);
    }
    
    client.debugAssert(program.fieldIsArray(arrayField));
    client.debugAssert(! program.fieldIsStatic(arrayField));
    
    String arrayFieldPred = vocabualry.instanceFieldToPred(arrayField);
    return setBinaryPredicate(arrayFieldPred, from, to, val);            
  }
  
  
  public boolean setAllocationSite(Object allocSite, int node, ITVLAKleeneValue val) {
    client.debugAssert(program.representsAllocationSite(allocSite));
    
    String allocSitePred = vocabualry.allocationSiteToPred(allocSite);
    return setUnaryPredicate(allocSitePred, node, val);			
  }
  
  public boolean setArrayAllocationSite(Object arrayAllocSite, int node, ITVLAKleeneValue val) {
    client.debugAssert(program.representsAllocationSite(arrayAllocSite));
    client.debugAssert(program.representsArrayClass(program.allocationSiteAllocatedClass(arrayAllocSite)));
    
    String arrayAllocSitePred = vocabualry.arrayAllocationSiteToPred(arrayAllocSite);
    return setUnaryPredicate(arrayAllocSitePred , node, val);						
  }
  
  public boolean setRefLocal(Object method, int indx, int node, ITVLAKleeneValue val) {
    client.debugAssert(program.representsMethod(method));
    
    String refLocalPred = vocabualry.refLocalToPred(method, indx);
    return setUnaryPredicate(refLocalPred, node, val);						
  }
}