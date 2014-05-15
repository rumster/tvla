/*
 * Created on Jun 21, 2004
 *
 */
package tvla.differencing;

import tvla.exceptions.ExceptionHandler;
import tvla.formulae.AllQuantFormula;
import tvla.formulae.AndFormula;
import tvla.formulae.EqualityFormula;
import tvla.formulae.EquivalenceFormula;
import tvla.formulae.ExistQuantFormula;
import tvla.formulae.Formula;
import tvla.formulae.IfFormula;
import tvla.formulae.NotFormula;
import tvla.formulae.OrFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.TransitiveFormula;
import tvla.formulae.ValueFormula;
import tvla.predicates.Instrumentation;

/**
 * @author alexey
 *
 */
public class PropagatedFutureFormulaDifferencing extends FormulaDifferencing {

    /**
     * @param programName
     */
    public PropagatedFutureFormulaDifferencing(String programName) {
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
	    Formula condFormula = instrumPredFormula.copy();
	    futureFormula = simplifyUpdate ?
		constructIfFormula(condFormula, constructNotFormula(delta.minus), delta.plus)
		: new IfFormula(condFormula, new NotFormula(delta.minus), delta.plus);

	} else {  // instrum == null, i.e. this is not a top-level call.
	    // Propagate the future formula to recompute.

	    if (formula instanceof PredicateFormula) {
		PredicateFormula predFormula = (PredicateFormula) formula;
		futureFormula = lookupUpdateFormula(predFormula, predUpdateMaps, tight);
		// If there's no update formula, then just use the predicate formula.
		if (futureFormula == null) futureFormula = predFormula.copy();
		
	    } else if (formula instanceof AndFormula) {
		AndFormula f = (AndFormula) formula;
		Formula futureLeft = futureFormula(null, null, f.left(), predUpdateMaps, tight);
		Formula futureRight = futureFormula(null, null, f.right(), predUpdateMaps, tight);
		futureFormula = constructAndFormula(futureLeft, futureRight);

	    } else if (formula instanceof OrFormula) {
		OrFormula f = (OrFormula) formula;
		Formula futureLeft = futureFormula(null, null, f.left(), predUpdateMaps, tight);
		Formula futureRight = futureFormula(null, null, f.right(), predUpdateMaps, tight);
		futureFormula = constructOrFormula(futureLeft, futureRight);

	    } else if (formula instanceof NotFormula) {
		NotFormula f = (NotFormula) formula;
		Formula futureSub = futureFormula(null, null, f.subFormula(), predUpdateMaps, tight);
		futureFormula = constructNotFormula(futureSub);

	    } else if (formula instanceof ExistQuantFormula) {
		ExistQuantFormula f = (ExistQuantFormula) formula;
		Formula futureSub = futureFormula(null, null, f.subFormula(), predUpdateMaps, tight);
		futureFormula = constructExistFormula(f.boundVariable(), futureSub);

	    } else if (formula instanceof AllQuantFormula) {
		AllQuantFormula f = (AllQuantFormula) formula;
		Formula futureSub = futureFormula(null, null, f.subFormula(), predUpdateMaps, tight);
		futureFormula = constructAllFormula(f.boundVariable(), futureSub);

	    } else if (formula instanceof TransitiveFormula) {
		TransitiveFormula f = (TransitiveFormula) formula;
		Formula futureSub = futureFormula(null, null, f.subFormula(), predUpdateMaps, tight);
		futureFormula = constructTransitiveFormula(f.left(), f.right(),
							   f.subLeft(), f.subRight(), futureSub);

	    } else if (formula instanceof IfFormula) {
		IfFormula f = (IfFormula) formula;
		Formula futureCond = futureFormula(null, null, f.condSubFormula(), predUpdateMaps, tight);
		Formula futureTrue = futureFormula(null, null, f.trueSubFormula(), predUpdateMaps, tight);
		Formula futureFalse = futureFormula(null, null, f.falseSubFormula(), predUpdateMaps, tight);
		futureFormula = constructIfFormula(futureCond, futureTrue, futureFalse);

	    } else if (formula instanceof EquivalenceFormula) {
		EquivalenceFormula f = (EquivalenceFormula) formula;
		Formula futureLeft = futureFormula(null, null, f.left(), predUpdateMaps, tight);
		Formula futureRight = futureFormula(null, null, f.right(), predUpdateMaps, tight);
		futureFormula = constructEquivalenceFormula(futureLeft, futureRight);

	    } else if (formula instanceof ValueFormula ||
		       formula instanceof EqualityFormula) {
		futureFormula = formula;
	    }
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
