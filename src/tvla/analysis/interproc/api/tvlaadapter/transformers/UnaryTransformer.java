//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.tvlaadapter.transformers;

import tvla.analysis.interproc.semantics.ActionInstance;
import tvla.api.ITVLATransformers.ITVSUnaryTransformer;

public class UnaryTransformer
extends AbstractUnaryTransformer
implements ITVSUnaryTransformer {
  
  public final  ActionInstance actionInstance;
  
  public UnaryTransformer(ActionInstance instance) {
    super();
    actionInstance = instance;
  }
  
  public ActionInstance getAction() {
    return actionInstance;
  }
  
  public int[] apply(int tvsId) {
    return  applierAdapter.apply(this,  tvsId);
    //return  applierAdapter.apply(this, tvsId);
  }

  public int[] apply(int[] in) {
    return  applierAdapter.apply(this,  in);
  }
  
  public String toString() {
    String ret = "UnaryTransformer: action " + actionInstance.getMacroName(true);
    return ret;    
  }
}  