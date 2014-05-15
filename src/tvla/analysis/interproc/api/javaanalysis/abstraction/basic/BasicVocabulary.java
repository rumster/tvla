//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.javaanalysis.abstraction.basic;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaAbstraction;
import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaVocabulary;
import tvla.analysis.interproc.api.tvlaadapter.TVLAPredicate;
import tvla.analysis.interproc.api.tvlaadapter.abstraction.TVLAVocabulary;
import tvla.api.ITVLAAPIDebuggingServices;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModelerServices;

public  class BasicVocabulary extends TVLAVocabulary  implements IJavaVocabulary {
  protected ITVLAJavaProgramModelerServices program;
  protected ITVLAAPIDebuggingServices      client;
  
  /**
   * The govering abstraction
   */  
  protected final IJavaAbstraction abstraction; 

  
  
   /**
   * @param abstraction1
   */
  public BasicVocabulary(
      IJavaAbstraction abstraction,
      ITVLAJavaProgramModelerServices program,  
      ITVLAAPIDebuggingServices client) {
    this.abstraction = abstraction;
    this.program = program;
    this.client = client;
  }
  
  public IJavaAbstraction getAbstraction() {
    return abstraction;
  }
  
  public TVLAPredicate[] getInternalPredicates() {
    return null;
  }


  
  /***********************************************
   * Array rerpesentation
   ***********************************************/
  
  public final TVLAPredicate arrayContains = new TVLAPredicate(BasicJavaConstants.containsStr, 2);				
  public String arrayContains() {
    return arrayContains.getPredId();
  }			
  
  public final TVLAPredicate isArray = new TVLAPredicate(BasicJavaConstants.isArrayStr, 1);              
  public String isArray() {
    return isArray.getPredId();
  }         

  
  public IPredicate getPredicate(String id) {
    if (id == null)
      return null;
    
    if (id.equals(arrayContains.getPredId()))
      return arrayContains;
    
    if (id.equals(isArray.getPredId()))
      return isArray;

    return super.getPredicate(id);
  }		
  
  
  /**************************************************
   * Java modeling: program dependant predicates
   **************************************************/
  
  

  /***********************************************
   * Locals
   ***********************************************/
  
  public String boolLocalToPred(Object method, int index) {
    return "b" + program.methodLocalName(method, index);
  }
  
  public String refLocalToPred(Object method, int index) {
    return "p" + program.methodLocalName(method, index);
  }
  
  public String arrayLocalToPred(Object method, int index) {
    return refLocalToPred(method, index);
  }
  
  /***********************************************
   * Parameter bindings
   ***********************************************/
  
  public String boolTransfer(int index) {
    return "b" + (index + 1);
  }
  
  public String refTransfer(int index) {
    return "p" + (index + 1);
  }
  
  public String arrayTransfer(int index) {
    return refTransfer(index);
  }

  /***********************************************
   * Types
   ***********************************************/  
  public String classToPred(Object klass) {
    return "C" + program.className(klass);
  }
  
  public String arrayClassToPred(Object array) {
    return "AC" + program.arrayClassName(array);
  }
  
  /***********************************************
   * AllocationSites
   ***********************************************/
  
  public String allocationSiteToPred(Object allocSite) {
    return program.allocationSiteUniqueId(allocSite);
  }
  
  public String arrayAllocationSiteToPred(Object arrayAllocSite) {
    return program.allocationSiteUniqueId(arrayAllocSite);
  }
  
  
  /***********************************************
   * Static Fields
   ***********************************************/
  
  public String staticFieldToPred(Object theStaticField) {
    client.debugAssert(program.fieldIsStatic(theStaticField));
    
    if (program.fieldIsArray(theStaticField))
      return arrayStaticFieldToPred(theStaticField);
    else if (program.fieldIsReference(theStaticField))
      return refStaticFieldToPred(theStaticField);
    else if (program.fieldIsBoolean(theStaticField))
      return boolStaticFieldToPred(theStaticField);

   
    client.debugAssert(false);
    
    return null;
  }
  
  public String boolStaticFieldToPred(Object boolStaticField) {
    client.debugAssert(program.fieldIsStatic(boolStaticField));
    client.debugAssert(program.fieldIsBoolean(boolStaticField));
    return program.fieldUniqueId(boolStaticField);
  }
  
  public String refStaticFieldToPred(Object refStaticField) {
    client.debugAssert(program.fieldIsStatic(refStaticField));
    client.debugAssert(program.fieldIsReference(refStaticField));
    return program.fieldUniqueId(refStaticField);
  }
  
  public String arrayStaticFieldToPred(Object arrayStaticField)  {
    client.debugAssert(program.fieldIsStatic(arrayStaticField));
    client.debugAssert(program.fieldIsArray(arrayStaticField));
    return program.fieldUniqueId(arrayStaticField);
  }

  /***********************************************
   * Instance Fields 
   ***********************************************/
  
  public String instanceFieldToPred(Object theInstanceField) {
    client.debugAssert(! program.fieldIsStatic(theInstanceField));
    
    if (program.fieldIsArray(theInstanceField))
      return arrayInstanceFieldToPred(theInstanceField);
    else if (program.fieldIsReference(theInstanceField))
      return refInstanceFieldToPred(theInstanceField);
    else if (program.fieldIsBoolean(theInstanceField))
      return boolInstanceFieldToPred(theInstanceField);    
    
    client.UNREACHABLE();
    
    return null;
  }
  
  public String boolInstanceFieldToPred(Object boolInstanceField) {
    client.debugAssert(! program.fieldIsStatic(boolInstanceField));
    client.debugAssert(program.fieldIsBoolean(boolInstanceField));
    return program.fieldUniqueId(boolInstanceField);
  }
  
  public String refInstanceFieldToPred(Object refInstanceField) {
    client.debugAssert(! program.fieldIsStatic(refInstanceField));
    client.debugAssert(program.fieldIsReference(refInstanceField));
    return program.fieldUniqueId(refInstanceField);
  }
  
  
  public String arrayInstanceFieldToPred(Object arrayInstanceArrayField) {
    client.debugAssert(! program.fieldIsStatic(arrayInstanceArrayField));
    client.debugAssert(program.fieldIsArray(arrayInstanceArrayField));
    return program.fieldUniqueId(arrayInstanceArrayField);
   
  }

}