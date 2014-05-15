package tvla.analysis.interproc.api.javaanalysis.abstraction.allocationsitesNN;

/**
 * This class contains names of the macros (sets and predicates)
 * used in the parametric definition of TVLA abstract domain (i.e., in the tvp).
 * Be very careful to match this file with the actua tvp TVLA reads as a mismatch may lead
 * to mayhem and all that is the opposite of good
 *
 * This file is analysis independent
 *
 * @author maon
 *
 */
public class NotNullConstants {
	/**************************************/
	/** Java-specific fiexed predicates  **/
	/**************************************/
	public final static String pointedToByPendingLocal = "ptpV";   
}