package tvla.language.BUC;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import tvla.analysis.multithreading.buchi.BuchiAutomaton;
import tvla.analysis.multithreading.buchi.BuchiState;
import tvla.language.TVM.DeclarationsAST;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashMapFactory;

public class BuchiAutomatonAST {
	protected String name;
	protected List actions;
	protected DeclarationsAST declarations;
	protected BuchiStateAST initial;
	
	// map of stateName->BuchiStateAST
	protected Map states;
	protected BuchiAutomaton automaton = null;
	
	public BuchiAutomatonAST(String id,BuchiStateAST init, List actions) {
		this.name = id;
		this.actions = actions;
		this.initial = init;
		
		states = HashMapFactory.make();
		
	}
	
	public void declarations(DeclarationsAST decl) {
		System.err.println("Got declarations " + decl);
		declarations = decl;
	}

	/**
	 * create state-predicates for states of the Buchi automaton
	 * create the corresponding BuchiAutomaton object (with BuchiStates and BuchiTransitions)
	 */
	public void compile() {
		
		System.err.println("BuchiAutomatonAST:compile()");
		
		// generate required declarations
		declarations.generate();
		
		// create the transitions of the Buchi automaton
		for (Iterator ai=actions.iterator(); ai.hasNext(); ) {
			BuchiTransitionAST curr = (BuchiTransitionAST)ai.next();
			
			BuchiState source = automaton.getState(curr.source().name);
			BuchiState target = automaton.getState(curr.target().name);
			Predicate transitionPredicate = curr.label().getPredicate();
				
			automaton.addTransition(source,target,transitionPredicate);
		}
	
		System.err.println("Ended Compilation of Buchi Automaton");
		automaton.dump();
		
	}
	public BuchiAutomaton getAutomaton() {
		return automaton;
	}

	public void generate() {
		
		System.err.println("BuchiAutomatonAST:generate()");
		
		for (Iterator i=actions.iterator(); i.hasNext();) {
			BuchiTransitionAST currTransition = (BuchiTransitionAST)i.next();
			BuchiStateAST source = currTransition.source();
			BuchiStateAST target = currTransition.target();
			
			if (!states.containsKey(source.name))
				states.put(source.name,source);
	
			if (!states.containsKey(target.name))
				states.put(target.name,target);
			
		}
		
		// create a buchi automaton
		automaton = new BuchiAutomaton();
		
		// create the states of the Buchi automaton
		
		for (Iterator i=states.keySet().iterator(); i.hasNext();) {
				String currName = (String)i.next();
				Predicate currPred = Vocabulary.createPredicate(currName, 0);
				System.err.println("State predicate " + currName + " is " + currPred);
				BuchiState currState = 
					new BuchiState(currName,
							((BuchiStateAST)states.get(currName)).isAccepting,
							currPred
							);
				automaton.addState(currState);
		}
		// set the initial state
		automaton.setInitial(initial.name);
	}	
}
