package tvla.language.TVP;

import java.util.List;
import java.util.Set;

import tvla.core.Constraints;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;

public class PredicateDefAST extends PredicateAST {

    protected Set<Kleene> attr;
    protected PredicatePropertiesAST properties;

    /** @author Tal Lev-Ami
     * @since 6.5.2001 Nullary predicates are now abstraction by default (Roman).
     */
    public PredicateDefAST(
        String name,
        List<PredicateAST> params,
        PredicatePropertiesAST props,
        Set<Kleene> attr,
        int arity) {
        super(name, params);

        this.attr = attr;
        this.arity = arity;
        if (props != null) {
            this.properties = props;
        } else {
            this.properties = new PredicatePropertiesAST();
        }

        if (arity > 1) {
            // the default is abstraction = true
            properties.setAbstraction(false);
        }

        properties.validate(generateName(), arity);
    }

    public PredicateDefAST(String name, List<PredicateAST> params) {
        super(name, params);
    }

    public PredicateDefAST(PredicateDefAST other) {
        super(other);
        this.arity = other.arity;
        this.properties = new PredicatePropertiesAST(other.properties);
        this.attr = other.attr;
    }

    public PredicateDefAST copy() {
        return new PredicateDefAST(this);
    }
    

    /**
     * set predicate's show attributes
     * @param predicate - predicate to be updated
     */
    protected void setAttr(Predicate predicate) {
    	if (attr != null) {
    		boolean showTrue = false;
    		boolean showUnknown = false;
    		boolean showFalse = false;
    		if (attr.contains(Kleene.trueKleene)) {
    			showTrue = true;
    		}
    		if (attr.contains(Kleene.unknownKleene)) {
    			showUnknown = true;
    		}
    		if (attr.contains(Kleene.falseKleene)) {
    			showFalse = true;
    		}
    		predicate.setShowAttr(showTrue, showUnknown, showFalse);
    	}
    	predicate.setShowAttr(properties.pointer());
    }

    /**
     * generate the predicate
     * @param predicate - predicate to be updated from AST
     */
    protected void generatePredicate(Predicate predicate) {
    	setAttr(predicate);
    	if (properties.unique()) {
    		predicate.unique(true);
    	}
    	if (properties.function()) {
    		predicate.function(true);
    	}
    	if (properties.invfunction()) {
    		predicate.invfunction(true);
    	}
    	if (properties.reflexive()) {
    		predicate.reflexive(true);
    	}
    	if (properties.acyclic()) {
    		predicate.acyclic(true);
    	}
    	PredicateAST ccPredicate = properties.uniquePerCCofPred();
    	if (ccPredicate != null) {
    		predicate.uniquePerCCofPred(ccPredicate.getPredicate());
    	}
    	if (Constraints.automaticConstraints) {
    		properties.generateConstraints(predicate);
    	}
    }

    /**
     * predicate show attributes as string
     * @return string representing predicate show attributes
     */
    public String showAttrToString() {
    	StringBuffer result = new StringBuffer();
    
    	if (attr != null) {
    		boolean prev = false;
    
    		result.append("{");
    		if (attr.contains(Kleene.trueKleene)) {
    			result.append(Kleene.trueKleene.toString());
    			prev = true;
    		}
    		if (attr.contains(Kleene.unknownKleene)) {
    			result.append((prev ? "," : ""));
    			result.append(Kleene.unknownKleene.toString());
    			prev = true;
    
    		}
    		if (attr.contains(Kleene.falseKleene)) {
    			result.append((prev ? "," : ""));
    			result.append(Kleene.falseKleene.toString());
    		}
    		result.append("}");
    	}
    	return result.toString();
    }
}
