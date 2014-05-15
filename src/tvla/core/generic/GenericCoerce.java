package tvla.core.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.analysis.AnalysisStatus;
import tvla.core.Coerce;
import tvla.core.Constraints;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignKleene;
import tvla.core.common.GetFormulaPredicates;
import tvla.exceptions.SemanticErrorException;
import tvla.formulae.AndFormula;
import tvla.formulae.EqualityFormula;
import tvla.formulae.ExistQuantFormula;
import tvla.formulae.Formula;
import tvla.formulae.NotFormula;
import tvla.formulae.OrFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.TransitiveFormula;
import tvla.formulae.ValueFormula;
import tvla.formulae.Var;
import tvla.io.IOFacade;
import tvla.logic.Kleene;
import tvla.predicates.Instrumentation;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Logger;

/** A generic implementation of the Coerce algorithm.
 * @author Tal Lev-Ami
 * @since 14.9.2001 Renamed this class from NaiveCoerce (Roman).
 * @since tvla-2-alpha Generalized to handle arbitrary arity predicates (16 May 2002 Roman).
 */
public class GenericCoerce extends Coerce {
	protected static final int Invalid = -1;
	protected static final int Unmodified = 0;
	protected static final int Modified = 1;

	/** A convenience instance.
	 */
	public static Coerce defaultGenericCoerce = new GenericCoerce(Constraints.getInstance().constraints());
	protected Map<Predicate, Collection<TransitiveFormula>> allTC = HashMapFactory.make();

	protected Collection<Constraint> constraints = new ArrayList<Constraint>();

	public GenericCoerce(Set<Constraints.Constraint> constraints) {
		super();
		//Logger.println("Building " + constraints.size() + " constraints...");

		for (Constraints.Constraint constraint : constraints) {
			addConstraint(constraint.getBody(), constraint.getHead());
		}
		// All instrumentation predicates have been created.  We can now tell if there
		// is an RTC closure of a given predicate, so we can create constraints enforcing
		// acyclicity and uniqueness per connected component.
		addClosureConstraints();
	}
	
	public static void reset() {
		defaultGenericCoerce = new GenericCoerce(Constraints.getInstance().constraints());
	}
	
	public boolean coerce(TVS structure) {
		boolean change;
		do {
			change = false;
			for (Iterator<Constraint> constraintIt = constraints.iterator(); 
				 constraintIt.hasNext(); ) {
				Constraint constraint = constraintIt.next();
                if (!constraint.isActive(structure)) {
                    continue;
                }
				
				int action = coerce(structure, constraint);
				if (action == Invalid)
					return false;
				if (action == Modified)
					change = true;
			}
		} while (change);
		return true;
	}

	protected void addConstraint(Formula body, Formula head) {
		constraints.add(createConstraint(body, head));
	}

	protected Constraint createConstraint(Formula body, Formula head) {
		Constraint constraint = new Constraint(body);

		if (head instanceof ValueFormula) {
			constraint.constant();
		}
		else {
			List<Var> headFreeVars = head.freeVars();
			List<Var> bodyFreeVars = body.freeVars();
			if (headFreeVars.size() != bodyFreeVars.size() ||
			    !headFreeVars.containsAll(bodyFreeVars)) {
				if (debug)
					Logger.println("Warning : constraint sanity check failed - " +
								   "Free variables of head (" +
								   head + ") and body (" +
								   body + ") don't match.");
			}

			if (head instanceof NotFormula) {
				constraint.negated();
				NotFormula nhead = (NotFormula) head;
				head = nhead.subFormula();
			}
			if (head instanceof PredicateFormula) {
				constraint.predicate((PredicateFormula) head);
			}
			else if (head instanceof EqualityFormula) {
				constraint.equality((EqualityFormula) head);
			}
			else {
				throw new SemanticErrorException("The head must be either the " +
					" constant 0, a literal or a negated literal. " +
					"The head is " + head);
			}
		}
		constraint.onlyHead = HashSetFactory.make(head.freeVars());
		constraint.onlyHead.removeAll(body.freeVars());
		return constraint;
	}

	// Helper to addClosureConstraints().
	// Goes through all instrumentation predicates and checks if there is an RTC of p,
	// which is either an instrum pred with name "rtc[p]" or an instrum pred defined as p*.
	// The former is for cases such as possibly-cyclic lists, in which the RTC is not
	// defined as p* but rather through roc/sfp.
	protected Predicate findRTCofPred(Predicate p) {
	    Predicate rtcPred = Vocabulary.getPredicateByName("rtc[" + p.name() + "]");
	    if (rtcPred != null)
		return rtcPred;

	    for (Instrumentation instrum : Vocabulary.allInstrumentationPredicates()) {
		if (instrum.arity() != 2)
		    continue;

		Formula formula = instrum.getFormula();
		TransitiveFormula tc = formula.getTCforRTC();
		if (tc == null)
		    continue;

		if (debug)
		    Logger.println("\nIdentified OrFormula " + formula + "\n\t as R" + tc);

		Formula subTC = tc.subFormula();
		if (subTC instanceof PredicateFormula) {
		    PredicateFormula subTcPred = (PredicateFormula) subTC;
		    if (subTcPred.predicate().arity() == 2 && subTcPred.predicate().equals(p))
			return instrum;
		}
	    }
	    return null;
	}

	/** Go through all predicates, and for each acyclic predicate pred, look for
	 * corresponding RTC closure, rtc[pred].  If found, create the two constraints below,
	 * o.w. create only the first one with pred* in place of rtc[pred]:
	 * rtc[pred](v1, v2) & rtc[pred](v2, v1) ==> v1 == v2  (1)
	 * rtc[pred](v1, v2) & v1 != v2 ==> !rtc[pred](v2, v1) (2)
	 *
	 * Also for each pred with non-null property uniquePerCCofPred = ccPred, add the
	 * constraints below if we do have rtc[ccPred] and these constraints with ccPred*
	 * in place of rtc[ccPred], if we don't:
	 * pred(v1) & rtc[ccPred](v1, v2) & pred(v2) ==> v1 == v2  (1)
	 * pred(v1) & rtc[ccPred](v1, v2) & v1 != v2 ==> !pred(v2) (2)
	 * v1 != v2 & rtc[ccPred](v1, v2) & pred(v2) ==> !pred(v1) (3)
	 * pred(v1) & v1 != v2 & pred(v2) ==> !rtc[ccPred](v1, v2) (4)
	 *
	 * Should think about automatically generating rtc[pred/ccPred] given pred/ccPred.
	 *
 	 * @ author Alexey Loginov.
	 */ 
	protected void addClosureConstraints() {
	    // Go through all binary predicates and check if there are acyclic ones.
	    for (Predicate pred : Vocabulary.allBinaryPredicates()) {
		if (!pred.acyclic())
		    continue;

		Var v1 = new Var("v1");
		Var v2 = new Var("v2");

		// Go through all predicates and check if there is an RTC of pred.
		Predicate rtcPred = findRTCofPred(pred);

		if (rtcPred != null) {
		    // Now add the two constraints using rtcPred.

		    // rtc[pred](v1, v2) & rtc[pred](v2, v1) ==> v1 == v2
		    Formula rtcPredV1V2 = new PredicateFormula(rtcPred, v1, v2);
		    Formula rtcPredV2V1 = new PredicateFormula(rtcPred, v2, v1);
		    Formula body = new AndFormula(rtcPredV1V2, rtcPredV2V1);
		    Formula eqV1V2 = new EqualityFormula(v1, v2);
		    Formula head = eqV1V2;
		    addConstraint(body, head);
		    if (debug)
			Logger.println("Adding acyclicity constraint: " + body + " ==> " + head);

		    // rtc[pred](v1, v2) & v1 != v2 ==> !rtc[pred](v2, v1)
		    body = new AndFormula(rtcPredV1V2.copy(), new NotFormula(eqV1V2.copy()));
		    head = new NotFormula(rtcPredV2V1.copy());
		    addConstraint(body, head);
		    if (debug)
			Logger.println("Adding acyclicity constraint: " + body + " ==> " + head);
		}

		else {
		    // RTC of pred not found, add pred*(v1, v2) & pred*(v2, v1) ==> v1 == v2.
		    Var v3 = new Var("v3");
		    Var v4 = new Var("v4");
		    Formula predV3V4 = new PredicateFormula(pred, v3, v4);
		    Formula rtc1 = new OrFormula(new EqualityFormula(v1, v2),
						 new TransitiveFormula(v1, v2, v3, v4, predV3V4));
		    Formula rtc2 = new OrFormula(new EqualityFormula(v2, v1),
						 new TransitiveFormula(v2, v1, v3, v4, predV3V4.copy()));
		    Formula body = new AndFormula(rtc1, rtc2);
		    Formula head = new EqualityFormula(v1, v2);
		    addConstraint(body, head);
		    if (debug)
			Logger.println("Adding acyclicity constraint: " + body + " ==> " + head);
		}
	    }


	    // Go through all unary predicates and check if there are uniquePerCC ones.
	    for (Predicate pred : Vocabulary.allUnaryPredicates()) {
		Predicate ccPred = pred.uniquePerCCofPred();
		if (ccPred == null)
		    continue;

		Var v1 = new Var("v1");
		Var v2 = new Var("v2");

		// Go through all predicates and check if there is an RTC of ccPred.
		Predicate rtcPred = findRTCofPred(ccPred);

		Formula rtcV1V2;

		if (rtcPred != null)
		    rtcV1V2 = new PredicateFormula(rtcPred, v1, v2);
		else {
		    // RTC of ccPred not found, construct ccPred*(v1, v2).
		    Var v3 = new Var("v3");
		    Var v4 = new Var("v4");
		    Formula ccPredV3V4 = new PredicateFormula(ccPred, v3, v4);
		    rtcV1V2 = new OrFormula(new EqualityFormula(v1, v2),
					    new TransitiveFormula(v1, v2, v3, v4, ccPredV3V4));
		}

		// Now add the four constraints using the constructed rtcV1V2.
		// rtc[ccPred] in the comments stands for ccPred*, if rtcPred is null.

		// pred(v1) & rtc[ccPred](v1, v2) & pred(v2) ==> v1 == v2  (1)
		Formula predV1 = new PredicateFormula(pred, v1);
		Formula predV2 = new PredicateFormula(pred, v2);
		Formula body = new AndFormula(new AndFormula(predV1, rtcV1V2), predV2);
		Formula eqV1V2 = new EqualityFormula(v1, v2);
		Formula head = eqV1V2;
		addConstraint(body, head);
		if (debug)
		    Logger.println("Adding uniqueness per CC constraint: " + body + " ==> " + head);

		// E v1 : pred(v1) & rtc[ccPred](v1, v2) & v1 != v2 ==> !pred(v2) (2)
		body = new AndFormula(new AndFormula(predV1.copy(), rtcV1V2.copy()),
				      new NotFormula(eqV1V2.copy()));
		body = new ExistQuantFormula(v1, body);
		head = new NotFormula(predV2.copy());
		addConstraint(body, head);
		if (debug)
		    Logger.println("Adding uniqueness per CC constraint: " + body + " ==> " + head);

		// E v2 : v1 != v2 & rtc[ccPred](v1, v2) & pred(v2) ==> !pred(v1) (3)
		body = new AndFormula(new AndFormula(new NotFormula(eqV1V2.copy()), rtcV1V2.copy()),
				      predV2.copy());
		body = new ExistQuantFormula(v2, body);
		head = new NotFormula(predV1.copy());
		addConstraint(body, head);
		if (debug)
		    Logger.println("Adding uniqueness per CC constraint: " + body + " ==> " + head);

		if (rtcPred != null) {
		    // The head of the constraint below is only valid if pred rtc[ccPred] exists.

		    // pred(v1) & v1 != v2 & pred(v2) ==> !rtc[ccPred](v1, v2) (4)
		    body = new AndFormula(new AndFormula(predV1.copy(), new NotFormula(eqV1V2.copy())),
					  predV2.copy());
		    head = new NotFormula(rtcV1V2.copy());
		    addConstraint(body, head);
		    if (debug)
			Logger.println("Adding uniqueness per CC constraint: " + body + " ==> " + head);
		}
	    }
	}

	protected final int coerce(TVS structure, Constraint constraint, Assign trueAssign) {
		if (constraint.constant) {
			// Constraint breached. No way to repair.
			if (AnalysisStatus.debug)
				IOFacade.instance().printStructure(structure, "Constraint Breached: " + 
															  constraint + " on assignment " + 
															  trueAssign);
			return Invalid;
		} 
		else if (constraint.predicateFormula != null) {
			Predicate predicate = constraint.predicateFormula.predicate();
			NodeTuple tuple = NodeTuple.EMPTY_TUPLE;
			
			if (predicate.arity() > 0) { // building the tuple for the truth assignment
				Var  [] vars     = constraint.predicateFormula.variables();
				Node [] nodesTmp = new Node[predicate.arity()];
				for (int index = 0; index < nodesTmp.length; ++index) {
					nodesTmp[index] = trueAssign.get(vars[index]);
				}
				tuple = NodeTuple.createTuple(nodesTmp);
			}
			
			Kleene currentValue = structure.eval(predicate, tuple);
			if (currentValue == (constraint.negated ? Kleene.trueKleene : Kleene.falseKleene)) {
				// Constraint breached. No way to repair.
				if (AnalysisStatus.debug)
					IOFacade.instance().printStructure(structure, "Constraint Breached: " + 
																  constraint + " on assignment " + 
																  trueAssign);
				return Invalid;
			}
			else if (currentValue == Kleene.unknownKleene) {
				// Fix the problem.
				structure.update(predicate,
								 tuple,
								 constraint.negated ? 
								 Kleene.falseKleene : Kleene.trueKleene);
				return Modified;
			}
		}
		else if (constraint.equality) {
			Node leftNode = trueAssign.get(constraint.left);
			Node rightNode = trueAssign.get(constraint.right);
			if (constraint.negated) {
				if (leftNode.equals(rightNode)) {
					// Constaint breached. No way to repair.
					if (AnalysisStatus.debug) 
						IOFacade.instance().printStructure(structure, "Constraint Breached:" + 
																	  constraint + " on assignment " + 
																	  trueAssign);
					return Invalid;
				}
			}
			else {
				if (leftNode.equals(rightNode)) {
					if (structure.eval(Vocabulary.sm, rightNode) == Kleene.unknownKleene) {
						// Fix the problem. This is no longer a summary node.
						structure.update(Vocabulary.sm,rightNode, Kleene.falseKleene);
						return Modified;
					}
				}
				else { // Constaint breached. No way to repair.
					if (AnalysisStatus.debug)
						IOFacade.instance().printStructure(structure, "Constraint Breached:" + 
																	  constraint + " on assignment " +
																	  trueAssign);
					return Invalid;
				}
			}
		}
		else {
			throw new RuntimeException("addConstraint should have handled this case.");
		}
		return Unmodified;
	}

	protected int coerce(TVS structure, Constraint constraint) {
		int total = Unmodified;
		//System.out.println("Bad constraint:" + constraint);
OUTER: 
		for (Iterator<AssignKleene> trueIt = 
							  structure.evalFormulaForValue(constraint.body,
															Assign.EMPTY,
															Kleene.trueKleene);
			 trueIt.hasNext(); ) {
			Assign trueAssign = (Assign) trueIt.next();
			for (Var var : trueAssign.bound()) {
				Node node = trueAssign.get(var);
				if (structure.eval(Vocabulary.active, node) == Kleene.unknownKleene) {
					continue OUTER;
				}
			}
			Iterator<Assign> iterator = Assign.getAllAssign(structure.nodes(), constraint.onlyHead, trueAssign);
			while (iterator.hasNext()) {
			    Assign assign = iterator.next();
    			int result = coerce(structure, constraint, assign);
    			if (result == Invalid) {
    				return Invalid;		
    			}
    			if (result == Modified)
    				total = Modified;
			}
		}
		return total;
	}

	/** A class representing a constraint that can be applied to
	 * a structure.
	 */
	protected static class Constraint {
		public Set<Var> onlyHead;
        public Formula body;
		boolean constant = false;
		boolean negated = false;
		boolean equality = false;
		public PredicateFormula predicateFormula = null;
		Collection<Predicate> predicates = new ArrayList<Predicate>();
		
	    public boolean isActive(TVS structure) {
	        Set<Predicate> activePredicates = structure.getVocabulary().all();
	        for (Predicate predicate : predicates) {
	            if (!activePredicates.contains(predicate)) {
	                return false;
	            }
	        }
	        return true;
	    }
		
		/** A variable needed for an equality constraint.
		 */
		public Var left;

		/** A variable needed for an equality constraint.
		 */
		public Var right;

		public Formula body() {
			return this.body;
		}

		public Constraint(Formula body) {
			this.body = body;
			predicates.addAll(GetFormulaPredicates.get(body));
		}    

		public void negated() {
			this.negated = true;
		}

		public void constant() {
			this.constant = true;
		}

		public void predicate(PredicateFormula pf) {
			this.predicateFormula = pf;
			predicates.add(pf.predicate());
		}

		public void equality(EqualityFormula equality) {
			this.equality = true;
			this.left = equality.left();
			this.right = equality.right();
		}

		public String toString() {
			StringBuffer result = new StringBuffer();
			result.append(body.toString());
			result.append(" ==> ");
			if (constant)
				result.append(0);
			if (negated)
				result.append("!");
			if (equality)
				result.append(left + " == " + right);
			if (predicateFormula != null)
				result.append(predicateFormula);
			return result.toString();
		}
	}
}
