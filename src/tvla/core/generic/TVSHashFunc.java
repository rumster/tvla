package tvla.core.generic;

import java.util.*;

import gnu.trove.TObjectHashingStrategy;
import tvla.core.Canonic;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.meet.Meet;
import tvla.logic.Kleene;
import tvla.predicates.*;

/** Computes a hash function for structures and compares
 * pairs of structures.
 * The hash function works by first blurring the input
 * structure and then computing the hash value from the
 * nullary predicates, the ordered set of canonical names and 
 * the values of binary predicates between ordered pairs of
 * canonical names.
 * Structure equality is determined by a general isomorphism
 * checking procedure or a more efficient check for bounded
 * structures, depending on an argument passed to the constructor.
 * 
 * @author Roman Manevich
 * @since August 29, 2009
 */
@SuppressWarnings("serial")
public class TVSHashFunc implements TObjectHashingStrategy<HighLevelTVS> {
	public static int equalityChecks;
	public static int hashCodes;
	private static GenericBlur blur = new GenericBlur();
	
	/** A reusable instance for hashing and equating bounded structures.
	 */
	public static final TVSHashFunc boundedTVSHashFunc = new TVSHashFunc(true);

	/** A reusable instance for hashing and equating general
	 * (possibly unbounded) structures.
	 */
	public static final TVSHashFunc generalTVSHashFunc = new TVSHashFunc(false);
	
	private final boolean bounded;
	
	public static void reset() {
		blur = new GenericBlur();
	}
	
	/** Constructs a hash function functor for structures.
	 * 
	 * @param bounded Specified whether the methods of this
	 * object will be called with bounded structures.
	 */
	public TVSHashFunc(boolean bounded) {
		this.bounded = bounded;
	}
	
	/** Computes a hash value by first blurring a copy
	 * of the structure and then hashing the values of all
	 * predicate evaluations.
	 */
	public int computeHashCode(HighLevelTVS structure) {
		++hashCodes;
		HighLevelTVS workingStruc = structure;
		if (!bounded) {
			workingStruc = structure.copy();
			workingStruc.blur();
		}
		
		// We store the canonical names in an ordered (TreeMap)
		// map to make sure that different, isomorphic, structures
		// give the same results.
		Map<Node, Canonic> canonicName = new TreeMap<Node,Canonic>();
		Map<Canonic, Node> invCanonicName = new TreeMap<Canonic, Node>();
		if (bounded)
			blur.makeCanonicMapsForBlurred(workingStruc, canonicName, invCanonicName);
		else
			blur.makeFullUnaryMap(workingStruc, canonicName, invCanonicName);
		canonicName = null; // unused
		
		int result = 0;
		for (Predicate nullary : Vocabulary.allNullaryPredicates()) {
			Kleene val = workingStruc.eval(nullary);
			result = result * 31 + val.kleene() * nullary.id(); 
		}
		
		for (Canonic canonic : invCanonicName.keySet()) {
			result = result * 31 + canonic.hashCode();
		}
		
		for (Map.Entry<Canonic, Node> entry1 : invCanonicName.entrySet()) {
			Node left = entry1.getValue();
			for (Map.Entry<Canonic, Node> entry2 : invCanonicName.entrySet()) {
				Node right = entry2.getValue();
				for (Predicate binary : Vocabulary.allBinaryPredicates()) {
					Kleene val = workingStruc.eval(binary, left, right);
					result = result * 31 + val.kleene() * binary.id();
				}
			}
		}
		
//		for (Canonic canonicleft : invCanonicName.keySet()) {
//			Node left = invCanonicName.get(canonicleft);
//			for (Canonic canonicRight : invCanonicName.keySet()) {
//				Node right = invCanonicName.get(canonicRight);
//				boolean in = invCanonicName.containsKey(canonicRight);
//				for (Predicate binary : Vocabulary.allBinaryPredicates()) {
//					Kleene val = workingStruc.eval(binary, left, right);
//					result = result * 31 + val.kleene() * binary.id();
//				}				
//			}
//		}	
		
		return result;
	}
	
	public boolean equals(HighLevelTVS left, HighLevelTVS right) {
//		boolean l = Meet.isEmbedded(left, right);
//		boolean r = Meet.isEmbedded(right, left);
//		if ((l||r) && !Meet.isomorphic(left, right))
//			System.out.println("found true embedding");
		
		equalityChecks++;
		if (left.nodes().size() != right.nodes().size())
			return false;
		
		//if (!bounded) {
			return Meet.isomorphic(left, right);
		//}
		
//		// check isomorphism between bounded structures
//		GenericBlur blur = new GenericBlur();
//		Map<Node, Canonic> leftCanonicName = new TreeMap<Node,Canonic>();
//		Map<Canonic, Node> leftInvCanonicName = new TreeMap<Canonic, Node>();
//		blur.makeCanonicMapsForBlurred(left, leftCanonicName, leftInvCanonicName);
//
//		Map<Node, Canonic> rightCanonicName = new TreeMap<Node,Canonic>();
//		Map<Canonic, Node> rightInvCanonicName = new TreeMap<Canonic, Node>();
//		blur.makeCanonicMapsForBlurred(right, rightCanonicName, rightInvCanonicName);
//		
//		if (!leftInvCanonicName.keySet().equals(rightInvCanonicName.keySet()))
//			return false;
//		
//		for (Predicate binary : Vocabulary.allBinaryPredicates()) {
//			if (left.numberSatisfy(binary) != right.numberSatisfy(binary))
//				return false;
//			
//			for (Map.Entry<Canonic, Node> leftCanonicNode1 : leftInvCanonicName
//					.entrySet()) {
//				Canonic canonic1 = leftCanonicNode1.getKey();
//				Node leftNode1 = leftCanonicNode1.getValue();
//				Node rightNode1 = rightInvCanonicName.get(canonic1);
//				assert rightNode1 != null;
//				for (Map.Entry<Canonic, Node> leftCanonicNode2 : leftInvCanonicName
//						.entrySet()) {
//					Canonic canonic2 = leftCanonicNode2.getKey();
//					Node leftNode2 = leftCanonicNode2.getValue();
//					Node rightNode2 = rightInvCanonicName.get(canonic2);
//					assert rightNode2 != null;
//					Kleene leftVal = left.eval(binary, leftNode1, leftNode2);
//					Kleene rightVal = right
//							.eval(binary, rightNode1, rightNode2);
//					if (leftVal != rightVal)
//						return false;
//				}
//			}
//		}
		
//		return true;
	}
}