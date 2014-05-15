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
import tvla.api.AbstractTVLAAPI;
import tvla.api.ITVLATransformers;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaAnalsyisEnvironmentServicesPovider;
import tvla.api.ITVLATransformers.ITVSUnaryTransformer;

/**
 * A no frills standard transformer factory.
 * @author maon
 *
 */
public class InterLocalHeapsTransformersAbstractFactory extends AbstratInterTransformersAbstractFactory  {
  protected final CommonCall2EntryParamBindingTransformersFactory c2eBinder;
  protected final CommonExit2ReturnParamBindingTransformersFactory x2rBinder;
  
  public InterLocalHeapsTransformersAbstractFactory(AbstractTVLAAPI tvlaAPI, ITVLAJavaAnalsyisEnvironmentServicesPovider environmentServicesProvider, IJavaVocabulary vocabulary) {
    super(tvlaAPI, environmentServicesProvider, vocabulary);
    c2eBinder = new CommonCall2EntryParamBindingTransformersFactory(tvlaAPI, this.program, vocabulary);
    x2rBinder = new CommonExit2ReturnParamBindingTransformersFactory(tvlaAPI,  this.program, vocabulary);
  }
  
  protected static class InterLocalHeapsActionsConstants {
    protected final static String actionCallStr  = "Call";                                                     // ()
    protected final static String actionEntryStr  = "Method_Entry";                                            // ()       
    protected final static String actionExitStr  = "Method_Exit";                                              // ()   
    protected final static String actionReturnStr = "Ret";                                                     // ()
    
    // current way to handle various less obvious aspects procedure calls  
    protected final static String actionPushStaticFieldsStr = "Push_Static_Fields";                            // () static fields are addiitonal roots for the local heap! 
    protected final static String actionSelectLocalHeapStr  = "Select_Callee_Local_Heap";                          // () remove all unreachable elements from the local heap. 


    // Convineient array
    public final static String[] allInterActionsStrs = new String[] {
        actionCallStr, 
        actionEntryStr,
        actionExitStr, 
        actionReturnStr,        
        actionPushStaticFieldsStr, 
        actionSelectLocalHeapStr
    };
    
    
    protected static Collection getLocalHeapsActionsNames() {
      return Arrays.asList(InterLocalHeapsActionsConstants.allInterActionsStrs);
    }
  }
  
  
  protected void getActionsNames(Set actionNames) {
    super.getActionsNames(actionNames);
    c2eBinder.getActionsNames(actionNames);
    x2rBinder.getActionsNames(actionNames);
    actionNames.addAll(InterLocalHeapsActionsConstants.getLocalHeapsActionsNames());
  }  
  
 
  
  /****************************************************************
   * Interprocedural 
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
      getClient().tracePrintln("TVLAJavaAdapter: makeCallToEntryTransformer " +
          " caller: " + program.methodName(caller) + 
          " callee: " + program.invocationCalleeName(invocation));
    
    List stub = new ArrayList(0);   
    return tvlaapi.getUnaryTransformer(InterLocalHeapsActionsConstants.actionCallStr, stub);
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
    
    
    if (DEBUG_LEVEL > 2 && getClient().trace()) {
      getClient().tracePrintln("TVLAJavaAdapter: makeCallAndExitToReturnBinaryTransformer " +
          " caller: " + program.methodName(caller) + 
          " callee: " + program.invocationCalleeName(invocation));
    }
    
    List stub = new ArrayList(0);   
    
    
    return tvlaapi.getBinaryTransformer(
        callAndExitCombiner, InterLocalHeapsActionsConstants.actionReturnStr, stub);
  }


  
  /****************************************************************
   * param binding - delegated
   ****************************************************************/

  /** 
   * 1. Sq' = makeCallerPreCallTransformer[y=p(x)](Sq)
   */
  
  public ITVSUnaryTransformer makeCallerPreCallTransformer(Object caller, Object invocation) {
    return c2eBinder.makeCallerPreCallTransformer(
        getCallerPreCallFirst(), 
        getCallerPreCallLast(), 
        caller, 
        invocation);
  }

  protected ITVLATransformers.ITVSUnaryTransformer getCallerPreCallFirst() {
    List stub = new ArrayList(0);
    return tvlaapi.getUnaryTransformer(InterLocalHeapsActionsConstants.actionPushStaticFieldsStr, stub);
  }
  
  protected ITVLATransformers.ITVSUnaryTransformer getCallerPreCallLast() {
    List stub = new ArrayList(0);
    return tvlaapi.getUnaryTransformer(InterLocalHeapsActionsConstants.actionSelectLocalHeapStr, stub);
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
 
  /**
   * Get the action to do at the entry to the callee after the transfer-parmaeter to 
   * formal-parameter binding is over
   * @return
   */  
  protected ITVLATransformers.ITVSUnaryTransformer getEntryAction() {
    List stub = new ArrayList(0);
    return tvlaapi.getUnaryTransformer(InterLocalHeapsActionsConstants.actionEntryStr, stub);
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

  /**
   * Get the action to do at the exit to the callee after the return value is set
   * @return
   */  
  protected ITVLATransformers.ITVSUnaryTransformer getExitAction() {
    List stub = new ArrayList(0);
    String exiter = InterLocalHeapsActionsConstants.actionExitStr;
    
    return tvlaapi.getUnaryTransformer(exiter, stub);
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


}
