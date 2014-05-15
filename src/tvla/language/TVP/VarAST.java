package tvla.language.TVP;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tvla.formulae.Var;

public class VarAST {
	public static List asVariables(List asStrings) {
		List asVariables = new ArrayList();
		for (Iterator i = asStrings.iterator(); i.hasNext(); ) {
			String str = (String) i.next();
			asVariables.add(new Var(str));
		}
		return asVariables;
	}
}