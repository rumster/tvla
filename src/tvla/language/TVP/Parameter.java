package tvla.language.TVP;

import java.util.List;

import tvla.exceptions.SemanticErrorException;

public class Parameter extends AST {
    private String parametricId;
    private PredicateAST actualId;

    public Parameter(String parametricId, SetAST set, List<Parameter> prev) {
        this.parametricId = parametricId;
        for (Parameter parameter : prev) {
            set.substitute(parameter.parametricId, parameter.actualId);
        }
        this.actualId = set.getMembers().iterator().next();        
    }

    public Parameter(String parametricId, PredicateAST actualId) {
        this.parametricId = parametricId;
        this.actualId = actualId;        
    }

    @Override
    public AST copy() {
        return new Parameter(parametricId, actualId);
    }

    @Override
    public void substitute(PredicateAST from, PredicateAST to) {
        if (from.isSimple() && from.name.equals(parametricId)) {
            throw new SemanticErrorException("Trying to substitute parametric Id");
        }
    }

    public PredicateAST getActualId() {
        return this.actualId;
    }

    public String getParametricId() {
        return this.parametricId;
    }

}
