package tvla.formulae;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.util.HashMapFactory;
import tvla.util.ProgramProperties;

/** An abstract base class for the recursive structure 
 * representing a three valued quanitified formula.
 * @see tvla.core.TVS
 * @see tvla.formulae.Var
 * @author Tal Lev-Ami
 */
public abstract class Formula {
	/** Should we try renaming bound variables when testing for formula equality?
	 * Keep this as a flag for now, so we can compare cost and results.
	 */
	protected static boolean alphaRenamingEquals =
	    ProgramProperties.getBooleanProperty("tvla.formulae.alphaRenamingEquals", false);

	/** Free vars of this formula.  Need to reset to recompute after a substitution.
	 */
	protected List<Var> freeVars;
	
	/** Vars bound in this formula or subformulae.  Note, this list doesn't need to
	 * be recomputed after a substitution (but it needs to be recomputed after a
	 * normalization).
	 */
	protected List<Var> boundVars;

	/** Creates a copy of the given formula.
	 */
	public abstract Formula copy();

	/** Evaluate the formula on the given formula and assignment of free variables.
	 * If a free variable is not assign to, an exception will be thrown.
	 * @param s the structure on which to evaluate the formula.
	 * @param z the assignment of free variables.
	 */
	public abstract Kleene eval(TVS s, Assign z);

	/** Return the free variable of this formula in the order, in which they
	 *  appear in the formula.
	 */
	public List<Var> freeVars() {
		if (freeVars == null)
			recalcFreeVars();
		return freeVars;
	}

	/** Return variables bound in this formula or in its subformulae in the order
	 * in which they appear in the formula.
	 * freeVars together with boundVars comprise all vars used in this formula.
	 */
	public List<Var> boundVars() {
		if (boundVars == null)
			boundVars = calcBoundVars();
		return boundVars;
	}

	/** Prepare to be evaluated on the the given structure. 
	 * @param structure The structure on which the formula is going to be evaluated.
	 */
	public boolean askPrepare(TVS s) {
		return false;
	}
	
	protected boolean shouldCallPrepare = true;
	
	public void prepare(TVS s) {
		if (shouldCallPrepare) {
			shouldCallPrepare = askPrepare(s);
		}
	}
	/** Substitute a variable.
	 * @param from the old variable.
	 * @param to the new variable.
	 */
	public void substituteVar(Var from, Var to) {
	}

	/** Substitute a variable, while making sure that the new
	 * variable will not be captured by normalizing it out first.
	 * @param from the old variable.
	 * @param to the new variable.
	 */
	public void safeSubstituteVar(Var from, Var to) {
		// Try skipping normalization.
		if (boundVars().contains(to)) {
		    Var[] toArray = {to};
		    NormalizeOutVars.normalize(this, toArray);
		}
		substituteVar(from, to);
	}

	/** Substitute variables in parallel according to the sub map. */
	public void substituteVars(Map<Var, Var> sub) {
	}

	/** Parallel substitution of variables, while making sure that the
	 * new variables will not be captured by normalizing them out first.
	 * @param from the old variable array.
	 * @param to the new variable array.
	 */
	public void safeSubstituteVars(Var[] from, Var[] to) {
		if (to.length == 0) return;

		// Try skipping normalization.
		for (int i = 0; i < to.length; i++)
		    if (boundVars().contains(to[i])) {
			NormalizeOutVars.normalize(this, to);
			break;
		    }

		// Make a renaming map.
		Map<Var, Var> fromTo = HashMapFactory.make(from.length);
		for (int i = 0; i < from.length; i++)
		    fromTo.put(from[i], to[i]);

		// Now just call the regular substituteVars.
		substituteVars(fromTo);
	}

	/** Calculate and return the free variable of this formula, must be overriden by
	 * the subclass.
	 */
	protected abstract List<Var> calcFreeVars();

	/** Calculate and return variables bound in this formula or in subformulae,
	 * must be overriden by the subclass.
	 */
	protected abstract List<Var> calcBoundVars();

	/** This class represents a quantified variable.
	 */
	private static class Quant {
		/** true - All. false - Exist
		 */
		public boolean all;

		/** The bound variable.
		 */
		public Var variable;
		
		public Quant(boolean all, Var variable) {
			this.all = all;
			this.variable = variable;
		}
	}

	/** Calculate the prenex DNF normal form of the given formula.
	 * The sub formula of a transitive closure is kept as is.
	 */
	public static Formula toPrenexDNF(Formula formula) {
		List<Quant> quants = new ArrayList<Quant>();
		Formula DNF = i_toPrenexDNF(formula, quants, false);
		Collections.reverse(quants);
		for (Iterator<Quant> i = quants.iterator(); i.hasNext(); ) {
			Quant quant = i.next();
			if (quant.all) {
				DNF = new AllQuantFormula(quant.variable, DNF);
			}
			else {
				DNF = new ExistQuantFormula(quant.variable, DNF);
			}
		}
		return DNF;
	}

	/** Calculate the prenex CNF normal form of the given formula.
	 * The sub formula of a transitive closure is kept as is.
	 */
	public static Formula toPrenexCNF(Formula formula) {
		List<Quant> quants = new ArrayList<Quant>();
		Formula CNF = i_toPrenexCNF(formula, quants, false);
		Collections.reverse(quants);
		for (Iterator<Quant> i = quants.iterator(); i.hasNext(); ) {
			Quant quant = i.next();
			if (quant.all) {
				CNF = new AllQuantFormula(quant.variable, CNF);
			} else {
				CNF = new ExistQuantFormula(quant.variable, CNF);
			}
		}
		return CNF;
	}

	/** Add the terms of the top-most disjunction to the list.
	 * @param ors the list to which the terms are added, must be initialized. 
	 */
	public static void getOrs(Formula formula, Collection<Formula> ors) {
        LinkedList<OrFormula> workList = new LinkedList<OrFormula>();
        while (true) {
            if (formula != null) {
                if (formula instanceof OrFormula) {
                    OrFormula orFormula = (OrFormula) formula;
                    workList.add(orFormula);
                    formula = orFormula.left();
                } else {
                    ors.add(formula);
                    formula = null;
                }
            } else {
                if (workList.isEmpty())
                    return;
                formula = workList.removeFirst().right();
            }
        }
	}

	/** Add the terms of the topmost conjunction to the list.
	 * @param ands the list to which the terms are added, must be initialized. 
	 */
	public static void getAnds(Formula formula, Collection<Formula> ands) {
        LinkedList<AndFormula> workList = new LinkedList<AndFormula>();
        while (true) {
            if (formula != null) {
                if (formula instanceof AndFormula) {
                    AndFormula andFormula = (AndFormula) formula;
                    workList.add(andFormula);
                    formula = andFormula.left();
                } else {
                    ands.add(formula);
                    formula = null;
                }
            } else {
                if (workList.isEmpty())
                    return;
                formula = workList.removeFirst().right();
            }
        }
    }
	
	/** Distribute the implicit And between left and right with the topmost 
	 * ors in left and right. 
	 */
	private static Formula distributeOrOverAnd(Formula left, Formula right) {
		List<Formula> leftOrs = new ArrayList<Formula>();
		getOrs(left, leftOrs);
		List<Formula> rightOrs = new ArrayList<Formula>();
		getOrs(right,rightOrs);

		// Notice that after this operation normalize will not work correctly since
		// sub formulae can be shared!!!
		
		Formula result = null;
		for (Iterator<Formula> leftIt = leftOrs.iterator(); leftIt.hasNext(); ) {
			Formula leftFormula = leftIt.next();
			for (Iterator<Formula> rightIt = rightOrs.iterator(); rightIt.hasNext(); ) {
				Formula rightFormula = rightIt.next();
				if (result == null) {
					result = new AndFormula(leftFormula, rightFormula);
				} else {
					result = new OrFormula(result, new AndFormula(leftFormula, rightFormula));
				}
			}
		}
		return result;
	}

	/** Distribute the implicit Or between left and right with the topmost 
	 * Ands in left and right. 
	 */
	private static Formula distributeAndOverOr(Formula left, Formula right) {
		List<Formula> leftAnds = new ArrayList<Formula>();
		getAnds(left, leftAnds);
		List<Formula> rightAnds = new ArrayList<Formula>();
		getAnds(right,rightAnds);

		// Notice that after this operation normalize will not work correctly since
		// sub formulae can be shared!!!
		
		Formula result = null;
		for (Iterator<Formula> leftIt = leftAnds.iterator(); leftIt.hasNext(); ) {
			Formula leftFormula = leftIt.next();
			for (Iterator<Formula> rightIt = rightAnds.iterator(); rightIt.hasNext(); ) {
				Formula rightFormula = rightIt.next();
				if (result == null) {
					result = new OrFormula(leftFormula, rightFormula);
				} else {
					result = new AndFormula(result, new OrFormula(leftFormula, rightFormula));
				}
			}
		}
		return result;
	}

	/** Recursively calculate the prenex DNF normal form, used by toPrenexDNF.
	 * Returns the prenex DNF of the given formula.
	 * @param formula the sub formula considered
	 * @param quants the quantifiers met so far.
	 * @param negated is the current sub formula negated in the whole formula.
	 */
	private static Formula i_toPrenexDNF(Formula formula, List<Quant> quants, 
										 boolean negated) {
		if (formula instanceof ExistQuantFormula) {
			ExistQuantFormula eformula = (ExistQuantFormula) formula;
			eformula.normalize();
			quants.add(new Quant(negated, eformula.boundVariable()));
			return i_toPrenexDNF(eformula.subFormula(), quants, negated);
		} else if (formula instanceof AllQuantFormula) {
			AllQuantFormula aformula = (AllQuantFormula) formula;
			aformula.normalize();
			quants.add(new Quant(!negated, aformula.boundVariable()));
			return i_toPrenexDNF(aformula.subFormula(), quants, negated);
		} else if (formula instanceof AndFormula) {
			AndFormula aformula = (AndFormula) formula;
			Formula left = i_toPrenexDNF(aformula.left(), quants, negated);
			Formula right = i_toPrenexDNF(aformula.right(), quants, negated);
			if (negated) {
				return new OrFormula(left, right);
			} else {
				return distributeOrOverAnd(left, right);
			}
		} else if (formula instanceof ImpliesFormula) {
			ImpliesFormula oformula = (ImpliesFormula) formula;
			Formula left = i_toPrenexDNF(oformula.left(), quants, !negated);
			Formula right = i_toPrenexDNF(oformula.right(), quants, negated);
			if (negated) {
				return distributeOrOverAnd(left, right);
			} else {
				return new OrFormula(left, right);
			}
		} else if (formula instanceof OrFormula) {
			OrFormula oformula = (OrFormula) formula;
			Formula left = i_toPrenexDNF(oformula.left(), quants, negated);
			Formula right = i_toPrenexDNF(oformula.right(), quants, negated);
			if (negated) {
				return distributeOrOverAnd(left, right);
			} else {
				return new OrFormula(left, right);
			}
		} else if (formula instanceof NotFormula) {
			NotFormula nformula = (NotFormula) formula;
			return i_toPrenexDNF(nformula.subFormula(), quants, !negated);
		} else if (formula instanceof AtomicFormula) {
			if (negated) {
				return new NotFormula(formula);
			} else {
				return formula;
			}
		} else if (formula instanceof EquivalenceFormula) {
			EquivalenceFormula eformula = (EquivalenceFormula) formula;
			return i_toPrenexDNF(new OrFormula(new AndFormula(eformula.left().copy(), 
															  eformula.right().copy()),
											   new AndFormula(new NotFormula(eformula.left()),
															  new NotFormula(eformula.right()))),
								 quants, negated);
		} else if (formula instanceof IfFormula) {
			IfFormula iformula = (IfFormula) formula;
			return i_toPrenexDNF(new OrFormula(new AndFormula(iformula.condSubFormula(),
															  iformula.trueSubFormula()),
											   new AndFormula(
															  new NotFormula(iformula.condSubFormula().copy()),
															  iformula.falseSubFormula())),
								 quants, negated);
		} else if (formula instanceof TransitiveFormula) {
			if (negated) {
				return new NotFormula(formula);
			} else {
				return formula;
			}
		}
		else {
			throw new RuntimeException("Iternal Error. Unknown formula type for formula " + formula);
		}
	}

	/** Recursively calculate the prenex CNF normal form, used by toPrenexCNF.
	 * Returns the prenex CNF of the given formula.
	 * @param formula the sub formula considered
	 * @param quants the quantifiers met so far.
	 * @param negated is the current sub formula negated in the whole formula.
	 */
	private static Formula i_toPrenexCNF(Formula formula, List<Quant> quants, 
										 boolean negated) {
		if (formula instanceof ExistQuantFormula) {
			ExistQuantFormula eformula = (ExistQuantFormula) formula;
			eformula.normalize();
			quants.add(new Quant(negated, eformula.boundVariable()));
			return i_toPrenexCNF(eformula.subFormula(), quants, negated);
		} else if (formula instanceof AllQuantFormula) {
			AllQuantFormula aformula = (AllQuantFormula) formula;
			aformula.normalize();
			quants.add(new Quant(!negated, aformula.boundVariable()));
			return i_toPrenexCNF(aformula.subFormula(), quants, negated);
		} else if (formula instanceof AndFormula) {
			AndFormula aformula = (AndFormula) formula;
			Formula left = i_toPrenexCNF(aformula.left(), quants, negated);
			Formula right = i_toPrenexCNF(aformula.right(), quants, negated);
			if (negated) {
				return distributeAndOverOr(left, right);
			} else {
				return new AndFormula(left, right);
			}
		} else if (formula instanceof OrFormula) {
			OrFormula oformula = (OrFormula) formula;
			Formula left = i_toPrenexCNF(oformula.left(), quants, negated);
			Formula right = i_toPrenexCNF(oformula.right(), quants, negated);
			if (negated) {
				return new AndFormula(left, right);
			} else {
				return distributeAndOverOr(left, right);
			}
		} else if (formula instanceof ImpliesFormula) {
			ImpliesFormula oformula = (ImpliesFormula) formula;
			Formula left = i_toPrenexCNF(oformula.left(), quants, !negated);
			Formula right = i_toPrenexCNF(oformula.right(), quants, negated);
			if (negated) {
				return new AndFormula(left, right);
			} else {
				return distributeAndOverOr(left, right);
			}
		} else if (formula instanceof NotFormula) {
			NotFormula nformula = (NotFormula) formula;
			return i_toPrenexCNF(nformula.subFormula(), quants, !negated);
		} else if (formula instanceof AtomicFormula) {
			if (negated) {
				return new NotFormula(formula);
			} else {
				return formula;
			}
		} else if (formula instanceof EquivalenceFormula) {
			EquivalenceFormula eformula = (EquivalenceFormula) formula;
			return i_toPrenexCNF(new OrFormula(new AndFormula(eformula.left().copy(), 
															  eformula.right().copy()),
											   new AndFormula(new NotFormula(eformula.left()),
															  new NotFormula(eformula.right()))),
								 quants, negated);
		} else if (formula instanceof IfFormula) {
			IfFormula iformula = (IfFormula) formula;
			return i_toPrenexCNF(new OrFormula(new AndFormula(iformula.condSubFormula(),
															  iformula.trueSubFormula()),
											   new AndFormula(
															  new NotFormula(iformula.condSubFormula().copy()),
															  iformula.falseSubFormula())),
								 quants, negated);
		} else if (formula instanceof TransitiveFormula) {
			if (negated) {
				return new NotFormula(formula);
			} else {
				return formula;
			}
		}
		throw new RuntimeException("Iternal Error. Unknown formula type for formula " + formula);
	}

	/** Retrun all TC in the formula.
	 * @param formula the formula considered
	 * @param TCs a preinitialized list for the TCs.
	 */
	public static void getAllTC(Formula formula, List<Formula> TCs) {
		if (formula instanceof ExistQuantFormula) {
			ExistQuantFormula eformula = (ExistQuantFormula) formula;
			getAllTC(eformula.subFormula(), TCs);
		} else if (formula instanceof AllQuantFormula) {
			AllQuantFormula aformula = (AllQuantFormula) formula;
			getAllTC(aformula.subFormula(), TCs);
		} else if (formula instanceof AndFormula) {
			AndFormula aformula = (AndFormula) formula;
			getAllTC(aformula.left(), TCs);
			getAllTC(aformula.right(), TCs);
		} else if (formula instanceof OrFormula) {
			OrFormula oformula = (OrFormula) formula;
			getAllTC(oformula.left(), TCs);
			getAllTC(oformula.right(), TCs);
		} else if (formula instanceof NotFormula) {
			NotFormula nformula = (NotFormula) formula;
			getAllTC(nformula.subFormula(), TCs);
		} else if (formula instanceof AtomicFormula) {
			; // Nothing
		} else if (formula instanceof EquivalenceFormula) {
			EquivalenceFormula eformula = (EquivalenceFormula) formula;
			getAllTC(eformula.left(), TCs);
			getAllTC(eformula.right(), TCs);
		} else if (formula instanceof IfFormula) {
			IfFormula iformula = (IfFormula) formula;
			getAllTC(iformula.condSubFormula(), TCs);
			getAllTC(iformula.trueSubFormula(), TCs);
			getAllTC(iformula.falseSubFormula(), TCs);
		} else if (formula instanceof TransitiveFormula) {
			TCs.add(formula); // No support for nested TCs.
		} else {
			throw new RuntimeException("Unknown formula type for formula " + formula);
		}
	}
	
	/** If this formula matches the pattern for translation of <pred>*,
	 * return the TC formula, so we can simulate processing RTC.
	 * Should eventually introduce a new formula type, ReflexiveTransitiveFormula. */
	public TransitiveFormula getTCforRTC() {
		if (!(this instanceof OrFormula))
		    return null;

		OrFormula or = (OrFormula) this;

		EqualityFormula eq;
		TransitiveFormula tc;

		if (or.left() instanceof EqualityFormula &&
		    or.right() instanceof TransitiveFormula) {
		    eq = (EqualityFormula) or.left();
		    tc = (TransitiveFormula) or.right();
		} else if  (or.right() instanceof EqualityFormula &&
			    or.left() instanceof TransitiveFormula) {
		    eq = (EqualityFormula) or.right();
		    tc = (TransitiveFormula) or.left();
		} else return null;

		if (!(eq.left().equals(tc.left())  && eq.right().equals(tc.right()) ||
		      eq.left().equals(tc.right()) && eq.right().equals(tc.left())))
		    return null;

		// Return the TC formula.  Can pretend that it is an RTC.
		return tc;
	}
	
	/** Adds the specified variables as free variables that belong to the formula.
	 * This is used in order to include variables from the left hand side of update
	 * formulae.
	 * @author Roman Manevich
	 * @since 18.4.2001 First created. Solves a bug that causes incorrect updating
	 * of formulae in which not all variables on the left-hand side of the formula
	 * appear on its right-hand side.
	 */
	
	private Collection<Var> additionalVars = null;
	
	public void addAdditionalFreeVars(Collection<Var> vars) {
		/*
		if (freeVars == null)
			freeVars = calcFreeVars();
		freeVars.addAll(vars);
		*/
		
		Collection<Var> boundVars = boundVars();
		for (Var v : vars) {
			if (boundVars.contains(v)) {
				throw new RuntimeException("Bound variable " + v + " in formula " + this + " referenced on the left-hand side of update formula!");
			}
		}
		additionalVars = new tvla.util.NoDuplicateLinkedList<Var>(vars);
		if (freeVars != null)
			freeVars.addAll(additionalVars);
	}
	
	private void recalcFreeVars() {
		freeVars = calcFreeVars();
		if (additionalVars != null)
			freeVars.addAll(additionalVars);
	}
	
	/**
	 * return hashcode ignoring variable names
	 * to compute hashcode under alpha-renaming without affecting 
	 * regular hashcode computation.
	 * This is a compromise to avoid expensive renaming of formula variables
	 * when computing hashcodes. Such renaming would outcost the hash-clashes
	 * resulting from computing hash-values which ignore variable names.
	 */
	public abstract int ignoreVarHashCode();
		
	public abstract <T> T visit(FormulaVisitor<T> visitor);
	
	public Formula pushBackNegations(boolean negated) {
		if (!negated)
			return this;
		else
			return new NotFormula(this);
	}
	
	public Formula pushBackQuant(Var bound, boolean allQuant) {
		if (bound == null || !freeVars().contains(bound))
			return this;
		else 
			return allQuant ? new AllQuantFormula(bound, this) : 
							  new ExistQuantFormula(bound, this);
		
	}
/*	
	public Formula pushBackExistQuant(Var bound) {
		if (bound == null || !freeVars().contains(bound))
			return this;
		else 
			return new ExistQuantFormula(bound, this);
	}

	public Formula pushBackAllQuant(Var bound) {
		if (bound == null || !freeVars().contains(bound))
			return this;
		else 
			return new AllQuantFormula(bound, this);
	}
*/	
	public void toCNFArray(Collection<Formula> partial) {
		partial.add(this);
	}
	
	public Formula optimizeForEvaluation() {
		Formula newFormula = this;
		
		newFormula = newFormula.pushBackNegations(false);
		newFormula = newFormula.untangleUnbound();
		//newFormula.rebalanceQuantified();
		return newFormula;
	}
	
	public void rebalanceQuantified() {
	}
	
	public abstract void traversePreorder(FormulaTraverser t);
	public abstract void traversePostorder(FormulaTraverser t);
	
	public void traverse(FormulaTraverser t) {
		traversePostorder(t);
	}
	
	public Formula untangleUnbound() {
		Formula f = this.copy();
		int n = boundVars().size();
		for (int i = 0; i < n ; ++i) {
			f = f.pushBackQuant(null, false);
		}
		f.traverse(new FormulaTraverser() {
			public void visit(Formula f) {
				f.boundVars = null;
				f.freeVars = null;
			}
		});
		return f;
	}
	
	/**
	 * Conjugate all formulae to one formula
	 * @param rules
	 * @return
	 * @author Greta Yorsh
	 */
	public static Formula andAll(Collection<Formula> formulae) {
		Formula result = null;
		for (Formula formula : formulae) {
			if (result==null){
				result = formula;
			} else {
				result = new AndFormula(result,formula);
			}
		}
		return result;
	}
	
	/**
	 * @param formulae
	 * @return
	 * @author Greta Yorsh
	 */
	public static Formula orAll(Collection<Formula> formulae) {
		Formula result = null;
        for (Formula formula : formulae) {
			if (result==null){
				result = formula;
			} else {
				result = new OrFormula(result,formula);
			}
		}
		return result;
	}
	
	Set<Predicate> predicates = null;
	
	public Set<Predicate> getPredicates() {
		if (predicates != null) {
			return predicates;
		}
		predicates = java.util.Collections.emptySet();
		return predicates;
	}
	
	public FormulaIterator assignments(TVS structure, Assign partial) {
		return assignments(structure, partial, null);
	}
	
	public FormulaIterator assignments(TVS structure, Assign partial, Kleene value) {
		return new FormulaIterator(structure, this, partial, value);
	}
}
