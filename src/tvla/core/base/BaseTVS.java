package tvla.core.base;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import tvla.analysis.Engine;
import tvla.core.Canonic;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.SparseTVS;
import tvla.core.StoresCanonicMaps;
import tvla.core.StructureGroup;
import tvla.core.TVS;
import tvla.core.TVSFactory;
import tvla.core.base.concrete.ConcreteKAryPredicate;
import tvla.core.base.concrete.ConcreteNullaryPredicate;
import tvla.core.base.concrete.ConcretePredicate;
import tvla.core.common.ModifiedPredicates;
import tvla.core.common.NodePair;
import tvla.core.common.NodeTupleIterator;
import tvla.core.common.NodeValue;
import tvla.core.functional.ConcreteKAryPredicateFlik;
import tvla.core.functional.FnUniverse;
import tvla.core.generic.MergeNodes;
import tvla.core.generic.NodeValueMap;
import tvla.core.generic.PredicateNode;
import tvla.logic.Kleene;
import tvla.predicates.DynamicVocabulary;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.EmptyIterator;
import tvla.util.Filter;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.MapInverter;
import tvla.util.ProgramProperties;

/**
 * An implementation of a three-valued structure. The implementation delays
 * unnecessary copy actions for its node set and predicates' values.
 * 
 * @author Tal Lev-Ami
 * @since tvla-2-alpha added support for k-ary predicates (Roman, May 13 2002)
 * @since added support for incremental structures (Igor, November 2006)
 */
public abstract class BaseTVS extends HighLevelTVS implements
		StoresCanonicMaps, SparseTVS {

	/**
	 * Enable incremental structures machinery
	 */
	public final static boolean EnableIncrements = ProgramProperties
			.getBooleanProperty("tvla.engine.incremental.enable", true);

	/**
	 * Turn on increment size tradeoff. The increment size is checked against
	 * the structure size, and the smaller of the two is returned by
	 * getIncrementalUpdates.
	 */
	public final static boolean CheckIncrementSize = ProgramProperties
			.getBooleanProperty("tvla.engine.incremental.checkSize", false);

	public final static boolean IncrementWithAddedNodes = ProgramProperties
			.getBooleanProperty("tvla.engine.incremental.incrementAddedNodes",
					false);

    public final static boolean IncrementFocusedNodes = 
        ProgramProperties
        .getBooleanProperty("tvla.engine.incremental.incrementFocusedNodes",
            true);
	
	/**
	 * Utilize increments computations to optimize memory use. When this is
	 * turned on, identical concrete predicates are collapsed to one shared
	 * object.
	 */
	public final static boolean OptimizeMemory = false;

	static final double IncrementsEfficiencyMultiplier = 0.6;

	public final static boolean UseOldModifiedPredicates = false;

	/**
	 * Maps predicate descriptions (tvla.Predicate) to concrete predicates.
	 */
	protected Map<Predicate, ConcretePredicate> predicates;

	protected Map<Node, Canonic> canonic = null;

	protected Map<Canonic, Node> invCanonic = null;

	protected DynamicVocabulary vocabulary = null;

	protected Object reference = null;

	// MicroCache mcache;
	protected PredicateCache mcache;

	protected FnUniverse U;

	protected BaseTVS originalStructure = null;

	// Modified predicates. Used by calcIncrements and AdvancedCoerce.
	protected Set<Predicate> modifiedPredicates = null;

	protected StructureGroup group;

    private Map<Node, Node> focusMapping;
	
	/**
	 * Constructs and initializes a structure with an empty node set, and a
	 * summary and an inactive predicate.
	 * 
	 * @author Tal Lev-Ami
	 */
	public BaseTVS() {
		super();

		this.predicates = HashMapFactory.make();
		// this.predicates = new LinkedHashMap<Predicate, ConcretePredicate>(11,
		// 0.5f);
		// this.predicates = new LinkedHashMap(0);
		// this.predicates = new QuickHashMap();
		// mcache = new MicroCache(predicates);
		mcache = new PredicateCache(predicates, this);

		U = FnUniverse.create();
	}

	public BaseTVS(DynamicVocabulary vocabulary) {
		this();
		this.vocabulary = vocabulary;
	}

	/**
	 * Conversion constructor.
	 * 
	 * @author Roman Manevich.
	 * @since 16.9.2001 Initial creation.
	 */
	public BaseTVS(TVS other) {
		this();

		BaseTVS otherBase = (BaseTVS) other;

		// Copy the predicate values
		U = otherBase.U.copy();

		this.vocabulary = otherBase.vocabulary;

		for (Predicate predicate : otherBase.predicates.keySet()) {
			this.copyPredicate(otherBase, predicate);
		}

		// INCREMENTS
		if (otherBase.modifiedPredicates != null)
			this.modifiedPredicates = HashSetFactory
					.make(otherBase.modifiedPredicates);
		else
			this.modifiedPredicates = null;
		this.originalStructure = otherBase.originalStructure;

		// Share canonic maps with original structure.
		this.canonic = otherBase.canonic;
		this.invCanonic = otherBase.invCanonic;
		
		this.group = otherBase.getStructureGroup() == null ? null :
			otherBase.getStructureGroup().copy(this);

		if (otherBase.focusMapping != null) {
		    this.focusMapping = HashMapFactory.make(otherBase.focusMapping);
		}
		mcache.clear();
	}

	/**
	 * Returns the predicate's interpretation in this structure.
	 */
	public final Kleene eval(Predicate predicate) {
		assert (predicate.arity() == 0);
		return eval(predicate, NodeTuple.EMPTY_TUPLE);
	}

	/**
	 * Returns the predicate's interpretation on the specified node for this
	 * structure.
	 */
	public final Kleene eval(Predicate predicate, Node node) {
		assert (predicate.arity() == 1);
		return eval(predicate, NodeTuple.createSingle(node));
	}

	/**
	 * Returns the predicate's interpretation on the specified node pair for
	 * this structure.
	 */
	public final Kleene eval(Predicate predicate, Node left, Node right) {
		assert (predicate.arity() == 2);
		return eval(predicate, NodeTuple.createPair(left, right));
	}

	/**
	 * Returns the predicate's interpretation on the specified node tuple for
	 * this structure.
	 * 
	 * @since tvla-2-alpha (May 12 2002).
	 */
	/*
	 * static BaseTVSBloomSet bloomSet = null; static BaseTVSBloomSet
	 * bloomSetInstance = new BaseTVSBloomSet();
	 * 
	 * public void setBloomSet() { if (BaseTVS.bloomSet != null) return;
	 * 
	 * BaseTVSBloomSet bloomSet = bloomSetInstance; bloomSet.clear();
	 * 
	 * Collection nodes = nodes();
	 * 
	 * NodeTuple empty = NodeTuple.EMPTY_TUPLE; for (Iterator<Predicate> it =
	 * Vocabulary.allNullaryPredicates().iterator(); it.hasNext();) { Predicate
	 * p = it.next(); bloomSet.add(p, empty, evalInternal(p, empty)); }
	 * 
	 * for (Iterator<Predicate> it = Vocabulary.allUnaryPredicates().iterator();
	 * it.hasNext();) { Predicate p = it.next(); for (Iterator<Node> itNode =
	 * nodes.iterator(); itNode.hasNext();) { Node n = itNode.next();
	 * bloomSet.add(p, n, evalInternal(p, n)); } }
	 * 
	 * for (Iterator<Predicate> it =
	 * Vocabulary.allBinaryPredicates().iterator(); it.hasNext();) { Predicate p
	 * = it.next(); for (Iterator<NodeTuple> itTuple =
	 * NodeTupleIterator.createIterator(nodes, 2); itTuple.hasNext();) {
	 * NodeTuple tuple = itTuple.next(); bloomSet.add(p, tuple, evalInternal(p,
	 * tuple)); } }
	 * 
	 * BaseTVS.bloomSet = bloomSet; }
	 * 
	 * static public void releaseBloomSet() { bloomSet = null; }
	 */

	/*
	 * Predicate cachedPredicate = null; ConcretePredicate
	 * cachedConcretePredicate = null;
	 */

	public void checkStructureValidity() {

		Collection<Node> nodes = nodes();

		for (Iterator<Predicate> it = Vocabulary.allUnaryPredicates()
				.iterator(); it.hasNext();) {
			Predicate p = it.next();
			for (Iterator<Entry<NodeTuple, Kleene>> itEntries = this
					.predicateSatisfyingNodeTuples(p, null, null); itEntries
					.hasNext();) {
				Node n = (Node) itEntries.next().getKey();
				if (!nodes.contains(n)) {
					throw new RuntimeException(
							"incompatible structure detected in unary predicate "
									+ p);
				}
			}
		}

		for (Iterator<Predicate> it = Vocabulary.allBinaryPredicates()
				.iterator(); it.hasNext();) {
			Predicate p = it.next();
			for (Iterator<Entry<NodeTuple, Kleene>> itEntries = this
					.predicateSatisfyingNodeTuples(p, null, null); itEntries
					.hasNext();) {
				NodePair n = (NodePair) itEntries.next().getKey();
				if (!nodes.contains(n.first()) || !nodes.contains(n.second())) {
					throw new RuntimeException(
							"incompatible structure detected in binary predicate "
									+ p);
				}
			}
		}
	}

	public final Kleene eval(Predicate predicate, NodeTuple tuple) {
		assert (predicate.arity() == tuple.size());
		ConcretePredicate pred = mcache.get(predicate);
		// ConcretePredicate pred = (ConcretePredicate)
		// predicates.get(predicate);

		if (pred != null) {
			return pred.get(tuple);
		} else if (getVocabulary().contains(predicate)) {
			return Kleene.falseKleene;
		} else {
			return Kleene.unknownKleene;
		}
	}

	final Kleene evalInternal(Predicate predicate, NodeTuple tuple) {
		assert (predicate.arity() == tuple.size());
		ConcretePredicate pred = predicates.get(predicate);
		if (pred != null) {
			return pred.get(tuple);
		} else if (getVocabulary().contains(predicate)) {
			return Kleene.falseKleene;
		} else {
			return Kleene.unknownKleene;
		}
	}

	public int compareTo(HighLevelTVS o) {
		BaseTVS other = (BaseTVS) o;
		ConcretePredicate pred1 = predicates.get(Vocabulary.sm);
		ConcretePredicate pred2 = (ConcretePredicate) other.predicates
				.get(Vocabulary.sm);
		int n1 = (pred1 == null) ? 0 : pred1.numberSatisfy();
		int n2 = (pred2 == null) ? 0 : pred2.numberSatisfy();
		/*
		 * if (n1 != n2) return n1 - n2; return hashCode() - o.hashCode();
		 */

		return n2 - n1;
	}

	/**
	 * return iterator over potentially satisfying node tuples with the desired
	 * value.
	 */

	public Iterator<Map.Entry<NodeTuple, Kleene>> iterator(Predicate predicate) {
		ConcretePredicate pred = mcache.get(predicate);
		if (pred != null) {
			return pred.iterator();
		} else if (getVocabulary().contains(predicate)) {
			return EmptyIterator.instance();
		} else {
			return NodeTupleIterator.createIterator(nodes(), predicate.arity(),
					Kleene.unknownKleene);
		}

	}

	public Iterator<Map.Entry<NodeTuple, Kleene>> predicateSatisfyingNodeTuples(
			Predicate predicate, Node[] partialNodes, Kleene desiredValue) {
		assert (desiredValue != Kleene.falseKleene);

		ConcretePredicate pred = mcache.get(predicate);
		if (pred != null) {
			return pred.satisfyingTupleIterator(partialNodes, desiredValue);
		} else if (getVocabulary().contains(predicate)) {
			return EmptyIterator.instance();
		} else {
			if (desiredValue == Kleene.trueKleene) {
				return EmptyIterator.instance();
			} else {
				return NodeTupleIterator.createIterator(nodes(), predicate
						.arity(), Kleene.unknownKleene);
			}
		}
	}

	/**
	 * Resets the specified predicate.
	 * 
	 * @see tvla.core.generic.ClearPredicate
	 */
	public final void clearPredicate(Predicate predicate) {
		if (!getVocabulary().contains(predicate)) {
			return;
		}

		clearCanonic();
		// ModifiedPredicates.modify(predicate);
		BaseTVSCache.modify(this, predicate);

		mcache.remove(predicate);
		this.predicates.remove(predicate);
	}

	/**
	 * Assigns a new interpretation for the specified predicate in this
	 * structure.
	 */
	public final void update(Predicate predicate, Kleene newVal) {
		assert (predicate.arity() == 0);
		update(predicate, NodeTuple.EMPTY_TUPLE, newVal);
	}

	/**
	 * Assigns a new interpretation for the specified predicate and node pair
	 * for this structure.
	 */
	public final void update(Predicate predicate, Node left, Node right,
			Kleene val) {
		assert (predicate.arity() == 2);
		update(predicate, NodeTuple.createPair(left, right), val);
	}

	/**
	 * Assigns a new interpretation for the specified predicate and node tuple
	 * for this structure.
	 * 
	 * @since tvla-2-alpha (May 12 2002).
	 */
	public final void update(Predicate predicate, NodeTuple tuple, Kleene val) {
		assert (predicate.arity() == tuple.size());
		if (!getVocabulary().contains(predicate)) {
			return;
		}
		clearCanonic();

		// INCREMENTS
		BaseTVSCache.modify(this, predicate);
		// ModifiedPredicates.modify(predicate);

		if (predicate.arity() == 0) {
			if (val == Kleene.falseKleene) {
				mcache.remove(predicate);
				predicates.remove(predicate);
				return;
			}

			// If we got here val != Kleene.falseKleene
			// ConcreteNullaryPredicate nullary = (ConcreteNullaryPredicate)
			// predicates.get(predicate);
			ConcreteNullaryPredicate nullary = (ConcreteNullaryPredicate) mcache
					.get(predicate);

			if (nullary == null || (nullary.get(NodeTuple.EMPTY_TUPLE) != val)) {
				nullary = ConcreteNullaryPredicate.getInstance(val);
				predicates.put(predicate, nullary);
				// mcache.putCurrent(nullary);
				mcache.put(predicate, nullary);
			} else {
				// Nothing changed so no update is needed.
			}

		} else {
			ConcreteKAryPredicate pred = (ConcreteKAryPredicate) mcache
					.get(predicate);

			if (pred == null) {
				if (val == Kleene.falseKleene)
					return;
				pred = new ConcreteKAryPredicate(predicate.arity());
				// FIXME: change back for flik.
				// pred.setUniverse(U);

				predicates.put(predicate, pred);
				mcache.put(predicate, pred);
/*				
			} else {
				pred.modify();
			}
			pred.set(tuple, val);
			if (pred.isAllFalse()) {
				predicates.remove(predicate);
				mcache.remove(predicate);
			}
*/
			} else if (pred.numberSatisfy() == 1 && val == Kleene.falseKleene) {
				if (pred.iterator().next().getKey().equals(tuple)) {
					predicates.remove(predicate);
					mcache.remove(predicate);
				}
				return;
			} else {
				pred.modify();
			}
			pred.set(tuple, val);

		}
	}

	public final Node newNode() {
		// Node node = Node.allocateNode();
		Node node = U.newElement();

		clearCanonic();
		update(Vocabulary.active, node, Kleene.trueKleene);

		// ModifiedPredicates.modify(Vocabulary.active);
		return node;
	}

	public final void removeNode(Node n) {
		clearCanonic();

		for (Iterator<Map.Entry<Predicate, ConcretePredicate>> i = predicates
				.entrySet().iterator(); i.hasNext();) {
			Map.Entry<Predicate, ConcretePredicate> entry = i.next();
			Predicate predicate = entry.getKey();
			if (predicate.arity() == 0)
				continue;

			ConcretePredicate concrete = entry.getValue();

			concrete.modify();
			concrete.removeNode(n);
			if (concrete.isAllFalse()) {
				mcache.remove(predicate);
				i.remove();
			}

			// INCREMENTS
			// ModifiedPredicates.modify(predicate);
		}

		U.remove(n);
	}

	public final Collection<Node> nodes() {
		return U;
	}

	public final void setCanonic(Map<Node, Canonic> canonic,
			Map<Canonic, Node> invCanonic) {
		this.canonic = canonic;
		this.invCanonic = invCanonic;
	}

	public final Map<Node, Canonic> getCanonic() {
		return canonic;
	}

	public final Map<Canonic, Node> getInvCanonic() {
		return invCanonic;
	}

	public final void clearCanonic() {
		this.canonic = null;
		this.invCanonic = null;
	}

	/**
	 * Returns the number of satisfying assignments for the specified predicate
	 * in this structure.
	 */
	public final int numberSatisfy(Predicate predicate) {
		ConcretePredicate concrete = predicates.get(predicate);
		if (concrete != null) {
			return concrete.numberSatisfy();
		} else if (getVocabulary().contains(predicate)) {
			return 0;
		} else {
			return numOfNodes();
		}
	}

	/**
	 * Merges the specified collection of nodes by joining their predicate
	 * values. The specified nodes are removed and the resulting node is
	 * returned.
	 */
	public final Node mergeNodes(Collection<Node> toMerge) {
		clearCanonic();
		Node result = MergeNodes.getInstance().mergeNodes(this, toMerge);

		return result;
	}

	/**
	 * Bifurcates the specified node.
	 * 
	 * @author Tal Lev-Ami
	 */
	public final Node duplicateNode(Node node) {		
		/*
		 * Node newNode = Node.allocateNode(); clearCanonic(); // The new node
		 * must be activated so that it is considered by the // node tuple
		 * iterator. update(Vocabulary.active, newNode, Kleene.trueKleene);
		 */
		Node newNode = newNode();
		
		if (getStructureGroup() != null) {
			Map<Node,Node> newMapping = StructureGroup.Member.buildIdentityMapping(this);
			newMapping.put(newNode, node);
			StructureGroup newGroup = new StructureGroup(this);
			newGroup.addMember(getStructureGroup(), newMapping, null);
			setStructureGroup(newGroup);
		}
		if (useFocusMapping()) {
		    if (focusMapping == null) {
		        focusMapping = StructureGroup.Member.buildIdentityMapping(this);
		    }
		    focusMapping.put(newNode, node);
		}

		// INCREMENTS

		for (Map.Entry<Predicate, ConcretePredicate> predicateEntry : predicates
				.entrySet()) {
			ConcretePredicate concrete = predicateEntry.getValue();

			if (concrete instanceof ConcreteNullaryPredicate)
				continue;

			Predicate predicate = predicateEntry.getKey();
			ConcreteKAryPredicate karyPredicate = (ConcreteKAryPredicate) concrete;
			Iterator<? extends NodeTuple> tupleIter = NodeTupleIterator
					.createIterator(nodes(), predicate.arity());
			while (tupleIter.hasNext()) {
				NodeTuple tuple = (NodeTuple) tupleIter.next();
				if (!tuple.contains(newNode))
					continue;
				NodeTuple destTuple = tuple.substitute(newNode, node);
				Kleene value = karyPredicate.get(destTuple);

				if (value != Kleene.falseKleene) {
					karyPredicate.modify();
					karyPredicate.set(tuple, value);
					// ModifiedPredicates.modify(predicate);
				}
			}
		}

		return newNode;
	}

	public Iterator<Predicate> nonZeroPredicates() {
		return predicates.keySet().iterator();
	}

	/**
	 * @author Tal Lev-Ami
	 * @todo generalize this to be arity independent
	 */
	private void copyPredicate(TVS structure, Predicate predicate) {
		clearCanonic();
		BaseTVS base = (BaseTVS) structure;

		// ModifiedPredicates.modify(predicate);
		ConcretePredicate concrete = (ConcretePredicate) base.predicates
				.get(predicate);

		if (concrete != null) {
			concrete = concrete.copy();
			if (concrete instanceof ConcreteKAryPredicateFlik) {
				((ConcreteKAryPredicateFlik) concrete).setUniverse(U);
			}
			this.predicates.put(predicate, concrete);
		} else {
			this.predicates.remove(predicate);
		}
	}

	public void pack() {
		Collection<Predicate> preds = predicates.keySet();
		for (Iterator<Predicate> it = preds.iterator(); it.hasNext();) {
			Predicate p = it.next();
			ConcretePredicate cp = predicates.get(p);
			if (cp == null)
				continue;
			cp.pack();
		}
	}

	/**
	 * @author: igor Incremental updates functions
	 */

	public void blur() {
		commit();
		super.blur();
	}

	private boolean coerced = false;

	public boolean coerce() {
		coerced = super.coerce();
		// TODO consider committing immediately and saving some information
		// about the structure being infeasible
		if (coerced) {
			commit();
			setOriginalStructure(this);
		}
		return coerced;
	}

	public boolean isCoerced() {
		return coerced;
	}

	/**
	 * @author: igor Return increments between the original snapshot and the
	 *          current structure.
	 */
	public NodeValueMap LastIncrements = null;

	public NodeValueMap getIncrementalUpdates() {
		if (!EnableIncrements)
			return null;

		if (originalStructure == null)
			return null;

		NodeValueMap increments = calcIncrements(originalStructure);
		LastIncrements = increments;

		if (increments == null)
			return null;

		// ADDED
		this.modifiedPredicates = increments.modifiedPredicates();
		
		if (!CheckIncrementSize)
			return increments;

		// Decide whether to return the increments or use the
		// structure as is - whichever is smaller.
		if (modifiedPredicates.contains(Vocabulary.active))
			modifiedPredicates = predicates.keySet();
		float fullSize = 0, incSize = increments.rankedSize();
		for (Iterator<Predicate> it = modifiedPredicates.iterator(); it
				.hasNext();) {
			Predicate p = it.next();
			ConcretePredicate cp = predicates.get(p);
			if (cp == null)
				continue;
			if (cp instanceof ConcreteKAryPredicate)
				fullSize += cp.numberSatisfy() * p.rank;
		}
		if (fullSize < incSize * IncrementsEfficiencyMultiplier)
			return null;
		else
			return increments;
	}

	/**
	 * /* @author: igor /* Used to reset increments calculation.
	 */
	public void commit() {
		modifiedPredicates = null;
		originalStructure = null;
        focusMapping = null;
	}

	public final TVS getOriginalStructure() {
		return originalStructure;
	}

	// Store structure to later calculate the increments from.
	public void setOriginalStructure(TVS orig) {
		if (orig instanceof BaseTVS) {
			originalStructure = (BaseTVS) orig;
		} else {
			originalStructure = null;
		}
	}

	// Mark predicate as modified. Called from ModifiedPredicates
	public void modify(Predicate p) {
		if (modifiedPredicates == null)
			modifiedPredicates = HashSetFactory.make(2); // was 31
		modifiedPredicates.add(p);
		// coerced = false;
	}

	public boolean isIncremented() {
		return modifiedPredicates != null;
	}

	// Get the set of modified predicates. Used by AdvancedCoerce.
	final public Set<Predicate> getModifiedPredicates() {
		if (UseOldModifiedPredicates)
			return ModifiedPredicates.getModified();
		if (modifiedPredicates == null)
			return Collections.emptySet();
		else
			return modifiedPredicates;
	}

	private void addPredicateNodeToAssignMap(Collection<Node> nodes, int arity,
			Node node, Collection<NodeTuple> col) {
		switch (arity) {
		case 0: // nullary predicates
			break;

		case 1: // unary predicates
			col.add(node);
			break;

		case 2:
			Iterator<Node> nodeIter = nodes.iterator();
			while (nodeIter.hasNext()) {
				Node n = nodeIter.next();
				col.add(NodeTuple.createPair(node, n));
				col.add(NodeTuple.createPair(n, node));
			}
			break;
		default: // predicate of arity > 2
			Iterator<? extends NodeTuple> tupleIter = NodeTupleIterator
					.createIterator(nodes, arity);
			while (tupleIter.hasNext()) {
				NodeTuple tuple = (NodeTuple) tupleIter.next();
				if (tuple.contains(node)) {
					col.add(tuple);
				}
			}
		}
	}

	public int numberSatisfy() {
		int n = 0;
		for (Iterator<Predicate> it = nonZeroPredicates(); it.hasNext();) {
			Predicate p = it.next();
			ConcretePredicate cp = this.predicates.get(p);
			n += cp.numberSatisfy();
		}
		return n;
	}

    public void recalcModifiedPredicates() {
        Set<Predicate> preds1 = this.predicates.keySet();
        Set<Predicate> preds2 = originalStructure.predicates.keySet();
        Set<Predicate> only_preds1 = HashSetFactory.make(preds1);
        only_preds1.removeAll(preds2);
        Set<Predicate> only_preds2 = HashSetFactory.make(preds2);
        only_preds2.removeAll(preds1);
        Set<Predicate> common_preds = HashSetFactory.make(preds1);
        common_preds.retainAll(preds2);
        Set<Predicate> all_preds = HashSetFactory.make(preds1);
        all_preds.addAll(only_preds2);

        modifiedPredicates = HashSetFactory.make();
        // Newly added predicates
        for (Predicate p : only_preds1) {
            if (p.arity() == 0)
                continue;

            ConcreteKAryPredicate cp = (ConcreteKAryPredicate) mcache.get(p);
            if (cp != null)
                modifiedPredicates.add(p);
        }
        // Removed predicates
        for (Predicate p : only_preds2) {
            if (p.arity() == 0)
                continue;
            ConcreteKAryPredicate cp = (ConcreteKAryPredicate) originalStructure.predicates
                    .get(p);
            if (cp != null)
                modifiedPredicates.add(p);
        }

        for (Predicate p : common_preds) {
            if (p.arity() == 0)
                continue;

            ConcreteKAryPredicate cp_this = (ConcreteKAryPredicate) mcache
                    .get(p);
            ConcreteKAryPredicate cp_orig = (ConcreteKAryPredicate) originalStructure.predicates
                    .get(p);

            if (cp_this.wasModified(cp_orig)) {
                modifiedPredicates.add(p);
            }
        }
    }
	
	// @author: igor
	// Calculate incremented values between this structure
	// and the original one. Fairly efficient, with the help
	// of modifiedPredicates, that is calculated on the fly.
	public NodeValueMap calcIncrements(BaseTVS orig) {
		Collection<Node> orig_nodes = orig.nodes();
		Collection<Node> this_nodes = this.nodes();

		// ADDED
		Map<Node,Node> origMapping = null;
		if (!orig_nodes.containsAll(this_nodes)) {
		    if (useFocusMapping() && focusMapping != null) {
		        origMapping = focusMapping;
		    } else if (!IncrementWithAddedNodes) {
		        return null;
		    }
		}		

		DynamicVocabulary deltaVoc = getVocabulary().subtract(orig.getVocabulary());
		Set<Predicate> preds1 = this.predicates.keySet();
		Set<Predicate> preds2 = orig.predicates.keySet();
		Set<Predicate> only_preds1 = HashSetFactory.make(preds1);
		only_preds1.removeAll(preds2);
		only_preds1.removeAll(deltaVoc.all());
		Set<Predicate> only_preds2 = HashSetFactory.make(preds2);
		only_preds2.removeAll(preds1);
		only_preds2.retainAll(getVocabulary().all()); // Only keep those in our voc
		Set<Predicate> common_preds = HashSetFactory.make(preds1);
		common_preds.retainAll(preds2);
		Set<Predicate> all_preds = HashSetFactory.make(preds1);
		all_preds.addAll(only_preds2);

		NodeValueMap increments = new NodeValueMap(
				(int) (all_preds.size() / .5));
		
		increments.addDeltaPredicates(deltaVoc);

		Map<PredicateNode, Kleene> map = new LinkedHashMap<PredicateNode, Kleene>(
				1023, (float) .5);

		if (origMapping == null) 
		    calcAddedNodeIncrements(orig_nodes, this_nodes, all_preds, map);

		// Newly added predicates
		for (Predicate p : only_preds1) {
			if (p.arity() == 0) {
				map.put(new PredicateNode(p, NodeTuple.EMPTY_TUPLE), eval(p));
				continue;
			}
			ConcreteKAryPredicate cp = (ConcreteKAryPredicate) mcache.get(p);
			if (cp == null)
				continue;

			for (Map.Entry<NodeTuple, Kleene> entry : cp) {
				map.put(new PredicateNode(p, entry.getKey()), entry.getValue());
			}
		}

		// Removed predicates
		for (Predicate p : only_preds2) {
			if (p.arity() == 0) {
				map.put(new PredicateNode(p, NodeTuple.EMPTY_TUPLE), Kleene.falseKleene);
				continue;
			}

			ConcreteKAryPredicate cp = (ConcreteKAryPredicate) orig.predicates
					.get(p);
			if (cp == null)
				continue;

			for (Map.Entry<NodeTuple, Kleene> entry : cp) {
				map.put(new PredicateNode(p, entry.getKey()),
						Kleene.falseKleene);
			}
		}

		// Changed predicates
		if (origMapping == null) {
    		for (Predicate p : common_preds) {
    			if (p.arity() == 0) {
    				Kleene thisValue = eval(p);
    				Kleene origValue = orig.eval(p);
    				if (thisValue != origValue)
    					map.put(new PredicateNode(p, NodeTuple.EMPTY_TUPLE), thisValue);
    				continue;
    			}
    
    			ConcreteKAryPredicate cp_this = (ConcreteKAryPredicate) mcache
    					.get(p);
    			ConcreteKAryPredicate cp_orig = (ConcreteKAryPredicate) orig.predicates
    					.get(p);
    
    			if (!cp_this.wasModified(cp_orig)) {
    			    continue;
    			}
    			for (Map.Entry<NodeTuple, Kleene> entry : cp_this) {
    				NodeTuple tuple = entry.getKey();
    				Kleene val = entry.getValue();
    				if (cp_orig.get(tuple) == val)
    					continue;
    				else
    					map.put(new PredicateNode(p, tuple), val);
    			}
    
    			for (Map.Entry<NodeTuple, Kleene> entry : cp_orig) {
    				NodeTuple tuple = entry.getKey();
    				if (cp_this.get(tuple) != Kleene.falseKleene)
    					continue;
    				map.put(new PredicateNode(p, tuple), Kleene.falseKleene);
    			}
    		}
		} else {
		    // Increment Focused Nodes 		    
		    Map<Node, Set<Node>> inverseOrigMapping = HashMapFactory.make(); 
            MapInverter.invertMapNonInjective(origMapping, inverseOrigMapping);
            for (Set<Node> nodes : inverseOrigMapping.values()) {
                increments.addInequality(nodes);
            }
            
            for (Predicate p : common_preds) {
    			if (p.arity() == 0) {
    				Kleene thisValue = eval(p);
    				Kleene origValue = orig.eval(p);
    				if (thisValue != origValue)
    					map.put(new PredicateNode(p, NodeTuple.EMPTY_TUPLE), thisValue);
    				continue;
    			}
    
                ConcreteKAryPredicate cp_this = (ConcreteKAryPredicate) mcache
                        .get(p);
                ConcreteKAryPredicate cp_orig = (ConcreteKAryPredicate) orig.predicates
                        .get(p);
    
                if (!cp_this.wasModified(cp_orig)) {
                    continue;
                }
                for (Map.Entry<NodeTuple, Kleene> entry : cp_this) {
                    NodeTuple tuple = entry.getKey();
                    NodeTuple origTuple = tuple.mapNodeTuple(origMapping);
                    Kleene val = entry.getValue();
                    if (cp_orig.get(origTuple) == val)
                        continue;
                    else
                        map.put(new PredicateNode(p, tuple), val);
                }
    
                for (Map.Entry<NodeTuple, Kleene> entry : cp_orig) {
                    NodeTuple tuple = entry.getKey();
                    Iterator<? extends NodeTuple> iterator = tuple.matchingTuples(inverseOrigMapping);
                    while (iterator.hasNext()) {
                        NodeTuple thisTuple = iterator.next();
                        if (cp_this.get(thisTuple) != Kleene.falseKleene)
                            continue;
                        map.put(new PredicateNode(p, thisTuple), Kleene.falseKleene);
                    }
                }
            }		    
		}

		for (Map.Entry<PredicateNode, Kleene> entry : map.entrySet()) {
			PredicateNode pn = entry.getKey();
			Kleene val = entry.getValue();
			increments.fastPut(pn.predicate, new NodeValue(pn.tuple, val,
					pn.added));
		}

		increments.nodesMap = origMapping;

		return increments;
	}

    private boolean useFocusMapping() {
        return IncrementFocusedNodes && Engine.activeEngine.doCoerceAfterFocus;
    }

    @SuppressWarnings("unchecked")
    private boolean calcAddedNodeIncrements(Collection<Node> orig_nodes, Collection<Node> this_nodes,
            Set<Predicate> all_preds, Map<PredicateNode, Kleene> map) {
        boolean addedNewNodes = false;

		// Newly added nodes
		if (!orig_nodes.containsAll(this_nodes)) {
			addedNewNodes = true;

			Set<Node> addedNodes = HashSetFactory.make(this_nodes);
			addedNodes.removeAll(orig_nodes);
			Object[] nodesMap = { null, null, null, null };

			for (Iterator<Predicate> it = all_preds.iterator(); it.hasNext();) {
				Predicate p = it.next();
				int arity = p.arity();
				if (arity < 1)
					continue;

				if (arity > nodesMap.length) {
					Object[] newMap = new Object[arity + 1];
					System.arraycopy(nodesMap, 0, newMap, 0, nodesMap.length);
					for (int i = nodesMap.length; i < arity + 1; ++i)
						newMap[i] = null;
					nodesMap = newMap;
				}

				// order is important: we first add false edges for all new
				// nodes, then some of them may be set to true according to the
				// concrete predicate.
				if (nodesMap[arity] == null) {
					Collection<NodeTuple> col = new LinkedList<NodeTuple>();

					for (Node n : addedNodes) {
						// FIXME: shouldn't it be this_nodes?
						addPredicateNodeToAssignMap(this_nodes, arity, n, col);
					}
					nodesMap[arity] = col;
				}

				Collection<NodeTuple> col = (Collection<NodeTuple>) nodesMap[arity];
				for (NodeTuple tuple : col) {
					map.put(new PredicateNode(p, tuple, true),
							Kleene.falseKleene);
				}
			}
		}
        return addedNewNodes;
    }

	public DynamicVocabulary getVocabulary() {
		if (vocabulary != null) {
			return vocabulary;
		} else {
			return DynamicVocabulary.full();
		}
	}

	public void updateVocabulary(DynamicVocabulary newVoc, Kleene defaultValue) {
		DynamicVocabulary onlyOld = getVocabulary().subtract(newVoc);
		DynamicVocabulary onlyNew = newVoc.subtract(getVocabulary());
		
        clearCanonic();
		
		for (Predicate predicate : onlyOld.all()) {
			mcache.remove(predicate);
			predicates.remove(predicate);
		}
		vocabulary = newVoc;
		for (Predicate predicate : onlyNew.all()) {
			setAllTuples(predicate, defaultValue);
		}
	}

	/**
	 * Set all tuples of given predicate to given value
	 */
	private void setAllTuples(Predicate predicate, Kleene value) {
		if (predicate.arity() == 0) {
			update(predicate, value);
		} else {
            BaseTVSCache.modify(this, predicate);
            ConcreteKAryPredicate concrete = new ConcreteKAryPredicate(predicate.arity());
            if (U.isEmpty() || value == Kleene.falseKleene) {
            	predicates.remove(predicate);
            	mcache.remove(predicate);
            } else {
	            predicates.put(predicate, concrete);
				mcache.put(predicate, concrete);
	
	            Iterator<? extends NodeTuple> iterator = NodeTupleIterator
						.createIterator(U, predicate.arity());
				while (iterator.hasNext()) {
					concrete.set(iterator.next(), value);
				}
            }
		}
	}

	public Object getStoredReference() {
		return reference;
	}

	public void setStoredReference(Object reference) {
		this.reference = reference;
	}

	public void setStructureGroup(StructureGroup group) {
		this.group = group;
	}

	public StructureGroup getStructureGroup() {
		return group;
	}

	@Override
	public HighLevelTVS permute(Map<Predicate, Predicate> mapping) {
		if (getStructureGroup() != null) {
			throw new RuntimeException("Premute is not supported for structure groups");
		}
		// Build vocabulary
		DynamicVocabulary pvoc = getVocabulary().permute(mapping);
		BaseTVS result = (BaseTVS) TVSFactory.getInstance().makeEmptyTVS(pvoc);
		result.U = U.copy();
		for (Predicate predicate : getVocabulary().all()) {
			ConcretePredicate concrete = predicates.get(predicate);
			if (mapping.containsKey(predicate))
				predicate = mapping.get(predicate);
			if (concrete != null) {
				concrete = concrete.copy();
				result.predicates.put(predicate, concrete);
				result.mcache.put(predicate, concrete);
			}
		}
		return result;
	}

    @Override
    public void filterNodes(Filter<Node> filter) {
        clearCanonic();

        Set<Node> toRemove = HashSetFactory.make();
        for (Node node : U) {
            if (!filter.accepts(node)) {
                toRemove.add(node);
            }
        }
        
        for (Iterator<Map.Entry<Predicate, ConcretePredicate>> i = predicates
                .entrySet().iterator(); i.hasNext();) {
            Map.Entry<Predicate, ConcretePredicate> entry = i.next();
            Predicate predicate = entry.getKey();
            if (predicate.arity() == 0)
                continue;

            ConcretePredicate concrete = entry.getValue();

            concrete.modify();
            concrete.removeNodes(toRemove);
            if (concrete.isAllFalse()) {
                mcache.remove(predicate);
                i.remove();
            }

            // INCREMENTS
            // ModifiedPredicates.modify(predicate);
        }

        for (Node node : toRemove) {
            U.remove(node);
        }
    }
	
}
