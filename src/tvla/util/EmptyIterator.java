package tvla.util;

import java.util.Iterator;


/**
 * @author sfink
 *
 * A singleton instance of an empty iterator; this is better than
 * Collections.EMPTY_SET.iterator(), which allocates an iterator object;
 */
public final class EmptyIterator<T> implements Iterator<T> {

  private static final EmptyIterator EMPTY = new EmptyIterator();

  @SuppressWarnings("unchecked")
  public static <T> EmptyIterator<T> instance() {
    return EMPTY;
  }

  /**
   * prevent instantiation
   */
  private EmptyIterator() {
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return false;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public T next() {
    return null;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  public void remove() {
    throw new UnsupportedOperationException();
  }

}
