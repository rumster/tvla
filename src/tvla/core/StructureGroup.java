package tvla.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import tvla.core.decompose.DecompositionName;
import tvla.exceptions.SemanticErrorException;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;

/**
 * Group of structure represented by a single structure.
 * For each member, a mapping is kept between the member's nodes and the representative
 * nodes. The name of the component the member came from can also be recorded.
 */
public class StructureGroup {
	/**
	 * A member of a structure group. Immutable.
	 */
	public static class Member {
		public final static boolean SAFETY_CHECKS = true;
		
		protected Map<Node, Node> mapping;
		protected HighLevelTVS structure;
		protected DecompositionName component;
		
		public Member(HighLevelTVS structure, Map<Node, Node> mapping, DecompositionName component) {
			super();
			this.component = component;
			this.mapping = mapping;
			this.structure = structure;
			if (SAFETY_CHECKS) {
				if (!HashSetFactory.make(mapping.values()).equals(HashSetFactory.make(structure.nodes()))) {
					throw new RuntimeException("Sanity check failed - mapping not surjective");
				}
			}
		}			

		/**
		 * Utility function to build an identity mapping between the structure's nodes 
		 * and themselves.
		 */
		public static Map<Node, Node> buildIdentityMapping(HighLevelTVS structure) {
			Map<Node, Node> nodeMapping = HashMapFactory.make();
			for (Node node : structure.nodes()) {
				nodeMapping.put(node, node);
			}
			return nodeMapping;
		}
		
		public Map<Node, Node> getMapping() {
			return mapping;
		}

		public HighLevelTVS getStructure() {
			return structure;
		}

		public DecompositionName getComponent() {
			return component;
		}

		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Structure: ").append(structure).append("\n");
			builder.append("Mapping: ").append(mapping).append("\n");
			builder.append("Component: ").append(component).append("\n");
			return builder.toString();
		}
	}
	
	/**
	 * Member of the group
	 */
	protected Collection<Member> members;
	/**
	 * Their representative
	 */
	protected HighLevelTVS representative;
	protected boolean frozen = false;

	public StructureGroup(HighLevelTVS representative) {
		super();
		this.members = new ArrayList<Member>();
		this.representative = representative;
	}	
	
	protected StructureGroup(HighLevelTVS representative, Collection<Member> members) {
		super();
		this.members = members;
		this.representative = representative;
	}	
	
	public void addMember(Member member) {
		if (frozen) throw new RuntimeException("Structure group is already frozen!");
		this.members.add(member);
	}

	/**
	 * Add the members of the structure group as components. Node mappings are composed
	 * with the new mapping. The component can be set if the group members didn't have one set. 
	 */
	public void addMember(StructureGroup group, Map<Node, Node> nodeMapping, DecompositionName component) {
		if (group == null) return;
		for (StructureGroup.Member member : group.members) {
			DecompositionName composedComponent = component;
			if (component != member.component) {
				if (component != null && member.component != null) {
					throw new SemanticErrorException("Trying to associate member to multiple components: " + component + " and " + member.component);
				}
				if (composedComponent == null) {
					composedComponent = member.component;
				}
			}
			Map<Node, Node> composedMapping = compose(nodeMapping, member.mapping);
			addMember(new Member(member.structure, composedMapping, composedComponent));
		}
	}

	/**
	 * Add a structure as a member to the group with the given nodeMapping and component.
	 * If the structure already represents a group, the group is added instead.
	 */
	public void addMember(HighLevelTVS structure, Map<Node, Node> nodeMapping, DecompositionName component) {
		if (structure.getStructureGroup() == null) {
			addMember(new Member(structure, nodeMapping, component));			
		} else {
			addMember(structure.getStructureGroup(), nodeMapping, component);
		}
	}

	public Iterable<Member> getMembers() {
		return members;
	}

	public HighLevelTVS getRepresentative() {
		return representative;
	}

	/**
	 * Utility function to compose two maps (function composition)
	 */
	private static <T> Map<T, T> compose(Map<T, T> left, Map<T, T> right) {
		Map<T, T> composed = HashMapFactory.make();
		for (T from : left.keySet()) {
			T tmp = left.get(from);
			T to = right.get(tmp);
			if (to == null) {
				throw new RuntimeException("Missing member in mapping composition " + from + " goes to " + tmp + " which has no mapping");
			}
			composed.put(from, to);
		}
		return composed;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Group Representative: ");
		builder.append(representative);
		builder.append("\nGroup Members: ");		
		builder.append(members);
		return builder.toString();
	}

	public StructureGroup copy(HighLevelTVS newRepresentative) {
		// mark as frozed and return this.
		StructureGroup copy = new StructureGroup(newRepresentative, this.members);
		copy.frozen = this.frozen  = true;		
		return copy;
	}
}
