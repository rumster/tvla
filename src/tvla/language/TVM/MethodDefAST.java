package tvla.language.TVM;

import java.util.List;
import java.util.Map;

import tvla.analysis.multithreading.ProgramMethodBody;
import tvla.util.HashMapFactory;

public class MethodDefAST extends BodyDefAST {
	
	private final static boolean xdebug = false;
	
	/** 
	 * map of (methodName,BodyDefAST)
	 */
	private static Map methods = HashMapFactory.make();

	/**
	 * the generated body definition
	 */
	protected ProgramMethodBody body; 
	

	public MethodDefAST(String name, List actions)
	{
		super(name,actions);
		methods.put(name,this);
		if (xdebug)
			System.err.println("MethodDefAST created for: " + name);
	}
	
	public static BodyDefAST get(String name) {
		MethodDefAST td = (MethodDefAST) methods.get(name);
		if (td == null) 
			throw new RuntimeException("Unknown method name " + name);
		return td;
	}
}
