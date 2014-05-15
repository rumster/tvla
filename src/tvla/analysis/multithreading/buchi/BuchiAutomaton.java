package tvla.analysis.multithreading.buchi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.predicates.Predicate;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;

/** Implements a Buchi automaton
 * @author Eran Yahav.
 */
public class BuchiAutomaton {
	/** Map of (name,BuchiState) */
	private Map states;
	/** Set of BuchiTransitions */
	private Set transitions;
	/** Map of (state-predicate,list of transitions) */
	private Map stateTransitions;
	/** Set of accepting state-predicates */
	private Set acceptingStatePredicates;

	/** initial state */
	private BuchiState initial;

	/**
	 * Create a new Buchi autoamton
	 */
	public BuchiAutomaton() {
		states = HashMapFactory.make();
		transitions = HashSetFactory.make();
		stateTransitions = HashMapFactory.make();
		acceptingStatePredicates = HashSetFactory.make();
	}

	/**
	 * add a state to the Buchi automaton
	 * @param st - state to be added
	 * when a state has a name that already exist in the automaton, the new state
	 * overrides the old state (and the old state is removed from the automaton)
	 */
	public void addState(BuchiState st) {
		states.put(st.name(), st);
		if (st.isAccepting()) {
			acceptingStatePredicates.add(st.predicate());
		}
	}
	/**
	 * set the inintial state of the buchi auatomaton
	 * @param stateName - name of state to be set as initial
	 * assumes that state already exists in the automaton
	 */
	public void setInitial(String stateName) {
		initial = getState(stateName);
		assert (initial != null);
	}

	/**
	 * get the name of the initial state
	 * @return name of initial state as a string
	 */
	public String initialState() {
		return initial.name();
	}

	/**
	 * get a state for a given state name
	 * @param name - name of state
	 * @return state of the automaton with the corresponding name
	 */
	public BuchiState getState(String name) {
		if (states.containsKey(name)) {
			return (BuchiState) states.get(name);
		} else {
			throw new RuntimeException("Buchi state " + name + " not found!");
		}
	}

	/**
	 * add a transition to the automaton.
	 * assumes states are already added to the automaton.
	 * @param src - source state of the transition
	 * @param tgt - target state of the transition
	 * @param label - predicate for transition label
	 */
	public void addTransition(
		final BuchiState src,
		final BuchiState tgt,
		final Predicate label) {
		BuchiTransition aTransition = new BuchiTransition(src, tgt, label);
		transitions.add(aTransition);
		if (!stateTransitions.containsKey(src.predicate())) {
			List newList = new ArrayList();
			newList.add(aTransition);
			stateTransitions.put(src.predicate(), newList);
		} else {
			((List) stateTransitions.get(src.predicate())).add(aTransition);
		}
	}

	/**
	 * returns an iterator iterating over all state predicates
	 * @return iterator over state predicates
	 */
	public Iterator allStatePredicates() {
		return stateTransitions.keySet().iterator();
	}

	/**
	 * returns an iterator iterating over aceepting state predicates
	 * @return iterator over accepting state predicates
	 */
	public Iterator allAcceptingStatePredicates() {
		return acceptingStatePredicates.iterator();
	}

	/**
	 * returns an iterator iterating over automaton states
	 * @return iterator iterating over automaton states
	 */
	public Iterator allStates() {
		return states.values().iterator();
	}
	/**
	 * return a list of transitions associated with the given state predicate.
	 * If no transitions exists, return a new empty list.
	 * @param statePredicate - source state predicate
	 * @return list of transitions associated with the given state predicate.
	 */
	public List transitions(Predicate statePredicate) {
		if (!stateTransitions.containsKey(statePredicate)) {
			return new ArrayList();
		} else {
			return (List) stateTransitions.get(statePredicate);
		}
	}

	/**
	 * dump automaton for debug purposes
	 */
	public void dump() {
		System.err.println("Buchi Automaton States");
		for (Iterator i = allStates(); i.hasNext();) {
			BuchiState curr = (BuchiState) i.next();
			System.err.println(curr.toString());
		}
		System.err.println("Buchi Automaton Transitions");
		for (Iterator t = transitions.iterator(); t.hasNext();) {
			BuchiTransition curr = (BuchiTransition) t.next();
			System.err.println(curr.toString());
		}
	}
}
