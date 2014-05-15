/*
 * Created on 22/09/2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package tvla.analysis.interproc;


/**
 * An atmoic type. 
 * @author maon
 */
public final class Atom extends Type {
	public static final int booleanAtom = 0;
	public static final int charAtom = 1;
	public static final int byteAtom = 2;
	public static final int shortAtom = 3;
	public static final int intAtom = 4;
	public static final int longAtom = 5;
	public static final int floatAtom = 6;
	public static final int doubleAtom = 7;
	public static final int voidAtom = 8;
	
	public static final String[] atomNames = {
			"boolean",
		   	"char",
		   	"byte",
		   	"short",
		   	"int",
		   	"long",
		   	"float",
		   	"double",
			"void"};

	public static final Atom[] atomTypes= {
			new Atom("boolean"),
			new Atom("char"),
			new Atom("int"),
			new Atom("byte"),
			new Atom("short"),
			new Atom("int"),
			new Atom("long"),
			new Atom("float"),
			new Atom("double"),
			new Atom("void")};

	
	private int type = -1;
	/**
	 * 
	 */
	public Atom(String name) {
		super(name);
		if (name.equals(atomNames[booleanAtom])) {
			type = booleanAtom;
		}
		else if (name.equals(atomNames[charAtom])) {
			type = charAtom;
		}
		else if (name.equals(atomNames[byteAtom])) {
			type = byteAtom;
		}
		else if (name.equals(atomNames[shortAtom])) {
			type = shortAtom;
		}
		else if (name.equals(atomNames[intAtom])) {
			type = intAtom;
		}
		else if (name.equals(atomNames[longAtom])) { 
			type = longAtom;
		}
		else if (name.equals(atomNames[floatAtom])) {
			type = floatAtom;
		}
		else if (name.equals(atomNames[doubleAtom]))  {
			type = doubleAtom;
		}
		else if (name.equals(atomNames[voidAtom]))  {
			type = voidAtom;
		}
		else {
			// not an atomic type
			assert(true);
		}
	
	}
		
	public static boolean isAtomType(String typeName) {
		if (typeName.equals(atomNames[booleanAtom])) {
			return true;
		}
		else if (typeName.equals(atomNames[charAtom])) {
			return true;
		}
		else if (typeName.equals(atomNames[byteAtom])) {
			return true;
		}
		else if (typeName.equals(atomNames[shortAtom])) {
			return true;
		}
		else if (typeName.equals(atomNames[intAtom])) {
			return true;
		}
		else if (typeName.equals(atomNames[longAtom])) { 
			return true;
		}
		else if (typeName.equals(atomNames[floatAtom])) {
			return true;
		}
		else if (typeName.equals(atomNames[doubleAtom]))  {
			return true;
		}
		else if (typeName.equals(atomNames[voidAtom]))  {
			return true;
		}
		
		// not an atomic type
		return false;
	}
	
	public static Atom getAtomType(String typeName) {
		if (typeName.equals(atomNames[booleanAtom])) {
			return atomTypes[booleanAtom];
		}
		else if (typeName.equals(atomNames[charAtom])) {
			return atomTypes[charAtom];
		}
		else if (typeName.equals(atomNames[byteAtom])) {
			return atomTypes[byteAtom];
		}
		else if (typeName.equals(atomNames[shortAtom])) {
			return atomTypes[shortAtom];
		}
		else if (typeName.equals(atomNames[intAtom])) {
			return atomTypes[intAtom];
		}
		else if (typeName.equals(atomNames[longAtom])) { 
			return atomTypes[longAtom];
		}
		else if (typeName.equals(atomNames[floatAtom])) {
			return atomTypes[floatAtom];
		}
		else if (typeName.equals(atomNames[doubleAtom]))  {
			return atomTypes[doubleAtom];
		}
		else if (typeName.equals(atomNames[voidAtom]))  {
			return atomTypes[voidAtom];
		}
		
		// not an atomic type
		return null;
	}
	
	public String getName(){
		assert(0 < type && type <=8);
		return atomNames[this.type];
	}
	
	public static String  getName(int type) {
		assert(0 < type && type <= 8);
		return atomNames[type];
		
	}
	
	
	public void dump(java.io.PrintStream out) {
		out.println("ATOM:  NAME<" + name + ">  NUM <" + type + ">");
	}
}
