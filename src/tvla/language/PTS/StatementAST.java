package tvla.language.PTS;

import java.io.PrintStream;

import tvla.language.TVP.AST;
//import tvla.transitionSystem.*;

/** An abstract syntax node for actions.
 * @author Tal Lev-Ami.
 */
public abstract class StatementAST extends AST {
	protected String label, next;
	
	public StatementAST(String label,  String next) {
		this.label = label;
		this.next = next;
	}	

	public String from() {
		return label;
	}

	public String to() {
		return next;
	}

	abstract public void generate(String sig);
	public abstract AST copy();
	public abstract void dump(PrintStream out);
}