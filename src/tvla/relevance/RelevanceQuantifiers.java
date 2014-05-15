/*
 * Created on Sep 11, 2003
 */
package tvla.relevance;

import java.util.Collections;
import java.util.List;

/**
 * @author Eran Yahav eyahav
 */
public class RelevanceQuantifiers {
	/**
	 * list of quantifier ordered from outermost quantifier to innermost one
	 */
	private List quantifierOrder;
	/**
	 * sets the quantifier-order list to the given order.
	 * @param order - a list of ordered quantifiers
	 */
	public void setOrder(List order) {
		this.quantifierOrder = order;
		System.out.println("Quantifier Order set to: " + order.toString());
	}

	/**
	 * gets the list of quantifier order
	 * @return unmodifiable-wrapped quantifier order list.
	 */
	public List getOrder() {
		return Collections.unmodifiableList(quantifierOrder);
	}

}
