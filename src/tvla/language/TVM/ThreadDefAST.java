package tvla.language.TVM;

import java.util.List;
import java.util.Map;

import tvla.analysis.Engine;
import tvla.analysis.multithreading.MultithreadEngine;
import tvla.analysis.multithreading.ProgramThread;
import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;
import tvla.util.HashMapFactory;

public class ThreadDefAST extends BodyDefAST {
	
	private final static boolean xdebug = false;
	
	/** 
	 * map of (threadName,threadDefAST)
	 */
	private static Map threads = HashMapFactory.make();
	
	/** the generated program thread */
	private ProgramThread programThread;
		
	
	public ThreadDefAST(String threadName, List threadActions)
	{
		super(threadName,threadActions);
		threads.put(name,this);
		if (xdebug)
			System.err.println("ThreadDefAST created for: " + name);
	}
	
	
	public static ThreadDefAST getThreadDef(String name) {
		ThreadDefAST td = (ThreadDefAST) threads.get(name);
		if (td == null) 
			throw new RuntimeException("Unknown thread type " + name);
		return td;
	}
	
	public static BodyDefAST get(String name) {
		ThreadDefAST td = (ThreadDefAST) threads.get(name);
		if (td == null) 
			throw new RuntimeException("Unknown thread type " + name);
		return td;
	}
	
	public void generate() {
		MultithreadEngine engine = (MultithreadEngine) Engine.activeEngine;
		programThread = engine.addThreadDefinition(name,actions);
	}
	
	public void compile() {
		MultithreadEngine engine = (MultithreadEngine) Engine.activeEngine;
		engine.compileThreadDefinition(name);
	}
	
	public ProgramThread getThread() {
		return programThread;
	}
	
	public AST copy() {
		throw new RuntimeException("Can't copy thread definitions.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute thread definitions.");
	}
}
