package tvla.core.generic;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tvla.core.Blur;
import tvla.core.Canonic;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.TVS;
import tvla.core.base.BaseTVS;
import tvla.core.base.PredicateEvaluator;
import tvla.logic.Kleene;
import tvla.predicates.DynamicVocabulary;
import tvla.predicates.Predicate;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.MapInverter;
import tvla.util.ProgramProperties;

/** An implementation of the Blur algorithm.
 * @author Tal-Lev Ami.
 * @since 14.9.2001 Renamed from NaiveBlur (Roman).
 */
public class GenericBlur extends Blur {
	/** A convenience instance.
	 */
	protected static GenericBlur defaultGenericBlur = new GenericBlur();
	
	public static GenericBlur getInstance() {
		return defaultGenericBlur;
	}
	
	/** Maps Node objects to their Canonic counterpart.
	 */
	protected Map<Node, Canonic> canonicName;
	
	/** Maps Canonic objetcs to their Node counterpart.
	 */
	protected Map<Canonic, Node> blurredInvCanonic;
	
	/** Constructs a generic Blur algorithm.
	 */
	public GenericBlur() {
		Canonic.CanonicNamesStatistics.doStatistics = ProgramProperties.getBooleanProperty("tvla.log.canonicNamesStatistics", false);
		buildNodeAccessTable(10);
	}
	
	public static void reset() {
		defaultGenericBlur = new GenericBlur();
	}	
	
	/** Blurs the specified structure in-place.
	 */
	public void blur(TVS structure) {
		canonicName = HashMapFactory.make(structure.nodes().size());
		// Maps Canonic objects to a Set of Node objects.
		Map<Canonic, Collection<Node>> invCanonicName = HashMapFactory.make(structure.nodes().size());

		makeCanonicMaps(structure, canonicName, invCanonicName);

		// Merge sets of nodes with the same canonic name.
		for (Map.Entry<Canonic, Collection<Node>> entry : invCanonicName.entrySet()) {
			Canonic name = entry.getKey();
			Set<Node> toMerge = (Set<Node>) entry.getValue();

			if (toMerge.size() > 1) {
				Node newNode = structure.mergeNodes(toMerge);
				canonicName.put(newNode, name);
				for (Iterator<Node> mergeIt = toMerge.iterator(); mergeIt.hasNext(); ) {
					Node mergedNode = mergeIt.next();
					if (!newNode.equals(mergedNode))
						canonicName.remove(mergedNode);
				}
			}			
		}
		
		// Create the inverse canonic map.
		blurredInvCanonic = HashMapFactory.make(canonicName.size());
		MapInverter.invertMap(canonicName, blurredInvCanonic);
	}
	
	/** Creates a mapping and inverse mapping between the nodes of the structure and 
	 * their canonic name. Only predicates marked as abstraction predicates are 
	 * considered in the canonization.
	 * @param structure A structure.
	 * @param canonicName Will contain the node to canonic name mapping. 
	 * Must be initialized. 
	 * @param invCanonicName Will contain the canonic name to List of nodes mapping. 
	 * Must be initialized.
	 * precondition: canonicName != null && invCanonicName != null
	 */
	public void makeCanonicMaps(TVS structure, Map<Node, Canonic> canonicName, Map<Canonic, Collection<Node>> invCanonicName) {
		assert (canonicName != null && invCanonicName != null);
		
		initCanonicMaps(structure);		
		updateCanonicNames2(structure, nodesTable, values, canonicMap);

		for (Node node : structure.nodes()) {
		    Canonic currentName = canonicMap[nodesTable[node.id()]];
			canonicName.put(node, currentName);
			//canonicSet.add(currentName);
			Collection<Node> invCollection = invCanonicName.get(currentName); 
			
			if (invCollection == null) {
				invCollection = HashSetFactory.make();
				invCanonicName.put(currentName, invCollection);
			}
			invCollection.add(node);
		}
		
		if (Canonic.CanonicNamesStatistics.doStatistics) {
			for (Iterator<Canonic> canonicIter = canonicName.values().iterator();
				 canonicIter.hasNext(); ) {
				Canonic c = canonicIter.next();
				Canonic.CanonicNamesStatistics.allCanonicNames.add(c);
			}
		}		
	}
	
    public Set<Canonic> makeCanonicSet(TVS structure) {
        Set<Canonic> canonicNames = new HashSet<Canonic>(structure.nodes().size());
        initCanonicMaps(structure);     
        updateCanonicNames(structure, nodesTable, values, canonicMap);
        for (Node node : structure.nodes()) {
            Canonic currentName = canonicMap[nodesTable[node.id()]];
            canonicNames.add(currentName);
        }
        return canonicNames;
    }
    
    protected void updateCanonicNames2(TVS structure, int[] nodesTable, Kleene[] values,
            Canonic[] canonicMap) {
        int index = 0;
        Set<Predicate> unaryRel = structure.getVocabulary().unaryRel();
        int pred_size = unaryRel.size();
        for (Node node : structure.nodes()) {
            canonicMap[index] = new Canonic(pred_size, true);
            nodesTable[node.id()] = index++;
        }
        index = 0;
        for (Predicate predicate : unaryRel) {
            Iterator<Map.Entry<NodeTuple, Kleene>> nodeIt = structure.iterator(predicate);            
            while (nodeIt.hasNext()) {
                Map.Entry<NodeTuple, Kleene> entry = nodeIt.next();
                Node node = (Node) entry.getKey();
                int nodeId = nodesTable[node.id()];
                canonicMap[nodeId].set(index, entry.getValue());
            }
            index++;
        }
    }

    protected void updateCanonicNames(TVS structure, int[] nodesTable, Kleene[] values,
            Canonic[] canonicMap) {
        int index = 0;
        int size = structure.nodes().size();
        int pred_size = structure.getVocabulary().unaryRel().size();
        for (Node node : structure.nodes()) {
            canonicMap[index] = new Canonic(pred_size);
            nodesTable[node.id()] = index++;
        }
        for (Predicate predicate : structure.getVocabulary().unaryRel()) {
			Iterator<Map.Entry<NodeTuple, Kleene>> nodeIt = structure.iterator(predicate);
			System.arraycopy(emptyValues, 0, values, 0, size);
			int i;
			
			while (nodeIt.hasNext()) {
				Map.Entry<NodeTuple, Kleene> entry = nodeIt.next();
				Node node = (Node) entry.getKey();
				i = nodesTable[node.id()];
				Kleene value = entry.getValue();
				values[i] = value;
			}
			for (i = 0; i < size; ++i) {
				Canonic currentName = canonicMap[i];
				currentName.add(values[i]);
			}

		}
    }
	
	/** Creates a mapping and inverse mapping of nodes <-> extended names.
	 * All positive arity predicates are considered for extended names.
	 * @param structure A structure.
	 * @param canonicName Will contain the node to canonic name mapping. 
	 * Must be initialized. 
	 * @param invCanonicName Will contain the canonic name to List of nodes mapping. 
	 * Must be initialized.
	 * precondition: canonicName != null && invCanonicName != null
	 * @author Gilad Arnold
	 * @param vocabulary 
	 */
	public void makeExtendedCanonicMaps(TVS structure, Map<Node, Canonic> canonicName, 
                                        Map<Canonic,Collection<Node>> invCanonicName, DynamicVocabulary vocabulary) {
		assert (canonicName != null && invCanonicName != null);
		int size = vocabulary.positiveArity().size();
        for (Node node : structure.nodes()) {
			canonicName.put(node, new Canonic(size));
		}

        for (Predicate predicate : vocabulary.unary()) {
            PredicateEvaluator eval = PredicateEvaluator.evaluator(predicate, structure);
            for (Node node : structure.nodes()) {
                Canonic currentName = canonicName.get(node);
                Kleene value = eval.eval(node);
                currentName.add(value);
            }
        }

        for (Predicate predicate : vocabulary.binary()) {
            PredicateEvaluator eval = PredicateEvaluator.evaluator(predicate, structure);
            for (Node node : structure.nodes()) {
                Canonic currentName = canonicName.get(node);
                Kleene value = eval.eval(NodeTuple.createPair(node, node));
                currentName.add(value);
            }
        }
        
        for (Predicate predicate : vocabulary.kary()) {
            PredicateEvaluator eval = PredicateEvaluator.evaluator(predicate, structure);
            Node [] nodeTupleTmp = new Node[predicate.arity()];
			for (Node node : structure.nodes()) {

                // create k-tuple of the node
                for (int i = 0; i < nodeTupleTmp.length; i++) {
                    nodeTupleTmp[i] = node;
                }
                NodeTuple nodeTuple = NodeTuple.createTuple(nodeTupleTmp);
                    
				Canonic currentName = canonicName.get(node);
				Kleene value = eval.eval(nodeTuple);
				currentName.add(value);
			}
		}
		
		for (Map.Entry<Node, Canonic> entry : canonicName.entrySet()) {
			Node node = entry.getKey();
			Canonic currentName = entry.getValue();
			Collection<Node> nameNodes = invCanonicName.get(currentName);
			if (nameNodes == null) {
			    nameNodes = new HashSet<Node>(structure.nodes().size());
			    invCanonicName.put(currentName, nameNodes);
			}
			nameNodes.add(node);
		}
		
		if (Canonic.CanonicNamesStatistics.doStatistics) {
			for (Canonic c : canonicName.values()) {
				Canonic.CanonicNamesStatistics.allCanonicNames.add(c);
			}
		}		
	}	

	private void buildNodeAccessTable(int size) {
		nodesAccessTable = new IntPair[size];
		for (int i = 0; i < nodesAccessTable.length; ++i) {
			nodesAccessTable[i] = new IntPair();
		}
		nodesTableAge = 0;
	}
	
	// makeCanonicMaps_Safe
	public void makeCanonicMaps_Safe(TVS structure, Map<Node, Canonic> canonicName, Map<Canonic, Collection<Node>> invCanonicName) {
		assert (canonicName != null && invCanonicName != null);
		
		Collection<Node> nodes = structure.nodes();
		int pred_size = structure.getVocabulary().unaryRel().size();
		int size = nodes.size();
		
		int maxId = 0, index = 0;
		for (Iterator<Node> nodeIt = nodes.iterator(); nodeIt.hasNext(); ) {
			Node node = nodeIt.next();
			if (node.id() > maxId)
				maxId = node.id();
		}
		
		if (this.nodesAccessTable.length < maxId + 1) {
			int newSize = (nodesAccessTable.length * 2 > maxId + 1) ? (nodesAccessTable.length * 2) : (maxId + 1);
			buildNodeAccessTable(newSize);
			//this.nodesTable = new int[newSize];
			//this.nodesTableAges = new int[newSize];
		}
		
		if (this.values.length < size) {
			this.values = new Kleene[size];
			emptyValues = new Kleene[size];
			for (int i = size; i > 0; ) {
				emptyValues[--i] = Kleene.falseKleene;
			}
			this.canonicMap = new Canonic[size];
		}
		
		//int[] nodesTable = this.nodesTable;
		//int[] nodesTableAges = this.nodesTableAges;
		IntPair[] nodesAccessTable = this.nodesAccessTable;
		int age = ++nodesTableAge;
		Kleene[] values = this.values;
		Canonic[] canonicMap = this.canonicMap;
		
		//java.util.Arrays.fill(nodesTable, -1);

		for (Iterator<Node> nodeIt = nodes.iterator(); nodeIt.hasNext(); ) {
			Node node = nodeIt.next();
			canonicMap[index] = new Canonic(pred_size);
			int id = node.id();
			//nodesTable[id] = index++;
			//nodesTableAges[id] = age;
			IntPair pair = nodesAccessTable[id];
			pair.first = index++;
			pair.second = age;
		}
		
		for (Predicate predicate : structure.getVocabulary().unaryRel()) {
			Iterator<Map.Entry<NodeTuple, Kleene>> nodeIt = structure.predicateSatisfyingNodeTuples(predicate, null, null);
			//java.util.Arrays.fill(values, 0, size, Kleene.falseKleene);
			System.arraycopy(emptyValues, 0, values, 0, size);
			int i;
			while (nodeIt.hasNext()) {
				Map.Entry<NodeTuple, Kleene> entry = nodeIt.next();
				Node node = (Node) entry.getKey();
				int id = node.id();
				if (id > maxId)
					continue;
				IntPair pair = nodesAccessTable[id];
				//if (id > maxId || nodesTableAges[id] < age)
				//	continue;
				if (pair.second < age)
					continue;
				i = pair.first;
				//i = nodesTable[id];
				Kleene value = entry.getValue();
				values[i] = value;
			}
			for (i = 0; i < size; ++i) {
				Canonic currentName = (Canonic)canonicMap[i];
				currentName.add(values[i]);
			}
		}
		
		for (Iterator<Node> nodeIt = nodes.iterator(); nodeIt.hasNext();) {
			Node node = nodeIt.next();
			Canonic currentName = (Canonic)canonicMap[nodesAccessTable[node.id()].first];
			canonicName.put(node, currentName);
			Collection<Node> invCollection = invCanonicName.get(currentName);
            if (invCollection == null) {
				invCollection = HashSetFactory.make(structure.nodes().size());
				invCanonicName.put(currentName, invCollection);
			}
			invCollection.add(node);
		}
		
		if (Canonic.CanonicNamesStatistics.doStatistics) {
			for (Canonic c : canonicName.values()) {
				Canonic.CanonicNamesStatistics.allCanonicNames.add(c);
			}
		}		
	}

	// makeCanonicMaps_Simple
	public void makeCanonicMaps_Simple(TVS structure, Map<Node, Canonic> canonicName, Map<Canonic, Set<Node>> invCanonicName) {
		assert (canonicName != null && invCanonicName != null);
		int size = structure.getVocabulary().unaryRel().size();
		for (Node node : structure.nodes()) {
			canonicName.put(node, new Canonic(size));
		}
		
		for (Predicate predicate : structure.getVocabulary().unaryRel()) {
			for (Node node : structure.nodes()) {
				Canonic currentName = canonicName.get(node);
				Kleene value = structure.eval(predicate, node);
				currentName.add(value);
			}
		}
		
		for (Map.Entry<Node, Canonic> entry : canonicName.entrySet()) {
			Node node = entry.getKey();
			Canonic currentName = entry.getValue();
			if (!invCanonicName.containsKey(currentName)) {
                Set<Node> nodes = HashSetFactory.make(structure.nodes().size());
                invCanonicName.put(currentName, nodes);
            }
			invCanonicName.get(currentName).add(node);
		}
		
		if (Canonic.CanonicNamesStatistics.doStatistics) {
			for (Iterator<Canonic> canonicIter = canonicName.values().iterator();
				 canonicIter.hasNext(); ) {
				Canonic c = canonicIter.next();
				Canonic.CanonicNamesStatistics.allCanonicNames.add(c);
			}
		}		
	}

	
	/** Creates canonic and inverse-canonic maps for a blurred structure.
	 */
	public void makeCanonicMapsForBlurred(TVS structure, 
										  Map<Node, Canonic> canonicName, 
										  Map<Canonic, Node> invCanonicName) {
		if (structure instanceof BaseTVS) {
			BaseTVS s = (BaseTVS)structure;
			if (s.getCanonic() != null) {
				canonicName.putAll(s.getCanonic());
				invCanonicName.putAll(s.getInvCanonic());
				return;
			}
		}
		makeCanonicMapForBlurred(structure, canonicName);
		MapInverter.invertMap(canonicName, invCanonicName);
	}
	
	static private final int InitialSize = 10; 
	
	private int[] nodesTable = new int[InitialSize];
	//private int[] nodesTableAges = new int[InitialSize];
	private Kleene[] values = new Kleene[0];
	private Kleene[] emptyValues = new Kleene[0];
	private Canonic[] canonicMap = new Canonic[0];
	private IntPair[] nodesAccessTable;
	private int nodesTableAge = 0;
	
	// makeCanonicMapForBlurred_Current
	public void makeCanonicMapForBlurred(TVS structure, Map<Node, Canonic> canonicName) {
		assert (canonicName != null);
		
        initCanonicMaps(structure);

		updateCanonicNames2(structure, nodesTable, values, canonicMap);

		for (Node node : structure.nodes()) {
			Canonic currentName = (Canonic)canonicMap[nodesTable[node.id()]];
			canonicName.put(node, currentName);
		}
	}


    private void initCanonicMaps(TVS structure) {		
		int size = structure.nodes().size();
		
		int maxId = 0;
		for (Node node : structure.nodes()) {
			if (node.id() > maxId)
				maxId = node.id();
		}
		
		if (this.nodesTable.length < maxId + 1) {
			int newSize = (nodesTable.length * 2 > maxId + 1) ? (nodesTable.length * 2) : (maxId + 1);
			this.nodesTable = new int[newSize];
		}
		
		if (this.values.length < size) {
			this.values = new Kleene[size];
			emptyValues = new Kleene[size];
			for (int i = size; i > 0; ) {
				emptyValues[--i] = Kleene.falseKleene;
			}
			this.canonicMap = new Canonic[size];
		}
    }

	public void makeCanonicMapForBlurred_Safe(TVS structure, Map<Node, Canonic> canonicName) {
		assert (canonicName != null);
		
		Collection<Node> nodes = structure.nodes();
		int pred_size = structure.getVocabulary().unaryRel().size();
		int size = nodes.size();
		
		int maxId = 0, index = 0;
		for (Iterator<Node> nodeIt = nodes.iterator(); nodeIt.hasNext(); ) {
			Node node = nodeIt.next();
			if (node.id() > maxId)
				maxId = node.id();
		}
		
		if (this.nodesAccessTable.length < maxId + 1) {
			int newSize = (nodesAccessTable.length * 2 > maxId + 1) ? (nodesAccessTable.length * 2) : (maxId + 1);
			buildNodeAccessTable(newSize);
			
		}
		
		if (this.values.length < size) {
			this.values = new Kleene[size];
			emptyValues = new Kleene[size];
			for (int i = size; i > 0; ) {
				emptyValues[--i] = Kleene.falseKleene;
			}
			this.canonicMap = new Canonic[size];
		}
		
		IntPair[] nodesAccessTable = this.nodesAccessTable;
		int age = ++nodesTableAge;
		Kleene[] values = this.values;
		Canonic[] canonicMap = this.canonicMap;
		
		for (Iterator<Node> nodeIt = nodes.iterator(); nodeIt.hasNext(); ) {
			Node node = nodeIt.next();
			canonicMap[index] = new Canonic(pred_size);
			int id = node.id();
			IntPair pair = nodesAccessTable[id];
			pair.first = index++;
			pair.second = age;
		}
		
		for (Predicate predicate : structure.getVocabulary().unaryRel()) {
			Iterator<Map.Entry<NodeTuple, Kleene>> nodeIt = structure.predicateSatisfyingNodeTuples(predicate, null, null);
			//java.util.Arrays.fill(values, 0, size, Kleene.falseKleene);
			System.arraycopy(emptyValues, 0, values, 0, size);
			int i;
			while (nodeIt.hasNext()) {
				Map.Entry<NodeTuple, Kleene> entry = nodeIt.next();
				Node node = (Node) entry.getKey();
				int id = node.id();
				if (id > maxId)
					continue;

				IntPair pair = nodesAccessTable[id];
				if (pair.second < age)
					continue;

				i = pair.first;
				Kleene value = entry.getValue();
				values[i] = value;
			}
			for (i = 0; i < size; ++i) {
				Canonic currentName = canonicMap[i];
				currentName.add(values[i]);
			}
		}
		
		for (Iterator<Node> nodeIt = nodes.iterator(); nodeIt.hasNext();) {
			Node node = nodeIt.next();
			Canonic currentName = canonicMap[nodesAccessTable[node.id()].first];
			canonicName.put(node, currentName);
		}
	}

	/** Creates a canonic map for a blurred structure.
	 * @author Roman Manevich.
	 * @since 24.11.2001 Initial creation.
	 */
	// makeCanonicMapForBlurred_Simple
	public void makeCanonicMapForBlurred_Simple(TVS structure, 
										 Map<Node, Canonic> canonicName) {
		int size = structure.getVocabulary().unaryRel().size();
        for (Node node : structure.nodes()) {
			canonicName.put(node, new Canonic(size));
		}
		
        for (Predicate predicate : structure.getVocabulary().unaryRel()) {
            for (Node node : structure.nodes()) {
				Canonic currentName = canonicName.get(node);
				Kleene value = structure.eval(predicate, node);
				currentName.add(value);
			}
		}
	}

	/** Creates a canonic map for a structure.
	 * @author Roman Manevich.
	 * @since 6.1.2002 Initial creation.
	 */
	public void makeCanonicMap(TVS structure, 
							   Map<Node, Canonic> canonicNames) {
		for (Node node : structure.nodes()) {
			Canonic canonicName = new Canonic(structure.getVocabulary().unaryRel().size());
			for (Predicate predicate : structure.getVocabulary().unaryRel()) {
				Kleene value = structure.eval(predicate, node);
				canonicName.add(value);
			}
			canonicNames.put(node, canonicName);
		}
	}

	/** Create canonical maps from all unary predicates.
	 * 
	 * @param structure A bounded structure.
	 * @param canonicNames An output map from nodes to Canonical names.
	 * @param invCanonicName An output map from Canonical names to nodes.
	 * @author Roman Manevich
	 * @since September 11, 2009
	 */
	public void makeFullUnaryMap(TVS structure, Map<Node, Canonic> canonicNames,
			Map<Canonic, Node> invCanonicName) {
		for (Node node : structure.nodes()) {
			Canonic canonicName = new Canonic(structure.getVocabulary()
					.unary().size());
			for (Predicate predicate : structure.getVocabulary().unary()) {
				Kleene value = structure.eval(predicate, node);
				canonicName.add(value);
			}
			canonicNames.put(node, canonicName);
		}
		MapInverter.invertMap(canonicNames, invCanonicName);
	}
}
