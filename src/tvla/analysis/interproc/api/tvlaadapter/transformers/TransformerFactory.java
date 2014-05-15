//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.tvlaadapter.transformers;

import tvla.analysis.interproc.api.tvlaadapter.ITVLAApplierAdapter;
import tvla.analysis.interproc.api.utils.TVLAAPIAssert;
import tvla.analysis.interproc.api.utils.TVLAAPIDebugControl;
import tvla.analysis.interproc.semantics.ActionInstance;
import tvla.api.ITVLATransformers.ITVSBinaryTransformer;
import tvla.api.ITVLATransformers.ITVSUnaryTransformer;
import tvla.core.Combine.INullaryCombiner;

public class TransformerFactory {
  protected final static int DEBUG_LEVEL = TVLAAPIDebugControl.getDebugLevel(4);
 
  public TransformerFactory(ITVLAApplierAdapter applierAdapter) {
    setApplier(applierAdapter);
  }
  
  protected ITVLAApplierAdapter setApplier(ITVLAApplierAdapter applier) {
    ITVLAApplierAdapter old =  AbstractTransformer.setApplier(applier);

    if (0 < DEBUG_LEVEL) {
      //  Guards agains "change" of applier during runtime
      TVLAAPIAssert.debugAssert(old == null);
    }
    return old;
  }
 
  
  /****************************************************************
   * Standard transformers 
   ****************************************************************/
  
  public AbstractUnaryTransformer getUnaryTransformer(ActionInstance actionInstance) {
    return new UnaryTransformer(actionInstance);
  }

  

  public ITVSBinaryTransformer getBinaryTransformer(
      INullaryCombiner nullaryCombiner, ActionInstance actionInstance) {
      return new BinaryTransformer(nullaryCombiner, actionInstance);
    }

  
  
  /****************************************************************
   * Staged transformers 
   ****************************************************************/

  
  public ITVSUnaryTransformer getUnaryStagedTransformer(ITVSBinaryTransformer stager) {
    return  new UnaryStagedTransformer(stager);
  }
  
  
  
  public ITVSBinaryTransformer getBinaryStagedTransformer(
      ITVSBinaryTransformer trans1, ITVSBinaryTransformer trans2) { 

    return new BinaryStagedTransformer(trans1, trans2);
  }
  
  
  
  /****************************************************************
   * Composed transformers 
   ****************************************************************/
  
  
  public UnaryComposedTransformer composedTransformers(ITVSUnaryTransformer[] transformers, int use, String name) {
    return new UnaryComposedTransformer(transformers, use, name);
  }
  
}
