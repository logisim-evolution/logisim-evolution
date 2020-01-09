/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.gui.hex;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.StringBuffer;

/**
 * BufferedLineReader combines features of RandomAccessFile, StringReader, and
 * BufferedReader, along with one or two other features, like the ability to
 * reset to the start of the stream, to keep count of line numbers and byte
 * positions, and to know the overall size.
 */

abstract class BufferedLineReader {

  /**
   * Construct a BufferedLineReader using String s as the underlying data
   * source.
   */
  public static BufferedLineReader forString(String s) {
    return new ReaderForString(s);
  }

  /**
   * Construct a BufferedLineReader using the named file as the underlying data
   * source.
   */
  public static BufferedLineReader forFile(File filename)
      throws IOException {
    return new ReaderForFile(filename);
  }

  private static class ReaderForString extends BufferedLineReader {
    StringReader cin;
    ByteArrayInputStream bin;

    ReaderForString(String s) {
      cin = new StringReader(s);
      try {
        byte[] b = s.getBytes("UTF-8");
        bin = new ByteArrayInputStream(b);
        bsize = b.length;
      } catch (UnsupportedEncodingException e) {
        throw new IllegalStateException(e.getMessage());
      }
      bpos = 0;
      cpos = 0;
    }
    public void reset() throws IOException {
      cin.reset();
      bin.reset();
      super.reset();
    }
    public void close() throws IOException {
      try {
        bin.close();
      } finally {
        cin = null;
        bin = null;
        buf = null;
      }
    }
    int underlyingReadUtf8(char[] cbuf, int off, int len) throws IOException {
      return cin.read(cbuf, off, len);
    }
    int underlyingReadBytes(byte[] bbuf, int off, int len) throws IOException {
      return bin.read(bbuf, off, len);
    }
  }

  private static class Adapter extends InputStream {
    RandomAccessFile r;
    int n;
    Adapter(RandomAccessFile in) { r = in; }
    public int read() throws IOException { return r.read(); }
    public int read(byte[] b) throws IOException { return r.read(b); }
    public int read(byte[] b, int off, int len) throws IOException { return r.read(b, off, len); }
  }

  private static class ReaderForFile extends BufferedLineReader {
    RandomAccessFile bin;
    Reader cin;
    ReaderForFile(File f) throws IOException {
      bin = new RandomAccessFile(f, "r");
      cin = new InputStreamReader(new Adapter(bin), "UTF-8");
      bsize = bin.length();
      bpos = 0;
      cpos = 0;
    }
    
    public void reset() throws IOException {
      bin.seek(0);
      cin = new InputStreamReader(new Adapter(bin), "UTF-8"); // ISR buffers internally
      super.reset();
    }
    
    public void close() throws IOException {
      try {
        bin.close();
      } finally {
        bin = null;
        cin = null;
        buf = null;
      }
    }

    int underlyingReadUtf8(char[] cbuf, int off, int len) throws IOException {
      return cin.read(cbuf, off, len);
    }
    
    int underlyingReadBytes(byte[] bbuf, int off, int len) throws IOException {
      return bin.read(bbuf, off, len);
    }
  }

  protected long bsize;
  protected int bpos, cpos;

  protected char[] buf = new char[4096];
  protected int bufCount = 0; // how much of buf is full
  protected int bufPos = 0; // read position in buf

  private boolean skipNextNewline = false;

  public void reset() throws IOException {
    bpos = 0;
    cpos = 0;
    bufPos = 0;
    bufCount = 0;
    skipNextNewline = false;
  }

  public int bytePosition() {
    return bpos;
  }

  public int charPosition() {
    return cpos;
  }

  public long byteLength() {
    return bsize;
  }

  abstract int underlyingReadUtf8(char[] cbuf, int off, int len) throws IOException;
  abstract int underlyingReadBytes(byte[] bbuf, int off, int len) throws IOException;
  public abstract void close() throws IOException;


  public int readBytes(byte bbuf[], int off, int len) throws IOException {
    if (buf == null)
      throw new IOException("stream closed");
    if (len <= 0)
      return 0;
    if (skipNextNewline || bufPos < bufCount)
      throw new IOException("raw byte read after unicode I/O");
    int total = underlyingReadBytes(bbuf, off, len);
    if (total <= 0)
      return total;
    bpos += total;
    while (total < len && bpos < bsize) {
      int n = underlyingReadBytes(bbuf, off + total, len - total);
      if (n <= 0)
        break;
      total += n;
    }
    return total;
  }

  public String readLine() throws IOException {
    if (buf == null)
      throw new IOException("stream closed");

    // refill buffer, if necessary and possible
    if (bufPos >= bufCount && !refill()) 
        return null; // EOF before reading anything

    // skip old leftover linefeed
    if (skipNextNewline && buf[bufPos] == '\n') {
      cpos++; bufPos++;

      // refill buffer, if necessary and possible
      if (bufPos >= bufCount && !refill()) 
          return null; // EOF after skipping newline
    }
    skipNextNewline = false;

    StringBuffer ret = new StringBuffer(100);
    for (;;) {
      // scan buffered data looking for end of line
      int initPos = bufPos;
      while (bufPos < bufCount) {
        // consume one
        char c = buf[bufPos];
        cpos++; bufPos++;
        // check for end of line
        if (c == '\n' || c == '\r') {
          skipNextNewline = (c == '\r');
          ret.append(buf, initPos, bufPos - initPos - 1);
          return ret.toString();
        }
      }

      // append partial line
      ret.append(buf, initPos, bufPos - initPos);

      // refill buffer, if necessary and possible
      if (bufPos >= bufCount && !refill()) 
          return ret.toString(); // note: file ended without newline
    }
  }

  public int readUtf8(char[] cbuf, int off, int len) throws IOException {
    if (buf == null)
      throw new IOException("stream closed");
    if (len <= 0)
      return 0;

    // zero-copy read, like BufferedReader does
    if (!skipNextNewline && bufPos >= bufCount && len >= buf.length) {
      int n = underlyingReadUtf8(cbuf, off, len);
      if (n > 0)
        cpos += n;
      return n;
    }

    // refill buffer, if necessary and possible
    if (bufPos >= bufCount && !refill()) 
        return -1; 

    // skip old leftover linefeed
    if (skipNextNewline && buf[bufPos] == '\n') {
      cpos++; bufPos++;

      // refill buffer, if necessary and possible
      if (bufPos >= bufCount && !refill()) 
          return -1; // EOF after skipping newline
    }
    skipNextNewline = false;

    int n = Math.min(len, bufCount - bufPos);
    System.arraycopy(buf, bufPos, cbuf, off, n);
    cpos += n; bufPos += n;
    return n;
  }

  private boolean refill() throws IOException {
    bufPos = 0;
    bufCount = 0;
    for (;;) {
      int n = underlyingReadUtf8(buf, 0, buf.length);
      if (n < 0) {
        return false;
      }else if (n > 0) {
        bufPos = 0;
        bufCount = n;
        return true;
      }
    }
  }

}