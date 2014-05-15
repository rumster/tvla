//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.tvlaadapter;

import tvla.analysis.interproc.api.tvlaadapter.transformers.BinaryTransformer;
import tvla.analysis.interproc.api.tvlaadapter.transformers.UnaryTransformer;

/**
 * The limited interfacs that the transfomer adapter exposes towards the 
 * Transformers.
 * Not to be confused with the interface exposed to the TVLAAPI which uses 
 * invokes apply on the Transormers
 * 
 * @author maon
 *
 */
public interface ITVLAApplierAdapter {
  //  NEW //
  /**
   * Standard unary action
   */
  

  public abstract int[] apply(
      UnaryTransformer governingTransformer, 
      int tvsId1);
  
  public abstract int[] apply(
      UnaryTransformer governingTransformer, 
      int in[]);

  /**
   * Standard binary action
   */
  
  public abstract int[] apply(
      BinaryTransformer governingTransformer, 
      int tvsId1, 
      int tvsId2);
  
  public abstract int[] apply(
      BinaryTransformer governingTransformer, 
      int tvsId1[], 
      int tvsId2);


  /**
   * Staged unary action
   */
  

  /**
   * Staged binary action
   */
  
  /**
   * Composite action
   */
  
/************************************************************/  
/*
  //  OLD //
  public abstract int[] apply(
      ITVSStagedTransformer governingTransformer, 
      ActionInstance unaryActions, 
      int[] in);

  public abstract int[] apply(
      ITVSStagedTransformer governingTransformer, 
      ActionInstance unaryAction, 
      int tvsId);

  public abstract int[] applyOrNullaries(
      ITVSStagedTransformer governingTransformer, 
      ActionInstance binaryAction, 
      int tvsId1, 
      int tvsId2);

  public abstract int[] applyProjectFirstNullaries(
      ITVSStagedTransformer governingTransformer, 
      ActionInstance binaryAction, 
      int tvsId1, 
      int tvsId2);

  public abstract int[] applyOrNullaries(
      ITVSStagedTransformer governingTransformer, 
      ActionInstance binaryAction, 
      int[] in1, 
      int tvsId2);

  public abstract int[] applyProjectFirstNullaries(
      ITVSStagedTransformer governingTransformer, 
      ActionInstance binaryAction, 
      int[] in1, 
      int tvsId2);  

*/
}