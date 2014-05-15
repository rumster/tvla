//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.tvlaadapter;

import tvla.analysis.AnalysisStatus;
import tvla.api.ITVLAAPI.ITVLAAPIStatistics;

public class TVLAAPIStatistics implements ITVLAAPIStatistics {
  protected final AnalysisStatus status;
  public TVLAAPIStatistics(AnalysisStatus status) {
    this.status = status ;      
  }
  
  public String toString() {
    if (status == null) 
      return "Failed to get TVLA statistics";
    
    return status.toString();
  }    
}