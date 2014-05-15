package tvla.analysis.interproc.api.utils;

import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaDebuggingServices;

/*******************************************************************************
 * A simple convinient client (mainly for testing the API)
 *******************************************************************************/ 

public class SimpleClient implements ITVLAJavaDebuggingServices {
  public boolean trace() {
    return true;
  }
  
  public void tracePrint(String str) {
    System.out.print(str);
  }
  
  public void tracePrintln(String str) {
    System.out.println(str);
  }
  
  public void debugAssert(boolean b) {
    assert(b);
  }
  
  public void debugAssert(boolean b, String msg) {
    assert b : msg;
  }
  
  public void registerException(Exception e) {
    System.err.println( " == Exception eaised! program aborts == " + e.getMessage());
    e.printStackTrace();
    System.exit(-1);
  }
  
  public void UNREACHABLE() {
    throw new RuntimeException("UNREACHABLE hit!");
  }
  
  public void UNREACHABLE(String msg) {
    throw new RuntimeException("UNREACHABLE hit! " + msg);
  }
}