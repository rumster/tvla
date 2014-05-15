package tvla.analysis.interproc.api.tvlaadapter.transformers;

import tvla.api.ITVLATransformers.ITVSBinaryTransformer;

/**
 * Creation of a binary abstract transformer.
 * Used to combine 2 TVSs at the return site.
 * @param actionName the name of the sction to instantiae (i.e., the name of the macro)
 * @param parameters an array with the parameter to the action. The array should have the number
 * of parameters expected by the action
 * @return an opaque object which represents an action.
 * Null, if failed to create an action.
 */

public abstract class AbstractBinaryTransformer 
extends AbstractTransformer 
implements ITVSBinaryTransformer {
  public AbstractBinaryTransformer() {
    super();
  }
}