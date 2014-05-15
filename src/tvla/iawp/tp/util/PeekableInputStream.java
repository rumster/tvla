package tvla.iawp.tp.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import tvla.util.Logger;

/**
 * An input stream that supports peeking. PushbackInputStream allows peeking
 * before reading <a href="http://www.cs.ucy.ac.cy/~epl602/notes/javaio.pdf"> IO
 * Streams </a> <a
 * href="http://developer.java.sun.com/developer/technicalArticles/Streams/ProgIOStreams/">
 * Streams </a>
 * 
 * @author Eran Yahav (eyahav)
 */

public class PeekableInputStream extends PushbackInputStream {



  public PeekableInputStream(InputStream in) {
    super(in);
  }

  /*****************************************************************************
   * peek ahead by reading next byte on stream and putting it back (unread) if
   * its not the end of stream.
   */
  public int peek() {
    int next = -1;
    try {
      if (available() == 0) {
        return -1;
      }
      next = read();
      if (next != -1)
        unread(next);
    } catch (IOException e) {
      Logger.fatalError("IO Error " + e.getMessage());
    }
    // Noam: removed warning
    // finally {
    // return next;
    // }
    return next;
  }

  /**
   * a wrapper reading as character and handling the possible IO Exception
   */
  public char readChar() {
    int next = 0;

    try {
      next = read();
      if (next == -1) {
        // Logger.fatalError("Unexpected end of stream");
        return (char) 0;
      }
      return (char) next;
    } catch (IOException e) {
      // Logger.fatalError("IO Error " + e.getMessage());
      return (char) 0;
    }
  }

  public void skipWhitespaces() {
    for (int next = peek(); CharUtils.isWhitespace(next); next = peek()) {
      readChar();
    }
      
  }

  /**
   * skips over an integer number
   */
  public void skipInteger() {
    while (Character.isDigit((char) peek())) {
      readChar();
    }
  }

  /**
   * skips the next char on the stram.
   * 
   * @return true when the expected char was seen on the stream. false when
   *         skipping saw a different character.
   */
  public boolean skipChar(char c) {
    return (readChar() == c);
  }

  /**
   * skips a given string.
   * 
   * @return true when the string provided is the one actaully seen on the
   *         stream. False when skipping did not find that right string.
   */
  public boolean skipString(String str) {
    for (int i = 0; i < str.length(); i++)
      if (!skipChar(str.charAt(i)))
        return false;
    return true;
  }

  public String readLine() {
    StringBuffer sb = new StringBuffer();

    char ch = readChar();
    if (ch == 0) {
      return null;
    }
    
    do {
      if ((ch == '\n') || (ch < 0)) {
        return sb.toString();
      }
      if (ch != '\r') {
        sb.append(ch);
      }

      ch = readChar();
    } while (true);
  }

  
}
