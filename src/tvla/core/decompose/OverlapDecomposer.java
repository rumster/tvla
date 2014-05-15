package tvla.core.decompose;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.analysis.AnalysisStatus;
import tvla.core.Framer;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVSFactory;
import tvla.core.TVSSet;
import tvla.core.common.ModifiedPredicates;
import tvla.differencing.FormulaDifferencing;
import tvla.differencing.FormulaDifferencing.Delta;
import tvla.exceptions.SemanticErrorException;
import tvla.formulae.AndFormula;
import tvla.formulae.Formula;
import tvla.formulae.NotFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.PredicateUpdateFormula;
import tvla.formulae.ValueFormula;
import tvla.logic.Kleene;
import tvla.predicates.DynamicVocabulary;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.transitionSystem.Action;
import tvla.util.Apply;
import tvla.util.ApplyIterable;
import tvla.util.ComposeIterator;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.MapInverter;
import tvla.util.Pair;
import tvla.util.ProgramProperties;
import tvla.util.Timer;

/**
 * Decomposer by keeping for each decomposition predicates the set of nodes for
 * which it holds
 */
public class OverlapDecomposer extends Decomposer {

	protected static final Set<Predicate> EMPTY_SET = Collections.emptySet();

	protected static final PClauseName EMPTY_DECOMP_NAME = PClauseName.create(
			DynamicVocabulary.empty(), DynamicVocabulary.full(), "{}", false,
			null);

	/**
	 * Set of decomposition names, each decomposition name is a set of
	 * predicates representing a disjunction of decomposition predicates
	 */
	protected final Set<PClauseName> criteria = HashSetFactory.make();

    /**
     * allow unknown decomposition predicates
     */
    public static final boolean allowUnknown = ProgramProperties
            .getBooleanProperty("tvla.decompose.allowUnknown", false);

	protected static final boolean cache = ProgramProperties
			.getBooleanProperty("tvla.decompose.overlap.cache", true);

	/**
	 * Should the complement of all the decomposition names be another
	 * decomposition name?
	 */
	protected static final boolean complement = ProgramProperties
			.getBooleanProperty("tvla.decompose.overlap.complement", false);

	/**
	 * For each decomposition name, the nullary predicate used to mark it on the
	 * structures
	 */
	protected final Map<PClauseName, Predicate> nameToMark = HashMapFactory
			.make();

	protected static final boolean coerceBeforeCompose = ProgramProperties
			.getBooleanProperty("tvla.decompose.coerceBeforeCompose", true);

	/**
	 * Decomposer by keeping for each decomposition predicates the set of nodes
	 * for which it holds
	 */
	public OverlapDecomposer() {
	}

	/**
	 * Perform the decomposition
	 * 
	 * @param structure
	 *            The structure to decompose
	 * @return For each decomposition name, the resulting TVSSet
	 */
	public CartesianElement decompose(HighLevelTVS structure) {
		CartesianElement result = new CartesianElement();
		DecompositionName name = getDecompositionName(structure);
		if (name != null && names().contains(name)) {
			structure.updateVocabulary(getVocabulary(name));
			result.join(name, structure);
			return result;
		}

		// Nodes that have no decomposition predicate hold
		Set<Node> noDecompPredicate = HashSetFactory.make(structure.nodes());

		// For each decomposition name
		for (PClauseName decompName : criteria) {
			decompose(structure, decompName, noDecompPredicate, result);
		}
		if (complement) {
			// Build substructure for the empty decomposition name and add it to
			// the result
			buildSubStructure(structure, noDecompPredicate, EMPTY_DECOMP_NAME,
					result);
		}

		return result;
	}

	private DecompositionName getDecompositionName(HighLevelTVS structure) {
		Collection<PClauseName> names = new ArrayList<PClauseName>();
		for (Map.Entry<PClauseName, Predicate> entry : nameToMark.entrySet()) {
			Predicate mark = entry.getValue();
			if (structure.getVocabulary().contains(mark)
					&& structure.eval(mark).equals(Kleene.trueKleene)) {
				names.add(entry.getKey());
			}
		}
		return compose(names);
	}

	/**
	 * Decompose a structure according to the given decomposition name
	 * 
	 * @param structure
	 *            The structure to decompose
	 * @param decompName
	 *            The name according to which the structure is decomposed
	 * @param noDecompPredicate
	 *            If not null, remove all nodes matching the decomposition name
	 *            from this set
	 * @param result
	 *            Where the result should be stored
	 */
	protected void decompose(HighLevelTVS structure, PClauseName decompName,
			Set<Node> noDecompPredicate, CartesianElement result) {
        Timer timer = Timer.getTimer("Low", "Decompose");
        try {
            timer.start();
    		assert decompName.canDecompose(structure, false) : "Decomposition predicates must be all definite!";
    		Set<Node> universe = HashSetFactory.make();
    		// Find all nodes that have this decomposition name
    		for (Predicate predicate : decompName.getDisjuncts().all()) {
    			Iterator<Map.Entry<NodeTuple, Kleene>> iterator = structure
    					.predicateSatisfyingNodeTuples(predicate, EMPTY_NODE_ARRAY,
    							allowUnknown ? null : Kleene.trueKleene);
    			while (iterator.hasNext()) {
    				Node node = (Node) iterator.next().getKey();
    				if (noDecompPredicate != null)
    					noDecompPredicate.remove(node);
    				universe.add(node);
    			}
    		}
    		// Build a substructure for this decomposition name and add it to the
    		// result
    		buildSubStructure(structure, universe, decompName, result);
        } finally {
            timer.stop();
        }
	}

	/**
	 * Decompose the given structure using the given name
	 */
	@Override
	public CartesianElement decompose(HighLevelTVS structure,
			DecompositionName name) {
		CartesianElement result = new CartesianElement();
		assert name instanceof PClauseName;
		decompose(structure, (PClauseName) name, null, result);
		return result;
	}

	/**
	 * Build a substructure for the given decomposition name with the given
	 * universe and add it to the appropriate bucket in the result.
	 * 
	 * @param structure
	 *            The structure to decompose
	 * @param universe
	 *            The universe of the decomposition name
	 * @param decompName
	 *            The decomposition name
	 * @param result
	 *            The result
	 */
	protected void buildSubStructure(HighLevelTVS structure,
			Set<Node> universe, PClauseName decompName, CartesianElement result) {
		HighLevelTVS substruct = structure.copy();

		DynamicVocabulary newVoc = getVocabulary(decompName);
		mark(substruct, decompName);
		if (explicitOutside) {
			addOutsideNode(universe, substruct);
		} else {
			project(substruct, universe);
		}
        substruct.updateVocabulary(newVoc);
		result.join(decompName, substruct);
	}

	@Override
	public Set<DecompositionName> permute(DecompositionName target) {
		PClauseName targetName = (PClauseName) target;
		// Permute
		Set<DecompositionName> premuted = HashSetFactory.make();
		premuted.add(targetName);

		Set<String> psetMembers = getDNParameters(targetName);
		if (!psetMembers.isEmpty()) {
			// Find matching decomposition name
			NAME: for (PClauseName name : criteria) {
				// Can't check size when we get an abstracted version
				if (!targetName.isAbstraction()
						&& name.getDisjuncts().size() != targetName
								.getDisjuncts().size()) {
					continue;
				}
				Set<String> otherMembers = getDNParameters(name);
				if (otherMembers.equals(psetMembers))
					continue;

				// Compare dnames under mapping
				for (Map<Predicate, Predicate> mapping : createMappings(
						psetMembers, otherMembers)) {
					for (Predicate predicate : targetName.getDisjuncts().all()) {
						if (mapping.containsKey(predicate)) {
							predicate = mapping.get(predicate);
						}
						if (!name.getDisjuncts().contains(predicate)) {
							continue NAME;
						}
					}
					if (targetName.isAbstraction()) {
						DynamicVocabulary permutedAbstractedVoc = targetName
								.getDisjuncts().permute(mapping);
						Set<DecompositionName> base = HashSetFactory.make();
						base.add(name);
						premuted.add(PClauseName.create(permutedAbstractedVoc,
								name.getKillVocabulary(), name.toString()
										+ "__", true, base));
					} else {
						premuted.add(name);
					}
				}
			}
		}

		return premuted;
	}

	private static void permute(String[] a, int n, Collection<String[]> result) {
		if (n == 1) {
			result.add(a.clone());
			return;
		}
		for (int i = 0; i < n; i++) {
			swap(a, i, n - 1);
			permute(a, n - 1, result);
			swap(a, i, n - 1);
		}
	}

	// swap the characters at indices i and j
	private static void swap(String[] a, int i, int j) {
		String c;
		c = a[i];
		a[i] = a[j];
		a[j] = c;
	}

	private Iterable<Map<Predicate, Predicate>> createMappings(
			Set<String> fromMembers, Set<String> toMembers) {
		if (fromMembers.size() != toMembers.size()) {
			return Collections.emptySet();
		}
		if (fromMembers.isEmpty()) {
			Map<Predicate, Predicate> mapping = HashMapFactory.make();
			return Collections.singleton(mapping);
		}
		Collection<Map<Predicate, Predicate>> result = new ArrayList<Map<Predicate, Predicate>>();
		// Find all bijections between fromMembers and toMembers
		String[] from = fromMembers.toArray(new String[fromMembers.size()]);
		String[] to = toMembers.toArray(new String[toMembers.size()]);
		Collection<String[]> toPerms = new ArrayList<String[]>();
		permute(to, to.length, toPerms);
		for (String[] toPerm : toPerms) {
			Map<Predicate, Predicate> mapping = HashMapFactory.make();
			for (int i = 0; i < from.length; i++) {
				mapping.putAll(createMapping(from[i], toPerm[i]));
			}
			result.add(mapping);
		}

		return result;
	}

	private Map<Predicate, Predicate> createMapping(String psetMember,
			String otherMember) {
		// Correlated lists
		Iterator<Predicate> myIter = ParametricSet.getPredicates(psetMember)
				.iterator();
		Iterator<Predicate> otherIter = ParametricSet
				.getPredicates(otherMember).iterator();
		Map<Predicate, Predicate> mapping = HashMapFactory.make();
		while (myIter.hasNext() && otherIter.hasNext()) {
			Predicate myPredicate = myIter.next();
			Predicate otherPredicate = otherIter.next();
			mapping.put(myPredicate, otherPredicate);
		}
		assert !myIter.hasNext() && !otherIter.hasNext();
		// Complete into a permutation by closing open cycles
		Map<Predicate, Predicate> invMapping = HashMapFactory.make();
		MapInverter.invertMap(mapping, invMapping);
		Map<Predicate, Predicate> completion = HashMapFactory.make();
		for (Map.Entry<Predicate, Predicate> entry : mapping.entrySet()) {
			Predicate from = entry.getKey();
			Predicate to = entry.getValue();
			if (!mapping.containsKey(to)) {
				while (invMapping.containsKey(from)) {
					from = invMapping.get(from);
				}
				completion.put(to, from);
			}
		}
		mapping.putAll(completion);

		return mapping;
	}

	@Override
	public Collection<Pair<DecompositionName, TVSSet>> permute(
			Collection<? extends DecompositionName> sourceComponents,
			TVSSet structures) {
		DecompositionName source = compose(sourceComponents);
		Set<String> psetMembers = getDNParameters(source);
		if (psetMembers.isEmpty()) {
			return Collections.singleton(Pair.create(source, structures));
		}
		Collection<Pair<DecompositionName, TVSSet>> result = new ArrayList<Pair<DecompositionName, TVSSet>>();

		// Find all possible targets
		Collection<Iterable<DecompositionName>> permuted = new ArrayList<Iterable<DecompositionName>>();
		for (DecompositionName name : sourceComponents) {
			permuted.add(permute(name));
		}
		Collection<DecompositionName> targets = HashSetFactory.make();
		for (Iterator<List<DecompositionName>> iterator = new ComposeIterator<DecompositionName>(
				permuted); iterator.hasNext();) {
			List<DecompositionName> targetComponents = iterator.next();
			targets.add(compose(targetComponents));
		}
		// For each target find all possible mappings
		for (DecompositionName target : targets) {
			for (Map<Predicate, Predicate> mapping : createMappings(
					psetMembers, getDNParameters(target))) {
				TVSSet targetStructures = apply(mapping, structures);
				result.add(Pair.create(target, targetStructures));
			}
		}
		return result;
	}

	@Override
	public Iterable<TVSSet> permute(DecompositionName target,
			DecompositionName source, TVSSet structures) {
		Collection<TVSSet> result = new ArrayList<TVSSet>();
		for (Map<Predicate, Predicate> mapping : getPermutation(target, source)) {
			TVSSet targetStructures = apply(mapping, structures);
			result.add(targetStructures);
		}
		return result;
	}

    @Override
    public Iterable<Map<Predicate, Predicate>> getPermutation(DecompositionName target, DecompositionName source) {
        return createMappings(
				getDNParameters(source), getDNParameters(target));
    }

	private TVSSet apply(Map<Predicate, Predicate> mapping, TVSSet structures) {
		TVSSet result = TVSFactory.getInstance().makeEmptySet(TVSFactory.JOIN_CONCRETE);
		for (HighLevelTVS structure : structures) {
			HighLevelTVS mapped = structure.permute(mapping);
			result.mergeWith(mapped);
		}
		return result;
	}

	/*
	 * @Override public TVSSet permute(DecompositionName decompName,
	 * HighLevelTVS structure) { // Permute TVSSet premuted = new
	 * ConcreteTVSSet(); premuted.add(structure);
	 * 
	 * String psetMember = getDNParameter(decompName); if (psetMember != null) {
	 * String pset = ParametricSet.getPSet(psetMember); SET: for (String
	 * otherMember : ParametricSet.getMembers(pset)) { if
	 * (otherMember.equals(psetMember)) continue;
	 * 
	 * HighLevelTVS premute = structure.copy(); // Correlated lists
	 * Iterator<Predicate> myIter =
	 * ParametricSet.getPredicates(psetMember).iterator(); Iterator<Predicate>
	 * otherIter = ParametricSet.getPredicates(otherMember).iterator(); while
	 * (myIter.hasNext() && otherIter.hasNext()) { Predicate myPredicate =
	 * myIter.next(); Predicate otherPredicate = otherIter.next(); if
	 * (!structure.getVocabulary().all().contains(otherPredicate)) { continue
	 * SET; } premute.premute(myPredicate, otherPredicate); } assert
	 * !myIter.hasNext() && !otherIter.hasNext(); premuted.add(premute); } }
	 * 
	 * return premuted; }
	 */
	private Set<String> getDNParameters(DecompositionName decompName) {
		Set<String> psetMembers = HashSetFactory.make();
		for (Predicate predicate : getVocabulary(decompName).unary()) {
			String pset = ParametricSet.getPSetMember(predicate);
			if (pset != null) {
				psetMembers.add(pset);
			}
		}
		return psetMembers;
	}

	protected DynamicVocabulary adjustVocabularyToKill(
			DynamicVocabulary vocabulary, DecompositionName name) {
		return vocabulary.subtract(((PClauseName) name).getDisjuncts());
	}

	public static int soft = 0;

	public static int total = 0;

	public static int hit = 0;

	@SuppressWarnings("unchecked")
	protected static <T> T deref(Reference<?> ref) {
		return (T) (ref == null ? null : ref.get());
	}

	@Override
	public HighLevelTVS prepareForAction(HighLevelTVS structure,
			DecompositionName name, DynamicVocabulary extraVoc) {
		DynamicVocabulary newVoc = getVocabulary(name).union(extraVoc);

		HighLevelTVS prepared = null;
		if (cache) {
			Reference<?> ref = (Reference<?>) structure.getStoredReference();
			prepared = deref(ref);
			total++;
			if (prepared != null && prepared.getVocabulary() == newVoc) {
				hit++;
				return prepared;
			}
			if (ref != null && prepared == null) {
				soft++;
			}
		}
		prepared = super.prepareForAction(structure, name, extraVoc);
		safeUpdateVocabulary(newVoc, prepared);

		Node outside = getOutsideNode(prepared);
		if (getVocabulary(name) != DynamicVocabulary.full()
				|| (outside != null && (!isOutsideUnaryPrecise() || !isOutsideBinaryPrecise()))) {
            Timer timer = Timer.getTimer("Coerce", "prepareForAction");
            timer.start();
			boolean valid = prepared.coerce();
			timer.stop();
			if (!valid) {
				AnalysisStatus.getActiveStatus().numberOfComposeConstraintBreaches++;
				return null;
			}
		}
		if (cache) {
			structure.setStoredReference(new SoftReference<HighLevelTVS>(
					prepared));
		}
		return prepared;
	}

	/**
	 * Prepare the given TVSSet to be composed s.t. it's new name will be
	 * newName
	 * 
	 * @return The prepared set
	 */
	public Iterable<HighLevelTVS> prepareForComposition(Iterable<HighLevelTVS> set, final DecompositionName newName, 
			DynamicVocabulary newVoc) {
		if (!coerceBeforeCompose) {
			return super.prepareForComposition(set, newName, newVoc);
		}
		// Set outside predicate to be compatible
		// Can't use original structure before of frame!
		Iterable<HighLevelTVS> result = changeOutside(set, Kleene.trueKleene, Kleene.unknownKleene, false);
		final DynamicVocabulary voc = newVoc == null ? getVocabulary(newName) : newVoc; 
		return new ApplyIterable<HighLevelTVS>(result, new Apply<HighLevelTVS>() {
        
            public HighLevelTVS apply(HighLevelTVS structure) {
                mark(structure, newName);
                safeUpdateVocabulary(voc, structure);
                Timer timer = Timer.getTimer("Coerce", "prepareForComposition");
                timer.start();
                boolean valid = structure.coerce();
                timer.stop();
                if (!valid) {
                    AnalysisStatus.getActiveStatus().numberOfComposeConstraintBreaches++;
                    return null;
                }
                return structure;
            }
        });
	}

	public DynamicVocabulary getVocabulary(DecompositionName name) {
		PClauseName fullName = (PClauseName) name;
		DynamicVocabulary newVoc = DynamicVocabulary.full().
		    subtract(Framer.markPredicatesVoc).
		    subtract(fullName.getKillVocabulary());
		return newVoc;
	}

	/**
	 * Mark the given structure using the given name
	 * 
	 * @param structure
	 * @param decompName
	 */
	@Override
	protected void mark(HighLevelTVS structure, DecompositionName decompName) {
		assert decompName instanceof PClauseName;
		for (PClauseName name : criteria) {
			boolean shouldMark = decompName.contains(name);
			structure.update(getMark(name), shouldMark ? Kleene.trueKleene
					: Kleene.falseKleene);
		}
	}

	/**
	 * Get the mark for the given name
	 */
	@Override
	public Predicate getMark(DecompositionName name) {
		assert name instanceof PClauseName;
		return nameToMark.get(name);
	}

	/**
	 * Return the set of decomposition names for which the given predicate holds
	 * (or doesn't hold if negated is true)
	 */
	@Override
	public Set<DecompositionName> match(Predicate predicate, boolean negated,
			DynamicVocabulary actionVoc, DynamicVocabulary actionKillVoc) {
		Set<DecompositionName> result = HashSetFactory.make();
		for (PClauseName name : criteria) {
			assert !negated;
			if (name.getDisjuncts().contains(predicate)) {
				result.add(name);
			}
		}
		if (actionVoc != null) {
			if (result.size() != 1) {
				throw new SemanticErrorException(
						"When working with actionVoc, the predicate must have a single match. However, "
								+ predicate
								+ " has "
								+ result.size()
								+ " matches");
			}
			PClauseName original = (PClauseName) result.iterator().next();
			Set<DecompositionName> base = HashSetFactory.make();
			base.add(original);
			PClauseName actionName = PClauseName.create(actionVoc, original
					.getKillVocabulary().union(actionKillVoc), original
					.toString()
					+ "__", true, base);
			result.clear();
			result.add(actionName);
		}
		return result;
	}

	/**
	 * Return the decomposition names of this decomposer
	 */
	@Override
	public Set<? extends DecompositionName> names() {
		return Collections.unmodifiableSet(this.criteria);
	}

	public static Set<Predicate> getDecompositionPredicates(Formula formula) {
		Set<Predicate> decompName = HashSetFactory.make();
        if (formula instanceof ValueFormula) {
            return decompName;
        }
		Collection<Formula> disjuncts = new ArrayList<Formula>();		
		Formula.getOrs(formula, disjuncts);
		for (Formula disjunct : disjuncts) {
			if (!(disjunct instanceof PredicateFormula)) {
				throw new SemanticErrorException("Decomposition formula "
						+ formula + " name must be disjunction of predicates");
			}
			PredicateFormula pformula = (PredicateFormula) disjunct;
			Predicate predicate = pformula.predicate();
			if (predicate.arity() != 1) {
				throw new SemanticErrorException("Decomposition predicate "
						+ predicate + " is not unary");
			}
			if (!predicate.abstraction()) {
				throw new SemanticErrorException("Decomposition predicate "
						+ predicate + " is not an abstraction predicate");
			}
			decompName.add(predicate);
		}
		return decompName;
	}

	public static Set<Predicate> getKillPredicates(Formula killFormula) {
		Set<Predicate> kill = HashSetFactory.make();
		if (killFormula != null) {
			Collection<Formula> conjuncts = new ArrayList<Formula>();
			Formula.getAnds(killFormula, conjuncts);
			for (Formula conjunct : conjuncts) {
				if (conjunct instanceof ValueFormula) {
					ValueFormula valueFormula = (ValueFormula) conjunct;
					if (valueFormula.value() == Kleene.trueKleene) {
						continue;
					} else {
						throw new SemanticErrorException(
								"Unsupported kill predicate " + conjunct);
					}
				}
				if (!(conjunct instanceof NotFormula)) {
					throw new SemanticErrorException("Kill formula "
							+ killFormula
							+ " must contain only negation of predicates");
				}
				conjunct = ((NotFormula) conjunct).subFormula();
				if (!(conjunct instanceof PredicateFormula)) {
					throw new SemanticErrorException("Kill formula "
							+ killFormula
							+ " must contain only negation of predicates");
				}
				PredicateFormula pformula = (PredicateFormula) conjunct;
				Predicate predicate = pformula.predicate();
				kill.add(predicate);
			}
		}
		return kill;
	}

	public static Pair<DynamicVocabulary, DynamicVocabulary> getDecompositionAndKillPredicates(
			Formula formula) {
		Formula origFormula = formula;
		Formula killFormula = null;
		if (formula instanceof AndFormula) {
			AndFormula andFormula = (AndFormula) formula;
			formula = andFormula.left();
			killFormula = andFormula.right();
		}
		Set<Predicate> decompName = getDecompositionPredicates(formula);
		Set<Predicate> kill = getKillPredicates(killFormula);
		Set<Predicate> both = HashSetFactory.make(kill);
		both.retainAll(decompName);
		if (!both.isEmpty()) {
			throw new SemanticErrorException("Decomposition name "
					+ origFormula + " has predicates " + both
					+ " as both names and kills");
		}
		return Pair.create(DynamicVocabulary.create(decompName),
				DynamicVocabulary.create(kill));
	}

	/**
	 * Add a new decomposition name described by the given formula
	 */
	@Override
	public void addDecompositionFormula(Formula formula, String prettyName) {
		Pair<DynamicVocabulary, DynamicVocabulary> predicates = getDecompositionAndKillPredicates(formula);
		DynamicVocabulary decompName = predicates.first;
		DynamicVocabulary kill = predicates.second;

		PClauseName name = PClauseName.create(decompName, kill, prettyName,
				false, null);
		this.criteria.add(name);
		Predicate mark = Vocabulary.createPredicate("dname[" + name + "]", 0);
		for (String param : getDNParameters(name)) {
			ParametricSet.addPredicate(param, mark);
		}
		this.nameToMark.put(name, mark);
	}

	@Override
	public boolean isBase(DecompositionName key) {
		return criteria.contains(key);
	}

	@Override
	public boolean isComposed(DecompositionName key) {
		return ((PClauseName) key).getBase().size() > 1;
	}

	@Override
	public boolean isAbstraction(DecompositionName key) {
		return ((PClauseName) key).isAbstraction();
	}

    @Override
    public Map<Predicate, PredicateUpdateFormula> getChangeFormulas(Action action) {
        Map<Predicate, PredicateUpdateFormula> result = HashMapFactory.make();
        for (PredicateUpdateFormula update : action.getUpdateFormulae().values()) {
            Predicate predicate = update.getPredicate();
            Formula formula = update.getFormula();
            PredicateFormula lhs = new PredicateFormula(predicate, update.variables);
            FormulaDifferencing diff = FormulaDifferencing.getInstance();
            Delta delta = diff.getPredicateDelta(lhs, formula, true);
            Formula changeFormula = diff.constructOrFormula(delta.plus, delta.minus); 
            if (predicate.arity() == 1 && !isOutsideUnaryPrecise()) {
                changeFormula = diff.constructAndFormula(diff.constructNotFormula(new PredicateFormula(Vocabulary.outside, update.variables[0])), changeFormula);
            } else if (predicate.arity() == 2 && !isOutsideBinaryPrecise()) {
                changeFormula = diff.constructAndFormula(diff.constructNotFormula(new PredicateFormula(Vocabulary.outside, update.variables[0])), changeFormula);
                changeFormula = diff.constructAndFormula(diff.constructNotFormula(new PredicateFormula(Vocabulary.outside, update.variables[1])), changeFormula);                
            }
            if (!(changeFormula instanceof ValueFormula && ((ValueFormula) changeFormula).value() == Kleene.falseKleene)) {  
                result.put(predicate, new PredicateUpdateFormula(changeFormula, predicate, update.variables));
            }
        }
        return result;
    }

    private void safeUpdateVocabulary(final DynamicVocabulary voc, HighLevelTVS structure) {
        DynamicVocabulary deltaVoc = voc.subtract(structure.getVocabulary());
        structure.updateVocabulary(voc);
        for (Predicate predicate : deltaVoc.all()) {
            ModifiedPredicates.modify(structure, predicate);
        }
    }

}
