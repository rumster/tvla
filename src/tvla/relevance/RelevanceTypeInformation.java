/*
 * Created on Sep 11, 2003
 */
package tvla.relevance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;

/**
 * @author Eran Yahav eyahav
 */
public class RelevanceTypeInformation {

	/**
	 * a map from component name to componentType
	 */
	private Map componentTypes = HashMapFactory.make();
	/**
	 * add a new component
	 * @param name - component name
	 * @param extendsId - component superclass
	 * @param implementsIds - implemented interfaces
	 * @return new ComponentType object
	 */
	public ComponentType addComponent(
		String name,
		String extendsId,
		List implementsIds) {
		ComponentType c = (ComponentType) componentTypes.get(name);
		if (c == null) {
			c = new ComponentType(name, extendsId, implementsIds);
			componentTypes.put(name, c);
			if (!extendsId.equals("")) {
				ComponentType exComp =
					addComponent(extendsId, "", new ArrayList());
				exComp.addDerivedComponent(name);
			}

			if (!implementsIds.isEmpty()) {
				for (Iterator it = implementsIds.iterator(); it.hasNext();) {
					String curr = (String) it.next();
					ComponentType impComp =
						addComponent(curr, "", new ArrayList());
					impComp.addDerivedComponent(name);
				}
			}
		} else {
			if (c.extendsId.equals("") && !extendsId.equals("")) {
				c.extendsId = extendsId;
				ComponentType exComp =
					addComponent(extendsId, "", new ArrayList());
				exComp.addDerivedComponent(name);
			}
			if (!c.implementsIds.containsAll(implementsIds)) {
				for (Iterator it = implementsIds.iterator(); it.hasNext();) {
					String curr = (String) it.next();
					if (c.implementsIds.contains(curr)) {
						addComponent(curr, "", new ArrayList());
						ComponentType impComp =
							(ComponentType) componentTypes.get(curr);
						impComp.addDerivedComponent(name);
					}
				}
			}
		}

		System.out.println(
			"Added component:"
				+ name
				+ ", "
				+ extendsId
				+ ", "
				+ implementsIds.toString());
		return c;
	}

	/**
	 * get a collection of components derived from the given component name
	 * @param name - component name
	 * @return collection of derived components
	 */
	public Collection getDerivedComponents(String name) {
		Set result = HashSetFactory.make();

		result.add(name);
		ComponentType curr = (ComponentType) componentTypes.get(name);
		if (curr != null) {
			for (Iterator it = curr.derivedComponents.iterator();
				it.hasNext();
				) {
				String child = (String) it.next();
				result.addAll(getDerivedComponents(child));
			}
		}

		return result;
	}

	/**
	 * representing a component (class or interface)
	 * @author Eran Yahav yahave
	 */
	public class ComponentType {
		/** component name */
		private String name;
		/** name of superclass */
		private String extendsId;
		/** list of Strings corresponding to component names */
		private List implementsIds = new ArrayList();
		/** set of components derived from this component */
		private Set derivedComponents = HashSetFactory.make();

		/**
		 * create a new ComponentType
		 * @param componentName - component name
		 * @param componentExtendsId - component superclass
		 * @param componentImplementsIds - interfaces implemented by this component
		 */
		public ComponentType(
			String componentName,
			String componentExtendsId,
			List componentImplementsIds) {
			this.name = componentName;
			this.extendsId = componentExtendsId;
			this.implementsIds = componentImplementsIds;
		}
		/**
		 * add a derived component
		 * @param aName - name of derived component
		 */
		public void addDerivedComponent(String aName) {
			derivedComponents.add(aName);
		}
		/**
		 * return a hashcode value for the component type
		 * @return component type hashcode
		 */
		public int hashCode() {
			return name.hashCode();
		}
		/**
		 * is the current component equal to other?
		 * @param other - object to be compared with
		 * @return true if equal
		 */
		public boolean equals(Object other) {
			if (!(other instanceof ComponentType)) {
				return false;
			}
			ComponentType otherComponent = (ComponentType) other;
			return (
				name.equals(otherComponent.name)
					&& extendsId.equals(otherComponent.extendsId)
					&& implementsIds.equals(otherComponent.implementsIds));
		}
		/**
		 * human readable representation of the component
		 * @return string representing component
		 */
		public String toString() {
			return "\n :::"
				+ name
				+ "\n"
				+ " extends "
				+ extendsId
				+ "\n"
				+ " implements "
				+ implementsIds.toString()
				+ "\n"
				+ " derived "
				+ derivedComponents.toString();
		}

	}

}
