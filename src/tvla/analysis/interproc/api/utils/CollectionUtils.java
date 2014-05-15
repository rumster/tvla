//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.utils;

import java.util.Collection;
import java.util.Iterator;

import tvla.util.StringUtils;

public class CollectionUtils {
  public static String printCollections(Collection col, boolean addNewLine) {
    StringBuffer sb = new StringBuffer();
    printCollection(col, sb, addNewLine);
    return sb.toString();
  }
  
  public static void printCollection(Collection col, StringBuffer to, boolean addNewLine) {
    if (col == null)
      addLine(to, "collection == null");
    else if (col.isEmpty()) 
      addLine(to, "col is empty");
    else {
      Iterator itr = col.iterator();
      while (itr.hasNext()) 
        addLine(to, itr.next().toString(), addNewLine);
    }
  }
  
  
  public static void addLine(StringBuffer bf, String str, boolean newLine) {
    bf.append(str);
    if (newLine)
      bf.append(StringUtils.newLine);
    else 
      bf.append(" ");
  }

  public static void addLine(StringBuffer bf, String str) {
    addLine(bf, str, true);
  }
}
