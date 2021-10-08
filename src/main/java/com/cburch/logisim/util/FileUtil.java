/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

public final class FileUtil {

  private FileUtil() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  public static String correctPath(String path) {
    return path.endsWith(File.separator) ? path : path + File.separator;
  }

  public static File createTmpFile(String content, String prefix, String suffix) throws IOException {
    final var tmp = File.createTempFile(prefix, suffix);

    try (final var out = new BufferedWriter(new FileWriter(tmp))) {
      out.write(content, 0, content.length());
    }
    return tmp;
  }

  public static byte[] getBytes(InputStream is) throws IOException {
    int len;
    var size = 1024;
    byte[] buf;

    if (is instanceof ByteArrayInputStream) {
      size = is.available();
      buf = new byte[size];
      len = is.read(buf, 0, size);
    } else {
      final var bos = new ByteArrayOutputStream();
      buf = new byte[size];
      while ((len = is.read(buf, 0, size)) != -1) bos.write(buf, 0, len);
      buf = bos.toByteArray();
    }
    return buf;
  }
}
