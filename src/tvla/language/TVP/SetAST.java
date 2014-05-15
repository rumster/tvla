package tvla.language.TVP;

import java.util.Set;

/** A base class for sets.
 * @author Tal Lev-Ami.
 */
public abstract class SetAST extends AST {
    abstract public Set<PredicateAST> getMembers();
    abstract public SetAST copy();
}