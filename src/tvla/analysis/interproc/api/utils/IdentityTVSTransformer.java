package tvla.analysis.interproc.api.utils;

import tvla.api.ITVLATransformers;

/**
 * @author maon
 *  
 * A flow function where out == in
 */
public class IdentityTVSTransformer  implements ITVLATransformers.ITVSUnaryTransformer {
  
  private final static IdentityTVSTransformer singleton = new IdentityTVSTransformer();
  
  public int[] apply(int i) {
    int[] ar = new int[1];
    ar[0] = i;
    return ar;
  }
  
  public static IdentityTVSTransformer identity() {
    return singleton;
  }
  
  public String toString() {
    return "IdentityTVSTransformer";
  }

  public void setPrecedingStageResult(int res) {      
  }
  
  public void clearPrecedingStageResult() {      
  }

  public int[] apply(int[] tvss) {
    // TODO Auto-generated method stub
    return tvss;
  }

}