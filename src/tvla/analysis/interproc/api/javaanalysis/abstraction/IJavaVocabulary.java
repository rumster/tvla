//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.javaanalysis.abstraction;

import tvla.analysis.interproc.api.tvlaadapter.TVLAPredicate;
import tvla.analysis.interproc.api.tvlaadapter.abstraction.ITVLAVocabulary;

public interface IJavaVocabulary extends ITVLAVocabulary {
  /**
   * Returns governing abstraction
   * @return
   */
  public IJavaAbstraction getAbstraction();

  
  /**
   * Returns the intrnal predicates
   */
  public TVLAPredicate[] getInternalPredicates();
  
  /***********************************************
   * Locals
   ***********************************************/

  public String boolLocalToPred(Object method, int index) ;
  public String refLocalToPred(Object method, int index) ;
  public String arrayLocalToPred(Object method, int index) ;

  /***********************************************
   * Parameter binfdings
   ***********************************************/
  
  /**
   * Transforms the index parameter to a predicate for transfering boolean values
   */
  public String boolTransfer(int index) ;

  
  /**
   * Transforms the index parameter to a predicate for transfering reference values
   */
public String refTransfer(int index) ;
    
  
  
  /***********************************************
   * Types
   ***********************************************/
  public String classToPred(Object klass) ;
  
  public String arrayClassToPred(Object array) ;

  
  /***********************************************
   * AllocationSites
   ***********************************************/

  public String arrayAllocationSiteToPred(Object arrayAllocSite) ;
  
  public String allocationSiteToPred(Object allocSite) ;
 
  /***********************************************
   * Array rerpesentation
   ***********************************************/

  /**
   * Returns the binary predicate that says that an object is contained in 
   * a given array
   * @return
   */ 
  public String arrayContains();
 
  /**
   * Returns the unary predicate that says that an object is an array object
   * (i.e., of an array class)
   * @return
   */ 
  public String isArray();  
  
  /***********************************************
   * Static Fields
   ***********************************************/
  
  /**
   * Returns the predicated pertaining to a static field after resoplving its type
   */
  public String staticFieldToPred(Object theStaticField) ;
  
  /**
   * Returns the predicated pertaining to a static field of type boolean
   */
  public String boolStaticFieldToPred(Object boolStaticField) ;
  
  /**
   * Returns the predicated pertaining to a static field of type reference
   */
  public String refStaticFieldToPred(Object refStaticField) ;

  /**
   * Returns the predicated pertaining to a static field that points to an array
   */
  public String arrayStaticFieldToPred(Object refStaticField) ;
 
  
  /***********************************************
   * Instance Fields 
   ***********************************************/
  
  public String instanceFieldToPred(Object theInstanceField) ;
  public String boolInstanceFieldToPred(Object boolInstanceField) ;
  public String refInstanceFieldToPred(Object refInstanceField);
  
  /**
   * Returns the predicated pertaining to an instance field that points to an array
   */
  public String arrayInstanceFieldToPred(Object refInstanceArrayField) ;

}