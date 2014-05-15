package tvla.analysis.interproc.worklist;

import tvla.analysis.interproc.transitionsystem.method.MethodTS;
import tvla.analysis.interproc.transitionsystem.method.TSNode;

/** This class is an abstract event. 
 * An event corresponds to a change in the chaotic iteration,
 * i.e., a propogation of a strucutre.  
 * @author maon
 */
public abstract class Event {
	public static final int INTRA = 0;
	public static final int STATIC_CALL = 1;
	public static final int VIRTUAL_CALL = 2;
	public static final int CONSTRUCTOR_CALL = 3;
	public static final int RET = 4;
	public static final int TRANSITION= 5;

	protected final MethodTS mtd;          // The method in which a change occured
	protected final TSNode site;          // The location of the change
	/**
	 * 
	 */
	protected Event(MethodTS mtd, TSNode site) {
		this.mtd = mtd;
		this.site = site;
	}

	public MethodTS getMethod() {
		return mtd;
	}
	
	public TSNode getSite() {
		return site;
	}

	public abstract int getType(); 
	
	public String toString() {
		return (" Event " + getTypeStr(getType()) + 
				" in method " + mtd.getMethod().getSig());
	}
	
	public String getTypeStr(int type) {
		assert(type == INTRA || 
			   type == STATIC_CALL || type == VIRTUAL_CALL || type == CONSTRUCTOR_CALL || 
			   type == RET ||
			   type == TRANSITION); 
		 
		switch(type) {
		case INTRA:
			return "INTRA";
		case STATIC_CALL:
			return "STATIC_CALL";
		case VIRTUAL_CALL:
			return "VIRTUAL_CALL";
		case CONSTRUCTOR_CALL:
			return "CONSTRUCTOR_CALL";
		case RET:
			return "RET";
		case TRANSITION:
			return "TRANSITION";
		default:
			throw new InternalError("getTypeStr: Unknown even type");
		}
	}
}
