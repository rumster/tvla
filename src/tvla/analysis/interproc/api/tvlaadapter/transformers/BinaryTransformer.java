//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.tvlaadapter.transformers;

import tvla.analysis.interproc.semantics.ActionInstance;
import tvla.core.Combine.INullaryCombiner;

public class BinaryTransformer
extends AbstractBinaryTransformer {
  public final INullaryCombiner nullaryCombiner;
  public final ActionInstance binaryActionInstance;
  
  public BinaryTransformer (
      INullaryCombiner nullaryCombiner,
      ActionInstance instance) {
    this.nullaryCombiner = nullaryCombiner;
    this.binaryActionInstance = instance;
  }
  
  public int[] apply(int tvsId1, int tvsId2) {    
    return applierAdapter.apply(this, tvsId1, tvsId2);
  }

  public int[] apply(int[] in1, int in2) {
    return applierAdapter.apply(this, in1, in2);
  }
  
  public String toString() {
    String ret = "BinaryTransformer: action " + binaryActionInstance.getMacroName(true) + 
                 " combiner " + nullaryCombiner.toString();
    return ret;    
  }
}