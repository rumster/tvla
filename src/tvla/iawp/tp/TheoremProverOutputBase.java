/*
 * Created on 03/01/2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tvla.iawp.tp;

import tvla.iawp.tp.util.PeekableInputStream;

/**
 * @author guyerez
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public abstract class TheoremProverOutputBase implements TheoremProverOutput {

  /**
   * InputSteram from the native process. Should be a PeekableInputStream.
   */
  protected PeekableInputStream from;

  /**
   * InputSteram from the native process. Should be a PeekableInputStream.
   */
  protected PeekableInputStream error;

  public TheoremProverOutputBase(NativeProcess np) {
    this.from = np.fromStream();
    this.error = np.errorStream();
  }
}
