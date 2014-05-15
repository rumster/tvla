package tvla.analysis.interproc.api.tvlaadapter.abstraction;

import tvla.api.ITVLATVS;
import tvla.core.HighLevelTVS;
import tvla.core.common.ModifiedPredicates;
import tvla.io.StructureToDOT;

/**
 * A wrapper around HighLevelTVSs Needed to prevent HighLevelTVSs from
 * implementing the ITVLATVS interface
 * 
 * @author maon
 * 
 */

public class TVLATVS implements ITVLATVS {
  final private HighLevelTVS tvs;

  public TVLATVS(HighLevelTVS tvs) {
    this.tvs = tvs;
  }

  public String toDOT(String heading) {
    StructureToDOT converter = getDotConverter();

    return converter.convert(tvs, heading);
  }

  public String toString() {
    return tvs.toString();
  }

  /**
   * Exposes these operations ONLY to the members of the package
   * 
   * @return
   */
  public HighLevelTVS tvs() {
    return tvs;
  }

  void blur() {
    tvs.blur();
  }

  public boolean feasible() {
    return tvs.coerce();
  }

  // This should have been a static method
  // but htis would cmlicate the interface
  public void clearModifiedPredicates() {
    ModifiedPredicates.clear();
  }
  
  private StructureToDOT getDotConverter() {
    return StructureToDOT.defaultInstance;
  }
}
