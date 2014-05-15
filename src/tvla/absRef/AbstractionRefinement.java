package tvla.absRef;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.Engine;
import tvla.core.Coerce;
import tvla.core.Constraints;
import tvla.core.Focus;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.TVSFactory;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignKleene;
import tvla.core.generic.AdvancedCoerce;
import tvla.core.generic.GenericCoerce;
import tvla.exceptions.AbstractionRefinementException;
import tvla.exceptions.SemanticErrorException;
import tvla.exceptions.TVLAException;
import tvla.exceptions.UserErrorException;
import tvla.formulae.AllQuantFormula;
import tvla.formulae.AndFormula;
import tvla.formulae.AtomicFormula;
import tvla.formulae.EqualityFormula;
import tvla.formulae.EquivalenceFormula;
import tvla.formulae.ExistQuantFormula;
import tvla.formulae.Formula;
import tvla.formulae.IfFormula;
import tvla.formulae.NotFormula;
import tvla.formulae.OrFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.QuantFormula;
import tvla.formulae.TransitiveFormula;
import tvla.formulae.ValueFormula;
import tvla.formulae.Var;
import tvla.io.IOFacade;
import tvla.language.TVM.TVMAST;
import tvla.language.TVM.TVMParser;
import tvla.language.TVP.TVPParser;
import tvla.language.TVS.TVSParser;
import tvla.logic.Kleene;
import tvla.predicates.Instrumentation;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.AnalysisGraph;
import tvla.transitionSystem.Location;
import tvla.util.HashSetFactory;
import tvla.util.Logger;
import tvla.util.Pair;
import tvla.util.ProgramProperties;

/** Generation of new instrumentation predicates based on query definition
    and results of fixed-point computation.
    @author Alexey Loginov
*/

public class AbstractionRefinement {
    // Is abstraction refinement enabled at all?
    protected static boolean refine =
	ProgramProperties.getBooleanProperty("tvla.absRef.refine", false);
	
    // Should an Unknown answer to a precondition result in an AR Exception?
    private static boolean throwUnknownPrecondException =
	ProgramProperties.getBooleanProperty("tvla.absRef.throwUnknownPreconditionException", false);

    // Should we introduce all subformulas of query at once to define new instrum preds?
    private static boolean introduceAllSubformulasAtOnce =
	ProgramProperties.getBooleanProperty("tvla.absRef.introduceAllSubformulasAtOnce", false);

    // Should we introduce only imprecise subformulas of query to define new instrum preds
    // when doing many at once?  Always introducing only imprecise when doing it one at a time.
    private static boolean introduceOnlyImpreciseSubformulas =
	!introduceAllSubformulasAtOnce ||
	ProgramProperties.getBooleanProperty("tvla.absRef.introduceOnlyImpreciseSubformulas", false);

    // Should we go into original instrumentation predicates when looking for a candidate?
    private static boolean analyzeDefnsOfStartingInstrumPreds =
	ProgramProperties.getBooleanProperty("tvla.absRef.analyzeDefnsOfStartingInstrumPreds", false);

    protected static boolean debug = // Send some flow information to the Logger?
	ProgramProperties.getBooleanProperty("tvla.absRef.debug", false);

    protected static boolean verbose = // Send more flow information to the Logger?
	debug && ProgramProperties.getBooleanProperty("tvla.absRef.verbose", false);

    // Which generated predicates be non-abstraction:
    // all - all nullary and unary
    // nullary - all nullary
    // unary - all unary
    // heuristic - use a heuristic (makeAbstraction()).
    // none - none of them
    protected static String nonAbstractionPredicates =
	ProgramProperties.getProperty("tvla.absRef.nonAbstractionPredicates", "none");

    // Should we reset the nonabs attribute of core predicates in findNewDefiningFormula?
    protected static boolean resetCoreNonabs =
	ProgramProperties.getBooleanProperty("tvla.absRef.resetCorePredNonabs", false);

    // Should we reset the nonabs attribute of instrumentation predicates in findNewDefiningFormula?
    // never - no
    // always - yes, whether or not the recursive call returns a new candidate
    // return - yes, if the recursive call returns a new candidate
    protected static String resetInstrumNonabs =
	ProgramProperties.getProperty("tvla.absRef.resetInstrumPredNonabs", "never");

    // Should we simply use the values stored in the TVS file for the new
    // predicate?  We must have ignored parse errors in initial parse of input.
    protected static boolean useTVSEntryOfNewPred =
	ProgramProperties.getBooleanProperty("tvla.absRef.useTVSEntryOfNewPred", false);

    // File containing the specification of the data-structure constructor
    // to be used for testing effectiveness of candidates on abstract input,
    // and for obtaining the most precise initial values for the new instrum pred.
    protected static String dataStructConsFileName =
	ProgramProperties.getProperty("tvla.absRef.dataStructureConstructor", "");

    // File containing the specification of the empty structure to be used for
    // testing effectiveness of candidates on abstract input, and for obtaining
    // the most precise initial values for the new instrum pred.  Only needed
    // if some core predicates have non-zero values in an empty structure. 
    protected static String emptyStructTVSFileName =
	ProgramProperties.getProperty("tvla.absRef.emptyStructureTVS", "");

    // Should we construct initial structures using the DSC on firts run?
    protected static boolean constructInitialStructures =
	ProgramProperties.getBooleanProperty("tvla.absRef.constructInitialStructures", false);

    // Should candidate defining formulas be tested for their potential for
    // improving the precision of the analysis?
    protected static boolean testEffectiveness =
	ProgramProperties.getBooleanProperty("tvla.absRef.testEffectiveness", false);

    // Should effectiveness be testing whether we see fewer 1/2 answers after
    // the final coerce than in the unfocused structure or should it be testing
    // whether we see more definite (0/1) answers?  This is the same thing
    // unless focus results in materialization or this action is a new/delete.
    // The latter case can (and should) be handled explicitly.
    // Because TVLA is often used to verify partial correctness without reasoning
    // about termination, seeing more 0/1 answers rather than seeing fewer 1/2
    // answers should be the default criterion.
    protected static boolean effectivenessCountUnknown =
	ProgramProperties.getBooleanProperty("tvla.absRef.effectivenessCountUnknown", false);

    // Should effectiveness (more 0/1 answers or fewer 1/2 answers) be expected
    // on all or only on at least one Out structure emerging from the final coerce?
    // Normal behavior suggests that we want to see effectiveness on all Out
    // structures but there may be situations in which this is too conservative.
    protected static boolean effectivenessReqOnAllOutStructures =
	ProgramProperties.getBooleanProperty("tvla.absRef.effectivenessReqOnAllOutStructures", false);

    // Should we check effectiveness on all actions?  If false, check it only on
    // actions which focus on or update some of the core predicates from the
    // candidate.  Later should figure out dependencies of core predicates from
    // the candidate based on constraints, and only test actions which focus on
    // or update predicates, on which core predicates from the candidate are
    // dependent.  (Of course, focusing on or updating an instrumentation predicate
    // should be treated as doing that to the core predicates in the definition.)
    protected static boolean effectivenessTestAllActions =
	ProgramProperties.getBooleanProperty("tvla.absRef.effectivenessTestAllActions", false);

    // Should and/or cases of pushForall/ExistQuantifier make a copy of the quantifier?
    // If they do, formula evaluation time will go up.  However, they may give new
    // opportunities for finding new instrumentation predicates.
    protected static boolean copyPushedQuantifier =
	ProgramProperties.getBooleanProperty("tvla.absRef.copyPushedQuantifier", false);

    /* Stores formulas that failed the effectiveness test, so that we can
       avoid generating them again. */
    protected static Collection ineffectiveDefiningFormulas = HashSetFactory.make();
    // Instrumentation predicates generated on previous iterations.
    protected static List generatedInstrumPreds = new ArrayList();

    // Saved analysis graph and empty store for DSC analysis.
    protected static AnalysisGraph dscCfg;
    protected static Collection emptyStore;

    // For restoring AnalysisGraph.activeGraph and programName property after DSC analysis.
    protected static AnalysisGraph programCfg;
    protected static String programName;

    /* Initial structure collection.  The collection does not change here,
       but the structures within it change with each iteration of refinement.
       The Runner passes the modified structures to the engine and here. */
    protected Collection initialStructures;

	/* Location, label, and action, when the AbstractionRefinementException
	 * happened.  It was due to the action's precondition evaluating to 1/2
	 * on exceptionStructure under exceptionAssignment.  These will only be
	 * set if throwUnknownPrecondException is set.  Should get the same action
	 * and location via Engine.getCurrentAction() & Engine.getCurrentLocation(). */
	protected Location exceptionLocation;
	protected String exceptionLabel;
	protected Action exceptionAction;
	protected TVS exceptionStructure;
	protected Assign exceptionAssign;

    // Engine for testing effectiveness of candidates on abstract input and
    // for obtaining the most precise initial values for new instrum preds.
    protected String engineType;

    // TVS file containing the initial input structures.  (Used to reload
    // initial structures after the introduction of a new predicate.) 
    protected String inputFile;

    // For locating the data-structure constructor spec file.
    protected String searchPath;

    protected boolean doFocus = true;
    protected boolean doCoerceAfterFocus = false;
    protected boolean doCoerceAfterUpdate = true;
    protected boolean freezeStructuresWithMessages = false;
    protected boolean breakIfCoerceAfterUpdateFailed = false;

    private static class IntPair {
	public int left;
	public int right;
	public IntPair(int left, int right) {
	    this.left = left;
	    this.right = right;
	}
    }

    public AbstractionRefinement(String engineType, String inputFile, String searchPath) {
	this.inputFile = inputFile;
	this.engineType = engineType;
	this.searchPath = searchPath;

	freezeStructuresWithMessages =
	    ProgramProperties.getBooleanProperty("tvla.engine.freezeStructuresWithMessages", false);
	breakIfCoerceAfterUpdateFailed =
	    ProgramProperties.getBooleanProperty("tvla.engine.breakIfCoerceAfterUpdateFailed", false);
		
	// Processing of action order property value.  Make sure these agree with
	// the settings in Engine.java.
	String action = ProgramProperties.getProperty("tvla.engine.actionOrder", "fpucb");
	int pos = 0;
	if (pos < action.length() && action.charAt(pos) == 'f') {
	    doFocus = true;
	    pos++;
	} else doFocus = false;

	if (pos < action.length() && action.charAt(pos) == 'c') {
	    doCoerceAfterFocus = true;
	    pos++;
	} else doCoerceAfterFocus = false;

	if (pos < action.length() && action.charAt(pos) == 'p')
	    pos++;
	else throw new UserErrorException("Illegal action order specified : " + action);

	if (pos < action.length() && action.charAt(pos) == 'u')
	    pos++;
	else throw new UserErrorException("Illegal action order specified : " + action);

	if (pos < action.length() && action.charAt(pos) == 'c') {
	    doCoerceAfterUpdate = true;
	    pos++;
	}
	else doCoerceAfterUpdate = false;

	if (pos < action.length() && action.charAt(pos) == 'b')
	    pos++;
    }

    /** A recursive helper traversal for pushQuantifiers that pushes
	the Forall quantifier of the input formula as deep as possible.
	Precondition: subFormula's quantifiers are already pushed in.
     */
    protected static Formula pushForallQuantifier(Var boundVar, Formula subFormula) {
	if (!subFormula.freeVars().contains(boundVar))
	    return subFormula;

	/* subFormula must contain boundVar as a free variable in all cases below. */
	if (subFormula instanceof NotFormula) {
	    NotFormula notSubFormula = (NotFormula) subFormula;
	    /* Switch NOT with Forall (making an Exists) and keep pushing
	       the new existential quantifier into notSubFormula. */
	    Formula newSubFormula = pushExistsQuantifier(boundVar, notSubFormula.subFormula());
	    return new NotFormula(newSubFormula);
	}
	else if (subFormula instanceof OrFormula) {
	    OrFormula orSubFormula = (OrFormula) subFormula;
	    if (!orSubFormula.left().freeVars().contains(boundVar)) {
		/* Push Forall past left Or operand and keep pushing it
		   into the right Or operand. */
		Formula newRightFormula = pushForallQuantifier(boundVar, orSubFormula.right());
		return new OrFormula(orSubFormula.left(), newRightFormula);
	    } else if (!orSubFormula.right().freeVars().contains(boundVar)) {
		/* Push Forall past right Or operand and keep pushing it
		   into the left Or operand. */
		Formula newLeftFormula = pushForallQuantifier(boundVar, orSubFormula.left());
		return new OrFormula(newLeftFormula, orSubFormula.right());
	    } else {
		/* Cannot push the universal quantifier deeper into Or.
		   Create the forall formula quantifying over the bound variable. */
		return new AllQuantFormula(boundVar, subFormula);
	    }
	}
	else if (subFormula instanceof AndFormula) {
	    AndFormula andSubFormula = (AndFormula) subFormula;
	    if (copyPushedQuantifier) {
		/* Push Forall past And and keep pushing it into left and right. */
		Formula newLeftFormula = pushForallQuantifier(boundVar, andSubFormula.left());
		Formula newRightFormula = pushForallQuantifier(boundVar, andSubFormula.right());
		return new AndFormula(newLeftFormula, newRightFormula);
	    } else {
		if (!andSubFormula.left().freeVars().contains(boundVar)) {
		    /* Push Forall past left And operand and keep pushing it
		       into the right And operand. */
		    Formula newRightFormula = pushForallQuantifier(boundVar, andSubFormula.right());
		    return new AndFormula(andSubFormula.left(), newRightFormula);
		} else if (!andSubFormula.right().freeVars().contains(boundVar)) {
		    /* Push Forall past right And operand and keep pushing it
		       into the left And operand. */
		    Formula newLeftFormula = pushForallQuantifier(boundVar, andSubFormula.left());
		    return new AndFormula(newLeftFormula, andSubFormula.right());
		} else {
		    /* Cannot push the universal quantifier deeper into And.
		       Create the forall formula quantifying over the bound variable. */
		    return new AllQuantFormula(boundVar, subFormula);
		}
	    }
	}
	else if (subFormula instanceof EquivalenceFormula) {
	    /* We could push the Forall deeper but we'd end up with a mess.
	       A v : (a(v) <-> b) is equivalent to
	       ((E v : a(v)) -> b) & (b -> (A v : a (v)))
	       Create the forall formula quantifying over boundVar. */
	    return new AllQuantFormula(boundVar, subFormula);
	}
	else if (subFormula instanceof IfFormula) {
	    IfFormula ifSubFormula = (IfFormula) subFormula;
	    if (!ifSubFormula.condSubFormula().freeVars().contains(boundVar)) {
		/* Push Forall past Cond operand and keep pushing it
		   into the True and False operands. */
		Formula newTrueFormula = pushForallQuantifier(boundVar, ifSubFormula.trueSubFormula());
		Formula newFalseFormula = pushForallQuantifier(boundVar, ifSubFormula.falseSubFormula());
		return new IfFormula(ifSubFormula.condSubFormula(), newTrueFormula, newFalseFormula);
	    } else {
		/* Cannot push the universal quantifier deeper into If.
		   Create the forall formula quantifying over boundVar. */
		return new AllQuantFormula(boundVar, subFormula);
	    }
	}
	else if (subFormula instanceof AllQuantFormula) {
	    AllQuantFormula allQuantSubFormula = (AllQuantFormula) subFormula;
	    Var subFormulaBoundVar = allQuantSubFormula.boundVariable();

	    // First try pushing the inner quantifier deeper.
	    Formula newSubFormula = pushForallQuantifier(subFormulaBoundVar,
							 allQuantSubFormula.subFormula());

	    if (newSubFormula instanceof AllQuantFormula &&
		subFormulaBoundVar.equals(((AllQuantFormula) newSubFormula).boundVariable())) {
		// We weren't able to push the quantifier of the subformula deeper.
		// Try switching the two quantifiers and pushing the outer quantifier
		// (over boundVar) into allQuantSubFormula's subformula.
		newSubFormula = pushForallQuantifier(boundVar, allQuantSubFormula.subFormula());

		if (newSubFormula instanceof AllQuantFormula &&
		    boundVar.equals(((AllQuantFormula) newSubFormula).boundVariable())) {
		    // We weren't able to push the outer quantifier (over boundVar)
		    // deeper, either.  Rebuild the original formula.
		    return new AllQuantFormula(boundVar, allQuantSubFormula);
		}
		else {
		    // We were able to push the outer quantifier (over boundVar) deeper.
		    // Return newSubFormula universally quantified over subFormulaBoundVar.
		    return new AllQuantFormula(subFormulaBoundVar, newSubFormula);
		}
	    }
	    else {
		// We were able to push the quantifier of the subformula deeper.
		// Now try pushing the outer quantifier deeper into the new subformula.
		return pushForallQuantifier(boundVar, newSubFormula);
	    }
	}
	else if (subFormula instanceof ExistQuantFormula ||
		 subFormula instanceof TransitiveFormula ||
		 subFormula instanceof AtomicFormula) {
	    /* Cannot push the universal quantifier deeper into these formulas.
	       Create the forall formula quantifying over boundVar. */
	    return new AllQuantFormula(boundVar, subFormula);
	}
	else {
	    throw new RuntimeException("Encountered an unfamiliar formula type: " +
				       subFormula.getClass().toString());
	}
    }

    /** A recursive helper traversal for pushQuantifiers that pushes
	the Exists quantifier of the input formula as deep as possible.
	Precondition: subFormula's quantifiers are already pushed in.
     */
    protected static Formula pushExistsQuantifier(Var boundVar, Formula subFormula) {
	if (!subFormula.freeVars().contains(boundVar))
	    return subFormula;

	/* subFormula must contain boundVar as a free variable in all cases below. */
	if (subFormula instanceof NotFormula) {
	    NotFormula notSubFormula = (NotFormula) subFormula;
	    /* Switch NOT with Exists (making a Forall) and keep pushing
	       the new universal quantifier into notSubFormula. */
	    Formula newSubFormula = pushForallQuantifier(boundVar, notSubFormula.subFormula());
	    return new NotFormula(newSubFormula);
	}
	else if (subFormula instanceof OrFormula) {
	    OrFormula orSubFormula = (OrFormula) subFormula;
	    if (copyPushedQuantifier) {
		/* Push Exists past Or and keep pushing it into left and right. */
		Formula newLeftFormula = pushExistsQuantifier(boundVar, orSubFormula.left());
		Formula newRightFormula = pushExistsQuantifier(boundVar, orSubFormula.right());
		return new OrFormula(newLeftFormula, newRightFormula);
	    } else {
		if (!orSubFormula.left().freeVars().contains(boundVar)) {
		    /* Push Exists past left Or operand and keep pushing it
		       into the right Or operand. */
		    Formula newRightFormula = pushExistsQuantifier(boundVar, orSubFormula.right());
		    return new OrFormula(orSubFormula.left(), newRightFormula);
		} else if (!orSubFormula.right().freeVars().contains(boundVar)) {
		    /* Push Exists past right Or operand and keep pushing it
		       into the left Or operand. */
		    Formula newLeftFormula = pushExistsQuantifier(boundVar, orSubFormula.left());
		    return new OrFormula(newLeftFormula, orSubFormula.right());
		} else {
		    /* Cannot push the existential quantifier deeper into Or.
		       Create the exists formula quantifying over the bound variable. */
		    return new ExistQuantFormula(boundVar, subFormula);
		}
	    }
	}
	else if (subFormula instanceof AndFormula) {
	    AndFormula andSubFormula = (AndFormula) subFormula;
	    if (!andSubFormula.left().freeVars().contains(boundVar)) {
		/* Push Exists past left And operand and keep pushing it
		   into the right And operand. */
		Formula newRightFormula = pushExistsQuantifier(boundVar, andSubFormula.right());
		return new AndFormula(andSubFormula.left(), newRightFormula);
	    } else if (!andSubFormula.right().freeVars().contains(boundVar)) {
		/* Push Exists past right And operand and keep pushing it
		   into the left And operand. */
		Formula newLeftFormula = pushExistsQuantifier(boundVar, andSubFormula.left());
		return new AndFormula(newLeftFormula, andSubFormula.right());
	    } else {
		/* Cannot push the existential quantifier deeper into And.
		   Create the exists formula quantifying over the bound variable. */
		return new ExistQuantFormula(boundVar, subFormula);
	    }
	}
	else if (subFormula instanceof EquivalenceFormula) {
	    /* Cannot push the existential quantifier deeper into this formula.
	       Create the exists formula quantifying over boundVar. */
	    return new ExistQuantFormula(boundVar, subFormula);
	}
	else if (subFormula instanceof IfFormula) {
	    IfFormula ifSubFormula = (IfFormula) subFormula;
	    if (!ifSubFormula.condSubFormula().freeVars().contains(boundVar)) {
		/* Push Exists past Cond operand and keep pushing it
		   into the True and False operands. */
		Formula newTrueFormula = pushExistsQuantifier(boundVar, ifSubFormula.trueSubFormula());
		Formula newFalseFormula = pushExistsQuantifier(boundVar, ifSubFormula.falseSubFormula());
		return new IfFormula(ifSubFormula.condSubFormula(), newTrueFormula, newFalseFormula);
	    } else {
		/* Cannot push the existential quantifier deeper into If.
		   Create the exists formula quantifying over boundVar. */
		return new ExistQuantFormula(boundVar, subFormula);
	    }
	}
	else if (subFormula instanceof ExistQuantFormula) {
	    ExistQuantFormula existQuantSubFormula = (ExistQuantFormula) subFormula;
	    Var subFormulaBoundVar = existQuantSubFormula.boundVariable();

	    // First try pushing the inner quantifier deeper.
	    Formula newSubFormula = pushExistsQuantifier(subFormulaBoundVar,
							 existQuantSubFormula.subFormula());

	    if (newSubFormula instanceof ExistQuantFormula &&
		subFormulaBoundVar.equals(((ExistQuantFormula) newSubFormula).boundVariable())) {
		// We weren't able to push the quantifier of the subformula deeper.
		// Try switching the two quantifiers and pushing the outer quantifier
		// (over boundVar) into existQuantSubFormula's subformula.
		newSubFormula = pushExistsQuantifier(boundVar, existQuantSubFormula.subFormula());

		if (newSubFormula instanceof ExistQuantFormula &&
		    boundVar.equals(((ExistQuantFormula) newSubFormula).boundVariable())) {
		    // We weren't able to push the outer quantifier (over boundVar)
		    // deeper, either.  Rebuild the original formula.
		    return new ExistQuantFormula(boundVar, existQuantSubFormula);
		}
		else {
		    // We were able to push the outer quantifier (over boundVar) deeper.
		    // Return newSubFormula existentially quantified over subFormulaBoundVar.
		    return new ExistQuantFormula(subFormulaBoundVar, newSubFormula);
		}
	    }
	    else {
		// We were able to push the quantifier of the subformula deeper.
		// Now try pushing the outer quantifier deeper into the new subformula.
		return pushExistsQuantifier(boundVar, newSubFormula);
	    }
	}
	else if (subFormula instanceof AllQuantFormula ||
		 subFormula instanceof TransitiveFormula ||
		 subFormula instanceof AtomicFormula) {
	    /* Cannot push the existential quantifier deeper into these formulas.
	       Create the exists formula quantifying over boundVar. */
	    return new ExistQuantFormula(boundVar, subFormula);
	}
	else {
	    throw new RuntimeException("Encountered an unfamiliar formula type: " +
				       subFormula.getClass().toString());
	}
    }

    /** A bottom-up traversal over the formula's structure
	that pushes quantifiers as deep as possible.
     */
    protected static Formula pushQuantifiers(Formula formula) {
	if (formula instanceof AllQuantFormula) {
	    AllQuantFormula allQuantFormula = (AllQuantFormula) formula;
	    Formula newSubFormula = pushQuantifiers(allQuantFormula.subFormula());
	    return pushForallQuantifier(allQuantFormula.boundVariable(), newSubFormula);
	}
	else if (formula instanceof ExistQuantFormula) {
	    ExistQuantFormula existQuantFormula = (ExistQuantFormula) formula;
	    Formula newSubFormula = pushQuantifiers(existQuantFormula.subFormula());
	    return pushExistsQuantifier(existQuantFormula.boundVariable(), newSubFormula);
	}
	else if (formula instanceof NotFormula) {
	    NotFormula notFormula = (NotFormula) formula;
	    Formula newSubFormula = pushQuantifiers(notFormula.subFormula());
	    return new NotFormula(newSubFormula);
	}
	else if (formula instanceof OrFormula) {
	    OrFormula orFormula = (OrFormula) formula;
	    Formula newLeftFormula  = pushQuantifiers(orFormula.left());
	    Formula newRightFormula = pushQuantifiers(orFormula.right());
	    return new OrFormula(newLeftFormula, newRightFormula);
	}
	else if (formula instanceof AndFormula) {
	    AndFormula andFormula = (AndFormula) formula;
	    Formula newLeftFormula  = pushQuantifiers(andFormula.left());
	    Formula newRightFormula = pushQuantifiers(andFormula.right());
	    return new AndFormula(newLeftFormula, newRightFormula);
	}
	else if (formula instanceof EquivalenceFormula) {
	    EquivalenceFormula equivFormula = (EquivalenceFormula) formula;
	    Formula newLeftFormula  = pushQuantifiers(equivFormula.left());
	    Formula newRightFormula = pushQuantifiers(equivFormula.right());
	    return new EquivalenceFormula(newLeftFormula, newRightFormula);
	}
	else if (formula instanceof IfFormula) {
	    IfFormula ifFormula = (IfFormula) formula;
	    Formula newCondFormula  = pushQuantifiers(ifFormula.condSubFormula());
	    Formula newTrueFormula  = pushQuantifiers(ifFormula.trueSubFormula());
	    Formula newFalseFormula = pushQuantifiers(ifFormula.falseSubFormula());
	    return new IfFormula(newCondFormula, newTrueFormula, newFalseFormula);
	}
	else if (formula instanceof TransitiveFormula) {
	    TransitiveFormula transitiveFormula = (TransitiveFormula) formula;
	    Formula newSubFormula = pushQuantifiers(transitiveFormula.subFormula());
	    return new TransitiveFormula(transitiveFormula.left(), transitiveFormula.right(),
					 transitiveFormula.subLeft(), transitiveFormula.subRight(),
					 newSubFormula);
	}
	else if (formula instanceof AtomicFormula) {
	    return formula;
	}
	else {
	    throw new RuntimeException("Encountered an unfamiliar formula type: " +
				       formula.getClass().toString());
	}
    }

    /** Returns true iff formula already defines an instrumentation predicate. */
    protected static boolean definesInstrumPred(Formula formula) {
	Iterator instrPredIter = Vocabulary.allInstrumentationPredicates().iterator();
	while (instrPredIter.hasNext()) {
	    Instrumentation instrum = (Instrumentation) instrPredIter.next();
		Formula instrumFormula = instrum.getFormula();

	    List defVars = instrumFormula.freeVars();
	    List formulaVars = formula.freeVars();
	    if (defVars.size() != formulaVars.size()) continue;

		// Test formula against the defining formula of instrum.
		// Make sure we use formula equality up to alpha renaming.  Alexey

	    // See if the formulas are equal without free var substitution.
	    if (formula.equals(instrumFormula)) return true;

	    // In order to avoid generating multiple instrumentation preds
	    // with different argument names and otherwise identical formulas,
	    // we replace free variables in the defining formula with those
	    // in the passed-in formula.  If the two formulas match, using
	    // formula for a new instrum pred is pointless.  The freeVars()
	    // method returns variables in the order, in which they appear
	    // in the formula, so that there is no need to try different
	    // permutations of variables.

	    // Create variable arrays for parallel substitution.
	    Var [] defVarArray = new Var[defVars.size()];
	    Var [] formulaVarArray = new Var[formulaVars.size()];

		// Convert the Lists into arrays defVars and formulaVars.
		defVars.toArray(defVarArray);
	    formulaVars.toArray(formulaVarArray);

	    // Substitute formulaVars for defVars in a copy of instrumFormula.
	    instrumFormula = instrumFormula.copy();
	    instrumFormula.safeSubstituteVars(defVarArray, formulaVarArray);

	    if (formula.equals(instrumFormula)) return true;
	}
	return false;
    }

    /** This is the implementation of function instrum from my prelim.
     *  Returns a Pair (candidate defining formula and its parents) if
     *  it succeeds in finding a candidate, and null otherwise.
     *  Additionally, collects in the last argument nonabstraction predicates
     * (instrumentation and core) that should become abstraction predicates. */
    protected Pair findNewDefiningFormula(Formula formula, TVS structure, Assign assign,
                                          Set nonabsPredsToAbs) {
	if (formula instanceof ValueFormula || formula instanceof EqualityFormula) {
	    return null;
	}
	else if (formula instanceof PredicateFormula) {
	    PredicateFormula predFormula = (PredicateFormula) formula;
	    Predicate pred = predFormula.predicate();
	    if (pred instanceof Instrumentation) {
		Instrumentation instrumPred = (Instrumentation) pred;

		Pair recCallResult = null;
		
		// Do not make the recursive call if this is an AR-generated instrum pred and
		// property analyzeDefnsOfStartingInstrumPreds is cleared.
		if (analyzeDefnsOfStartingInstrumPreds || generatedInstrumPreds.contains(instrumPred)) {
			// In order to maintain correspondence between variables and
			// their bindings in assign, we have to substitute variables in
			// the defining formula before passing it on in a recursive call.
			Formula definingFormula = instrumPred.getFormula().copy();
			Var [] defVars = new Var[instrumPred.getVars().size()];
			// The list gets copied to the defVars array.
			instrumPred.getVars().toArray(defVars);
			Var [] predVars = predFormula.variables();
			definingFormula.safeSubstituteVars(defVars, predVars);

			if (debug)
			    Logger.println("AbsRef: PredicateFormula case makes a recursive call:" +
				               "\n\tpredFormula:      " + predFormula +
				               "\n\tdefining formula: " + definingFormula);

			// No need to add any parents to the list returned by the recursive call here.
			recCallResult =
			  findNewDefiningFormula(definingFormula, structure, assign, nonabsPredsToAbs);
		}

		// If this predicate is nonabs, make it abs according to property settings.
		if (!instrumPred.abstraction() && instrumPred.arity() < 2 &&
			  (recCallResult != null && resetInstrumNonabs.equals("return") ||
			   resetInstrumNonabs.equals("always"))) {
			// A setAbstraction call by itself is not enough!
			// We must update the predicate categories in Vocabulary!
			Vocabulary.setAbstractionProperty(instrumPred, true);
			nonabsPredsToAbs.add(instrumPred);
			if (debug)
				Logger.println("\nAbsRef: instrumentation predicate " + predFormula + " made abstraction");
		}
		return recCallResult;
	    }
	    else {
		if (resetCoreNonabs && !pred.abstraction() && pred.arity() < 2) {
			// A setAbstraction call by itself is not enough!
			// We must update the predicate categories in Vocabulary!
			Vocabulary.setAbstractionProperty(pred, true);
			nonabsPredsToAbs.add(pred);
			if (debug)
				Logger.println("\nAbsRef: core predicate " + predFormula + " made abstraction");
		}
		return null;
	    }
	}
	else if (formula instanceof NotFormula) {
	    NotFormula notFormula = (NotFormula) formula;
	    Pair subCallResult =
	      findNewDefiningFormula(notFormula.subFormula(), structure, assign, nonabsPredsToAbs);
		if (subCallResult != null)
		    // Add notFormula to list of parents.
		    ((List) subCallResult.second).add(formula);
		return subCallResult;
	}
	else if (formula instanceof OrFormula ||
		 formula instanceof AndFormula) {

	    if (debug)
		Logger.println("AbsRef: " + formula.getClass() + " case:\n\tformula: " + formula);

	    // If this disjunction/conjunction is not already used to define
	    // an instrum predicate and it wasn't discarded before, try that first.
	    if (!definesInstrumPred(formula) && !ineffectiveDefiningFormulas.contains(formula)) {
		if (debug) Logger.println("\tReturning whole formula.");
		// Return formula with a new list to hold parents.
		return new Pair(formula, new ArrayList());
	    }

	    Formula left = (formula instanceof OrFormula) ?
		((OrFormula) formula).left() : ((AndFormula) formula).left();

	    if (introduceAllSubformulasAtOnce && !introduceOnlyImpreciseSubformulas ||
	        // Does the left subformula evaluate to 1/2 on structure with assign?
	        structure.evalFormulaForValue(left, assign, Kleene.unknownKleene).hasNext()) {
		if (debug) Logger.println("\tMaking a recursive call on left: " + left);
		Pair leftCallResult = findNewDefiningFormula(left, structure, assign, nonabsPredsToAbs);
		if (leftCallResult != null) {
		    if (debug) Logger.println("\tReturning result of call on left.");
		    // Add or/andFormula to list of parents.
		    ((List) leftCallResult.second).add(formula);
		    return leftCallResult;
		}
	    }

	    Formula right = (formula instanceof OrFormula) ?
		((OrFormula) formula).right() : ((AndFormula) formula).right();

	    if (introduceAllSubformulasAtOnce && !introduceOnlyImpreciseSubformulas ||
	        // Does the right subformula evaluate to 1/2 on structure with assign?
	        structure.evalFormulaForValue(right, assign, Kleene.unknownKleene).hasNext()) {
		if (debug) Logger.println("\tMaking a recursive call on right: " + right);
		Pair rightCallResult = findNewDefiningFormula(right, structure, assign, nonabsPredsToAbs);
		if (debug) Logger.println("\tReturning result of call on right.");
		if (rightCallResult != null)
		    // Add or/andFormula to list of parents.
		    ((List) rightCallResult.second).add(formula);
		return rightCallResult;
	    }

	    return null;	    
	}
	else if (formula instanceof EquivalenceFormula) {
		// If this equivalence formula is not already used to define an
		// instrum predicate and it wasn't discarded before, try that first.
		if (!definesInstrumPred(formula) && !ineffectiveDefiningFormulas.contains(formula)) {
		  // Return formula with a new list to hold parents.
		  return new Pair(formula, new ArrayList());
		}

	    EquivalenceFormula equivFormula = (EquivalenceFormula) formula;
	    // Should just translate to DNF but will need to remember about
	    // this translation when refining transition relations!  Alexey
	    if (debug)
		  Logger.println("EquivalenceFormula is not analyzed by " +
		                 "abstraction refinement for now!");
	    return null;
	}
	else if (formula instanceof IfFormula) {
		// If this If formula is not already used to define an instrum
		// predicate and it wasn't discarded before, try that first.
		if (!definesInstrumPred(formula) && !ineffectiveDefiningFormulas.contains(formula)) {
		  // Return formula with a new list to hold parents.
		  return new Pair(formula, new ArrayList());
		}

	    IfFormula ifFormula = (IfFormula) formula;
	    // Should just translate to DNF but will need to remember about
	    // this translation when refining transition relations!  Alexey
	    if (debug)
		  Logger.println("IfFormula is not analyzed by " +
		                 "abstraction refinement for now!");
	    return null;
	}
	else if (formula instanceof QuantFormula) {
	    // If this quantified formula is not already used to define
	    // an instrum predicate and it wasn't discarded before, try that first.
	    if (!definesInstrumPred(formula) && !ineffectiveDefiningFormulas.contains(formula)) {
		// Return formula with a new list to hold parents.
		return new Pair(formula, new ArrayList());
	    }

	    Formula subFormula = ((QuantFormula) formula).subFormula();
	    if (debug)
	      Logger.println("AbsRef: quantifier case" +
	                     "\n\tformula: " + formula + "\n\tsubFormula: " + subFormula);

	    if (introduceAllSubformulasAtOnce && !introduceOnlyImpreciseSubformulas) {
	    	Pair subCallResult = findNewDefiningFormula(subFormula, structure, assign, nonabsPredsToAbs);
			if (subCallResult != null)
			  // Add quantFormula to list of parents.
			  ((List) subCallResult.second).add(formula);
			return subCallResult;
	    }

	    // Try all extensions of assign, in which the subformula evaluates to 1/2
	    // under the extension.  Use the extended assignment in the recursive call.
	    Iterator unknownIter =
		structure.evalFormulaForValue(subFormula, assign, Kleene.unknownKleene);

	    // Keep trying all extensions until the recursive call comes back non-null!
	    // Need to understand how much the choice of assignment extension affects
	    // the success.  If the effect is at all significant, try using heuristics
	    // to pick assignment.  For example, see if one of the extensions, with which
	    // subFormula evaluates to 1/2, binds to a non-summary individual.  Alexey
	    while (unknownIter.hasNext()) {
		AssignKleene extension = (AssignKleene) unknownIter.next();

		if (debug)
		    Logger.println("AbsRef: quantifier case extends assignment with " + extension);

		assign.put(extension);
		Pair subCallResult = findNewDefiningFormula(subFormula, structure, assign, nonabsPredsToAbs);
		if (subCallResult != null) {
			// Add quantFormula to list of parents.
		    ((List) subCallResult.second).add(formula);
		    return subCallResult;
		}
	    }

	    return null;
	}
	else if (formula instanceof TransitiveFormula) {
	    // Note, this case is different from what is described in my prelim
	    // because we're looking at TC instead of RTC!  Fix this up!  Alexey

        if (debug)
            Logger.println("AbsRef: TC case ignored for now; assuming that the enclosing\n" +
                           "\tRTC has already been chosen to define a new instrum pred.");
        
	    // If this transitive formula is not already used to define
	    // an instrum predicate and it wasn't discarded before, try that first.
	    if (!definesInstrumPred(formula) && !ineffectiveDefiningFormulas.contains(formula)) {
		// Return formula with a new list to hold parents.
		return new Pair(formula, new ArrayList());
	    }

	    Formula subFormula = ((TransitiveFormula) formula).subFormula();

	    if (introduceAllSubformulasAtOnce && !introduceOnlyImpreciseSubformulas) {
	      Pair subCallResult = findNewDefiningFormula(subFormula, structure, assign, nonabsPredsToAbs);
	      if (subCallResult != null)
	        // Add transitiveFormula to list of parents.
	        ((List) subCallResult.second).add(formula);
	      return subCallResult;
		}

	    // Try all extensions of assign, in which the subformula evaluates to 1/2
	    // under the extension.  Use the extended assignment in the recursive call.
	    Iterator unknownIter =
		structure.evalFormulaForValue(subFormula, assign, Kleene.unknownKleene);

	    // Keep trying all extensions until the recursive call comes back non-null!
	    // Need to understand how much the choice of assignment extension affects
	    // the success.  If the effect is at all significant, try using some
	    // heuristics to pick assignment.  For example, see if one of the extensions,
	    // with which subFormula evaluates to 1/2, binds non-summary individuals.
	    // Otherwise, we probably want to make sure that subLeft and subRight are not
	    // bound to the same summary individual!  Alexey
	    while (unknownIter.hasNext()) {
		AssignKleene extension = (AssignKleene) unknownIter.next();

		if (debug)
		    Logger.println("AbsRef: transitive case extends assignment with " + extension);

		assign.put(extension);
		Pair subCallResult = findNewDefiningFormula(subFormula, structure, assign, nonabsPredsToAbs);
		if (subCallResult != null) {
		    // Add transitiveFormula to list of parents.
		    ((List) subCallResult.second).add(formula);
		    return subCallResult;
		}
	    }

	    return null;
	}
	else {
	    throw new RuntimeException("Encountered an unfamiliar formula type: " +
				       formula.getClass().toString());
	}
    }

    // Collect the core predicates focused by this action.
    protected Set getFocusedCorePreds(Action action) {
	Set focusedCorePreds = HashSetFactory.make();

	if (doFocus && action.getFocusFormulae().size() > 0) {
	    Iterator focusFormulaIter = action.getFocusFormulae().iterator();
	    while (focusFormulaIter.hasNext()) {
		Formula focusFormula = (Formula) focusFormulaIter.next();
		focusedCorePreds.addAll(GetFormulaCorePreds.get(focusFormula));
	    }
	}

	return focusedCorePreds;
    }

    // Collect the core predicates updated by this action.
    protected Set getUpdatedCorePreds(Action action) {
	Set updatedCorePreds = HashSetFactory.make();

	// Take all updated predicates then remove instrumentation predicates.
	updatedCorePreds.addAll(action.getUpdateFormulae().keySet());
	updatedCorePreds.removeAll(Vocabulary.allInstrumentationPredicates());

	return updatedCorePreds;
    }

    // Get the number of unknown answers of formula on structure.
    protected IntPair numDefiniteAndUnknownAnswers(TVS structure, Formula formula) {
	Iterator unknownIter =
	    structure.evalFormulaForValue(formula, new Assign(), Kleene.unknownKleene);

	// We have to iterate to count here because evalFormulaForValue
	// returns an Iterator (with no size).  Fix later.  Alexey
	int numUnknownAnswers;
	for (numUnknownAnswers = 0; unknownIter.hasNext(); numUnknownAnswers++)
	    unknownIter.next();

	// Compute the number of definite answers by subtracting the number of unknown
	// answers from the total number of tuples.
	int arity = formula.freeVars().size();
	int numNodes = structure.nodes().size();
    int numTuples = (int) Math.pow(numNodes, arity);

    if (numUnknownAnswers > numTuples) {
        throw new SemanticErrorException("Number of unknown answers of formula\n\t" +
                                         formula +
                                         "\n\texceeds the number of tuples:\n\t" +
                                         "numUnknownAnswers = " + numUnknownAnswers +
                                         "  numTuples = " + numTuples);
    }

	return new IntPair(numUnknownAnswers, numTuples - numUnknownAnswers);
    }

    // Helper to isEffective.  Takes a candidate defining formula, an action, and an In
    // structure at the action's location and returns true if evaluating the candidate
    // on the Out structure(s) corresponding to the In structure is "more precise".
    // The Out structures are computed by taking In structure through the focus, coerce,
    // precondition, update, and coerce steps of the action.  Because of focus and
    // (possibly) multiple assignments of precondition affecting update, there may be
    // several Out structures.
    // The definition of "more precise" is dependent on some flag settings and is still
    // in flux.  Read the code to see the current state.  Alexey
    protected boolean isEffective(Action action, String label,
    		TVS inStructure, Formula candDefFormula) {
    	// Get the numbers of definite and unknown answers of candDefFormula on inStructure.
    	IntPair counts_InStruct = numDefiniteAndUnknownAnswers(inStructure, candDefFormula);
    	int numUnknown_InStruct = counts_InStruct.left;
    	int numDefinite_InStruct = counts_InStruct.right;
    	
    	if (verbose)
    		Logger.println("\tnumber of 1/2 values of candidate on In  structure: " +
    				numUnknown_InStruct +
    				"\n\tnumber of 0/1 values of candidate on In  structure: " +
    				numDefinite_InStruct);
    	
    	// Now compute the structures that inStructure is transformed into after
    	// the final coerce. This is almost the same code as what is found in
    	// Engine.apply() up until the blurring step.
    	
    	// Focus
    	Collection focusResult = null;
    	if (doFocus && action.getFocusFormulae().size() > 0)
    		focusResult = Focus.focus(inStructure, action.getFocusFormulae());
    	else focusResult = Collections.singleton(inStructure);
    	
    	for (Iterator focusIt = focusResult.iterator(); focusIt.hasNext(); ) {
    		HighLevelTVS outStructure = (HighLevelTVS) focusIt.next();
    		
    		// Coerce if coerce-after-focus is enabled
    		if (doCoerceAfterFocus) {
    			boolean valid = outStructure.coerce();
    			if (!valid) continue;
    		}
    		
    		// Precondition evaluation
    		Collection assigns = action.checkPrecondition(outStructure);
    		
    		for (Iterator assignIt = assigns.iterator(); assignIt.hasNext(); ) {
    			Assign assign = (Assign)assignIt.next();
    			
    			// Don't need to check halt condition (if it were satisfied we'd be done).
    			// However, we need to check messages to ignore frozen structures.
    			Collection newMessages = action.reportMessages(outStructure, assign);
    			if (!newMessages.isEmpty() && freezeStructuresWithMessages)
    				continue;  // Don't consider this structure because it goes away.
    			
    			// Perform the update of this action.  Calling updatePredicates
    			// instead of action.evaluate() allows us to ignore  the effects
    			// of new, clone, and retain.  I think, this is good.  Alexey
    			//outStructure.setOriginalStructure(null);
    			outStructure.updatePredicates(action.getUpdateFormulae().values(), assign);
    			
    			// Coerce (if coerce after update is enabled)
    			if (doCoerceAfterUpdate) {
    				boolean valid = outStructure.coerce();
    				if (!valid)
    					continue;  // Don't consider this structures because it goes away.
    			}
    			
    			// Get the numbers of definite and unknown answers of candDefFormula on inStructure.
    			IntPair counts_OutStruct = numDefiniteAndUnknownAnswers(outStructure, candDefFormula);
    			int numUnknown_OutStruct = counts_OutStruct.left;
    			int numDefinite_OutStruct = counts_OutStruct.right;
    			
    			boolean effectiveThisOutStructure = effectivenessCountUnknown
    			? numUnknown_OutStruct < numUnknown_InStruct
    					: numDefinite_OutStruct > numDefinite_InStruct;
    					
    					if (verbose)
    						Logger.println("\tnumber of 1/2 values of candidate on Out structure: " +
    								numUnknown_OutStruct +
    								"\n\tnumber of 0/1 values of candidate on Out structure: " +
    								numDefinite_OutStruct);
    					
    					if (effectivenessReqOnAllOutStructures && !effectiveThisOutStructure) {
    						if (verbose)
    							Logger.println("\tIneffective on this Out structure, returning false.");
    						return false;
    					}
    					
    					if (!effectivenessReqOnAllOutStructures && effectiveThisOutStructure) {
    						if (verbose)
    							Logger.println("\tEffective on this Out structure, returning true.");
    						return true;
    					}
    					
    		}  // for (Iterator assignIt ...)
    	}  // for (Iterator focusIt ...)
    	
    	if (effectivenessReqOnAllOutStructures) {
    		if (verbose)
    			Logger.println("\tEffective on all Out structures, returning true.");
    		return true;
    	} else {
    		if (verbose)
    			Logger.println("\tIneffective on all Out structures, returning false.");
    		return false;
    	}
    }
    
    /** Returns true if defining an instrumentation predicate by candDefFormula
     will increase precision.  This can happen in one of two ways:
     1. the new instrum pred's value for the initial input (obtained
     via symbolic supervaluation, for instance) can be more precise
     than computing candDefFormula on initial input, and
     2. maintaining instrum pred can be done more precisely than
     recomputing candDefFormula after updates.
     
     To test 1, we'll need to involve symbolic supervaluation.  For
     now, I'll assume that precision has to come from 2.  Alexey
     
     To test 2, determine actions that affect (focus on or update) any
     any core predicates used in the core normal form of candDefFormula.
     For each such action compare the values of the candidate on In
     structures computed on the last analysis run at the action's location
     with the values of candDefFormula on corresponding Out structures at
     the locations (these are obtained by running an In structure through
     all steps until (but not including) blur).  If the latter values are
     more precise, candDefFormula is effective.
     
     The above test determines of precise values for the candidate
     instrumentation predicate are ever generated.  Possibly the more
     meaningful test would be to see if precise values are ever lost.
     This would involve comparing the candidate's values on Out structures
     as computed here with the candidate's values on the blurred and joined
     structures.  Unfortunately, the test would then have to be applied at
     all actions because, while precise values can only be generated by
     actions affecting the core preds in candidate, these values can be
     lost by actions that change seemingly irrelevant core preds (e.g.
     x = null may result in the blurring of the node that used to be
     pointed to by x with some other node).
     
     Another option to explore if the above tests turn out to be too
     expensive, is a collection of heuristics.  For example, if
     candDefFormula contains a quantifier that quantifies out a variable
     in a non-abstraction predicate, it can be effective.  Also, if
     candDefFormula is a TC formula, it can be effective.  Alexey
     */
    protected boolean isEffective(Formula candDefFormula) {
    	// 1. Test abstract input.
    	//Iterator inputStructIter = initialStructures.iterator();
    	// We need to compare to joined structures, rather than initial ones.
    	Location entryLocation = AnalysisGraph.activeGraph.getEntryLocation();
    	Iterator inputStructIter = entryLocation.allStructures();
    	while (inputStructIter.hasNext()) {
    		TVS inputStruct = (TVS) inputStructIter.next();
    		if (verbose)
    			Logger.println("AbsRef: testing effectiveness on input structure with " +
    					inputStruct.nodes().size() + " nodes");
    		
    		Iterator unknownIter =
    			inputStruct.evalFormulaForValue(candDefFormula, new Assign(), Kleene.unknownKleene);
    		
    		if (debug && unknownIter.hasNext()) {
    			Logger.print("AbsRef: candidate defining formula MAY BE EFFECTIVE!\n" +
    					"\tIt evaluates to 1/2 on input structure with " +
    					inputStruct.nodes().size() + " nodes\n" +
    			"\twith the following assignments:\n\t");
    			while (unknownIter.hasNext()) {
    				Logger.print(unknownIter.next() + "  ");
    			}
    			Logger.println();
    		}
    		if (verbose) Logger.println();
    	}
    	
    	// 2. Test transitions.
    	Set corePredsInCandDefFormula = GetFormulaCorePreds.get(candDefFormula);
    	if (verbose)
    		Logger.println("AbsRef: core preds in core normal form of candidate: " +
    				corePredsInCandDefFormula + "\n");
    	// Go through all actions (in all locations), and find all actions
    	// that modify one of the core predicates in corePredsInCandDefFormula.
    	Iterator locIter = AnalysisGraph.activeGraph.getLocations().iterator();
    	while (locIter.hasNext()) {
    		Location currentLocation = (Location) locIter.next();
    		if (verbose)
    			Logger.println("AbsRef: testing effectiveness at location " + currentLocation.label());
    		
    		for (int actionNum = 0; actionNum < currentLocation.getActions().size(); actionNum++) {
    			Action action = currentLocation.getAction(actionNum);
    			if (verbose)
    				Logger.println("AbsRef: testing effectiveness at action \"" + action + "\"");
    			
    			if (!effectivenessTestAllActions) {
    				// Get all core preds affected by this action (those focused on and those
    				// updated) and intersect this set with those in core normal form of candidate
    				// defining formula.  Later should figure out dependencies of core predicates
    				// from the candidate based on constraints, and only test actions which focus
    				// on or update predicates, on which core predicates from the candidate are
    				// dependent.  (Of course, focusing on or updating an instrumentation predicate
    				// should be treated as doing that to the core predicates in the definition.)
    				
    				Set affectedCorePreds = getUpdatedCorePreds(action);
    				if (doFocus && action.getFocusFormulae().size() > 0)
    					affectedCorePreds.addAll(getFocusedCorePreds(action));
    				
    				affectedCorePreds.retainAll(corePredsInCandDefFormula);
    				
    				if (affectedCorePreds.isEmpty())
    					// For now requiring that Core NF of candDefFormula contain a
    					// pred that is in precondition or is updated by this action.
    					continue;
    			}
    			
    			Iterator curLocStructIter = currentLocation.allStructures();
    			while (curLocStructIter.hasNext()) {
    				TVS curLocStruct = (TVS) curLocStructIter.next();
    				if (verbose)
    					Logger.println("\n\tTesting effectiveness on In structure with " +
    							curLocStruct.nodes().size() + " nodes");
    				
    				String label = currentLocation.label();
    				if (isEffective(action, label, curLocStruct, candDefFormula)) {
    					if (debug) {
    						Logger.println("\nAbsRef: candidate effective on In structure with " +
    								curLocStruct.nodes().size() + " nodes" +
    								"\n\tat action " + action +
    								"\n\tof location " + label + "\n");
    						IOFacade.instance().printStructure(curLocStruct,
    								"Candidate effective on In structure" +
    								"\nof action \\\"" + action +
    								"\\\"\nat location " + label +
    								"\ncandidate " + candDefFormula);
    					}
    					return true;
    				}
    			}
    		}
    		if (verbose)
    			Logger.println("\nAbsRef: candidate ineffective at this location!\n");
    	}
    	
    	return false;
    }
    
    // The number of instrumentation predicates introduced by refinement.
    protected static int refinementPredNumber = 0;
    
    protected static Instrumentation nameNewDefiningFormula(Formula formula,
    		boolean abstraction) {
    	/* Change to use a simple heuristic based on the top-level operator.
    	 It would be nice to be able to name n* by rtc[n], for example.  Alexey */
    	
    	// The arguments list will be in the order returned by freeVars,
    	// namely in the order in which they appear in the formula.
    	List arguments = new ArrayList(formula.freeVars());
    	return Vocabulary.createInstrumentationPredicate("__p" + (++refinementPredNumber),
    			arguments.size(), abstraction, formula,
    			arguments);
    }
    
    // Updates structures in the first argument with computed values of
    // uninitialized instrumentation predicates in the second argument.
    // Instrum preds are evaluated in the topological order computed by 
    // Differencing, so that values of instrum preds used in definitions
    // of other instrum preds get computed before those using them. 
    protected void computeValuesOfUninitInstrumPreds(Collection structures,
    		Collection instrumPreds) {
    	List instrumPredsInTopolOrder = tvla.differencing.Differencing.getTopologicalOrder(instrumPreds);
    	
    	Iterator structIter = structures.iterator();
    	while (structIter.hasNext()) {
    		TVS structure = (TVS) structIter.next();
    		// TODO: clean out the initialized predicate interface or remove.
    		//Collection initializedPredicates = structure.initializedPredicates;
    		Collection initializedPredicates = new ArrayList();
    		
    		if (debug) {
    			Logger.println("\nAbsRef: updating structure with " +
    					structure.nodes().size() + " nodes\n");
    			IOFacade.instance().printStructure(structure,
    					"Updating input structure\nwith values for "
    					+ instrumPreds.size() + " predicates");
    			Logger.println("AbsRef: TVS contains entries for the following predicates:\n\t"
    					+ initializedPredicates + "\n\twhich will "
    					+ (useTVSEntryOfNewPred ? "":"NOT ") + "be used!\n");
    		}
    		
    		// Process instrum preds in topological order.
    		Iterator topolOrderIter = instrumPredsInTopolOrder.iterator();
    		while (topolOrderIter.hasNext()) {
    			Instrumentation instrum = (Instrumentation) topolOrderIter.next();
    			
    			Formula instrumDefFormula = instrum.getFormula();
    			List instrumVars = instrum.getVars();
    			Formula instrumPredFormula = new PredicateFormula(instrum, instrumVars);
    			
    			if (useTVSEntryOfNewPred && initializedPredicates.contains(instrum)) {
    				if (debug && generatedInstrumPreds.contains(instrum))
    					Logger.println("AbsRef: Using TVS entry for new predicate!\n");
    				
    			} else {
    				structure.modify(instrum);
    				structure.clearPredicate(instrum);  // just to be safe
    				
    				Iterator satisfyIt = structure.evalFormula(instrumDefFormula, new Assign());
    				while (satisfyIt.hasNext()) {
    					AssignKleene result = (AssignKleene) satisfyIt.next();
    					
    					if (instrum.arity() == 0)
    						structure.update(instrum, result.kleene);
    					else {
    						// create the node tuple for this assignment
    						Node [] nodesTmp = new Node[instrum.arity()];
    						for (int index = 0; index < instrum.arity(); index++) {
    							Var ithVar = (Var) instrumVars.get(index);
    							nodesTmp[index] = result.get(ithVar);
    						}
    						NodeTuple tuple = NodeTuple.createTuple(nodesTmp);
    						structure.update(instrum, tuple, result.kleene);
    					}
    					
    				}  // while (satisfyIt.hasNext())
    			}  // else (if (useTVSEntryOfNewPred))
    			
    		}  // while (topolOrderIter.hasNext())
    		
    		if (debug)
    			IOFacade.instance().printStructure(structure,
    					"Updated input structure\nwith values for "
    					+ instrumPreds.size() + " predicates");
    	}  // while (structIter.hasNext())
    }
    
    protected Collection recomputeAbstractInput(Collection instrumPredsToDifference)
    throws Exception {
    	// Set activeGraph to point to the DSC CFG.
    	AnalysisGraph.activeGraph = dscCfg;
    	ProgramProperties.setProperty("tvla.programName", dataStructConsFileName);
    	
    	// Now compute values of instrumPredsToDifference.
    	// There is no precision loss in any computation on an empty structure.
    	if (!instrumPredsToDifference.isEmpty())
    		computeValuesOfUninitInstrumPreds(emptyStore, instrumPredsToDifference);
    	
    	// Output the DSC before analyzing it.
    	IOFacade.instance().printProgram(AnalysisGraph.activeGraph);
    	
    	clearAllLocations();  // Clear results of last DSC analysis.
    	
    	// Perform finite differencing.
    	tvla.differencing.Differencing.differencing(instrumPredsToDifference);
    	
    	if (debug)
    		Logger.println("\nAbsRef: Starting DS Constructor Analysis ...");
    	
    	// Perform the fixpoint finding analysis.
    	Engine.activeEngine.evaluate(emptyStore);
    	
    	// Dump graph after the data-structure-constructor run.
    	if (AnalysisGraph.activeGraph != null)
    		// this has no effect for multithreaded engines
    		AnalysisGraph.activeGraph.dump();
    	
    	Location exit = AnalysisGraph.activeGraph.getLocationByLabel("exit");
    	if (exit == null)
    		throw new UserErrorException("Data-structure constructor has no exit location!");
    	
    	Collection newInitialStructures = new ArrayList();
    	
    	for (Iterator i = exit.allStructures(); i.hasNext();
    	newInitialStructures.add(i.next()));
    	
    	if (debug)
    		Logger.println("\nAbsRef: All DS Constructor Analysis Tasks Completed");
    	
    	// Restore activeGraph to point to the program CFG.
    	AnalysisGraph.activeGraph = programCfg;
    	ProgramProperties.setProperty("tvla.programName", programName);
    	
    	return newInitialStructures;
    }  // recomputeAbstractInput
    
    protected Collection refineAbstractInput(Collection instrumPredsToDifference)
    throws Exception {
    	// This initialization call will re-init TVS storage
    	// and so take into account the new instrum pred.
    	TVSFactory.getInstance().init();
    	
    	if (!useTVSEntryOfNewPred &&
    			!dataStructConsFileName.equals("") && !dataStructConsFileName.equals("null"))
    		return recomputeAbstractInput(instrumPredsToDifference);
    	
    	// No data-structure constructor spec was specified.  Simply
    	// evaluate instrum's defining formula on initial input structures.
    	// However, if useTVSEntryOfNewPred is set, then we'll rely on the
    	// values for the new pred found in the TVS file (this feature is
    	// there for testing).
    	
    	//Iterator inputStructIter = entryLocation.allStructures();
    	// Use the pre-join initial structures.  Moreover, reload them
    	// from the TVS file since we reinitialized TVSFactory above.
    	
    	if (debug)
    		Logger.print("AbsRef: Re-reading TVS file ... ");
    	Collection newInitialStructures = TVSParser.readStructures(inputFile);
    	
    	// Update new initial structures with values of all uninitialized instrum preds
    	// generated by AR so far.  Note, that this assumes that original predicates whose
    	// definitions were changed already had precise values.  Otherwise, we'd need to use
    	// the more precise of the computed values and stored values for those predicates.
    	computeValuesOfUninitInstrumPreds(newInitialStructures, generatedInstrumPreds);
    	
    	return newInitialStructures;
    }  // refineAbstractInput
    
    // Refine abstract input and replace structures in inStructures
    // with new ones, if recomputing using the DSC.
    protected void refineAbstractInput(Collection inStructures,
    		Collection instrumPredsToDifference) throws Exception {
    	Collection newInStructures = refineAbstractInput(instrumPredsToDifference);
    	// Now replace the original initial structures with the new ones.
    	// These will be passed to the Engine on the next analysis run.
    	inStructures.clear();
    	inStructures.addAll(newInStructures);
    }
    
    /** Replaces instrum.getFormula() with instrum(args) in formula. */
    protected Formula replaceDefWithPred(Formula formula, Instrumentation instrum) {
    	if (instrum.arity() == formula.freeVars().size()) {
    		// Test if formula is the same as instrum's definition.
    		
    		// For now try comparing the formula as is (with no var substitutions).
    		// Note, equality in subclasses of Formula should check for equality
    		// up to alpha renaming.  This is done by using property setting
    		// tvla.formulae.alphaRenamingEquals = true.
    		Formula instrumFormula = instrum.getFormula();
    		List instrumVars = instrum.getVars();
    		PredicateFormula instrumPredFormula = new PredicateFormula(instrum, instrumVars);
    		
    		if (formula.equals(instrumFormula)) {
    			if (debug)
    				Logger.println("AbsRef: matched defining formula for " + instrumPredFormula +
    						" without var substitutions:\n\t" + formula );
    			return instrumPredFormula;
    		}
    		
    		// Now try substituting free vars of formula (in order of their
    		// appearance in the formula) for instrumVars.  If the number
    		// of free variables in formula is the same as the predicate's
    		// arity, then if there is any permutation of free variables
    		// that works, it will be found because instrumVars are ordered
    		// by appearance in the defining formula.  (However, we will have
    		// missed the chance to substitute the predicate if variables
    		// have to be repeated in the arguments.) 
    		
    		List formulaVars = formula.freeVars();
    		
    		// Create variable arrays for parallel substitution.
    		Var [] instrumVarArray = new Var[instrumVars.size()];
    		Var [] formulaVarArray = new Var[formulaVars.size()];
    		
    		// Convert the Lists into arrays defVars and formulaVars.
    		instrumVars.toArray(instrumVarArray);
    		formulaVars.toArray(formulaVarArray);
    		
    		// Substitute formulaVars for defVars in a copy of instrumFormula.
    		instrumFormula = instrumFormula.copy();
    		instrumFormula.safeSubstituteVars(instrumVarArray, formulaVarArray);
    		
    		if (formula.equals(instrumFormula)) {
    			if (debug)
    				Logger.println("AbsRef: matched defining formula for " + instrumPredFormula +
    						" with variable substitutions:" + "\n\t" +
    						instrumVars + " -> " + formulaVars + "\n\tformula: " + formula );
    			instrumPredFormula = new PredicateFormula(instrum, formulaVars);
    			return instrumPredFormula;
    		} 
    	}
    	
    	if (formula instanceof AllQuantFormula) {
    		AllQuantFormula allQuantFormula = (AllQuantFormula) formula;
    		Formula newSubFormula = replaceDefWithPred(allQuantFormula.subFormula(), instrum);
    		return new AllQuantFormula(allQuantFormula.boundVariable(), newSubFormula);
    	}
    	else if (formula instanceof ExistQuantFormula) {
    		ExistQuantFormula existQuantFormula = (ExistQuantFormula) formula;
    		Formula newSubFormula = replaceDefWithPred(existQuantFormula.subFormula(), instrum);
    		return new ExistQuantFormula(existQuantFormula.boundVariable(), newSubFormula);
    	}
    	else if (formula instanceof NotFormula) {
    		NotFormula notFormula = (NotFormula) formula;
    		Formula newSubFormula = replaceDefWithPred(notFormula.subFormula(), instrum);
    		return new NotFormula(newSubFormula);
    	}
    	else if (formula instanceof OrFormula) {
    		OrFormula orFormula = (OrFormula) formula;
    		Formula newLeftFormula  = replaceDefWithPred(orFormula.left(), instrum);
    		Formula newRightFormula = replaceDefWithPred(orFormula.right(), instrum);
    		return new OrFormula(newLeftFormula, newRightFormula);
    	}
    	else if (formula instanceof AndFormula) {
    		AndFormula andFormula = (AndFormula) formula;
    		Formula newLeftFormula  = replaceDefWithPred(andFormula.left(), instrum);
    		Formula newRightFormula = replaceDefWithPred(andFormula.right(), instrum);
    		return new AndFormula(newLeftFormula, newRightFormula);
    	}
    	else if (formula instanceof EquivalenceFormula) {
    		EquivalenceFormula equivFormula = (EquivalenceFormula) formula;
    		Formula newLeftFormula  = replaceDefWithPred(equivFormula.left(), instrum);
    		Formula newRightFormula = replaceDefWithPred(equivFormula.right(), instrum);
    		return new EquivalenceFormula(newLeftFormula, newRightFormula);
    	}
    	else if (formula instanceof IfFormula) {
    		IfFormula ifFormula = (IfFormula) formula;
    		Formula newCondFormula  = replaceDefWithPred(ifFormula.condSubFormula(), instrum);
    		Formula newTrueFormula  = replaceDefWithPred(ifFormula.trueSubFormula(), instrum);
    		Formula newFalseFormula = replaceDefWithPred(ifFormula.falseSubFormula(), instrum);
    		return new IfFormula(newCondFormula, newTrueFormula, newFalseFormula);
    	}
    	else if (formula instanceof TransitiveFormula) {
    		TransitiveFormula transitiveFormula = (TransitiveFormula) formula;
    		Formula newSubFormula = replaceDefWithPred(transitiveFormula.subFormula(), instrum);
    		return new TransitiveFormula(transitiveFormula.left(), transitiveFormula.right(),
    				transitiveFormula.subLeft(), transitiveFormula.subRight(),
    				newSubFormula);
    	}
    	else if (formula instanceof AtomicFormula) {
    		return formula.copy();
    	}
    	else {
    		throw new RuntimeException("Encountered an unfamiliar formula type: " +
    				formula.getClass().toString());
    	}
    	
    }
    
    /** Replaces instrum(args) with instrum.getFormula() in formula. */
    protected Formula replacePredWithDef(Formula formula, Instrumentation instrum) {
    	PredicateFormula instrumPredFormula = new PredicateFormula(instrum, instrum.getVars());
    	
    	if (formula instanceof PredicateFormula) {
    		// Test if formula is the same as instrumPredFormula.
    		
    		// This test should include a clause for the two formulas
    		// being equal up to a renaming of free vars.  However,
    		// this would involve trying all permutations of the free
    		// vars of formula.  Alternatively, we could collect free
    		// vars in two lists in order of their appearance in the
    		// two formulas.  The two lists would define the from and
    		// to arrays to substitute for testing equality.  Alexey
    		
    		// For now try comparing the formula as is (with no var substitutions).
    		// Note, equality in subclasses of Formula should check for equality
    		// up to alpha renaming.  This is done by using property setting
    		// tvla.formulae.alphaRenamingEquals = true.
    		if (formula.equals(instrumPredFormula)) {
    			if (debug)
    				Logger.println("AbsRef: matched instrumentation predicate " + instrum +
    						" without var substitutions:\n\t" + formula );
    			return instrum.getFormula().copy();
    		}
    		else return formula.copy();
    	}
    	else if (formula instanceof AllQuantFormula) {
    		AllQuantFormula allQuantFormula = (AllQuantFormula) formula;
    		Formula newSubFormula = replacePredWithDef(allQuantFormula.subFormula(), instrum);
    		return new AllQuantFormula(allQuantFormula.boundVariable(), newSubFormula);
    	}
    	else if (formula instanceof ExistQuantFormula) {
    		ExistQuantFormula existQuantFormula = (ExistQuantFormula) formula;
    		Formula newSubFormula = replacePredWithDef(existQuantFormula.subFormula(), instrum);
    		return new ExistQuantFormula(existQuantFormula.boundVariable(), newSubFormula);
    	}
    	else if (formula instanceof NotFormula) {
    		NotFormula notFormula = (NotFormula) formula;
    		Formula newSubFormula = replacePredWithDef(notFormula.subFormula(), instrum);
    		return new NotFormula(newSubFormula);
    	}
    	else if (formula instanceof OrFormula) {
    		OrFormula orFormula = (OrFormula) formula;
    		Formula newLeftFormula  = replacePredWithDef(orFormula.left(), instrum);
    		Formula newRightFormula = replacePredWithDef(orFormula.right(), instrum);
    		return new OrFormula(newLeftFormula, newRightFormula);
    	}
    	else if (formula instanceof AndFormula) {
    		AndFormula andFormula = (AndFormula) formula;
    		Formula newLeftFormula  = replacePredWithDef(andFormula.left(), instrum);
    		Formula newRightFormula = replacePredWithDef(andFormula.right(), instrum);
    		return new AndFormula(newLeftFormula, newRightFormula);
    	}
    	else if (formula instanceof EquivalenceFormula) {
    		EquivalenceFormula equivFormula = (EquivalenceFormula) formula;
    		Formula newLeftFormula  = replacePredWithDef(equivFormula.left(), instrum);
    		Formula newRightFormula = replacePredWithDef(equivFormula.right(), instrum);
    		return new EquivalenceFormula(newLeftFormula, newRightFormula);
    	}
    	else if (formula instanceof IfFormula) {
    		IfFormula ifFormula = (IfFormula) formula;
    		Formula newCondFormula  = replacePredWithDef(ifFormula.condSubFormula(), instrum);
    		Formula newTrueFormula  = replacePredWithDef(ifFormula.trueSubFormula(), instrum);
    		Formula newFalseFormula = replacePredWithDef(ifFormula.falseSubFormula(), instrum);
    		return new IfFormula(newCondFormula, newTrueFormula, newFalseFormula);
    	}
    	else if (formula instanceof TransitiveFormula) {
    		TransitiveFormula transitiveFormula = (TransitiveFormula) formula;
    		Formula newSubFormula = replacePredWithDef(transitiveFormula.subFormula(), instrum);
    		return new TransitiveFormula(transitiveFormula.left(), transitiveFormula.right(),
    				transitiveFormula.subLeft(), transitiveFormula.subRight(),
    				newSubFormula);
    	}
    	else if (formula instanceof AtomicFormula) {
    		return formula;
    	}
    	else {
    		throw new RuntimeException("Encountered an unfamiliar formula type: " +
    				formula.getClass().toString());
    	}
    	
    }
    
    // Introduces all constraints that would have been generated automatically if instrum
    // with its current definition were part of the specification from the start.
    // This code is copied from InstrumPredicateAST.java.  It should probably
    // be moved to one place, so we can call it from both places.  We may also
    // want to do this somewhere else.  Think about that later.  Alexey
    protected void addConstraints(Instrumentation instrum) {
    	if (!Constraints.automaticConstraints)
    		return;
    	
    	Formula instrumDefFormula = instrum.getFormula();
    	List instrumVars = instrum.getVars();
    	Formula instrumPredFormula = new PredicateFormula(instrum, instrumVars);
    	
    	// Add the definition constraints
    	// Added copying instrumDefFormula in several places for now.  This fixed an exception.
    	// Should clear up copying in Generic/AdvancedCoerce and in prenexToDNF.  Alexey
    	Constraints.getInstance().addConstraint(instrumDefFormula.copy(), instrumPredFormula.copy());
    	Constraints.getInstance().addConstraint(new NotFormula(instrumDefFormula.copy()), 
    			new NotFormula(instrumPredFormula.copy()));
    	
    	// If the definition is expanded to extended horn, create the closure.
    	
    	// Get the prenex DNF normal form.
    	Formula prenexDNF = Formula.toPrenexDNF(instrumDefFormula.copy());
    	boolean negated = false;
    	
    	// If this instrum pred is defined as pk(v1..vk) = A (v1..vl) pl(v1..vl),
    	// (l > k, and vars can be in any order), then create constraint
    	// pk(v1..vk) | pl(v1..vl) ==> pl(v1..vl).  (We really want
    	// pk(v1..vk) ==> pl(v1..vl) but we OR on pl(v1..vl) in the body to
    	// appease TVLA with the same set of free vars in the head and the body. 
    	if (prenexDNF instanceof AllQuantFormula) {
    		// First get to the top subformula that's not a forall.
    		AllQuantFormula allFormula = (AllQuantFormula) prenexDNF;
    		Formula subFormula = allFormula.subFormula();
    		while (subFormula instanceof AllQuantFormula)
    			subFormula = ((AllQuantFormula) subFormula).subFormula();
    		
    		if (subFormula instanceof PredicateFormula) {
    			// Now create the constraint pk(v1..vk) | pl(v1..vl) ==> pl(v1..vl).
    			PredicateFormula predSubFormula = (PredicateFormula) subFormula;
    			Formula body = new OrFormula(instrumPredFormula.copy(), predSubFormula.copy());
    			Formula head = predSubFormula.copy();
    			Constraints.getInstance().addConstraint(body, head);
    		}   
    	}
    	
    	// Remove all existential quantifiers.
    	while (true) {
    		if (prenexDNF instanceof ExistQuantFormula) {
    			ExistQuantFormula eformula = (ExistQuantFormula) prenexDNF;
    			prenexDNF = eformula.subFormula();
    		}
    		else {
    			break;
    		}
    	}
    	
    	if ((prenexDNF instanceof AllQuantFormula) || (prenexDNF instanceof OrFormula)) {
    		// Try the negated formula.
    		negated = true;
    		prenexDNF = Formula.toPrenexDNF(new NotFormula(instrumDefFormula.copy()));
    		// Remove all existential quantifiers.
    		while (true) {
    			if (prenexDNF instanceof ExistQuantFormula) {
    				ExistQuantFormula eformula = (ExistQuantFormula) prenexDNF;
    				prenexDNF = eformula.subFormula();
    			}
    			else {
    				break;
    			}
    		}
    	}
    	
    	if (prenexDNF instanceof AndFormula) {
    		// OK. This is a good candidate for closure.
    		List terms = new ArrayList();
    		Formula.getAnds(prenexDNF, terms);
    		for (Iterator termIt = terms.iterator(); termIt.hasNext(); ) {
    			Formula term = (Formula) termIt.next();
    			Formula origTerm = term;
    			boolean negatedTerm = false;
    			if (term instanceof NotFormula) {
    				NotFormula nterm = (NotFormula) term;
    				term = nterm.subFormula();
    				negatedTerm = true;
    			}
    			
    			if ((term instanceof PredicateFormula) ||
    					(negatedTerm && (term instanceof EqualityFormula))) {
    				// The body formula is the terms without this term and 
    				// with the negated instrumentation. All the variables not in the head are
    				// existentialy quantified.
    				Formula body = negated ?
    						instrumPredFormula.copy() : new NotFormula(instrumPredFormula.copy());
    						for (Iterator otherTermIt = terms.iterator(); otherTermIt.hasNext(); ) {
    							Formula otherTerm = (Formula) otherTermIt.next();
    							if (otherTerm == origTerm)
    								continue;
    							body = new AndFormula(body, otherTerm.copy());
    						}
    						Formula head = negatedTerm ? term.copy() : new NotFormula(term.copy());
    						Set freeVars = HashSetFactory.make(body.freeVars());
    						freeVars.removeAll(head.freeVars());
    						for (Iterator varIt = freeVars.iterator(); varIt.hasNext(); ) {
    							Var var = (Var) varIt.next();
    							body = new ExistQuantFormula(var, body);
    						}
    						
    						Constraints.getInstance().addConstraint(body, head);
    			}
    		}
    	}
    }
    
    protected void replaceDefinitionOccurences(Instrumentation instrum,
    		List changedInstrumPreds) {
    	boolean replacedPredDefn = false;
    	boolean replacedPrecond = false;
    	
    	// Go through all other instrumentation predicates, and replace instrum's
    	// defining formula with its name (instrum(vars)) in their defining formulas.
    	Iterator instrPredIter = Vocabulary.allInstrumentationPredicates().iterator();
    	while (instrPredIter.hasNext()) {
    		Instrumentation otherInstrum = (Instrumentation) instrPredIter.next();
    		if (otherInstrum.equals(instrum)) continue;
    		
    		Formula otherInstrumDef = otherInstrum.getFormula();
    		
    		// Instrum's definition comes from query with pushed quantifiers.  To get
    		// matches we need to push quantifiers in the definition of otherInstrum.
    		Formula otherInstrumDefWithPushedQuants = pushQuantifiers(otherInstrumDef);
    		
    		// See if the definition of otherInstrum (with pushed quantifiers) has an
    		// occurence of the definition of instrum as a subformula.  If so, replace
    		// the occurence with the use of instrum with appropriate arguments.
    		Formula newOtherInstrumDef = replaceDefWithPred(otherInstrumDefWithPushedQuants, instrum);
    		
    		if (!newOtherInstrumDef.equals(otherInstrumDefWithPushedQuants)) {
    			replacedPredDefn = true;
    			if (!changedInstrumPreds.contains(otherInstrum))
    				changedInstrumPreds.add(otherInstrum);  // signal only the first change
    			
    			// Set the defining formula of otherInstrum.
    			otherInstrum.setFormula(newOtherInstrumDef);
    			
    			if (debug) {
    				PredicateFormula otherInstrumFormula =
    					new PredicateFormula(otherInstrum, otherInstrum.getVars());
    				Logger.println("AbsRef: replaced definition of " + otherInstrumFormula +
    						"\n\told: " +  otherInstrumDef +
    						"\n\tnew: " +  newOtherInstrumDef);
    			}
    		}
    	}
    	if (debug) Logger.println();
    	
    	// Go through all actions (in all locations), and replace instrum's
    	// defining formula with its name (instrum(vars)) in preconditions.
    	Iterator locIter = AnalysisGraph.activeGraph.getLocations().iterator();
    	while (locIter.hasNext()) {
    		Location location = (Location) locIter.next();
    		
    		for (int actionNum = 0; actionNum < location.getActions().size(); actionNum++) {
    			Action action = location.getAction(actionNum);
    			
    			// Get the action's precondition formula.
    			// Any other places in need of refinement?  Focus, coerce?
    			Formula precondFormula = action.getPrecondition();
    			if (precondFormula == null) continue;
    			
    			// Instrum's definition comes from query with pushed quantifiers.  To get
    			// matches we need to push quantifiers in the precondition formula.
    			Formula precondFormulaWithPushedQuants = pushQuantifiers(precondFormula);
    			
    			// See if precondition (with pushed quantifiers) has an occurence of the
    			// definition of instrum as a subformula.  If so, replace the occurence
    			// with the use of instrum with appropriate arguments.
    			Formula newPrecondFormula = replaceDefWithPred(precondFormulaWithPushedQuants, instrum);
    			
    			if (!newPrecondFormula.equals(precondFormulaWithPushedQuants)) {
    				replacedPrecond = true;
    				
    				// Set the action's precondition formula.
    				action.precondition(newPrecondFormula);
    				
    				if (debug)
    					Logger.println("AbsRef: replaced precondition of action \"" + action +
    							"\" at location " + location.label() +
    							"\n\told: " + precondFormula +
    							"\n\tnew: " + newPrecondFormula);
    			}
    		}
    	}
    	
    	if (!replacedPredDefn & !replacedPrecond) {
    		PredicateFormula instrumFormula = new PredicateFormula(instrum, instrum.getVars());
    		throw new TVLAException("\n\tNo predicate definition or precondition replaced\n" +
    				"\tafter the introduction of predicate " + instrumFormula +
    				"\tdefined as " + instrum.getFormula()); 
    	}
    }
    
    /** Decides based on the nonAbstractionPredicates setting whether
     *  formula with given parents, should define an abstraction or a
     *  non-abstraction predicate.  One of the settings is "heuristic".
     *  The current heuristic returns true if each parent is of type
     *  Forall, And, or Or.  This is not very effective.  Should make
     *  this more flexible by providing a property interface. */
    protected boolean makeAbstraction(Formula formula, List parents) {
    	int arity = formula.freeVars().size();
    	
    	if (arity >= 2 ||
    			nonAbstractionPredicates.equals("all") ||
    			nonAbstractionPredicates.equals("nullary") && arity == 0 ||
    			nonAbstractionPredicates.equals("unary") && arity == 1)
    		return false;
    	
    	if (!nonAbstractionPredicates.equals("heuristic"))
    		return true;
    	
    	for (Iterator iter = parents.iterator(); iter.hasNext();) {
    		Formula parentFormula = (Formula) iter.next();
    		if (parentFormula instanceof AllQuantFormula ||
    				parentFormula instanceof OrFormula ||
    				parentFormula instanceof AndFormula)
    			continue;
    		else {
    			if (debug)
    				Logger.println("AbsRef: making new predicate abstraction:" +
    						"\n\tdefiningFormula: " + formula +
    						"\n\tparent: " + parentFormula + "\n");
    			return true;
    		}
    	}
    	
    	// No bad parents found, so make non-abstraction.
    	return false;
    }
    
    protected Instrumentation introduceNewInstrumPred(Action action, String label,
    		Formula query, TVS structure,
    		Set nonabsPredsToAbs) throws Exception {
    	Formula queryWithPushedQuants = pushQuantifiers(query);
    	
    	if (debug) {
    		Logger.println("AbsRef: query and query with pushed quantifiers are:" +
    				"\n\tquery:  " + query + "\n\tpushed: " + queryWithPushedQuants);
    		IOFacade.instance().printStructure(structure, "Refining in response to query:\n" +
    				query + "\nof action \\\"" + action +
    				"\\\"\nat location " + label);
    	}
    	
    	Formula candidateDefiningFormula;
    	List candidateParents;
    	while(true) {
    		Pair candidatePair = findNewDefiningFormula(queryWithPushedQuants, structure, new Assign(),
    				nonabsPredsToAbs);
    		if (candidatePair == null) return null;
    		candidateDefiningFormula = (Formula) candidatePair.first;
    		candidateParents = (List) candidatePair.second;
    		
    		if (debug)
    			Logger.println("\nAbsRef: candidate defining formula:\n\t  " +
    					candidateDefiningFormula + "\n");
    		
    		if (!testEffectiveness || isEffective(candidateDefiningFormula)) break;
    		else ineffectiveDefiningFormulas.add(candidateDefiningFormula);
    		
    		if (debug)
    			Logger.println("AbsRef: candidate defining formula is ineffective.\n\n");
    	}
    	
    	boolean abstraction = makeAbstraction(candidateDefiningFormula, candidateParents);
    	Instrumentation newInstrumPred = nameNewDefiningFormula(candidateDefiningFormula, abstraction);
    	generatedInstrumPreds.add(newInstrumPred);
    	if (debug) {
    		List instrumVars = newInstrumPred.getVars();
    		Formula instrumPredFormula = new PredicateFormula(newInstrumPred, instrumVars);
    		Logger.println("AbsRef: candidate defining formula is effective." +
    				"\n\tcandidate defining formula:\n\t  " + candidateDefiningFormula +
    				"\n\twill be used to define " +
    				(abstraction ? "an abstraction" : "a non-abstraction") + " predicate" +
    				"\n\tcandidate named " + instrumPredFormula + "\n");
    	}
    	
    	return newInstrumPred;
    }
    
    // Replaces occurences of new defining formulas in queries and old
    // defining formulas with uses of corresp. instrumentation predicates;
    // creates constraints based on new definions; invokes differencing
    // for changed and new instrum preds.  Returns the set of changed and
    // new instrumentation predicates.
    protected Collection refineTransitionRels(List newInstrumPreds) {
    	List changedInstrumPreds = new ArrayList();
    	
    	// First make all replacements of definitions with predicate names.
    	for (Iterator iter = newInstrumPreds.iterator(); iter.hasNext();) {
    		Instrumentation newInstrum = (Instrumentation) iter.next();
    		// Replace occurences of the definition of the new instrum pred with the
    		// use of the predicate.  Currently, this is done for precondition formulas
    		// and definitions of other instrumentation predicates.  Collect predicates
    		// with changed definitions in second argument.
    		replaceDefinitionOccurences(newInstrum, changedInstrumPreds);
    	}
    	
    	// Now introduce all constraints using the final versions of
    	// definitions of new and changed instrumentation predicates.
    	
    	for (Iterator iter = newInstrumPreds.iterator(); iter.hasNext();) {
    		Instrumentation newInstrum = (Instrumentation) iter.next();
    		// Introduce all constraints that would have been generated automatically
    		// if instrum were part of the specification from the start.
    		addConstraints(newInstrum);
    	}
    	
    	for (Iterator iter = changedInstrumPreds.iterator(); iter.hasNext();) {
    		// Introduce all constraints that would have been generated automatically
    		// if the new definition were used for otherInstrum from the start.
    		Instrumentation otherInstrum = (Instrumentation) iter.next();
    		if (!newInstrumPreds.contains(otherInstrum))
    			// Some of the changed instrum preds are new, they're already done.
    			addConstraints(otherInstrum);
    	}
    	
    	// Re-init coerce classes with new collection of constraints.
    	GenericCoerce.defaultGenericCoerce = new GenericCoerce(Constraints.getInstance().constraints());
    	HighLevelTVS.advancedCoerce = new AdvancedCoerce(Constraints.getInstance().constraints());
    	
    	// Perform finite differencing.
    	Set instrumPredsToDifference = HashSetFactory.make(newInstrumPreds);
    	instrumPredsToDifference.addAll(changedInstrumPreds);
    	tvla.differencing.Differencing.differencing(instrumPredsToDifference);
    	
    	return instrumPredsToDifference;
    }
    
    // Introduces new instrumentation predicates (one or more depending on
    // property settings and results of calls to introduceNewInstrumPred).
    // Also, collects in the last argument nonabstraction predicates (core
    // and instrumentation) that should become abstraction predicates.
    protected List introduceNewInstrumPredicates(
    		Action action, String label, TVS structure, Set nonabsPredsToAbs) throws Exception {
    	
    	Formula query = action.getPrecondition();
    	List newInstrumPreds = new ArrayList();
    	
    	Instrumentation newInstrumPred =
    		introduceNewInstrumPred(action, label, query, structure, nonabsPredsToAbs);
    	if (newInstrumPred != null) newInstrumPreds.add(newInstrumPred);
    	
    	if (introduceAllSubformulasAtOnce)
    		while(newInstrumPred != null) {
    			// Action's precondition (query) can be changed by replaceDefinitionOccurences!
    			query = action.getPrecondition();
    			newInstrumPred =
    				introduceNewInstrumPred(action, label, query, structure, nonabsPredsToAbs);
    			if (newInstrumPred != null) newInstrumPreds.add(newInstrumPred);
    		}
    	
    	// Perform refinement steps only once.  Refine transition relations
    	// if new instrumentation predicates were added, and refine abstract
    	// input if new instrumentation predicates were added or some old
    	// nonabstraction instrumentation or core predicates became abstraction.
    	
    	Collection instrumPredsToDifference = Collections.EMPTY_SET;
    	if (!newInstrumPreds.isEmpty())
    		// Refine transition relations first.  Then the newly added constraints
    		// constraints will be taken into account in the data-structure constructor.
    		instrumPredsToDifference = refineTransitionRels(newInstrumPreds);
    	
    	if (!newInstrumPreds.isEmpty() || !nonabsPredsToAbs.isEmpty())
    		// Refine abstract input and replace structures in initialStructures
    		// with new ones, if recomputing using the DSC.
    		refineAbstractInput(initialStructures, instrumPredsToDifference);
    	
    	return newInstrumPreds;
    }
    
    /** Clears structures at all locations, so that we can run the
     next iteration of analysis. */
    protected void clearAllLocations() {
    	// Clear structures at all locations.
    	Iterator locIter = AnalysisGraph.activeGraph.getLocations().iterator();
    	while (locIter.hasNext()) {
    		Location location = (Location) locIter.next();
    		location.clearLocation();
    	}
    }
    
    // Initializes the DSC and constructs abstract input using the DSC.
    public void constructAbstractInput(Collection initial) throws Exception {
    	if (!constructInitialStructures && (!refine || useTVSEntryOfNewPred) ||
    			dataStructConsFileName.equals("") || dataStructConsFileName.equals("null"))
    		return;  // Skip this if DSC use is not requested.
    	
    	// First read in and store the CFG for the data-structure constructor
    	// and its input (empty store).
    	
    	// Save the actual program CFG, so that activeGraph can be
    	// set to the data-structure constructor.
    	// TODO: Should clean out all uses of this global var in TVLA later.
    	programCfg = AnalysisGraph.activeGraph;
    	programName = ProgramProperties.getProperty("tvla.programName", "program");
    	
    	File dsConsFile = new File(dataStructConsFileName + ".tvp");
    	if (!dsConsFile.exists())
    		throw new UserErrorException("Data-structure constructor file " +
    				dataStructConsFileName + ".tvp not found!");
    	
    	if (debug)
    		Logger.print("\nAbsRef: Loading data-structure constructor spec ... ");
    	ProgramProperties.setProperty("tvla.programName", dataStructConsFileName);
    	AnalysisStatus.loadTimer.start();
    	
    	if (engineType.equals("tvla")) {
    		AnalysisGraph.activeGraph = new AnalysisGraph();
    		TVPParser.configure(dataStructConsFileName, searchPath);
    		AnalysisGraph.activeGraph.init();
    		// IOFacade.instance().printProgram(AnalysisGraph.activeGraph);
    	} 
    	else if (engineType.equals("tvmc")) {
    		TVMAST tvmFile = TVMParser.configure(dataStructConsFileName, searchPath);
    		tvmFile.compileAll();
    	}
    	else if (engineType.equals("ddfs")) {
    		TVMAST tvmFile = TVMParser.configure(dataStructConsFileName, searchPath);
    		tvmFile.generateProgram();
    		tvmFile.generateDeclarations();
    		tvmFile.compileProgram();
    	}
    	else {
    		throw new UserErrorException("An invalid engine was specified: " + engineType);
    	}			
    	if (debug) Logger.println("done"); // done loading the specification
    	
    	// Set up the empty structure.  If a TVS file was specified, use it and
    	// recompute only the instrum preds generated by AR.  A TVS file for the
    	// empty structure should be specified when some core predicates have
    	// non-zero values in the empty structure.  If no TVS file was specified,
    	// just create an empty structure and recompute all instrum preds.
    	// In both cases use topological ordering of instrum preds computed in
    	// Differencing, so that values of instrum preds used in definitions of
    	// other instrum preds get computed before those using them.
    	File emptyStructTVSFile = new File(emptyStructTVSFileName + ".tvs");
    	if (emptyStructTVSFile.exists()) {
    		if (debug) Logger.print("AbsRef: Reading TVS file for Empty Structure ... ");
    		emptyStore = TVSParser.readStructures(emptyStructTVSFileName);
    	} else {
    		TVS emptyStruct = TVSFactory.getInstance().makeEmptyTVS();
    		// This will tell us that we need to recompute all instrum preds,
    		// and not just those introduced by AR.
    		// TODO: clean out initialized predicate interface or remove.
    		//emptyStruct.initializedPredicates = new ArrayList();
    		emptyStore = Collections.singleton(emptyStruct);
    	}
    	AnalysisStatus.loadTimer.stop();
    	if (debug) Logger.println("done");
    	
    	// Now compute values of all instrum preds and save them in emptyStore.
    	// There is no precision loss in any computation on an empty structure.
    	// WRONG: structures with the free set lose precision.  Skip this step (at least for now).
    	// Assume that initially the user supplies everything for the starting vocabulary.
    	//computeValuesOfUninitInstrumPreds(emptyStore, Vocabulary.allInstrumentationPredicates());
    	
    	// Perform finite differencing for all instrum preds.
    	tvla.differencing.Differencing.differencing();
    	
    	// Save the DSC AnalysisGraph for refineAbstractInput calls.
    	dscCfg = AnalysisGraph.activeGraph;
    	
    	// At this point, the DSC AnalysisGraph and the input emptyStore are
    	// initialized for all subsequent calls to refineAbstractInput.
    	// Check if we are in the mode of constructing inputs using the DSC.
    	if (constructInitialStructures) {
    		refineAbstractInput(initial, Collections.EMPTY_SET);
    		// Clear the static load timer once load time gets reported
    		// after the first DSC analysis in the above call.
    		AnalysisStatus.loadTimer = new tvla.util.Timer();
    		
    		// Output the program to separate the DSC's structures from the program's.
    		IOFacade.instance().printProgram(AnalysisGraph.activeGraph);
    	}
    }
    
    /** Tests if there is any imprecision in the results of the last analysis.
     If so, attempts to introduce a new instrumentation predicate to refine.
     
     Imprecision is defined as having a precondition evaluate to 1/2 on any
     tuple and on any structure annotating the location of the precondition. */
    public boolean refineIfImprecise(Collection initial,
    		AbstractionRefinementException arException)
    throws Exception {
    	initialStructures = initial;
    	// Avoid printing out "Constraint Breached" graphs during refinement.
    	boolean savedCoerceDebug = Coerce.debug;
    	
    	if (!refine) {
    		if (debug)
    			Logger.println("AbsRef: Abstraction refinement disabled!");
    		return false;
    	}
    	
    	if (throwUnknownPrecondException) {
    		if (arException == null) {
    			if (debug) Logger.println("AbsRef: found no imprecision in answers!");
    			return false;
    		}
    		// Do not throw the AbstractionRefinementException inside of refinement.
    		Action.throwUnknownPrecondException = false;
    		
    		// Output message about the cause of exception,
    		// and save all attributes of the exception.
    		Logger.println(arException.getMessage());
    		exceptionAction = arException.getAction();
    		exceptionLabel = arException.getLabel();
    		exceptionLocation = exceptionAction.location();
    		exceptionStructure = arException.getStructure();
    		exceptionAssign = arException.getAssign();
    		
    		// Avoid printing out "Constraint Breached" graphs during refinement.
    		Coerce.debug = false;
    		Set nonabsPredsToAbs = HashSetFactory.make();
    		Collection newInstrumPreds =
    			introduceNewInstrumPredicates(exceptionAction, exceptionLabel, exceptionStructure,
    					nonabsPredsToAbs);
    		Coerce.debug = savedCoerceDebug;  // Restore the old value.
    		
    		if (newInstrumPreds.isEmpty() && nonabsPredsToAbs.isEmpty()) {
    			if (debug)
    				Logger.println("\nAbsRef: refinement failed for precondition of" +
    						"\n\taction \"" + exceptionAction +
    						"\" at location " + exceptionLabel);
    		} else {
    			if (debug) Logger.println("\nAbsRef: refinement succeeded!!!");
    			clearAllLocations();
    			// For now just output the program to show the start of the
    			// next refinement iteration in output.
    			IOFacade.instance().printProgram(AnalysisGraph.activeGraph);
    		}
    		
    		if (newInstrumPreds.isEmpty() && nonabsPredsToAbs.isEmpty())
    			if (debug) Logger.println("AbsRef: found imprecision in answers!");
    		
    		Action.throwUnknownPrecondException = true;  // Restore the old value.
    		return !(newInstrumPreds.isEmpty() && nonabsPredsToAbs.isEmpty());
    	}
    	
    	// Avoid printing out "Constraint Breached" graphs during refinement.
    	Coerce.debug = false;
    	
    	boolean foundImprecision = false;
    	
    	// Go through all actions (in all locations), and
    	// test their preconditions for imprecision.
    	Iterator locIter = AnalysisGraph.activeGraph.getLocations().iterator();
    	while (locIter.hasNext()) {
    		Location location = (Location) locIter.next();
    		
    		actionLoop :
    			for (int actionNum = 0; actionNum < location.getActions().size(); actionNum++) {
    				Action action = location.getAction(actionNum);
    				
    				// Get the action's precondition formula.
    				Formula precondFormula = action.getPrecondition();
    				if (precondFormula == null) continue;
    				
    				// Test it for imprecision on all structures at location.
    				Iterator structIter = location.allStructures();
    				while (structIter.hasNext()) {
    					TVS structure = (TVS) structIter.next();
    					
    					// First focus the structure!
    					Collection focusResult = null;
    					if (doFocus && action.getFocusFormulae().size() > 0)
    						focusResult = Focus.focus(structure, action.getFocusFormulae());
    					else focusResult = Collections.singleton(structure);
    					
    					for (Iterator focusIt = focusResult.iterator(); focusIt.hasNext(); ) {
    						HighLevelTVS focusedStructure = (HighLevelTVS) focusIt.next();
    						
    						// Run through coerce.
    						if (!focusedStructure.coerce()) continue;
    						
    						// If the precondition evaluates to 1/2 on focused structure,
    						// use this pair to introduce a new instrum predicate.
    						Iterator unknownIter =
    							focusedStructure.evalFormulaForValue(precondFormula, new Assign(),
    									Kleene.unknownKleene);
    						if (unknownIter.hasNext()) {
    							foundImprecision = true;
    							
    							if (debug) {
    								Logger.print("\n\nAbsRef: found imprecision in answers!\n" +
    										"\tprecondition of action \"" + action +
    										"\" at location " + location.label() +
    								"\n\tevaluates to 1/2 with assignments:\n\t");
    								while (unknownIter.hasNext()) {
    									Logger.print(unknownIter.next() + "  ");
    								}
    								Logger.println("\n");
    							}
    							
    							Set nonabsPredsToAbs = HashSetFactory.make();
    							Collection newInstrumPreds =
    								introduceNewInstrumPredicates(action, location.label(), focusedStructure,
    										nonabsPredsToAbs);
    							
    							if (newInstrumPreds.isEmpty() && nonabsPredsToAbs.isEmpty()) {
    								if (debug)
    									Logger.println("\nAbsRef: refinement failed for precondition of" +
    											"\n\taction \"" + action + "\" at location " +
    											location.label());
    								// Should we try another focused structure instead?  Alexey
    								continue actionLoop;
    							}
    							else {
    								if (debug)
    									Logger.println("\nAbsRef: refinement succeeded!!!");
    								Coerce.debug = savedCoerceDebug;  // Restore the old value.
    								clearAllLocations();
    								// For now just output the program to show the start of the
    								// next refinement iteration in output.
    								IOFacade.instance().printProgram(AnalysisGraph.activeGraph);
    								return true;
    							}
    						}
    						
    					}  // for (Iterator focusIt ...)
    				}  // while (structIter.hasNext())
    			}  // for (int actionNum ...)
    	}  // while (locIter.hasNext())
    	
    	Coerce.debug = savedCoerceDebug;  // Restore the old value.
    	
    	if (debug)
    		Logger.println("AbsRef: found " + (foundImprecision ? "" : "no ")
    				+ "imprecision in answers!");
    	return false;
    	
    }  // refineIfImprecise
    
} // public class AbstractionRefinement
