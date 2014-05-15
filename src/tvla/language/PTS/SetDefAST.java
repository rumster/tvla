package tvla.language.PTS;

import java.util.ArrayList;
import java.util.List;

import tvla.language.TVP.PredicateAST;

public class SetDefAST extends tvla.language.TVP.SetDefAST {
	final private static boolean debug = false;
	
	static SetDefAST labels  = new SetDefAST(new String("Labels"),new ArrayList<PredicateAST>());

	public static void addLabels(List<String> newLabels) {
		labels.addMembers(newLabels);
		if (debug) 
			System.err.println("Added Labels: " + labels.members);
	}
	
	public SetDefAST(String name, List<PredicateAST> members) {
		super(name, members);
	}
	
	public void addMembers(List<String> newMembers) {
	    List<PredicateAST> predicates = PredicateAST.asPredicates(newMembers);
		members.removeAll(predicates);
		members.addAll(predicates);
	}
}