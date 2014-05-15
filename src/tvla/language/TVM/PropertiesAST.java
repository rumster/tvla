package tvla.language.TVM;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;

public class PropertiesAST extends AST {
	
	private List propertyList;
	
	public PropertiesAST() {
		propertyList = new ArrayList();
	}
	
	public PropertiesAST(List list) {
		propertyList = list;
	}
	
	public void addProperty(GlobalActionAST action) {
		propertyList.add(action);
	}
	
	public void generate() {
		for (Iterator i = propertyList.iterator(); i.hasNext();) {
			GlobalActionAST ga = (GlobalActionAST)i.next();
			ga.generate();
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