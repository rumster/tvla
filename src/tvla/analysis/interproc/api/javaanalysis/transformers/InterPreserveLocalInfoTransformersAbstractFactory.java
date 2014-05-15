package tvla.analysis.interproc.api.javaanalysis.transformers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
// This class hierarchy is deprecated!  should have an object that does the reutn value
public class InterPreserveLocalInfoTransformersAbstractFactory extends  AbstratInterTransformersAbstractFactory { // AbstractReturnValueTransformersAbstractFactory {
  protected final CommonExit2ReturnParamBindingTransformersFactory x2rBinder;

  public InterPreserveLocalInfoTransformersAbstractFactory(
      AbstractTVLAAPI tvlaAPI, 
      ITVLAJavaAnalsyisEnvironmentServicesPovider environmentServicesProvider,
      IJavaVocabulary vocabulary) {
    super(tvlaAPI, environmentServicesProvider, vocabulary);
    x2rBinder = new CommonExit2ReturnParamBindingTransformersFactory(tvlaAPI, this.program, vocabulary);
  }
  
  /*********************************************
   * Interprocedural transformers (Actions)
   *********************************************/
  protected static class InterPreserveLocalInfoConstants  {
    // Caller Side
    public final static String actionPreserveLocalIntoStr = "Preserve"; 
    
    // Callee Side
    public final static String actionEntryWithThisStr = "Entry_With_This"; 

    // Convineient array
    public final static String[] allPreserveLocalInfoActionsStrs = new String[] {
      actionPreserveLocalIntoStr,
      actionEntryWithThisStr      
    };
    
    protected static Collection getPreserveLocalInfoActions() {
      return Arrays.asList(InterPreserveLocalInfoConstants.allPreserveLocalInfoActionsStrs);
    }
  }
   
  protected void getActionsNames(Set actionNames) {
    super.getActionsNames(actionNames);
    x2rBinder.getActionsNames(actionNames);
    actionNames.addAll(InterPreserveLocalInfoConstants.getPreserveLocalInfoActions());
  }  
  
  /****************************************************************
   * Interprocedural - combine call-state with method tvs
   ****************************************************************/
 
  /**
   *  3. Sp = makeCalleeEntryTransformer[y=p(x)](Sp')
   *  We make no assumptions on the callee entry state, but this:
   *  if the invoked method is not static (i.e., has a this parameter)
   *  than this is not null
   */
  public ITVLATransformers.ITVSUnaryTransformer makeCalleeEntryTransformer(final Object callee) {
    getClient().debugAssert(program.representsMethod(callee));
    int thisIndex = program.methodGetThis(callee);
    
    if (DEBUG_LEVEL > 2) {
      getClient().tracePrintln("InterPreserveLocalInfoTransformersAbstractFactory: makeCalleeEntryTransformer " +
          " callee: " + program.methodName(callee) + " " +
          ((thisIndex < 0) ?  "has no this - nothing to do" : "has this - using not nnulness ")); 
    }
  
    if (thisIndex < 0)
        return null;
    
    String thisStr = vocabulary.refLocalToPred(callee, thisIndex);
    
    List args = new ArrayList(2);
    args.add(0, "filler");
    args.add(1, thisStr);
    
    return tvlaapi.getUnaryTransformer(InterPreserveLocalInfoConstants.actionEntryWithThisStr, args);
 }  
  
  /**
   *  5. Sq'' = makeCalleeToCallerTransformer[y=p(x)](Sq',Sp'')
   */
  public ITVLATransformers.ITVSBinaryTransformer makeCallAndExitToReturnBinaryTransformer(
      Object caller, Object invocation){
    getClient().debugAssert(program.representsMethod(caller));
    getClient().debugAssert(program.representsInvocation(invocation));
    
    
    if (DEBUG_LEVEL > 2 && getClient().trace()) {
      getClient().tracePrintln("InterPreserveLocalInfoTransformersAbstractFactory: makeCallAndExitToReturnBinaryTransformer " +
          " caller: " + program.methodName(caller) + 
          " callee: " + program.invocationCalleeName(invocation));
    }

    return tvlaapi.getBinaryTransformer(
        callAndExitCombiner,
        InterPreserveLocalInfoConstants.actionPreserveLocalIntoStr, stub);
  }      
  

  public ITVSUnaryTransformer makeCallerPostCallTransformer(Object caller, Object invocation) {
    return x2rBinder.makeCallerPostCallTransformer(caller, invocation);
  }      

  // We "allow" it syntactically, (since we also do get_Ret and do not want to have too many oprtions)
  // but asserts that it is not uused "semantically"
  // Note that ignoring this statment, is safe as the return value is ignored anyway
  public ITVLATransformers.ITVSUnaryTransformer makeReturnValueFlowFunction(Object method, int retValIndex) {
    getClient().UNREACHABLE("No interpocedural call is expected for this FunctionProvider");
    return null; // appease Eclipse
  }      
  
  /****************************************************************
   * Interprocedural - should ne be callled!
   ****************************************************************/
  
  // 1. Sq' = makeCallerPreCallTransformer[y=p(x)](Sq)
  public ITVLATransformers.ITVSUnaryTransformer makeCallerPreCallTransformer(
      final Object caller, final Object invocation) {
    getClient().UNREACHABLE("No interpocedural call is expected for this FunctionProvider");
    return null; // appease Eclipse
  }      
  
  // 2. Sp' = makeCallerToCalleeTransformer[y=p(x)](Sq')
  public ITVLATransformers.ITVSUnaryTransformer makeCallToEntryTransformer(
      Object caller, Object invocation) {
    getClient().UNREACHABLE("No interpocedural call is expected for this FunctionProvider");
    return null; // appease Eclipse
  }      
   
  // 4. Sp'' = makeCalleeExitTransformer[y=p(x)](Sp')
  public ITVLATransformers.ITVSUnaryTransformer makeCalleeExitTransformer(final Object callee) {
    getClient().UNREACHABLE("No interpocedural call is expected for this FunctionProvider");
    return null; // appease Eclipse
  }

  
  

}
