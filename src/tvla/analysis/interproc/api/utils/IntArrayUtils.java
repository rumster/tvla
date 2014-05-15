package tvla.analysis.interproc.api.utils;


import java.util.Arrays;

/**
 * A truckload of helper functions
 *
 */
public class IntArrayUtils {
  public final static int DEBUG_LEVEL = TVLAAPIDebugControl.getDebugLevel(2);
  
  /**
   * returns a copy of array without duplicated values
   * assuming all entries are bigger than -1
   */
  
  public static int[] prune(int[] array) {
    Arrays.sort(array);
    
    int distinctValues = array.length;
    for (int i=0; i<array.length-1; i++) {
      if (0 < DEBUG_LEVEL)
        TVLAAPIAssert.debugAssert(-2 < array[i]);
      
      if (array[i] == array[i+1]) {
        array[i] = -2;
        distinctValues--;
      }
    }
    
    if (0 < DEBUG_LEVEL)
      TVLAAPIAssert.debugAssert(-2 < array[array.length-1]);
    
    if (distinctValues == array.length)
      return array;
    
    if (0 < DEBUG_LEVEL) {
      TVLAAPIAssert.debugAssert(0 < distinctValues);
      TVLAAPIAssert.debugAssert(distinctValues <= array.length);
    }
    
    int[] ret = new int[distinctValues];
    for (int j=array.length-1; 0 <=j; j--) {	
      if (array[j] != -2) {
        distinctValues--;
        ret[distinctValues] = array[j];
      }
    }

    if (0 < DEBUG_LEVEL)
      TVLAAPIAssert.debugAssert(0 == distinctValues);
    
    return ret;
  }
  
  
  /**
   * returns an array which conatins the union of elements of the input arrays
   */
  
  public static int[] union(int[][] arrays, int numOfValidResults) {
    if (arrays == null || arrays.length == 0 || numOfValidResults == 0)
      return null;
    
    int resSize = 0;
    for (int i=0; i<numOfValidResults; i++)
      resSize += arrays[i].length;
    
    if (resSize == 0)
      return null;
        
    int[] joined = new int[resSize];
    for (int i=0, at=0; i < numOfValidResults; i++)
      for (int k=0 ; k < arrays[i].length; k++, at++)
        joined[at] = arrays[i][k];
    
    int[] ret = prune(joined);
    
    return ret;
  }
  
}
