package tvla.analysis.interproc.api.tvlaadapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import tvla.analysis.interproc.api.utils.IntArrayUtils;
import tvla.analysis.interproc.api.utils.TVLAAPIAssert;
import tvla.analysis.interproc.api.utils.TVLAAPIDebugControl;
import tvla.api.ITVLATVSIndexIterator;
import tvla.core.HighLevelTVS;
import tvla.core.TVSFactory;
import tvla.core.TVSSet;
import tvla.util.MutableMapping;

public class TVSRepository {
  public static final int DEBUG_LEVEL = TVLAAPIDebugControl.getDebugLevel(6);

  protected final TVSSet relTVSSet;

  protected MutableMapping index;

  private ArrayList mergeMap;

  public TVSRepository() {
    relTVSSet = TVSFactory.getInstance().makeEmptySet(TVSFactory.JOIN_RELATIONAL);
    index = new MutableMapping();
    
    // index 0 is reserved for bottom
    int nullIndex = index.add(null);
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(nullIndex == 0);
    
    mergeMap = new ArrayList(4);
  }

  int[] addTVSs(Collection inputTVSs) {
    if (inputTVSs.isEmpty()) {
      return null;
    }

    int[] ret = new int[inputTVSs.size()];

    int i = 0;
    for (Iterator itr = inputTVSs.iterator(); itr.hasNext(); i++) {
      HighLevelTVS tvs = (HighLevelTVS) itr.next();
      // Bounds the tvs in case it was not

      mergeMap.clear();
      boolean isNew = relTVSSet.mergeWith(tvs, mergeMap);

      // We assume that the join is relational
      if (0 < DEBUG_LEVEL) {
        TVLAAPIAssert.debugAssert(isNew && mergeMap.isEmpty() || !isNew && !mergeMap.isEmpty());
      }

      int tvsPos = -1;

      if (mergeMap.isEmpty()) {
        tvsPos = index.add(tvs);
      } else {

        if (0 < DEBUG_LEVEL) {
          TVLAAPIAssert.debugAssert(mergeMap.size() == 1);
        }

        tvla.util.Pair pair = (tvla.util.Pair) mergeMap.get(0);
        tvsPos = index.getMappedIndex((HighLevelTVS) pair.second);

        if (0 < DEBUG_LEVEL) {
          TVLAAPIAssert.debugAssert(pair.first == tvs);
        }
      }

      if (0 < DEBUG_LEVEL) {
        TVLAAPIAssert.debugAssert(tvsPos != -1);
      }

      ret[i] = tvsPos;
    }

    return IntArrayUtils.prune(ret);
  }

  public int addTVS(HighLevelTVS tvs) {
    mergeMap.clear();
    boolean isNew = relTVSSet.mergeWith(tvs, mergeMap);
    int tvsPos = -1;
    if (isNew) {
      tvsPos = index.add(tvs);
    } else {
      for (Iterator existingItr = mergeMap.iterator(); existingItr.hasNext();) {
        tvla.util.Pair pair = (tvla.util.Pair) existingItr.next();

        if (pair.first == tvs) {
          tvsPos = index.getMappedIndex((HighLevelTVS) pair.second);
          break;
        }
      }
    }

    if (0 < DEBUG_LEVEL)
      TVLAAPIAssert.debugAssert(tvsPos != -1);

    int ret = tvsPos;

    return ret;
  }

  public HighLevelTVS getTVS(int indx) {
    return (HighLevelTVS) index.getMappedObject(indx);
  }

  int getIndex(HighLevelTVS tvs) {
    return index.getMappedIndex(tvs);
  }

  int getMaxIndex() {
    return index.getMappingSize() - 1;
  }

  public int getRepositorySize() {
    return index.getMappingSize();
  }

  ITVLATVSIndexIterator iterator() {
    return new ITVLATVSIndexIterator() {
      int curIndx = 1;

      public boolean hasNext() {
        return curIndx < index.getMappingSize();
      }

      public int next() {
        return curIndx++;
      }
    };
  }

}
