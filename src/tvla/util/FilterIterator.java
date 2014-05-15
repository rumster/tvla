package tvla.util;

import java.util.Iterator;


/**
 * A <code>FilterIterator</code> filters an
 * <code>Iterator</code> to generate a new one.
 * 
 * @author Mauricio J. Serrano
 * @author John Whaley
 * @author sfink
 */
public class FilterIterator<T> implements java.util.Iterator<T> {
  protected final Iterator<T> i;
  final Filter<T> f;
  protected T next = null;
  protected boolean done = false;

  /**
   * @param i the original iterator
   * @param f a filter which defines which elements belong to the generated iterator
   */
  public FilterIterator(Iterator<T> i, Filter<T> f) {
    this.i = i;
    this.f = f;
    advance();
  }


  /**
   * update the internal state to prepare for the next access to this iterator
   */
  protected void advance() {
    while (i.hasNext()) {
      next = i.next();
      if (f.accepts(next))
        return;
    }
    done = true;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public T next() {
    if (done)
      throw new java.util.NoSuchElementException();
    T o = next;
    advance();
    return o;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return !done;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  public void remove() {
    throw new java.lang.UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "filter " + f + " of " + i;
  }
}