package tvla.analysis.interproc.api.javaanalysis.transformers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaVocabulary;
import tvla.api.AbstractTVLAAPI;
import tvla.api.ITVLATransformers;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaAnalsyisEnvironmentServicesPovider;
import tvla.api.ITVLATransformers.ITVSUnaryTransformer;


/**
 * Generates actions for analyses that use TVLA for intraprocedural statement and
 * fall back to soe sound abstraction of the memory state after a call
 * but are capable to record local caller information which the callee 
 * could not have modified.
 * 
 * For example, the callee cannot modify the values / nullness of local variables
 *
 * @author noam rinetzky
 */

public class InterPartialHeapsTransformersAbstractFactory extends  AbstratInterTransformersAbstractFactory {
  protected final CommonCall2EntryParamBindingTransformersFactory c2eBinder;
  protected final CommonExit2ReturnParamBindingTransformersFactory x2rBinder;
  
  public InterPartialHeapsTransformersAbstractFactory(
      AbstractTVLAAPI tvlaAPI, 
      ITVLAJavaAnalsyisEnvironmentServicesPovider environmentServicesProvider,
      IJavaVocabulary vocabulary) {
    super(tvlaAPI, environmentServicesProvider, vocabulary);

    c2eBinder = new CommonCall2EntryParamBindingTransformersFactory(tvlaAPI, this.program, vocabulary);
    x2rBinder = new CommonExit2ReturnParamBindingTransformersFactory(tvlaAPI, this.program, vocabulary);
  }
  
  /*********************************************
   * Interprocedural transformers (Actions)
   *********************************************/
  protected static class InterPartialHeapsConstants  {
    //public final static String actionStagedJVMCallStr  = "JVM_Call_Partial";                                        // call from the JVM, i.e., invoking main
    public final static String actionStagedCallStr  = "Call_Partial";                                               // ()
    public final static String actionEntryStr  = "Method_Entry_Partial";                                            // ()       
    public final static String actionExitStr  = "Method_Exit_Partial";                                              // ()   

    // The return is comprised of 2 operations 
    public final static String actionPreserveLocalIntoStr = "Preserve"; 
    public final static String actionReturnStr = "Ret_Partial";                                                     // ()

    // Convineient array
    public final static String[] allInterPartialHeapsActionssStrs = new String[] {
      //actionStagedJVMCallStr,
      actionStagedCallStr, 
      actionEntryStr,
      actionExitStr, 
      actionPreserveLocalIntoStr,
      actionReturnStr,        
    };
    
    protected static Collection getInterPartialHeapsActions() {
      return Arrays.asList(InterPartialHeapsConstants.allInterPartialHeapsActionssStrs);
    }
  }
   
  protected void getActionsNames(Set actionNames) {
    super.getActionsNames(actionNames);
    c2eBinder.getActionsNames(actionNames);
    x2rBinder.getActionsNames(actionNames);
    actionNames.addAll(InterPartialHeapsConstants.getInterPartialHeapsActions());
  }  
  
  

  /****************************************************************
   * param binding - delegated
   ****************************************************************/

  /** 
   * 1. Sq' = makeCallerPreCallTransformer[y=p(x)](Sq)
   */
  
  public ITVSUnaryTransformer makeCallerPreCallTransformer(Object caller, Object invocation) {
    return c2eBinder.makeCallerPreCallTransformer(
        null, 
        null, 
        caller, 
        invocation);
  }
  
  
  /** 3. Sp = makeCalleeEntryTransformer[y=p(x)](Sp') 
   * 
   * Note the call to getEntryAction in order to get the action to do at the entry to the 
   * callee after the transfer-parmaeter to formal-parameter binding is over
 
   * (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makeCalleeEntryTransformer(tvla.api.TVLAJavaAdapterImpl.MethodCallFrame)
   */
   
 
  public ITVSUnaryTransformer makeCalleeEntryTransformer(Object callee) {
    return c2eBinder.makeCalleeEntryTransformer(null, getEntryAction(), callee);
  }
 
  

  /** 4. Sp'' = makeCalleeExitTransformer[y=p(x)](Sp')
   * (non-Javadoc)
   * 
   * Note the call to getExitAction in order to get the action to do at the exit to the 
   * callee after the return value is set

   * @see tvla.api.TVLAJavaAdapter#makeCalleeExitTransformer(tvla.api.TVLAJavaAdapterImpl.MethodExitFrame)
   */

  public ITVSUnaryTransformer makeCalleeExitTransformer(Object callee) {
    return c2eBinder.makeCalleeExitTransformer(getExitAction(), callee);
  }


  
  
  /********************************************************************
   * Set / Get the return value
   ********************************************************************/


  // return v;
  public ITVSUnaryTransformer makeReturnValueFlowFunction(Object method, int retValIndex) {
    return x2rBinder.makeReturnValueFlowFunction(method, retValIndex);
  }

  
  // 6. Sq''' = makeCallerPostCallTransformer(Sq'')
  /* (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makeCallerPostCallTransformer(tvla.api.TVLAJavaAdapterImpl.MethodAssignementDesc, tvla.api.TVLAJavaAdapterImpl.MethodExitFrame)
   */
  
  public ITVSUnaryTransformer makeCallerPostCallTransformer(Object caller, Object invocation) {
    return x2rBinder.makeCallerPostCallTransformer(caller, invocation);
  }


  
  /****************************************************************
   * Interprocedural - analysis specific
   ****************************************************************/
  
  // 2. Sp' = makeCallerToCalleeTransformer[y=p(x)](Sq')
  /* (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makeCallToEntryTransformer(tvla.api.TVLAJavaAdapterImpl.MethodInvocationDesc, tvla.api.TVLAJavaAdapterImpl.MethodCallFrame)
   */
  public ITVLATransformers.ITVSUnaryTransformer makeCallToEntryTransformer(
      Object caller, Object invocation) 
  { 
    getClient().debugAssert(program.representsInvocation(invocation));
    getClient().debugAssert(program.representsMethod(caller));
    
    if (DEBUG_LEVEL > 2) 
      getClient().tracePrintln("InterPartialHeapsConstants: makeCallToEntryTransformer " +
          " caller: " + program.methodName(caller) + 
          " callee: " + program.invocationCalleeName(invocation));
    

    /*
    String action = 
      program.methodIsJVMMain(caller) ?
        InterPartialHeapsConstants.actionStagedJVMCallStr : 
          InterPartialHeapsConstants.actionStagedCallStr;
    */
    String action = InterPartialHeapsConstants.actionStagedCallStr;
    
    return tvlaapi.getUnaryStagedTransformer(callToEntryStagedCombiner, action, stub);
  }

  /** 3 specific
   * Get the action to do at the entry to the callee after the transfer-parmaeter to 
   * formal-parameter binding is over
   * @return
   */  
  protected ITVLATransformers.ITVSUnaryTransformer getEntryAction() {
     return tvlaapi.getUnaryTransformer(InterPartialHeapsConstants.actionEntryStr, stub);
  }


  /**  4.  specific
   * Get the action to do at the exit to the callee after the return value is set
   * @return
   */  
  protected ITVLATransformers.ITVSUnaryTransformer getExitAction() {
    String exiter = InterPartialHeapsConstants.actionExitStr;
    
    return tvlaapi.getUnaryTransformer(exiter, stub);
 }

  /** 5. Sq'' = makeCalleeToCallerTransformer[y=p(x)](Sq',Sp'')
   * (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makeExitToReturnTransformer(tvla.api.TVLAJavaAdapterImpl.MethodAssignementDesc, tvla.api.TVLAJavaAdapterImpl.MethodExitFrame)
   */
  public ITVLATransformers.ITVSBinaryTransformer makeCallAndExitToReturnBinaryTransformer(
      Object caller, Object invocation)
  { 
    getClient().debugAssert(program.representsMethod(caller));
    getClient().debugAssert(program.representsInvocation(invocation));
    
    
    if (DEBUG_LEVEL > 2) {
      getClient().tracePrintln("InterPartialHeapsConstants: makeCallAndExitToReturnBinaryTransformer " +
          " caller: " + program.methodName(caller) + 
          " callee: " + program.invocationCalleeName(invocation));
    }
    
     return tvlaapi.getBinaryStagedTransformer(
        callAndExitStagedCombiner, InterPartialHeapsConstants.actionPreserveLocalIntoStr, stub,
        callAndExitCombiner, InterPartialHeapsConstants.actionReturnStr, stub);
  }


}
