/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.hex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

/**
 * BufferedLineReader combines features of RandomAccessFile, StringReader, and BufferedReader, along
 * with one or two other features, like the ability to reset to the start of the stream, to keep
 * count of line numbers and byte positions, and to know the overall size.
 */
abstract class BufferedLineReader {

  protected long bsize;
  protected int bpos;
  protected int cpos;
  protected char[] buf = new char[4096];
  protected int bufCount = 0; // how much of buf is full
  protected int bufPos = 0; // read position in buf
  private boolean skipNextNewline = false;

  /** Construct a BufferedLineReader using String s as the underlying data source. */
  public static BufferedLineReader forString(String s) {
    return new ReaderForString(s);
  }

  /** Construct a BufferedLineReader using the named file as the underlying data source. */
  public static BufferedLineReader forFile(File filename) throws IOException {
    return new ReaderForFile(filename);
  }

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

  public int readBytes(byte[] bbuf, int off, int len) throws IOException {
    if (buf == null) throw new IOException("stream closed");
    if (len <= 0) return 0;
    if (skipNextNewline || bufPos < bufCount)
      throw new IOException("raw byte read after unicode I/O");
    int total = underlyingReadBytes(bbuf, off, len);
    if (total <= 0) return total;
    bpos += total;
    while (total < len && bpos < bsize) {
      int n = underlyingReadBytes(bbuf, off + total, len - total);
      if (n <= 0) break;
      total += n;
    }
    return total;
  }

  public String readLine() throws IOException {
    if (buf == null) throw new IOException("stream closed");

    // refill buffer, if necessary and possible
    if (bufPos >= bufCount && !refill()) return null; // EOF before reading anything

    // skip old leftover linefeed
    if (skipNextNewline && buf[bufPos] == '\n') {
      cpos++;
      bufPos++;

      // refill buffer, if necessary and possible
      if (bufPos >= bufCount && !refill()) return null; // EOF after skipping newline
    }
    skipNextNewline = false;

    StringBuilder ret = new StringBuilder(100);
    for (; ; ) {
      // scan buffered data looking for end of line
      int initPos = bufPos;
      while (bufPos < bufCount) {
        // consume one
        char c = buf[bufPos];
        cpos++;
        bufPos++;
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
      if (!refill())
        return ret.toString(); // note: file ended without newline
    }
  }

  public int readUtf8(char[] cbuf, int off, int len) throws IOException {
    if (buf == null) throw new IOException("stream closed");
    if (len <= 0) return 0;

    // zero-copy read, like BufferedReader does
    if (!skipNextNewline && bufPos >= bufCount && len >= buf.length) {
      int n = underlyingReadUtf8(cbuf, off, len);
      if (n > 0) cpos += n;
      return n;
    }

    // refill buffer, if necessary and possible
    if (bufPos >= bufCount && !refill()) return -1;

    // skip old leftover linefeed
    if (skipNextNewline && buf[bufPos] == '\n') {
      cpos++;
      bufPos++;

      // refill buffer, if necessary and possible
      if (bufPos >= bufCount && !refill()) return -1; // EOF after skipping newline
    }
    skipNextNewline = false;

    int n = Math.min(len, bufCount - bufPos);
    System.arraycopy(buf, bufPos, cbuf, off, n);
    cpos += n;
    bufPos += n;
    return n;
  }

  private boolean refill() throws IOException {
    bufPos = 0;
    bufCount = 0;
    for (; ; ) {
      int n = underlyingReadUtf8(buf, 0, buf.length);
      if (n < 0) {
        return false;
      } else if (n > 0) {
        bufPos = 0;
        bufCount = n;
        return true;
      }
    }
  }

  private static class ReaderForString extends BufferedLineReader {
    StringReader cin;
    ByteArrayInputStream bin;

    ReaderForString(String s) {
      cin = new StringReader(s);
      byte[] b = s.getBytes(StandardCharsets.UTF_8);
      bin = new ByteArrayInputStream(b);
      bsize = b.length;
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

    int underlyingReadBytes(byte[] bbuf, int off, int len) {
      return bin.read(bbuf, off, len);
    }
  }

  private static class Adapter extends InputStream {
    final RandomAccessFile randAccessFile;

    Adapter(RandomAccessFile in) {
      randAccessFile = in;
    }

    public int read() throws IOException {
      return randAccessFile.read();
    }

    public int read(byte[] b) throws IOException {
      return randAccessFile.read(b);
    }

    public int read(byte[] b, int off, int len) throws IOException {
      return randAccessFile.read(b, off, len);
    }
  }

  private static class ReaderForFile extends BufferedLineReader {
    RandomAccessFile bin;
    Reader cin;

    ReaderForFile(File f) throws IOException {
      bin = new RandomAccessFile(f, "r");
      cin = new InputStreamReader(new Adapter(bin), StandardCharsets.UTF_8);
      bsize = bin.length();
      bpos = 0;
      cpos = 0;
    }

    public void reset() throws IOException {
      bin.seek(0);
      // ISR buffers internally
      cin = new InputStreamReader(new Adapter(bin), StandardCharsets.UTF_8);
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
}
