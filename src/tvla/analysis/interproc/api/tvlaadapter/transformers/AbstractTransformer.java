package tvla.analysis.interproc.api.tvlaadapter.transformers;

import tvla.analysis.interproc.api.tvlaadapter.ITVLAApplierAdapter;
import tvla.analysis.interproc.api.utils.TVLAAPIAssert;
import tvla.analysis.interproc.api.utils.TVLAAPIDebugControl;
import tvla.api.ITVLATransformers.ITVSStagedTransformer;

/**
 * Root class for all transformers
 * Becasue all transformers use the same applier (directly or indirectly),
 * we can (for efficiency) cache its value here.
 */

public class AbstractTransformer implements ITVSStagedTransformer {  
  protected final static int DEBUG_LEVEL = TVLAAPIDebugControl.getDebugLevel(4);
  
  /*******************************************************
   * Shared by all transformers
   *******************************************************/
  
  /**
   * A reference to the one and only applier
   */ 
  protected static ITVLAApplierAdapter applierAdapter;
  
  /**
   * Sets the aplier to a new value 
   * 
   * @param newApplier the new applier
   * @return returns the old applier
   */
  public static ITVLAApplierAdapter setApplier(ITVLAApplierAdapter newApplier) {
    if (0 < DEBUG_LEVEL) {
      // make sure the applier we get is not null
      TVLAAPIAssert.debugAssert(newApplier != null);
    }
    
    ITVLAApplierAdapter old = applierAdapter;
    applierAdapter = newApplier;
    
    return old;   
  }
  
  
  /*******************************************************
   * Stsaged info management
   *******************************************************/
  
  protected int expexctedResult = -1;
  
  public void setPrecedingStageResult(int res) {
    expexctedResult = res;
  }
  
  public void clearPrecedingStageResult() {
    expexctedResult = -1;
  }
}