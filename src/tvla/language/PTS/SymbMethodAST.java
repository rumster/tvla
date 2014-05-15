/*
 * File: MethodDefStaticAST.java 
 * Created on: 23/09/2004
 */

package tvla.language.PTS;

import java.util.List;
import java.util.Vector;

import tvla.analysis.interproc.Atom;
import tvla.language.TVP.AST;
import tvla.language.TVP.PredicateAST;

/** AST for a definition of a static method
 * @author maon
 */
public abstract class SymbMethodAST extends  tvla.language.TVP.AST {
	String signature;
	String entry;
	String exit;

	String className;
	String methodName;
	List formalArgs;
	
	String retType; // void for constructors

	
	public SymbMethodAST(
			String signature,
			List formalArgs,
			String entry,
			String exit) {
		// TODO Auto-generated constructor stub
		this.signature = signature;
		this.entry = entry;
		this.exit = exit;
		this.className = signature.substring(1,signature.indexOf(':')); // skip <
		int preMethod = signature.indexOf(' ') + 1;
		preMethod = signature.indexOf(' ',preMethod);
		int postMethod = signature.indexOf('(');
		this.methodName = signature.substring(preMethod+1,postMethod) ;
		this.formalArgs = new Vector(formalArgs);	
		this.retType = Atom.getName(Atom.voidAtom);
	}

	public AST copy() {
		throw new RuntimeException("Can't copy method definitions.");
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		throw new RuntimeException("Can't substitute method definitions.");
	}
	
	public String getDesc() {
		StringBuffer strBuff = new StringBuffer();
		strBuff.append("CLASS: <" + className + ">" + 
					   " METHOD: <" + methodName + ">");
		for (int i=0; i<formalArgs.size();i++) {
			String arg = (String) formalArgs.get(i);
			strBuff.append("\n  PARAM ["+i+"] <" + 
					           getTypeOfArg(i) + 
						       ":" + arg+ ">");   		
		}
		
		if (formalArgs.size() == 0)
			strBuff.append("  <NO PARAMS>");
		
		return strBuff.toString();
	}

	public String getSig() {
		return signature;
	}

	public String getEntryLabel() {
		return entry;
	}

	public String getExitLabel() {
		return exit;
	}
	
	public abstract String getRetTypeString();
	
	public int numOfFormalArgs() {
		return formalArgs.size();
	}
	
	public String getFormalArg(int i) {
		assert (0 <= i);
		assert (i < formalArgs.size());
		return (String) formalArgs.get(i);
	}

	public String getTypeOfArg(int argNum) {
		assert(0 <= argNum && argNum < formalArgs.size());
		
		int startIndex = 0;
		int postIndex = 0;
		
		if (argNum==0) {
			startIndex = signature.indexOf('(') + 1;
		}
		else {
			startIndex = signature.indexOf(',') + 1;
			int i = argNum;
			while (0 < --i) { 
				startIndex = signature.indexOf(',',startIndex);
				startIndex++;
			}
		}
		
		if (argNum == formalArgs.size() -1 )
			postIndex = signature.indexOf(')');
		else
			postIndex = signature.indexOf(',',startIndex);
		
		if (startIndex < 0 || postIndex < 0 || postIndex <= startIndex){
			System.out.println(postIndex   + "  " + 
					           startIndex + "  " +  
							   postIndex + " " +
							   startIndex);
			throw new Error("Bad paramter list for method " + signature);
		}
		
		String res = signature.substring(startIndex,postIndex);
		
		
		return res;
	}
}
