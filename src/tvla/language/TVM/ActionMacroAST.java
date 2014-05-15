package tvla.language.TVM;

import java.util.List;

public class ActionMacroAST extends tvla.language.TVP.ActionMacroAST {
	public ActionMacroAST(String name, List args, ActionDefAST def) {
		super(name, args, def);
	}

	public void generate() {
		def.generate();
	}	
	
	public tvla.language.TVP.ActionDefAST expand(List actualArgs) {
		if (actualArgs.size() != args.size()) {
			throw new RuntimeException("For action " + name + " need " +
				args.size() + " args, but got " + 
				actualArgs.size());
		}
		ActionDefAST newDef = (ActionDefAST) def.copy();
		for (int i = 0; i < args.size(); i++) {
			newDef.substitute((String) args.get(i), (String) actualArgs.get(i));
		}
		//newDef.evaluate();
		return newDef;
	}
}
