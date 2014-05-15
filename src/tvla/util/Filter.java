package tvla.util;

/**
 * @author sfink
 *
 * Simple interface for an intensional set definition
 */
public interface Filter<T> {
  /**
   * @param o
   * @return true iff o is in the set defined by this filter
   */
  public boolean accepts(T o);
}
