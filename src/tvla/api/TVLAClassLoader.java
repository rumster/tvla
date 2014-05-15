//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.api;

public class TVLAClassLoader extends ClassLoader {
  private static int DEBUG_LEVEL = 1;
  
  public static void setTVLAClassLoader() {
    
    ClassLoader newCL = new  TVLAClassLoader();
    
    Thread.currentThread().setContextClassLoader(newCL);
  }
  
  public Class loadClass(String name) throws ClassNotFoundException {
    if (0 < DEBUG_LEVEL)
      System.out.println("-CLSLDR- loading " + name);
    
    return super.loadClass(name);
  }

}
