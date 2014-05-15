//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.utils;

import tvla.api.ITVLAAPIDebuggingServices;

public class TVLAAPIAssert {
  public static final boolean ASSERT = TVLAAPIDebugControl.Assert.tvlaapiAssertActive;
  
  public static ITVLAAPIDebuggingServices clientServices = null;
  
  public static void setClient(ITVLAAPIDebuggingServices client) {
    clientServices = client;
  }

  public static void debugAssert(boolean b) {
   if (ASSERT) {
     if (TVLAAPIDebugControl.Assert.tvlaapiAssertClientActive && clientServices != null)
       clientServices.debugAssert(b);
     else
       assert(b);
   }
  }

  public static void debugAssert(boolean b, String msg) {
    if (ASSERT) {
      if (TVLAAPIDebugControl.Assert.tvlaapiAssertClientActive && clientServices != null)
        clientServices.debugAssert(b, msg);
      else
        assert b : msg;
    }
  }

  public static void UNREACHABLE() {
    if (ASSERT) {
      if (TVLAAPIDebugControl.Assert.tvlaapiAssertClientActive && clientServices != null)
        clientServices.UNREACHABLE();
      else
        assert false : "UNREACHABLE!";
    }    
  }

  public static void UNREACHABLE(String msg) {
    if (ASSERT) {
      if (TVLAAPIDebugControl.Assert.tvlaapiAssertClientActive && clientServices != null)
        clientServices.UNREACHABLE(msg);
      else
        assert false : "UNREACHABLE! " + msg;
    }    
  }
}
