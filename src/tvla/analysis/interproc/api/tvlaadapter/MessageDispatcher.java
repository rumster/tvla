//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.tvlaadapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.analysis.interproc.api.tvlaadapter.abstraction.TVLATVS;
import tvla.analysis.interproc.api.utils.TVLAAPIAssert;
import tvla.analysis.interproc.api.utils.TVLAAPIDebugControl;
import tvla.api.ITVLATVS;
import tvla.api.ITVLAAPI.ITVLAApplierListener;
import tvla.api.ITVLAAPI.ITVLAMessage;
import tvla.api.ITVLATransformers.ITVSStagedTransformer;
import tvla.core.HighLevelTVS;

public class MessageDispatcher {
  public final static int DEBUG_LEVEL = TVLAAPIDebugControl.getDebugLevel(0);
  
  private List listeners;

  /**
     * Map used to report events to listners.
     * The list is resued accrros all actions
     */         
  private List events;


  public MessageDispatcher() {   
    this.listeners = new ArrayList(2); 
    this.events = new ArrayList(4);
  }
  
  public boolean registerListner(ITVLAApplierListener listener) {
    if (listener == null)
      return false;
    
    listeners.add(listener);
    return true;
  }
  
  
  public void reportMessages(ITVSStagedTransformer transformer, int input, Map msgs) {
    reportMessages(transformer, input, -1, msgs);
  }
  
  public void reportMessages(ITVSStagedTransformer transformer, int input1, int input2, Map msgs) {
    if (0 < DEBUG_LEVEL) {
      TVLAAPIAssert.debugAssert(events.isEmpty());
      TVLAAPIAssert.debugAssert(0 < listeners.size());
      TVLAAPIAssert.debugAssert(! msgs.isEmpty());
    }
    
    buildMessages(events, transformer, input1, input2, msgs);
    fireMessages(events);
    events.clear();
  }
  
  private void buildMessages(List reports, ITVSStagedTransformer transformer, int input1, int input2, Map msgs2) {       
    Set tvsXmsg  = msgs2.entrySet();
    Iterator tvsMsgItr = tvsXmsg.iterator();
    
    
    while (tvsMsgItr.hasNext()) {
      Map.Entry tvs_msg = (Map.Entry) tvsMsgItr.next();
      
      HighLevelTVS focused = (HighLevelTVS) tvs_msg.getKey();
      List msgs = (List) tvs_msg.getValue();
      
      Message report = new Message(transformer, input1, input2, focused, msgs);
      reports.add(report);
    }       
  }   
  
  private void fireMessages(List messages) {
      int numOfListeners = listeners.size();
      TVLAAPIAssert.debugAssert(! messages.isEmpty());
      TVLAAPIAssert.debugAssert(0 < numOfListeners);
      
      for (int i = 0 ; i < numOfListeners; i++) {
          fireMessagesToAListener((ITVLAApplierListener) listeners.get(i), messages);
      }
  }
  
  private void fireMessagesToAListener(ITVLAApplierListener listener, List messages) {
      int numOfMessages = messages.size();
      TVLAAPIAssert.debugAssert(! messages.isEmpty());
      
      for (int i = 0 ; i < numOfMessages; i++) {
          ITVLAMessage msg = (ITVLAMessage) messages.get(i);
          listener.messageGenerated(msg);
      }
  }
  
  
  private static class Message implements ITVLAMessage {
      final private ITVSStagedTransformer transformer;
      final private int input1;
      final private int input2;
      final private List msgs;
      final private HighLevelTVS focusedTVS;
      
      public Message(ITVSStagedTransformer transformer, int input1, int input2, HighLevelTVS specific, List msgs) {
          super();
          // TODO Auto-generated constructor stub
          this.transformer = transformer;
          this.input1 = input1;
          this.input2 = input2;
          this.focusedTVS = specific;
          this.msgs = msgs;
      }
      
      public List getMessagesAsStrings() {
          return msgs;
      }
      
      public int getCause(int tvsNum) {
          if (tvsNum == 0)
              return input1;
          
          if (tvsNum == 1)
              return input2;
          
          return -1;
      }
      
      public ITVLATVS specifyReason() {
          return new TVLATVS(focusedTVS);
      }
      
      public ITVSStagedTransformer getTransformer() {
          return transformer;
      }
      
      public String toString(){ 
          StringBuffer res = new StringBuffer( 
                  "Cause(0) id = " + input1 + 
                  ((-1 < input2) ? "Cause(1) id = " + input2 : " ") +
                  "Action " + transformer.toString() + " " +
                  "Specifically, " + focusedTVS);
          
          for (int i=0; i < msgs.size(); i++)
              res.append(" Message " + i + ": " + msgs.get(i) + " ");
          
          return res.toString();
          
      }
      
  }    
 

}
