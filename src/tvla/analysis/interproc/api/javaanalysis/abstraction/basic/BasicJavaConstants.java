package tvla.analysis.interproc.api.javaanalysis.abstraction.basic;

/**
 * This class contains all the names of the macros (sets and predicates)
 * used in the parametric definition of TVLA abstract domain  (i.e., in the tvp)..
 * Be very careful to match this file with the actua tvp TVLA reads as a mismatch may lead
 * to mayhem and all that is the opposite of good
 *
 * This file is analysis independent
 *
 * @author maon
 *
 */
public class BasicJavaConstants {
	/**************************************/
	/** Java-specific fiexed predicates  **/
	/**************************************/
	public final static String containsStr = "contains";   
    public final static String isArrayStr = "isArray";   
	
	
	/**************************************/
	/** AbstractDomain parameters (SETs) **/
	/**************************************/
	public final static String setClassesStr = "Classes";   
	public final static String setArrayClassesStr = "ArrayClasses";   
	public final static String setBoolStaticFieldsStr = "BoolStaticFields";
	public final static String setRefStaticFieldsStr = "RefStaticFields";
    public final static String setArrayStaticFieldsStr = "ArrayStaticFields";
	public final static String setBoolInstanceFieldsStr = "BoolInstanceFields";
	public final static String setRefInstanceFieldsStr = "RefInstanceFields";
    public final static String setArrayInstanceFieldsStr = "ArrayInstanceFields";
	public final static String setAllocationSitesStr = "AllocationSites";
	public final static String setArrayAllocationSitesStr = "ArrayAllocationSites";
	public final static String setBoolParametersStr = "BoolParameters"; 
	public final static String setRefParametersStr = "RefParameters";
    public final static String setArrayParametersStr = "ArrayParameters";
	public final static String setBoolLocalsStr = "BoolLocals"; 
	public final static String setRefLocalsStr = "RefLocals";
    public final static String setArrayLocalsStr = "ArrayLocals";
	
	public final static String[] allSetsStrs = new String[] {
		setClassesStr,
		setArrayClassesStr,
		setBoolStaticFieldsStr,
		setRefStaticFieldsStr,
        setArrayStaticFieldsStr,
		setBoolInstanceFieldsStr,
		setRefInstanceFieldsStr,
        setArrayInstanceFieldsStr,
		setBoolParametersStr,
		setRefParametersStr,
        setArrayParametersStr,
		setBoolLocalsStr,
		setRefLocalsStr,
        setArrayLocalsStr,
		setAllocationSitesStr,
		setArrayAllocationSitesStr
	};
}