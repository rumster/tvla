package tvla.core.base;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tvla.analysis.AnalysisStatus;
import tvla.core.Canonic;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.TVS;
import tvla.core.generic.GenericTVSSet;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Pair;

/** An implementation of single-structure join optimized for the base representation.
 */
public class BaseSingleTVSSet extends GenericTVSSet {
	/** Applies the Join confluence operator.
	 * @return The difference between the updated set
	 * and the old set or null if there is no difference.
	 */
	public HighLevelTVS mergeWith(HighLevelTVS structure) {
        if (structure.getStructureGroup() != null) throw new RuntimeException("Join doesn't support structure group");
		AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.BLUR_TIME);
		structure.blur();
		AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.BLUR_TIME);

		if (structures.isEmpty()) {
			structures.add(structure);
			return structure;		
		}
		cleanup();
		BaseTVS newStructure = (BaseTVS) structure;
		
		Set<Predicate> nullary = newStructure.getVocabulary().nullary();
		Canonic newCanonic = calcNullaryCanonic(nullary, newStructure);
		
		for (Iterator<HighLevelTVS> iterator = structures.iterator();
			 iterator.hasNext(); ) {
			HighLevelTVS nextStructure = iterator.next();
			assert nextStructure.getVocabulary() == newStructure.getVocabulary();
			
			BaseTVS singleStructure = (BaseTVS) nextStructure;
			Canonic singleCanonic = calcNullaryCanonic(nullary, singleStructure);
			if (!singleCanonic.equals(newCanonic))
				continue;

			iterator.remove();
			boolean change = false;
			
			Map<Node, Canonic> singleCanonicName = singleStructure.getCanonic();
			if (singleCanonicName == null)
				singleStructure.blur();
			singleCanonicName = singleStructure.getCanonic();
			Map<Canonic, Node> singleInvCanonicName = singleStructure.getInvCanonic();
			
			Map<Node, Canonic> newCanonicName = newStructure.getCanonic();
			Map<Canonic, Node> newInvCanonicName = newStructure.getInvCanonic();
			
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
					if (newValue != singleValue && singleValue != Kleene.unknownKleene) { // test before setting
						change = true;
						singleStructure.update(Vocabulary.active, singleNode, Kleene.unknownKleene);
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
					if (singleStructure.eval(Vocabulary.active, singleNode) != Kleene.unknownKleene) {
						singleStructure.update(Vocabulary.active, singleNode, Kleene.unknownKleene);
						change = true;
					}
				}
			}
			
			// Join nullary non-abstraction predicates.
			for (Predicate nullaryPred : singleStructure.getVocabulary().nullaryNonRel()) {
				Kleene singleValue = singleStructure.eval(nullaryPred);
				Kleene newValue = newStructure.eval(nullaryPred);
				if (singleValue != newValue && singleValue != Kleene.unknownKleene) { // test before setting
					change = true;
					singleStructure.update(nullaryPred, Kleene.unknownKleene);
				}
			}
			
			for (Iterator<Node> singleNodeIt = singleStructure.nodes().iterator(); 
				 singleNodeIt.hasNext(); ) {
				Node singleNode = (Node) singleNodeIt.next();
				Node newNode = rename.get(singleNode);
				
				if (newNode == null)
					continue;

				// Join unary predicates.
				for (Predicate predicate : singleStructure.getVocabulary().unary()) {
					if (predicate.equals(Vocabulary.active)) // handled earlier
						continue;
					Kleene singleValue = singleStructure.eval(predicate, singleNode);
					Kleene newValue = newStructure.eval(predicate, newNode);
					if (added.contains(singleNode)) {
						singleStructure.update(predicate, singleNode, newValue);
					}
					else if (singleValue != newValue && singleValue != Kleene.unknownKleene) { // test before setting
						change = true;
						singleStructure.update(predicate, singleNode, Kleene.unknownKleene);
					}
					else { // keep the same predicate value
					}
				}

				// Join binary predicates.
                for (Predicate predicate : singleStructure.getVocabulary().binary()) {
					Node singleLeftNode = singleNode;
					Node newLeftNode = newNode;

					for (Iterator<Node> rightNodeIt = singleStructure.nodes().iterator(); 
						 rightNodeIt.hasNext(); ) {
						Node singleRightNode = (Node) rightNodeIt.next();
						Node newRightNode = rename.get(singleRightNode);
						if (newRightNode == null)
							continue;
						
						Kleene singleValue = singleStructure.eval(predicate, singleLeftNode, singleRightNode);						
						Kleene newValue = newStructure.eval(predicate, newLeftNode, newRightNode);
						if (added.contains(singleRightNode) || added.contains(singleLeftNode)) {
							singleStructure.update(predicate, singleLeftNode, singleRightNode, 
															   newValue);
						} 
						else if (singleValue != newValue && singleValue != Kleene.unknownKleene) { // test before setting
							change = true;
							singleStructure.update(predicate, singleLeftNode, singleRightNode, 
												   Kleene.unknownKleene);
						}
						else { // keep the same predicate value
						}
					}
				}
			}
			
			structures.add(singleStructure);
			HighLevelTVS result = (HighLevelTVS) (change ? singleStructure : null);
			return result;
		}
		structures.add(newStructure);
		return (HighLevelTVS) newStructure;
	}

	private static Canonic calcNullaryCanonic(Set<Predicate> nullary, TVS structure) {
		Canonic canonic = new Canonic();
		for (Predicate predicate : nullary) {
			canonic.add(structure.eval(predicate));
		}
		return canonic;
	}

	@Override
	public boolean mergeWith(HighLevelTVS S, Collection<Pair<HighLevelTVS, HighLevelTVS>> mergedWith) {
		throw new UnsupportedOperationException() ;
	}

}
