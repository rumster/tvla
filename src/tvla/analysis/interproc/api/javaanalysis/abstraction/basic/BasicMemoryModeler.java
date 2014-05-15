package tvla.analysis.interproc.api.javaanalysis.abstraction.basic;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaAbstraction;
import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaMemoryModeler;
import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaVocabulary;
import tvla.analysis.interproc.api.tvlaadapter.TVLAAPI;
import tvla.analysis.interproc.api.utils.TVLAAPIDebugControl;
import tvla.api.ITVLAAPIDebuggingServices;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModelerServices;


/**
 * The bridge connecting the DOMO engine with TVLA.
 * The bridge translates domoPrimitives (e.g., classes, methods, variables, VariableKeys, flowfunctions, factoid numerals etc.) 
 * to TVLA primitives (prediacates, actions, TVSs, and TVSSets) and vice versa
 * @author noam rinetzky
 */
public  class BasicMemoryModeler implements IJavaMemoryModeler {
  protected final TVLAAPI tvlaapi; 						// A reference to the (one and only) TVLA backend 
  protected final ITVLAJavaProgramModelerServices program;	// allows to interrogate the program
  protected final IJavaVocabulary vocabulary;           // translates program entities to predicates 
  protected final ITVLAAPIDebuggingServices client;        // The analysis environement
  
  /**
   * The govering abstraction
   */  
  protected final IJavaAbstraction abstraction; 

  
  private static int DEBUG_LEVEL = TVLAAPIDebugControl.getDebugLevel(3);
  
  /////////////////////////////////////////
  ///  Initializtion of the JavaAdapter ///
  /////////////////////////////////////////
  
  public BasicMemoryModeler(
      IJavaAbstraction abstraction,
      TVLAAPI tvlaAPI,
      IJavaVocabulary vocabulary,
      ITVLAAPIDebuggingServices client,
      ITVLAJavaProgramModelerServices programModeler) {
    this.tvlaapi =  tvlaAPI;
    this.vocabulary = vocabulary;
    this.client = client;
    this.program = programModeler;
    this.abstraction = abstraction;
  }
  
  public IJavaAbstraction getAbstraction() {
    return abstraction;
  }
  
  public void addClass(Object klass) {
    getClient().debugAssert(program.representsClass(klass));
    
    String pred = vocabulary.classToPred(klass);
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("BasicMemoryModeler: adding class " + program.className(klass) + 
          " (unique id " + program.classUniqueId(klass)+ " pred = " + pred + ")");
    
    tvlaapi.addToSet(BasicJavaConstants.setClassesStr, pred);
  }      
  
  public void addArrayClass(Object array) {
    getClient().debugAssert(program.representsArrayClass(array));
    
    
    String pred = vocabulary.arrayClassToPred(array);
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("BasicMemoryModeler: adding array class " + program.arrayClassName(array) + 
          " (unique id " + program.arrayClassUniqueId(array)+ ", pred = " + pred + ")");
    
    tvlaapi.addToSet(BasicJavaConstants.setArrayClassesStr, pred);
  }      
  
  
  public void addAllocationSite(Object allocSite) {
    getClient().debugAssert(program.representsAllocationSite(allocSite));
    getClient().debugAssert(program.representsClass(program.allocationSiteAllocatedClass(allocSite)));
    getClient().debugAssert(!program.representsArrayClass(program.allocationSiteAllocatedClass(allocSite)));
    
    
    String pred = vocabulary.allocationSiteToPred(allocSite);
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("BasicMemoryModeler: adding allocation site " + allocSite + " pred: " + pred);
    
    tvlaapi.addToSet(BasicJavaConstants.setAllocationSitesStr, pred);
  }
  
  
  public void addArrayAllocationSite(Object allocSite) {
    getClient().debugAssert(program.representsAllocationSite(allocSite));
    getClient().debugAssert(program.representsClass(program.allocationSiteAllocatedClass(allocSite)));
    getClient().debugAssert(program.representsArrayClass(program.allocationSiteAllocatedClass(allocSite)));
    
    String pred = vocabulary.allocationSiteToPred(allocSite);
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("BasicMemoryModeler: adding array allocation site " + allocSite + " pred: " + pred);
    
    tvlaapi.addToSet(BasicJavaConstants.setArrayAllocationSitesStr, vocabulary.allocationSiteToPred(allocSite));
  }
  
  
  public void addField(Object field) {
    getClient().debugAssert(program.representsField(field));
    
    if (program.fieldIsStatic(field)) {
      addStaticField(field);
    }  
    else {
      addInstanceField(field);
    }
  }
  
  
  protected void addStaticField(Object field) {
    getClient().debugAssert(program.fieldIsStatic(field));
    
    if (program.fieldIsBoolean(field)) {
      if (DEBUG_LEVEL > 2) {
        String name = program.fieldName(field);
        String uniqueId = program.fieldUniqueId(field);
        getClient().tracePrintln("BasicMemoryModeler: adding static boolean field " + name +  
            " (uniq id " + uniqueId + ")");
      }				
      tvlaapi.addToSet(BasicJavaConstants.setBoolStaticFieldsStr, vocabulary.boolStaticFieldToPred(field));				
    }
    else if (program.fieldIsReference(field)){      
      if (DEBUG_LEVEL > 2) {
        String name = program.fieldName(field);
        String uniqueId = program.fieldUniqueId(field);
        getClient().tracePrintln("BasicMemoryModeler: adding static reference field " + name +   
            " (uniq id " + uniqueId + ")");
      }				
      tvlaapi.addToSet(BasicJavaConstants.setRefStaticFieldsStr, vocabulary.refStaticFieldToPred(field));	
    }		
    else {      
      getClient().debugAssert(program.fieldIsArray(field));
      if (DEBUG_LEVEL > 2) {
        String name = program.fieldName(field);
        String uniqueId = program.fieldUniqueId(field);
        getClient().tracePrintln("BasicMemoryModeler: adding static array field " + name +   
            " (uniq id " + uniqueId + ")");
      }             
      tvlaapi.addToSet(BasicJavaConstants.setArrayStaticFieldsStr, vocabulary.arrayStaticFieldToPred(field));   
    }       
  }
  
  protected void addInstanceField(Object field) {
    getClient().debugAssert(! program.fieldIsStatic(field));
    
    if (program.fieldIsBoolean(field)) {
      if (DEBUG_LEVEL > 2) {
        String name = program.fieldName(field);
        String uniqueId = program.fieldUniqueId(field);
        getClient().tracePrintln("BasicMemoryModeler: adding instance boolean field " + name +  
            " (uniq id " + uniqueId + ")");
      }               
      tvlaapi.addToSet(BasicJavaConstants.setBoolInstanceFieldsStr, vocabulary.boolInstanceFieldToPred(field));               
    }
    else if (program.fieldIsReference(field)){
      getClient().debugAssert(program.fieldIsReference(field));
      if (DEBUG_LEVEL > 2) {
        String name = program.fieldName(field);
        String uniqueId = program.fieldUniqueId(field);
        getClient().tracePrintln("BasicMemoryModeler: adding instance reference field " + name +   
            " (uniq id " + uniqueId + ")");
      }               
      tvlaapi.addToSet(BasicJavaConstants.setRefInstanceFieldsStr, vocabulary.refInstanceFieldToPred(field)); 
    }     
    else {
      getClient().debugAssert(program.fieldIsArray(field));
      if (DEBUG_LEVEL > 2) {
        String name = program.fieldName(field);
        String uniqueId = program.fieldUniqueId(field);
        getClient().tracePrintln("BasicMemoryModeler: adding instance array field " + name +   
            " (uniq id " + uniqueId + ")");
      }               
      tvlaapi.addToSet(BasicJavaConstants.setArrayInstanceFieldsStr, vocabulary.arrayInstanceFieldToPred(field)); 
    }     
  }
  
  
  public void addMethod(Object method) {
    getClient().debugAssert(program.representsMethod(method));
    
    for(int paramIndex=0; paramIndex < program.methodNumberOfParameters(method); paramIndex++) {
      int valNum = program.methodParameterNumberToLocalIndex(method, paramIndex);
      if (program.methodLocalTypeIsBoolean(method, valNum)) {
        if (DEBUG_LEVEL > 2 && getClient().trace()) {
          String name = program.methodLocalName(method, valNum);
          String uniqueId = program.methodLocalUniqueId(method, valNum);
          getClient().tracePrintln("BasicMemoryModeler: adding boolean  # " + valNum + " name " + valNum + " name " + name +  
              " (uniq id " + uniqueId + ")" + " in method " + program.methodName(method));
        }
        
        String foramlName = vocabulary.boolTransfer(paramIndex);
        tvlaapi.addToSet(BasicJavaConstants.setBoolParametersStr, foramlName); 				
        tvlaapi.addToSet(BasicJavaConstants.setBoolLocalsStr, foramlName); 				
        String predName = vocabulary.boolLocalToPred(method, valNum);
        tvlaapi.addToSet(BasicJavaConstants.setBoolLocalsStr, predName); 				
      } 
      else if (program.methodLocalTypeIsReference(method, valNum)) {
        if (DEBUG_LEVEL > 2 && getClient().trace()) {
          String name = program.methodLocalName(method, valNum);
          String uniqueId = program.methodLocalUniqueId(method, valNum);
          getClient().tracePrintln("BasicMemoryModeler: adding reference parameter # " + valNum + " name " + name +  
              " (uniq id " + uniqueId + ")" + " in method " + program.methodName(method));
        }
        String formalName = vocabulary.refTransfer(paramIndex);
        tvlaapi.addToSet(BasicJavaConstants.setRefParametersStr, formalName); 				
        tvlaapi.addToSet(BasicJavaConstants.setRefLocalsStr, formalName); 				
        String predName = vocabulary.refLocalToPred(method, valNum);
        tvlaapi.addToSet(BasicJavaConstants.setRefLocalsStr, predName); 				
      }		
      else {
        if (DEBUG_LEVEL > 2 && getClient().trace()) {
          String name = program.methodLocalName(method, valNum);
          String uniqueId = program.methodLocalUniqueId(method, valNum);
          getClient().tracePrintln("BasicMemoryModeler: ignoring parameter number" + valNum + " in method " + name + " local unique id " + uniqueId + ")");
        }
        
      }
    }
  }
  
  
  public void addArrayLocal(Object method, int indx) {
    getClient().debugAssert(program.representsMethod(method));
    
    if (DEBUG_LEVEL > 2 && getClient().trace()) {
      String name = program.methodLocalName(method, indx);
      String uniqueId = program.methodLocalUniqueId (method, indx);
      getClient().tracePrintln("BasicMemoryModeler: adding reference local " + name +  
          " (uniq id " + uniqueId + ")" + " in method " + program.methodName(method));
    }
    tvlaapi.addToSet(BasicJavaConstants.setArrayLocalsStr, vocabulary.arrayLocalToPred(method, indx));              
  }
  
  public void addRefLocal(Object method, int indx) {
    getClient().debugAssert(program.representsMethod(method));
    
    if (DEBUG_LEVEL > 2 && getClient().trace()) {
      String name = program.methodLocalName(method, indx);
      String uniqueId = program.methodLocalUniqueId (method, indx);
      getClient().tracePrintln("BasicMemoryModeler: adding reference local " + name +  
          " (uniq id " + uniqueId + ")" + " in method " + program.methodName(method));
    }
    tvlaapi.addToSet(BasicJavaConstants.setRefLocalsStr, vocabulary.refLocalToPred(method, indx)); 				
  }
  
  public void addBooleanLocal(Object method, int indx) {
    getClient().debugAssert(program.representsMethod(method));
    
    if (DEBUG_LEVEL > 2 && getClient().trace()) {
      String name = program.methodLocalName(method, indx);
      String uniqueId = program.methodLocalUniqueId (method, indx);
      getClient().tracePrintln("BasicMemoryModeler: adding boolean local " + name +  
          " (uniq id " + uniqueId + ")" + " in method " + program.methodName(method));
    }
    tvlaapi.addToSet(BasicJavaConstants.setBoolLocalsStr, vocabulary.boolLocalToPred(method, indx)); 				
  }
  
  /* (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#processProgramModel()
   */ 
  public boolean processProgramModel() {   
    return true;
  }
  
  /*******************************************************
   * Helper methods
   */
  
  private ITVLAAPIDebuggingServices getClient() {
    return client;
  }
}
