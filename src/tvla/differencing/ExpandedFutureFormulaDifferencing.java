/*
 * Created on Jun 21, 2004
 *
 */
package tvla.differencing;

import tvla.exceptions.ExceptionHandler;
import tvla.formulae.AllQuantFormula;
import tvla.formulae.ExistQuantFormula;
import tvla.formulae.Formula;
import tvla.formulae.IfFormula;
import tvla.formulae.NotFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.TransitiveFormula;
import tvla.predicates.Instrumentation;
import tvla.util.ProgramProperties;

/**
 * @author alexey
 *
 */
public class ExpandedFutureFormulaDifferencing extends FormulaDifferencing {

    // Expand even PredicateFormula's future formula (set to true to obtain the ESOP'03 version)?
    protected static boolean expandPredicateFormulaFuture =
	ProgramProperties.getBooleanProperty("tvla.differencing.expandPredicateFormulaFuture", true);

    /**
     * @param programName
     */
    public ExpandedFutureFormulaDifferencing(String programName) {
        super(programName);
    }

    /** Returns the Future formula for formula.
     *  If instrum is non-null, this is a top-level call to obtain
     *  the future formula for instrumentation predicate instrum. */
    public Formula futureFormula(String header,
                                 Instrumentation instrum,
                                 Formula formula,
                                 // Maps from Predicate to PredicateUpdateFormula
                                 PredicateUpdateMaps predUpdateMaps,
                                 boolean tight) {
	Formula futureFormula = null;
	Delta delta = null;
	PredicateFormula instrumPredFormula = null;

	if (instrum != null) {
	    // This is a top-level call to difference instrum, defined by formula.
	    // Create a predicate formula consisting of instrum predicate with its vars.
	    instrumPredFormula = new PredicateFormula(instrum, instrum.getVars());
	    if (header != null)
	      println("\n\nGenerating for " + header +
		    "  predicate: " + instrumPredFormula +
		    "\n              formula: " + formula +
		    "\n              tight: " + tight);

	    // Once non-top-level TC differencing works, simplify the call below.
	    // Top level transitive formulae are handled specially.
	    TransitiveFormula rtc = formula.getTCforRTC();
	    if (rtc != null) {
		println("\nIdentified OrFormula " + formula + "\n\t as R" + rtc);
		delta = transitiveDeltaFormulas(true, instrumPredFormula, rtc, predUpdateMaps, tight);
	    }
	    else if (formula instanceof TransitiveFormula) {
		TransitiveFormula tcFormula = (TransitiveFormula) formula;
		delta = transitiveDeltaFormulas(false, instrumPredFormula, tcFormula, predUpdateMaps, tight);
	    }
	    else {
		delta = deltaFormulas(instrumPredFormula, formula, predUpdateMaps, tight);
	    }

	    if (substituteDeltas) {
		// See if either delta matches an instrumentation predicate's
		// defining formula.  If so, use the instrumentation predicate.
		delta.plus = findMatchingInstrPred(instrumPredFormula, delta.plus);
		delta.minus = findMatchingInstrPred(instrumPredFormula, delta.minus);
	    }

	    if (tight) {
		// One of the instrumPredFormula.copy() statements is unnecessary because
		// predicateFormula (passed here) was created in futureFormula and isn't shared.
		if (formula instanceof ExistQuantFormula)
		    delta.plus = constructAndFormula(delta.plus,
						     constructNotFormula(instrumPredFormula.copy()));

		if (formula instanceof AllQuantFormula)
		    delta.minus = constructAndFormula(delta.minus, instrumPredFormula.copy());

		if (formula instanceof TransitiveFormula) {
		    delta.minus = constructAndFormula(delta.minus, instrumPredFormula.copy());
		    delta.plus = constructAndFormula(delta.plus,
						     constructNotFormula(instrumPredFormula.copy()));
		}
	    }
	} else {  // instrum == null, i.e. this is not a top-level call.

	    // If expandPredicateFormulaFuture is true, then expand the future formula,
	    // as in the ESOP'03 paper.  Otherwise, perform the expansion for all formula
	    // types with the exception of PredicateFormula.  For those, simply use the
	    // update formula returned by lookup.
	    if (!expandPredicateFormulaFuture && formula instanceof PredicateFormula) {
	        PredicateFormula predFormula = (PredicateFormula) formula;
	        futureFormula = lookupUpdateFormula(predFormula, predUpdateMaps, tight);
	        // If there's no update formula, then just use the predicate formula.
	        if (futureFormula == null) futureFormula = predFormula.copy();
	    } else {  // Perform the full expansion, as in the ESOP'03 paper.
	        delta = deltaFormulas(null, formula, predUpdateMaps, tight);
	    }
	}

	if (futureFormula == null) {
	    Formula condFormula = instrum == null ? formula : instrumPredFormula.copy();
	    futureFormula = simplifyUpdate ?
	        constructIfFormula(condFormula, constructNotFormula(delta.minus), delta.plus)
	      : new IfFormula(condFormula, new NotFormula(delta.minus), delta.plus);
	}

	if (minimizeUpdateFormulae)
	    try {
		futureFormula = minimize(futureFormula);
	    } catch (Throwable t) {
		System.err.println("Exception while attempting to minimize!");
		//System.err.println("Will return original update formula.");
		ExceptionHandler.instance().handleException(t);
		// May want to remove the exit later but should use while testing.
		System.exit(1);
	    }

	if (instrum != null) {
	    if (futureFormula.equals(instrumPredFormula))
		println("\nUnchanged predicate\t" + instrumPredFormula);
	    else
		println("\nUpdate formula for " + instrumPredFormula +
			" is\n\t" + futureFormula +
			"\n\tdelta+ = " + delta.plus +
			"\n\tdelta- = " + delta.minus +
			"\n\ttight  = " + tight);
	}

	return futureFormula;
    }

}
