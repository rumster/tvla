package tvla.differencing;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tvla.exceptions.SemanticErrorException;
import tvla.exceptions.UserErrorException;
import tvla.formulae.AllQuantFormula;
import tvla.formulae.AndFormula;
import tvla.formulae.AtomicFormula;
import tvla.formulae.EqualityFormula;
import tvla.formulae.EquivalenceFormula;
import tvla.formulae.ExistQuantFormula;
import tvla.formulae.Formula;
import tvla.formulae.FormulaParser;
import tvla.formulae.FormulaTraverser;
import tvla.formulae.IfFormula;
import tvla.formulae.NotFormula;
import tvla.formulae.OrFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.PredicateUpdateFormula;
import tvla.formulae.TransitiveFormula;
import tvla.formulae.ValueFormula;
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.predicates.Instrumentation;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Logger;
import tvla.util.Pair;
import tvla.util.ProgramProperties;

/**
 * Generating predicate-update formula for instrumentation predicates.
 * 
 * @author Tal Lev-Ami, Tom Reps, Mooly Sagiv, and Alexey Loginov
 */

public abstract class FormulaDifferencing {
    // Note that the properties below are initialized before any getInstance
    // call
    // but after properties are loaded in Runner's main method.

    protected static boolean simplify = // Use simplifying constructors?
    ProgramProperties.getBooleanProperty("tvla.differencing.simplify", false);

    protected static boolean simplifyUpdate = // Simplify top-level update
                                                // formulae?
    ProgramProperties.getBooleanProperty("tvla.differencing.simplifyUpdate", false);

    protected static boolean substituteDeltas = // Look for instrum preds
                                                // matching deltas?
    ProgramProperties.getBooleanProperty("tvla.differencing.substituteDeltas", false);

    protected static boolean minimizeUpdateFormulae = // Minimize formulae to
                                                        // be returned from
                                                        // updateFormula?
    ProgramProperties.getBooleanProperty("tvla.differencing.minimizeUpdateFormulae", false);

    protected static String minTmpFile = // Tmp file for minimization output
                                            // parsing.
    ProgramProperties.getProperty("tvla.differencing.minTmpFile", null);

    protected static String searchPath = // Search path to be passed to
                                            // formula parser.
    ProgramProperties.getProperty("tvla.searchPath", ";");

    protected static boolean logging = // Log main steps of differencing in a
                                        // file?
    ProgramProperties.getBooleanProperty("tvla.differencing.logging", false)
            && ProgramProperties.getBooleanProperty("tvla.differencing.difference", false);

    // Should we assume that all structures are non-empty in simplification?
    protected static boolean nonEmptyStructs = ProgramProperties.getBooleanProperty(
            "tvla.simplify.nonEmptyStructs", false);

    protected static boolean warnAboutNonUnitChanges = ProgramProperties.getBooleanProperty(
            "tvla.differencing.warnAboutNonUnitChanges", true);

    protected static PrintStream diffLogStream;

    /**
     * The one and only instance of this class.
     */
    protected static FormulaDifferencing instance;

    public static void reset() {
    	instance = null;
    }
    
    /**
     * Returns the one and only instance of this class.
     */
    public static FormulaDifferencing getInstance() {
        if (instance == null) {
            String implementation = ProgramProperties.getProperty(
                    "tvla.differencing.implementation", "expandedFuture");
            if (implementation.toLowerCase().startsWith("expandedfuture"))
                instance = new ExpandedFutureFormulaDifferencing(ProgramProperties.getProperty(
                        "tvla.programName", "defaultProgramName"));
            else if (implementation.toLowerCase().startsWith("propagatedfuture"))
                instance = new PropagatedFutureFormulaDifferencing(ProgramProperties.getProperty(
                        "tvla.programName", "defaultProgramName"));
            else
                throw new UserErrorException(
                        "Invalid property value specified for tvla.differencing.implementation : "
                                + implementation
                                + "valid property values are: expandedFuture and propagatedFuture\n");
        }
        return instance;
    }

    protected FormulaDifferencing(String programName) {
        if (logging) {
            String diffLogFile = programName == null ? "diff.log" : programName + ".diff.log";
            try {
                diffLogStream = new PrintStream(new FileOutputStream(diffLogFile), true);
            } catch (FileNotFoundException e) {
                System.err.print("File not found: " + e.getMessage());
                Logger.println(e.getMessage());
                if (ProgramProperties.getBooleanProperty("tvla.printExceptionStackTrace", false))
                    e.printStackTrace();
            }
        }
    }

    protected static void print(Object o) {
        if (logging && diffLogStream != null)
            diffLogStream.print(o);
    }

    protected static void println(Object o) {
        if (logging && diffLogStream != null)
            diffLogStream.println(o);
    }

    public static class Delta {
        public Formula plus;

        public Formula minus;

        public Delta(Formula plus, Formula minus) {
            this.plus = plus;
            this.minus = minus;
        }
    }

    protected Formula falseFormula = new ValueFormula(Kleene.falseKleene);

    protected Formula unknownFormula = new ValueFormula(Kleene.unknownKleene);

    protected Formula trueFormula = new ValueFormula(Kleene.trueKleene);

    protected Formula lookupUpdateFormula(PredicateFormula predFormula,
            PredicateUpdateMaps predUpdateMaps, boolean tight) {
        Predicate pred = predFormula.predicate();

        // First, check for generated update (if found, it overrides the
        // supplied one).
        PredicateUpdateFormula predicateUpdate = tight ? (PredicateUpdateFormula) predUpdateMaps.generatedTight
                .get(pred)
                : (PredicateUpdateFormula) predUpdateMaps.generatedUntight.get(pred);

        if (predicateUpdate == null)
            predicateUpdate = (PredicateUpdateFormula) predUpdateMaps.supplied.get(pred);

        Formula updateFormula = null;
        if (predicateUpdate != null) {
            updateFormula = predicateUpdate.getFormula().copy();

            if (pred.arity() > 0)
                updateFormula
                        .safeSubstituteVars(predicateUpdate.variables, predFormula.variables());
        }
        return updateFormula;
    }

    protected Delta lookupDeltaFormulas(PredicateFormula lhsPredicateFormula,
            PredicateUpdateMaps predUpdateMaps, boolean tight) {
        Formula updateFormula = lookupUpdateFormula(lhsPredicateFormula, predUpdateMaps, tight);
        return getPredicateDelta(lhsPredicateFormula, updateFormula, tight);
    }

    public Delta getPredicateDelta(PredicateFormula lhsPredicateFormula, Formula updateFormula, boolean tight) {
        // If there's no update formula stored, or it is the identity update,
        // deltas are 0.
        if (updateFormula == null || updateFormula.equals(lhsPredicateFormula))
            return new Delta(falseFormula, falseFormula);

        Delta delta = null;
        if (updateFormula instanceof IfFormula) {
            IfFormula ifFormula = (IfFormula) updateFormula;
            if (ifFormula.condSubFormula() instanceof PredicateFormula) {
                PredicateFormula predicateFormula = (PredicateFormula) ifFormula.condSubFormula();
                if (predicateFormula.equals(lhsPredicateFormula)) {
                    delta = new Delta(ifFormula.falseSubFormula(), constructNotFormula(ifFormula
                            .trueSubFormula()));
                }
            }
            if (ifFormula.falseSubFormula() instanceof PredicateFormula) {
                PredicateFormula predicateFormula = (PredicateFormula) ifFormula.falseSubFormula();
                if (predicateFormula.equals(lhsPredicateFormula)) {
                    delta = new Delta(constructAndFormula(ifFormula.condSubFormula(), ifFormula
                            .trueSubFormula()), constructAndFormula(ifFormula.condSubFormula(),
                            constructNotFormula(ifFormula.trueSubFormula())));
                }
            }
        } else if (updateFormula instanceof OrFormula) {
            OrFormula orFormula = (OrFormula) updateFormula;
            Formula left = orFormula.left();
            Formula right = orFormula.right();
            if (left instanceof PredicateFormula) {
                PredicateFormula predicateFormula = (PredicateFormula) left;
                if (predicateFormula.equals(lhsPredicateFormula)) {
                    delta = new Delta(right, falseFormula);
                }
            } else if (right instanceof PredicateFormula) {
                PredicateFormula predicateFormula = (PredicateFormula) right;
                if (predicateFormula.equals(lhsPredicateFormula)) {
                    delta = new Delta(left, falseFormula);
                }
            }
        } else if (updateFormula instanceof AndFormula) {
            AndFormula andFormula = (AndFormula) updateFormula;
            Formula left = andFormula.left();
            Formula right = andFormula.right();
            if (left instanceof PredicateFormula) {
                PredicateFormula predicateFormula = (PredicateFormula) left;
                if (predicateFormula.equals(lhsPredicateFormula)) {
                    delta = new Delta(falseFormula, constructNotFormula(right));
                }
            } else if (right instanceof PredicateFormula) {
                PredicateFormula predicateFormula = (PredicateFormula) right;
                if (predicateFormula.equals(lhsPredicateFormula)) {
                    delta = new Delta(falseFormula, constructNotFormula(left));
                }
            }
        }

        // None of the above patterns were matched.
        if (delta == null)
            delta = new Delta(updateFormula, constructNotFormula(updateFormula));

        if (tight) {
            // delta+ := delta+ & !p; delta- := delta- & p
            delta.plus = constructAndFormula(delta.plus, constructNotFormula(lhsPredicateFormula
                    .copy()));
            delta.minus = constructAndFormula(delta.minus, lhsPredicateFormula.copy());
        }

        println("\nParsed update formula for predicate " + lhsPredicateFormula + "\n\tupdate = "
                + updateFormula + "\n\tdelta+ = " + delta.plus + "\n\tdelta- = " + delta.minus
                + "\n\ttight  = " + tight + "\n\tfreeVars = " + updateFormula.freeVars());

        return delta;
    }

    /* Constructors for simplified formulae */
    public Formula constructNotFormula(Formula f) {
        if (simplify) {
            if (f.equals(falseFormula))
                return trueFormula;
            if (f.equals(unknownFormula))
                return unknownFormula;
            if (f.equals(trueFormula))
                return falseFormula;
            if (f instanceof NotFormula) {
                NotFormula temp = (NotFormula) f;
                return temp.subFormula();
            }
            if (f instanceof IfFormula) {
                IfFormula temp = (IfFormula) f;
                return constructIfFormula(temp.condSubFormula(), constructNotFormula(temp
                        .trueSubFormula()), constructNotFormula(temp.falseSubFormula()));
            }
            if (f instanceof ExistQuantFormula) {
                ExistQuantFormula temp = (ExistQuantFormula) f;
                return constructAllFormula(temp.boundVariable(), constructNotFormula(temp
                        .subFormula()));
            }
            if (f instanceof AllQuantFormula) {
                AllQuantFormula temp = (AllQuantFormula) f;
                return constructExistFormula(temp.boundVariable(), constructNotFormula(temp
                        .subFormula()));
            }
            if (f instanceof AndFormula) {
                AndFormula temp = (AndFormula) f;
                return constructOrFormula(constructNotFormula(temp.left()),
                        constructNotFormula(temp.right()));
            }
            if (f instanceof OrFormula) {
                OrFormula temp = (OrFormula) f;
                return constructAndFormula(constructNotFormula(temp.left()),
                        constructNotFormula(temp.right()));
            }
        }
        return new NotFormula(f);
    }

    public Formula constructAndFormula(Formula f, Formula g) {
        if (simplify) {
            if (f.equals(falseFormula))
                return falseFormula;
            if (f.equals(trueFormula))
                return g;

            if (g.equals(falseFormula))
                return falseFormula;
            if (g.equals(trueFormula))
                return f;

            if (f.equals(g))
                return f;

            if (f instanceof NotFormula) {
                NotFormula temp = (NotFormula) f;
                if (g.equals(temp.subFormula()))
                    return falseFormula;
            }
            if (g instanceof NotFormula) {
                NotFormula temp = (NotFormula) g;
                if (f.equals(temp.subFormula()))
                    return falseFormula;
            }
        }

        return new AndFormula(f, g);
    }

    public Formula constructOrFormula(Formula f, Formula g) {
        if (simplify) {
            if (f.equals(falseFormula))
                return g;
            if (f.equals(trueFormula))
                return trueFormula;

            if (g.equals(falseFormula))
                return f;
            if (g.equals(trueFormula))
                return trueFormula;

            if (f.equals(g))
                return f;

            if (f instanceof NotFormula) {
                NotFormula temp = (NotFormula) f;
                if (g.equals(temp.subFormula()))
                    return trueFormula;
            }
            if (g instanceof NotFormula) {
                NotFormula temp = (NotFormula) g;
                if (f.equals(temp.subFormula()))
                    return trueFormula;
            }
        }
        return new OrFormula(f, g);
    }

    public Formula constructIfFormula(Formula f, Formula g, Formula h) {
        if (simplify) {
            if (f.equals(trueFormula))
                return g;
            if (f.equals(falseFormula))
                return h;

            if (g.equals(trueFormula))
                return constructOrFormula(f, h);
            if (g.equals(falseFormula))
                return constructAndFormula(constructNotFormula(f), h);

            if (h.equals(trueFormula))
                return constructOrFormula(constructNotFormula(f), g);
            if (h.equals(falseFormula))
                return constructAndFormula(f, g);

            if (g.equals(h))
                return g;

            if (f instanceof NotFormula) {
                NotFormula temp = (NotFormula) f;
                return new IfFormula(temp.subFormula(), h, g);
            }
        }

        return new IfFormula(f, g, h);
    }

    public Formula constructEquivalenceFormula(Formula f, Formula g) {
        if (simplify) {
            if (f.equals(trueFormula))
                return g;
            if (f.equals(falseFormula))
                return constructNotFormula(g);

            if (g.equals(trueFormula))
                return f;
            if (g.equals(falseFormula))
                return constructNotFormula(f);

            if (f.equals(g))
                return trueFormula;

            if (f instanceof NotFormula) {
                NotFormula temp = (NotFormula) f;
                if (g.equals(temp.subFormula()))
                    return falseFormula;
            }
            if (g instanceof NotFormula) {
                NotFormula temp = (NotFormula) g;
                if (f.equals(temp.subFormula()))
                    return falseFormula;
            }
        }

        return new EquivalenceFormula(f, g);
    }

    public Formula constructExistFormula(Var v, Formula f) {
        if (simplify) {
            if (f.equals(falseFormula))
                return falseFormula;
            // If structures can be empty, cannot simplify if f is true or
            // unknown, since Exists over an empty structure is false.
            if (nonEmptyStructs && f instanceof ValueFormula)
                return f;
        }
        return new ExistQuantFormula(v, f);
    }

    public Formula constructAllFormula(Var v, Formula f) {
        if (simplify) {
            if (f.equals(trueFormula))
                return trueFormula;
            // If structures can be empty, cannot simplify if f is false or
            // unknown, since Forall over an empty structure is true.
            if (nonEmptyStructs && f instanceof ValueFormula)
                return f;
        }
        return new AllQuantFormula(v, f);
    }

    public Formula constructTransitiveFormula(Var left, Var right, Var subLeft, Var subRight,
            Formula f) {
        if (simplify) {
            if (f.equals(falseFormula))
                return falseFormula;
            // If structures can be empty, cannot simplify if f is true or
            // unknown, since TC over an empty structure is false.
            if (nonEmptyStructs && f instanceof ValueFormula)
                return f;
        }
        return new TransitiveFormula(left, right, subLeft, subRight, f);
    }

    public Formula constructReflexiveTransitiveFormula(Var left, Var right, Var subLeft,
            Var subRight, Formula f) {
        if (simplify) {
            if (f.equals(falseFormula))
                return constructReflexiveFormula(left, right, falseFormula);
            // If structures can be empty, cannot simplify if f is true or
            // unknown, since TC over an empty structure is false.
            if (nonEmptyStructs && f instanceof ValueFormula)
                return constructReflexiveFormula(left, right, f);
        }
        Formula tcFormula = new TransitiveFormula(left, right, subLeft, subRight, f);
        return constructReflexiveFormula(left, right, tcFormula);
    }

    public Formula constructReflexiveFormula(Var left, Var right, Formula f) {
        Formula eqFormula = new EqualityFormula(left, right);
        return constructOrFormula(eqFormula, f);
    }

    // Returns the simplified formula.
    public Formula simplify(Formula inputFormula) {
        // Save (and later restore) flag simplify. Set to true for now.
        boolean savedSimplify = simplify;
        simplify = true;

        Formula simplifiedFormula = null;

        if (inputFormula instanceof AndFormula) {
            AndFormula formula = (AndFormula) inputFormula;
            simplifiedFormula = constructAndFormula(simplify(formula.left()), simplify(formula
                    .right()));

        } else if (inputFormula instanceof OrFormula) {
            OrFormula formula = (OrFormula) inputFormula;
            simplifiedFormula = constructOrFormula(simplify(formula.left()), simplify(formula
                    .right()));

        } else if (inputFormula instanceof NotFormula) {
            NotFormula formula = (NotFormula) inputFormula;
            simplifiedFormula = constructNotFormula(simplify(formula.subFormula()));

        } else if (inputFormula instanceof ExistQuantFormula) {
            ExistQuantFormula formula = (ExistQuantFormula) inputFormula;
            simplifiedFormula = constructExistFormula(formula.boundVariable(), simplify(formula
                    .subFormula()));

        } else if (inputFormula instanceof AllQuantFormula) {
            AllQuantFormula formula = (AllQuantFormula) inputFormula;
            simplifiedFormula = constructAllFormula(formula.boundVariable(), simplify(formula
                    .subFormula()));

        } else if (inputFormula instanceof TransitiveFormula) {
            TransitiveFormula formula = (TransitiveFormula) inputFormula;
            simplifiedFormula = constructTransitiveFormula(formula.left(), formula.right(), formula
                    .subLeft(), formula.subRight(), simplify(formula.subFormula()));

        } else if (inputFormula instanceof IfFormula) {
            IfFormula formula = (IfFormula) inputFormula;
            simplifiedFormula = constructIfFormula(simplify(formula.condSubFormula()),
                    simplify(formula.trueSubFormula()), simplify(formula.falseSubFormula()));

        } else if (inputFormula instanceof EquivalenceFormula) {
            EquivalenceFormula formula = (EquivalenceFormula) inputFormula;
            simplifiedFormula = constructEquivalenceFormula(simplify(formula.left()),
                    simplify(formula.right()));

        } else if (inputFormula instanceof AtomicFormula)
            simplifiedFormula = inputFormula;

        simplify = savedSimplify; // Restore simplify flag.
        return simplifiedFormula;
    }

    // Performs semantic minimization using the external minimize command.
    protected Formula minimize(Formula formula) throws InterruptedException, IOException, Exception {
        // Get the Runtime object.
        Runtime rt = Runtime.getRuntime();

        // Execute the command.
        Process minProc = rt.exec("minimize -");

        // Pass input to process.
        OutputStream procIn = minProc.getOutputStream();
        PrintWriter pw = new PrintWriter(procIn);
        pw.println(formula);
        pw.close();

        // Wait for process to complete.
        minProc.waitFor();

        // Get the output from the process (can do same with error stream).
        InputStream procOut = minProc.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(procOut));
        println("\n\nMinimization process input:\n\t" + formula);
        String minOut = br.readLine();
        println("\nMinimization process output:\n\t" + minOut);

        // Can we avoid this by passing System.in to CPreProcessorStream
        // somehow?
        PrintWriter pwFile = new PrintWriter(new FileOutputStream(minTmpFile));
        pwFile.println(minOut);
        pwFile.close();

        // Now parse the file (passing filename and searchpath).
        Pair minimizedFormulaPair = FormulaParser.configure(minTmpFile, searchPath);

        return ((tvla.language.TVP.FormulaAST) minimizedFormulaPair.first).getFormula();
    }

    /**
     * Returns the Future formula for formula. If instrum is non-null, this is a
     * top-level call to obtain the future formula for instrumentation predicate
     * instrum.
     */
    public abstract Formula futureFormula(String header, Instrumentation instrum, Formula formula,
    // Maps from Predicate to PredicateUpdateFormula
            PredicateUpdateMaps predUpdateMaps, boolean tight);

    // Search for an instrumentation predicate whose defining formula matches
    // the input formula (for now we only check for exact matches). If there is
    // a match, return the application of the matched instrumentation predicate
    // to the appropriate variables. The input predicateFormula is just for
    // identifying the free vars, their number, and their order.
    protected Formula findMatchingInstrPred(PredicateFormula predicateFormula, Formula formula) {
        if (formula instanceof AtomicFormula)
            return formula;

        Predicate predicate = predicateFormula.predicate();
        int arity = predicate.arity();
        int numFreeVars = formula.freeVars().size();

        // Go through all instrumentation predicates.
        for (Iterator<Instrumentation> iterator = Vocabulary.allInstrumentationPredicates().iterator(); iterator
                .hasNext();) {
            Instrumentation instrum = (Instrumentation) iterator.next();
            Formula testFormula = instrum.getFormula();

            if (numFreeVars != testFormula.freeVars().size())
                continue;

            // Create a predicate formula consisting of instrum predicate with
            // its vars.
            PredicateFormula testPredicateFormula = new PredicateFormula(instrum, instrum.getVars());

            // First try comparing the formula as is (with no var
            // substitutions).
            if (formula.equals(testFormula)) {
                println("\nMatched defining formula for " + testPredicateFormula
                        + " without var substitutions:\n\t" + testFormula);
                return testPredicateFormula;
            }

            // Try substituting free vars in testFormula to match our formula.
            // Note, equality in subclasses of Formula should check for equality
            // up to alpha renaming. This is done by using property setting
            // tvla.formulae.alphaRenamingEquals = true.

            // Now also check that the arity of our predicate and that of the
            // test
            // predicate are the same. This is a stronger condition than the
            // free
            // var test above. If the arity is the same, there is a natural set
            // of variable substitutions to try.
            if (arity == 0 || arity != instrum.arity())
                continue;

            // Substitute the variables used in the test predicate formula
            // with those used in our predicate.
            Formula testFormulaCopy = testFormula.copy();
            Var[] from = testPredicateFormula.variables();
            Var[] to = predicateFormula.variables();
            testFormulaCopy.safeSubstituteVars(from, to);

            if (formula.equals(testFormulaCopy)) {
                println("\nMatched defining formula for " + testPredicateFormula
                        + " with variable substitutions:" + "\n\t" + from + " -> " + to
                        + "\n\tformula: " + testFormula);
                formula = new PredicateFormula(instrum, predicateFormula.variables());
                return formula;
            }
        }
        return formula; // No match: return unmodified formula.
    }

    // Computes delta+ and delta- formulas for formula.
    // instrumPredFormula is non-null if this is the top-level call,
    // i.e. formula defines the predicate of instrumPredFormula.
    protected Delta deltaFormulas(PredicateFormula instrumPredFormula, Formula formula,
            PredicateUpdateMaps predUpdateMaps, boolean tight) {
        Delta delta = null;

        if (formula instanceof PredicateFormula) {
            PredicateFormula predFormula = (PredicateFormula) formula;
            delta = lookupDeltaFormulas(predFormula, predUpdateMaps, tight);

        } else if (formula instanceof AndFormula) {
            AndFormula andFormula = (AndFormula) formula;
            Formula left = andFormula.left();
            Formula right = andFormula.right();
            Delta leftDelta = deltaFormulas(null, left, predUpdateMaps, tight);
            Delta rightDelta = deltaFormulas(null, right, predUpdateMaps, tight);
            Formula minus = (tight ? constructOrFormula(
                    constructAndFormula(leftDelta.minus, right), constructAndFormula(left,
                            rightDelta.minus)) : constructOrFormula(leftDelta.minus,
                    rightDelta.minus));
            Formula leftFuture = futureFormula(null, null, left, predUpdateMaps, tight);
            Formula rightFuture = futureFormula(null, null, right, predUpdateMaps, tight);
            Formula plus = constructOrFormula(constructAndFormula(leftDelta.plus, rightFuture),
                    constructAndFormula(leftFuture, rightDelta.plus));
            delta = new Delta(plus, minus);

        } else if (formula instanceof OrFormula) {
            OrFormula orFormula = (OrFormula) formula;

            TransitiveFormula rtc = orFormula.getTCforRTC();
            if (rtc != null) {
                println("\nIdentified OrFormula " + orFormula + "\n\t as R" + rtc);
                delta = transitiveDeltaFormulas(true, null, rtc, predUpdateMaps, tight);

                if (tight) {
                    // If this is top-level, futureFormula will AND on p/!p
                    // instead.
                    if (instrumPredFormula == null) {
                        delta.minus = constructAndFormula(delta.minus, orFormula.copy());
                        delta.plus = constructAndFormula(delta.plus, constructNotFormula(orFormula
                                .copy()));
                    }
                }
            } else { // Normal OrFormula

                Formula left = orFormula.left();
                Formula right = orFormula.right();
                Delta leftDelta = deltaFormulas(null, left, predUpdateMaps, tight);
                Delta rightDelta = deltaFormulas(null, right, predUpdateMaps, tight);
                Formula leftFuture = futureFormula(null, null, left, predUpdateMaps, tight);
                Formula rightFuture = futureFormula(null, null, right, predUpdateMaps, tight);
                Formula minus = constructOrFormula(constructAndFormula(leftDelta.minus,
                        constructNotFormula(rightFuture)), constructAndFormula(
                        constructNotFormula(leftFuture), rightDelta.minus));
                Formula plus = (tight ? constructOrFormula(constructAndFormula(leftDelta.plus,
                        constructNotFormula(right)), constructAndFormula(constructNotFormula(left),
                        rightDelta.plus)) : constructOrFormula(leftDelta.plus, rightDelta.plus));
                delta = new Delta(plus, minus);
            }

        } else if (formula instanceof NotFormula) {
            NotFormula notFormula = (NotFormula) formula;
            delta = deltaFormulas(null, notFormula.subFormula(), predUpdateMaps, tight);
            // swap Delta^+ and Delta^-
            Formula tmp = delta.plus;
            delta.plus = delta.minus;
            delta.minus = tmp;

        } else if (formula instanceof ExistQuantFormula) {
            ExistQuantFormula existFormula = (ExistQuantFormula) formula;
            Delta subDelta = deltaFormulas(null, existFormula.subFormula(), predUpdateMaps, tight);

            Formula plus = constructExistFormula(existFormula.boundVariable(), subDelta.plus);
            Formula subFuture = futureFormula(null, null, existFormula.subFormula(),
                    predUpdateMaps, tight);
            Formula minus = constructAndFormula(constructExistFormula(existFormula.boundVariable(),
                    subDelta.minus), constructNotFormula(constructExistFormula(existFormula
                    .boundVariable(), subFuture)));
            if (tight)
                // If this is top-level, futureFormula will AND on !p instead.
                if (instrumPredFormula == null)
                    // delta+ := delta+ & !(E v : phi1)
                    plus = constructAndFormula(plus, constructNotFormula(existFormula.copy()));

            delta = new Delta(plus, minus);

        } else if (formula instanceof AllQuantFormula) {
            AllQuantFormula allFormula = (AllQuantFormula) formula;
            Delta subDelta = deltaFormulas(null, allFormula.subFormula(), predUpdateMaps, tight);

            Formula minus = constructExistFormula(allFormula.boundVariable(), subDelta.minus);
            Formula subFuture = futureFormula(null, null, allFormula.subFormula(), predUpdateMaps,
                    tight);
            Formula plus = constructAndFormula(constructExistFormula(allFormula.boundVariable(),
                    subDelta.plus), constructAllFormula(allFormula.boundVariable(), subFuture));
            if (tight)
                // If this is top-level, futureFormula will AND on p instead.
                if (instrumPredFormula == null)
                    // delta- := delta- & (A v : phi1)
                    minus = constructAndFormula(minus, allFormula.copy());

            delta = new Delta(plus, minus);

        } else if (formula instanceof TransitiveFormula) {
            TransitiveFormula tcFormula = (TransitiveFormula) formula;
            delta = transitiveDeltaFormulas(false, null, tcFormula, predUpdateMaps, tight);

            if (tight) {
                // If this is top-level, futureFormula will AND on p/!p instead.
                if (instrumPredFormula == null) {
                    delta.minus = constructAndFormula(delta.minus, tcFormula.copy());
                    delta.plus = constructAndFormula(delta.plus, constructNotFormula(tcFormula
                            .copy()));
                }
            }

        } else if (formula instanceof ValueFormula) {
            //ValueFormula valueFormula = (ValueFormula) formula;
            delta = new Delta(falseFormula, falseFormula);

        } else if (formula instanceof EqualityFormula) {
            //EqualityFormula equalFormula = (EqualityFormula) formula;
            delta = new Delta(falseFormula, falseFormula);

        } else if (formula instanceof EquivalenceFormula) {
            EquivalenceFormula equivFormula = (EquivalenceFormula) formula;
            // Just translate (f1 <-> f2) into ((!f1 | f2) & (!f2 | f1)).
            Formula andFormula = constructAndFormula(constructOrFormula(
                    constructNotFormula(equivFormula.left()), equivFormula.right()),
                    constructOrFormula(constructNotFormula(equivFormula.right()), equivFormula
                            .left()));
            delta = deltaFormulas(null, andFormula, predUpdateMaps, tight);

        } else if (formula instanceof IfFormula) {
            IfFormula ifFormula = (IfFormula) formula;
            // Just translate (f1 ? f2 : f3) into (f1 & f2 | !f1 & f3).
            Formula orFormula = constructOrFormula(constructAndFormula(ifFormula.condSubFormula(),
                    ifFormula.trueSubFormula()), constructAndFormula(constructNotFormula(ifFormula
                    .condSubFormula()), ifFormula.falseSubFormula()));
            delta = deltaFormulas(null, orFormula, predUpdateMaps, tight);
        }
        return delta;
    }

    protected Set<Var> uniqueVars(Predicate deltaPred, Formula f, Set<Var> uvIn) {
        Set<Var> uvOut;

        // Simplify the formula first (this may result in more unique vars).
        Formula simpForm = simplify(f);

        if (simpForm instanceof ValueFormula) {
            uvOut = uvIn;

        } else if (simpForm instanceof EqualityFormula) {
            EqualityFormula formula = (EqualityFormula) simpForm;
            Var left = formula.left();
            Var right = formula.right();

            if (uvIn.contains(left)) {
                uvOut = HashSetFactory.make(uvIn);
                uvOut.add(right);
            } else if (uvIn.contains(right)) {
                uvOut = HashSetFactory.make(uvIn);
                uvOut.add(left);
            } else
                uvOut = uvIn;

        } else if (simpForm instanceof PredicateFormula) {
            PredicateFormula formula = (PredicateFormula) simpForm;

            uvOut = uvIn;

            // No information from nullary predicates.
            if (formula.predicate().arity() == 1) {
                PredicateFormula upf = (PredicateFormula) formula;

                if (upf.predicate().unique()) {
                    uvOut = HashSetFactory.make(uvIn);
                    uvOut.add(upf.getVariable(0));
                }

                else {
                    Predicate ccPred = upf.predicate().uniquePerCCofPred();
                    if (ccPred != null) {
                        // We should call these sfe but there may still be test
                        // files with ste.
                        Predicate sfe = Vocabulary.getPredicateByName("sfe[" + ccPred.name() + "]");
                        Predicate ste = Vocabulary.getPredicateByName("ste[" + ccPred.name() + "]");
                        if (sfe != null && deltaPred.equals(sfe) || ste != null
                                && deltaPred.equals(ste)) {

                            // For the purposes of testing deltas of
                            // sfe[ccPred]/ste[ccPred]
                            // (spanning forest/tree edge) for unit size change,
                            // the upf's
                            // predicate's uniqueness per connected component of
                            // ccPred is
                            // as good as uniqueness.
                            uvOut = HashSetFactory.make(uvIn);
                            uvOut.add(upf.getVariable(0));
                        }
                    }
                }

            } else if (formula.predicate().arity() == 2) {
                PredicateFormula bpf = (PredicateFormula) formula;
                if (bpf.predicate().function() && uvIn.contains(bpf.getVariable(0))) {
                    uvOut = HashSetFactory.make(uvIn);
                    uvOut.add(bpf.getVariable(1));
                } else if (bpf.predicate().invfunction() && uvIn.contains(bpf.getVariable(1))) {
                    uvOut = HashSetFactory.make(uvIn);
                    uvOut.add(bpf.getVariable(0));
                }
            }
            // Currently can't get any information from preds of arity > 2.

        } else if (simpForm instanceof NotFormula) {
            // If formula is of one of the patterns below, we'd like to apply
            // uniqueVars
            // to the right side:
            // !f1 => uv(f1,uvIn); f2|f3 => uv(!f2&!f3,uvIn); f2&f3 =>
            // uv(!f2|!f3,uvIn)
            // Instead of doing this here, we simplify the formula at the start
            // of this
            // method to get all of these simplifications done. The negation
            // must apply
            // to a literal and we can't extract any info out of that.
            uvOut = uvIn;

        } else if (simpForm instanceof AndFormula) {
            AndFormula formula = (AndFormula) simpForm;
            Formula left = formula.left();
            Formula right = formula.right();

            // Iterate alternated uniqueVars applications to a fixed point.
            uvOut = uvIn;
            Set<Var> uvLeftOut = uniqueVars(deltaPred, left, uvOut);
            Set<Var> uvRightOut = uniqueVars(deltaPred, right, uvLeftOut);
            while (uvRightOut.size() > uvOut.size()) {
                uvOut = uvRightOut;
                uvLeftOut = uniqueVars(deltaPred, left, uvOut);
                uvRightOut = uniqueVars(deltaPred, right, uvLeftOut);
            }

        } else if (simpForm instanceof OrFormula) {
            OrFormula formula = (OrFormula) simpForm;
            Formula left = formula.left();
            Formula right = formula.right();

            Set<Var> uvLeftOut = uniqueVars(deltaPred, left, uvIn);
            Set<Var> uvRightOut = uniqueVars(deltaPred, right, uvIn);

            // Avoid unnecessary set copying.
            if (!uvLeftOut.equals(uvRightOut)) {
                uvOut = HashSetFactory.make(uvLeftOut);
                uvOut.retainAll(uvRightOut);
            } else
                uvOut = uvLeftOut;

        } else if (simpForm instanceof ExistQuantFormula) {
            // Should also test for a pattern simulating ExistsUnique.
            ExistQuantFormula formula = (ExistQuantFormula) simpForm;
            Var var = formula.boundVariable();

            // (uniqueVars(subFormula, uvIn \ {var}) \ {var}) UNION uvIn
            // The latter union is to add var back in, if it's in uvIn.
            uvOut = HashSetFactory.make(uvIn);
            uvOut.remove(var);
            uvOut = uniqueVars(deltaPred, formula.subFormula(), uvOut);

            // Remove var (in case it was added) unless it was in uvIn.
            if (!uvIn.contains(var))
                uvOut.remove(var);

        } else if (simpForm instanceof AllQuantFormula) {
            // Should make sure all literals not using the bound variable
            // are outside the quantifier. O.w. we may lose some unique vars.
            // After that's done, no more information can be obtained.
            // Cases such as (A v : x(v)) imply having 0 or 1 individuals.
            // Any other cases with uniqueness information?
            uvOut = uvIn;

        } else if (simpForm instanceof TransitiveFormula) {
            // No information can be obtained here.
            uvOut = uvIn;

        } else if (simpForm instanceof EquivalenceFormula) {
            EquivalenceFormula formula = (EquivalenceFormula) simpForm;
            // Just translate (f1 <-> f2) into ((!f1 | f2) & (!f2 | f1)).
            Formula andFormula = constructAndFormula(constructOrFormula(constructNotFormula(formula
                    .left()), formula.right()), constructOrFormula(constructNotFormula(formula
                    .right()), formula.left()));
            uvOut = uniqueVars(deltaPred, andFormula, uvIn);

        } else if (simpForm instanceof IfFormula) {
            IfFormula formula = (IfFormula) simpForm;
            // Just translate (f1 ? f2 : f3) into (f1 & f2 | !f1 & f3).
            Formula orFormula = constructOrFormula(constructAndFormula(formula.condSubFormula(),
                    formula.trueSubFormula()), constructAndFormula(constructNotFormula(formula
                    .condSubFormula()), formula.falseSubFormula()));
            uvOut = uniqueVars(deltaPred, orFormula, uvIn);
        } else {
            uvOut = null;
            throw new RuntimeException("Encountered an unfamiliar formula type : "
                    + simpForm.getClass().toString());
        }

        if (logging) {
            print("\nuniqueVars for formula " + f + "\n\tsimplified formula " + simpForm);
            print(":\n\t{");
            String sep = "";
            for (Iterator<Var> i = uvOut.iterator(); i.hasNext();) {
                Var var = i.next();
                print(sep + var);
                sep = ", ";
            }
            println("}");
        }

        return uvOut;
    }

    protected boolean unitChangePlus(PredicateFormula pf, Delta delta) {
        Var left = pf.getVariable(0);
        Var right = pf.getVariable(1);

        // Make sure left and right are among the free vars of delta+.
        Collection<Var> freeVars = delta.plus.freeVars();
        if (!(freeVars.contains(left) && freeVars.contains(right)))
            return false;

        // If the set of variables for which there can be at most one assignment
        // in Delta+ includes left and right, this is a positive unit change.
        Set<Var> uniqueVars = uniqueVars(pf.predicate(), delta.plus, Collections.<Var> emptySet());
        if (uniqueVars.contains(left) && uniqueVars.contains(right)) {
            println("\nIdentified unit positive change for predicate " + pf + "\n\tdelta+ = "
                    + delta.plus);
            // Delta- must be 0!
            if (!delta.minus.equals(falseFormula)) {
                println("\tNon-zero negative change: " + delta.minus);
                return false;
            }
            return true;
        }
        String message = "\nIdentified NON-UNIT positive change for predicate " + pf
                + "\n\tdelta+ = " + delta.plus;
        if (warnAboutNonUnitChanges)
            throw new SemanticErrorException(message);
        println(message);
        return false;
    }

    protected boolean unitChangeMinus(PredicateFormula pf, Delta delta) {
        Var left = pf.getVariable(0);
        Var right = pf.getVariable(1);

        // Make sure left and right are among the free vars of delta-.
        Collection<Var> freeVars = delta.minus.freeVars();
        if (!(freeVars.contains(left) && freeVars.contains(right)))
            return false;

        // If the set of variables for which there can be at most one assignment
        // in Delta- includes left and right, this is a negative unit change.
        Set<Var> uniqueVars = uniqueVars(pf.predicate(), delta.minus, Collections.<Var> emptySet());
        if (uniqueVars.contains(left) && uniqueVars.contains(right)) {
            println("\nIdentified unit negative change for predicate " + pf + "\n\tdelta- = "
                    + delta.minus);
            // Delta+ must be 0!
            if (!delta.plus.equals(falseFormula)) {
                println("\tNon-zero positive change: " + delta.plus);
                return false;
            }
            return true;
        }
        String message = "\nIdentified NON-UNIT negative change for predicate " + pf
                + "\n\tdelta- = " + delta.minus;
        if (warnAboutNonUnitChanges)
            throw new SemanticErrorException(message);
        println(message);
        return false;
    }

    // Tests if the (R)TC is a named (defines an instrum pred) binary predicate,
    // if the subformula is another binary predicate, and the latter is acyclic.
    // If all conditions are met, the acyclic predicate of the subFormula is
    // returned.
    // This is needed to check if we can use the Dong&Su updates.
    protected PredicateFormula acyclicBinaryPredicateInNamedBinaryTC(
            PredicateFormula instrumPredFormula, TransitiveFormula formula) {
        // Does formula define a binary instrumentation predicate?
        if (instrumPredFormula != null && instrumPredFormula.predicate().arity() == 2) {
            // Now test that the subformula is acyclic (for now assume this is a
            // predicate).
            if (formula.subFormula() instanceof PredicateFormula) {
                PredicateFormula pf = (PredicateFormula) formula.subFormula();
                if (pf.predicate().arity() == 2 && pf.predicate().acyclic()) {
                    return pf;
                }
            }
        }
        return null;
    }

    protected Delta transitiveDeltaFormulas(boolean reflexive, PredicateFormula instrumPredFormula,
            TransitiveFormula formula, PredicateUpdateMaps predUpdateMaps, boolean tight) {
        if (!reflexive) {
            String errStr = "Error: encountered non-reflexive TC " + formula;
            if (instrumPredFormula != null)
                errStr += "\nPredicate: " + instrumPredFormula;
            println(errStr);
            // throw new TVLAException(errStr);
        }

        // Use tight subDelta only if necessary, i.e. in the Dong&Su updates.
        // Use the untight/default subDelta in the Psi case to avoid extra
        // precision loss.
        // Should rethink this. May want to switch to tight core deltas
        // everywhere. Alexey
        Delta subDeltaTight = deltaFormulas(null, formula.subFormula(), predUpdateMaps, true);
        Delta subDelta = tight ? subDeltaTight : deltaFormulas(null, formula.subFormula(),
                predUpdateMaps, tight);

        // See if we can use the precise update for unit change of acyclic
        // (R)TC.
        // To use that, we need the (R)TC to be named (need to reuse the old
        // value),
        // and the subformula to be an acyclic binary predicate.
        PredicateFormula pf = acyclicBinaryPredicateInNamedBinaryTC(instrumPredFormula, formula);
        if (pf != null) {
            // We assume that subLeft and subRight are the two args of pf.
            // If this weren't the case, TVLA would have complained by now.
            // However, we don't care in what order they appear in pf.
            // unitChangeXxx tests thus simply use the args of pf. We
            // pass subLeft and subRight to acyclicUnitChangeDeltaXxxFormula
            // methods because the Dong&Su formulas need to follow the
            // subLeft/subRight order (and not the pf argument order)
            // when constructing the chain of bound variable in the plus
            // formula of the edge addition case and in the Suspicious
            // formula of the edge removal case.

            // Tight delta here helps detect the unit change when testing
            // delta+ of sfe for roc update for n-edge removal. That way
            // a roc term appears in its delta+. We may want to use an
            // untight delta+ after the test passes. This is correct for
            // Dong&Su update of RTC but should check that for TC. Using
            // the same formula as the one being tested (and using the
            // tight version for both delta+ and delta-) is also cleaner.
            if (unitChangePlus(pf, subDeltaTight)) {
                println("\nDifferencing (Dong&Su) " + instrumPredFormula
                        + (reflexive ? ", an RTC" : ", a TC") + " of acyclic predicate " + pf
                        + "\n\twith unit positive change " + subDeltaTight.plus);
                Formula plus = acyclicUnitChangeDeltaPlusFormula(reflexive, instrumPredFormula, pf,
                        subDeltaTight.plus, formula.subLeft(), formula.subRight());
                return new Delta(plus, falseFormula);
            }
            // Switch to tight delta here to help detect the unit change and
            // to make sure delta- has two free variables. The S (Suspicious)
            // formula holds for too many tuples otherwise.
            else if (unitChangeMinus(pf, subDeltaTight)) {
                println("\nDifferencing (Dong&Su) " + instrumPredFormula
                        + (reflexive ? ", an RTC" : ", a TC") + " of acyclic predicate " + pf
                        + "\n\twith unit negative change " + subDeltaTight.minus);
                Formula minus = acyclicUnitChangeDeltaMinusFormula(reflexive, instrumPredFormula,
                        pf, subDeltaTight.minus, formula.subLeft(), formula.subRight());
                return new Delta(falseFormula, minus);
            }
        }

        // Use regular subDelta here to avoid unnecessary precision loss.
        Formula psi = psiFormula(reflexive, formula, instrumPredFormula, predUpdateMaps, tight);

        Formula plus = constructAndFormula(constructExistFormula(formula.subLeft(),
                constructExistFormula(formula.subRight(), subDelta.plus)), psi);

        Formula minus = constructAndFormula(constructExistFormula(formula.subLeft(),
                constructExistFormula(formula.subRight(), subDelta.minus)),
                constructNotFormula(psi));

        if (!plus.equals(falseFormula) || !minus.equals(falseFormula))
            println("\nDifferencing (Psi-style) " + instrumPredFormula
                    + (reflexive ? ", an RTC of " : ", a TC of ") + formula.subFormula()
                    + "\n\tdelta+ = " + plus + "\n\tdelta- = " + minus);

        return new Delta(plus, minus);
    }

    // Returns delta+ for reflexive and non-reflexive TC of an acyclic binary
    // predicate with a unit positive
    // change. Function info etc. is not used but that would not improve the
    // precision or cost.
    protected Formula acyclicUnitChangeDeltaPlusFormula(boolean reflexive,
            PredicateFormula tcInstrumPredFormula, PredicateFormula binPredSubFormula,
            Formula subPlus, Var subLeft, Var subRight) {
        Formula plus;

        Var v1 = Var.allocateVar();
        Var v2 = Var.allocateVar();
        Var left = tcInstrumPredFormula.getVariable(0);
        Var right = tcInstrumPredFormula.getVariable(1);

        if (!reflexive) {
            // Follow Dong & Su's construction:
            // (E v1 : tcInstrumPredFormula(left, v1) && subPlus(v1, right)) ||
            // (E v2 : subPlus(left, v2) && tcInstrumPredFormula(v2, right)) ||
            // (E v1,v2 : tcInstrumPredFormula(left, v1) && subPlus(v1, v2) &&
            // tcInstrumPredFormula(v2, right)) ||
            // (subPlus(left, right))

            Formula tcLeftV1 = tcInstrumPredFormula.copy();
            tcLeftV1.safeSubstituteVar(right, v1);
            Formula subPlusV1Right = subPlus.copy();
            subPlusV1Right.safeSubstituteVar(subLeft, v1);
            Formula tail = constructExistFormula(v1, constructAndFormula(tcLeftV1, subPlusV1Right));

            Formula subPlusLeftV2 = subPlus.copy();
            subPlusLeftV2.safeSubstituteVar(subRight, v2);
            Formula tcV2Right = tcInstrumPredFormula.copy();
            tcV2Right.safeSubstituteVar(left, v2);
            Formula head = constructExistFormula(v2, constructAndFormula(subPlusLeftV2, tcV2Right));

            Formula subPlusV1V2 = subPlus.copy();
            subPlusV1V2.safeSubstituteVar(subLeft, v1);
            subPlusV1V2.safeSubstituteVar(subRight, v2);
            Formula middle = constructExistFormula(v1, constructExistFormula(v2,
                    constructAndFormula(tcLeftV1.copy(), constructAndFormula(subPlusV1V2, tcV2Right
                            .copy()))));

            Formula subPlusLeftRight = subPlus.copy();
            subPlusLeftRight.safeSubstituteVar(subLeft, left);
            subPlusLeftRight.safeSubstituteVar(subRight, right);
            Formula alone = subPlusLeftRight;

            plus = constructOrFormula(tail, constructOrFormula(head, constructOrFormula(middle,
                    alone)));

        } else { // This is even simpler!
            // (E v1,v2 : tcInstrumPredFormula(left, v1) && subPlus(v1, v2) &&
            // tcInstrumPredFormula(v2, right))
            Formula tcLeftV1 = tcInstrumPredFormula.copy();
            tcLeftV1.safeSubstituteVar(right, v1);
            Formula subPlusV1V2 = subPlus.copy();
            subPlusV1V2.safeSubstituteVar(subLeft, v1);
            subPlusV1V2.safeSubstituteVar(subRight, v2);
            Formula tcV2Right = tcInstrumPredFormula.copy();
            tcV2Right.safeSubstituteVar(left, v2);
            plus = constructExistFormula(v1, constructExistFormula(v2, constructAndFormula(
                    tcLeftV1, constructAndFormula(subPlusV1V2, tcV2Right))));
        }
        return plus;
    }

    // Returns delta- for reflexive and non-reflexive TC of an acyclic binary
    // predicate with a unit negative
    // change. Function info etc. is not used, so this is more general than
    // what's needed for linked lists
    // and even than for trees (tree links are invfunction). Consider splitting
    // this up into those cases
    // for more efficient updates in those special cases (precision should not
    // be affected).
    protected Formula acyclicUnitChangeDeltaMinusFormula(boolean reflexive,
            PredicateFormula tcInstrumPredFormula, PredicateFormula binPredSubFormula,
            Formula subMinus, Var subLeft, Var subRight) {
        Var v1 = Var.allocateVar();
        Var v2 = Var.allocateVar();
        Var v11 = Var.allocateVar();
        Var v12 = Var.allocateVar();
        Var left = tcInstrumPredFormula.getVariable(0);
        Var right = tcInstrumPredFormula.getVariable(1);

        // Note that the underlying predicate is acyclic, so S_ab must be
        // antireflexive.
        // As a result, with no change the normal Dong & Su construction works
        // for RTC!

        // Follow Dong & Su's construction.

        // S_ab: the set of all paths left -> right in the old TC which use the
        // deleted edge.
        // S is for suspicious.
        // (E v11,v12 : tcInstrumPredFormula(left, v11) && subMinus(v11, v12) &&
        // tcInstrumPredFormula(v12, right))
        Formula tcLeftV11 = tcInstrumPredFormula.copy();
        tcLeftV11.safeSubstituteVar(right, v11);
        Formula subMinusV11V12 = subMinus.copy();
        subMinusV11V12.safeSubstituteVar(subLeft, v11);
        subMinusV11V12.safeSubstituteVar(subRight, v12);
        Formula tcV12Right = tcInstrumPredFormula.copy();
        tcV12Right.safeSubstituteVar(left, v12);
        Formula S_ab = constructExistFormula(v11, constructExistFormula(v12, constructAndFormula(
                tcLeftV11, constructAndFormula(subMinusV11V12, tcV12Right))));

        // T_ab: the set of paths left -> right in (TC^old - S_ab) U G^new. T is
        // for trusty.
        // (tcInstrumPredFormula(left, right) && !S_ab(left, right)) ||
        // (binPredSubFormula(left, right) && !subMinus(left, right))
        Formula bpsfLeftRight = binPredSubFormula.copy();
        bpsfLeftRight.safeSubstituteVar(subLeft, left);
        bpsfLeftRight.safeSubstituteVar(subRight, right);
        Formula subMinusLeftRight = subMinus.copy();
        subMinusLeftRight.safeSubstituteVar(subLeft, left);
        subMinusLeftRight.safeSubstituteVar(subRight, right);
        Formula Gnew = constructAndFormula(bpsfLeftRight, constructNotFormula(subMinusLeftRight));

        Formula tcLeftRight = tcInstrumPredFormula.copy();
        Formula T_ab = constructOrFormula(constructAndFormula(tcLeftRight,
                constructNotFormula(S_ab)), Gnew);

        // The final answer is T_ab U (T_ab o T_ab) U (T_ab o T_ab o T_ab),
        // where R1 o R2 := {(x,y) | E u (R1(x,u) & R2(u,y))} (join/projection).
        Formula T_abLeftV1 = T_ab.copy();
        T_abLeftV1.safeSubstituteVar(right, v1);
        Formula T_abV1Right = T_ab.copy();
        T_abV1Right.safeSubstituteVar(left, v1);
        Formula T_abT_ab = constructExistFormula(v1, constructAndFormula(T_abLeftV1, T_abV1Right));

        // It seems like the middle T_ab (T_abV1V2) below could be replaced with
        // simply G_new
        // provided there's another term handling the reflexive case.
        Formula T_abV1V2 = T_ab.copy();
        T_abV1V2.safeSubstituteVar(left, v1);
        T_abV1V2.safeSubstituteVar(right, v2);
        Formula T_abV2Right = T_ab.copy();
        T_abV2Right.safeSubstituteVar(left, v2);
        Formula T_abT_abT_ab = constructExistFormula(v1, constructExistFormula(v2,
                constructAndFormula(T_abLeftV1.copy(), constructAndFormula(T_abV1V2, T_abV2Right))));

        // (T_ab o T_ab o T_ab) is reflexive TC, and T_ab U (T_ab o T_ab) U
        // (T_ab o T_ab o T_ab)
        // is non-reflexive TC. Delta- is the negation.
        Formula TC = reflexive ? T_abT_abT_ab : constructOrFormula(T_ab, constructOrFormula(
                T_abT_ab, T_abT_abT_ab));
        return constructNotFormula(TC);

    }

    // Psi subformula of the differencing rule for TC. Now works for both
    // top-level
    // differencing calls, which correspond to instrum preds, and for lower
    // level
    // TC formulas which do not correspond to instrum preds. The latter case is
    // distinguished by passing null for intstrumPredFormula.
    // Also works for TC rules (as before), as well as for RTC rules, as defined
    // in the PoPL'03 submission. This psi is not correct in the non-reflexive
    // case
    // when v1 = v2 = w1 = w2. See discussions with Tom 9-10 July, 2002.
    protected Formula psiFormula(boolean reflexive, TransitiveFormula formula,
            PredicateFormula instrumPredFormula, PredicateUpdateMaps predUpdateMaps, boolean tight) {

        Var v1 = formula.subLeft();
        Var v2 = formula.subRight();
        Var v3 = formula.left();
        Var v4 = formula.right();

        // If arity isn't 2 (should only be 1 then), we can't use the predicate
        // in
        // constructing formulae like phi(v1,v2). In that case we have to
        // process
        // it as if the TC did not have a corresponding instrumentation
        // predicate.
        boolean useInstrPred = (instrumPredFormula != null && instrumPredFormula.predicate()
                .arity() == 2);

        // New vars must not be in phi or phi1!
        Var w1 = new Var("_w1_" + Var.maxId());
        Var w2 = new Var("_w2_" + Var.maxId());
        Var newV1 = null, newV2 = null;
        if (!useInstrPred) { // Non top-level
            // newV1 and newV2 will be the vars bound by TC (subLeft and
            // subRight).
            // Create these vars, so that we can use the actual TC formula
            // (rather
            // than the instrumentation predicate, which only works if the TC
            // formula
            // is top-level) when we need phi (for phiV1V2, phiV1W1, and
            // phiW2V2).
            // This is possible if we replace v3 and v4 in phi (TC formula) with
            // newV1/w2 and newV2/w1. As a result, we need to replace v1 with
            // newV1
            // and v2 with newV2 in Future[phi1(v1,v2)], i.e. t1, and in
            // equality tests
            // v1 = w1 and w2 = v2. When replacing, make sure to copy formulae
            // (and
            // deltas) before making changes to them.

            // New vars must not be in phi or phi1!
            newV1 = new Var("__v1_" + Var.maxId());
            newV2 = new Var("__v2_" + Var.maxId());
        }

        Formula t1 = futureFormula(null, null, formula.subFormula(), predUpdateMaps, tight);
        if (!useInstrPred) { // Non top-level
            // See comments above to explain the substitutions below.
            t1 = t1.copy();
            t1.safeSubstituteVar(v1, newV1);
            t1.safeSubstituteVar(v2, newV2);
        }

        Formula phiV1V2;
        if (useInstrPred) // Top-level
            phiV1V2 = new PredicateFormula(instrumPredFormula.predicate(), v1, v2);
        else {
            phiV1V2 = formula.copy();
            // phiV1V2.safeSubstituteVar(v3, newV1);
            // phiV1V2.safeSubstituteVar(v4, newV2);
            // Handling transitive formulae with left = right: (TC v1,v2 : f)
            // (v3,v3).
            ((TransitiveFormula) phiV1V2).substituteLeft(newV1);
            ((TransitiveFormula) phiV1V2).substituteRight(newV2);

            if (reflexive) // Don't forget to turn into RTC!
                phiV1V2 = constructReflexiveFormula(newV1, newV2, phiV1V2);
        }

        Formula deltaMinus = deltaFormulas(null, formula.subFormula(), predUpdateMaps, tight).minus;
        deltaMinus.safeSubstituteVar(v1, w1);
        deltaMinus.safeSubstituteVar(v2, w2);

        Formula phiV1W1;
        if (useInstrPred) // Top-level
            phiV1W1 = new PredicateFormula(instrumPredFormula.predicate(), v1, w1);
        else {
            phiV1W1 = formula.copy();
            // phiV1W1.safeSubstituteVar(v3, newV1);
            // phiV1W1.safeSubstituteVar(v4, w1);
            // Handling transitive formulae with left = right: (TC v1,v2 : f)
            // (v3,v3).
            ((TransitiveFormula) phiV1W1).substituteLeft(newV1);
            ((TransitiveFormula) phiV1W1).substituteRight(w1);

            if (reflexive) // Don't forget to turn into RTC!
                phiV1W1 = constructReflexiveFormula(newV1, w1, phiV1W1);
        }

        Formula phiW2V2;
        if (useInstrPred) // Top-level
            phiW2V2 = new PredicateFormula(instrumPredFormula.predicate(), w2, v2);
        else {
            phiW2V2 = formula.copy();
            // phiW2V2.safeSubstituteVar(v3, w2);
            // phiW2V2.safeSubstituteVar(v4, newV2);
            // Handling transitive formulae with left = right: (TC v1,v2 : f)
            // (v3,v3).
            ((TransitiveFormula) phiW2V2).substituteLeft(w2);
            ((TransitiveFormula) phiW2V2).substituteRight(newV2);

            if (reflexive) // Don't forget to turn into RTC!
                phiW2V2 = constructReflexiveFormula(w2, newV2, phiW2V2);
        }

        Var subLeft = useInstrPred ? v1 : newV1;
        Var subRight = useInstrPred ? v2 : newV2;

        Formula eqV1W1 = null, eqW2V2 = null, eqV1V2 = null;
        if (!reflexive) {
            eqV1W1 = new EqualityFormula(subLeft, w1);
            eqW2V2 = new EqualityFormula(w2, subRight);
        } else
            eqV1V2 = new EqualityFormula(subLeft, subRight);
        Formula eqW1W2 = new EqualityFormula(w1, w2);

        Formula existBody = reflexive ? constructAndFormula(constructAndFormula(
                constructAndFormula(deltaMinus, phiV1W1), phiW2V2), constructNotFormula(eqW1W2))

        : constructAndFormula(constructAndFormula(constructAndFormula(deltaMinus,
                constructOrFormula(phiV1W1, eqV1W1)), constructOrFormula(phiW2V2, eqW2V2)),
                constructOrFormula(constructNotFormula(eqW1W2), constructAndFormula(eqV1W1.copy(),
                        eqW2V2.copy())));
        Formula transitiveBody = constructOrFormula(
                reflexive ? constructOrFormula(eqV1V2, t1) : t1, constructAndFormula(phiV1V2,
                        constructNotFormula(constructExistFormula(w1, constructExistFormula(w2,
                                existBody)))));
        return (reflexive ?
        // If this is an RTC, return (v3 == v4) | TC(...).
        constructReflexiveTransitiveFormula(v3, v4, subLeft, subRight, transitiveBody)
                : constructTransitiveFormula(v3, v4, subLeft, subRight, transitiveBody));
    }

    Map<Formula, Long> atomicIds;
    public Formula strongSimplify(Formula inputFormula) {
        // Canonize predicate formulas
        final Set<Formula> atomic = HashSetFactory.make();
        inputFormula.traverse(new FormulaTraverser() {
            public void visit(Formula f) {
                if (f instanceof AtomicFormula) {
                    atomic.add(f);
                }
            }
        });
        atomicIds = HashMapFactory.make();
        long  id = 1;
        for (Formula f : atomic) {
            atomicIds.put(f, id);
            id <<= 1;
        }
       
        return strongSimplify(inputFormula, 0, 0);
    }

    private Pair<Boolean, Long> parse(Formula formula) {
        boolean formulaNegated = false;
        if (formula instanceof NotFormula) {
            formula = ((NotFormula) formula).subFormula();
            formulaNegated = true;
        }
        if (formula instanceof PredicateFormula) {
            return Pair.create(formulaNegated, atomicIds.get(formula));
        } else {
            return null;
        }
    }
    
    // Returns the simplified formula.
    private Formula strongSimplify(Formula inputFormula, long posContext, long negContext) {
        // Save (and later restore) flag strongSimplify. Set to true for now.
        boolean savedSimplify = simplify;
        simplify = true;

        Formula simplifiedFormula = null;

        if (inputFormula instanceof AndFormula) {
            AndFormula formula = (AndFormula) inputFormula;
            Formula atomic = formula.left();
            Formula other = formula.right();
            Pair<Boolean, Long> atomicId = parse(atomic);            
            if (atomicId == null) {
                Formula tmp = atomic; atomic = other; other = tmp;
                atomicId = parse(atomic);
            } 
            if (atomicId != null) {
                atomic = strongSimplify(atomic, posContext, negContext);
                if (atomicId.first) { // Negated
                    negContext |= atomicId.second;
                } else {
                    posContext |= atomicId.second;
                }
                other = strongSimplify(other, posContext, negContext);
                simplifiedFormula = constructAndFormula(other, atomic);                    
            } else{
                Formula left = strongSimplify(formula.left(), posContext, negContext);
                Formula right = strongSimplify(formula.right(), posContext, negContext);
                if (parse(left) != null || parse(right) != null) {
                    simplifiedFormula = strongSimplify(new AndFormula(left,right), posContext, negContext);
                } else {
                    simplifiedFormula = constructAndFormula(left, right);
                }
            }                            
        } else if (inputFormula instanceof OrFormula) {
            OrFormula formula = (OrFormula) inputFormula;
            Formula atomic = formula.left();
            Formula other = formula.right();
            Pair<Boolean, Long> atomicId = parse(atomic);            
            if (atomicId == null) {
                Formula tmp = atomic; atomic = other; other = tmp;
                atomicId = parse(atomic);
            } 
            if (atomicId != null) {
                atomic = strongSimplify(atomic, posContext, negContext);
                if (atomicId.first) { // Negated
                    posContext |= atomicId.second;
                } else {
                    negContext |= atomicId.second;
                }
                other = strongSimplify(other, posContext, negContext);
                simplifiedFormula = constructOrFormula(other, atomic);                    
            } else{
                Formula left = strongSimplify(formula.left(), posContext, negContext);
                Formula right = strongSimplify(formula.right(), posContext, negContext);
                if (parse(left) != null || parse(right) != null) {
                    simplifiedFormula = strongSimplify(new OrFormula(left,right), posContext, negContext);
                } else {
                    simplifiedFormula = constructOrFormula(left, right);
                }
            }                            

        } else if (inputFormula instanceof NotFormula) {
            NotFormula formula = (NotFormula) inputFormula;
            simplifiedFormula = constructNotFormula(strongSimplify(formula.subFormula(), posContext, negContext));

        } else if (inputFormula instanceof ExistQuantFormula) {
            ExistQuantFormula formula = (ExistQuantFormula) inputFormula;
            simplifiedFormula = constructExistFormula(formula.boundVariable(), strongSimplify(formula
                    .subFormula(), posContext, negContext));

        } else if (inputFormula instanceof AllQuantFormula) {
            AllQuantFormula formula = (AllQuantFormula) inputFormula;
            simplifiedFormula = constructAllFormula(formula.boundVariable(), strongSimplify(formula
                    .subFormula(), posContext, negContext));

        } else if (inputFormula instanceof TransitiveFormula) {
            TransitiveFormula formula = (TransitiveFormula) inputFormula;
            simplifiedFormula = constructTransitiveFormula(formula.left(), formula.right(), formula
                    .subLeft(), formula.subRight(), strongSimplify(formula.subFormula(), 0, 0));

        } else if (inputFormula instanceof IfFormula) {
            IfFormula formula = (IfFormula) inputFormula;
            simplifiedFormula = constructIfFormula(
                    strongSimplify(formula.condSubFormula(), 0, 0),
                    strongSimplify(formula.trueSubFormula(), posContext, negContext), 
                    strongSimplify(formula.falseSubFormula(), posContext, negContext));

        } else if (inputFormula instanceof EquivalenceFormula) {
            EquivalenceFormula formula = (EquivalenceFormula) inputFormula;
            simplifiedFormula = constructEquivalenceFormula(strongSimplify(formula.left(), 0, 0),
                    strongSimplify(formula.right(), 0, 0));

        } else if (inputFormula instanceof ValueFormula) {
            simplifiedFormula = inputFormula;
        } else if (inputFormula instanceof AtomicFormula) { // Predicate and Equality
            long id = atomicIds.get(inputFormula);
            if ((posContext & id) != 0) {
                simplifiedFormula = new ValueFormula(Kleene.trueKleene);
            } else if ((negContext & id) != 0) {
                simplifiedFormula = new ValueFormula(Kleene.falseKleene);                
            } else {
                simplifiedFormula = inputFormula;
            }
        }
        
        simplify = savedSimplify; // Restore strongSimplify flag.
        return simplifiedFormula;
    }

}
