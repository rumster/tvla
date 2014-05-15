/*
 * File: MethodDefStaticAST.java 
 * Created on: 23/09/2004
 */

package tvla.language.PTS;

import java.io.PrintStream;
import java.util.List;
import java.util.Vector;

import tvla.analysis.Engine;
import tvla.analysis.interproc.InterProcEngine;
import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;

/** AST for a definition of a static method
 * @author maon
 */
public class SymbAST extends  tvla.language.TVP.AST {	
	static private final boolean xdebug = false;
	static private java.io.PrintStream out = System.out;

	String main;
	int numOfIntra;
	int numOfStaticCallSites;
	int numOfVirtualCallSites;
	int numOfCtorCallSites;
	List classes;	
	List interfaces;	
	List extendsInterfaces;
	List extendsClasses;
	List implementsClasses;	
	List methods;	
	  
	public SymbAST (String main,
					int numOfIntra,
					int numOfStaticCallSites,
					int numOfVirtualCallSites,
					int numOfCtorCallSites,
					List interfaces,
					List classes,	
					List extendsInterfaces,	
					List extendsClasses,	
					List implementsClasses,	
					List methods){
		this.main = main;
		this.numOfIntra = numOfIntra;
		this.numOfStaticCallSites = numOfStaticCallSites;
		this.numOfVirtualCallSites = numOfVirtualCallSites;
		this.numOfCtorCallSites = numOfCtorCallSites;
		this.classes = new Vector(classes);	
		this.interfaces = new Vector(interfaces);	
		this.extendsInterfaces = new Vector(extendsInterfaces);	
		this.extendsClasses = new Vector(extendsClasses);	
		this.implementsClasses = new Vector(implementsClasses);	
		this.methods = new Vector(methods);	
	}

	public AST copy() {
		throw new RuntimeException("Can't copy class decleration.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute class decleration.");
	}

	public void compile() {
		InterProcEngine eng = (InterProcEngine) Engine.activeEngine; 

		if (xdebug)
			dump(System.out);

		eng.prepareTables(main,
						  numOfIntra, 
						  numOfStaticCallSites, 
						  numOfVirtualCallSites,	
						  numOfCtorCallSites,
						  interfaces.size(),
						  classes.size(),
						  methods.size());

		for (int i=0; i<interfaces.size(); i++) {
			SymbInterfaceAST intrf = (SymbInterfaceAST) interfaces.get(i);
			eng.addInterface(intrf.interfaceName);
		}
		
		for (int i=0; i<classes.size(); i++) {
			SymbClassAST cls = (SymbClassAST) classes.get(i);
			eng.addClass(cls.className);
		}
		
		for (int i=0; i<extendsInterfaces.size(); i++) {
			SymbExtendsInterfaceAST extdIntrf = (SymbExtendsInterfaceAST) extendsInterfaces.get(i);
			eng.addInterfaceSuperType(extdIntrf.subTypeName, extdIntrf.superTypeName);
		}
		
		for (int i=0; i<extendsClasses.size(); i++) {
			SymbExtendsClassAST extdCls = (SymbExtendsClassAST) extendsClasses.get(i);
			eng.addClassSuperType(extdCls.subTypeName, extdCls.superTypeName);
		}
		
		for (int i=0; i<implementsClasses.size(); i++) {
			SymbImplementsAST impl = (SymbImplementsAST) implementsClasses.get(i);
			eng.addInterfaceToClass(impl.className, impl.interfaceName);
		}
		
		for (int i=0; i<methods.size(); i++) {
			SymbMethodAST mtd = (SymbMethodAST) methods.get(i);
			String[] args = null;
			String[] types = null;
			String entry = mtd.getEntryLabel();
			String exit = mtd.getExitLabel();

			if (mtd.numOfFormalArgs() > 0) {
				args = new String[mtd.numOfFormalArgs()];
				types = new String[mtd.numOfFormalArgs()];
				for (int j=0; j<mtd.numOfFormalArgs(); j++) {
					args[j] = mtd.getFormalArg(j);
					types[j] = mtd.getTypeOfArg(j);
				}
			}
			
			if (mtd instanceof SymbMethodStaticAST)	{
				SymbMethodStaticAST smtd = (SymbMethodStaticAST) mtd;
				
				eng.addMethod(smtd.className,
							  smtd.methodName,
							  smtd.getSig(),
							  smtd.retType,
							  args,
							  types,
							  entry,
							  exit,
							  true, // is static
							  false); // is constructor
			}
			else if (mtd instanceof SymbMethodVirtualAST)	{
				SymbMethodVirtualAST vmtd = (SymbMethodVirtualAST) mtd;
				
				eng.addMethod(vmtd.className,
							  vmtd.methodName,
							  vmtd.getSig(),
							  vmtd.retType,
							  args,
							  types,
							  entry,
							  exit,
							  false, // is static
							  false); // is constructor
			} else { 
				assert(mtd instanceof SymbConstructorAST);
				SymbConstructorAST cmtd = (SymbConstructorAST) mtd;
				
				eng.addMethod(cmtd.className,
							  cmtd.methodName,
							  cmtd.getSig(),
							  null,
							  args,
							  types,
							  entry,
							  exit,
							  false, // is static
							  true); // is constructor	
			}
		}
	}	
	
	public void dump(PrintStream out) {
		out.println();
		out.println("=====================");
		out.println("= Symbol Table Dump =");
		out.println("======================");

		out.println();
		out.println("Interface Table");
		out.println("---------------");
		for (int i=0; i<interfaces.size(); i++) {
			SymbInterfaceAST intrf = (SymbInterfaceAST) interfaces.get(i);
			out.println("INTERFACE: ["+i+"] " + intrf.interfaceName);
		}
				
		out.println();
		out.println("Class Table");
		out.println("-----------");
		for (int i=0; i<classes.size(); i++) {
			SymbClassAST cls = (SymbClassAST) classes.get(i);
			out.println("CLASS: ["+i+"] " + cls.className);
		}

		out.println();
		out.println("Interface Inheritance");
		out.println("---------------------");
		for (int i=0; i<extendsInterfaces.size(); i++) {
			SymbExtendsInterfaceAST intrfExtd = (SymbExtendsInterfaceAST) extendsInterfaces.get(i);
			out.println("INTERFACE: ["+i+"] " + intrfExtd.subTypeName + 
					    " EXTENDS: " + intrfExtd.superTypeName);
		}

		out.println();
		out.println("Class Inheritance");
		out.println("---------------------");
		for (int i=0; i<extendsClasses.size(); i++) {
			SymbExtendsClassAST clsExtd = (SymbExtendsClassAST) extendsClasses.get(i);
			out.println("CLASS: ["+i+"] " + clsExtd.subTypeName + 
					    " EXTENDS: " + clsExtd.superTypeName);
		}

		out.println();
		out.println("Class Implements");
		out.println("----------------");
		for (int i=0; i<implementsClasses.size(); i++) {
			SymbImplementsAST clsImpl = (SymbImplementsAST) implementsClasses.get(i);
			out.println("CLASS: ["+i+"] " + clsImpl.className + 
					    " IMPLEMENTS: " + clsImpl.interfaceName);
		}

		out.println();
		out.println("Methods");
		out.println("-------");
		int s=0,v=0,c=0;
		for (int i=0; i<methods.size(); i++) {
			SymbMethodAST mtd = (SymbMethodAST) methods.get(i);
			if (mtd instanceof SymbMethodStaticAST) {
				SymbMethodStaticAST smtd = (SymbMethodStaticAST) mtd;
				out.println("STATIC METHOD: ["+i+" ("+s+")] " + 
							smtd.getDesc());
				s++;
			}
			else if (mtd instanceof SymbMethodVirtualAST){
				SymbMethodVirtualAST vmtd = (SymbMethodVirtualAST) mtd;
				out.println("VIRTUAL METHOD: ["+i+" ("+v+")] " + 
						vmtd.getDesc());
				v++;
			}
			else {
				assert(mtd instanceof SymbConstructorAST);
				SymbConstructorAST cmtd = (SymbConstructorAST) mtd;
				out.println("CONSTRUCTOR: ["+i+" ("+c+")] " + 
						cmtd.getDesc());
				c++;
			}
		}

	}
}