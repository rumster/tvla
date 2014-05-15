//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.javaanalysis.transformers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaVocabulary;
import tvla.analysis.interproc.api.utils.IdentityTVSTransformer;
import tvla.analysis.interproc.api.utils.TVLAAPIAssert;
import tvla.analysis.interproc.api.utils.TVLAAPIDebugControl;
import tvla.analysis.interproc.api.utils.TVLAAPITrace;
import tvla.api.AbstractTVLAAPI;
import tvla.api.ITVLATransformers;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModelerServices;

/**
 * The trasnfoer in this class extends the intra procedural with  
 * the ability to set the return value of a function into a local
 * 
 * This operation is a sort of a hibrid inter/intra as it uses function
 * return value (hench, inter) and also modifies the value of 
 * local variales (hench intra)
 * 
 * @author maon
 *
 */

public class CommonExit2ReturnParamBindingTransformersFactory 
{
  public final static int DEBUG_LEVEL = TVLAAPIDebugControl.getDebugLevel(6);
  
  protected final AbstractTVLAAPI tvlaapi; 
  protected final ITVLAJavaProgramModelerServices program; 
  protected final IJavaVocabulary vocabulary;
  
  public CommonExit2ReturnParamBindingTransformersFactory(
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
  public static class ReturnValueConstants  {
    // Return values
    //// Callee Side
    public final static String actionCopyBooleanIntoTransferRetValueStr = "boolean_Set_Ret_Val";            // (PT, x)  Exit[b] = x
    public final static String actionCopyReferenceIntoTransferRetValueStr = "Set_Ret_Val";                  // (PT, x)  Exit[p] = x
    
    //// Caller side
    public final static String actionMoveTransferRetValueIntoBooleanStr = "boolean_Get_Ret_Val";            // (PT, b)  b = Exit[b] ; Exit[b] = false
    public final static String actionBooleanClearTransferReturnsStr = "boolean_Clear_Ret_Val";              // (PT): If the calee returnea value which is igmored by the caller

    public final static String actionMoveTransferRetValueIntoReferenceStr = "Get_Ret_Val";                  // (PT, x)  x = Exit[p] ; Exit[p]= null
    public final static String actionReferenceClearTransferReturnsStr = "Clear_Ret_Val";                    // (PT): If the calee returnea value which is igmored by the caller
    
    // Convineient array
    public final static String[] allParamBinidingActionsStrs = new String[] {
      actionCopyBooleanIntoTransferRetValueStr,  
      actionCopyReferenceIntoTransferRetValueStr, 
      actionMoveTransferRetValueIntoBooleanStr, 
      actionMoveTransferRetValueIntoReferenceStr, 
      actionBooleanClearTransferReturnsStr, 
      actionReferenceClearTransferReturnsStr
    };
    
    protected static Collection getReturnActionsNames() {
      return Arrays.asList(ReturnValueConstants.allParamBinidingActionsStrs);
    }
  }
   
  protected void getActionsNames(Set actionNames) {
    actionNames.addAll(ReturnValueConstants.getReturnActionsNames());
  }


   
  /********************************************************************
   * Set the return value 
   ********************************************************************/
  
  
  public ITVLATransformers.ITVSUnaryTransformer makeReturnValueFlowFunction(Object method, int retValIndex) {
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(program.representsMethod(method));
    
    String returner = null;
    String rv = null;
    if (program.methodReturnTypeIsReference(method)) {
      rv = vocabulary.refLocalToPred(method, retValIndex);
      returner = ReturnValueConstants.actionCopyReferenceIntoTransferRetValueStr;
    }
    else if (program.methodReturnTypeIsBoolean(method)) {
      rv = vocabulary.boolLocalToPred(method, retValIndex);
      returner = ReturnValueConstants.actionCopyBooleanIntoTransferRetValueStr;
    }
    else {   
      if (0 < DEBUG_LEVEL)
        TVLAAPITrace.tracePrintln("CommonExit2ReturnParamBindingTransformersFactory: Return value of unsupported type - retrun value ignored! method:" + method);
      return null;
    }
    
    
    if (0 < DEBUG_LEVEL)
      TVLAAPITrace.tracePrintln("CommonExit2ReturnParamBindingTransformersFactory: makeReturnValueFlowFunction - " + 
          " method " + program.methodName(method) + " : returns " + rv); 
    
    List args = new ArrayList(2);
    args.add(0, "filler");
    args.add(1, rv);
    
    return tvlaapi.getUnaryTransformer(returner, args);
  }
  

  
  /********************************************************************
   * Get the return value
   ********************************************************************/

  
  // 6. Sq''' = makeCallerPostCallTransformer(Sq'')
  /* (non-Javadoc)
   * @see tvla.api.TVLAJavaAdapter#makeCallerPostCallTransformer(tvla.api.TVLAJavaAdapterImpl.MethodAssignementDesc, tvla.api.TVLAJavaAdapterImpl.MethodExitFrame)
   */
  
  public ITVLATransformers.ITVSUnaryTransformer makeCallerPostCallTransformer(
      Object caller, Object invocation)
  { 
    if (TVLAAPIAssert.ASSERT) {
      TVLAAPIAssert.debugAssert(program.representsInvocation(invocation));
      TVLAAPIAssert.debugAssert(program.representsMethod(caller));
    }
    
    
    if (program.invocationReturnTypeIsVoid(invocation))  {
      if (0 < DEBUG_LEVEL)
        TVLAAPITrace.tracePrintln("CommonExit2ReturnParamBindingTransformersFactory: makeCallerPostCallTransformer - method returns void - returns identity strnasformer");
      
      return IdentityTVSTransformer.identity();
    }
    
    String assignedToPred= null;
    String assigner;
    
    if (program.invocationReturnTypeIsReference(invocation)) {
      if (program.invocationHasDef(invocation)) {
        int assignedToIndex = program.invocationGetDef(invocation);
        assigner = ReturnValueConstants.actionMoveTransferRetValueIntoReferenceStr;
        assignedToPred = vocabulary.refLocalToPred(caller, assignedToIndex);
      }
      else
        assigner = ReturnValueConstants.actionReferenceClearTransferReturnsStr;
    }
    else if (program.invocationReturnTypeIsBoolean(invocation)) {
      if (program.invocationHasDef(invocation)) {
        int assignedToIndex = program.invocationGetDef(invocation);
        assigner = ReturnValueConstants.actionMoveTransferRetValueIntoBooleanStr;
        assignedToPred = vocabulary.boolLocalToPred(caller, assignedToIndex);
      }
      else
        assigner = ReturnValueConstants.actionBooleanClearTransferReturnsStr;
    }
    else {
      // TODO add to the comment the type of the return value / method is init
      if (0 < DEBUG_LEVEL)
        TVLAAPITrace.tracePrintln("TVLAJavaAdapter: makeCallerPostCallTransformer - no special action needed for this return type - return identity");
      
      return IdentityTVSTransformer.identity();
    }
    
    if (0 < DEBUG_LEVEL)
      TVLAAPITrace.tracePrintln("CommonExit2ReturnParamBindingTransformersFactory: makeCallerPostCallTransformer " +
          " caller: " + program.methodName(caller) + 
          " callee: " + program.invocationCalleeName(invocation) +
          " return value is " +  ((assignedToPred != null)  ? "assigned to: " + assignedToPred : " not assigned "));    
    
    List args = new ArrayList(2);
    args.add(0, "filler");
    if (program.invocationHasDef(invocation)) {
      if (TVLAAPIAssert.ASSERT) 
        TVLAAPIAssert.debugAssert(assignedToPred != null);
      args.add(1, assignedToPred);
    }
    else {
      if (TVLAAPIAssert.ASSERT) 
        TVLAAPIAssert.debugAssert(assignedToPred == null);          
    }
    
    return tvlaapi.getUnaryTransformer(assigner, args);      
  }
}
