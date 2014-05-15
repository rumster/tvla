/*
 * Created on Aug 24, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package tvla.iawp.tp;

import tvla.exceptions.TVLAException;

/**
 * @author user
 * 
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
/**
 * An exception denoting a (possibly) non-termination condition for Focus.
 */
public class InconsistentModelException extends TVLAException {
  /**
   * 
   */
  private static final long serialVersionUID = 7077609665842776073L;

  public InconsistentModelException(String message) {
    super(message);
  }
}
