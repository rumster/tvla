package tvla.core.decompose;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collections;
import java.util.Map.Entry;

import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.NodeTuple;
import tvla.core.base.PredicateEvaluator;
import tvla.formulae.Formula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.Var;
import tvla.logic.Kleene;
import tvla.predicates.DynamicVocabulary;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashConsFactory;
import tvla.util.HashSetFactory;

/**
 * A decomposition name for a disjunction of positive predicates represented by
 * a set of predicates.
 * 
 * @author tla
 * 
 */
public class PClauseName implements DecompositionName {
    private static final Node[] EMPTY_NODE_ARRAY = new Node[0];
	protected static HashConsFactory<PClauseName, PClauseNameKey> factory = new HashConsFactory<PClauseName, PClauseNameKey>() {    
        @Override
        protected PClauseName actualCreate(PClauseNameKey key) {
            PClauseName result = new PClauseName(key.disjuncts, key.killVocabulary, key.prettyName, key.abstraction, key.base);
            result.init();
            return result;
        }    
    };    
    protected static HashConsFactory<PClauseName, PClauseNameKey>.CommBinOp<PClauseName> compose = factory.new CommBinOp<PClauseName>() {
        @Override
        protected PClauseName actualApply(PClauseName left, PClauseName right) {
            DynamicVocabulary newDisjuncts = left.disjuncts.union(right.disjuncts); 
            DynamicVocabulary newVoc = left.killVocabulary.intersection(right.killVocabulary);
            String prettyName= left.prettyName + "+" + right.prettyName;
            Set<DecompositionName> base = HashSetFactory.make();
            base.addAll(left.getBase());
            base.addAll(right.getBase()); 
            return PClauseName.create(newDisjuncts, newVoc, prettyName, left.isAbstraction() || right.isAbstraction(), base);
        }    
    };

    protected final DynamicVocabulary disjuncts;
    protected final DynamicVocabulary killVocabulary;
    protected Formula formula;
    protected String prettyName;
    protected Set<DecompositionName> base;
    protected boolean abstraction;

    private static class PClauseNameKey extends PClauseName {
        public PClauseNameKey(DynamicVocabulary disjucts, DynamicVocabulary vocabulary,
                String prettyName, boolean abstraction, Set<DecompositionName> base) {
            super(disjucts, vocabulary, prettyName, abstraction, base);
        }
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PClauseNameKey) {
                PClauseNameKey other = (PClauseNameKey) obj;                
                return this.disjuncts.equals(other.disjuncts) && this.killVocabulary.equals(other.killVocabulary) &&
                	this.abstraction == other.abstraction;
            } else {
                return false;
            }
        }
        @Override
        public int hashCode() {
            return disjuncts.hashCode() * 231 + killVocabulary.hashCode() * 31 + (abstraction ? 1201 : 1203);
        }
    }
    
    public PClauseName(DynamicVocabulary disjucts, DynamicVocabulary vocabulary, String prettyName, boolean abstraction, Set<DecompositionName> base) {
        this.disjuncts = disjucts;
        this.killVocabulary = vocabulary;
        this.prettyName = prettyName;
        if (this.prettyName == null) {
            this.prettyName = toUglyName();
        }
		this.abstraction = abstraction;
		this.base = base;
    }

    protected void init() {
		if (base == null) {
		    this.base = Collections.singleton((DecompositionName) this);
		}
    }

    public static PClauseName create(DynamicVocabulary name, DynamicVocabulary vocabulary, String prettyName, boolean abstraction, Set<DecompositionName> base) {
        PClauseName result = factory.create(new PClauseNameKey(name, vocabulary, prettyName, abstraction, base));
		assert result.killVocabulary == vocabulary;
		assert result.abstraction == abstraction;
		assert base == null || result.base.equals(base);
		return result;
    }

    public DynamicVocabulary getKillVocabulary() {
        return killVocabulary;
    }

    public String toString() {
        return prettyName;
    }
    
    public String toUglyName() {
        StringBuilder builder = new StringBuilder();
        String sep = "(";
        for (Predicate predicate : new TreeSet<Predicate>(disjuncts.all())) {
            builder.append(sep);
            builder.append(predicate);
            sep = "|";
        }
        if (disjuncts.all().isEmpty()) {
            builder.append("0");
        } else {
            builder.append(")");
        }
        sep = "&amp;(";
        for (Predicate predicate : new TreeSet<Predicate>(killVocabulary.all())) {
            builder.append(sep);
            builder.append("!").append(predicate);
            sep = "&amp;";
        }
        if (killVocabulary.all().isEmpty()) {
            builder.append(")");
        }
        return builder.toString();
    }

    public DecompositionName compose(DecompositionName o) {
        assert o instanceof PClauseName;
        return compose.apply(this, (PClauseName) o);
    }

    public DynamicVocabulary getDisjuncts() {
        return disjuncts;
    }

    public boolean canDecompose(HighLevelTVS structure, boolean ignoreOutside) {
        if (OverlapDecomposer.allowUnknown)
            return disjuncts.subsetof(structure.getVocabulary());
    	Node outside = null;
    	if (ignoreOutside) {
	    	Iterator<Entry<NodeTuple, Kleene>> outsideIt = structure.iterator(Vocabulary.outside);
	    	assert outsideIt.hasNext();
	    	outside = (Node) outsideIt.next().getKey();
	    	assert !outsideIt.hasNext();
    	}    	
        for (Predicate disjunct : disjuncts.all()) {
        	Iterator<Entry<NodeTuple, Kleene>> unknownIter = structure.predicateSatisfyingNodeTuples(disjunct, EMPTY_NODE_ARRAY, Kleene.unknownKleene);
        	while (unknownIter.hasNext()) {
        		Node node = (Node) unknownIter.next().getKey();
        		if (node.equals(outside))
        			continue;
        		else {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean canDecomposeFrom(DecompositionName other) {
        if (other instanceof PClauseName) {
            PClauseName otherName = (PClauseName) other;
            return this.disjuncts.subsetof(otherName.disjuncts) && otherName.killVocabulary.subsetof(this.killVocabulary);
        } else {
            return false;
        }
    }


    public boolean contains(DecompositionName other) {
        if (other instanceof PClauseName) {
            PClauseName otherName = (PClauseName) other;
	    return getBase().containsAll(other.getBase());
            //return otherName.disjuncts.subsetof(this.disjuncts);
        } else {
            return false;
        }
    }

    public boolean isAbstraction() {
    	return abstraction;
    }

    public Set<DecompositionName> getBase() {
    	return base;
    }

    public Formula getFormula() {
        if (formula != null) {
            List<Formula> disjuncts = new ArrayList<Formula>();
            Var v = new Var("v");
            for (Predicate predicate : this.disjuncts.all()) {
                disjuncts.add(new PredicateFormula(predicate, v));
            }
            formula = Formula.orAll(disjuncts);
        }
        return formula;
    }
}
