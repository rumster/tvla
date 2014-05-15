package tvla.language.TVM;

import java.util.Iterator;
import java.util.List;

import tvla.language.TVP.AST;
import tvla.language.TVP.ForeachAST;
import tvla.language.TVP.PredicateAST;

public class DeclarationsAST extends AST {
	
	private List declarationList;
	
	public DeclarationsAST(List list)
	{
		declarationList = list;
	}
	
	public void generate() {
		for (Iterator i = declarationList.iterator(); i.hasNext(); ) {
			AST ast = (AST) i.next();
			if (ast instanceof ForeachAST) {
				ForeachAST foreach = (ForeachAST) ast;
				for (Iterator j = foreach.evaluate().iterator(); j.hasNext(); ) {
					AST generatedAst = (AST) j.next();
					generatedAst.generate();
				}
			} 
			else {
				ast.generate();
			}
		}
	}
	
	public void compile() {		
	}
	
	public AST copy() {
		throw new RuntimeException("Can't copy declarations.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute declarations.");
	}
}
