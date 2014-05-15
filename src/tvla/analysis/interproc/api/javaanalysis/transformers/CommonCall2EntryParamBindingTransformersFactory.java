package tvla.analysis.interproc.api.javaanalysis.transformers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaVocabulary;
import tvla.analysis.interproc.api.utils.TVLAAPIAssert;
import tvla.analysis.interproc.api.utils.TVLAAPIDebugControl;
import tvla.analysis.interproc.api.utils.TVLAAPITrace;
import tvla.api.AbstractTVLAAPI;
import tvla.api.ITVLATransformers;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModelerServices;
import tvla.api.ITVLATransformers.ITVSUnaryTransformer;


/**
 * "Standard treatment" for binindg of parameters in a call to a procedure
 */

public class CommonCall2EntryParamBindingTransformersFactory
{
  public final static int DEBUG_LEVEL = TVLAAPIDebugControl.getDebugLevel(6);
  
  protected final AbstractTVLAAPI tvlaapi; 
  protected final ITVLAJavaProgramModelerServices program; 
  protected final IJavaVocabulary vocabulary;
  
  public CommonCall2EntryParamBindingTransformersFactory(
      AbstractTVLAAPI tvlaAPI, 
      ITVLAJavaProgramModelerServices programModeler, 
      IJavaVocabulary vocabulary) {
    this.tvlaapi = tvlaAPI;
    this.program = programModeler;
    this.vocabulary = vocabulary;
  }

  /**********************************************************************
   * Standard parameter binding actions
   **********************************************************************/
  protected static class Call2EntryParamBindingConstants  {
    // Actual parameter ==> formal parameters
    //// Caller side
    public final static String actionCopyBooleanIntoTransferPrameterStr = "boolean_Set_Transfer_Param";     // (PT, formal, actual) C[formal] = actual
    public final static String actionCopyReferenceIntoTransferPrameterStr = "Set_Transfer_Param";           // (PT, formal, actual) C[formal] = actual
        
    //// Callee Side
    public final static String actionCopyTransferPrameterIntoBooleanStr = "boolean_Use_Transfer_Param";     // (PT, actualThis, formalThis) actualThis = C[formalThis] 
    public final static String actionCopyTransferPrameterIntoReferenceStr = "Use_Transfer_Param";           // (PT, formal, actual) actual = C[formal]

     
    public final static String[] allCall2EntryParamBinidingActionsStrs = new String[] {
      actionCopyBooleanIntoTransferPrameterStr,  
      actionCopyReferenceIntoTransferPrameterStr,
      actionCopyTransferPrameterIntoBooleanStr, 
      actionCopyTransferPrameterIntoReferenceStr, 
     };

    protected static Collection  getCall2EntryParameterBindingActionsNames() {
      return Arrays.asList(Call2EntryParamBindingConstants.allCall2EntryParamBinidingActionsStrs);
    }
  }
  
  
  protected void getActionsNames(Set actionNames) {
     actionNames.addAll(Call2EntryParamBindingConstants.getCall2EntryParameterBindingActionsNames());
  }  
  
  /**********************************************************************
   * Utilities
   **********************************************************************/
  
  /******************************************************************************
   * Parameter binding actions
   ******************************************************************************/
  
  /** 
   * 1. Sq' = makeCallerPreCallTransformer[y=p(x)](Sq)
   */
  public ITVLATransformers.ITVSUnaryTransformer makeCallerPreCallTransformer(
      final ITVLATransformers.ITVSUnaryTransformer optionalFirstTransformer,
      final ITVLATransformers.ITVSUnaryTransformer optionalCallerPreCallLast,
      final Object caller, 
      final Object invocation) 
  {     
    if (TVLAAPIAssert.ASSERT) {
      TVLAAPIAssert.debugAssert(program.representsInvocation(invocation));
      TVLAAPIAssert.debugAssert(program.representsMethod(caller));
    }
    
    if (DEBUG_LEVEL > 2)
      TVLAAPITrace.tracePrintln("CommonCall2EntryParamBindingTransformersFactory: makeCallerPreCallTransformer " + 
          " caller: " + program.methodName(caller) + 
          " callee: " + program.invocationCalleeName(invocation));
    
    
    
    ITVLATransformers.ITVSUnaryTransformer[] transformers = new ITVLATransformers.ITVSUnaryTransformer[2 + program.invocationNumberOfParameters(invocation)];
    int effectiveTransformers = 0;
    
    if (optionalFirstTransformer != null) {
      transformers[0] = optionalFirstTransformer;
      effectiveTransformers = 1;
    }
    
    for(int i=0; i < program.invocationNumberOfParameters(invocation); i++) {
      List binding = new ArrayList(3);
      binding.add(0, "filler");
      
      String formal, actual, binder;                    
      if (program.invocationParametersTypeIsBoolean(invocation, i)) {
        formal = vocabulary.boolTransfer(i);
        actual = vocabulary.boolLocalToPred(caller, program.invocationActualParameterIndex(invocation, i));
        binder = Call2EntryParamBindingConstants.actionCopyBooleanIntoTransferPrameterStr;
      }
      else if (program.invocationParametersTypeIsReference(invocation, i)) {
        formal = vocabulary.refTransfer(i);
        actual = vocabulary.refLocalToPred(caller, program.invocationActualParameterIndex(invocation, i));                          
        binder = Call2EntryParamBindingConstants.actionCopyReferenceIntoTransferPrameterStr;
      }
      else {
        formal = actual = binder = null; // appease Eclipse 
        TVLAAPITrace.tracePrintln(" makeCallerPreCallTransformer: type of parameter " + i + " not tracked - ignoring formal parameter");                         
        continue;
      }
      
      binding.add(1, formal);
      binding.add(2, actual);
      
      transformers[effectiveTransformers++] = tvlaapi.getUnaryTransformer(binder, binding); 
    }
    
    if (optionalCallerPreCallLast != null)
      transformers[effectiveTransformers++] = optionalCallerPreCallLast;
    
    
    ITVLATransformers.ITVSUnaryComposedTransformer composedTransformer =  
      tvlaapi.composedTransformers(
        transformers, 
        effectiveTransformers, 
        "CallerPreCallTransformer: caller " + program.methodName(caller) + 
        " callee: " + program.invocationCalleeName(invocation));

    return composedTransformer.simplify();

  }
  
  /** 3. Sp = makeCalleeEntryTransformer[y=p(x)](Sp') 
   * 
   * Note the call to getEntryAction in order to get the action to do at the entry to the 
   * callee after the transfer-parmaeter to formal-parameter binding is over
 
   * (non-Javadoc)
   * @param optionalCalleeEntryFirst TODO
   * @see tvla.api.TVLAJavaAdapter#makeCalleeEntryTransformer(tvla.api.TVLAJavaAdapterImpl.MethodCallFrame)
   */
  public ITVLATransformers.ITVSUnaryTransformer makeCalleeEntryTransformer(
      final ITVSUnaryTransformer optionalCalleeEntryFirst, 
      final ITVLATransformers.ITVSUnaryTransformer optionalCalleeEntryLast, 
      final Object callee) 
  { 
    if (TVLAAPIAssert.ASSERT) 
      TVLAAPIAssert.debugAssert(program.representsMethod(callee));
    
    if (DEBUG_LEVEL > 2) {
      TVLAAPITrace.tracePrintln("CommonCall2EntryParamBindingTransformersFactory: makeCalleeEntryTransformer " + program.methodName(callee));
    }
    
    int numOfOptionals = (optionalCalleeEntryFirst!= null ? 1 : 0) + (optionalCalleeEntryLast != null ? 1 : 0);
     
    ITVLATransformers.ITVSUnaryTransformer[] transformers = 
      new ITVLATransformers.ITVSUnaryTransformer[program.methodNumberOfParameters(callee) + numOfOptionals];
   
    int effectiveTransformers = 0;
    
    if (optionalCalleeEntryFirst != null) {
      transformers[0] = optionalCalleeEntryFirst;
      effectiveTransformers = 1;
    }
    
    for(int paramIndex=0; paramIndex < program.methodNumberOfParameters(callee); paramIndex++) {
      List binding = new ArrayList(3);
      binding.add(0, "filler");
      
      int localIndex = program.methodParameterNumberToLocalIndex(callee, paramIndex);
      
      String formal = null, transfer = null, popper = null; // appease Eclispe
      
      if (program.methodLocalTypeIsBoolean(callee, localIndex)) {
        transfer = vocabulary.boolTransfer(paramIndex);
        //int formalIndex = program.methodParameterNumberToLocalIndex(callee, paramIndex);
        formal = vocabulary.boolLocalToPred(callee, localIndex );
        popper = Call2EntryParamBindingConstants.actionCopyTransferPrameterIntoBooleanStr;
      }
      else if (program.methodLocalTypeIsReference(callee, localIndex)) {
        transfer = vocabulary.refTransfer(paramIndex);
        //int formalIndex = program.methodParameterNumberToLocalIndex(callee, paramIndex);
        formal = vocabulary.refLocalToPred(callee, localIndex);
        popper = Call2EntryParamBindingConstants.actionCopyTransferPrameterIntoReferenceStr;
      }
      else {
        if (2 < DEBUG_LEVEL) {
          TVLAAPITrace.tracePrintln("makeCalleeEntryTransformer: type of parameter " + paramIndex + " not tracked - ignoring formal parameter");                         
          continue;
        }
      }
      
      binding.add(1, formal);
      binding.add(2, transfer);
      transformers[effectiveTransformers++] = tvlaapi.getUnaryTransformer(popper, binding); 
    }
    
    if (optionalCalleeEntryLast != null)
      transformers[effectiveTransformers++] = optionalCalleeEntryLast;
    
    ITVLATransformers.ITVSUnaryComposedTransformer composedTransformer = 
      tvlaapi.composedTransformers(
          transformers, 
          effectiveTransformers, 
          "makeCalleeEntryTransformer callee " + program.methodName(callee));
    
    return composedTransformer.simplify();
  }    

  
  
  /** 4. Sp'' = makeCalleeExitTransformer[y=p(x)](Sp')
   * (non-Javadoc)
   * 
   * Note the call to getExitAction in order to get the action to do at the exit to the 
   * callee after the return value is set

   * @see tvla.api.TVLAJavaAdapter#makeCalleeExitTransformer(tvla.api.TVLAJavaAdapterImpl.MethodExitFrame)
   */
  public ITVLATransformers.ITVSUnaryTransformer makeCalleeExitTransformer(
      final ITVLATransformers.ITVSUnaryTransformer optionalCalleeExitLast,
      final Object callee) 
  { 
    if (TVLAAPIAssert.ASSERT) 
      TVLAAPIAssert.debugAssert(program.representsMethod(callee));
    
    if (DEBUG_LEVEL > 2)
      TVLAAPITrace.tracePrintln("TVLAJavaAdapter: makeCalleeExitTransformer " + program.methodName(callee));
        
     
    ITVLATransformers.ITVSUnaryTransformer transformers[] = new ITVLATransformers.ITVSUnaryTransformer[program.methodNumberOfParameters(callee) + 1];
    int effectiveBindings = 0;
    for(int paramIndex=0; paramIndex< program.methodNumberOfParameters(callee); paramIndex++) {
      List binding = new ArrayList(3);
      binding.add(0, "filler");
      
      int localIndex = program.methodParameterNumberToLocalIndex(callee, paramIndex);
      
      String formal, transfer, binder;                  
      if (program.methodLocalTypeIsBoolean(callee, localIndex)) {
        transfer = vocabulary.boolTransfer(paramIndex);
        //int formalIndex = program.methodParameterNumberToLocalIndex(callee, paramIndex);
        formal = vocabulary.boolLocalToPred(callee, localIndex);
        binder = Call2EntryParamBindingConstants.actionCopyBooleanIntoTransferPrameterStr;
      }
      else if (program.methodLocalTypeIsReference(callee, localIndex)) {
        transfer = vocabulary.refTransfer(paramIndex);
        //int formalIndex = program.methodParameterNumberToLocalIndex(callee, paramIndex);
        formal = vocabulary.refLocalToPred(callee, localIndex);                            
        binder = Call2EntryParamBindingConstants.actionCopyReferenceIntoTransferPrameterStr;
      }
      else {
        if (2 < DEBUG_LEVEL) 
          TVLAAPITrace.tracePrintln(" makeCalleeExitTransformer: type of parameter " + paramIndex + " not tracked - ignoring formal parameter");
        continue;   
      }
      
      binding.add(1, transfer);
      binding.add(2, formal);
      
      transformers[effectiveBindings++] = CommonCall2EntryParamBindingTransformersFactory.this.tvlaapi.getUnaryTransformer(binder, binding); 
    }
    
    if (optionalCalleeExitLast != null)
      transformers[effectiveBindings++] = optionalCalleeExitLast;
    
    ITVLATransformers.ITVSUnaryComposedTransformer composedTransformer = tvlaapi.composedTransformers(transformers, effectiveBindings, "makeCalleeExitTransformer");
    return composedTransformer.simplify();
  }
}
