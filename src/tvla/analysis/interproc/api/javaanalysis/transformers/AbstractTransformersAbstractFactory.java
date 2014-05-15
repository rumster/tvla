//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.javaanalysis.transformers;

import tvla.analysis.interproc.api.utils.TVLAAPIDebugControl;


public abstract class AbstractTransformersAbstractFactory implements ITransformersAbstractFactory {
  /**
   * Current debug level
   */
  protected final static int DEBUG_LEVEL = TVLAAPIDebugControl.getDebugLevel(4);
  
  /**
   * Should the trasnformer factory check assertions
   */
  protected final static boolean shouldAssert = true;
  
  /**
   * Does the analysis cares about primitive values (e.g., booleans!)    
   */
  public static final boolean trackingPrimitives = false;
}
