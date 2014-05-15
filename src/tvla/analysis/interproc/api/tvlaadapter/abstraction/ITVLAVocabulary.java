//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.tvlaadapter.abstraction;

import tvla.api.ITVLAAPI.IVocabulary;
import tvla.predicates.Predicate;

public interface ITVLAVocabulary extends IVocabulary {
  public Predicate getTVLASM() ;
  
  public Predicate getTVLAInUc() ;
  
  public Predicate getTVLAInUx() ;
  
  public Predicate getTVLAKill() ;
}
