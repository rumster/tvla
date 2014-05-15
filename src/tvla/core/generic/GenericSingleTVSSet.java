package tvla.core.generic;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tvla.analysis.AnalysisStatus;
import tvla.core.Canonic;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.StructureGroup;
import tvla.core.TVS;
import tvla.core.decompose.DecompositionName;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Pair;

/** A generic implementation of single-structure join.
 */
public class GenericSingleTVSSet extends GenericTVSSet {
	/** A temporary store for the nullary canonic name of the new (candidate) structure.
	 */
	protected Canonic newCanonic;
	
	/** Maps nodes to canonic names for the new (candidate) tructure.
	 */
	protected Map<Node, Canonic> newCanonicName		= HashMapFactory.make();
	
	/** Maps nodes to canonic names for the single (old) structure.
	 */
	protected Map<Node, Canonic> singleCanonicName		= HashMapFactory.make();

	/** Maps nodes to canonic names for the new (candidate) structure.
	 */
	protected Map<Canonic, Node> newInvCanonicName		= HashMapFactory.make();
	
	/** Maps nodes to canonic names for the single (old) structure.
	 */
	protected Map<Canonic, Node> singleInvCanonicName	= HashMapFactory.make();
	
	
	/** Applies the Join confluence operator.
	 * @return The difference between the updated set
	 * and the old set or null if there is no difference.
	 */
	public HighLevelTVS mergeWith(HighLevelTVS newStructure) {
		AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.BLUR_TIME);
		newStructure.blur();
		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.BLUR_TIME);
		
		if (structures.isEmpty()) {
			structures.add(newStructure);
			return newStructure;		
		}
		cleanup();
		
		for (Iterator<HighLevelTVS> iterator = structures.iterator(); iterator.hasNext(); ) {
			HighLevelTVS singleStructure = iterator.next();
			if (!mergeCondition(singleStructure, newStructure))
				continue;			
			iterator.remove();
			makeCanonicMaps(singleStructure, newStructure);
			boolean change = mergeStructures(singleStructure, newStructure);
			structures.add(singleStructure);
			HighLevelTVS result = (HighLevelTVS) (change ? singleStructure : null);
			return result;
		}
		structures.add(newStructure);
		return (HighLevelTVS) newStructure;
	}

	/** Merges newStructure into singleStructure.
	 */
	protected boolean mergeStructures(TVS singleStructure, TVS newStructure) {
	    assert singleStructure.getVocabulary() == newStructure.getVocabulary();
	    boolean change = false;
		Map<Node, Node> rename = HashMapFactory.make();
		Set<Node> added = HashSetFactory.make();

		// Find nodes in the new structure that are not in the single structure.
		for (Map.Entry<Node, Canonic> entry : newCanonicName.entrySet()) {
			Node newNode = entry.getKey();
			Canonic canonic = entry.getValue();
			Node singleNode = singleInvCanonicName.get(canonic);
			if (singleNode == null) {
				// This is a new node. Add it to the single graph and set active to unknown.
				Node allocated = singleStructure.newNode();
				singleStructure.update(Vocabulary.active, allocated, Kleene.unknownKleene);
				rename.put(allocated, newNode);
				added.add(allocated);
				change = true;
			}
			else {
				Kleene newValue = newStructure.eval(Vocabulary.active, newNode);
				Kleene singleValue = singleStructure.eval(Vocabulary.active, singleNode);
				if (newValue != singleValue) {
					if (singleValue != Kleene.unknownKleene) {
						change = true;
						singleStructure.update(Vocabulary.active, singleNode, Kleene.unknownKleene);
					}
				}
				rename.put(singleNode, newNode);
			}	    
		}
		
		// Find nodes in the single structure that are not in the new structure.
		for (Map.Entry<Node, Canonic> entry : singleCanonicName.entrySet()) {
			Node singleNode = entry.getKey();
			Canonic canonic = entry.getValue();
			Node newNode = newInvCanonicName.get(canonic);
			if (newNode == null) {
				// A node with no matching node. Set active to unknown.
				if (!singleStructure.eval(Vocabulary.active, singleNode).equals(Kleene.unknownKleene)) {
					singleStructure.update(Vocabulary.active, singleNode, Kleene.unknownKleene);
					change = true;
				}
			}
		}
		
		// Join nullary non-abstraction predicates.
        for (Predicate nullaryPred : singleStructure.getVocabulary().nullary()) {
			if (nullaryPred.abstraction())
				continue;
			Kleene singleValue = singleStructure.eval(nullaryPred);
			Kleene newValue = newStructure.eval(nullaryPred);
			if (singleValue != newValue && singleValue != Kleene.unknownKleene) { // test before setting
				change = true;
				singleStructure.update(nullaryPred, Kleene.unknownKleene);
			}
		}
		
		for (Node singleNode : singleStructure.nodes()) { 
			Node newNode = rename.get(singleNode);
			
			if (newNode == null)
				continue;

			// Join unary predicates.
			for (Predicate predicate : singleStructure.getVocabulary().unary()) {
				if (predicate.equals(Vocabulary.active))
					continue;
				Kleene singleValue = singleStructure.eval(predicate, singleNode);
				Kleene newValue = newStructure.eval(predicate, newNode);
				if (added.contains(singleNode)) {
					singleStructure.update(predicate, singleNode, newValue);
				} else if (singleValue != newValue && singleValue != Kleene.unknownKleene) {
					change = true;
					singleStructure.update(predicate, singleNode, Kleene.unknownKleene);
				}
			}

			// Join binary predicates.
            for (Predicate predicate : singleStructure.getVocabulary().binary()) {
				Node singleLeftNode = singleNode;
				Node newLeftNode = newNode;

				for (Node singleRightNode : singleStructure.nodes()) {
					Node newRightNode = rename.get(singleRightNode);
					if (newRightNode == null)
						continue;
					
					Kleene singleValue = singleStructure.eval(predicate, singleLeftNode, singleRightNode);					
					Kleene newValue = newStructure.eval(predicate, newLeftNode, newRightNode);
					if (added.contains(singleRightNode) || added.contains(singleLeftNode)) {
						singleStructure.update(predicate, singleLeftNode, singleRightNode, 
											   newValue);
					} 
					else if (singleValue != newValue && singleValue != Kleene.unknownKleene) {
						change = true;
						singleStructure.update(predicate, singleLeftNode, singleRightNode, 
											   Kleene.unknownKleene);
					}
				}
			}
		}
        recomputeStructureGroup(singleStructure, newStructure);
		return change;
	}
	
	public static Canonic calcNullaryCanonic(TVS structure) {
        Canonic canonic = new Canonic(structure.getVocabulary().nullaryRel().size());
        for (Predicate predicate : structure.getVocabulary().nullaryRel()) {
			canonic.add(structure.eval(predicate));
		}
		return canonic;
	}

	protected void makeCanonicMaps(TVS singleStructure, TVS newStructure) {
		genericBlur.makeCanonicMapsForBlurred(singleStructure, 
											  singleCanonicName,
											  singleInvCanonicName);
		genericBlur.makeCanonicMapsForBlurred(newStructure,
											  newCanonicName,
											  newInvCanonicName);
	}
	
	protected boolean mergeCondition(TVS singleStructure, TVS newStructure) {
		if (newCanonic == null)
			newCanonic = calcNullaryCanonic(newStructure);
		Canonic singleCanonic = calcNullaryCanonic(singleStructure);		
		return singleCanonic.equals(newCanonic);
	}
	
	/** Clears information gathered during merging.
	 */
	protected void cleanup() {
		super.cleanup();
		newCanonic = null;
		newCanonicName.clear();
		newInvCanonicName.clear();
		singleInvCanonicName.clear();
		singleCanonicName.clear();
	}
	
	public boolean mergeWith(HighLevelTVS S, Collection<Pair<HighLevelTVS, HighLevelTVS>> mergedWith) {
		throw new UnsupportedOperationException() ;
	}

    protected void recomputeStructureGroup(TVS singleStructure, TVS newStructure) {
        if (newStructure.getStructureGroup() != null) {
            assert !cachingMode;
            StructureGroup mergedGroup = new StructureGroup((HighLevelTVS) singleStructure);
            Set<HighLevelTVS> members = HashSetFactory.make();
            StructureGroup singleGroup = singleStructure.getStructureGroup();
            if (singleGroup != null) {
                addGroupMembers(mergedGroup, singleGroup, members, false);
            }
            addGroupMembers(mergedGroup, newStructure.getStructureGroup(), members, true);            
            singleStructure.setStructureGroup(mergedGroup);
        }
    }

    protected void addGroupMembers(StructureGroup group, StructureGroup orig, Set<HighLevelTVS> members, boolean remap) {
        for (StructureGroup.Member member : orig.getMembers()) {
            DecompositionName component = member.getComponent();
            HighLevelTVS structure = member.getStructure();
            if (members.add(structure)) {
                Map<Node, Node> mapping = member.getMapping();;
                if (remap) {
                    // Remap names from newStructure to singleStructure according to 
                    // canonical names mapping.
                    Map<Node, Node> origMapping = mapping;
                    mapping = HashMapFactory.make();
                    for (Map.Entry<Node, Node> entry : origMapping.entrySet()) {
                        Node from = entry.getKey();
                        Node newFrom = singleInvCanonicName.get(newCanonicName.get(from));
                        mapping.put(newFrom, entry.getValue());
                    }
                }
                group.addMember(structure, mapping, component);
            } else {
                // Already in group. Do nothing
            }
        }
    }
}
