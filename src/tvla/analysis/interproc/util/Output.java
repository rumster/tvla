/*
 * File: Output.java 
 * Created on: 20/10/2004
 */

package tvla.analysis.interproc.util;

import java.util.Iterator;
import java.util.List;

/** 
 * @author maon
 */
public class Output {
	public static String invocationString(String sig, List args) {
		String ret = sig;
		ret = ret.concat("(");
		Iterator itr=args.iterator();
		int pos = 0;
		while(itr.hasNext()) {
			pos++;
			String arg = (String) itr.next();
			ret = ret.concat(arg);
			if (pos != args.size())
				ret.concat(",");
		}
		ret = ret.concat(")");
		
		return ret;
	}
	
	public static String invocationString(String sig, List args, String retVar) {
		String ret = retVar;
		ret = ret.concat(" = ");
		ret = ret.concat(invocationString(sig,args));
		return ret;
	}

	public static String virtualInvocationString(String sig, String target, List args) {
		String ret = target + "->";
		ret = ret.concat(invocationString(sig,args));
		return ret;
	}

	public static String virtualInvocationString(String sig, String target, List args, String retVar) {
		String ret = retVar;
		ret = ret.concat(" = " + target + "->");
		ret = ret.concat(invocationString(sig,args));
		return ret;
	}
}
