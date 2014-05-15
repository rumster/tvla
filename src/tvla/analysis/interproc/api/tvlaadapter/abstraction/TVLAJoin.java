//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.tvlaadapter.abstraction;

import java.util.ArrayList;

import tvla.analysis.interproc.api.tvlaadapter.TVSRepository;
import tvla.analysis.interproc.api.utils.IntArrayUtils;
import tvla.analysis.interproc.api.utils.TVLAAPIAssert;
import tvla.analysis.interproc.api.utils.TVLAAPIDebugControl;
import tvla.core.HighLevelTVS;
import tvla.core.TVSFactory;
import tvla.core.TVSSet;
import tvla.util.Pair;


public class TVLAJoin {
  private static final int DEBUG_LEVEL = TVLAAPIDebugControl.getDebugLevel(6);
  
  protected final TVSRepository repository;
  
  public TVLAJoin(TVSRepository repository) {
    this.repository = repository;
  }
  
  // TODO see what happen if partial join changes a tvs IN the set 
  // Note that we may use onlty a prefix of the array
  public int[] join(int use, int[] input, int[] inputToOutputMap) {
    if ( 0 < DEBUG_LEVEL) {
      TVLAAPIAssert.debugAssert(use <= input.length);
      TVLAAPIAssert.debugAssert(use <= inputToOutputMap.length);
    }
    
    if (input.length == 1) {
      if (inputToOutputMap != null)
        inputToOutputMap[0] = 0;
      
      return null; // TODO  fix this HACK input;
    }
    
    final TVSSet tvsSet = TVSFactory.getInstance().makeEmptySet();
    
    // NOTE that we may copy the TVS to avoid changes to the input TVSs,
    HighLevelTVS[] copy = new HighLevelTVS[use];
    HighLevelTVS[] mergedInto = new HighLevelTVS[use];
    
    ArrayList mergureMap = new ArrayList(1);
    for (int i=0; i<use; i++) {
      HighLevelTVS currentTVS = repository.getTVS(input[i]);
      
      if (0 < DEBUG_LEVEL)
        TVLAAPIAssert.debugAssert(currentTVS != null);
      
      copy[i] = (HighLevelTVS) currentTVS.copy();         
      
      tvsSet.mergeWith(copy[i], mergureMap);
      if (mergureMap.isEmpty()) {
        // input[i] was added to the tvSet              
        mergedInto[i] = copy[i];
      }
      else {
        if (0 < DEBUG_LEVEL)
          TVLAAPIAssert.debugAssert(mergureMap.size() == 1);
        
        Pair pair = (Pair) mergureMap.get(0);
        HighLevelTVS oldTVS = (HighLevelTVS) pair.first; 
        HighLevelTVS newTVS = (HighLevelTVS) pair.second;
        
        if (0 < DEBUG_LEVEL)
          TVLAAPIAssert.debugAssert(oldTVS == copy[i]);
        
        mergedInto[i] = newTVS;
        mergureMap.clear();
      }           
    }
    
    // We add the TVSs to the rpository counting on the fact that we did the merge from 0 upwards
    
    
    
    if (0 < DEBUG_LEVEL)
      TVLAAPIAssert.debugAssert(mergedInto[0] == copy[0]);
    
    inputToOutputMap[0] = repository.addTVS(mergedInto[0]);
    
    for (int i=1; i<use; i++) {
      if (mergedInto[i] == copy[i]) {
        // copy[i] is represented by itself
        inputToOutputMap[i] = repository.addTVS(mergedInto[i]);             
      }
      else {
        // copy[i] is represented by a TVS that we ahve already inserted into the TVS
        int indx = -2;
        for (int j=0; j < i ; j++) {
          if (mergedInto[i] == copy[j]) {
            indx = j;
            break;
          }
        }
        if (0 < DEBUG_LEVEL)
          TVLAAPIAssert.debugAssert(0 <= indx);
        
        inputToOutputMap[i] = inputToOutputMap[indx];
      }
    }
    
    int [] ret = (int[]) inputToOutputMap.clone();
    
    return IntArrayUtils.prune(ret);
  }
}
