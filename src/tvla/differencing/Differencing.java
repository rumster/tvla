package tvla.differencing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import tvla.core.decompose.CloseCycle;
import tvla.differencing.FormulaDifferencing.Delta;
import tvla.exceptions.SemanticErrorException;
import tvla.exceptions.UserErrorException;
import tvla.formulae.AndFormula;
import tvla.formulae.EquivalenceFormula;
import tvla.formulae.ExistQuantFormula;
import tvla.formulae.Formula;
import tvla.formulae.IfFormula;
import tvla.formulae.NotFormula;
import tvla.formulae.OrFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.PredicateUpdateFormula;
import tvla.formulae.QuantFormula;
import tvla.formulae.TransitiveFormula;
import tvla.formulae.ValueFormula;
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.predicates.Instrumentation;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.AnalysisGraph;
import tvla.transitionSystem.Location;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Logger;
import tvla.util.ProgramProperties;

public class Differencing {
   protected static final boolean strongSimplification = 
   ProgramProperties.getBooleanProperty("tvla.differencing.strongSimplification", false);

// Note that the properties below are initialized before any getInstance call
  // but after properties are loaded in Runner's main method.

  protected static boolean difference = // Is differencing enabled?
  ProgramProperties.getBooleanProperty("tvla.differencing.difference", false);

  protected static boolean differenceIdForms = // Should we ignore implicit
                                                // identity formulae?
  ProgramProperties.getBooleanProperty("tvla.differencing.differenceIdForms", false);

  protected static boolean differenceNonIdForms = // Should we ignore supplied
                                                  // (non-id) formulae?
  ProgramProperties.getBooleanProperty("tvla.differencing.differenceNonIdForms", false);

  protected static boolean tight = // Use tight versions of deltas?
  ProgramProperties.getBooleanProperty("tvla.differencing.tight", false);

  protected static boolean debug = // Send some flow information to the Logger?
  ProgramProperties.getBooleanProperty("tvla.differencing.debug", false);

  protected static boolean univChangeWarnOnce = // Should we warn about universe
                                                // changing actions once
  // or once per such action?
  ProgramProperties.getBooleanProperty("tvla.differencing.warnAboutUniverseChangingActionsOnce", false);

  protected static boolean messageForCloseCycle = // Should we give a message for close a cycle
      // on predicates defined as acyclic
  ProgramProperties.getBooleanProperty("tvla.differencing.messageForCloseCycle", true);
  
  
  // Stores actions with new, clone, or retain formulas for which we already
  // issued a warning about possible incorrectness of differencing.
  protected static Set<Action> warnedUniverseChangingActions = HashSetFactory.make(10);

  /**
   * A set of actions for which differencing is requested.
   */
  protected static Set<Action> registeredActions = HashSetFactory.make(20);

  public static void reset() {
	  registeredActions = HashSetFactory.make(20);
  }
  
  /**
   * Registers <code>action</code> for differencing.
   * 
   * @param action
   *          An action.
   */
  public static void registerAction(Action action) {
    registeredActions.add(action);
  }

  // =========================================================================
  // Complete the equation system with generated update formulas
  // =========================================================================

  public static void initializeDifferencing(Collection<Instrumentation> instrumPreds) {
    // We're only asked to process instrum preds in Collection instrumPreds.
    // However, some instrum preds that appear in (R)TC formulas may need to
    // be re-differenced to make sure we have a non-tight delta version.

    // Register all actions in all locations for differencing.
    // (In case AnalysisGraph is not null and actions in it were not
    // previously registered.)
    AnalysisGraph cfg = AnalysisGraph.activeGraph;
    if (cfg != null) {
      Iterator<Location> locIter = cfg.getLocations().iterator();
      while (locIter.hasNext()) {
        Location location = (Location) locIter.next();
        for (int actionNum = 0; actionNum < location.getActions().size(); actionNum++) {
          Action action = location.getAction(actionNum);
          registerAction(action);
          // generateUpdates(action, order, tClosedInstrumPreds);
        }
      }
    }
  }

  public static List<Instrumentation> getOrder(Collection<Instrumentation> instrumPreds, Set<Instrumentation> tClosedInstrumPreds) {
    Set<Instrumentation> instrumPredsToDifference = HashSetFactory.make(instrumPreds);
    instrumPredsToDifference.addAll(tClosedInstrumPreds);
    List<Instrumentation> order = getTopologicalOrder(instrumPredsToDifference);
    return order;
  }
  
  public static void differencing(Collection<Instrumentation> instrumPreds) {
    if (difference) {
      Set<Instrumentation> tClosedInstrumPreds = gatherTClosedInstrumPreds(instrumPreds);
      List<Instrumentation> order = getOrder(instrumPreds,tClosedInstrumPreds);
      
      if (debug) {
        Logger.println("\nAsked to difference:\n" + instrumPreds);
        Logger.println("\nOrdering of instrumentation predicates:");
        for (Iterator<Instrumentation> i = order.iterator(); i.hasNext();) {
          Instrumentation instr = i.next();
          Logger.print(instr + " ");
        }
        Logger.println("");

        Logger.println("\nInstrumentation predicates whose (R)TC is taken:");
        for (Iterator<Instrumentation> i = tClosedInstrumPreds.iterator(); i.hasNext();) {
          Instrumentation instr = (Instrumentation) i.next();
          Logger.print(instr + " ");
        }
        Logger.println("\n");
      }
      
      initializeDifferencing(instrumPreds);
      // Apply differencing to all reigstered actions.
      for (Iterator<Action> actionIter = registeredActions.iterator(); actionIter.hasNext();) {
        Action action = actionIter.next();
        try {
        	generateUpdates(action, order, tClosedInstrumPreds);
        }
        catch (SemanticErrorException e) {
        	e.append("while generating update formulae for action " + action);
        	throw e;
        }
      }
    }
  }

  private static void generateCloseCycleMessage(Action action, PredicateUpdateMaps predUpdateMaps) {
      if (messageForCloseCycle) {      
          // For every acyclic predicate
          for (Predicate binary : Vocabulary.allBinaryPredicates()) {
              if (!binary.acyclic()) continue;

              
              Formula definition = null;
              Var left = null;
              Var right = null;
              if (binary instanceof Instrumentation) {
                  Instrumentation instrum = (Instrumentation) binary;
                  definition = instrum.getFormula();
                  List<Var> vars = instrum.getVars();
                  left = vars.get(0);
                  right = vars.get(1);
              } else {
                  left = new Var("v1");
                  right = new Var("v2");
                  definition = new PredicateFormula(binary, left, right);
              }
              FormulaDifferencing fd = FormulaDifferencing.getInstance();
              Delta delta = fd.deltaFormulas(null, 
                      definition, predUpdateMaps, true);              
              if (isConstantKleene(delta.plus, Kleene.falseKleene)) {
                  // No edges added, no problem
                  continue;
              }
              Formula closeCycleCondition = null;
              // No edge removed and unique edge added - check dp[p](v1,v2) & p*(v2,v1)                  
              Var subLeft = Var.allocateVar();
              Var subRight = Var.allocateVar();
              Formula binaryTC = 
                  fd.constructTransitiveFormula(right, left, subLeft, subRight, 
                          new PredicateFormula(binary, subLeft, subRight));
              binaryTC = fd.findMatchingInstrPred(new PredicateFormula(binary, right, left), binaryTC);
              Formula binaryRTC = 
                  fd.constructReflexiveFormula(right, left, binaryTC); 
              binaryRTC = fd.findMatchingInstrPred(new PredicateFormula(binary, right, left), binaryRTC);

              boolean suppliedUpdate = 
		binaryRTC instanceof PredicateFormula &&
                predUpdateMaps.supplied.containsKey(((PredicateFormula) binaryRTC).predicate());
              if (suppliedUpdate || !fd.unitChangePlus(new PredicateFormula(binary, left, right), delta)) {
                  // non-unit change - use future version of p for RTC.
                  binaryRTC = fd.futureFormula("", null, binaryRTC, predUpdateMaps, tight);
              }
              // check dp[p](v1,v2) & p*(v2, v1)
              closeCycleCondition = ExistQuantFormula.close(fd.constructAndFormula(delta.plus, binaryRTC));
              action.addMessage(closeCycleCondition, 
                      "Closed cycle for acyclic predicate " + binary, 
                      CloseCycle.getDName(binary));
              if (debug)
                  Logger.println("\nAdded close cycle message with condition " + closeCycleCondition + "\n");
              
          }
          
      }      
  }

  private static boolean isConstantKleene(Formula formula, Kleene kleene) {
    boolean constantKleene = false;
      if (formula instanceof ValueFormula) {
          if (((ValueFormula) formula).value() == kleene) {
              constantKleene = true;
          }
      }
    return constantKleene;
  }

  public static void differencing() {
    differencing(Vocabulary.allInstrumentationPredicates());
  }

  // =========================================================================
  // Complete actions with generated update formulas
  // =========================================================================

  /**
   * Generate the update formulas (according to above options) for the given
   * action in the supplied order (List of objects of type Instrumentation) for
   * treating each instrumentation predicate.
   */
  public static void generateUpdates(Action action, List<Instrumentation> order, Set<Instrumentation> tClosedInstrumPreds) {
    if (debug)
      Logger.println("\nDifferencing for action " + action);

    PredicateUpdateMaps predUpdateMaps = 
        new PredicateUpdateMaps(
                HashMapFactory.<Predicate, PredicateUpdateFormula> make(action.getUpdateFormulae()), 
                HashMapFactory.<Predicate, PredicateUpdateFormula> make(),
                HashMapFactory.<Predicate, PredicateUpdateFormula> make());

    for (Iterator<Instrumentation> iterator = order.iterator(); iterator.hasNext();) {
      Instrumentation instrum = iterator.next();
      PredicateUpdateFormula update = action.getUpdateFormula(instrum);

      // Difference if no formula was supplied and differenceIdForms is set or
      // if formula is supplied and is marked as auto or differenceNonIdForms 
      // is set.
      // May need another trigger when controlled by abstraction refinement.
      // Alexey
      if ((update == null && differenceIdForms || update != null && (update.getAuto() || differenceNonIdForms))) {

        // If this action changes the universe (i.e., includes a new,
        // retain, or clone formula), then we should warn the user.
        if (action.isUniverseChanging()) {

          // If the flag below is set, then warn only once, otherwise warn once
          // for each action. Could put original AST references back into
          // actions and warn only once per schema but that would be messy.
          if (univChangeWarnOnce ? warnedUniverseChangingActions.isEmpty() : !warnedUniverseChangingActions.contains(action)) {

            System.err.println("\n\nWARNING!\n\tAttempting to generate an update for predicate " + instrum + "\n\tin action "
                + action + "\n\tThis is a universe-changing action (it contains a new, retain,\n"
                + "\tor clone formula).  Differencing ignores these formulae.\n"
                + "\tIf this instrumentation predicate can be affected by new/retain/clone,\n"
                + "\tplease define your own update and turn off differencing for it, or\n"
                + "\tswitch to the use of the freeList idiom for allocation/deallocation.");

            if (univChangeWarnOnce)
              System.err.println("This warning will not be issued for other universe-changing actions.\n\n");
            else
              System.err.println("This warning will not be issued for other instrumentation predicates.\n\n");
          }

          warnedUniverseChangingActions.add(action);
        }

        PredicateFormula predicateFormula = new PredicateFormula(instrum, instrum.getVars());
        if (debug && update != null) {
          Logger.println("\n\nIgnoring supplied update formula" + "\n\tpredicate:       " + predicateFormula
              + "\n\tsupplied update: " + update.getFormula());
        }

        String header = "action: " + action;
        Formula formula = FormulaDifferencing.getInstance().futureFormula(header, instrum, instrum.getFormula(), predUpdateMaps,
            tight);
        if (strongSimplification) {
            formula = FormulaDifferencing.getInstance().strongSimplify(formula);
        }

        // Do some sanity checks
        /*
        if (debug && update == null && !formula.equals(predicateFormula))
          Logger.println("\nWarning: obtained unexpected non-identity formula after differencing!" + "\n\tpredicate:       "
              + predicateFormula + "\n\tcomputed update: " + formula);
        else 
        */    
        if (debug && update != null && formula.equals(predicateFormula))
          Logger.println("\nWarning: obtained unexpected identity formula after differencing!" + "\n\tpredicate:       "
              + predicateFormula + "\n\tsupplied update: " + update.getFormula());

        // Save the newly generated update formula if it is not the identity,
        // or if it is the identity but wasn't supposed to be.
        if (difference && (!formula.equals(predicateFormula) || update != null)) {
          if (instrum.arity() == 0)
            action.setPredicateUpdateFormula(instrum, formula, update != null && update.getAuto());
          else
            action.setPredicateUpdateFormula(instrum, formula, ((PredicateFormula) predicateFormula).variables(), update != null
                && update.getAuto());
          if (tight)
            predUpdateMaps.generatedTight.put(instrum, action.getUpdateFormula(instrum));
          else
            predUpdateMaps.generatedUntight.put(instrum, action.getUpdateFormula(instrum));
        }

        if (!tight && tClosedInstrumPreds.contains(instrum)) {
          // Need to obtain a tight version of the update for this instrum pred.
          if (debug)
            Logger.println("\nObtaining a tight update for instrum pred " + instrum);

          formula = FormulaDifferencing.getInstance().futureFormula(header, instrum, instrum.getFormula(), predUpdateMaps, true);

          PredicateUpdateFormula tightUpdate = new PredicateUpdateFormula(formula, instrum, ((PredicateFormula) predicateFormula)
              .variables(), update != null && update.getAuto());
          predUpdateMaps.generatedTight.put(instrum, tightUpdate);
        }
      }
    }

    generateCloseCycleMessage(action, predUpdateMaps);
    
    if (debug)
      Logger.println("\nFinished differencing for action " + action + "\n");
  }

  // =========================================================================
  // Compute dependencies and topological order between instrumentation
  // predicates
  // =========================================================================

  /**
   * Collect in the set the instrumentation predicates used in the given
   * formula.
   */
  static private void gatherInstrumPreds(Formula inputFormula, Set<Instrumentation> result) {
    if (inputFormula instanceof PredicateFormula) {
      // Base case
      PredicateFormula formula = (PredicateFormula) inputFormula;
      Predicate pred = formula.predicate();
      if (pred instanceof Instrumentation) {
        result.add((Instrumentation) pred);
      }
    } else if (inputFormula instanceof NotFormula) {
      NotFormula formula = (NotFormula) inputFormula;
      gatherInstrumPreds(formula.subFormula(), result);
    } else if (inputFormula instanceof AndFormula) {
      AndFormula formula = (AndFormula) inputFormula;
      gatherInstrumPreds(formula.left(), result);
      gatherInstrumPreds(formula.right(), result);
    } else if (inputFormula instanceof OrFormula) {
      OrFormula formula = (OrFormula) inputFormula;
      gatherInstrumPreds(formula.left(), result);
      gatherInstrumPreds(formula.right(), result);
    } else if (inputFormula instanceof EquivalenceFormula) {
      EquivalenceFormula formula = (EquivalenceFormula) inputFormula;
      gatherInstrumPreds(formula.left(), result);
      gatherInstrumPreds(formula.right(), result);
    } else if (inputFormula instanceof IfFormula) {
      IfFormula formula = (IfFormula) inputFormula;
      gatherInstrumPreds(formula.condSubFormula(), result);
      gatherInstrumPreds(formula.trueSubFormula(), result);
      gatherInstrumPreds(formula.falseSubFormula(), result);
    } else if (inputFormula instanceof QuantFormula) {
      QuantFormula formula = (QuantFormula) inputFormula;
      gatherInstrumPreds(formula.subFormula(), result);
    } else if (inputFormula instanceof TransitiveFormula) {
      TransitiveFormula formula = (TransitiveFormula) inputFormula;
      gatherInstrumPreds(formula.subFormula(), result);
    }
  }

  public static List<Instrumentation> getTopologicalOrder(Collection<Instrumentation> instrumPreds) {
    /*
     * First, compute for each instrumentation predicate the instrumentation
     * predicates used in its defining formula.
     */
    Map<Instrumentation, Set<Instrumentation>> mapPredDependencies = HashMapFactory.make(instrumPreds.size());
    for (Iterator<Instrumentation> iterator = instrumPreds.iterator(); iterator.hasNext();) {
      Instrumentation instrum = (Instrumentation) iterator.next();
      Formula formula = instrum.getFormula();
      Set<Instrumentation> set = new TreeSet<Instrumentation>();
      gatherInstrumPreds(formula, set);
      mapPredDependencies.put(instrum, set);
    }
    /*
     * Now computes a topologival order and throw an exception if a cycle is
     * detected.
     */
    List<Instrumentation> order = new ArrayList<Instrumentation>(instrumPreds.size());

    Set<Instrumentation> setPreds = HashSetFactory.make(instrumPreds);
    while (!setPreds.isEmpty()) {
      boolean changed = false;
      // Search for an key with an empty associated set
      for (Iterator<Instrumentation> i = setPreds.iterator(); i.hasNext();) {
        Instrumentation instrum = (Instrumentation) i.next();
        Set<Instrumentation> set = mapPredDependencies.get(instrum);
        // Remove instrum preds that aren't being processed.
        set.retainAll(instrumPreds);
        if (set.isEmpty()) {
          order.add(instrum);
          i.remove(); // Removes from the set of keys
          mapPredDependencies.remove(instrum); // Remove from the map
          // Remove instrum from all dependencies
          for (Iterator<Instrumentation> j = setPreds.iterator(); j.hasNext();) {
            Instrumentation instrum2 = (Instrumentation) j.next();
            Set<Instrumentation> set2 = mapPredDependencies.get(instrum2);
            set2.remove(instrum);
          }
          changed = true;
        }
      }
      if (!changed) {
        throw new UserErrorException(
            "tvla.differencing.Differencing: dependency cycle between instrumentation predicates detected\n");
      }
    }
    return order;
  }

  // =========================================================================
  // Find the set of instrumentation predicates appearing in TransitiveFormulas
  // =========================================================================

  static private void gatherTClosedInstrumPreds(Formula inputFormula, Set<Instrumentation> result) {
    if (inputFormula instanceof TransitiveFormula) {
      TransitiveFormula formula = (TransitiveFormula) inputFormula;
      // Collect all instrum preds below into result.
      gatherInstrumPreds(formula.subFormula(), result);
    }

    else if (inputFormula instanceof NotFormula) {
      NotFormula formula = (NotFormula) inputFormula;
      gatherTClosedInstrumPreds(formula.subFormula(), result);
    } else if (inputFormula instanceof AndFormula) {
      AndFormula formula = (AndFormula) inputFormula;
      gatherTClosedInstrumPreds(formula.left(), result);
      gatherTClosedInstrumPreds(formula.right(), result);
    } else if (inputFormula instanceof OrFormula) {
      OrFormula formula = (OrFormula) inputFormula;
      gatherTClosedInstrumPreds(formula.left(), result);
      gatherTClosedInstrumPreds(formula.right(), result);
    } else if (inputFormula instanceof EquivalenceFormula) {
      EquivalenceFormula formula = (EquivalenceFormula) inputFormula;
      gatherTClosedInstrumPreds(formula.left(), result);
      gatherTClosedInstrumPreds(formula.right(), result);
    } else if (inputFormula instanceof IfFormula) {
      IfFormula formula = (IfFormula) inputFormula;
      gatherTClosedInstrumPreds(formula.condSubFormula(), result);
      gatherTClosedInstrumPreds(formula.trueSubFormula(), result);
      gatherTClosedInstrumPreds(formula.falseSubFormula(), result);
    } else if (inputFormula instanceof QuantFormula) {
      QuantFormula formula = (QuantFormula) inputFormula;
      gatherTClosedInstrumPreds(formula.subFormula(), result);
    }
  }

  /**
   * Collect in the set the instrumentation predicates used in TC subformulas of
   * instrumentation predicate definitions.
   */
  static public Set<Instrumentation> gatherTClosedInstrumPreds(Collection<Instrumentation> instrumPreds) {
    Set<Instrumentation> set = new TreeSet<Instrumentation>();

    for (Iterator<Instrumentation> iterator = instrumPreds.iterator(); iterator.hasNext();) {
      Instrumentation instrum = (Instrumentation) iterator.next();
      Formula formula = instrum.getFormula();
      gatherTClosedInstrumPreds(formula, set);
    }
    return set;
  }
}
