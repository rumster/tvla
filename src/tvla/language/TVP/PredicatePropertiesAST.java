package tvla.language.TVP;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tvla.core.Constraints;
import tvla.exceptions.SemanticErrorException;
import tvla.formulae.AndFormula;
import tvla.formulae.EqualityFormula;
import tvla.formulae.ExistQuantFormula;
import tvla.formulae.Formula;
import tvla.formulae.NotFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.ValueFormula;
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;

/**
 * An AST node for predicate properties.
 * Properties of a predicate are recorded using a set of boolean flags.
 * The properties are then used to generate constraints.
 * @author Eran Yahav
 * @since tvla-2-alpha (May 14 2002) Initial creation.
 */
public final class PredicatePropertiesAST extends AST {
	/** predicate arity */
	private int predicateArity;

	/** is the predicate unique? */
	protected static final int UNIQUE = 1;
	/** is this a function? */
	protected static final int FUNCTION = 2;
	/** is this an inverse function? */
	protected static final int INVFUNCTION = 3;
	/** is this an abstraction predicate (currently can only for nullary and unary) */
	protected static final int ABSTRACTION = 4; // DEFAULT TRUE
	/** is this a symmetric predicate? */
	protected static final int SYMMETRIC = 5;
	/** is this transitive? */
	protected static final int TRANSITIVE = 6;
	/** is this antisymmetric? */
	protected static final int ANTISYMMETRIC = 7;
	/** is this predicate reflexive? */
	protected static final int REFLEXIVE = 8;
	/** is this predicate anti-reflexive? */
	protected static final int ANTIREFLEXIVE = 9;
	/** does this predicate represent a pointer? (a display property) */
	protected static final int POINTER = 10;
	/** is this predicate a k-ary function? */
	protected static final int KARYFUNCTION = 11;
	/** @author Alexey Loginov. */
	protected static final int ACYCLIC = 12;

	/**
	* space for storing actual property bits
	* note that if extra properties are added, space
	* may require expansion
	*/
	private short propertiesBits;

	/** @TODO: Alexey -- document this */
	protected PredicateAST uniquePerCCofPred = null;

	/**
	 * list of uninterpreted property for general extensions
	 */
	private List uninterpretedProperties;

	/** default constructor */
	public PredicatePropertiesAST() {
		//	set default value
		setPropertyBit(ABSTRACTION, true);
	}

	/**
	 * copy constructor
	 * @param other - other PredicatePropertiesAST to be copied from
	 */
	public PredicatePropertiesAST(PredicatePropertiesAST other) {
		this.propertiesBits = other.propertiesBits;
		this.uniquePerCCofPred = other.uniquePerCCofPred;
	}

	/** Returns a copy of this PredicatePropertiesAST()
	 * @return a new copy of the properties AST
	 */
	public AST copy() {
		return new PredicatePropertiesAST(this);
	}

	/** Substitue doesn't need to do anything
	 * @param from - string to substitute
	 * @param to - string to replace the from-string
	 */
	public void substitute(PredicateAST from, PredicateAST to) {
	}

	/** Adds a property.
	 * @param prop - property string
	 */
	public void addProperty(String prop) {
		prop = prop.trim();
		if (prop.equals("unique")) {
			setPropertyBit(UNIQUE, true);
		} else if (prop.equals("function")) {
			setPropertyBit(FUNCTION, true);
		} else if (prop.equals("invfunction")) {
			setPropertyBit(INVFUNCTION, true);
		} else if (prop.equals("abs")) {
			setPropertyBit(ABSTRACTION, true);
		} else if (prop.equals("nonabs")) {
			setPropertyBit(ABSTRACTION, false);
		} else if (prop.equals("reflexive")) {
			setPropertyBit(REFLEXIVE, true);
		} else if (prop.equals("antireflexive")) {
			setPropertyBit(ANTIREFLEXIVE, true);
		} else if (prop.equals("symmetric")) {
			setPropertyBit(SYMMETRIC, true);
		} else if (prop.equals("antisymmetric")) {
			setPropertyBit(ANTISYMMETRIC, true);
		} else if (prop.equals("transitive")) {
			setPropertyBit(TRANSITIVE, true);
		} else if (prop.equals("pointer") || prop.equals("box")) {
			setPropertyBit(POINTER, true);
		} else if (prop.equals("acyclic")) {
			setPropertyBit(ACYCLIC, true);
		} else {
			throw new SemanticErrorException("Unknown property " + prop);
		}
	}

	/** Adds a property with parameters.
	 * @param prop - property name
	 * @param params - property parameters
	 */
	public void addProperty(String prop, List params) {
		if (params.isEmpty()) {
			addProperty(prop);
		} else {
			prop = prop.trim();

			if (prop.equals("uniquePerCC")) {

				if (params.size() == 1) {
					String ccPredicate = ((String) params.get(0)).trim();
					uniquePerCCofPred =
						PredicateAST.getPredicateAST(
							ccPredicate,
							new ArrayList());
				} else {
					throw new SemanticErrorException(
						"Property uniquePerCC is qualified by a single predicate name\n"
							+ "\tsaw list "
							+ params);
				}

			} else {
				throw new SemanticErrorException("Unknown property " + prop);
			}
		}
	}

	/**
	 * Adds a functional dependency.
	 * @param lhs - functional dependency left-hand-side
	 * @param rhs - functional dependency right-hand-side
	 */
	public void addFunctionalDependency(List lhs, String rhs) {
		throw new UnsupportedOperationException(
			"Implementation incomplete "
				+ "for functional dependencies of predicates with arbitrary arity!");
	}

	/**
	 * Validates the properties for a given predicate arity
	 * name is given for error reporting purposes.
	 * @param predName - predicate name
	 * @param arity - predicate arity
	 * An exception is thrown if predicate properties are invalid
	 */
	public void validate(String predName, int arity) {
		if (arity != 1) {
			if (getPropertyBit(UNIQUE)) {
				throw new SemanticErrorException(
					"Only unary predicates can be unique in " + predName);
			}
			if (this.uniquePerCCofPred != null) {
				throw new SemanticErrorException(
					"Only unary predicates can be uniquePerCC in " + predName);
			}
			if (getPropertyBit(POINTER)) {
				throw new SemanticErrorException(
					"Only unary predicates can be in a pointer in " + predName);
			}

		}
		if (arity != 2) {
			if (getPropertyBit(FUNCTION)) {
				throw new SemanticErrorException(
					"Only binary predicates can be injective in " + predName);
			}
			if (getPropertyBit(INVFUNCTION)) {
				throw new SemanticErrorException(
					"Only binary predicates can be invfunction in " + predName);
			}
			if (getPropertyBit(REFLEXIVE)) {
				throw new SemanticErrorException(
					"Only binary predicates can be reflexive in " + predName);
			}
			if (getPropertyBit(ANTIREFLEXIVE)) {
				throw new SemanticErrorException(
					"Only binary predicates can be antireflexive in "
						+ predName);
			}
			if (getPropertyBit(SYMMETRIC)) {
				throw new SemanticErrorException(
					"Only binary predicates can be symmetric in " + predName);
			}

			if (getPropertyBit(ANTISYMMETRIC)) {
				throw new SemanticErrorException(
					"Only binary predicates can be antisymmetric in "
						+ predName);
			}

			if (getPropertyBit(TRANSITIVE)) {
				throw new SemanticErrorException(
					"Only binary predicates can be transitive in " + predName);
			}

			if (getPropertyBit(ACYCLIC)) {
				throw new SemanticErrorException(
					"Only binary predicates can be acyclic in " + predName);
			}
		}
	}

	/**
	 * Generates constraints for the given predicate according to the
	 * current properties.
	 * @param predicate - predicate for which constraints are generated
	 */
	public void generateConstraints(Predicate predicate) {
		if (getPropertyBit(UNIQUE)) {
			Var v1 = new Var("v1");
			Var v2 = new Var("v2");
			Constraints.getInstance().addConstraint(
				new AndFormula(
					new PredicateFormula(predicate, v1),
					new PredicateFormula(predicate, v2)),
				new EqualityFormula(v1, v2));
			Constraints.getInstance().addConstraint(
				new ExistQuantFormula(
					v1,
					new AndFormula(
						new PredicateFormula(predicate, v1),
						new NotFormula(new EqualityFormula(v1, v2)))),
				new NotFormula(new PredicateFormula(predicate, v2)));
		}

		if (getPropertyBit(FUNCTION)) {
			Var v1 = new Var("v1");
			Var v2 = new Var("v2");
			Var v = new Var("v");
			Constraints.getInstance().addConstraint(
				new ExistQuantFormula(
					v,
					new AndFormula(
						new PredicateFormula(predicate, v, v1),
						new PredicateFormula(predicate, v, v2))),
				new EqualityFormula(v1, v2));
			Constraints.getInstance().addConstraint(
				new ExistQuantFormula(
					v1,
					new AndFormula(
						new PredicateFormula(predicate, v, v1),
						new NotFormula(new EqualityFormula(v1, v2)))),
				new NotFormula(new PredicateFormula(predicate, v, v2)));
		}

		if (getPropertyBit(INVFUNCTION)) {
			Var v1 = new Var("v1");
			Var v2 = new Var("v2");
			Var v = new Var("v");
			Constraints.getInstance().addConstraint(
				new ExistQuantFormula(
					v,
					new AndFormula(
						new PredicateFormula(predicate, v1, v),
						new PredicateFormula(predicate, v2, v))),
				new EqualityFormula(v1, v2));
			Constraints.getInstance().addConstraint(
				new ExistQuantFormula(
					v1,
					new AndFormula(
						new PredicateFormula(predicate, v1, v),
						new NotFormula(new EqualityFormula(v1, v2)))),
				new NotFormula(new PredicateFormula(predicate, v2, v)));
		}
		if (getPropertyBit(SYMMETRIC)) {
			Var v1 = new Var("v1");
			Var v2 = new Var("v2");
			Constraints.getInstance().addConstraint(
				new PredicateFormula(predicate, v1, v2),
				new PredicateFormula(predicate, v2, v1));
		}
		if (getPropertyBit(ANTISYMMETRIC)) {
			Var v1 = new Var("v1");
			Var v2 = new Var("v2");
			Constraints.getInstance().addConstraint(
				new AndFormula(
					new PredicateFormula(predicate, v1, v2),
					new PredicateFormula(predicate, v2, v1)),
				new EqualityFormula(v1, v2));
			Constraints.getInstance().addConstraint(
				new AndFormula(
					new PredicateFormula(predicate, v1, v2),
					new NotFormula(new EqualityFormula(v1, v2))),
				new NotFormula(new PredicateFormula(predicate, v2, v1)));
		}
		if (getPropertyBit(REFLEXIVE)) {
			Var v1 = new Var("v1");
			Var v2 = new Var("v2");
			Constraints.getInstance().addConstraint(
				new EqualityFormula(v1, v2),
				new PredicateFormula(predicate, v1, v2));
		}
		if (getPropertyBit(ANTIREFLEXIVE)) {
			Var v1 = new Var("v1");
			Var v2 = new Var("v2");
			Constraints.getInstance().addConstraint(
				new EqualityFormula(v1, v2),
				new NotFormula(new PredicateFormula(predicate, v1, v2)));
		}
		if (getPropertyBit(TRANSITIVE)) {
			Var v1 = new Var("v1");
			Var v2 = new Var("v2");
			Var v3 = new Var("v3");
			Constraints.getInstance().addConstraint(
				new ExistQuantFormula(
					v2,
					new AndFormula(
						new PredicateFormula(predicate, v1, v2),
						new PredicateFormula(predicate, v2, v3))),
				new PredicateFormula(predicate, v1, v3));
		}
		if (getPropertyBit(KARYFUNCTION)) {
			// k-ary function, taking (k-1)-tuple into a single value
			int arity = predicate.arity();

			Var[] vars1 = new Var[arity];
			Var[] vars2 = new Var[arity];

			int domSize = arity - 1;
			for (int i = 0; i < domSize; i++) {
				Var currVar = new Var("v" + i);
				vars1[i] = currVar;
				vars2[i] = currVar;
			}

			Var u1 = new Var("u1");
			Var u2 = new Var("u2");

			vars1[arity] = u1;
			vars2[arity] = u2;

			AndFormula andFormula =
				new AndFormula(
					new PredicateFormula(predicate, vars1),
					new PredicateFormula(predicate, vars2));

			Formula currFormula = andFormula;
			for (int j = domSize; j >= 0; j--) {
				currFormula = new ExistQuantFormula(vars1[j], currFormula);
			}
			Constraints.getInstance().addConstraint(
				currFormula,
				new EqualityFormula(u1, u2));

			Constraints.getInstance().addConstraint(
				new ExistQuantFormula(
					u1,
					new AndFormula(
						new PredicateFormula(predicate, vars1),
						new NotFormula(new EqualityFormula(u1, u2)))),
				new NotFormula(new PredicateFormula(predicate, vars2)));
		}

		if (getPropertyBit(ACYCLIC)) {
			Var v = new Var("v");
			Constraints.getInstance().addConstraint(
				new PredicateFormula(predicate, v, v),
				new ValueFormula(Kleene.falseKleene));
			// At this point we don't know if there's an instrumentation predicate, rtc[p], which
			// is the RTC closure of p.  However, we'll know it when Coerce converts these
			// constraints to its structure.  At that point we will add the two constraints below
			// if we do have rtc[p] and only the first one with p* in place of rtc[p], if we don't:
			// rtc[p](v1, v2) & rtc[p](v2, v1) ==> v1 == v2  (1)
			// rtc[p](v1, v2) & v1 != v2 ==> !rtc[p](v2, v1) (2)
			// In the future we may instead want to automatically generate rtc[p] given p.
		}

		if (uniquePerCCofPred != null) {
			Predicate ccPredicate = uniquePerCCofPred.getPredicate();

			Var v1 = new Var("v1");
			Var v2 = new Var("v2");
			Constraints.getInstance().addConstraint(
				new AndFormula(
					new AndFormula(
						new PredicateFormula(predicate, v1),
						new PredicateFormula(ccPredicate, v1, v2)),
					new PredicateFormula(predicate, v2)),
				new EqualityFormula(v1, v2));

			// At this point we don't know if there's an instrumentation predicate, rtc[ccPred],
			// which is the RTC closure of ccPred.  However, we'll know it when Coerce converts
			// these constraints to its structure.  At that point we will add the constraints
			// below if we do have rtc[ccPred] and these constraints with ccPred* in place of
			// rtc[ccPred], if we don't:
			// pred(v1) & rtc[ccPred](v1, v2) & pred(v2) ==> v1 == v2  (1)
			// pred(v1) & rtc[ccPred](v1, v2) & v1 != v2 ==> !pred(v2) (2)
			// v1 != v2 & rtc[ccPred](v1, v2) & pred(v2) ==> !pred(v1) (3)
			// pred(v1) & v1 != v2 & pred(v2) ==> !rtc[ccPred](v1, v2) (4)
			// In the future we may instead want to automatically generate rtc[ccPred] given ccPred.
		}
	}

	/**
	 * allows adding uninterpreted properties for general extensions
	 * @param str - property string
	 */
	public void addUninterpretedProperty(String str) {
		if (uninterpretedProperties == null) {
			uninterpretedProperties = new ArrayList();
		}
		uninterpretedProperties.add(str);
	}

	/**
		 * is the predicate unique?
		 * @return true when predicate is unique (can hold at most for one individual)
		*/
	public boolean unique() {
		return getPropertyBit(UNIQUE);
	}
	/**
		 * is this an abstraction predicate?
		 * @return true when abstraction predicate
		 */
	public boolean abstraction() {
		return getPropertyBit(ABSTRACTION);
	}
	/**
		 * is this predicate a function?
		 * @return true when the prediacte is a function
		 */
	public boolean function() {
		return getPropertyBit(FUNCTION);
	}
	/**
		 * is this predicate an inverse function?
		 * @return true when the predicate is an inverse function
		 */

	public boolean invfunction() {
		return getPropertyBit(INVFUNCTION);
	}

	/**
	* is this predicate a transitive?
	* @return true when the predicate is transitive
	*/
	public boolean transitive() {
		return getPropertyBit(TRANSITIVE);
	}

	/**
	* is this predicate a pointer? (this is only a representation property)
	* @return true when the predicate is a pointer
	*/
	public boolean pointer() {
		return getPropertyBit(POINTER);
	}

	/** @author Alexey Loginov.
	 * @return true when predicate is acyclic
	 */
	public boolean acyclic() {
		return getPropertyBit(ACYCLIC);
	}
	/**
	* enable/disable the abstraction for a predicate
	* @param val - true to enable abstraction
	*/
	public void setAbstraction(boolean val) {
		setPropertyBit(ABSTRACTION, val);
	}

	/**
	 * is this predicate reflexive?
	 * @return true when predicate is reflexive
	 */
	public boolean reflexive() {
		return getPropertyBit(REFLEXIVE);
	}

	/** @author Alexey Loginov.
	 * @return a predicate AST
	 */
	public PredicateAST uniquePerCCofPred() {
		return uniquePerCCofPred;
	}

	/**
	 * sets the artity to be recorded in properties
	 * @param predArity - predicate arity
	 */
	public void setArity(int predArity) {
		predicateArity = predArity;
	}

	/**
	 * get the value of the given property bit
	 * @param i - property bit to get
	 * @return the value of the property bit
	 */
	public boolean getPropertyBit(int i) {
		return (propertiesBits & (1 << i)) != 0;
	}

	/**
	 * sets the given property bit to the given value
	 * @param i - bit to be set
	 * @param flag - value to be set to
	 */
	public void setPropertyBit(int i, boolean flag) {
		if (flag) {
			propertiesBits = (short) (propertiesBits | (1 << i));
		} else {
			propertiesBits = (short) (propertiesBits & ~(1 << i));
		}
	}

	/**
	 * returns a human-readable representation of properties
	 * @return properties as string
	 */
	public String toString() {
		StringBuffer result = new StringBuffer("(");
		if (getPropertyBit(UNIQUE)) {
			result.append("unique ");
		}
		if (getPropertyBit(FUNCTION)) {
			result.append("function ");
		}
		if (getPropertyBit(INVFUNCTION)) {
			result.append("invfunction ");
		}
		if (getPropertyBit(ABSTRACTION)) {
			result.append("abstraction ");
		}
		if (getPropertyBit(SYMMETRIC)) {
			result.append("symmetric ");
		}
		if (getPropertyBit(TRANSITIVE)) {
			result.append("transitive ");
		}
		if (getPropertyBit(ANTISYMMETRIC)) {
			result.append("antisymmetric ");
		}
		if (getPropertyBit(REFLEXIVE)) {
			result.append("reflexive ");
		}
		if (getPropertyBit(ANTIREFLEXIVE)) {
			result.append("antireflexive ");
		}
		if (getPropertyBit(POINTER)) {
			result.append("pointer ");
		}
		if (getPropertyBit(KARYFUNCTION)) {
			result.append("karyfunction ");
		}
		if (getPropertyBit(ACYCLIC)) {
			result.append("acyclic ");
		}
		if (uniquePerCCofPred != null) {
			result.append("uniquePerCC[" + uniquePerCCofPred + "] ");
		}
		result.append(")");
		return result.toString();
	}

	/**
	* returns a string for re-parsing
	* @return string for re-parsing
	*/
	public String toParserString() {
		StringBuffer result = new StringBuffer();

		if (getPropertyBit(UNIQUE)) {
			result.append("unique ");
		}
		if (getPropertyBit(FUNCTION)) {
			result.append("function ");
		}
		if (getPropertyBit(INVFUNCTION)) {
			result.append("invfunction ");
		}
		if (getPropertyBit(SYMMETRIC)) {
			result.append("symmetric ");
		}
		if (getPropertyBit(TRANSITIVE)) {
			result.append("transitive ");
		}
		if (getPropertyBit(ANTISYMMETRIC)) {
			result.append("antisymmetric ");
		}
		if (getPropertyBit(REFLEXIVE)) {
			result.append("reflexive ");
		}
		if (getPropertyBit(ANTIREFLEXIVE)) {
			result.append("antireflexive ");
		}
		if (getPropertyBit(POINTER)) {
			result.append("pointer ");
		}
		if (getPropertyBit(KARYFUNCTION)) {
			result.append("karyfunction ");
		}
		if (getPropertyBit(ACYCLIC)) {
			result.append("acyclic ");
		}
		if (uniquePerCCofPred != null) {
			result.append("uniquePerCC[" + uniquePerCCofPred + "] ");
		}
		if (!getPropertyBit(ABSTRACTION) && predicateArity != 2) {
			result.append("nonabs ");
		}

		if (uninterpretedProperties != null) {
			String separator = "";
			for (Iterator it = uninterpretedProperties.iterator();
				it.hasNext();
				) {
				result.append(separator);
				result.append(it.next());
				separator = " ";
			}
		}
		return result.toString();
	}

}
