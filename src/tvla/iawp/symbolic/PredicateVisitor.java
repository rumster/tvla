/*
 * Created on Jun 27, 2004
 *
 */
package tvla.iawp.symbolic;

import java.util.Collection;

import tvla.formulae.Formula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.TransitiveFormula;
import tvla.util.HashSetFactory;

/**
 * @author gretay
 *
 */
public class PredicateVisitor extends InPlaceVisitor {
	public static Collection getAllPredicates(Formula f) {
		PredicateVisitor visitor = new PredicateVisitor();
		f.visit(visitor);
		return visitor.getSet();		
	}
		
	private Collection set =  HashSetFactory.make();
	public Collection getSet(){
		return set;
	}
	public Object accept(PredicateFormula f) {
		set.add(f.predicate());
		return null;
	}
	public Object accept(TransitiveFormula f) {
		f.subFormula().visit(this);
		return null;
	}
}
