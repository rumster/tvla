//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.tvlaadapter.transformers;

import tvla.api.ITVLATransformers;
import tvla.api.ITVLATransformers.ITVSBinaryTransformer;

/**
 * A unary transformer that uses a binary transformer to 
 * implement the stages analysis 
 * 
 * @author maon
 *
 */
public class UnaryStagedTransformer
extends AbstractUnaryTransformer
implements ITVLATransformers.ITVSUnaryTransformer {
  protected final ITVSBinaryTransformer stagedAction; 
   
  public UnaryStagedTransformer( 
       ITVSBinaryTransformer  stagedAction) {
    this.stagedAction = stagedAction;
  }
  
  public int[] apply(int tvsId) {
    return  stagedAction.apply(tvsId, expexctedResult);
  }

  public int[] apply(int[] in) {
    return  stagedAction.apply(in, expexctedResult);
  }  
  
  public String toString() {
    String ret = "UnaryStagedTransformer: expected " + expexctedResult + 
                 " (staged) action " + stagedAction.toString();
    return ret;    
  }
}