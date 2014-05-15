package tvla.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tvla.core.Combine.INullaryCombiner;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignKleene;
import tvla.core.generic.AddUniverse;
import tvla.core.generic.ClearPredicate;
import tvla.core.generic.DuplicateNode;
import tvla.core.generic.MergeNodes;
import tvla.core.generic.NodeValueMap;
import tvla.core.generic.NumberSatisfy;
import tvla.core.generic.FilterNodes;
import tvla.core.generic.SetAll;
import tvla.formulae.Formula;
import tvla.formulae.NotFormula;
import tvla.io.StructureToTVS;
import tvla.logic.Kleene;
import tvla.predicates.DynamicVocabulary;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.Filter;

/** A low-level interface for three-valued structures.
 * @author Mooly Sagiv.
 * @author Ganesan Ramalingam.
 * @author Roman Manevich.
 * @author Deepak Goyal.
 * @author John Field.
 * @since tvla-2-alpha (May 12 2002)
 */
public abstract class TVS {
	///////////////////////////////////////////////////////////////////////////
	//                           Core TVS operations                         //
	///////////////////////////////////////////////////////////////////////////
	
    /** Creates a copy of this structure.
	 */
	public abstract TVS copy();

	/** Returns the predicate's interpretation in this structure.
	 */
	public abstract Kleene eval(Predicate nullaryPredicate);

	/** Returns the predicate's interpretation on the specified
	 * node for this structure.
	 */
	public abstract Kleene eval(Predicate unaryPredicate, Node node);

	/** Returns the predicate's interpretation on the specified 
	 * node pair for this structure.
	 */
	public abstract Kleene eval(Predicate binaryPredicate, Node from, Node to);
	
	/** Returns the predicate's interpretation on the specified 
	 * node tuple for this structure.
	 * precondition: predicate.arity() == tuple.size()
	 * @since tvla-2-alpha (May 12 2002).
	 */
	public abstract Kleene eval(Predicate predicate, NodeTuple tuple);
	
	/** Assigns a new interpretation for the specified predicate
	 * in this structure.
	 */
	public abstract void update(Predicate nullaryPredicate, Kleene newValue);
	
	/** Assigns a new interpretation for the specified predicate
	 * and node pair for this structure.
	 */
	public abstract void update(Predicate binaryPredicate, 
								Node from, 
								Node to, 
								Kleene newValue);

	/** Assigns a new interpretation for the specified predicate
	 * and node tuple for this structure.
	 * precondition: predicate.arity() == tuple.size()
	 * @since tvla-2-alpha (May 12 2002).
	 */
	public abstract void update(Predicate predicate, 
								NodeTuple tuple,
								Kleene newValue);

	/** Returns the universe of this structure.
	 */
	public abstract Collection<Node> nodes();
	
	/** Adds a new node to the structure's universe.
	 * precondition: eval(Vocabulary.active, node) == Kleene.falseKleene
	 * postcondition: eval(Vocabulary.active, node) == Kleene.trueKleene
	 */
	public abstract Node newNode();
	
	/** Removes a node from the structure's universe.
	 * precondition: eval(Vocabulary.active, node) == Kleene.trueKleene
	 */
	public abstract void removeNode(Node node);
	
    /**
     * iterate over the potentially satisfying assignments for a predicate
     * mandatory for performance of formulae evaluation 
     * @param pred
     * @param partialNodes 
     * @param desiredValue
     * @return iterator over NodeTuples
     */
    public abstract Iterator<Map.Entry<NodeTuple, Kleene>> predicateSatisfyingNodeTuples(Predicate pred, Node[] partialNodes, Kleene desiredValue);
    
    public Iterator<Map.Entry<NodeTuple, Kleene>> iterator(Predicate predicate) {
    	throw new java.lang.UnsupportedOperationException();
    }
	///////////////////////////////////////////////////////////////////////////
	//            The following are operations are optional.                 //
	//       They may be implemented to take advantage of specific TVS       //
	//              implementations to increase efficiency.                  //
	///////////////////////////////////////////////////////////////////////////
    
	/** Duplicates the specified node by adding another node to the
	 * structure and copying its predicate values.
	 * An optional operation.
	 * @see tvla.core.generic.DuplicateNode
	 */
	public Node duplicateNode(Node node) {
		return DuplicateNode.getInstance().duplicateNode(this, node);
	}
	
	/** Merges the specified collection of nodes by joining their 
	 * predicate values. The specified nodes are removed and the 
	 * resulting node is returned. Note that the result may be a
	 * node in the specified collection, in which case it's not
	 * removed from the structure's universe.
	 * The operation does not affect the node collection.
	 * An optional operation.
	 * @see tvla.core.generic.MergeNodes
	 */
	public Node mergeNodes(Collection<Node> nodesToMerge) {
		return MergeNodes.getInstance().mergeNodes(this, nodesToMerge);
	}
	
	/** @return An iterator to a set of assignments that satisfy the formula.
	 * An optional operation.
	 * @see tvla.core.generic.EvalFormula
	 */
	public Iterator<AssignKleene> evalFormula(Formula formula, Assign partialAssignment) {
		//return EvalFormula.evalFormula(this, formula, partialAssignment);
		formula.prepare(this);
		return formula.assignments(this, partialAssignment);
	}

	/** @return An iterator to a set of assignments that evaluate to the
	 * specified value.
	 * An optional operation.
	 * @see tvla.core.generic.EvalFormulaForValue
	 */
	public Iterator<AssignKleene> evalFormulaForValue(Formula formula, 
										Assign partialAssignment, 
										Kleene desiredValue) {
		//return EvalFormulaForValue.evalFormulaForValue(this, 
		//											   formula, 
		//											   partialAssignment, 
		//											   desiredValue);
	    if (desiredValue == Kleene.falseKleene) {
	        formula = new NotFormula(formula);
	        desiredValue = Kleene.trueKleene;
	    }
		formula.prepare(this);
		return formula.assignments(this, partialAssignment, desiredValue);
	}
	
     
	/** Resets the specified predicate.
	 * An optional operation.
	 * @see tvla.core.generic.ClearPredicate
	 */
	public void clearPredicate(Predicate predicate) {
		ClearPredicate.getInstance().clearPredicate(this, predicate);
	}
	
	/** Returns the number of satisfying assignments for the 
	 * specified predicate in this structure.
	 * An optional operation.
	 * postcondition: (predicate == Vocabulary.active) implies return == nodes().size()
	 */
	public int numberSatisfy(Predicate predicate) {
		return NumberSatisfy.getInstance().numberSatisfy(this, predicate);
	}
	

 	/** Copies all the nodes from structure from to this strucutre.
	 * Note that the predicate values are also copied. 
	 * The strucutre toAdd is not modified.
	 * 
	 * pre: The nodes in from and in the this stucuture
	 * have different canonic names. 
	 * @param from A structure to add its node.
	 */
	public void addUniverse(TVS from)  {
		modify(Vocabulary.active);
		AddUniverse.getInstance().addUniverse(this, from);
	}
	
 	/** Sets a given value to all the nodes in the strucutre
 	 * for a given (unary) predicate.
 	 * 
	 * pre: pred has arity 1.
	 */
	public void setAll(Predicate uniPred, Kleene val) {
		modify(uniPred);
		SetAll.getInstance().setAll(this, uniPred, val);
	}
	
	
 	/** Removes all the nodes in which the given unary predicate 
 	 * have the given value.
 	 * 
	 * pre: pred has arity 1.
	 */
	public void filterNodes(Filter<Node> filter) {
		FilterNodes.getInstance().filterNodes(this, filter);
	}
	
 	/** Returns the number of nodes in the strucutre.
	 */
	public abstract int numOfNodes();	

    /**
     * Combines two structures with user suplied nullaryCombiner.
     * Implemented using the protected method doCombine.
     */
    public static TVS combine(INullaryCombiner nullaryCombiner, TVS tvsL, TVS tvsR) {
        assert(tvsL != null);
        assert(tvsR != null);

        return tvsL.combineWith(nullaryCombiner, tvsR);
    }
    
	/**
	 * The actual implementation of the combine operation.
	 * Methods that oveload this method should:
	 *  - be symetric, i.e., t1.combineWith(t2) should be isomorphic 
	 *    to t2.combineWith(t1). 
	 *  - not modify thethe receiver object or the argument.
	 * @param tvsR
	 * @return
	 */
    protected abstract TVS combineWith(INullaryCombiner nullaryCombiner, TVS tvsR);   
     
	/** Returns a human readable representation of this structure
	 * (in TVS format).
	 */ 
	public String toString() {
		return StructureToTVS.defaultInstance.convert(this);
	}
	
	public NodeValueMap getIncrementalUpdates() {
		return null;
	}
	
	public boolean isIncremented() {
		return false;
	}
	
	public void commit() {
		
	}
	
	public void disableIncrements() {
		
	}
	
	public void setOriginalStructure(TVS structure) {
		
	}
	
	public void modify(Predicate p) {
		
	}
	
	public boolean isCoerced() {
		throw new UnsupportedOperationException();
	}
	
	public Set<Predicate> getModifiedPredicates() {
		return java.util.Collections.emptySet();
	}

	public DynamicVocabulary getVocabulary() {
        return DynamicVocabulary.full();
	}
	
    public void updateVocabulary(DynamicVocabulary newVoc) {
    	updateVocabulary(newVoc, Kleene.unknownKleene);
    }
    
    public void updateVocabulary(DynamicVocabulary newVoc, Kleene defaultValue) {
        if (newVoc != DynamicVocabulary.full()) {
            throw new UnsupportedOperationException();
        }
    }	

    public TVS permute(Map<Predicate, Predicate> mapping) {
        throw new UnsupportedOperationException();        
    }
    
    public Object getStoredReference() {
        return null;
    }    
    
    public void setStoredReference(Object reference) {
    }
    
	public void setStructureGroup(StructureGroup eqClass) {
	}

	public StructureGroup getStructureGroup() {
		return null;
	}	
}