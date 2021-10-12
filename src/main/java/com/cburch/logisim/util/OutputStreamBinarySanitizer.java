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
import java.io.Writer;

public class OutputStreamBinarySanitizer extends OutputStream {
  protected final Writer out;

  public OutputStreamBinarySanitizer(Writer out) {
    this.out = out;
  }

  public void close() throws IOException {
    out.close();
  }

  public void flush() throws IOException {
    out.flush();
  }

  @SuppressWarnings("checkstyle:AvoidEscapedUnicodeCharacters")
  @Override
  public void write(int c) throws IOException {
    if ((0x20 <= c && c <= 0x7E) || c == '\t' || c == '\n' || c == '\r') out.write((char) c);
    else out.write('\uFFFD');
  }
}
