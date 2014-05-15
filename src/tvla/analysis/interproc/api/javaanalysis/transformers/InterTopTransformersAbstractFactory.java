package tvla.analysis.interproc.api.javaanalysis.transformers;

import java.util.Set;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaVocabulary;
import tvla.api.AbstractTVLAAPI;
import tvla.api.ITVLATransformers;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaAnalsyisEnvironmentServicesPovider;


/**
 * Generates ction for analyses that use TVLA for intraprocedural statement and
 * fall back to soe sound abstraction of the memory state after a call
 * This can be , e.g., program-code-independant-TOP, program-TOP, or method-TOP. 
 *
 * @author noam rinetzky
 */

public class InterTopTransformersAbstractFactory extends AbstratInterTransformersAbstractFactory {
  /////////////////////////////////////////
  ///  Initializtion                    ///
  /////////////////////////////////////////
  public InterTopTransformersAbstractFactory(
      AbstractTVLAAPI tvlaAPI, 
      ITVLAJavaAnalsyisEnvironmentServicesPovider environmentServicesProvider,
      IJavaVocabulary vocabulary) {
    super(tvlaAPI, environmentServicesProvider, vocabulary);
  }
  
  
  protected void getActionsNames(Set actionNames) {
    super.getActionsNames(actionNames);
  }

  
  /****************************************************************
   * Interprocedural
   ****************************************************************/
  
  public ITVLATransformers.ITVSUnaryTransformer makeReturnValueFlowFunction(Object method, int retValIndex) {
    getClient().UNREACHABLE("No interpocedural call is expected for this FunctionProvider");
    return null; // appease Eclipse
  }      
  
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
  
  // 3. Sp = makeCalleeEntryTransformer[y=p(x)](Sp')
  public ITVLATransformers.ITVSUnaryTransformer makeCalleeEntryTransformer(final Object callee) {
    getClient().UNREACHABLE("No interpocedural call is expected for this FunctionProvider");
    return null; // appease Eclipse
  }      		
  
  
  // 4. Sp'' = makeCalleeExitTransformer[y=p(x)](Sp')
  public ITVLATransformers.ITVSUnaryTransformer makeCalleeExitTransformer(final Object callee) {
    getClient().UNREACHABLE("No interpocedural call is expected for this FunctionProvider");
    return null; // appease Eclipse
  }      
  // 5. Sq'' = makeCalleeToCallerTransformer[y=p(x)](Sq',Sp'')
  public ITVLATransformers.ITVSBinaryTransformer makeCallAndExitToReturnBinaryTransformer(
      Object caller, Object invocation){
    getClient().UNREACHABLE("No interpocedural call is expected for this FunctionProvider");
    return null; // appease Eclipse
  }      
  
  // 6. Sq''' = makeCallerPostCallTransformer(Sq'')
  public ITVLATransformers.ITVSUnaryTransformer makeCallerPostCallTransformer(
      Object caller, Object invocation){
    getClient().UNREACHABLE("No interpocedural call is expected for this FunctionProvider");
    return null; // appease Eclipse
  }      
}  

