//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.tvlaadapter.transformers;

import tvla.analysis.interproc.api.utils.IdentityTVSTransformer;
import tvla.analysis.interproc.api.utils.TVLAAPIAssert;
import tvla.api.ITVLATransformers.ITVSUnaryComposedTransformer;
import tvla.api.ITVLATransformers.ITVSUnaryTransformer;
import tvla.util.StringUtils;

public class UnaryComposedTransformer extends AbstractUnaryTransformer implements ITVSUnaryComposedTransformer {
  protected final ITVSUnaryTransformer[]  transformers;
  protected final int numberOfTransformersToUse;
  protected final String composedName;      
  
  public UnaryComposedTransformer(
      ITVSUnaryTransformer[] transformers, 
      int use, String name) {
    this.transformers = transformers;
    this.numberOfTransformersToUse = use;
    this.composedName = name;
  }
  
  public int[] apply(int tvsId) {
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(numberOfTransformersToUse <= transformers.length);
    
    int[] in = new int[]{tvsId};
    
    if (numberOfTransformersToUse == 0)
      // should act as identity, null means universal kill! 
      return in;
    
    int[] res = apply(in);
    // note that res might be null, which means we killed all the input!
    return res;
  }
  
  public int[] apply(int[] in) {
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(numberOfTransformersToUse <= transformers.length);
    
    if (numberOfTransformersToUse == 0)
      return null;
    
    int[] res = in;
    
    for (int currTrans = 0; currTrans < numberOfTransformersToUse; currTrans++) {
      int[] curRes = transformers[currTrans].apply(res);
      if (curRes == null)
        return null;
      
      res = curRes;
    }
    
    return res;
  }
  

  public ITVSUnaryTransformer simplify() {
    ITVSUnaryTransformer ret;
    
    if (transformers == null)
      ret = IdentityTVSTransformer.identity();
    else {
      switch(numberOfTransformersToUse) {
      case 0: return IdentityTVSTransformer.identity();
      case 1: return transformers[0];
      default: return this;
      }
    }
    
    return ret;
  }
  
  
  
  public String toString() {
    StringBuffer desc = new StringBuffer("Coposed Transformer for " + composedName + StringUtils.newLine);
    for (int currTrans = 0; currTrans < numberOfTransformersToUse; currTrans++) {
      desc.append("  " + currTrans + ". " + transformers[currTrans].toString() + StringUtils.newLine);
    }
    return desc.toString();
  }


}