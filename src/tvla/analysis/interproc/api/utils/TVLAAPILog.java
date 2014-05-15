//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.utils;

import tvla.util.Logger;

/**
 * Prints logging statments int oa given stream 
 * (currently the screen + Trace file)
 * @author maon
 *
 */
public class TVLAAPILog {
  public static final boolean log = TVLAAPIDebugControl.Log.tvlaapiLogActive;

  public static final boolean logToTrace = TVLAAPIDebugControl.Log.logToTrace; 
  public static final boolean logToScreen = TVLAAPIDebugControl.Log.verbose; 
  public static final boolean logToTVLALog = TVLAAPIDebugControl.Log.logToTVLALog; 
  
  public static  void print(String s) {
    if (log) {
      if (logToTrace)
        TVLAAPITrace.tracePrint(s);
      if (logToTVLALog)
        Logger.print(s);
      if(logToScreen)
        System.err.print(s);
    }
  }

  public static void println(String s) {
    if (log) {
      if (logToTrace)
        TVLAAPITrace.tracePrintln(s);
      if (logToTVLALog)
        Logger.println(s);
      if(logToScreen)
        System.err.println(s);
    }
  }
}
