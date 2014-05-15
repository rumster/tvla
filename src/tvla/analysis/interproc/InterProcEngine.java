/** This class represents an interporocedural engine that applies a 
 * fixed-point iterative algorithm to a program specified by the fTVP 
 * file and program inputs specified by the TVS file.
 * @author Noam Rinetzky
 */
package tvla.analysis.interproc;


import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import tvla.analysis.AnalysisStatus;
import tvla.analysis.Engine;
import tvla.analysis.interproc.semantics.AbstractInterpreter;
import tvla.analysis.interproc.semantics.ActionDefinition;
import tvla.analysis.interproc.semantics.ActionInstance;
import tvla.analysis.interproc.semantics.Applier;
import tvla.analysis.interproc.transitionsystem.ProgramTS;
import tvla.analysis.interproc.transitionsystem.method.MethodTS;
import tvla.analysis.interproc.transitionsystem.method.TSNode;
import tvla.core.HighLevelTVS;
import tvla.core.TVS;
import tvla.io.TVLAIO;
import tvla.language.PTS.ActionMacroAST;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.PrintableProgramLocation;
import tvla.util.HashMapFactory;
import tvla.util.Logger;
import tvla.util.graph.Graph;
import tvla.util.graph.GraphFactory;



/**
 * @author maon
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class InterProcEngine extends  Engine { // implements AnalysisMonitor{
	static private final boolean printProgress = true;
	static private final boolean xdebug = false;
	static private java.io.PrintStream out = System.out;
	static private final int NUM_OF_EVENTS_IN_ITERATION = 100;
	
	private TableOfClasses classes = null;
	private TableOfInterfaces interfaces = null;
	private TableOfMethods methods = null;
	
	private ProgramTS progTS;
	private String main;
	
	private Graph interfaceInheritacne = null;
	private Graph classInheritacne = null;
	private Graph classImplementation = null;
	private Object edgeInfo = null; // the same edge info is used for all edges in all graphs.
	
	private Map macros = null;

	private Chaotic chaotic = null;
	private AbstractInterpreter ai = null;
	
	private boolean initialized = false;

	/**
	 * The tables are constructed by prepareTalbes so we can 
	 * initialize them with good sizes.
	 */
	public InterProcEngine() {
		super();
		if (xdebug) 
			out.println("InterProc.Engine - constructor");
		init();
		
//		if (doCoerceAfterFocus) 
//			throw new InternalError("Cannot perform coerce after focus");

			
		macros = HashMapFactory.make();
	}
	
	/**
	 * Constructing the internal data strucutres before the analysis starts.
	 */
	public void prepareTables(String main,
							  int numOfIntra,
							  int numOfStaticCallSites,
							  int numOfVirtualCallSites,
							  int numOfCtorCallSites,
							  int numOfInterfaces, 
							  int numOfClasses,
							  int numOfMethods) {
		assert(main != null);
		assert(numOfIntra >= 0);
		assert(numOfStaticCallSites >= 0);	
		assert(numOfVirtualCallSites  >= 0);
		assert(numOfCtorCallSites  >= 0);
		assert(numOfInterfaces  >= 0); 
		assert(numOfClasses  >= 0);
		assert(numOfMethods >= 0);	
		
		this.main = main;
		
		classes = new TableOfClasses(numOfClasses);
		interfaces = new TableOfInterfaces(numOfInterfaces);
		methods = new TableOfMethods(numOfMethods);
		
		interfaceInheritacne = GraphFactory.newGraph(numOfInterfaces);
		classInheritacne = GraphFactory.newGraph(numOfClasses);
		classImplementation = GraphFactory.newGraph(numOfInterfaces + numOfClasses);
		edgeInfo = new Object();
		
		progTS = new ProgramTS(numOfClasses,numOfMethods,
							   numOfIntra, 
							   numOfStaticCallSites, 
							   numOfVirtualCallSites,	
							   numOfCtorCallSites);

		boolean doFocusProperty = doesFocus();
		// FIXME !!! Noam - just for deugging the combine 
		boolean doCoerceAfterFocusProperty = true; // BUG! doesCoerceAfterFocus();
		boolean doCoerceAfterUpdateProperty = true; // BUG! doesCoerceAfterUpdate();
		boolean doBlurProperty = doesBlur();
		boolean freezeStructuresWithMessagesProperty = freezesStructuresWithMessages();
		boolean breakIfCoerceAfterUpdateFailedProperty = breaksIfCoerceAfterUpdateFailed(); 
		
		if (!doBlurProperty) 
			throw new Error("Must blur after every action");
		
		// FIXME: AnalysisStatus totalStatus = new AnalysisStatus();
		AnalysisStatus totalStatus = this.status;
		
		
		Applier intraApplier = new Applier(
				totalStatus,
				doFocusProperty,
				doCoerceAfterFocusProperty,
				doCoerceAfterUpdateProperty,
				doBlurProperty,
				freezeStructuresWithMessagesProperty,
				breakIfCoerceAfterUpdateFailedProperty,
				"Intra");
		
		Applier guardApplier = new Applier(
				totalStatus,
				true,
				true,
				doCoerceAfterUpdateProperty,
				true,
				freezeStructuresWithMessagesProperty,
				breakIfCoerceAfterUpdateFailedProperty,
				"IP:Guard");

		Applier callApplier = new Applier(
				totalStatus,
				true,
                true,
                doCoerceAfterUpdateProperty,
				true,
				freezeStructuresWithMessagesProperty,
				breakIfCoerceAfterUpdateFailedProperty,
				"IP:Call");

		callApplier.setPrintStrucutreIfCoerceAfetFocusFailed(true);	
		
		Applier retApplier = new Applier(
				totalStatus,
				true,
				false,
				doCoerceAfterUpdateProperty,
				true,
				freezeStructuresWithMessagesProperty,
				breakIfCoerceAfterUpdateFailedProperty,
				"IP:Ret");


		ai = new AbstractInterpreter(
				totalStatus,
				intraApplier,
				guardApplier,
				callApplier,
				retApplier);
		
		initialized = true;
	}
	
	///////////////////////////////////////////////////
	///    Construction of program symbol table    ////
	///     and other type-related information     ////
	///////////////////////////////////////////////////

	public void addInterface(String interfaceName) {
		assert(interfaces != null);

		Interface intrf = new Interface(interfaceName);
		interfaces.addInterface(interfaceName,intrf);
		
		interfaceInheritacne.addNode(intrf);
		classImplementation.addNode(intrf);
	}

	
	public void addClass(String className) {
		assert(classes != null);

		Class cls = new Class(className);
		classes.addClass(className,cls);
		classInheritacne.addNode(cls);
		classImplementation.addNode(cls);
	}
	
	public void addInterfaceSuperType(String interfaceName,
									  String superInterfaceName) {
		Interface intrf, superIntrf;
		
		assert(interfaceName != null &&
			   superInterfaceName != null &&
			   !superInterfaceName.equals(interfaceName));

		intrf = interfaces.getInterface(interfaceName);
		superIntrf = interfaces.getInterface(superInterfaceName);
	
		assert(intrf != null);
		assert(superIntrf != null);
		
		interfaceInheritacne.addEdge(intrf,superIntrf,edgeInfo);
	}
	
	
	public void addClassSuperType(String clsName,
			  					  String superClsName) {
		assert(clsName != null &&
			   superClsName != null &&
		       !superClsName.equals(clsName));

		Class cls, superCls;
		
		cls = classes.getClass(clsName);
		superCls = classes.getClass(superClsName);

		assert(cls != null);
		assert(superCls!= null);
				
		classInheritacne.addEdge(cls,superCls,edgeInfo);
	}

	public void addInterfaceToClass(String clsName,
			  						String interfaceName) {
		assert(clsName != null &&
			   interfaceName != null);
		
		Class cls = classes.getClass(clsName);;
		Interface intrf = interfaces.getInterface(interfaceName);
		
		assert(cls != null);
		assert(intrf != null);
		
		classImplementation.addEdge(cls,intrf,edgeInfo);
	}
	
	public Method addMethod(String className,
						  String methodName,	
						  String sig,          // sig uniquely identifies a method
						  String retTypeName,
						  String[] aformalArgsTypes,
						  String[] aformalArgsNames,
						  String entryLabel,
						  String exitLabel,
						  boolean isStatic,
						  boolean isConstructor)
	{
		assert(className != null && methodName != null && sig != null);
		// FIXME - check that the return type is void
		//assert(retTypeName != null ^ isConstructor);
		assert((aformalArgsTypes == null && aformalArgsNames == null) ||
			   (aformalArgsTypes.length == aformalArgsNames.length && aformalArgsTypes.length > 0)    );
		assert(entryLabel != null && exitLabel != null);
		assert(!entryLabel.equals(exitLabel));
		
		Class cls = classes.getClass(className);
		assert (cls != null);
		
		Method mtd = methods.getMethod(sig);
		assert (mtd == null);
		
		Type retType = null;
		if (!isConstructor) {
			if (Atom.isAtomType(retTypeName)) 
				retType = Atom.getAtomType(retTypeName);
			else
				retType = new Class(retTypeName);			
		}

		
		Type[] types=null;
		if (aformalArgsTypes != null) {
			types = new Type[aformalArgsTypes.length];
			for (int i=0; i<aformalArgsTypes.length; i++) {
				if (Atom.isAtomType(aformalArgsTypes[i])) 
					types[i] = Atom.getAtomType(aformalArgsTypes[i]);
				else
					types[i] = new Class(aformalArgsTypes[i]);

			}
		}
		
		if (isStatic) {
			// static method
			assert(isConstructor == false);
			mtd = new MethodStatic(
					cls,methodName,sig,
					retType,types,aformalArgsNames,
					entryLabel, exitLabel,
					isStatic,isConstructor);
			
			if (sig.equals(this.main))
				mtd.setMain();
		} 
		else if (isConstructor) {
			// constructor
			mtd = new MethodConstructor(
					cls,methodName,sig,
					types,aformalArgsNames,
					entryLabel, exitLabel,
					isStatic,isConstructor);
		} else {
			// virtual method
			mtd = new MethodVirtual(
					cls,methodName,sig,
					retType,types,aformalArgsNames,
					entryLabel, exitLabel,
					isStatic,isConstructor);
		}
		
		methods.addMethod(sig,mtd);
		
		return mtd;
	}

	public Method getMethod(String sig) {
		assert(sig != null);
		assert(methods != null);
		return methods.getMethod(sig);
	}
			
	public String getMain() {
		return progTS.getMain().getMethod().getSig();
	}	

	
	
	////////////////////////////////////////////////////
	///      Constructing the definition of the      ///
	///           abstract transformers              ///
	////////////////////////////////////////////////////
	
	public void addActionDefinition(ActionMacroAST action) {
		ActionDefinition def;
		assert (macros.get(action.getName()) == null);
		def = new ActionDefinition(action);
		macros.put(action.getName(), def);
	}
	
	
	////////////////////////////////////////////////////
	///    Constructing the program CFG annotated    ///
	///             abstract transformers            ///
	////////////////////////////////////////////////////
	
	public void addMethodDefinition(String sig, int numOfStmts){
		assert (sig != null);
		Method mtd = methods.getMethod(sig);
		if (mtd == null)
			throw new InternalError("Attempt to define an undeclared method" + sig);	
		
		if (progTS.contains(mtd))
			throw new InternalError("Attempt to redefine method " + sig);	

		String entry = mtd.getEntryLabel(); 
		String exit = mtd.getExitLabel();
		assert(exit != null && entry != null);
		
		if (mtd.isConstructor())
			progTS.addConstructor(mtd, numOfStmts, entry, exit);
		else if (mtd.isStatic())
			progTS.addStaticMethod(mtd, numOfStmts, entry, exit);
		else
			progTS.addVirtualMethod(mtd, numOfStmts, entry, exit);
	}
	
	public void addIntraStmt(
			String sig,
			String from, String to,
			String macroName, List args) {
		assert(sig != null && from != null && from != null && to != null); 
		assert(macroName!= null && args!= null);

		if (xdebug)
			out.println("addIntraStmt: " + from + "->" + to + ": " + macroName);
	
		Method mtd = methods.getMethod(sig);
		if (mtd == null)
			throw new InternalError("adding a statement to an undeclared method " + sig);
		
		ActionDefinition macDef = (ActionDefinition) macros.get(macroName);
		if (macDef == null)
			throw new Error("Attempt to use undefined macro: " + macroName +
					        " in method " + sig);
		ActionInstance ai = ActionInstance.getActionInstance(macDef, args);
		if (ai == null)
			throw new InternalError("Failed to instantiate macro " + macroName +
					                " un method " + sig + " with arguments " + args);
			
		
		progTS.addIntraStmt(mtd, from, to, ai);	
	}
	
	public void addConstructorInvocation(
			String sig,
			String from, String to,
			String ctorSig, List args,
			String macCall, List argsCall,
			String macRet, List argsRet) {
		assert(sig != null && from != null && from != null && to != null); 
		assert(ctorSig != null && args!= null);
		
		if (xdebug)
			out.println("addConstructorInvocation: " + from + "->" + to + ": " + sig);
		
		Method mtd = methods.getMethod(sig);
		if (mtd == null)
			throw new InternalError("adding a statement to an undeclared method " + sig);

		Method ctor = methods.getMethod(ctorSig);
		if (ctor == null) 
			throw new Error("Invoking undefined constructor " + ctorSig +
					        " In method " + sig + 
							" at label " + from);
		
		ActionDefinition macCallDef = (ActionDefinition) macros.get(macCall); 
		if (macCallDef == null)
			throw new Error("Call Macro " + macCall +  
					        " not defined in  constructor invocation " + ctorSig + 
					        " at " + from + " in method " + sig);
		ActionInstance aiCall = ActionInstance.getActionInstance(macCallDef, argsCall);
		
		ActionDefinition macRetDef = (ActionDefinition) macros.get(macRet); 
		if (macRetDef == null)
			throw new Error("Ret Macro " + macRet + 
					        " not defined in  constructor invocation " + ctorSig + 
					        " at " + from + " in method " + sig);
		ActionInstance aiRet = ActionInstance.getActionInstance(macRetDef, argsRet);
		
		progTS.addConstructorInvocation(mtd, args, from, to, 
										ctor, aiCall, aiRet);
	}

	public void addStaticInvocation(
			String sig,
			String from, String to,
			String calleeSig, List args, 
			String assignResTo,
			String macCall, List argsCall,
			String macRet, List argsRet) {
		assert(sig != null && from != null && from != null && to != null); 
		assert(calleeSig != null && args!= null);

		if (xdebug)
			out.println("addStaticInvocation: " + from + "->" + to + ": " + calleeSig);
		
		Method mtd = methods.getMethod(sig);
		if (mtd == null)
			throw new InternalError("adding a statement to an undeclared method " + sig);
		
		Method invokedMtd = methods.getMethod(calleeSig);
		if (invokedMtd == null) { 
			handleStaticInvocationOfUndefinedMethod(mtd,from,to,calleeSig,args);
			return;
		}
		
		ActionDefinition macCallDef = (ActionDefinition) macros.get(macCall); 
		if (macCallDef == null)
			throw new Error("Call Macro " + macCall +  
					        " not defined in  static method invocation " + calleeSig + 
					        " at " + from + " in method " + sig);
		ActionInstance aiCall = ActionInstance.getActionInstance(macCallDef, argsCall);
		
		ActionDefinition macRetDef = (ActionDefinition) macros.get(macRet); 
		if (macRetDef == null)
			throw new Error("Ret Macro " + macRet + 
					        " not defined in  static method invocation " + calleeSig + 
					        " at " + from + " in method " + sig);
		ActionInstance aiRet = ActionInstance.getActionInstance(macRetDef, argsRet);

		progTS.addStaticInvocation(mtd, args, from, to, 
								   invokedMtd, aiCall, aiRet);
	
	}
	
	protected void handleStaticInvocationOfUndefinedMethod(
			Method mtd,
			String from, String to,
			String calleeSig, List args) {
		
		
		throw new Error("Invoking undefined static method " + calleeSig);
	}


	public void addVirtualInvocation(
			String sig,
			String from, String to,
			String calleeSig, List args, 
			String assignResTo,
			String receiver,
			String macCall, List argsCall,
			String macRet, List argsRet,
			String macGuard, List argsGuard) {
		assert(sig != null && from != null && from != null && to != null); 
		assert(calleeSig != null && args!= null && receiver != null);

		if (xdebug)
			out.println("addVirtualInvocation: " + from + "->" + to + ": " + calleeSig);
	
		Method mtd = methods.getMethod(sig);
		if (mtd == null)
			throw new InternalError("adding a statement to an undeclared method " + sig);

		Method invokedMtd = methods.getMethod(calleeSig);
		if (invokedMtd == null) 
			throw new Error("Invoking undefined virtual method " + calleeSig);
		
		ActionDefinition macCallDef = (ActionDefinition) macros.get(macCall); 
		if (macCallDef == null)
			throw new Error("Call Macro " + macCall +  
					        " not defined in  virtual method invocation " + calleeSig + 
					        " at " + from + " in method " + sig);
		ActionInstance aiCall = ActionInstance.getActionInstance(macCallDef, argsCall);
		
		ActionDefinition macRetDef = (ActionDefinition) macros.get(macRet); 
		if (macRetDef == null)
			throw new Error("Ret Macro " + macRet + 
					        " not defined in  virtual method invocation " + calleeSig + 
					        " at " + from + " in method " + sig);
		ActionInstance aiRet = ActionInstance.getActionInstance(macRetDef, argsRet);
		
		ActionDefinition macGuardDef = (ActionDefinition) macros.get(macGuard); 
		if (macGuardDef == null)
			throw new Error("Guard Macro " + macGuard + 
					        " not defined in  virtual method invocation " + calleeSig + 
					        " at " + from + " in method " + sig);
		ActionInstance aiGuard = ActionInstance.getActionInstance(macGuardDef, argsGuard);

		progTS.addVirtualInvocation(mtd, args, from, to, 
				   					invokedMtd, aiCall, aiRet, aiGuard);
		
	}

	
	////////////////////////////////////////////////////
	///                    Output                    ///
	////////////////////////////////////////////////////

	public void setPrintAllNodes() {
		progTS.setPrintAllNodes();
	}
	
	public void setPrintInterProcNodes() {
		progTS.setPrintInterProcNodes();
	}
	
	
	public void setPrintAllNodesOfMethod(String mtdSig) {
		Method mtd = methods.getMethod(mtdSig);
		if (mtd == null)
			throw new Error("Attempt to print a node of a non declared method " + mtdSig);
		
		progTS.setPrintAllNodesOfMethod(mtd);
	}

	public void setPrintNode(String mtdSig, String nodeLabel) {
		Method mtd = methods.getMethod(mtdSig);
		if (mtd == null)
			throw new Error("Attempt to print a node of a non declared method " + mtdSig);
		
		progTS.setPrintNode(mtd, nodeLabel);
		
	}
	
	public Collection getPrintableProgram() {
		
		return progTS.getPrintableProgram();
	}
	
	
	public void printResults(TVLAIO io) {
		progTS.printResults(io);
	}
/*	
	public void saveResults(TVLAIO io, XML xml) {
		progTS.saveResults(io,xml);
		ai.saveAnalysis(io,xml);
	}
*/

	////////////////////////////////////////////////////
	///              Fixpoint computation            ///
	////////////////////////////////////////////////////

	public void evaluate(Collection<HighLevelTVS> initial) {
		evaluate(initial, -1);
	}
	
	/* (non-Javadoc)
	 * @see tvla.analysis.Engine#evaluate(java.util.Collection)
	 */
	public void evaluate(Collection<HighLevelTVS> initial, int maxEvents) {
		assert(initial != null);
		if (!initialized())
			throw new InternalError("PASTA engine failed to initialize");

		if (initial.isEmpty())
			throw new Error("Initial state was not set");
			
		Method mainMethod = methods.getMethod(main);
		if (mainMethod == null)
			throw new Error("Main method <" + main + "> is not declared");
			
		MethodTS mainTS = progTS.setMain(mainMethod);
		assert(mainTS.getMethod() == mainMethod);
		TSNode mainEntry = mainTS.getEntrySite();
		assert(mainEntry.isEntrySite());

		progTS.completeDefinitions();
		
		chaotic = new Chaotic(progTS, ai);
		
		Date startedAt = new Date();
		if (printProgress) { 
			out.println("<<<  Analyzing program: " + main + " >>>");			
			startedAt.setTime(System.currentTimeMillis());
			out.println("<<<  Starting at  " + startedAt + " >>>");
			out.println("<<<  Initializing data structures ... >>>");
		}

		ai.startAnalysis();		
		Collection initialASTVSs = progTS.initAnalysis(initial);
		chaotic.initAnalysis(mainTS, initialASTVSs);

		if (printProgress) 
			out.println("<<<  Starting chaotic iterations ... >>>");
		
		
		boolean finished = false;
		boolean stop = false;
		while (!finished && !stop) {
			chaotic.updateStatus();
			finished = chaotic.iterate(NUM_OF_EVENTS_IN_ITERATION);
			if (printProgress) 
				out.println("<<<  " + chaotic.getNumOfIterations() + " events processed  >>>");
			
			stop = (0 < maxEvents && maxEvents <= chaotic.getNumOfIterations());
		}
		ai.stopAnalysis();
			
		if (printProgress) {
			Date finishedAt = new Date();
			finishedAt.setTime(System.currentTimeMillis());
			long analysisTime = (finishedAt.getTime() - startedAt.getTime()) / 1000;
			out.println("<<<  Chaotic iterations finished at  " + finishedAt + "  >>>");
			out.println("<<<  Analysis time  " + analysisTime + " seconds >>>");
			out.println("<<<  Fixpoint " + (finished ? "reached" : "not reached!  >>>"));
		}

		
		printStatistics();
	}

	// TODO change HighLevelTVS to TVS here and in class Engine
	public Collection apply(Action action, HighLevelTVS structure, String label, Map messages) {
		throw new UnsupportedOperationException();
	}
	
	/////////////////////////////////////////////////////
	/////             AnalysisMonitor               /////
	/////////////////////////////////////////////////////
	
	public PrintableProgramLocation  getProcessedLocation() {
		return chaotic.getProcessedLocation();
	}
	
	public long getNumOfIterations() {
		return this.getNumOfIterations();
	}	
	
		
	public void printStatistics() {
		Logger.println();
		Logger.println("Statistics for program: " + main);
		Logger.println("-----------------------");
		Logger.println();
		
		Logger.println("Statistics of chaotic iterations");
		Logger.println("--------------------------------");
		chaotic.printStatistics();
		Logger.println();
		Logger.println();
		
		Logger.println("Statistics of transition system");
		Logger.println("-------------------------------");
		progTS.printStatistics();
		Logger.println();
		Logger.println();

		Logger.println("Statistics of applyers");
		Logger.println("----------------------");
		ai.printStatistics();
		Logger.println();
		Logger.println();
	}
	////////////////////////////////////////////////////
	///                Helper methods                ///
	////////////////////////////////////////////////////
	
	public void dump(java.io.PrintStream to){
		assert(interfaces != null);
		assert(classes != null);
		out.println("===========================");
		out.println("= InterProc.Engine - DUMP =");
		out.println("===========================");

		interfaces.dump(out);
		classes.dump(out);
		out.println("== MAIN METHOD: <" + main + ">");
		methods.dump(out);
	}
	
	private boolean initialized() {
		return initialized;
	}
}
