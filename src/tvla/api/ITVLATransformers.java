
package tvla.api;

public interface ITVLATransformers {	
  public static interface ITVSStagedTransformer {
    /**
     * returns the name of the encapsulated action (macro name + parameters)
     */
    String toString();
    
    
    /**
     * Adds information computed by previous stages about the expected result fo the 
     * the transformer.
     * The information is given in the form of a TVS.
     * The transformer may and may not make use of this information
     * The default convension is to scheme is that the inforamtion is ignored 
     */
    public void setPrecedingStageResult(int res);
    
    /**
     * Clear the preceding results, thus enable resuing of the tranfomer
     */
    public void clearPrecedingStageResult();
    
  }
  
  /**
   * A unary abstract transformer.
   * We use the convension that null is 
   */ 
  public static interface ITVSUnaryTransformer extends ITVSStagedTransformer {
    /**
     * Apply the action to the tvs with id tvsId.
     * The tvs is taken from the repository of the TVSAPI that generated that transformer.
     */
    public int[] apply(int tvsId);
    
    
    /**
     * Apply the action to all the tvss and return the UNION of the result
     * @param tvss
     * @return
     */
    public int[] apply(int tvss[]);
  }
  
  /**
   * A composed transformer
   */
  public static interface ITVSUnaryComposedTransformer extends ITVSUnaryTransformer {
    
    /**
     * Attempts to simlify the aggregated transformer 
     * @return
     * Identity - if no elements where composed
     * FlowFunction - if only 1 element was composed
     * itself - if the function cannot be simplified
     */
    
    public ITVSUnaryTransformer simplify();
  }
  
  /**
   * Returns a composed tranformer of the first use transforemrs in the transformers array
   * and associated a name with the transformer (for debugging)
   * Returns null if all transformers ar null. 
   */
  ITVSUnaryComposedTransformer composedTransformers(ITVSUnaryTransformer[] transformers, int use, String name);
  
  /**
   * A binary abstract transformer.
   * Used to combine 2 TVSs at the return site.
   */
  public static interface ITVSBinaryTransformer extends ITVSStagedTransformer {
    /**
     * Apply the action to the tvs with ids tvsIdC (pertaining to the tvs at the cal site) and
     * tvsIdX (pertaining to the tvs at the exit site).
     * The tvs is taken from the repository of the TVSAPI that generated that transformer.
     */
    public int[] apply(int tvsIdC, int tvsIdX); 
    
    /**
     * Apply the action pointwise to all the tvss and tvs2 and return the UNION of the result
     * @param tvss
     * @return
     */
    public int[] apply(int tvsIdC[], int tvsIdX); 
    
  }
  
  
}
