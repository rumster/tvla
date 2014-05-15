//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.tvlaadapter.transformers;

import tvla.analysis.interproc.api.utils.TVLAAPIAssert;
import tvla.api.ITVLATransformers.ITVSBinaryTransformer;

/**
 * This is a special composite binary action for composing 2 actions
 * @author maon
 *
 */

public class BinaryStagedTransformer
extends AbstractBinaryTransformer {
  protected final ITVSBinaryTransformer  firstAction; 
  protected final ITVSBinaryTransformer  secondAction;
  
  public BinaryStagedTransformer(
      ITVSBinaryTransformer firstAction, 
      ITVSBinaryTransformer secondAction) {    
    this.firstAction = firstAction;
    this.secondAction = secondAction;
 }
  
  public int[] apply(int tvsId1, int tvsId2) {
    if (0 < DEBUG_LEVEL)
      TVLAAPIAssert.debugAssert(
        this.expexctedResult > -1,
        "OOPS - no expected staged result");
    
    int[] stagedResult = firstAction.apply(tvsId1, this.expexctedResult);

    int[] res = secondAction.apply(stagedResult, tvsId2);
    
    return res;
  }

  public int[] apply(int[] in1, int in2) {
    if (0 < DEBUG_LEVEL)
      TVLAAPIAssert.debugAssert(
        this.expexctedResult > -1,
        "OOPS - no expected staged result");
    
    int[] stagedResult = firstAction.apply(in1, this.expexctedResult);

    int[] res = secondAction.apply(stagedResult, in2);
    
    return res;
  }
  
  public String toString() {
    String ret = "BinaryStagedTransformer: expected " + expexctedResult + 
                 " first action " + firstAction.toString() + 
                 " second action " + secondAction.toString();
    return ret;    
  }

}