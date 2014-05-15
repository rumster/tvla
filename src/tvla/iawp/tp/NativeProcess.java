package tvla.iawp.tp;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import tvla.iawp.tp.util.PeekableInputStream;
import tvla.util.Logger;
import tvla.util.ProgramProperties;

/*******************************************************************************
 * Wrapper for a native invoked process used for running the theorem prover.
 * 
 * "The created subprocess does not have its own terminal or console. All its
 * standard io (i.e. stdin, stdout, stderr) operations will be redirected to the
 * parent process through three streams (Process.getOutputStream(),
 * Process.getInputStream(), Process.getErrorStream()). The parent process uses
 * these streams to feed input to and get output from the subprocess. Because
 * some native platforms only provide limited buffer size for standard input and
 * output streams, failure to promptly write the input stream or read the output
 * stream of the subprocess may cause the subprocess to block, and even
 * deadlock." -- java.lang.Process documentation.
 * 
 * This clas implements a quite standard way for hadnling runtime.exec
 * processes. <a
 * href="http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html">tips
 * on runtime.exec</a> <a
 * href="http://mountainstorm.com/publications/javazine.html"> more on
 * runtime.exec </a>
 * 
 * @author Eran Yahav (eyahav)
 */
public class NativeProcess {

  private final int MAX_CHUNK = 100000;

  /**
   * debug flag
   */
  private static final boolean fileDebug = ProgramProperties.getBooleanProperty("tvla.tp.filedebug", false);

  /**
   * file name for debug output recording file
   */
  private static final String debugFileName = ProgramProperties.getProperty("tvla.tp.debugfilename", "iawpout.txt");

  /**
   * name of the native process
   */
  public final String name;

  /**
   * the process itself
   */
  protected Process np = null;

  /**
   * output stream _to_ the process
   */
  protected PrintStream to = null;

  /**
   * debug output file, logging all information sent to the theorem prover.
   * writing to this file is enabled by the boolean flag fileDebug
   */
  protected FileWriter debugOutputFile = null;

  /**
   * a Peekable input stream, allows peeking on the next character coming from
   * the process
   */
  protected PeekableInputStream from = null;

  /**
   * error stream from the process
   */
  protected PeekableInputStream error;

  /**
   * starts a native process with the given name and path
   */
  public NativeProcess(String name, String path) {
    this.name = name;
    try {
      np = Runtime.getRuntime().exec(path);
      assert np != null : "Could not start" + name;
    } catch (IOException e) {
      Logger.fatalError("\n IO Error starting" + name + " from " + path + "\n check your path settings and executable name for "
          + name + "\n");
    }
    OutputStream out = np.getOutputStream();
    assert out != null : "Could not get output stream";
    to = new PrintStream(out);
    from = new PeekableInputStream(np.getInputStream());
    error = new PeekableInputStream(np.getErrorStream());

    if (fileDebug)
      try {
        debugOutputFile = new FileWriter("c:\\temp\\guye_" + name + "_" + System.currentTimeMillis());
      } catch (IOException e) {
        Logger.fatalError("IO Error opening debug file " + debugFileName);
      }

    assert to != null : "Could not create output stream";
    assert from != null : "Could not create input stream";
  }

  /**
   * close the native process streams and destroy the native process
   */
  public void close() {
    try {
      if (to != null) {
        to.close();
        to = null;
      }
      if (from != null) {
        from.close();
        from = null;
      }
    } catch (IOException e) {
      Logger.fatalError(name + e.getMessage());
    }
  }

  /**
   * returns the input stream _from_ the native process note that this is a
   * peekable input stream
   */
  public PeekableInputStream fromStream() {
    return from;
  }

  /**
   * returns the error stream _from_ the native process.
   * note that this is a peekable input stream.
   */
  public PeekableInputStream errorStream() {
    return error;
  }
  
  /**
   * sends a string to the native-process by putting it on the _to_ output
   * stream if fileDebug is enabled, also writes string to the debugFileName
   * file
   */
  public void send(String s) {
    assert np != null;
    writeDebugFile(s);

    to.println(s);
    to.flush();
  }

  public void strongSend(String s) {
    assert np != null;
    // System.out.println(s.length()+":"+s);

    to.println(s);
    to.print("\0");
    to.flush();
    to.close();
    writeDebugFile(s);

  }

  private void writeDebugFile(String s) {

    if (fileDebug) {
      try {
        debugOutputFile.write(s);
        debugOutputFile.flush();

      } catch (IOException e) {
        Logger.fatalError("Error writing to debug file" + debugFileName);
      }
    }

  }

  public int exitValue() {
    return np.exitValue();
  }

}