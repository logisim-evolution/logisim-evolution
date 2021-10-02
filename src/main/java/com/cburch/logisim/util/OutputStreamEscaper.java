/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

public class OutputStreamEscaper extends OutputStream {

  protected static final String[] escapes = {
      "\\0", // (0) 0x00
      "\\a", // (1) 0x07
      "\\b", // (2) 0x08
      "\\t", // (3) 0x09
      "\\n", // (4) 0x0a
      "\\v", // (5) 0x0b
      "\\f", // (6) 0x0c
      "\\r", // (7) 0x0d
      "\\\"", // (8) 0x22 - not used during stream operations
      "\\'", // (9) 0x27 - not used during stream operations
      "\\?", // (11) 0x3f - not used during stream operations
      "\\\\", // (11) 0x5c - special, handled during stream operations
  };

  protected final Writer out;
  protected boolean preserveWhitespace;
  protected int textWidth;
  protected int col;
  protected char lastChar;
  protected final char[] sep = System.lineSeparator().toCharArray();

  public OutputStreamEscaper(Writer out) {
    this.out = out;
  }

  public OutputStreamEscaper(Writer out, boolean preserveWhitespace, int textWidth) {
    this.out = out;
    this.preserveWhitespace = preserveWhitespace;
    this.textWidth = textWidth;
  }

  public boolean preservesWhitespace() {
    return preserveWhitespace;
  }

  public void preserveWhitespace(boolean b) {
    preserveWhitespace = b;
  }

  public int textWidth() {
    return textWidth;
  }

  public void textWidth(int cols) {
    textWidth = cols;
  }

  @Override
  public void close() throws IOException {
    flush();
    out.close();
  }

  @Override
  public void flush() throws IOException {
    if (textWidth > 0 && lastChar != '\n') {
      out.write(sep);
      lastChar = '\n';
      col = 0;
    }
    out.flush();
  }

  @Override
  public void write(int b) throws IOException {
    if (0x20 <= b && b <= 0x7E && b != '\\') {
      out.write(textWidth > 0 ? linebreak((char) b) : (char) b);
    } else if (preserveWhitespace && (b == '\n' || b == '\r' || b == '\t')) {
      out.write(textWidth > 0 ? linebreak((char) b) : (char) b);
    } else {
      String s = escapeCode(b);
      out.write(textWidth > 0 ? linebreak(s) : s);
    }
  }

  protected char linebreak(char b) throws IOException {
    if (b == '\r' || b == '\n') {
      col = 0;
    } else if (++col > textWidth) {
      out.write(sep);
      col = 1;
    }
    lastChar = b;
    return b;
  }

  protected String linebreak(String esc) throws IOException {
    int n = esc.length();
    if (col + n > textWidth) {
      out.write(sep);
      col = 0;
    }
    col += n;
    lastChar = 0;
    return esc;
  }

  protected static String escapeCode(int b) {
    return switch (b) {
      case 0x00 -> escapes[0];
      case 0x07 -> escapes[1];
      case 0x08 -> escapes[2];
      case 0x09 -> escapes[3];
      case 0x0a -> escapes[4];
      case 0x0b -> escapes[5];
      case 0x0c -> escapes[6];
      case 0x0d -> escapes[7];
      case 0x22 -> escapes[8];
      case 0x27 -> escapes[9];
      case 0x3f -> escapes[10];
      case 0x5c -> escapes[11];
      default -> "\\x" + (char) int2hex((b >>> 4) & 0xf) + (char) int2hex(b & 0xf);
    };
  }

  // converts 0-15 to ascii '0-9a-zA-Z' (or -1 on failure)
  public static byte int2hex(int n) {
    if (0 <= n && n <= 9) return (byte) ('0' + n);
    else if (0xA <= n && n <= 0xF) return (byte) ('A' + (n - 0xA));
    else return (byte) -1;
  }

  // converts any character to ascii string with C-like escapes
  public static String escape(char b) {
    return (b >= 0x20 && b <= 0x7E && b != '\\')
      ? String.valueOf(b)
      : escapeCode(b);
  }

  // converts a string to an ascii string with C-like escapes
  public static String escape(String w) {
    final var s = new StringWriter();
    for (var i = 0; i < w.length(); i++) {
      var b = w.charAt(i);
      if (b >= 0x20 && b <= 0x7E && b != '\\')
        s.write(b);
      else
        s.write(escapeCode(b));
    }
    return s.toString();
  }
}
