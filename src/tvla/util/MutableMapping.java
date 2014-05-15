package tvla.util;

import java.util.Iterator;
import java.util.Map;

/**
 * @author sfink
 * 
 * A bit set mapping based on an object array. This is not terribly efficient,
 * but is useful for prototyping.
 */
public class MutableMapping {

  private static final int INITIAL_CAPACITY = 20;

  private Object[] array;

  private int nextIndex = 0;

  /**
   * A mapping from object to Integer.
   */
  Map map = HashMapFactory.make();

  /**
   * Constructor for MutableMapping.
   */
  public MutableMapping(final Object[] array) {
    this.array = new Object[2 * array.length];
    for (int i = 0; i < array.length; i++) {
      this.array[i] = array[i];
      map.put(array[i], new Integer(i));
    }
    nextIndex = array.length;
  }

  /**
   * Constructor MutableMapping.
   */
  public MutableMapping() {
    array = new Object[INITIAL_CAPACITY];
    nextIndex = 0;
  }

  public Object getMappedObject(int n) {
    return array[n];
  }

  public int getMappedIndex(Object o) {
    Integer I = (Integer) map.get(o);
    if (I == null) {
      return -1;
    } else {
      return I.intValue();
    }

  }

  /**
   */
  public int getMappingSize() {
    return nextIndex;
  }

  /**
   * Add an Object to the set of mapped objects.
   * 
   * @return the integer to which the object is mapped.
   */
  public int add(Object o) {
    Integer I = (Integer) map.get(o);
    if (I != null) {
      return I.intValue();
    }
    map.put(o, new Integer(nextIndex));
    if (nextIndex >= array.length) {
      Object[] old = array;
      array = new Object[2 * array.length];
      System.arraycopy(old, 0, array, 0, old.length);
    }
    int result = nextIndex++;
    array[result] = o;
    return result;
  }

  public String toString() {
    StringBuffer result = new StringBuffer();
    for (int i = 0; i < nextIndex; i++) {
      result.append(i).append("  ").append(array[i]).append("\n");
    }
    return result.toString();
  }

  public Iterator iterator() {
    return map.keySet().iterator();
  }

  /**
   * @param n
   */
  public void deleteMappedObject(Object n) {
    int index = getMappedIndex(n);
    if (index != -1) {
      array[index] = null;
      map.remove(n);
    }

  }

}
