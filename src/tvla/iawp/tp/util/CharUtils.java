//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.iawp.tp.util;

public class CharUtils {

  private static final int SPACEBAR = 32;

  private static final int TAB = 9;

  private static final int NEWLINE = 10;

  private static final int CR = 13;
  
  /**
   * @param i
   * @return boolean
   */
  public static boolean isWhitespace(int i) {
    return (i == SPACEBAR || i == TAB || i == NEWLINE || i == CR);
  }

}
