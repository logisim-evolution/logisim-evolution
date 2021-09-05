/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.hdl;

import static com.cburch.hdl.Strings.S;

import com.cburch.logisim.std.hdl.HdlContentEditor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import lombok.val;

public class HdlFile {

  public static void open(File file, HdlContentEditor editor) throws IOException {
    try {
      val in = new BufferedReader(new FileReader(file));
      val content = new StringBuilder();
      String line;

      while ((line = in.readLine()) != null) {
        content.append(line);
        // FIXME we can most likely replace `line.property` with Java's "%n" platform specific LF
        content.append(System.getProperty("line.separator"));
      }
      editor.setText(content.toString());
    } catch (IOException ex) {
      throw new IOException(S.get("hdlFileReaderError"));
    }
  }

  public static void save(File file, HdlContentEditor editor) throws IOException {
    try {
      val out = new BufferedWriter(new FileWriter(file));
      val data = editor.getText();
      out.write(data, 0, data.length());
    } catch (IOException ex) {
      throw new IOException(S.get("hdlFileWriterError"));
    }
  }
}
