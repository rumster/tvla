//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.utils;

import tvla.api.ITVLAAPIDebuggingServices;

public class TVLAAPITrace {
  public static final boolean TRACE = TVLAAPIDebugControl.Trace.tvlaapiTraceActive;

  public static final boolean traceToClient = TVLAAPIDebugControl.Trace.tvlaapiTraceClientActive;
  public static final boolean traceToStderr = TVLAAPIDebugControl.Trace.tvlaapiTraceStderrActive; 
  
  public static ITVLAAPIDebuggingServices clientServices = null;
  
  public static void setClient(ITVLAAPIDebuggingServices client) {
    clientServices = client;
  }
    
  public static  void tracePrint(String s) {
    if (TRACE) {
      if (traceToClient) {
        if (clientServices != null)
          clientServices.tracePrint(s);
        else if (! traceToStderr)
          System.err.print("traceClinet == null, tracing to stderr: " + s);
      }
      
      if(traceToStderr)
        System.err.print(s);
    }
  }

  public static void tracePrintln(String s) {
    if (TRACE) {
      if (traceToClient) {
        if (clientServices != null)
          clientServices.tracePrintln(s);
        else if (! traceToStderr)
          System.err.println("traceClinet == null, tracing to stderr: " + s);
      }
      
      if(traceToStderr)
        System.err.println(s);
    }
  }
}
