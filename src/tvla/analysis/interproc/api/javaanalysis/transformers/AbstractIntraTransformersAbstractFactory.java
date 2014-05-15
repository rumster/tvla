package tvla.analysis.interproc.api.javaanalysis.transformers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaVocabulary;
import tvla.analysis.interproc.api.tvlaadapter.TVLAAPI;
import tvla.api.AbstractTVLAAPI;
import tvla.api.ITVLAAPIDebuggingServices;
import tvla.api.ITVLATransformers;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaAnalsyisEnvironmentServicesPovider;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModelerServices;
import tvla.api.ITVLATransformers.ITVSUnaryTransformer;
import tvla.util.HashSetFactory;


/**
 * The Intraprocedural ptransformers factory.
 * Currently, thee is only one.
 * 
 * @author noam rinetzky
 */
public abstract class AbstractIntraTransformersAbstractFactory extends AbstractTransformersAbstractFactory {
  protected final TVLAAPI tvlaapi; 						     // A reference to the (one and only) TVLA backend 
 
  protected final ITVLAJavaAnalsyisEnvironmentServicesPovider environmentServicesProvider;
  
  // Fast access to the environment services
  protected final ITVLAAPIDebuggingServices client;			 // The analysis environement
  protected final ITVLAJavaProgramModelerServices program;	     // allos to interrogate the program
  protected final IJavaVocabulary vocabulary;                // translates program entities to tvla predicates
  
  /////////////////////////////////////////
  // Shared by all subclases             //
  /////////////////////////////////////////
  
  /**
   * Arguments for actions with an empty parameter list
   */
  protected static final List stub = new ArrayList(0);   

  
  /////////////////////////////////////////
  ///  Initializtion                    ///
  /////////////////////////////////////////
  
  
  public AbstractIntraTransformersAbstractFactory(
      AbstractTVLAAPI tvlaAPI,
      ITVLAJavaAnalsyisEnvironmentServicesPovider environmentServicesProvider,
      IJavaVocabulary vocabulary) {
    this.tvlaapi = (TVLAAPI) tvlaAPI;
    this.environmentServicesProvider = environmentServicesProvider;
    this.client = environmentServicesProvider.getJavaDebuggingServices();
    this.program = environmentServicesProvider.getJavaProgramModelerServices();
    this.vocabulary = vocabulary;
  }
  
  
  /* (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#processProgramModel()
   */ 
  public boolean processProgramModel() {
    Set expectedMacros = getActionsNames();
    
    boolean ret =  tvlaapi.instantiateParametericDomain(expectedMacros);
    
    return ret;
  }
    
  protected void getActionsNames(Set actionNames) {
    actionNames.addAll(IntraConstants.getIntraActionsNames());
  }

  public final Set getActionsNames(){
    Set expectedMacros = HashSetFactory.make();
    
    getActionsNames(expectedMacros);
    
    return expectedMacros;
  }
  
  
  /****************************************************************
   * 
   ****************************************************************/

  protected static class IntraConstants {
    /*********************************************
     * Intraprocedural transformers (Actions)
     *********************************************/
    // PT == program point (ignore it)
    public final static String actionSetTrueBooleanLocalStr = "boolean_Set_True";                           // (PT, b)          b = true
    public final static String actionSetFalseBooleanLocalStr = "boolean_Set_False";                         // (PT, b)          b = false
    public final static String actionSetUnkownBooleanLocalStr = "boolean_Set_Unknown";                      // (PT, b)          b = 1/2
    public final static String actionCopyBooleanLocalToBooleanLocalStr = "boolean_Copy_Var";                // (PT,b1,b2)       b1 = b2
    
    public final static String actionGetStaticBooleanFieldStr = "boolean_Get_Static";                       // (PT, b, T, f)    b = T.f
    public final static String actionPutStaticBooleanFieldStr = "boolean_Set_Static";                       // (PT, T, f, b)    T.f = b
    
    public final static String actionGetInstanceBooleanFieldStr = "boolean_Get_Field";                      // (PT, b, x, f)    b = x.f
    public final static String actionPutInstanceBooleanFieldStr = "boolean_Set_Field";                      // (PT, y, f, b)    y.f = b  
    
    
    
    public final static String actionAllocStr = "NewClass";                                                 // (PT, y, T)       y = new T()
    public final static String actionArrayAllocStr = "NewArray";                                            // (PT, y, T)       y = newArray T()
    public final static String actionArrayGetStr = "ArrayGet";                                              // (PT, y, x)       y = x[?]
    public final static String actionArrayPutStr = "ArrayPut";                                              // (PT, y, x)       y[?] = x
    public final static String actionArrayBooleanGetStr = "boolean_ArrayGet";                               // (PT, b, x)       y = b[?]
    public final static String actionArrayBooleanPutStr = "boolean_ArrayPut";                               // (PT, y, b)       y[?] = b
    public final static String actionArrayLengthStr = "ArrayLength";                                        // (PT, y)          y.length
    
    
    public final static String actionNullifyReferenceLocalStr = "Set_Null_Var";                             // (PT, y)          y = null
    public final static String actionCopyReferenceLocalToReferenceLocalStr = "Copy_Var";                    // (PT, y, x)       y = x
    
    public final static String actionGetStaticReferenceFieldStr = "Assign_Ref_With_Static_Field";           // (PT, x, T, f)    x = T.f
    public final static String actionPutStaticReferenceFieldStr = "Assign_Static_Field_with_Local";         // (PT, T, f, x)    T.f = x
    public final static String actionNullifyStaticReferenceFieldStr = "Assign_Static_Field_with_Null";      // (PT, T, f, x)    T.f = null
    
    public final static String actionGetInstanceReferenceFieldStr = "Assign_Local_with_Field";              // (PT, y, x, f)    y = x.f
    public final static String actionPutInstanceReferenceFieldStr = "Assign_Field_with_Local";              // (PT, y, f, x)    y.f = x
    public final static String actionNullifyInstanceReferenceFieldStr = "Assign_Field_with_Null";           // (PT, y, f)       y.f = null
    
    
    public final static String actionBooleanLocalIsTrueStr = "boolean_Is_True";                              // (PT, b1)            b1
    public final static String actionBooleanLocalIsFalseStr = "boolean_Is_False";                            // (PT, b1)            !b1
    public final static String actionBooleanLocalsAreEqStr = "boolean_Are_Eq";                               // (PT, b1, b2)        b1 == b2
    public final static String actionBooleanLocalsAreNotEqStr = "boolean_Are_Not_Eq";                        // (PT, b1, b2)        b1 != b2
    
    public final static String actionReferenceLocalsAreEqStr = "Are_Eq";                                     // (PT, x1, x2)        x1 == x2
    public final static String actionReferenceLocalsAreNotEqStr = "Are_Not_Eq";                              // (PT, x1, x2)        x1 != x2
    public final static String actionReferenceLocalIsNullStr = "Is_Null";                                    // (PT, x1, x2)        x1 == x2
    public final static String actionReferenceLocalIsNotNullStr = "Is_Not_Null";                             // (PT, x1, x2)        x1 == x2

    
    // Convineient array
    public final static String[] allIntraActionsStrs = new String[] {
        actionArrayAllocStr,
        actionArrayLengthStr,
        actionArrayPutStr, 
        actionArrayGetStr,
        actionArrayBooleanGetStr,
        actionArrayBooleanPutStr,
        actionSetTrueBooleanLocalStr, 
        actionSetFalseBooleanLocalStr,
        actionSetUnkownBooleanLocalStr,
        actionCopyBooleanLocalToBooleanLocalStr, 
        actionGetStaticBooleanFieldStr, 
        actionPutStaticBooleanFieldStr, 
        actionGetInstanceBooleanFieldStr, 
        actionPutInstanceBooleanFieldStr, 
        actionAllocStr,
        actionNullifyReferenceLocalStr, 
        actionCopyReferenceLocalToReferenceLocalStr,
        actionGetStaticReferenceFieldStr,
        actionPutStaticReferenceFieldStr, 
        actionNullifyStaticReferenceFieldStr,
        actionGetInstanceReferenceFieldStr,
        actionPutInstanceReferenceFieldStr, 
        actionNullifyInstanceReferenceFieldStr,
        actionBooleanLocalIsTrueStr,
        actionBooleanLocalIsFalseStr,
        actionBooleanLocalsAreEqStr, 
        actionBooleanLocalsAreNotEqStr,
        actionReferenceLocalsAreEqStr,
        actionReferenceLocalsAreNotEqStr,
        actionReferenceLocalIsNullStr,
        actionReferenceLocalIsNotNullStr,
    };    

    protected static Collection getIntraActionsNames() {
      return Arrays.asList(IntraConstants.allIntraActionsStrs);
    }

  }
  
  
  /*********************************************
   * Local varaibles
   *********************************************/
  
  /* (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makeCopyBooleanToBooleanFlowFunction(java.lang.String, java.lang.String, java.lang.String)
   */
  public ITVSUnaryTransformer makeCopyBooleanToBooleanFlowFunction(Object method, int lhsBool, int rhsBool) {
    if (shouldAssert()) {
      getClient().debugAssert(program.representsMethod(method));
      // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsBoolean(method, lhsBool));
      // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsBoolean(method, rhsBool));
    }
    
    String lhs = vocabulary.boolLocalToPred(method, lhsBool);
    String rhs = vocabulary.boolLocalToPred(method, rhsBool); 
    
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeCopyBooleanToBooleanFlowFunction - " + 
          " method " + program.methodName(method) + " : " + 
          lhs + " = " + rhs );
    
    List args = new ArrayList(3);
    args.add(0, "filler");
    args.add(1, lhs);
    args.add(2, rhs);
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionCopyBooleanLocalToBooleanLocalStr, args);
  }
  
  /* (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makeCopyBooleanToBooleanFlowFunction(java.lang.String, java.lang.String, boolean)
   */	
  
  public ITVSUnaryTransformer makeAssignConstToBooleanFlowFunction(Object method, int lhsBool, boolean val) {
    getClient().debugAssert(program.representsMethod(method));
    // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsBoolean(method, lhsBool));
    
    String lhs = vocabulary.boolLocalToPred(method, lhsBool);
    
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeConstToBooleanFlowFunction - " + 
          " method " + program.methodName(method) + " : " + 
          lhs + " = " + (val ? "true" : "false"));
    
    List args = new ArrayList(2);
    args.add(0, "filler");
    args.add(1, lhs);
    
    if (val)
      return tvlaapi.getUnaryTransformer(IntraConstants.actionSetTrueBooleanLocalStr, args);
    else
      return tvlaapi.getUnaryTransformer(IntraConstants.actionSetFalseBooleanLocalStr, args);	
  }
  
  
  /* (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makeAssignUnknownToBooleanFlowFunction(java.lang.String, java.lang.String)
   */	
  public ITVSUnaryTransformer makeAssignUnknownToBooleanFlowFunction(Object  method, int lhsBool) {
    getClient().debugAssert(program.representsMethod(method));
    // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsBoolean(method, lhsBool));
    
    String lhs = vocabulary.boolLocalToPred(method, lhsBool);
    
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeCopyBooleanToBooleanFlowFunction - " + 
          " method " + program.methodName(method) + " : " + 
          lhs + " = 1/2");
    
    
    List args = new ArrayList(2);
    args.add(0, "filler");
    args.add(1, lhs);
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionSetUnkownBooleanLocalStr, args);
  }
  
  
  
  /* (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makeNullifyReferenceFlowFunction(java.lang.String, java.lang.String)
   */
  
  public ITVSUnaryTransformer makeAssignNullToReferenceFlowFunction(Object method, int lhsRef) {
    getClient().debugAssert(program.representsMethod(method));
    // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsReference(method, lhsRef));
    
    String lhs = vocabulary.refLocalToPred(method, lhsRef);
    
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeAssignNullToReferenceFlowFunction - " + 
          " method " + program.methodName(method) + " : " + lhs + " = null" );
    
    List args = new ArrayList(2);
    args.add(0, "filler");
    args.add(1, lhs);
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionNullifyReferenceLocalStr, args);
  }
  
  /* (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makeCopyReferenceToReferenceFlowFunction(java.lang.String, java.lang.String, java.lang.String)
   */
  public ITVSUnaryTransformer makeCopyReferenceToReferenceFlowFunction(Object method, int lhsRef, int rhsRef) {
    getClient().debugAssert(program.representsMethod(method));
    // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsReference(method, lhsRef));
    // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsReference(method, rhsRef));
    
    String lhs = vocabulary.refLocalToPred(method, lhsRef);
    String rhs = vocabulary.refLocalToPred(method, rhsRef);
    
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeCopyReferenceToReferenceFlowFunction - " + 
          " method " + program.methodName(method) + " : " + lhs + " = " + rhs );
    
    List args = new ArrayList(3);
    args.add(0, "filler");
    args.add(1, lhs);
    args.add(2, rhs);
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionCopyReferenceLocalToReferenceLocalStr, args);
  }
  
  
  
  
  /****************************************************
   * TESTS 
   ****************************************************/
  
  public ITVLATransformers.ITVSUnaryTransformer 
  makeCheckBooleanFlowFunction(Object method, int rhsBool, boolean isTrue) {
    if (shouldAssert()) {
      getClient().debugAssert(program.representsMethod(method));
      // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsBoolean(method, rhsBool));
    }
    
    String rhs1 = vocabulary.boolLocalToPred(method, rhsBool);
    
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeCheckBooleansFlowFunction - " + 
          " method " + program.methodName(method) + " : " + 
          rhs1 + (isTrue ? " == true " : " == false"));
    
    List args = new ArrayList(3);
    args.add(0, "filler");
    args.add(1, rhs1);
    
    String comparator = isTrue ? 
        IntraConstants.actionBooleanLocalIsTrueStr : 
        IntraConstants.actionBooleanLocalIsFalseStr;
    
    return tvlaapi.getUnaryTransformer(comparator, args);
    
    
  }

  
  public ITVLATransformers.ITVSUnaryTransformer 
  makeCompareBooleansFlowFunction(Object method, int rhsBool1, int rhsBool2, boolean eq) {
    if (shouldAssert()) {
      getClient().debugAssert(program.representsMethod(method));
      // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsBoolean(method, rhsBool1));
      // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsBoolean(method, rhsBool2));
    }
    
    String rhs1 = vocabulary.boolLocalToPred(method, rhsBool1);
    String rhs2 = vocabulary.boolLocalToPred(method, rhsBool2); 
    
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeCompareBooleansFlowFunction - " + 
          " method " + program.methodName(method) + " : " + 
          rhs1 + (eq ? " = " : " != ") + rhs2 );
    
    List args = new ArrayList(3);
    args.add(0, "filler");
    args.add(1, rhs1);
    args.add(2, rhs2);
    
    String comparator = eq ? IntraConstants.actionBooleanLocalsAreEqStr : IntraConstants.actionBooleanLocalsAreNotEqStr;
    
    return tvlaapi.getUnaryTransformer(comparator, args);
    
  }
  
  public ITVLATransformers.ITVSUnaryTransformer 
  makeCompareReferencesFlowFunction(Object method, int rhsRef1, int rhsRef2, boolean eq) {
    if (shouldAssert()) {
      getClient().debugAssert(program.representsMethod(method));
      // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsBoolean(method, lhsBool1));
      // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsBoolean(method, lhsBool2));
    }
    
    String rhs1 = vocabulary.refLocalToPred(method, rhsRef1);
    String rhs2 = vocabulary.refLocalToPred(method, rhsRef2); 
    
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeCompareReferencesFlowFunction - " + 
          " method " + program.methodName(method) + " : " + 
          rhs1 + (eq ? " = " : " != ") + rhs2 );
    
    List args = new ArrayList(3);
    args.add(0, "filler");
    args.add(1, rhs1);
    args.add(2, rhs2);
    
    String comparator = eq ? IntraConstants.actionReferenceLocalsAreEqStr : IntraConstants.actionReferenceLocalsAreNotEqStr;
    
    return tvlaapi.getUnaryTransformer(comparator, args);
    
  }
  
  public ITVLATransformers.ITVSUnaryTransformer 
  makeCompareReferenceToNullFlowFunction(Object method, int rhsRef, boolean eq) {
    if (shouldAssert()) {
      getClient().debugAssert(program.representsMethod(method));
      // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsBoolean(method, lhsBool1));
      // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsBoolean(method, lhsBool2));
    }
    
    String rhs = vocabulary.refLocalToPred(method, rhsRef);
    
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeCompareReferenceToNullFlowFunction - " + 
          " method " + program.methodName(method) + " : " + 
          rhs + (eq ? " == " : " != ") + " null");
    
    List args = new ArrayList(3);
    args.add(0, "filler");
    args.add(1, rhs);
    
    String comparator = eq ? IntraConstants.actionReferenceLocalIsNullStr : IntraConstants.actionReferenceLocalIsNotNullStr;
    
    return tvlaapi.getUnaryTransformer(comparator, args);
    
  }
  
  
  /*********************************************
   * Allocations
   *********************************************/
  
  /* (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makeAllocFlowFunction(java.lang.String, java.lang.String, java.lang.String)
   */
  
  public ITVSUnaryTransformer makeAllocFlowFunction(Object method, int lhsRef, Object allocationSite) {
    getClient().debugAssert(program.representsMethod(method));
    getClient().debugAssert(program.representsAllocationSite(allocationSite));
    
    Object allocatedClass = program.allocationSiteAllocatedClass(allocationSite);
    getClient().debugAssert(program.representsClass(allocatedClass));
    getClient().debugAssert(!program.representsArrayClass(allocatedClass));
    // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsReference(method, lhsRef));
    
    String lhs = vocabulary.refLocalToPred(method, lhsRef);
    String classOfAllocatedObject = vocabulary.classToPred(allocatedClass);
    String tag = vocabulary.allocationSiteToPred(allocationSite);
    
    
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeAllocFlowFunction - " + 
          " method " + program.methodName(method) + " : " + lhs + " = new "  + classOfAllocatedObject);
    
    
    List args = new ArrayList(4);
    args.add(0, "filler");
    args.add(1, lhs);
    args.add(2, classOfAllocatedObject);
    args.add(3, tag);
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionAllocStr, args);
  }
  
  
  
  /*********************************************
   * Arrays
   *********************************************/
  
  public ITVLATransformers.ITVSUnaryTransformer  
  makeArrayAllocFlowFunction(Object method, int lhsRef, Object allocationSite) {
    getClient().debugAssert(program.representsMethod(method));
    getClient().debugAssert(program.representsAllocationSite(allocationSite));
    Object allocatedArrayClass = program.allocationSiteAllocatedClass(allocationSite);
    getClient().debugAssert(program.representsArrayClass(allocatedArrayClass));
    // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsReference(method, lhsRef));
    
    String lhsPred = vocabulary.arrayLocalToPred(method, lhsRef);
    String arrayClassOfAllocatedArrayPred = vocabulary.arrayClassToPred(allocatedArrayClass);
    String tag = vocabulary.allocationSiteToPred(allocationSite);
    
    // TODO Need to add also a summary node for the elemnts
    
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeAllocFlowFunction - " + 
          " method " + program.methodName(method) + " : " + lhsPred + " = new "  + arrayClassOfAllocatedArrayPred);
    
    int dim = program.arrayClassGetDimension(allocatedArrayClass);
    if (1 < dim)
      getClient().tracePrintln(" Array dimentsion is bigger than 1 - not supported! Analysis may return unsound results");
    
    List args = new ArrayList(4);
    args.add(0, "filler");
    args.add(1, lhsPred);
    args.add(2, arrayClassOfAllocatedArrayPred);
    args.add(3, tag);
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionArrayAllocStr, args);		
  }
  
  
  public ITVSUnaryTransformer makeArrayLengthFlowFunction(Object method, int lhs, int rhsRef) {
    getClient().debugAssert(program.representsMethod(method));
    // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsReference(method, lhsRef));
    // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsReference(method, rhsRef));
    
    String rhs = vocabulary.arrayLocalToPred(method, rhsRef);
    
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeArrayLengthFlowFunction - " + 
          " method " + program.methodName(method) + " : " + rhs );
    
    List args = new ArrayList(2);
    args.add(0, "filler");
    args.add(1, rhs);
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionArrayLengthStr, args);
  }
  
  public ITVSUnaryTransformer makeArrayPutFlowFunction(Object method, int lhsRef, int rhsRef) {
    getClient().debugAssert(program.representsMethod(method));
    // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsReference(method, lhsRef));
    // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsReference(method, rhsRef));
    
    String lhs = vocabulary.arrayLocalToPred(method, lhsRef);
    String rhs = vocabulary.refLocalToPred(method, rhsRef);
    
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeArrayPutFlowFunction - " + 
          " method " + program.methodName(method) + " : " + lhs + "[?] = " + rhs );
    
    List args = new ArrayList(3);
    args.add(0, "filler");
    args.add(1, lhs);
    args.add(2, rhs);
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionArrayPutStr, args);
  }
  
  public ITVSUnaryTransformer makeArrayGetFlowFunction(Object method, int lhsRef, int rhsRef) {
    getClient().debugAssert(program.representsMethod(method));
    // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsReference(method, lhsRef));
    // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsReference(method, rhsRef));
    
    String lhs = vocabulary.refLocalToPred(method, lhsRef);
    String rhs = vocabulary.arrayLocalToPred(method, rhsRef);
    
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeArrayGetFlowFunction - " + 
          " method " + program.methodName(method) + " : " + lhs + " = " + rhs + "[?]");
    
    List args = new ArrayList(3);
    args.add(0, "filler");
    args.add(1, lhs);
    args.add(2, rhs);
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionArrayGetStr, args);
  }
  
  
  
  public ITVSUnaryTransformer  makeArrayBooleanPutFlowFunction(Object method, int lhsRef, int rhsRef) {
    getClient().debugAssert(program.representsMethod(method));
    // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsReference(method, lhsRef));
    // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsReference(method, rhsRef));
    
    String lhs = vocabulary.refLocalToPred(method, lhsRef);
    String rhs = vocabulary.refLocalToPred(method, rhsRef);
    
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeArrayBooleanPutStr - " + 
          " method " + program.methodName(method) + " : " + lhs + "[?] = " + rhs );
    
    List args = new ArrayList(3);
    args.add(0, "filler");
    args.add(1, lhs);
    args.add(2, rhs);
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionArrayBooleanPutStr, args);
  }
  
  
  
  public ITVSUnaryTransformer makeArrayBooleanGetFlowFunction(Object method, int lhsRef, int rhsRef) {
    getClient().debugAssert(program.representsMethod(method));
    // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsReference(method, lhsRef));
    // DOMO restriction getClient().debugAssert(program.methodLocalTypeIsReference(method, rhsRef));
    
    String lhs = vocabulary.refLocalToPred(method, lhsRef);
    String rhs = vocabulary.refLocalToPred(method, rhsRef);
    
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeArrayBooleanGetStr - " + 
          " method " + program.methodName(method) + " : " + lhs + " = " + rhs + "[?]" );
    
    List args = new ArrayList(3);
    args.add(0, "filler");
    args.add(1, lhs);
    args.add(2, rhs);
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionArrayBooleanGetStr, args);
  }
  
  
  
  
  /*********************************************
   * Static fields
   *********************************************/
  
  /* (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makeGetStaticBooleanFieldFlowFunction(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  
  public ITVSUnaryTransformer makeGetStaticBooleanFieldFlowFunction(Object method, int lhsBool, Object theStaticField) {
    if (DEBUG_LEVEL > 2) 
      getClient().tracePrintln("TVLAJavaAdapter: makeGetStaticReferenceFieldFlowFunction");  
    
    List args = genGetStaticFieldArgList(method, lhsBool, theStaticField);
    
    if (shouldAssert()) {
      // DOMO restriction - getClient().debugAssert(program.methodLocalTypeIsBoolean(method,lhsBool));
      getClient().debugAssert(program.fieldIsBoolean(theStaticField));
    }
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionGetStaticBooleanFieldStr, args);	  
  }	
  
  public ITVSUnaryTransformer makeGetStaticReferenceFieldFlowFunction(Object method, int lhsRef, Object theStaticField) {
    if (DEBUG_LEVEL > 2) 
      getClient().tracePrintln("TVLAJavaAdapter: makeGetStaticReferenceFieldFlowFunction");  
    
    List args = genGetStaticFieldArgList(method, lhsRef, theStaticField);
    
    if (shouldAssert()) {
      // DOMO restriction - getClient().debugAssert(program.methodLocalTypeIsReference(lhsRef));
      boolean v = program.fieldIsReference(theStaticField);
      getClient().debugAssert(v);
    }
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionGetStaticReferenceFieldStr, args);	  
  }
  
  private List genGetStaticFieldArgList(Object method, int lhsT, Object theStaticField) {
    if (shouldAssert()) {
      getClient().debugAssert(program.representsMethod(method));
      getClient().debugAssert(program.representsField(theStaticField));
      getClient().debugAssert(program.fieldIsStatic(theStaticField));
    }
    
    Object fieldDeclaredInClass = program.fieldDeclaredInClass(theStaticField);			
    // This "logic" is needed because of the problem to retrice type of locals
    String lhs = null; 
    if (program.fieldIsReference(theStaticField)) {
      lhs = vocabulary.refLocalToPred(method, lhsT);
    }
    else if (program.fieldIsBoolean(theStaticField)) {
      lhs = vocabulary.boolLocalToPred(method, lhsT);
    }
    else {
      getClient().debugAssert(false, "Unknonw field type " + theStaticField);
    }
    
    String className = vocabulary.classToPred(fieldDeclaredInClass);
    String fieldId = vocabulary.staticFieldToPred(theStaticField);
    
    if (DEBUG_LEVEL > 2) {
      getClient().tracePrintln("TVLAJavaAdapter: genGetStaticFieldArgList - " + 
          " method " + program.methodName(method) + " : " + 
          lhs + " = " + className + "." +  fieldId);
    }
    
    List args = new ArrayList(4);
    args.add(0, "filler");
    args.add(1, lhs);
    args.add(2, className);
    args.add(3, fieldId);			
    
    return args;
  }
  
  
  
  /* (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makePutStaticBooleanFieldFlowFunction(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  public ITVLATransformers.ITVSUnaryTransformer makePutStaticBooleanFieldFlowFunction(Object theStaticField, Object method, int rhsBool) {
    if (DEBUG_LEVEL > 2) 
      getClient().tracePrintln("TVLAJavaAdapter: makePutStaticBooleanFieldFlowFunction"); 
    
    List args = genPutStaticFieldArgList(theStaticField, method, rhsBool, false);
    
    if (shouldAssert()) {	
      getClient().debugAssert(program.fieldIsBoolean(theStaticField));
      // DOmo restriction - getClient().debugAssert(program.methodLocalTypeIsBoolean(method, rhsBool));
    }
    
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionPutStaticBooleanFieldStr, args);	  
  }
  
  
  /* (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makePutStaticReferenceFieldFlowFunction(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  public ITVSUnaryTransformer makePutStaticReferenceFieldFlowFunction(Object theStaticField, Object method, int rhsRef) {
    if (DEBUG_LEVEL > 2) 
      getClient().tracePrintln("TVLAJavaAdapter: makePutStaticBooleanFieldFlowFunction"); 
    
    List args = genPutStaticFieldArgList(theStaticField, method, rhsRef, false);
    
    if (shouldAssert()) {	
      getClient().debugAssert(program.fieldIsReference(theStaticField));
      // Domo asssertion - getClient().debugAssert(program.methodLocalTypeIsReference(method, rhsRef));
    }
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionPutStaticReferenceFieldStr, args);	  
  }
  
  public ITVSUnaryTransformer makeNullifyStaticReferenceFieldFlowFunction(
      Object theStaticField, Object method) {
    if (DEBUG_LEVEL > 2) 
      getClient().tracePrintln("TVLAJavaAdapter: makeNullifyStaticReferenceFieldFlowFunction"); 
    
    List args = genPutStaticFieldArgList(theStaticField, method, -1, true);
    
    if (shouldAssert()) {   
      getClient().debugAssert(program.fieldIsReference(theStaticField));
    }
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionNullifyStaticReferenceFieldStr, args);    
  }
  
  
  public List genPutStaticFieldArgList(Object theStaticField, Object method, int rhsT, boolean toFixedValue) {
    if (shouldAssert()) {	
      getClient().debugAssert(theStaticField != null);			
      getClient().debugAssert(program.representsField(theStaticField));
      getClient().debugAssert(program.fieldIsStatic(theStaticField));
      getClient().debugAssert(program.representsMethod(method));
    }
    
    Object fieldDeclaredInClass = program.fieldDeclaredInClass(theStaticField);		
    
    String className = vocabulary.classToPred(fieldDeclaredInClass);
    String fieldId = vocabulary.staticFieldToPred(theStaticField);
    
    if (toFixedValue) {
      List args = new ArrayList(3);
      args.add(0, "filler");
      args.add(1, className);
      args.add(2, fieldId);

      if (DEBUG_LEVEL > 2) {
        getClient().tracePrintln("TVLAJavaAdapter: genPutStaticFieldArgList - " + 
            " method " + program.methodName(method) + " : " + 
            className + "." +  fieldId + " = [fixed value]");
      }    
      
      return args;
    }

    List args = new ArrayList(4);
    args.add(0, "filler");
    args.add(1, className);
    args.add(2, fieldId);
    
    // This "logic" is needed because of the problem to retrive type of locals
    String rhs = null; 
    if (program.fieldIsReference(theStaticField)) {
      rhs = vocabulary.refLocalToPred(method, rhsT);
    }
    else if (program.fieldIsBoolean(theStaticField)) {
      rhs = vocabulary.boolLocalToPred(method, rhsT);
    }
    else {
      getClient().debugAssert(false, "Unknonw field type " + theStaticField);
    }
    
    if (DEBUG_LEVEL > 2) {
      getClient().tracePrintln("TVLAJavaAdapter: genPutStaticFieldArgList - " + 
          " method " + program.methodName(method) + " : " + 
          className + "." +  fieldId + " = " + rhs);
    }    
    
    args.add(3, rhs);
    
    return args;	  
  }
  


  

  
  /*********************************************
   * Instance fields
   *********************************************/
  
  /* (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makeGetInstanceBooleanFieldFlowFunction(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  public ITVSUnaryTransformer makeGetInstanceBooleanFieldFlowFunction(Object method, int lhsBool, int rhsRef, Object theInstanceField) {
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeGetInstanceBooleanFieldFlowFunction");
    
    List args = genGetInstanceFieldArgList(method, lhsBool, rhsRef, theInstanceField);
    
    if (shouldAssert()) {	
      getClient().debugAssert(program.fieldIsBoolean(theInstanceField));
      // DOMO restriction - getClient().debugAssert(program.methodLocalTypeIsBoolean(method, lhsBool));
    }
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionGetInstanceBooleanFieldStr, args);	  
  }
  
  /* (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makeGetInstanceReferenceFieldFlowFunction(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  public ITVLATransformers.ITVSUnaryTransformer makeGetInstanceReferenceFieldFlowFunction(Object method, int lhsRef, int rhsRef, Object theInstanceField) {
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeGetInstanceReferenceFieldFlowFunction");
    
    List args = genGetInstanceFieldArgList(method, lhsRef, rhsRef, theInstanceField);
    
    if (shouldAssert()) {	
      getClient().debugAssert(program.fieldIsReference(theInstanceField));
      // DOMO restriction - getClient().debugAssert(program.methodLocalTypeIsBoolean(method, lhsBool));
    }
    
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionGetInstanceReferenceFieldStr, args);	  
  }


  public List genGetInstanceFieldArgList(Object method, int lhsT, int rhsRef, Object theInstanceField) {
    if (shouldAssert()) {
      getClient().debugAssert(program.representsMethod(method));
      getClient().debugAssert(program.representsField(theInstanceField));
      getClient().debugAssert(! program.fieldIsStatic(theInstanceField));
      // DOMO restriction - getClient().debugAssert(program.methodLocalTypeIsReference(method, rhsRef));
    }
    
    String fieldId = vocabulary.instanceFieldToPred(theInstanceField);
    String rhs = vocabulary.refLocalToPred(method, rhsRef);
    
    // This "logic" is needed because of the problem to retrice type of locals
    String lhs = null;		
    if (program.fieldIsReference(theInstanceField)) {
      lhs = vocabulary.refLocalToPred(method, lhsT);
    }
    else if (program.fieldIsBoolean(theInstanceField)) {
      lhs = vocabulary.boolLocalToPred(method, lhsT);
    }
    else {
      getClient().debugAssert(false, "Unknonw field type " + theInstanceField);
    }
    
    
    if (DEBUG_LEVEL > 2) {
      getClient().tracePrintln(
          "TVLAJavaAdapter: genGetInstanceFieldArgList - " + 
          " method " + program.methodName(method) + " : " + 
          lhs + " = " + rhs + " . " +  fieldId);
    }
    
    List args = new ArrayList(4);
    args.add(0, "filler");
    args.add(1, lhs);
    args.add(2, rhs);
    args.add(3, fieldId);
    
    return args;
  }
  
  
  
  /* (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makePutInstanceBooleanFieldFlowFunction(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  public ITVSUnaryTransformer makePutInstanceBooleanFieldFlowFunction(Object method, int lhsRef, Object theInstanceField, int rhsBool) {
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makePutInstanceBooleanFieldFlowFunction");
    
    List args = genPutInstanceFieldArgList(method, lhsRef, theInstanceField, rhsBool);
    
    if (shouldAssert()) {	
      getClient().debugAssert(program.fieldIsBoolean(theInstanceField));
      // DOMO restriction - getClient().debugAssert(program.methodLocalTypeIsBoolean(method,rhsBool));
    }
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionPutInstanceBooleanFieldStr, args);	  
  }
  
  /**
   * Statement: y.f = x
   * Instanitate action: Assign_Field_with_Local(PT, y, f, x)
   *  (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makePutInstanceReferenceFieldFlowFunction(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  public ITVSUnaryTransformer makePutInstanceReferenceFieldFlowFunction(Object method, int lhsRef, Object theInstanceField, int rhsRef) {
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makePutInstanceReferenceFieldFlowFunction");
    
    if (shouldAssert()) {	
      getClient().debugAssert(program.fieldIsReference(theInstanceField));
      // DOMO restriction - getClient().debugAssert(program.methodLocalTypeIsBoolean(method,rhsBool));
    }
    
    ITVSUnaryTransformer[] transformers = new ITVSUnaryTransformer[2];
    
    
    List argsKiller = genPutInstanceFieldArgList(method, lhsRef, theInstanceField);
    transformers[0] = tvlaapi.getUnaryTransformer(IntraConstants.actionNullifyInstanceReferenceFieldStr, argsKiller);
    
    List argsSetter = genPutInstanceFieldArgList(method, lhsRef, theInstanceField, rhsRef);
    transformers[1] = tvlaapi.getUnaryTransformer(IntraConstants.actionPutInstanceReferenceFieldStr, argsSetter);
    
    return tvlaapi.composedTransformers(transformers, 2, "makePutInstanceReferenceFieldFlowFunction");
  }
  
  public ITVSUnaryTransformer makeNullifyInstanceReferenceFieldFlowFunction(
      Object method, int lhsRef, Object theInstanceField) {
    if (DEBUG_LEVEL > 2)
      getClient().tracePrintln("TVLAJavaAdapter: makeNullifyInstanceReferenceFieldFlowFunction");
    
    List args = genPutInstanceFieldArgList(method, lhsRef, theInstanceField);
    
    if (shouldAssert()) {   
      getClient().debugAssert(program.fieldIsReference(theInstanceField));
      // DOMO restriction - getClient().debugAssert(program.methodLocalTypeIsBoolean(method,rhsBool));
    }
    
    return tvlaapi.getUnaryTransformer(IntraConstants.actionNullifyInstanceReferenceFieldStr, args);    
 }
  
  
  
  public List genPutInstanceFieldArgList(Object method, int lhsRef, Object theInstanceField) {
    return genPutInstanceFieldArgList(method, lhsRef, theInstanceField, false, -1);
  }
  
  public List genPutInstanceFieldArgList(Object method, int lhsRef, Object theInstanceField, int rhsT) {
    return genPutInstanceFieldArgList(method, lhsRef, theInstanceField, true, rhsT);
  }
  
  public List genPutInstanceFieldArgList(Object method, int lhsRef, Object theInstanceField, boolean hasRhs, int rhsT) {
    if (shouldAssert()) {
      getClient().debugAssert(program.representsMethod(method));
      getClient().debugAssert(program.representsField(theInstanceField));
      getClient().debugAssert(! program.fieldIsStatic(theInstanceField));
      // DOMO restriction - getClient().debugAssert(program.methodLocalTypeIsReference(method, lhsRef));
    }
    
    String fieldId = vocabulary.instanceFieldToPred(theInstanceField);
    String lhs = vocabulary.refLocalToPred(method, lhsRef);		
    
    List args = new ArrayList(4);
    args.add(0, "filler");
    args.add(1, lhs);
    args.add(2, fieldId);
    
    String rhs = null;
    if (hasRhs){
      // This "logic" is needed because of the problem to retrive type of locals
      if (program.fieldIsReference(theInstanceField)) {
        rhs = vocabulary.refLocalToPred(method, rhsT);
      }
      else if (program.fieldIsBoolean(theInstanceField)) {
        rhs = vocabulary.boolLocalToPred(method, rhsT);
      }
      else {
        getClient().debugAssert(false, "Unknonw field type " + theInstanceField);
      }
      args.add(3, rhs);
    }
    
    
    if (DEBUG_LEVEL > 2) {
      getClient().tracePrintln(
          "TVLAJavaAdapter: genPutInstanceFieldArgList - " + 
          " method " + program.methodName(method) + " : " + 
          lhs + " . " +  fieldId + " = " + (hasRhs? rhs : "...constant... "));
    }
    
    return args;
  }	
  
  
  
   
  /**********************************************************
   * Helper classes
   **********************************************************/
  
  protected ITVLAAPIDebuggingServices  getClient() {
    return client;
  }
  
  
  protected boolean  shouldAssert() {
    return shouldAssert;
  }
  
}
