package tvla.language.BUC;

import tvla.language.TVP.PredicateAST;

public class BuchiTransitionAST 
{
	protected BuchiStateAST source,target;
	protected PredicateAST label;
	
	public BuchiTransitionAST(BuchiStateAST src, PredicateAST lab, BuchiStateAST tgt) {
		source = src;
		label = lab;
		target = tgt;
	}

	public BuchiStateAST source() {
		return source;
	}
	
	public PredicateAST label() {
		return label;
	}
	
	public BuchiStateAST target() {
		return target;
	}
		
}
