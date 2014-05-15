//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.utils;


/**
 * This class is the master control switch for all debug asertin and checks 
 * done by SafeT
 * 
 * @author maon
 *
 */
public class TVLAAPIDebugControl {
  public final static boolean barebone =  false;
  
  public static class Assert{
    public final static boolean tvlaapiAssertActive = !barebone;
    public final static boolean tvlaapiAssertClientActive = !barebone;
  }

  public static class Debug {
    // public final static boolean tvlaDebugFlag = ProgramProperties.getBooleanProperty("tvla.debug", false);
    // if -1, there is no limit on the debug level
    // TODO set to 0 if SafeTProperties.verbose == 0
    public final static int masterMaxDebugLevel = -1; 
  }

  public static class Trace {
    public final static boolean tvlaapiTraceActive = !barebone ;
    public final static boolean tvlaapiTraceClientActive = true;
    public static final boolean tvlaapiTraceStderrActive = false;

  }
  
  public static class Output {

  }
  
  // TODO fnish this so it will work with TVLA log system
  public static class Log {
    /** Should TVLA log information */
    public static final boolean tvlaapiLogActive = true ;

    /** Should SafeT log be sent to stderr */
    public static final boolean verbose = true ;

    /** Should SafeT log be sent to the trace file */
    public static final boolean logToTrace = true;

    /** Should SafeT log be sent to TVLA log file */
    public static final boolean logToTVLALog = true;

  }
  
  /**
   * Return the debug level of the class of the preceduing method 
   * @param the value set in the file.
   * 
   * @return debug level of the caller's class of -1 if failed
   * @note A Hack from hell to get the current method full name.
   * This is pretty xpensive, so do it oly in the iitialization 
  */

  public static int getDebugLevel(int suggestedValue) {   
    if (Debug.masterMaxDebugLevel == 0)
      return 0;
    
    if (Debug.masterMaxDebugLevel == -1)
      return suggestedValue;
    
    StackTraceElement[] stack = new Exception().getStackTrace();
    final int callerIndex = 1;    
    String className = stack[callerIndex].getClassName();
    
    System.err.println(className);
    
    int ret = getDebugLevelForFullClassName(className, suggestedValue);
    
    return ret;
  }
  
  private static int getDebugLevelForFullClassName(String classFullName, int suggestedValue) {
    String packageName = classFullName.substring(0, classFullName.lastIndexOf('.'));
    String className = classFullName.substring(classFullName.lastIndexOf('.') + 1);
    
    int res = getClassSpecificDebugLevel(packageName, className, suggestedValue);
    if (-1 < res)
      return res;
    
    res = getPackageSpecifcDebugLevel(packageName, suggestedValue);
    if (-1 < res)
      return res;
    
    return Debug.masterMaxDebugLevel;
  }


  /**
   * Return a debug level for a specifc class , or -1 if no specific value exists
   * @param packageName
   * @param className
   * @return
   */
  private static int getClassSpecificDebugLevel(String packageName, String className, int suggestedValue) {
    if (0 <= Debug.masterMaxDebugLevel)
      return Math.min(Debug.masterMaxDebugLevel, suggestedValue);
    else
      return suggestedValue;
  }
  
  /**
   * Return a debug level for a specifc package, or -1 if no specific value exists
   * @param packageName
   * @return
   */
  private static int getPackageSpecifcDebugLevel(String packageName, int suggestedValue) {
    return -1;
  }
  
  
}
