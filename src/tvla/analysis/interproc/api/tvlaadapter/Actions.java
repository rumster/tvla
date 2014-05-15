
package tvla.analysis.interproc.api.tvlaadapter;

/**
 * Manager of actions 
 * TODO factor out the use of action instance 
 *  
 */


import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.analysis.interproc.api.utils.CollectionUtils;
import tvla.analysis.interproc.api.utils.TVLAAPIAssert;
import tvla.analysis.interproc.api.utils.TVLAAPIDebugControl;
import tvla.analysis.interproc.api.utils.TVLAAPITrace;
import tvla.analysis.interproc.semantics.ActionDefinition;
import tvla.analysis.interproc.semantics.ActionInstance;
import tvla.differencing.Differencing;
import tvla.language.PTS.ActionMacroAST;
import tvla.predicates.Vocabulary;
import tvla.transitionSystem.Action;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.ProgramProperties;

public class Actions {
  public static final int  DEBUG_LEVEL = TVLAAPIDebugControl.getDebugLevel(3);
  
  private Map macros = null;
  
  public Actions() {
    macros = HashMapFactory.make();
  }
  
  
  /**
   * This methos is ivoked (indirectly) by the PTS/SFT parser
   * @param action
   */
  public void actionAddDefinition(ActionMacroAST action) {
    ActionDefinition def;
    if (0 < DEBUG_LEVEL)
      TVLAAPIAssert.debugAssert(macros.get(action.getName()) == null);
    
    def = new ActionDefinition(action);
    macros.put(action.getName(), def);
    
    if (1 < DEBUG_LEVEL)
      TVLAAPITrace.tracePrintln("addActionDefinition adding ... " + action.getName());
  }
  
  
  /**
   * This method returns an action identifier which is is instantiated on demand to 
   * a TVLA action  
   * @param macroName name of action 
   * @param args the macro arguments 
   * @return
   */
  ActionInstance getActionInstance(
      String macroName, List args) {
    if (TVLAAPIAssert.ASSERT) {
      TVLAAPIAssert.debugAssert(macroName!= null);
      TVLAAPIAssert.debugAssert(args!= null);
    }
    
    ActionDefinition macDef = (ActionDefinition) macros.get(macroName);
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(macDef != null, "Attempt to use undefined macro: " + macroName);
    
    TVLAAPIAssert.debugAssert(macDef != null);
    
    ActionInstance ai = ActionInstance.getActionInstance(macDef, args);
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(ai != null, "Failed to instantiate macro " + macroName + " with arguments " + args);
    
    
    if (differencing && ! ai.cachedAction())
      differencingGenerateUpdates(ai);
    
    if (2 < DEBUG_LEVEL) {
      StringBuffer sb = new StringBuffer();
      Action action = ai.getAction();
      action.print(sb);
      TVLAAPITrace.tracePrintln(" ================  Macro " + ai.getMacroName(true) + "============") ;
      TVLAAPITrace.tracePrintln(sb.toString());
      TVLAAPITrace.tracePrintln(" =================================================================");
    }
    
    return ai;
  }
  
  /**
   * Verifies that the Macros define all the macros in the the given set
   */
  
  boolean init(Set expectedMacros) {
    Set macrosNames = macros.keySet();
    
    Set macrosNamesCopy = HashSetFactory.make(macrosNames);
    macrosNamesCopy.removeAll(expectedMacros);
    if (! macrosNamesCopy.isEmpty()) {
      TVLAAPITrace.tracePrintln("Analysis defines unused actions (initialization continues)");
      TVLAAPITrace.tracePrintln("  Unused actions: " + CollectionUtils.printCollections(macrosNamesCopy,false));
    }
    
    expectedMacros.removeAll(macrosNames);
    if (! expectedMacros.isEmpty()) {
      TVLAAPITrace.tracePrintln("Analysis does not define required actions! initialization failed");
      TVLAAPITrace.tracePrintln("  Undefiend actions: " + CollectionUtils.printCollections(expectedMacros,false));
      return false;
    }
    
    initDifferencingX();
    
    return true;
  }
  
  
  /*****************************************************************************
   * Differencing
   *****************************************************************************/
  public static class ActionsDifferencingConstants {
    public final static String differencingSkipForActionsStr = "tvla.api.skipDifferencigForActions";
    public final static String differencingSkipForActionsDefaultValueStr = null;
  }
  
  /**
   * Do we need to do differencing
   */
  private boolean differencing = false;
  
  /** 
   * Used for differencing plumbing 
   */
  private Set differencingClosedInstrumPreds;
  private List differencingInstrumPredsOrder;
  
  
  /**
   * A property containing whitspace seperated action names for which differencing
   * is NOT performed even if differencing is enabled.
   * The set contains the names of the actions.
   */
  private Set differencingSkippedActions;
  
  
  protected void initDifferencingX() {
    differencingSkippedActions = HashSetFactory.make(4);
    differencing = ProgramProperties.getBooleanProperty("tvla.differencing.difference", differencing);;
    
    
    differeningInitialize();
    
    List noDiff = ProgramProperties.getStringListProperty(ActionsDifferencingConstants.differencingSkipForActionsStr, null);
    if (noDiff != null) {
      Iterator itr = noDiff.iterator();
      while (itr.hasNext()) {
        String action = (String) itr.next();
        differencingSkippedActions.add(action);
        TVLAAPITrace.tracePrintln("Differencing will not be performed for action " + action);
      }
    }
  }
  
  protected void differeningInitialize() {
    Collection instrumPreds = Vocabulary.allInstrumentationPredicates();
    differencingClosedInstrumPreds = Differencing.gatherTClosedInstrumPreds(instrumPreds);
    differencingInstrumPredsOrder = Differencing.getOrder(instrumPreds, differencingClosedInstrumPreds);
  }
  
  protected void differencingGenerateUpdates(ActionInstance actionInstance) {
    
    String macro = actionInstance.getMacroName(false);
    if (differencingSkippedActions.contains(macro)) {
      if (2 < DEBUG_LEVEL)
        TVLAAPITrace.tracePrintln(" Skipping differencing for action " + macro);
      
      return;
    }
    
    Differencing.generateUpdates(
        actionInstance.getAction(),
        differencingInstrumPredsOrder,
        differencingClosedInstrumPreds);
  }
}
