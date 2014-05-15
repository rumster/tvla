package tvla.analysis.interproc.api.tvlaadapter;

/**
 *  This class provides the facilities to actually apply actions to TVSs.
 *  It is currently implemented as a hack over PASTA's Applier. 
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.interproc.api.tvlaadapter.abstraction.ITVLAVocabulary;
import tvla.analysis.interproc.api.utils.TVLAAPIDebugControl;
import tvla.analysis.interproc.api.utils.TVLAAPILog;
import tvla.analysis.interproc.api.utils.TVLAAPITrace;
import tvla.analysis.interproc.semantics.ActionInstance;
import tvla.api.TVLAAPIAnalysisFlags;
import tvla.api.ITVLAAPI.ITVLATabulatorServices;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.TVS;
import tvla.core.Combine.INullaryCombiner;
import tvla.io.StructureToDOT;
import tvla.logic.Kleene;
import tvla.util.Filter;
import tvla.util.ProgramProperties;
import tvla.util.StringUtils;

public class AbstractInterpreter {
  public static final int DEBUG_LEVEL = TVLAAPIDebugControl.getDebugLevel(3);
  
  // TODO get these prperties from the property file
  private final boolean doFocusProperty = true;

  private final boolean doCoerceAfterFocusProperty = TVLAAPIAnalysisFlags.Applier.doCoerceAfterFocusProperty;  

  private final boolean doCoerceAfterUpdateProperty = TVLAAPIAnalysisFlags.Applier.doCoerceAfterUpdateProperty; 

  private final boolean doBlurProperty = true;

  private final boolean freezeStructuresWithMessagesProperty = ProgramProperties.getBooleanProperty(
      "tvla.engine.freezeStructuresWithMessages", true);

  private final boolean breakIfCoerceAfterUpdateFailedProperty = ProgramProperties.getBooleanProperty(
      "tvla.engine.breakIfCoerceAfterUpdateFailed", true);

  private final boolean doLetUnary = ProgramProperties.getBooleanProperty("tvla.api.doLetUnary", false);
  private final boolean doLetBinary = ProgramProperties.getBooleanProperty("tvla.api.doLetBinary", false);
  
  // The underlying vocabulary
  final private ITVLAVocabulary voc;

  // apply all 1 strucute actions
  private APIApplier applier;

  
  // We need a special applier to 2 strucutres operations to ensure that there
  // is no coerce after the focus
  // and before the update as the strucutre is obviously not in a valid state
  private APIApplier combineApplier;

  private AnalysisStatus status;

  public AbstractInterpreter(ITVLAVocabulary voc, ITVLATabulatorServices engine) {
    status = new AnalysisStatus();

    if (!doBlurProperty) {
      throw new Error("Must blur after every action");
    }

    // FIXME: AnalysisStatus totalStatus = new AnalysisStatus();
    AnalysisStatus totalStatus = this.status;

    applier = new APIApplier(
        voc,
        engine, 
        totalStatus, 
        doFocusProperty, 
        doLetUnary, 
        doCoerceAfterFocusProperty, 
        doCoerceAfterUpdateProperty,
        doBlurProperty, 
        freezeStructuresWithMessagesProperty, 
        breakIfCoerceAfterUpdateFailedProperty, "Unary");

    // The combine applier does not execute coerce because it is applied to a
    // combined structre which is not valid.
    // The interpreter applies coerce afterwards for the resulting strucutre
    // after the update if the doCoerceAfterUpdateProperty,
    // and may also break (if breakIfCoerceAfterUpdateFailedProperty is set).
    combineApplier = new APIApplier(
        voc, 
        engine,
        totalStatus, 
        doFocusProperty, 
        doLetBinary, 
        false, 
        false,
        doBlurProperty, 
        freezeStructuresWithMessagesProperty, 
        false, "Binary");

  
    this.voc = voc;
  }

  /*****************************************************************************
   * 1 strucutre applier
   ****************************************************************************/

  public Collection apply(ActionInstance action, TVS input, Map msgs) {

    applier.getAnalysisStatus().startTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);
    Collection res = applier.apply(action, input, msgs);
    applier.getAnalysisStatus().stopTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);

    return res;
  }

  /*****************************************************************************
   * 2 strucutres appliers
   ****************************************************************************/

  public Collection applyCombine(INullaryCombiner nullaryCombiner, ActionInstance action, TVS tvsCall, TVS tvsExit, Map msgs) {

    applier.getAnalysisStatus().startTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);

    TVS copiedTVSCall = tvsCall.copy();
    TVS copiedTVSExit = tvsExit.copy();

    copiedTVSCall.setAll(voc.getTVLAInUc(), Kleene.trueKleene);
    copiedTVSExit.setAll(voc.getTVLAInUx(), Kleene.trueKleene);

    TVS combinedTVS = TVS.combine(nullaryCombiner, copiedTVSCall, copiedTVSExit);

    if (2 < DEBUG_LEVEL) {
      TVLAAPITrace.tracePrintln("Combined TVS = " + combinedTVS);
    }
    
    Collection res = combineApplier.apply(action, combinedTVS, msgs);

    assert (res != null);

    Iterator resItr = res.iterator();
    while (resItr.hasNext()) {
      final HighLevelTVS tvs = (HighLevelTVS) resItr.next();
      tvs.setAll(voc.getTVLAInUc(), Kleene.falseKleene);
      tvs.setAll(voc.getTVLAInUx(), Kleene.falseKleene);
      tvs.filterNodes(new Filter<Node>() {
        public boolean accepts(Node node) {
            return tvs.eval(voc.getTVLAKill(), node) != Kleene.trueKleene;
        }
      });
      boolean feasible = true;
      if (doCoerceAfterUpdateProperty)
        feasible = tvs.coerce();

      if (!feasible && breakIfCoerceAfterUpdateFailedProperty) {
        TVLAAPILog.println(StringUtils.newLine + "applyRet: "
            + " The analysis has stopped since a constraint was breached during the operation "
            + "of Coerce, after Update was applied!" + StringUtils.newLine + 
            "Action = " + action.toString() + StringUtils.newLine +
            " input TVS " + combinedTVS + StringUtils.newLine + 
            " updated TVS" + tvs);
        TVLAAPILog.println("// Input (combined) TVS");
        String combinedDT = StructureToDOT.defaultInstance.convert(combinedTVS);
        TVLAAPILog.println(combinedDT);
        
        TVLAAPILog.println("// Coerced TVS");
        String coercedDT = StructureToDOT.defaultInstance.convert(tvs);
        TVLAAPILog.println(coercedDT);

        
        status.finishAnalysis();
        // FIXME, should return emptry-set, but we throw an exception
        throw new InternalError("Coerce after update failed! ");
      }

    }

    applier.getAnalysisStatus().stopTimer(AnalysisStatus.TOTAL_ANALYSIS_TIME);

    return res;
  }

  public AnalysisStatus getStatus() {
    return status;
  }

  
/*
  public APIApplier getApplier() {
    return applier;
  }

  public boolean isBreakIfCoerceAfterUpdateFailedProperty() {
    return breakIfCoerceAfterUpdateFailedProperty;
  }

  public APIApplier getCombineApplier() {
    return combineApplier;
  }

  public boolean isDoBlurProperty() {
    return doBlurProperty;
  }

  public boolean isDoCoerceAfterFocusProperty() {
    return doCoerceAfterFocusProperty;
  }

  public boolean isDoCoerceAfterUpdateProperty() {
    return doCoerceAfterUpdateProperty;
  }

  public boolean isDoFocusProperty() {
    return doFocusProperty;
  }

  public boolean isFreezeStructuresWithMessagesProperty() {
    return freezeStructuresWithMessagesProperty;
  }
*/
}
