/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.file;

import static com.cburch.hdl.Strings.S;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class HdlFile {

  public static String load(File file) throws IOException {

    try (BufferedReader in = new BufferedReader(new FileReader(file))) {

      StringBuilder content = new StringBuilder();
      String l;

      while ((l = in.readLine()) != null) {
        content.append(l);
        content.append(System.getProperty("line.separator"));
      }
      return content.toString();
    } catch (IOException ex) {
      throw new IOException(S.get("hdlFileReaderError"));
    }
  }

  public static void save(File file, String text) throws IOException {

    try (BufferedWriter out = new BufferedWriter(new FileWriter(file))) {
      out.write(text, 0, text.length());
    } catch (IOException ex) {
      throw new IOException(S.get("hdlFileWriterError"));
    }
  }
}
