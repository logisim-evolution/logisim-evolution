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

public class HdlFile {

  public static void open(File file, HdlContentEditor editor) throws IOException {

    try (final var in = new BufferedReader(new FileReader(file))) {

      final var content = new StringBuilder();
      String l;

      while ((l = in.readLine()) != null) {
        content.append(l);
        content.append(System.getProperty("line.separator"));
      }
      editor.setText(content.toString());
    } catch (IOException ex) {
      throw new IOException(S.get("hdlFileReaderError"));
    }
  }

  public static void save(File file, HdlContentEditor editor) throws IOException {

    try (final var out = new BufferedWriter(new FileWriter(file))) {
      final var data = editor.getText();
      out.write(data, 0, data.length());
    } catch (IOException ex) {
      throw new IOException(S.get("hdlFileWriterError"));
    }
  }
}
