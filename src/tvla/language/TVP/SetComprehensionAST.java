package tvla.language.TVP;

import java.util.List;
import java.util.Set;

import tvla.util.HashSetFactory;

public class SetComprehensionAST extends SetAST {
    protected SetAST set;
    protected List<PredicateAST> expr;
    protected String id;
    
    public SetComprehensionAST(List<PredicateAST> expr, String id, SetAST set) {
        this.expr = expr;
        this.id = id;
        this.set = set;
    }

    public SetComprehensionAST(SetComprehensionAST other) {
        this.expr = copyList(other.expr);
        this.id = other.id;
        this.set = other.set.copy();
    }

    @Override
    public Set<PredicateAST> getMembers() {
        Set<PredicateAST> members = HashSetFactory.make();
        for (PredicateAST member : set.getMembers()) {
            for (PredicateAST predicate : expr) {
                PredicateAST copy = predicate.copy();
                copy.substitute(id, member);
                members.add(copy);
            }
        }
        return members;
    }

    @Override
    public SetComprehensionAST copy() {
        return new SetComprehensionAST(this);
    }

    @Override
    public void substitute(PredicateAST from, PredicateAST to) {
        this.set.substitute(from, to);
        for (PredicateAST predicate : expr) {
            predicate.substitute(from, to);
        }
    }

}
