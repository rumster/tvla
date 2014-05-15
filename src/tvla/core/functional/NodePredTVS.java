package tvla.core.functional;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.assignments.Assign;
import tvla.core.generic.ClearPredicate;
import tvla.formulae.Formula;
import tvla.formulae.PredicateUpdateFormula;
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.ProgramProperties;

public class NodePredTVS extends HighLevelTVS {

	FnUniverse U;

	public Collection<Node> nodes() { return U; }

	// Nodes and predicates have corresponding integer ids:

	private static int id(Node n) { return n.id(); }
	private static int id(Predicate p) { return p.num; }

	// The implementation of binary predicates requires an encoding from
	// pairs of nodes into integers: Node * Node -> int
	protected static int encode(Node first, Node second) {
		return encodeIntPair ( id(first), id(second) );
	}

	protected static int encodeIntPair(int n1, int n2) {
		// diagonal encoding:
		//    0 1 3 6 ...
		//    2 4 7 ...
		//    5 8 ...
		//    9 ...
		//    ...
		int diagonal = n1 + n2; 
		// must check for possible overflow and throw new Error("Node  are too big");
		int result = ( (diagonal * (diagonal+1))/2 ) + n1;
		return result;
	}

	// Nullary predicates are implemented using a single int->kleene map:
	protected IntKleeneMap nullary;

	public Kleene eval (Predicate p) {
		assert (p.arity() == 0);
		return nullary.lookup( id(p) );
	}

	public void update (Predicate p, Kleene k) {
		assert (p.arity() == 0);
		if (eval(p).equals(k)) return;
		nullary = nullary.update( id(p), k );
	}

	// Unary and binary predicates are implemented using two-level maps, as:
	//    Node-> Predicate -> Kleene
	// and
	//    (Node * Node) -> Predicate -> Kleene
	// respectively. We use a int->object map to represent the top-level map.

	protected IntObjectMap absUnary;
	protected IntObjectMap nonabsUnary;
	protected IntObjectMap binary;

	static protected IntKleeneMap defaultUnaryMap() {
		return PackedIntKleeneMap.zero;
	}

	static protected IntKleeneMap defaultBinaryMap() {
		return PackedIntKleeneMap.zero;
	}

	// lookup/update for unary predicates:
	public final IntKleeneMap auFlik(Node n) {
		return (IntKleeneMap) absUnary.lookup ( id(n) );
	}

	public final void normalizeAuFlik(Node n) {
		IntKleeneMap flik = (IntKleeneMap) absUnary.lookup ( id(n) );
		if (flik == null) flik = defaultUnaryMap();
		flik = flik.normalize();
		absUnary = absUnary.update ( id(n), flik );
	}

	public final void clearAuFlik(Node n) {
		IntKleeneMap flik = defaultUnaryMap();
		absUnary = absUnary.update ( id(n), flik );
	}

	public final IntKleeneMap nuFlik(Node n) {
		return (IntKleeneMap) nonabsUnary.lookup ( id(n) );
	}

	public final void normalizeNuFlik(Node n) {
		IntKleeneMap flik = (IntKleeneMap) nonabsUnary.lookup ( id(n) );
		if (flik == null) flik = defaultUnaryMap();
		flik = flik.normalize();
		nonabsUnary = nonabsUnary.update ( id(n), flik );
	}

	public final void clearNuFlik(Node n) {
		IntKleeneMap flik = defaultUnaryMap();
		nonabsUnary = nonabsUnary.update ( id(n), flik );
	}

	public final void setNuFlik(Node n, IntKleeneMap val) {
		nonabsUnary = nonabsUnary.update ( id(n), val );
	}

	public final IntKleeneMap binaryFlik(Node n1, Node n2) {
		return (IntKleeneMap) binary.lookup ( encode(n1, n2) );
	}

	public final void normalizeBinaryFlik(Node n1, Node n2) {
		int pair = encode(n1, n2);
		IntKleeneMap flik = (IntKleeneMap) binary.lookup (pair);
		if (flik == null) flik = defaultBinaryMap();
		flik = flik.normalize();
		binary = binary.update (pair, flik);
	}

	public final void clearBinaryFlik(Node n1, Node n2) {
		IntKleeneMap flik = defaultBinaryMap();
		binary = binary.update (encode(n1,n2), flik);
	}

	public final void setBinaryFlik(Node n1, Node n2, IntKleeneMap val) {
		binary = binary.update (encode(n1,n2), val);
	}

	public static boolean isAbstract (Predicate p) {
		// return p.abstraction() || (p instanceof Instrumentation) ;
		return p.abstraction();
	}

	public Kleene eval (Predicate p, Node n) {
		assert (p.arity() == 1);
		IntObjectMap majorMap = isAbstract(p) ? absUnary : nonabsUnary;
		IntKleeneMap minorMap = (IntKleeneMap) majorMap.lookup ( id(n) );
		if (minorMap ==  null)  {
			// minorMap = defaultValue();
			return Kleene.falseKleene;
		}
		return minorMap.lookup (id(p));
	}

	public void update (Predicate p, Node n, Kleene k) {
		assert (p.arity() == 1);
		if (eval(p,n).equals(k)) return;
		IntObjectMap majorMap = isAbstract(p) ? absUnary : nonabsUnary;
		IntKleeneMap minorMap = (IntKleeneMap) majorMap.lookup ( id(n) );
		if (minorMap ==  null) 
			minorMap = defaultUnaryMap();
		minorMap = minorMap.update(id(p), k);
		if (isAbstract(p))
			absUnary = absUnary.update ( id(n), minorMap );
		else
			nonabsUnary = nonabsUnary.update ( id(n), minorMap );

		if( isAbstract(p) ) blurred = false;
		/*
		if (p == cachedPredicate) {
		cachedPredicate = null;
		cachedValue = null;
		}
		*/
	}

	// lookup/update for binary predicates:

	public Kleene eval (Predicate p, Node n1, Node n2) {
		assert (p.arity() == 2);
		IntKleeneMap minorMap = (IntKleeneMap) binary.lookup ( encode(n1, n2) );
		if (minorMap ==  null)  {
			// minorMap = defaultValue();
			return Kleene.falseKleene;
		}
		return minorMap.lookup (id(p));
	}

	/** Returns the predicate's interpretation on the specified 
	 * node tuple for this structure.
	 * @author Roman Manevich
	 * @since tvla-2-alpha (May 12 2002).
	 */
	public Kleene eval(Predicate predicate, NodeTuple tuple) {
		assert (predicate.arity() == tuple.size());
		switch (tuple.size()) {
		case 0: return eval(predicate);
		case 1: return eval(predicate, tuple.get(0));
		case 2: return eval(predicate, tuple.get(0), tuple.get(1));
		default: throw new UnsupportedOperationException("Support predicates of " +
					"arity > 2 is not yet available by the functional implementation!");
		}
	}

	/** Assigns a new interpretation for the specified predicate
	 * and node tuple for this structure.
	 * @author Roman Manevich
	 * @since tvla-2-alpha (May 12 2002).
	 */
	public void update(Predicate predicate, 
					   NodeTuple tuple,
					   Kleene newValue) {
		assert (predicate.arity() == tuple.size());
		switch (tuple.size()) {
		case 0: update(predicate, newValue);
				break;
		case 1: update(predicate, tuple.get(0), newValue);
				break;
		case 2: update(predicate, tuple.get(0), tuple.get(1), newValue);
				break;
		default: throw new UnsupportedOperationException("Support predicates of " +
					"arity > 2 is not yet available by the functional implementation!");
		}
	}
	
	public void update (Predicate p, Node n1, Node n2, Kleene k) {
		assert (p.arity() == 2);
		if (eval(p,n1,n2).equals(k)) return;
		int pair = encode(n1, n2);
		IntKleeneMap minorMap = (IntKleeneMap) binary.lookup (pair);
		if (minorMap ==  null) 
			minorMap = defaultBinaryMap();
		minorMap = minorMap.update(id(p), k);
		binary = binary.update (pair, minorMap);
	}

	public Node newNode() {
		Node n = U.newElement();
		
		clearAuFlik(n);
		clearNuFlik(n);
		for (Iterator i = U.iterator(); i.hasNext(); ) {
			Node m = (Node) i.next();
			clearBinaryFlik(n,m);
			clearBinaryFlik(m,n);
		}

		this.update (Vocabulary.active, n, Kleene.trueKleene);
		blurred = false;
		return n;
	}


	public void removeNode (Node n) {
		// remove dead values from map.
		clearAuFlik(n);
		clearNuFlik(n);
		for (Iterator i = U.iterator(); i.hasNext(); ) {
			Node m = (Node) i.next();
			clearBinaryFlik(n,m);
			clearBinaryFlik(m,n);
		}

		U.remove(n);
	}


	public IntKleeneMap canonicName (Node n) {
		return (IntKleeneMap) absUnary.lookup ( id(n) );
	}

	public void clearPredicate (Predicate p) {
		ClearPredicate.getInstance().clearPredicate(this, p);
		if (isAbstract(p)) blurred = false;
	}

	private Node merge(Node n1, Node n2, Collection allNodes) {
		IntKleeneMap flik = nuFlik(n1).join (nuFlik(n2));
		setNuFlik (n1, flik);

		for (Iterator i = allNodes.iterator(); i.hasNext(); ) {
			Node n3 = (Node) i.next();
			if ((n3 != n1) && (n3 != n2)) {
				flik = binaryFlik(n1,n3).join (binaryFlik(n2,n3));
				setBinaryFlik (n1, n3, flik);
				flik = binaryFlik(n3,n1).join (binaryFlik(n3,n2));
				setBinaryFlik (n3, n1, flik);
			}
		}

		flik = binaryFlik(n1,n1).join (binaryFlik(n1,n2));
		flik = flik.join( binaryFlik(n2,n1) );
		flik = flik.join( binaryFlik(n2,n2) );
		setBinaryFlik (n1, n1, flik);

		// this.removeNode(n2);
		// allNodes.remove(n2);
		this.update(Vocabulary.sm, n1, Kleene.unknownKleene);
		return n1;
	}

	/*
	public Node mergeNodes(Collection nodesToMerge) {
	Iterator it = nodesToMerge.iterator();
	Node first = (Node) it.next();
	while (it.hasNext()) {
	Node snd = (Node) it.next();
	first = merge(first, snd);
	}
	return first;
	}
	*/

	public void blur() {
		if (blurred) return;

		Comparator cmp = new CanonicNodeComparator(this);
		TreeSet nodesInOrder = new TreeSet(cmp);

		for (Iterator i = U.iterator(); i.hasNext(); ) {
			Node n = (Node) i.next();
			normalizeAuFlik(n);
			nodesInOrder.add(n);
		}

		Iterator it = nodesInOrder.iterator();
		FnUniverse newUniverse = U.emptyCopy();

		if (it.hasNext()) {
			Node prev = (Node) it.next();
			newUniverse.addFirst(prev);
			while (it.hasNext()) {
				Node next = (Node) it.next();
				if (auFlik(next).uid() == auFlik(prev).uid()) {
					prev = merge(prev, next, nodesInOrder);
					newUniverse.addFree(next);
					it.remove(); // hmmm ... relies on merge returning first.
				} else {
					prev = next;
					newUniverse.addFirst(next);
				}
			}
		}

		U = newUniverse;

		blurred = true;
	}

	public void partNormalize() {
		this.blur();
		nullary = nullary.normalize();
	}

	public int partialSignature() {
		int hash = 0;
		for (Iterator i = this.nodes().iterator(); i.hasNext(); ) {
			Node node = (Node)i.next();
			hash *= 31;
			hash += auFlik(node).uid();
		}
		return hash;
	}

	public boolean partiallyIsomorphic(HighLevelTVS other) {
		NodePredTVS that = (NodePredTVS) other;
		Collection nodes1 = this.nodes();
		Collection nodes2 = that.nodes();

		if (nodes1.size() != nodes2.size()) return false;

		// Compare nullary predicate values
		if (this.nullary.uid() != that.nullary.uid()) return false;

		// Compare all abstraction unary predicate values
		Iterator iter1 = nodes1.iterator(); 
		Iterator iter2 = nodes2.iterator(); 
		while (iter1.hasNext()) {
			Node node1 = (Node) iter1.next();
			Node node2 = (Node) iter2.next();
			if (this.auFlik(node1).uid() != that.auFlik(node2).uid()) return false;
		}

		return true;
	}

	/*** INCOMPLETE
	// singleJoinWith (other): other structure may be any structure;
	// in particular, it may have a set of canonic nodes different from this structure.
	public boolean singleJoinWith(HighLevelTVS other) {
	NodePredTVS that = (NodePredTVS) other;
	Collection nodes1 = this.nodes();
	Collection nodes2 = that.nodes();
	Iterator iter1 = nodes1.iterator(); 
	Iterator iter2 = nodes2.iterator(); 
	while (iter1.hasNext()) {
	Node node1 = (Node) iter1.next();
	Node node2 = (Node) iter2.next();
	}
	}
	***/

	// mergeWith (other): other is required to be a structure with the same
	// set of canonic nodes as this structure.

	public boolean mergeWith(HighLevelTVS other) {
		NodePredTVS that = (NodePredTVS) other;
		Collection nodes1 = this.nodes();
		Collection nodes2 = that.nodes();
		boolean changed = false;

		// Merge all non-abstraction unary predicate values
		Iterator iter1 = nodes1.iterator(); 
		Iterator iter2 = nodes2.iterator(); 
		while (iter1.hasNext()) {
			Node node1 = (Node) iter1.next();
			Node node2 = (Node) iter2.next();
			IntKleeneMap original = this.nuFlik(node1) ;
			IntKleeneMap flik = original.join (that.nuFlik(node2));
			flik = flik.normalize();
			if (flik.uid() != original.uid()) {
				this.setNuFlik (node1, flik);
				changed = true;
			}
		}
		
		// Make sure that the binary predicates match on each of the node pairs.
		Iterator leftIter1 = nodes1.iterator(); 
		Iterator leftIter2 = nodes2.iterator(); 
		while (leftIter1.hasNext()) {
			Node oldLeftNode = (Node)leftIter1.next();
			Node candidateLeftNode = (Node)leftIter2.next();

			Iterator rightIter1 = nodes1.iterator(); 
			Iterator rightIter2 = nodes2.iterator(); 
			while (rightIter1.hasNext()) {
				Node oldRightNode = (Node)rightIter1.next();
				Node candidateRightNode = (Node)rightIter2.next();

				IntKleeneMap original = this.binaryFlik(oldLeftNode, oldRightNode);
				IntKleeneMap flik = original.join (that.binaryFlik(candidateLeftNode, candidateRightNode));
				flik = flik.normalize();
				if (flik.uid() != original.uid()) {
					this.setBinaryFlik (oldLeftNode, oldRightNode, flik);
					changed = true;
				}
			}
		}

		return changed;
	}

	public int signature() {
		if (renumber) {
			int hash = this.nodes().size();
			hash *= 31; hash += this.nullary.uid();
			hash *= 31; hash += this.absUnary.objectHashCode(); 
			hash *= 31; hash += this.nonabsUnary.objectHashCode();
			hash *= 31; hash += this.binary.objectHashCode(); 
			return hash;
		} else if (normalizeStructure) 
			return uniqueId;
		else {
			int hash = 0;
			for (Iterator i = this.nodes().iterator(); i.hasNext(); ) {
				Node node = (Node)i.next();
				hash *= 31;
				hash += auFlik(node).uid();
				hash += nuFlik(node).uid();
			}
			return hash;
		}
	}

	// NOTE: The implementations of hashCode() and equals() below
	// guarantee that two NodePredTVS are equal iff they are isomorphic.
	// These are currently turned off, as parts of the TVLA engine use
	// "Sets" of TVS, which can cause these isomorphism-check methods
	// to be invoked at inappropriate times.

	/*
	public int hashCode() {
	return this.signature();
	}

	// equals: should be called only on fully normalized structures:
	public boolean equals(Object other) {
	if (other instanceof NodePredTVS) {
	NodePredTVS that = (NodePredTVS) other;
	return this.isomorphic(that);
	} else
	return false; // Should not happen
	}
	*/

	public void normalize() {

		this.blur();

		nullary = nullary.normalize();

		for (Iterator i1 = U.iterator(); i1.hasNext(); ) {
			Node n1 = (Node) i1.next();
			normalizeNuFlik(n1);
			for (Iterator i2 = U.iterator(); i2.hasNext(); ) {
				Node n2 = (Node) i2.next();
				normalizeBinaryFlik(n1, n2);
			}
		}

		if (renumber)
			renumberNodes();
		else if (normalizeStructure) {
			computeUniqueId();
		} else {
			if (normalizeMapsFully) {
				absUnary = absUnary.normalize(defaultUnaryMap());
				nonabsUnary = nonabsUnary.normalize(defaultUnaryMap());
				binary = binary.normalize(defaultBinaryMap());
			}
			if (normalizeNodelist) U.normalize();
		}
	}

	public boolean isomorphic (HighLevelTVS other) {
		if (renumber) {
			NodePredTVS that = (NodePredTVS) other;
			if (this.nodes().size() != that.nodes().size()) return false;
			if (this.nullary != that.nullary) return false;
			if (this.absUnary != that.absUnary) return false;
			if (this.nonabsUnary != that.nonabsUnary) return false;
			if (this.binary != that.binary) return false;
			return true;
		} else if (normalizeStructure) {
			int sig1 = this.uniqueId;
			int sig2 = ((NodePredTVS) other).uniqueId;
			return (sig1 == sig2);
		} else
			return this.isomorphismTest (other);
	}

	public boolean isomorphismTest (HighLevelTVS other) {
		NodePredTVS that = (NodePredTVS) other;
		Collection nodes1 = this.nodes();
		Collection nodes2 = that.nodes();

		if (nodes1.size() != nodes2.size()) return false;

		// Compare nullary predicate values
		if (this.nullary.uid() != that.nullary.uid()) return false;

		// Compare all unary predicate values
		Iterator iter1 = nodes1.iterator(); 
		Iterator iter2 = nodes2.iterator(); 
		while (iter1.hasNext()) {
			Node node1 = (Node) iter1.next();
			Node node2 = (Node) iter2.next();
			if (this.auFlik(node1).uid() != that.auFlik(node2).uid()) return false;
			if (this.nuFlik(node1).uid() != that.nuFlik(node2).uid()) return false;
		}
		
		// Make sure that the binary predicates match on each of the node pairs.
		Iterator leftIter1 = nodes1.iterator(); 
		Iterator leftIter2 = nodes2.iterator(); 
		while (leftIter1.hasNext()) {
			Node oldLeftNode = (Node)leftIter1.next();
			Node candidateLeftNode = (Node)leftIter2.next();

			Iterator rightIter1 = nodes1.iterator(); 
			Iterator rightIter2 = nodes2.iterator(); 
			while (rightIter1.hasNext()) {
				Node oldRightNode = (Node)rightIter1.next();
				Node candidateRightNode = (Node)rightIter2.next();

				if (that.binaryFlik(candidateLeftNode, candidateRightNode).uid() !=
					this.binaryFlik(oldLeftNode, oldRightNode).uid())
					return false;
			}
		}

		return true;
	}

	protected static IntHashCons hashCons = null;

	private int uniqueId;

	public void computeUniqueId () {
		Collection nodes = this.nodes();

		int uniqueSig;

		// nullary predicate values
		int allNullaryValues = this.nullary.uid();
		uniqueSig = allNullaryValues;

		if (nodes.size() > 0) {
			// all unary predicate values
			Iterator iter = nodes.iterator(); 
			Node node = (Node) iter.next();
			int thisval = hashCons.instance (auFlik(node).uid(), nuFlik(node).uid());
			int allUnaryValues = thisval;
			while (iter.hasNext()) {
				node = (Node) iter.next();
				thisval = hashCons.instance (auFlik(node).uid(), nuFlik(node).uid());
				allUnaryValues = hashCons.instance (thisval, allUnaryValues);
			}
			uniqueSig = hashCons.instance (allUnaryValues, uniqueSig);
			
			// the binary predicates 
			Iterator leftIter = nodes.iterator(); 
			while (leftIter.hasNext()) {
				Node leftNode = (Node)leftIter.next();

				Iterator rightIter = nodes.iterator(); 
				Node rightNode = (Node)rightIter.next();
				int rowval = this.binaryFlik(leftNode, rightNode).uid();
				while (rightIter.hasNext()) {
					rightNode = (Node)rightIter.next();
					int thisbinval = this.binaryFlik(leftNode, rightNode).uid();
					rowval = hashCons.instance (thisbinval, rowval);
				}
				uniqueSig = hashCons.instance (rowval, uniqueSig);
			}
		}

		// Combine the node size last, so that the whole process is reversible:
		uniqueSig = hashCons.instance (nodes.size(), uniqueSig);

		uniqueId = uniqueSig;
	}

	public void renumberNodes () {

		IntObjectMap newAuMap = TernaryTree.empty();
		IntObjectMap newNuMap = TernaryTree.empty();
		IntObjectMap newBinMap = TernaryTree.empty();

		Collection nodes = this.nodes();
		int numNodes = nodes.size();

		// Renumber all nodes 0 through numNodes - 1.

		// Construct new unary predicate maps:
		Iterator iter = nodes.iterator(); 
		for (int n = numNodes-1; n >= 0; n--) {
			Node node = (Node) iter.next();
			newAuMap = 	newAuMap.update ( n, auFlik(node) );
			newNuMap = 	newNuMap.update ( n, nuFlik(node) );
		}
		
		// Construct new binary predicate maps:
		Iterator leftIter = nodes.iterator(); 
		for (int leftNum = numNodes - 1; leftNum >= 0; leftNum--) {
			Node leftNode = (Node)leftIter.next();

			Iterator rightIter = nodes.iterator(); 
			for (int rightNum = numNodes - 1; rightNum >= 0; rightNum--) {
				Node rightNode = (Node)rightIter.next();
				newBinMap = 	newBinMap.update ( encodeIntPair (leftNum, rightNum),
												   this.binaryFlik(leftNode, rightNode) );
			}
		}

		// Replace original maps:

		absUnary = newAuMap.normalize(defaultUnaryMap());
		nonabsUnary = newNuMap.normalize(defaultUnaryMap());
		binary = newBinMap.normalize(defaultBinaryMap());

		// Replace node list:
		U = new FnUniverse(numNodes);

	}

	// Could be used for optimization.
	// Roman: I commented out this functionality, since it is not
	// currently used anywhere.
	//public void implementationHint(int hint) {
	//	if (normalizeStructure && (hint == MARK_UNCOLNEABLE)) {
	//		// nullifying nullary predicates does not really save anything:
	//		U.deallocate();
	//		absUnary = null;
	//		nonabsUnary = null;
	//		binary = null;
	//	}
	//}

	public void updatePredicates(Collection updateFormulae, Assign assignment) {
		TVS oldVersion = this.copy();
		Collection oldNodes = oldVersion.nodes();

		for (Iterator updates = updateFormulae.iterator(); updates.hasNext(); ) {
			PredicateUpdateFormula updateFormula = (PredicateUpdateFormula) updates.next();
			Formula formula = updateFormula.getFormula(); // the formula's right-hand side
			Predicate predicate = updateFormula.getPredicate();

			switch (predicate.arity()) {
			case 0:
			    // Attempt to solve TC cache bug by calling prepare on eval
                formula.prepare(this);
				Kleene newValue = formula.eval(oldVersion, assignment);
				this.update(predicate, newValue);
				break;
			case 1:
				{
					Assign resultAssign = new Assign(assignment);
					Var var = updateFormula.getVariable(0);
					formula.prepare(this);
					for (Iterator it = oldNodes.iterator(); it.hasNext(); ) {
						Node n = (Node) it.next();
						resultAssign.put(var, n);
						Kleene rval = formula.eval(oldVersion, resultAssign); 
						this.update(predicate, n, rval);
					}
				}
				break;
			case 2:
				{
					Assign resultAssign = new Assign(assignment);
					Var leftVar = updateFormula.getVariable(0);
					Var rightVar = updateFormula.getVariable(1);
					formula.prepare(this);
					for (Iterator it1 = oldNodes.iterator(); it1.hasNext(); ) {
						Node n1 = (Node) it1.next();
						resultAssign.put(leftVar, n1);
						for (Iterator it2 = oldNodes.iterator(); it2.hasNext(); ) {
							Node n2 = (Node) it2.next();
							resultAssign.put(rightVar, n2);
							Kleene rval = formula.eval(oldVersion, resultAssign); 
							this.update(predicate, n1, n2, rval);
						}
					}
				}
				break;
			default:
				throw new UnsupportedOperationException("Support is not available for " +
					"predicates of arity > 2 for the funcitonal implementation!");
			}
		}
	}

	public void updatePredicates(Collection updateFormulae, Assign assignment, int hash) {
		if (optimizeUpdates)
			OptimizedUpdate.updatePredicates(this, updateFormulae, assignment, hash);
		else
			updatePredicates(updateFormulae, assignment);
	}

	
	// Cached predicate values:
	// Predicate cachedPredicate;
	// Nodelist cachedValue;

	Nodelist nonFalse(Predicate p) {
		// if (p == cachedPredicate) {
		// return cachedValue;
		// }
		Nodelist result = null;
		for (Iterator it = this.nodes().iterator(); it.hasNext(); ) {
			Node n = (Node) it.next();
			Kleene k = this.eval (p, n);
			if (k != Kleene.falseKleene)
				result = new Nodelist (n, result);
		}
		// cachedPredicate = p;
		// cachedValue = result;
		return result;
	}

	protected boolean blurred;

	public int uid(Node n) {
		return blurred ? (auFlik(n).uid()) : -1;
	}

	public int visitCount; // for computing statistics.

	public void computeSpace(NPSpaceCounter data) {
		int size = U.size();
		data.numNodesNoSharing += size;
		data.nodesSqr += size*size;
		U.computeSpace(data); // data.visit(U);
		data.visit(nullary);
		data.visit(absUnary);
		data.visit(nonabsUnary);
		data.visit(binary);
	}

	// Constructor:

	public NodePredTVS() {
		U = new FnUniverse();
		nullary = PackedIntKleeneMap.zero;
		absUnary = TernaryTree.empty();
		nonabsUnary = TernaryTree.empty();
		binary = TernaryTree.empty();
		blurred = false;
		// cachedPredicate = null;
		// cachedValue = null;
		visitCount = 0;
	}

	public NodePredTVS(NodePredTVS that) {
		U = that.U.copy();
		nullary = that.nullary;
		absUnary = that.absUnary;
		nonabsUnary = that.nonabsUnary;
		binary = that.binary;
		blurred = that.blurred;
		// cachedPredicate = that.cachedPredicate;
		// cachedValue = that.cachedValue;
		visitCount = 0;
	}

	public NodePredTVS copy() {
		return new NodePredTVS(this);
	}

	public static boolean normalizeNodelist = false;
	public static boolean normalizeMapsFully = false;
	public static boolean normalizeStructure = false;
	public static boolean renumber = false;
	public static boolean optimizeUpdates = true;

	public static void init() {
		optimizeUpdates = ProgramProperties.getBooleanProperty("tvla.flik.opt", true);
		int level = ProgramProperties.getIntProperty("tvla.flik.canonizationLevel", 3);
		switch (level) {
		case 1:
			// Only fliks (level 2 maps) are normalized in this setting.
			break;
		case 2:
			// The node list, level 1, and level 2 maps are normalized.
			normalizeNodelist = true;
			normalizeMapsFully = true;
			break;
		case 3:
			// The node list, level 1, and level 2 maps are normalized,
			// after nodes are renumbered from 0 to n-1.
			renumber = true;
			break;
		case 4:
			// This is a variant of the renumbering case, where the structure
			// is reduced to a unique number, and nothing else:
			normalizeStructure = true;
			hashCons = new IntHashCons(50001);
			break;
		default:
			// default setting: same as case 3.
			renumber = true;
			break;
		}
		PartitionByArityAbs.init();
		PackedIntKleeneMap.init();
	}

  public Iterator predicateSatisfyingNodeTuples(Predicate pred, Node[] partialNodes, Kleene desiredValue) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("NYI");
  }

}

/* CanonicNodeComparator: a comparator used for sorting; establishes a total
 * order on nodes based on their canonic name; nodes with same canonic name
 * will occur consecutively in this ordering.
 */

class CanonicNodeComparator implements Comparator {
	private NodePredTVS TVS;

	public int compare (Object o1, Object o2) {
		Node n1 = (Node) o1;
		Node n2 = (Node) o2;
		IntKleeneMap canonicName1 =  TVS.canonicName(n1);
		IntKleeneMap canonicName2 =  TVS.canonicName(n2);
		int res = canonicName1.uid() - canonicName2.uid();
		return (res != 0) ? res : (n1.id() - n2.id()) ;
	}

	public CanonicNodeComparator (NodePredTVS m) {
		TVS = m;
	}
}

/* PartitionByArityAbs:
 *    Auxiliary class used for "numbering" predicates in a way convenient for
 * the data structure and analysis: The numbering is based on partitioning
 * the set of predicates into the following four classes:
 * - nullary predicates 
 * - unary abstraction predicates 
 * - unary non-abstraction predicates 
 * - binary predicates
 * Predicates of each class are numbered consecutively from 0 on. Predicates
 * from different classes may be assigned the same number, but distinct
 * predicates in the same class will have distinct numbers.
 */

class PartitionByArityAbs {
	public static int numOfNullary = 0;
	public static int numOfAbsUnary = 0;
	public static int numOfNonAbsUnary = 0;
	public static int numOfOther = 0;

	public static void setNumber(Predicate p, int n) {
		p.num = n;
	}

	public static int number(Predicate p) {
		return p.num;
	}

	public static void init() {
		numOfNullary = 0;
		for (Iterator i = Vocabulary.allNullaryPredicates().iterator(); i.hasNext(); ) {
			Predicate p = (Predicate) i.next();
			setNumber (p, numOfNullary++);
		}
		numOfAbsUnary = 0;
		numOfNonAbsUnary = 0;
		for (Iterator i = Vocabulary.allUnaryPredicates().iterator(); i.hasNext(); ) {
			Predicate p = (Predicate) i.next();
			if (p.abstraction()) {
				setNumber(p, numOfAbsUnary++);
			} else
				setNumber(p, numOfNonAbsUnary++);
		}
		numOfOther = 0;
		for (Iterator i = Vocabulary.allBinaryPredicates().iterator(); i.hasNext(); ) {
			Predicate p = (Predicate) i.next();
			setNumber(p, numOfOther++);
		}
	}
}