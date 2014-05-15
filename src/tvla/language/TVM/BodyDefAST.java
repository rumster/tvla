package tvla.language.TVM;

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
	
	/** thread name  */
	protected String name;
	/** a list of ActionASTs */
	protected List actions;
	/** the generated program thread */
	protected ProgramMethodBody programBody;
		
	
	public BodyDefAST(String name, List actions)
	{
		this.name = name;
		this.actions = actions;
		bodies.put(name,this);
		if (xdebug)
			System.err.println("BodyDefAST created for: " + name);
	}
	
	public static BodyDefAST get(String name) {
		BodyDefAST bd = (BodyDefAST) bodies.get(name);
		if (bd == null) 
			throw new RuntimeException("Unknown body name " + name);
		return bd;
	}
	
	public void generate() {
		MultithreadEngine engine = (MultithreadEngine) Engine.activeEngine;
		programBody = engine.addMethodDefinition(name,actions);
	}
	
	public void compile() {
		MultithreadEngine engine = (MultithreadEngine) Engine.activeEngine;
		engine.compileBodyDefinition(name);
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
