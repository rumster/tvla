package tvla.formulae;

/** A visitor over first-order formulae (Visitor pattern).
 * @author Roman Manevich.
 * @since tvla-2-alpha November 18 2002, Initial creation.
 */
public  class FormulaVisitor<T> {
	/** A bottom-up traversal over the formula's structure.
	 */
	public void traverse(Formula formula) {
		if (formula instanceof AllQuantFormula) {
			AllQuantFormula allQuantFormula = (AllQuantFormula) formula;
			accept(allQuantFormula);
			traverse(allQuantFormula.subFormula());
		}
		else if (formula instanceof ExistQuantFormula) {
			ExistQuantFormula existQuantFormula = (ExistQuantFormula) formula;
			accept(existQuantFormula);
			traverse(existQuantFormula.subFormula());
		}
		else if (formula instanceof AndFormula) {
			AndFormula andFormula = (AndFormula) formula;
			accept(andFormula);
			traverse(andFormula.left());
			traverse(andFormula.right());
		}
		else if (formula instanceof EqualityFormula) {
			accept((EqualityFormula) formula);
		}
		else if (formula instanceof EquivalenceFormula) {
			EquivalenceFormula equivalenceFormula = (EquivalenceFormula) formula;
			accept(equivalenceFormula);
			traverse(equivalenceFormula.left());
			traverse(equivalenceFormula.right());
		}
		else if (formula instanceof ImpliesFormula) {
			ImpliesFormula impliesFormula = (ImpliesFormula) formula;
			accept(impliesFormula);
			traverse(impliesFormula.left());
			traverse(impliesFormula.right());
		}
		else if (formula instanceof IfFormula) {
			IfFormula ifFormula = (IfFormula) formula;
			accept(ifFormula);
			traverse(ifFormula.condSubFormula());
			traverse(ifFormula.trueSubFormula());
			traverse(ifFormula.falseSubFormula());
		}
		else if (formula instanceof NotFormula) {
			NotFormula notFormula = (NotFormula) formula;
			accept(notFormula);
			traverse(notFormula.subFormula());
		}
		else if (formula instanceof OrFormula) {
			OrFormula orFormula = (OrFormula) formula;
			accept(orFormula);
			traverse(orFormula.left());
			traverse(orFormula.right());
		}
		else if (formula instanceof PredicateFormula) {
			accept((PredicateFormula) formula);
		}
		else if (formula instanceof TransitiveFormula) {
			TransitiveFormula transitiveFormula = (TransitiveFormula) formula;
			accept(transitiveFormula);
			traverse(transitiveFormula.subFormula());
		}
		else if (formula instanceof ValueFormula) {
			accept((ValueFormula) formula);
		}
		else {
			throw new RuntimeException("Formula visitor encountered an unfamiliar " + 
				"formula type : " + formula.getClass().toString());
		}
	}

	/** Can be used to record the result of a formula acceptance.
	 */
	public void setResult(T result) {
	}
	
	/** Returns the last result stored.
	 */
	public T getResult() {
		return null;
	}

	/** Accepts a universally quantified formula.
	 */
	
	
	public T accept(AllQuantFormula formula)
	{
		return null;
	}
	
	/** Accepts a conjunctive formula.
	 */
	public T accept(AndFormula formula)
	{
		return null;
	}
	
	/** Accepts a variable equality formula.
	 */
	public T accept(EqualityFormula formula)
	{
		return null;
	}
	
	/** Accepts a equivalence ( <=> ) formula.
	 */
	public T accept(EquivalenceFormula formula)
	{
		return null;
	}

	/** Accepts a existencially quantified formula.
	 */
	public T accept(ExistQuantFormula formula)
	{
		return null;
	}

	/** Accepts an if-then-else formula.
	 */
	public T accept(IfFormula formula)
	{
		return null;
	}

	/** Accepts a negation formula.
	 */
	public T accept(NotFormula formula)
	{
		return null;
	}
	
	/** Accepts a disjunctive formula.
	 */
	public T accept(OrFormula formula)
	{
		return null;
	}

	/** Accepts an implies formula.
	 */
	public T accept(ImpliesFormula formula)
	{
		return null;
	}


	/** Accepts a predicate formula.
	 */
	public T accept(PredicateFormula formula)
	{
		return null;
	}

	/** Accepts a transitive-closure formula.
	 */
	public T accept(TransitiveFormula formula)
	{
		return null;
	}

	/** Accepts a constant truth value formula.
	 */
	public T accept(ValueFormula formula)
	{
		return null;
	}
}