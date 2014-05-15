package tvla.language.PTS;

import java.util.List;
import java.util.Map;

import tvla.analysis.Engine;
import tvla.analysis.multithreading.MultithreadEngine;
import tvla.analysis.multithreading.ProgramMethodBody;
import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;
import tvla.util.HashMapFactory;

public class BodyDefAST extends AST {
	
	private final static boolean xdebug = false;
	
	/** 
	 * map of (bodyName,bodyDefAST)
	 */
	private static Map bodies = HashMapFactory.make();
	
	/** method signature (full name)  */
	protected String sig;
	/** a list of ActionASTs */
	protected List actions;
	/** the generated program thread */
	protected ProgramMethodBody programBody;
		
	
	public BodyDefAST(String sig, List actions)
	{
		this.sig = sig;
		this.actions = actions;
		bodies.put(sig,this);
		if (xdebug)
			System.err.println("BodyDefAST created for: " + sig);
	}
	
	public static BodyDefAST get(String sig) {
		BodyDefAST bd = (BodyDefAST) bodies.get(sig);
		if (bd == null) 
			throw new RuntimeException("Unknown body sig " + sig);
		return bd;
	}
	
	public void generate() {
		MultithreadEngine engine = (MultithreadEngine) Engine.activeEngine;
		programBody = engine.addMethodDefinition(sig,actions);
	}
	
	public void compile() {
		MultithreadEngine engine = (MultithreadEngine) Engine.activeEngine;
		engine.compileBodyDefinition(sig);
	}
	
	public ProgramMethodBody getBody() {
		return programBody;
	}
	
	public AST copy() {
		throw new RuntimeException("Can't copy thread definitions.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute thread definitions.");
	}
}
