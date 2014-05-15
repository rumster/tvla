package tvla.transitionSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.Engine;
import tvla.analysis.multithreading.ProgramThread;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignKleene;
import tvla.core.common.ModifiedPredicates;
import tvla.core.decompose.Decomposer;
import tvla.core.decompose.DecompositionName;
import tvla.exceptions.AbstractionRefinementException;
import tvla.exceptions.SemanticErrorException;
import tvla.formulae.AndFormula;
import tvla.formulae.CloneUpdateFormula;
import tvla.formulae.EqualityFormula;
import tvla.formulae.Formula;
import tvla.formulae.NewUpdateFormula;
import tvla.formulae.NotFormula;
import tvla.formulae.OrFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.PredicateUpdateFormula;
import tvla.formulae.RetainUpdateFormula;
import tvla.formulae.TransitiveFormula;
import tvla.formulae.ValueFormula;
import tvla.formulae.Var;
import tvla.io.IOFacade;
import tvla.logic.Kleene;
import tvla.predicates.DynamicVocabulary;
import tvla.predicates.LocationPredicate;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashSetFactory;
import tvla.util.ProgramProperties;
import tvla.util.SingleIterator;
import tvla.util.StringUtils;

/** This class represents the algorithm used to apply the abstract
 * semantics (transfer funciton) to a structures.
 * @author Tal Lev-Ami.
 */
public class Action {
	public static Set<Location> locationsWherePropertyFails = new HashSet<Location>();
	private boolean initialized = false;
	private Location actionLocation;
	
	private List<Formula> focusFormulae = new ArrayList<Formula>();
	private List<ReportMessage> messages = new ArrayList<ReportMessage>();
	private List<ReportMessage> postMessages = new ArrayList<ReportMessage>();
	private Formula precondition = null;
    private Formula composeFormula = null;
    private Formula decomposeFormula = null;
	private Formula internalPrecondition = null;
	private List<Formula> preconditionConjunction = null;
	private List<Formula> preconditionTC = null;
	private String title = null;
  
	private Formula haltCondition = null;
	
    /** A map from let Predicate to PredicateUpdateFormula.
     */
	
    //private Map letFormulae = HashMapFactory.make();
	private Map<Predicate, PredicateUpdateFormula> letFormulae = new LinkedHashMap<Predicate, PredicateUpdateFormula>();

    /** A map from Predicate to PredicateUpdateFormula.
	 */
	//private Map updateFormulae = HashMapFactory.make();
    private Map<Predicate, PredicateUpdateFormula> updateFormulae = new LinkedHashMap<Predicate, PredicateUpdateFormula>();
	private NewUpdateFormula newFormula = null;
	private RetainUpdateFormula retainFormula = null;
	private CloneUpdateFormula cloneFormula = null;
	private ProgramThread threadType;
	
	private Formula startFormula = null;
//NR	private Formula waitFormula = null;
	private Formula stopFormula = null;
//NR	private Var newVar = null;
//NR	private Var retainVar = null;
	private Var startVar = null;
//NR	private Var waitVar = null;
	private Var stopVar = null;
	
	/** internal predicates behavior */
//NR	private boolean focusRunnable = true;
	private boolean performUnschedule = true;
	private boolean updateLocation = true;
	private boolean checkRunnable = true;
    private Set<ReportMessage> checkedMessages = HashSetFactory.make();
    private Set<ReportMessage> candidateMessages = HashSetFactory.make();

	/** Should an Unknown answer to a precondition result in an AR Exception? */
	public static boolean throwUnknownPrecondException =
	  ProgramProperties.getBooleanProperty("tvla.absRef.refine", false) &&
	  ProgramProperties.getBooleanProperty("tvla.absRef.throwUnknownPreconditionException", false);
	private Formula framePre;
	private Formula frame;
	
	public static void reset() {
		locationsWherePropertyFails = new HashSet<Location>();
	}

	/** A class for associating a formula with
	 * an associated message string.
	 */
	public static class ReportMessage {
		protected Formula formula;
		protected String message;
        protected Formula composeFormula;
		
		public ReportMessage(Formula formula, String message, Formula composeFormula) {
			this.formula = formula;
			this.message = message;
            this.composeFormula = composeFormula;
		}
        
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (composeFormula != null) {
                builder.append("[").append(composeFormula).append("] ");
            }
            builder.append(formula);
            builder.append(" ==> ");
            builder.append(message);
            return builder.toString();
        }

        public void reportMessage(TVS structure, Assign assign, Collection<String> answer) {
            // Attempt to solve TC cache bug by calling prepare on eval
            formula.prepare(structure);
            Kleene value = formula.eval(structure, assign);
            if (value != Kleene.falseKleene) {
            	answer.add(message);
            	if (AnalysisStatus.debug)
            		IOFacade.instance().printStructure(structure, message);
            }
        }
        
        public void reportDefiniteMessage(TVS structure, Assign assign, Collection<String> answer, Kleene val) {
            // Attempt to solve TC cache bug by calling prepare on eval
            formula.prepare(structure);
            Kleene value = formula.eval(structure, assign);
            if (value == val) {
            	answer.add(message);
            	if (AnalysisStatus.debug)
            		IOFacade.instance().printStructure(structure, message);
            }
            else {
            	locationsWherePropertyFails.add((Location)Engine.getCurrentLocation());
            }
        }        

        public Formula getComposeFormula() {
            return composeFormula;
        }

        public boolean shouldCheck(DecompositionName currentComponent) {
            // Consider caching
            if (getComposeFormula() == null) {
                return true;
            }
            Set<Set<DecompositionName>> names = Decomposer.toDecompositionNames(getComposeFormula());
            Set<DecompositionName> composed = Decomposer.toComposedDecompositionNames(names);
            assert composed.size() == 1;
            DecompositionName name = composed.iterator().next();
            
            return name.canDecomposeFrom(currentComponent);
        }
	}

	/** Returns the answer to whether this action may change a structure.
	 * @author Roman Manevich,
	 * @since 27.8.2001 Initial creation.
	 */
	public boolean isSkipAction() {
		if (focusFormulae.size() != 0)
			return false;
		if (precondition != null)
			return false;
		if (newFormula != null)
			return false;
		if (retainFormula != null)
			return false;
		if (messages.size() != 0)
			return false;
		if (postMessages.size() != 0)
			return false;
		if (updateFormulae.size() != 0)
			return false;
		return true;
	}
	
	/** creates internal update formulae
	 */
	public void createInternalFormulae(LocationPredicate sourcePred, LocationPredicate targetPred) {
		Var freeThread = new Var("f");
		Formula source = new AndFormula(
										new PredicateFormula(sourcePred,freeThread),
										new NotFormula(new EqualityFormula(freeThread,Var.tr)));
		Formula target = new OrFormula(
									   new PredicateFormula(targetPred,freeThread),
									   new EqualityFormula(freeThread,Var.tr));
		
		if (updateLocation) {
			// set source location predicate to false
			setPredicateUpdateFormula(sourcePred,source,freeThread);
			// set target location predicate to true
			setPredicateUpdateFormula(targetPred,target,freeThread);
		}
		
		if (performUnschedule) {
			Formula unsched = new AndFormula(
											 new AndFormula(
															new PredicateFormula(Vocabulary.isThread,freeThread),
															new PredicateFormula(Vocabulary.ready,freeThread)),
											 new ValueFormula(Kleene.unknownKleene));
			setPredicateUpdateFormula(Vocabulary.runnable,unsched,freeThread);
		}
	}
	
	/** Creates a precondition for checking the internal requirements
	 */
	public void createInternalPrecondition(LocationPredicate source) {
		Formula precond;
		if (checkRunnable) {
			precond = new AndFormula(
									 new PredicateFormula(Vocabulary.runnable,Var.tr),
									 new PredicateFormula(source,Var.tr));	
			addFocusFormula(
							new PredicateFormula(Vocabulary.runnable,Var.tr));
		}
		else {
			precond = new PredicateFormula(source,Var.tr);	
		}
		internalPrecondition(precond);
	}	

	public void addFocusFormula(Formula formula) {
		focusFormulae.add(formula);
	}

	public boolean performUnschedule() {
		return performUnschedule;
	}
	
	public void performUnschedule(boolean perform) {
		performUnschedule = perform;
	}
	
	public boolean updateLocation() {
		return updateLocation;
	}
	
	public void updateLocation(boolean perform) {
		updateLocation = perform;
	}
	
	public void threadType(ProgramThread aThreadType) {
		threadType = aThreadType;
	}

	public void addMessage(Formula formula, String message, Formula composeFormula) {
		messages.add(new ReportMessage(formula, message, composeFormula));
	}

	public void addPostMessage(Formula formula, String message, Formula composeFormula) {
		postMessages.add(new ReportMessage(formula, message, composeFormula));
	}
	
	public void precondition(Formula formula) {
		this.precondition = formula.optimizeForEvaluation();
		// Clear the internal precondition representations
		// so they get reinitialized by checkPrecondition.
		this.preconditionConjunction = null;
		this.preconditionTC = null;
	}
	
	public void internalPrecondition(Formula formula) {
		this.internalPrecondition = formula;
		if (precondition == null) 
			precondition = internalPrecondition;
		else
			precondition = new AndFormula(internalPrecondition,precondition);
	}

	public NewUpdateFormula getNewFormula() {
		return newFormula;
	}

	public void newFormula(Formula formula) {
		this.newFormula = new NewUpdateFormula(formula);
	}

	public RetainUpdateFormula getRetainFormula() {
		return retainFormula;
	}

	public void retainFormula(Formula formula) {
		this.retainFormula = new RetainUpdateFormula(formula);
	}

	public CloneUpdateFormula getCloneFormula() {
		return cloneFormula;
	}

	public void cloneFormula(Formula formula) {
		this.cloneFormula = new CloneUpdateFormula(formula);
	}

	public void startFormula(Formula formula) {
		this.startFormula = formula;
	}
	public void waitFormula(Formula formula) {
//NR		this.waitFormula = formula;
	}

	public void stopFormula(Formula formula) {
		this.stopFormula = formula;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	public void setLocation(Location loc) {
		actionLocation = loc;
	}
	
	public Location location() {
		return actionLocation;
	}
	
	public void haltCondition(Formula hc) {
		haltCondition = hc;
	}
	
	public boolean isHalting() {
		return (haltCondition != null);
	}

	public Formula haltCondition() {
		return haltCondition.copy();
	}
	
	public String toString() {
		return title;
	}
    
    public void print(StringBuffer sb) {
      // title
      addLine(sb, "Title " + title);
      
      // compose
      if (composeFormula != null) {
          addLine(sb, "Compose: " + composeFormula);
      }
     
      // focus
      if (!focusFormulae.isEmpty()) {
        addLine(sb, "Focus formulae");
        for (Iterator<Formula> focusItr = focusFormulae.iterator(); focusItr.hasNext(); ) {
          Object o = focusItr.next();
          addLine(sb, "  " + o);
        }        
      }
      
      // precondition 
      if (precondition != null) {
        addLine(sb, "Precondition: " + precondition);
      }
      
      // message
      if (! messages.isEmpty()) {
        addLine(sb, "Messages");
        for (Iterator<ReportMessage> msgItr = messages.iterator(); msgItr.hasNext(); ) {
          Object o = msgItr.next();
          addLine(sb, "  " + o);
        }                
      }
      
      // new
      if (newFormula != null) {
        addLine(sb, "newFormula : " + newFormula.toString() );
      }
      
      // clone 
      if (cloneFormula != null) {
        addLine(sb, "cloneFormula : " + cloneFormula );
      }
      
      // let
      if (! letFormulae.isEmpty()) {
        addLine(sb, "Let formulae");
        for (Iterator<PredicateUpdateFormula> letItr = letFormulae.values().iterator(); letItr.hasNext(); ) {
          Object o = letItr.next();
          addLine(sb, "  " + o);
        }        
      }
      
      // update
      if (! updateFormulae.isEmpty()) {
        addLine(sb, "Update formulae");
        for (Iterator<PredicateUpdateFormula> updItr = updateFormulae.values().iterator(); updItr.hasNext(); ) {
          Object o = updItr.next();
          addLine(sb, "  " + o);
        }
      }
       
      // retain 
      if (retainFormula != null) {
        addLine(sb, "retainFormula : " + cloneFormula );
      }
      
      // post message
      if (! postMessages.isEmpty()) {
        addLine(sb, "Post messages");
        for (Iterator<ReportMessage> msgItr = postMessages.iterator(); msgItr.hasNext(); ) {
          Object o = msgItr.next();
          addLine(sb, "  " + o);
        }                
      }
      
      // decompose
      if (decomposeFormula != null) {
          addLine(sb, "Decompose: " + decomposeFormula);
      }

    }

    private void addLine(StringBuffer bf, String str) {
      bf.append(str + StringUtils.newLine);
    }
    

	public Collection<Assign> checkPrecondition(TVS structure) {
		assert initialized : "Attempt to check a precodnition on action " + this + ", which has not been initialized!";
		try {
		Set<Assign> satisfy = HashSetFactory.make();
		if (precondition == null) {
			satisfy.add(Assign.EMPTY);
		}
		else {
			if (preconditionConjunction == null) {
				preconditionConjunction = new ArrayList<Formula>();
				Formula.getAnds(precondition, preconditionConjunction);
				preconditionTC = new ArrayList<Formula>();
				Formula.getAllTC(precondition, preconditionTC);
			}
			for (Iterator<Formula> it = preconditionTC.iterator(); it.hasNext(); ) {
				tvla.formulae.TransitiveFormula TC = (TransitiveFormula) it.next();
				TC.explicitRecalc();
			}
			int numberOfSteps = preconditionConjunction.size();
			Assign[] stepAssign = new Assign[numberOfSteps + 1];
			Iterator[] stepIt = new Iterator[numberOfSteps + 1];
			stepIt[0] = new SingleIterator<Assign>(Assign.EMPTY);
			int currentStep = 0;
			while (currentStep >= 0) {
				if (stepIt[currentStep].hasNext()) {
					Assign currentAssign = stepAssign[currentStep] = (Assign) stepIt[currentStep].next();
					if (currentStep == numberOfSteps) {
						Assign satisfyAssign = new Assign(currentAssign);
						satisfyAssign.project(precondition.freeVars());
						satisfy.add(satisfyAssign);
						if (throwUnknownPrecondException &&
						    ((AssignKleene) currentAssign).kleene.equals(Kleene.unknownKleene))
							throw new AbstractionRefinementException(actionLocation.label(), this,
							                                         structure, satisfyAssign);
					} 
					else {
						Formula formula = preconditionConjunction.get(currentStep);
						currentStep++;
						stepIt[currentStep] = structure.evalFormula(formula, currentAssign);
					}
				} 
				else {
					currentStep--;
				}
			}
			for (Iterator<Formula> it = preconditionTC.iterator(); it.hasNext(); ) {
				tvla.formulae.TransitiveFormula TC = (TransitiveFormula) it.next();
				TC.setCalculatedTC(null);
			}
		}
		return satisfy;
		}
		catch (SemanticErrorException e) {
			e.append("while checking the precondition " + precondition);
			throw e;
		}
	}

	public boolean checkHaltCondition(HighLevelTVS structure, Assign assign) {
		assert initialized : "Attempt to check a precodnition on action " + this + ", which has not been initialized!";
		if (haltCondition == null) 
			return false;
		else {
		    // Attempt to solve TC cache bug by calling prepare on eval
            haltCondition.prepare(structure);
			return haltCondition.eval(structure, assign).equals(Kleene.trueKleene);
        }
	}
	
	public List<Formula> getFocusFormulae() {
	  return focusFormulae;
	}
	
	public Formula getPrecondition() {
	  return precondition;
	}
	
	/** Retrieves the let formula associated with a predicate.
	 */ 
	public PredicateUpdateFormula getLetFormula(Predicate predicate) {
	  return letFormulae.get(predicate);
	}
	
	/** Retrieves all the update formulas, as a Map from Predicate to PredicateUpdateFormula.
	 */
	public Map<Predicate, PredicateUpdateFormula> getLetFormulae(){
	  return letFormulae;
	}
	
	
	/** Retrieves the update formula associated with a predicate.
	 */ 
	public PredicateUpdateFormula getUpdateFormula(Predicate predicate) {
	  return updateFormulae.get(predicate);
	}
	
	/** Retrieves all the update formulas, as a Map from Predicate to PredicateUpdateFormula.
	 */
	public Map<Predicate, PredicateUpdateFormula> getUpdateFormulae(){
	  return updateFormulae;
	}
	
	/** Associates an update formula with a nulary predicate.
	 */
	public void setPredicateUpdateFormula(Predicate predicate, Formula formula) {
	  updateFormulae.put(predicate, new PredicateUpdateFormula(formula, predicate, false));
	}
	
	/** Associates an update formula with a nulary predicate.
	 * @author Alexey Loginov.
	 */
	public void setPredicateUpdateFormula(Predicate predicate, Formula formula, boolean auto) {
	  updateFormulae.put(predicate, new PredicateUpdateFormula(formula, predicate, auto));
	}
	
	/** Associates an update formula with a unary predicate.
	 */
	public void setPredicateUpdateFormula(Predicate predicate, Formula formula, Var v) {
	  updateFormulae.put(predicate, new PredicateUpdateFormula(formula, predicate, v, false));
	}
	
	/** Associates an update formula with a unary predicate.
	 * @author Alexey Loginov.
	 */
	public void setPredicateUpdateFormula(Predicate predicate, Formula formula, Var v, boolean auto) {
	  updateFormulae.put(predicate, new PredicateUpdateFormula(formula, predicate, v, auto));
	}
	
	/** Associates an update formula with a binary predicate.
	 */
	public void setPredicateUpdateFormula(Predicate predicate, Formula formula, Var left, Var right) {
	  updateFormulae.put(predicate, new PredicateUpdateFormula(formula, predicate, left, right, false));
	}
	
	/** Associates an update formula with a binary predicate.
	 * @author Alexey Loginov.
	 */
	public void setPredicateUpdateFormula(Predicate predicate, Formula formula, Var left, Var right, boolean auto) {
	  updateFormulae.put(predicate, new PredicateUpdateFormula(formula, predicate, left, right, auto));
	}
	
	/** Associates an update formula with a predicate.
	 */
	public void setPredicateUpdateFormula(Predicate predicate, Formula formula, 
	    Var[] vars) {
	  updateFormulae.put(predicate, new PredicateUpdateFormula(formula, predicate, vars, false));
	}
	
	/** Associates an update formula with a predicate.
	 * @author Alexey Loginov.
	 */
	public void setPredicateUpdateFormula(Predicate predicate, Formula formula, 
	    Var[] vars, boolean auto) {
	  updateFormulae.put(predicate, new PredicateUpdateFormula(formula, predicate, vars, auto));
	}
	
	/** Associates an update formula with a predicate.
	 */
	public void setPredicateUpdateFormula(Predicate predicate, Formula formula, 
	    List<Var> vars) {
	  updateFormulae.put(predicate, new PredicateUpdateFormula(formula, predicate, vars, false));
	}
	
	/** Associates an update formula with a predicate.
	 * @author Alexey Loginov.
	 */
	public void setPredicateUpdateFormula(Predicate predicate, Formula formula, 
	    List<Var> vars, boolean auto) {
	  updateFormulae.put(predicate, new PredicateUpdateFormula(formula, predicate, vars, auto));
	}
	
	public DynamicVocabulary getUpdatedVocabulary() {
		Set<Predicate> updated = HashSetFactory.make();
		for (PredicateUpdateFormula update : updateFormulae.values()) {
			boolean identity = false;
			Formula formula = update.getFormula();
			Predicate predicate = update.getPredicate();
			if (formula instanceof PredicateFormula && update.variables.length == predicate.arity()) {
				PredicateFormula updateFormula = (PredicateFormula) formula;
				if (updateFormula.predicate() == predicate) {
					identity = true;
					for (int i = 0; i < predicate.arity(); i++) {
						if (updateFormula.getVariable(i) != update.variables[i]) {
							identity = false;
							break;
						}
					}
				}
			}
			if (!identity) {
				updated.add(predicate);
			}
		}
		return DynamicVocabulary.create(updated);
	}
	
	
    /*
     * Let formulae
     */
    
    /** Associates an update formula with a nulary predicate.
     * @author Noam.
     */
    public void setPredicateLetFormula(Predicate predicate, Formula formula, boolean auto) {
      letFormulae.put(predicate, new PredicateUpdateFormula(formula, predicate, auto));
    }

    
    /** Associates an update formula with a unary predicate.
     * @author Noam.
     */
    public void setPredicateLetFormula(Predicate predicate, Formula formula, Var v, boolean auto) {
      letFormulae.put(predicate, new PredicateUpdateFormula(formula, predicate, v, auto));
    }
    
    /** Associates an update formula with a binary predicate.
     * @author Noam.
     */
    public void setPredicateLetFormula(Predicate predicate, Formula formula, Var left, Var right, boolean auto) {
      letFormulae.put(predicate, new PredicateUpdateFormula(formula, predicate, left, right, auto));
    }
    
    /** Associates an update formula with a predicate.
     * @author Noam.
     */
    public void setPredicateLetFormula(Predicate predicate, Formula formula, 
                                       List<Var> vars, boolean auto) {
        letFormulae.put(predicate, new PredicateUpdateFormula(formula, predicate, vars, auto));
    } 
    
	/** Returns the set of messages (strings) that correspond to message
	 * formulae that are potentially satisfied for the specified structure
	 * and partial assignment.
	 */
	public Set<String> reportMessages(TVS structure, Assign assign) {
		assert initialized : "Attempt to report messages on action " + this + ", which has not been initialized!";		
		return reportMessages(structure, assign, getMessages());
	}

	/** Returns the set of messages (strings) that correspond to post message
	 * formulae that are potentially satisfied for the specified structure
	 * and partial assignment.
	 */
	public Set<String> reportPostMessages(TVS structure, Assign assign) {
		assert initialized : "Attempt to report post messages on action " + this + ", which has not been initialized!";
		return reportMessages(structure, assign, getPostMessages());
	}

	protected Set<String> reportMessages(TVS structure, Assign assign,
			Collection<ReportMessage> messageReports) {
		Set<String> answer = new HashSet<String>();
		for (Iterator<ReportMessage> it = messageReports.iterator(); it.hasNext(); ) {
	        ReportMessage report = it.next();
	        if (report.message.startsWith("1:") || report.message.startsWith("0:")) {
	        	Kleene val = report.message.startsWith("1:") ? Kleene.trueKleene : Kleene.falseKleene;
	        	report.reportDefiniteMessage(structure, assign, answer, val);
	        }
	        else {
	        	report.reportMessage(structure, assign, answer);
	        }
		}
		return answer;
	}

    protected Var initFormulaVar(Formula formula, Collection<Var> freeVars) {
		Var aVar = null;
		if (formula != null) {
			Set<Var> vars = HashSetFactory.make(formula.freeVars());
			vars.removeAll(freeVars);
			
			if (vars.size() == 0) {
				aVar = null;
			}
			else if (vars.size() == 1) {
				aVar = (Var) vars.iterator().next();
			}
			else {
				throw new SemanticErrorException("Formula (" + formula + ") must be nullary or unary.");
			}
		}
		return aVar;
	}
	
	public void init() {
		if (initialized)
			return;
		initialized = true;
		// Verify all formula have the right variables.
		Collection precondFree = 
								(precondition == null) ? Collections.EMPTY_LIST : 
														 precondition.freeVars();

		// Check the new formula.
		if (newFormula != null) {
			Set<Var> newVars = HashSetFactory.make(newFormula.freeVars());
			newVars.removeAll(precondFree);
			
			if (newVars.size() == 0) {
				newFormula.newVar = null;
			}
			else if (newVars.size() == 1) {
				newFormula.newVar = (Var) newVars.iterator().next();
			}
			else {
				throw new SemanticErrorException("New formula (" + newFormula + 
					") must be nullary or unary.");
			}
		}

		// Check the clone formula.
		if (cloneFormula != null) {
			Set<Var> cloneVars = HashSetFactory.make(cloneFormula.freeVars());
			cloneVars.removeAll(precondFree);
			if (cloneVars.size() == 1) {
				cloneFormula.var = (Var) cloneVars.iterator().next();
			}
			else {
				throw new SemanticErrorException("Clone formula (" + cloneFormula + 
					") must be unary.");
			}
		}

		// Check the update formulae
		for (Map.Entry<Predicate,PredicateUpdateFormula> entry : updateFormulae.entrySet()) {
			Predicate predicate = entry.getKey();
			PredicateUpdateFormula update = entry.getValue();

            checkUpdate(predicate, update, precondFree);
            
            /*
			Set freeVars = HashSetFactory.make(update.freeVars());
			freeVars.removeAll(precondFree);
			switch (predicate.arity()) {
			case 0:	if (!freeVars.isEmpty()) {
						throw new SemanticErrorException("Nullary update formula " +
							"for " + predicate +
							" should be closed but has " +
							freeVars + " as free variables.");
					}
					break;
			case 1:
					Var v = update.getVariable(0);
					freeVars.remove(v);

					if (!freeVars.isEmpty()) {
						throw new SemanticErrorException("Unary update formula " +
							"for " + predicate +
							" has the following superfluous " +
							"free variables " + freeVars);
					}
					break;
			case 2:
					Var leftVar  = update.getVariable(0);
					Var rightVar = update.getVariable(1);
					
					freeVars.remove(leftVar);
					freeVars.remove(rightVar);

					if (!freeVars.isEmpty()) {
						throw new SemanticErrorException("Binary update formula " +
							"for " + predicate +
							" has the following superfluous " +
							"free variables " + freeVars);
					}
					break;
			default:
					int varNum = update.predicateArity();
					for (int i=0;i<varNum;i++) {
						Var currVar = update.getVariable(i);
						freeVars.remove(currVar);
					}
					if (!freeVars.isEmpty()) {
						throw new SemanticErrorException("Update formula " +
							"for " + predicate +
							" has the following superfluous " +
							"free variables " + freeVars);
					}
					break;
			}*/
            
            }
		
        List<Var> empty = new ArrayList<Var>();
        for (Map.Entry<Predicate,PredicateUpdateFormula> letEntry : letFormulae.entrySet()) {
		  Predicate predicate = letEntry.getKey();
		  PredicateUpdateFormula letUpdate = letEntry.getValue();
		  
		  checkUpdate(predicate, letUpdate, empty);
		}
		
		// check message formulae
		for (ReportMessage reportMessage : messages) {
			Set<Var> freeVars = HashSetFactory.make(reportMessage.formula.freeVars());
			freeVars.removeAll(precondFree);
			if (!freeVars.isEmpty()) {
				throw new SemanticErrorException(
						"In action " + title + 
						": message " + reportMessage
						+ " has superfluous free variables: " + freeVars);
			}
		}
		for (ReportMessage reportMessage : messages) {
			Set<Var> freeVars = HashSetFactory.make(reportMessage.formula.freeVars());
			freeVars.removeAll(precondFree);
			if (!freeVars.isEmpty()) {
				throw new SemanticErrorException(
						"In action " + title + 
						": post message " + reportMessage
						+ " has superfluous free variables: " + freeVars);
			}
		}

		// Check the retain formula
		if (retainFormula != null) {
			Set<Var> retainVars = HashSetFactory.make(retainFormula.freeVars());
			retainVars.removeAll(precondFree);

			if (retainVars.size() != 1) {
				throw new SemanticErrorException("Retain formula (" + 
					retainFormula + ") must be unary.");
			}
			retainFormula.retainVar = (Var) retainVars.iterator().next();
		}
		startVar = initFormulaVar(startFormula,precondFree);
//		waitVar = initFormulaVar(waitFormula,precondFree);
		stopVar = initFormulaVar(stopFormula,precondFree);
	}

	/** Performs sanity checks for update formulae.
	 * 
	 * @param predicate A predicate on the left hand side of an update.
	 * @param update An update formula.
	 * @param precondFree The free variables of the precondition of
	 * the action to which the 
	 */
    protected void checkUpdate(Predicate predicate,
			PredicateUpdateFormula update, Collection<Var> precondFree) {
		try {
			
			for (Var v : update.getCopyOfArguments()) {
				if (precondFree.contains(v)) {
					throw new SemanticErrorException(
							"Update formulae for "
									+ predicate
									+ " uses variable "
									+ v
									+ " which clashes with a variable of the same name "
									+ "in a precondition!");
				}
			}
			
			Set<Var> freeVars = HashSetFactory.make(update.freeVars());
			freeVars.removeAll(precondFree);
			switch (predicate.arity()) {
			case 0:
				if (!freeVars.isEmpty()) {
					throw new SemanticErrorException("Nullary update formula "
							+ "for " + predicate + " should be closed but has "
							+ StringUtils.collectionToList(freeVars)
							+ " as free variables.");
				}
				break;
			case 1:
				Var v = update.getVariable(0);
				
				freeVars.remove(v);
				if (!freeVars.isEmpty()) {
					throw new SemanticErrorException("Unary update formula "
							+ "for " + predicate
							+ " has the following superfluous "
							+ "free variables "
							+ StringUtils.collectionToList(freeVars));
				}
				break;
			case 2:
				Var leftVar = update.getVariable(0);
				Var rightVar = update.getVariable(1);

				freeVars.remove(leftVar);
				freeVars.remove(rightVar);

				if (!freeVars.isEmpty()) {
					throw new SemanticErrorException("Binary update formula "
							+ "for " + predicate
							+ " has the following superfluous "
							+ "free variables "
							+ StringUtils.collectionToList(freeVars));
				}
				break;
			default:
				int varNum = update.predicateArity();
				for (int i = 0; i < varNum; i++) {
					Var currVar = update.getVariable(i);
					freeVars.remove(currVar);
				}
				if (!freeVars.isEmpty()) {
					throw new SemanticErrorException("Update formula " + "for "
							+ predicate + " has the following superfluous "
							+ "free variables "
							+ StringUtils.collectionToList(freeVars));
				}
				break;
			}
		} catch (SemanticErrorException e) {
			e.append("While evaluating action " + toString());
			throw e;
		}
	}
    
	/** Evaluates the effect of the action on the specified structure.
	 * @return The updated structure.
	 * @author Tal Lev-Ami.
	 */
    
    // DHACK 
    //protected static int regCounter = 0;
    //protected static int formulaEvalCounter = 0;

	public HighLevelTVS evaluate(HighLevelTVS structure, Assign assign) {
		init();
		
//		Collection newThreadNodes = null;
		HighLevelTVS newStructure = (HighLevelTVS) structure.copy();

        // DHACK 
        //TVLAAPITrace.tracePrintln("Regular Action.evaluate counter = " + ++regCounter);
        
		if (newFormula != null)
			newStructure.applyNewUpdateFormula(newFormula, assign);
		
		if (cloneFormula != null)
			newStructure.applyCloneUpdateFormula(cloneFormula, assign);

		//////////////////////////////
		// process new thread creation
		if (threadType != null) {
			NewUpdateFormula dummyNullaryFormula = new NewUpdateFormula(null);
			Collection newNodes = newStructure.applyNewUpdateFormula(dummyNullaryFormula, assign);
			Predicate entryLocationPredicate = threadType.getEntryLocationPredicate();
			
			if (!newNodes.isEmpty()) {
				ModifiedPredicates.modify(newStructure, Vocabulary.active);
				ModifiedPredicates.modify(newStructure, Vocabulary.isThread);
				ModifiedPredicates.modify(newStructure, entryLocationPredicate);
			}
			
			for (Iterator nodeIter = newNodes.iterator(); nodeIter.hasNext(); ) {
				Node newThreadNode = (Node) nodeIter.next();			
				newStructure.update(Vocabulary.isThread, newThreadNode, Kleene.trueKleene);
				newStructure.update(entryLocationPredicate, newThreadNode, Kleene.trueKleene);
				//ModifiedPredicates.modify(Vocabulary.isThread);
				//ModifiedPredicates.modify(entryLocationPredicate);
			}
		}
		
		newStructure.updatePredicates(updateFormulae.values(), assign);

		if (retainFormula != null)
			newStructure.applyRetainUpdateFormula(retainFormula, assign, structure);

		// start threads
		// note: this takes place after the "unsched" formula was applied
		// so we must set "runnable" as well (only if "unsched" took place).
		if (startFormula != null) {
			Collection<Node> toChange = new ArrayList<Node>();
			Collection<Node> maybeActive = new ArrayList<Node>();
			
			for (Iterator i = structure.evalFormula(startFormula,assign); 
				 i.hasNext(); ) {
				AssignKleene anAssign = (AssignKleene) i.next();
				Node node  = (Node) anAssign.get(startVar);		
				if (structure.eval(Vocabulary.isThread, node).equals(Kleene.falseKleene))
					throw new RuntimeException("can not start a non-thread node");
				
				if (anAssign.kleene == Kleene.unknownKleene)
					maybeActive.add(node);
				else
					toChange.add(node);
			}
			
			if (!toChange.isEmpty()) {
				ModifiedPredicates.modify(newStructure, Vocabulary.ready);
				if (performUnschedule) {
					ModifiedPredicates.modify(newStructure, Vocabulary.runnable);
				}
			}
			if (!maybeActive.isEmpty()) {
				ModifiedPredicates.modify(newStructure, Vocabulary.active);
			}
			
			for (Iterator<Node> i = toChange.iterator(); i.hasNext(); ) {
				Node node = i.next();
				newStructure.update(Vocabulary.ready, node, Kleene.trueKleene);
				if (performUnschedule)
					newStructure.update(Vocabulary.runnable, node, Kleene.unknownKleene);
			}
			for (Iterator<Node> i = maybeActive.iterator(); i.hasNext(); ) {
				Node node = i.next();
				newStructure.update( Vocabulary.active, node, Kleene.unknownKleene);
			}
		}
		
		// stop threads
		// note: this takes place after the "unschedule" formula was applied
		// so we must set "runnable" as well (only if "unsched" took place). 
		if (stopFormula != null) {
			Collection<Node> toChange = new ArrayList<Node>();
			Collection<Node> maybeActive = new ArrayList<Node>();
			
			for (Iterator i = structure.evalFormula(stopFormula, assign); 
				 i.hasNext(); ) {
				AssignKleene anAssign = (AssignKleene) i.next();
				Node node  = (Node) anAssign.get(stopVar);		
				
				if (structure.eval(Vocabulary.isThread, node).equals(Kleene.falseKleene))
					throw new RuntimeException("cannot stop a non-thread node");
				
				if (anAssign.kleene == Kleene.unknownKleene)
					maybeActive.add(node);
				else
					toChange.add(node);
			}

			if (!toChange.isEmpty()) {
				ModifiedPredicates.modify(newStructure, Vocabulary.ready);
				if (performUnschedule) {
					ModifiedPredicates.modify(newStructure, Vocabulary.runnable);
				}
			}
			if (!maybeActive.isEmpty()) {
				ModifiedPredicates.modify(newStructure, Vocabulary.active);
			}

			for (Iterator<Node> i = toChange.iterator(); i.hasNext(); ) {
				Node node = i.next();
				newStructure.update(Vocabulary.ready, node, Kleene.falseKleene);
				if (performUnschedule)
					newStructure.update(Vocabulary.runnable, node, Kleene.falseKleene);
			}
			for (Iterator<Node> i = maybeActive.iterator(); i.hasNext(); ) {
				Node node = i.next();
				newStructure.update(Vocabulary.active, node, Kleene.unknownKleene);
			}
		}

		// Do cleanup to reset the values of the temporary predicates isNew and instance.
		if (newFormula != null || cloneFormula != null || threadType != null) {
			newStructure.clearPredicate(Vocabulary.isNew);
			newStructure.clearPredicate(Vocabulary.instance);
			ModifiedPredicates.modify(Vocabulary.isNew);
			ModifiedPredicates.modify(Vocabulary.instance);
		}
		
		return newStructure;
	}

    /**
     * Checks if the action has any let predicates
     */
    public boolean hasLet() {
      return ! letFormulae.isEmpty();
    }
    
    /** Evaluates values of the let predicate and stores them in the structure
     * @return A copy of the structure + let values.
     * @author maon
     */
    
    // DHACK 
   // protected static int letCounter = 0;

    public HighLevelTVS evaluateLet(HighLevelTVS structure, Assign assign) {
        init();
        HighLevelTVS newStructure = (HighLevelTVS) structure.copy();

        // DHACK 
        //TVLAAPITrace.tracePrintln("Let Action.evaluate counter = " + ++letCounter);
    
        newStructure.updatePredicates(letFormulae.values(), assign);

        
        return newStructure;
    }
    
    /**
     * Clears th values of the let predicates
     */
    public void clearLet(HighLevelTVS structure) {
        for (Iterator<Predicate> letPredItr = letFormulae.keySet().iterator(); letPredItr.hasNext(); ) {
          Predicate predicate = letPredItr.next();
          	structure.modify(predicate);
            structure.clearPredicate(predicate);
        }
    }
    
    public Collection<ReportMessage> getMessages() {
        return messages;
    }

    public Collection<ReportMessage> getPostMessages() {
        return postMessages;
    }    

    public void composeFormula(Formula composeFormula) {
        this.composeFormula = composeFormula;
    }
    
    public Formula getComposeFormula() {
        return this.composeFormula;
    }

    public void decomposeFormula(Formula decomposeFormula) {
        this.decomposeFormula = decomposeFormula;
    }

    public Formula getDecomposeFormula() {
        return this.decomposeFormula;
    }

    public void checkedMessage(ReportMessage message) {
        checkedMessages.add(message);
    }

    public void candidateMessage(ReportMessage message) {
        candidateMessages.add(message);
    }

    public boolean checkedAllMessages() {
        return checkedMessages.equals(candidateMessages);
    }

	public void framePre(Formula framePre) {
		this.framePre = framePre;
	}

	public void frame(Formula frame) {
		this.frame = frame;
	}

	public Formula getFramePre() {
		return framePre;
	}
	
	public Formula getFrame() {
		return frame;
	}

    public boolean isUniverseChanging() {
        return newFormula != null || retainFormula != null || cloneFormula != null;
    }
}
